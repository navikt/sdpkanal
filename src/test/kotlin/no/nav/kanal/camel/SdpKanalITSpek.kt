package no.nav.kanal.camel

import com.fasterxml.jackson.module.kotlin.readValue
import com.jcraft.jsch.ChannelSftp
import com.nhaarman.mockitokotlin2.reset
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.timeout
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.jackson.jackson
import io.ktor.response.respond
import io.ktor.routing.post
import io.ktor.routing.routing
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import no.difi.begrep.sdp.schema_v10.SDPKvittering
import no.difi.begrep.sdp.schema_v10.SDPMelding
import no.digipost.api.representations.EbmsOutgoingMessage
import no.nav.kanal.ArchiveResponse
import no.nav.kanal.ConnectionPool
import no.nav.kanal.LegalArchiveLogger
import no.nav.kanal.config.SdpConfiguration
import no.nav.kanal.config.SdpKeys
import no.nav.kanal.config.VaultCredentials
import no.nav.kanal.createCamelContext
import no.nav.kanal.objectMapper
import org.amshove.kluent.any
import org.amshove.kluent.mock
import org.amshove.kluent.shouldBeGreaterOrEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.amshove.kluent.shouldContain
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
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantReadWriteLock
import javax.jms.BytesMessage
import javax.jms.ConnectionFactory
import javax.jms.TextMessage
import javax.naming.InitialContext
import javax.xml.bind.JAXBContext
import kotlin.concurrent.write

