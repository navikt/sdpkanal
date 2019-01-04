package no.nav.kanal.camel

import com.fasterxml.jackson.module.kotlin.readValue
import com.jcraft.jsch.ChannelSftp
import com.nhaarman.mockitokotlin2.reset
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.timeout
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import no.difi.begrep.sdp.schema_v10.SDPKvittering
import no.difi.begrep.sdp.schema_v10.SDPMelding
import no.nav.kanal.camel.ebms.EbmsPush
import no.nav.kanal.config.SdpConfiguration
import no.nav.kanal.config.SdpKeys
import no.nav.kanal.config.VaultCredentials
import no.nav.kanal.config.createJmsConfig
import no.nav.kanal.config.createMessageSender
import no.nav.kanal.datahandler
import no.nav.kanal.log.LegalArchiveLogger
import no.nav.kanal.objectMapper
import no.nav.kanal.receiver
import no.nav.kanal.route.createDeadLetterRoute
import no.nav.kanal.route.createReceiptPollingRoute
import no.nav.kanal.route.createSendRoute
import org.amshove.kluent.any
import org.amshove.kluent.mock
import org.amshove.kluent.shouldBeInstanceOf
import org.amshove.kluent.shouldEqual
import org.amshove.kluent.shouldNotEqual
import org.apache.activemq.artemis.core.config.impl.ConfigurationImpl
import org.apache.activemq.artemis.core.server.ActiveMQServers
import org.apache.camel.component.jms.JmsEndpoint
import org.apache.camel.impl.DefaultCamelContext
import org.apache.camel.impl.DefaultShutdownStrategy
import org.apache.commons.io.IOUtils
import org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.SignalMessage
import org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.UserMessage
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import org.unece.cefact.namespaces.standardbusinessdocumentheader.StandardBusinessDocument
import java.io.StringReader
import java.net.ServerSocket
import java.nio.file.Files
import java.nio.file.Paths
import java.util.Base64
import java.util.UUID
import java.util.concurrent.locks.ReentrantReadWriteLock
import javax.jms.BytesMessage
import javax.jms.ConnectionFactory
import javax.jms.Session
import javax.jms.TextMessage
import javax.naming.InitialContext
import javax.xml.bind.JAXBContext
import kotlin.concurrent.write

val sdpMockPort: Int = ServerSocket(0).use {
    it.localPort
}

open class QueuedReceiptHandler : SbdHandler {
    val receipts: MutableList<EbmsResponse> = mutableListOf()
    val userMessageHandlers = mutableListOf<()->EbmsResponse>()
    private val reentrantReadWriteLock: ReentrantReadWriteLock = ReentrantReadWriteLock()

    override fun handleUserMessage(sbdIn: StandardBusinessDocument, attachments: List<EbmsAttachment>, senderOrgNumber: String, userMessage: UserMessage): EbmsResponse {
        // Assume its a request to send SDP
        if (userMessageHandlers.isEmpty())
            return EbmsResponse(null, null, true, listOf())
        return reentrantReadWriteLock.write {
            userMessageHandlers.removeAt(0)()
        }
    }


    override fun handleSignalMessage(attachments: List<EbmsAttachment>, senderOrgNumber: String, signalMessage: SignalMessage): EbmsResponse = reentrantReadWriteLock.write {
        if (receipts.size > 0) {
            println("Returning receipt")
            return receipts.removeAt(0)
        }
        DefaultSbdHandler.noNewMessages()
    }
}

