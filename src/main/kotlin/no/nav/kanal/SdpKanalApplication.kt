package no.nav.kanal

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.HostKey
import com.jcraft.jsch.JSch
import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.response.respondText
import io.ktor.response.respondTextWriter
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.exporter.common.TextFormat
import kotlinx.coroutines.runBlocking
import no.digipost.api.representations.EbmsAktoer
import no.nav.kanal.camel.BOQLogger
import no.nav.kanal.camel.BackoutReason
import no.nav.kanal.camel.DocumentPackageCreator
import no.nav.kanal.camel.EbmsPull
import no.nav.kanal.camel.XmlExtractor
import no.nav.kanal.camel.ebms.EbmsPush
import no.nav.kanal.config.SdpConfiguration
import no.nav.kanal.config.SdpKeys
import no.nav.kanal.config.VaultCredentials
import no.nav.kanal.config.createConnectionFactory
import no.nav.kanal.config.createJmsConfig
import no.nav.kanal.config.createMessageSender
import no.nav.kanal.log.LegalArchiveStub
import no.nav.kanal.route.createDeadLetterRoute
import no.nav.kanal.route.createReceiptPollingRoute
import no.nav.kanal.route.createSendRoute
import org.apache.camel.component.jms.JmsEndpoint
import org.apache.camel.impl.DefaultCamelContext
import org.apache.camel.impl.DefaultShutdownStrategy
import org.apache.commons.io.IOUtils
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Base64
import java.util.concurrent.TimeUnit
import javax.jms.Session


const val METRICS_NAMESPACE = "sdpkanal"
val objectMapper: ObjectMapper = ObjectMapper().registerKotlinModule()

fun main(args: Array<String>) {
    val config = SdpConfiguration()

    val datahandler = EbmsAktoer.avsender("889640782")
    val receiver = EbmsAktoer.meldingsformidler("984661185")

    val vaultCredentials: VaultCredentials = objectMapper.readValue(File(config.credentialsPath))

    val sdpKeys = SdpKeys(config.keystorePath, config.truststorePath, vaultCredentials)

    val mqConnection = createConnectionFactory(config.mqHostname, config.mqPort, config.mqQueueManager, config.mqChannel, vaultCredentials)
    val jmsConfig = createJmsConfig(mqConnection, config.mqConcurrentConsumers)

    val jschSession = JSch().apply {
        addIdentity(config.sftpKeyPath, vaultCredentials.sftpKeyPassword)
        setKnownHosts(config.knownHostsFile)
    }.getSession(vaultCredentials.sftpUsername, config.sftpUrl)
    jschSession.connect()
    val sftpChannel = jschSession.openChannel("sftp") as ChannelSftp
    sftpChannel.connect()

    val session = mqConnection.createConnection().createSession(false, Session.AUTO_ACKNOWLEDGE)

    val legalArhive = LegalArchiveStub()
    val messageSender = createMessageSender(datahandler, receiver, sdpKeys, vaultCredentials, config.ebmsEndpointUrl)
    val ebmsPull = EbmsPull(messageSender, receiver)
    val ebmsPush = EbmsPush(config.maxRetries, config.retryIntervalInSeconds, legalArhive, messageSender, datahandler, receiver)
    val boqLogger = BOQLogger()
    val backoutReason = BackoutReason()
    val xmlExtractor = XmlExtractor()
    val documentPackageCreator = DocumentPackageCreator(sdpKeys, sftpChannel, config.documentDirectory)

    val camelContext = DefaultCamelContext().apply {
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

    runBlocking {
        val ktorServer = embeddedServer(CIO, 8080) {
            routing {
                get("/is_alive") {
                    call.respondText("I'm alive")
                }

                get("/is_ready") {
                    call.respondText("I'm ready")
                }

                get("/prometheus") {
                    val names = call.request.queryParameters.getAll("name[]")?.toSet() ?: setOf()
                    call.respondTextWriter(ContentType.parse(TextFormat.CONTENT_TYPE_004)) {
                        TextFormat.write004(this, CollectorRegistry.defaultRegistry.filteredMetricFamilySamples(names))
                    }
                }
            }
        }

        Runtime.getRuntime().addShutdownHook(Thread {
            camelContext.stop()
            ktorServer.stop(10, 10, TimeUnit.SECONDS)
        })

        ktorServer.start(wait = true)
    }
}