fun randomPort() = ServerSocket(0).use {
    it.localPort
}
val sdpMockPort: Int = randomPort()
val legalArchiveMockPort = randomPort()

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
        if (signalMessage.pullRequest != null && receipts.containsKey(signalMessage.pullRequest.mpc)) {
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

    val requestHandler = spy(QueuedReceiptHandler())
    fun initSdpServer() = createSDPMockServer(sbdHandler = requestHandler, port = sdpMockPort, keyStore = keyStore)
    var sdpServer = initSdpServer()

    val vaultCredentials: VaultCredentials = objectMapper.readValue(VaultCredentials::class.java.getResourceAsStream("/vault.json"))
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
            keystorePath = "build/keystore.p12.b64",
            truststorePath = "build/keystore.p12.b64",
            mqConcurrentConsumers = 4,
            receiptPollIntervalNormal = 1000,
            legalArchiveUrl = "http://localhost:$legalArchiveMockPort/upload"
    )

    val requestMock = mock<() -> Any>()

    // TODO: Use mock engine whenever it supports JSON
    val legalArchiveMock = embeddedServer(CIO, port = legalArchiveMockPort) {
        install(ContentNegotiation) {
            jackson {  }
        }
        routing {
            post("/upload") {
                call.respond(requestMock())
            }
        }
    }.start()

    val legalArchiveLogger = LegalArchiveLogger(config.legalArchiveUrl, "user", "pass")

    val sdpKeys = SdpKeys(config.keystorePath, config.truststorePath, vaultCredentials)

    val connectionFactory = InitialContext().lookup("ConnectionFactory") as ConnectionFactory
    val queueConnection = connectionFactory.createConnection()
    queueConnection.start()

    val connectionPool = ConnectionPool({ mock(ChannelSftp::class) }, {})
    val camelContext = createCamelContext(config, sdpKeys, connectionFactory, connectionPool, legalArchiveLogger)
    camelContext.start()

    val session = queueConnection.createSession()

    val normalQueueSender = session.createProducer(session.createQueue(config.inputQueueNormal))
    val normalQueueConsumer = session.createConsumer(session.createQueue(config.inputQueueNormal))
    val priorityQueueSender = session.createProducer(session.createQueue(config.inputQueuePriority))
    val normalReceiptConsumer = session.createConsumer(session.createQueue(config.receiptQueueNormal))
    val priorityReceiptConsumer = session.createConsumer(session.createQueue(config.receiptQueuePriority))
    val normalBackoutConsumer = session.createConsumer(session.createQueue(config.inputQueueNormalBackout))
    val priorityBackoutConsumer = session.createConsumer(session.createQueue(config.inputQueuePriorityBackout))

    val messageBytes = IOUtils.toByteArray(SdpKanalITSpek::class.java.getResourceAsStream("/payloads/inline.xml"))

    fun shutdownServer() {
        sdpServer.server.stop()
        sdpServer.server.destroy()
        sdpServer.sfb.bus.shutdown(true)
    }

    afterEachTest {
        reset(requestHandler, requestMock)
        whenever(requestMock()).thenReturn(ArchiveResponse(id = 0))
    }

    afterGroup {
        legalArchiveMock.stop(10, 10, TimeUnit.SECONDS)
        camelContext.shutdown()
        queueConnection.close()
        shutdownServer()
        activeMQServer.stop(true)
        println("SHUTDOWN")
    }

    // TODO: I want something to help figure out if the route was completed successfully
    /*fun whenBodiesDone(content: String) = NotifyBuilder(camelContext)
            .fromRoute(SEND_NORMAL_ROUTE_NAME)
            .whenBodiesDone(content)
            .or()
            .fromRoute(SEND_PRIORITY_ROUTE_NAME)
            .whenBodiesDone(content)
            .create()*/

    fun genPayload(conversationId: String = UUID.randomUUID().toString()): String =
            messageBytes.toString(Charsets.UTF_8).replace("CONV_ID", conversationId)

    describe("Sending messages on the input queue") {
        listOf(normalQueueSender, priorityQueueSender).forEach {
            it("Results in the message dispatcher receiving the message from input queue ${it.destination}") {
                val payload = genPayload()
                //val notify = whenBodiesDone(payload)
                it.send(session.createTextMessage(payload))
                verify(requestHandler, timeout(20000).times(1)).handleUserMessage(any(), any(), any(), any())

                //notify.matches(10000, TimeUnit.MILLISECONDS) shouldEqual true
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
                val payload = genPayload()
                //val notify = whenBodiesDone(payload)
                inputQueue.send(session.createTextMessage(payload))
                requestHandler.userMessageHandlers.add {
                    throw RuntimeException("Integration test")
                }

                val message = backoutQueue.receive(10000)
                message shouldBeInstanceOf TextMessage::class
                message as TextMessage
                message.text shouldEqual payload
                //notify.matches(10000, TimeUnit.MILLISECONDS) shouldEqual true
            }
        }
    }

    describe("In-flight messages") {
        it("In-flight messages get sent to BOQ whenever they can't reach the message dispatcher") {
            shutdownServer()

            val numberOfMessages = 100

            val inputMessages = 0.until(numberOfMessages).map { genPayload() }
            inputMessages.forEach { normalQueueSender.send(session.createTextMessage(it)) }

            // Let the route run for a second to make sure its in a transaction
            Thread.sleep(1000)

            camelContext.shutdown()

            val backoutMessages = 0.rangeTo(numberOfMessages*2)
                    .map { normalBackoutConsumer.receiveNoWait() }
                    .filterNotNull()
                    .filter { it is TextMessage }

            val inputQueueMessages = 0.rangeTo(numberOfMessages*2)
                    .map { normalQueueConsumer.receiveNoWait() }
                    .filterNotNull()
                    .filter { it is TextMessage }

            val messagesLeft = listOf(backoutMessages, inputQueueMessages).flatten()
                    .map { it as TextMessage }
                    .map { it.text }

            println("Done getting messages from queues, results: backout=${backoutMessages.size}, input=${inputQueueMessages.size}")
            backoutMessages.size shouldBeGreaterOrEqualTo config.mqConcurrentConsumers
            messagesLeft.size shouldBeGreaterOrEqualTo numberOfMessages
            inputMessages.forEach { messagesLeft shouldContain it }

            camelContext.start()

            sdpServer = initSdpServer()
        }
    }

    describe("Legal Archive logging") {
        it("Message should go through even regardless of message being logged to legal archive") {

        }
    }
})