object SdpKanalITSpek : Spek({
    System.setProperty("javax.xml.soap.SAAJMetaFactory", "com.sun.xml.messaging.saaj.soap.SAAJMetaFactoryImpl")
    val activeMQServer = ActiveMQServers.newActiveMQServer(ConfigurationImpl()
            .setPersistenceEnabled(false)
            .setJournalDirectory("target/data/journal")
            .setSecurityEnabled(false)
            .addAcceptorConfiguration("invm", "vm://0"))
    activeMQServer.start()
    val keyStore = generateKeyStore().apply {
        Files.newOutputStream(Paths.get("build/keystore.p12")).use {
            store(it, "changeit".toCharArray())
        }
        Base64.getEncoder().wrap(Files.newOutputStream(Paths.get("build/keystore.p12.b64"))).use {
            store(it, "changeit".toCharArray())
        }
    }
    //val keystoreB64 = Base64.getEncoder().encode(Files.readAllBytes(Paths.get("build/keystore.p12")))
    //Files.write(Paths.get("build/keystore.p12.b64"), keystoreB64)

    val requestHandler = spy(QueuedReceiptHandler())
    val sdpServer = createSDPMockServer(sbdHandler = requestHandler, port = sdpMockPort, keyStore = keyStore)

    val config = SdpConfiguration(
            knownHostsFile = "TODO",
            ebmsEndpointUrl = "http://localhost:$sdpMockPort/sdpmock",
            mqHostname = "UNUSED",
            mqPort = -1,
            mqQueueManager = "UNUSED",
            mqChannel = "UNUSED",
            inputQueueNormal = "sdp_input_normal",
            inputQueuePriority = "sdp_input_priority",
            inputQueueNormalBackout = "sdp_input_normal_boq",
            inputQueuePriorityBackout = "sdp_input_priority_boq",
            receiptQueueNormal = "sdp_receipt_normal",
            receiptQueuePriority = "sdp_receipt_priority",
            receiptQueueNormalBackout = "sdp_receipt_normal_boq",
            receiptPriorityBackoutQueue = "sdp_receipt_priority_boq",
            keystorePath = "build/keystore.p12.b64",
            truststorePath = "build/keystore.p12.b64",
            mqConcurrentConsumers = 1,
            receiptPollIntervalNormal = 1000
    )
    val vaultCredentials: VaultCredentials = objectMapper.readValue(VaultCredentials::class.java.getResourceAsStream("/vault.json"))
    val sftpChannel = mock(ChannelSftp::class)
    val legalArchive = mock(LegalArchiveLogger::class)

    val sdpKeys = SdpKeys(config.keystorePath, config.truststorePath, vaultCredentials)

    val connectionFactory = InitialContext().lookup("ConnectionFactory") as ConnectionFactory
    val queueConnection = connectionFactory.createConnection()
    queueConnection.start()
    val session = queueConnection.createSession(false, Session.AUTO_ACKNOWLEDGE)

    val jmsConfig = createJmsConfig(connectionFactory, config.mqConcurrentConsumers)

    val boqLogger = BOQLogger(legalArchive)
    val backoutReason = BackoutReason()
    val xmlExtractor = XmlExtractor()

    val documentPackageCreator = DocumentPackageCreator(sdpKeys, sftpChannel, "/tmp/documents/")
    val messageSender = createMessageSender(datahandler, receiver, sdpKeys, vaultCredentials, config.ebmsEndpointUrl)
    val ebmsPull = EbmsPull(messageSender, receiver)
    val ebmsPush = EbmsPush(config.maxRetries, config.retryIntervalInSeconds, legalArchive, messageSender, datahandler, receiver)

    val camelContext = DefaultCamelContext().apply {
        isAllowUseOriginalMessage = true
        fun createJmsEndpoint(queueName: String): JmsEndpoint {
            val endpoint = JmsEndpoint.newInstance(session.createQueue(queueName))
            // TODO: endpoint.transactionManager = transactionManager
            endpoint.configuration = jmsConfig
            endpoint.camelContext = this
            return endpoint
        }

        val inputQueueNormal = createJmsEndpoint(config.inputQueueNormal)
        val inputQueuePriority = createJmsEndpoint(config.inputQueuePriority)
        val inputQueueNormalBackout = createJmsEndpoint(config.inputQueueNormalBackout)
        val inputQueuePriorityBackout = createJmsEndpoint(config.inputQueuePriorityBackout)
        val receiptQueueNormal = createJmsEndpoint(config.receiptQueueNormal)
        val receiptQueuePriority = createJmsEndpoint(config.receiptQueuePriority)
        // TODO: Wire up backout queues for receipts
        val receiptNormalBackoutQueue = createJmsEndpoint(config.receiptQueueNormalBackout)
        val receiptPriorityBackoutQueue = createJmsEndpoint(config.receiptPriorityBackoutQueue)

        shutdownStrategy = DefaultShutdownStrategy().apply { timeout  = 20 }
        disableJMX()
        addRoutes(createReceiptPollingRoute("pullReceiptsPriority", config.mpcPrioritert, config.receiptPollIntervalNormal, ebmsPull, receiptQueuePriority))
        addRoutes(createReceiptPollingRoute("pullReceiptsNormal", config.mpcNormal, config.receiptPollIntervalNormal, ebmsPull, receiptQueueNormal))

        addRoutes(createDeadLetterRoute("backoutMessageNormal", inputQueueNormalBackout, boqLogger, backoutReason))
        addRoutes(createDeadLetterRoute("backoutMessagePriority", inputQueuePriorityBackout, boqLogger, backoutReason))

        addRoutes(createSendRoute("sendSDPNormal", config.mpcNormal, inputQueueNormal, "backoutMessageNormal", xmlExtractor, documentPackageCreator, ebmsPush))
        addRoutes(createSendRoute("sendSDPPriority", config.mpcPrioritert, inputQueuePriority, "backoutMessagePriority", xmlExtractor, documentPackageCreator, ebmsPush))
    }
    camelContext.start()

    val inputQueueSender = session.createProducer(session.createQueue(config.inputQueueNormal))
    val receiptConsumer = session.createConsumer(session.createQueue(config.receiptQueueNormal))
    val backoutConsumer = session.createConsumer(session.createQueue(config.inputQueueNormalBackout))
    val messageBytes = IOUtils.toByteArray(SdpKanalITSpek::class.java.getResourceAsStream("/payloads/inline.xml"))

    afterEachTest {
        reset(requestHandler)
    }

    afterGroup {
        camelContext.shutdown()
        queueConnection.close()
        sdpServer.server.stop()
        sdpServer.server.destroy()
        sdpServer.sfb.bus.shutdown(true)
        activeMQServer.stop(true)
        println("SHUTDOWN")
    }

    describe("Sending messages on the input queue") {
        it("Results in the message dispatcher receiving the message") {
            inputQueueSender.send(session.createTextMessage(messageBytes.toString(Charsets.UTF_8)))
            verify(requestHandler, timeout(20000).times(1)).handleUserMessage(any(), any(), any(), any())
        }
    }

    describe("Returning a receipt from the message dispatcher") {
        it ("Results in a receipt on the receipt queue") {
            val messageId = UUID.randomUUID().toString()
            println("UUID used for messageId $messageId")
            requestHandler.receipts.add(DefaultSbdHandler.defaultReceipt(messageId))

            val message = receiptConsumer.receive(10000)
            message shouldBeInstanceOf BytesMessage::class
            message as BytesMessage
            val bytes = ByteArray(message.bodyLength.toInt())
            message.readBytes(bytes)
            val receipt = bytes.toString(Charsets.UTF_8)
            // The receipt should be able to get unmarshalled into a StandardBusinessDocument
            val sbdJaxbContext = JAXBContext.newInstance(StandardBusinessDocument::class.java, SDPMelding::class.java)
            val sbdUnmarshaller = sbdJaxbContext.createUnmarshaller()
            val sbd = sbdUnmarshaller.unmarshal(StringReader(receipt)) as StandardBusinessDocument
            sbd.any shouldNotEqual null
            sbd.any shouldBeInstanceOf SDPKvittering::class
            sbd.standardBusinessDocumentHeader.documentIdentification.instanceIdentifier shouldEqual messageId
            println("Received receipt $receipt")
        }
    }

    describe("SOAP fault from the message dispatcher") {
        it("Results in the message ending up on the backout queue") {
            inputQueueSender.send(session.createTextMessage(messageBytes.toString(Charsets.UTF_8)))
            requestHandler.userMessageHandlers.add {
                throw RuntimeException("Integration test")
            }

            val message = backoutConsumer.receive(10000)
            message shouldBeInstanceOf TextMessage::class
            message as TextMessage
            message.text shouldEqual messageBytes.toString(Charsets.UTF_8)
        }
    }
})
