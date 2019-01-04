package no.nav.kanal.camel

import com.fasterxml.jackson.module.kotlin.readValue
import com.jcraft.jsch.ChannelSftp
import com.nhaarman.mockitokotlin2.reset
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.timeout
import com.nhaarman.mockitokotlin2.verify
import no.difi.begrep.sdp.schema_v10.SDPKvittering
import no.difi.begrep.sdp.schema_v10.SDPMelding
import no.digipost.api.representations.EbmsOutgoingMessage
import no.nav.kanal.config.SdpConfiguration
import no.nav.kanal.config.SdpKeys
import no.nav.kanal.config.VaultCredentials
import no.nav.kanal.createCamelContext
import no.nav.kanal.log.LegalArchiveLogger
import no.nav.kanal.objectMapper
import org.amshove.kluent.any
import org.amshove.kluent.mock
import org.amshove.kluent.shouldBeInstanceOf
import org.amshove.kluent.shouldEqual
import org.amshove.kluent.shouldNotEqual
import org.apache.activemq.artemis.core.config.impl.ConfigurationImpl
import org.apache.activemq.artemis.core.server.ActiveMQServers
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
import javax.jms.TextMessage
import javax.naming.InitialContext
import javax.xml.bind.JAXBContext
import kotlin.concurrent.write

val sdpMockPort: Int = ServerSocket(0).use {
    it.localPort
}

open class QueuedReceiptHandler : SbdHandler {
    val receipts: MutableMap<String, EbmsResponse> = mutableMapOf()
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
        println("WAZZAH DUDE ${signalMessage.pullRequest?.mpc}")
        if (!receipts.isEmpty()) {
            println("Looking for ${signalMessage.pullRequest?.mpc} in $receipts")
        }
        if (signalMessage.pullRequest != null && receipts.containsKey(signalMessage.pullRequest.mpc)) {
            println("Returning receipt")
            return receipts.remove(signalMessage.pullRequest.mpc)!!
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

    val camelContext = createCamelContext(config, vaultCredentials, sdpKeys, connectionFactory, sftpChannel, legalArchive)
    camelContext.start()

    val session = queueConnection.createSession()

    val normalQueueSender = session.createProducer(session.createQueue(config.inputQueueNormal))
    val priorityQueueSender = session.createProducer(session.createQueue(config.inputQueuePriority))
    val normalReceiptConsumer = session.createConsumer(session.createQueue(config.receiptQueueNormal))
    val priorityReceiptConsumer = session.createConsumer(session.createQueue(config.receiptQueuePriority))
    val normalBackoutConsumer = session.createConsumer(session.createQueue(config.inputQueueNormalBackout))
    val priorityBackoutConsumer = session.createConsumer(session.createQueue(config.inputQueuePriorityBackout))

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
        listOf(normalQueueSender, priorityQueueSender).forEach {
            it("Results in the message dispatcher receiving the message for input queue ${it.destination}") {
                it.send(session.createTextMessage(messageBytes.toString(Charsets.UTF_8)))
                verify(requestHandler, timeout(20000).times(1)).handleUserMessage(any(), any(), any(), any())
            }
        }
    }

    describe("Returning a receipt from the message dispatcher") {
        val normalMpc = "urn:${EbmsOutgoingMessage.Prioritet.NORMAL.value()}:${config.mpcNormal}"
        val priorityMpc = "urn:${EbmsOutgoingMessage.Prioritet.PRIORITERT.value()}:${config.mpcPrioritert}"
        listOf(normalMpc to normalReceiptConsumer, priorityMpc to priorityReceiptConsumer).forEach { (mpcId, receiptQueue) ->
            it("Returning a receipt with mpc $mpcId results in a receipt on the queue $receiptQueue") {
                val messageId = UUID.randomUUID().toString()
                println("UUID used for messageId $messageId")
                requestHandler.receipts[mpcId] = DefaultSbdHandler.defaultReceipt(messageId)

                val message = receiptQueue.receive(10000)
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
    }

    describe("SOAP fault from the message dispatcher") {
        listOf(normalQueueSender to normalBackoutConsumer, priorityQueueSender to priorityBackoutConsumer).forEach { (inputQueue, backoutQueue) ->
            it("Sending a message on the input queue ${inputQueue.destination} while exception is thrown ends up at $backoutQueue") {
                inputQueue.send(session.createTextMessage(messageBytes.toString(Charsets.UTF_8)))
                requestHandler.userMessageHandlers.add {
                    throw RuntimeException("Integration test")
                }

                val message = backoutQueue.receive(10000)
                message shouldBeInstanceOf TextMessage::class
                message as TextMessage
                message.text shouldEqual messageBytes.toString(Charsets.UTF_8)
            }
        }
    }
})
