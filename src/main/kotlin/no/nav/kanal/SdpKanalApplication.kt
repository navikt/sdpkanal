package no.nav.kanal

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.jcraft.jsch.ChannelSftp
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
import no.digipost.api.EbmsEndpointUriBuilder
import no.digipost.api.representations.EbmsAktoer
import no.digipost.api.representations.EbmsOutgoingMessage
import no.nav.kanal.camel.BackoutReason
import no.nav.kanal.camel.DocumentPackageCreator
import no.nav.kanal.camel.EbmsPull
import no.nav.kanal.camel.EbmsPush
import no.nav.kanal.config.SdpConfiguration
import no.nav.kanal.config.SdpKeys
import no.nav.kanal.config.VaultCredentials
import no.nav.kanal.config.createConnectionFactory
import no.nav.kanal.config.createJmsConfig
import no.nav.kanal.ebms.EbmsSender
import no.nav.kanal.route.createDeadLetterRoute
import no.nav.kanal.route.createReceiptPollingRoute
import no.nav.kanal.route.createSendRoute
import org.apache.camel.component.jms.JmsEndpoint
import org.apache.camel.impl.DefaultCamelContext
import org.apache.camel.impl.DefaultShutdownStrategy
import java.io.File
import java.net.URI
import java.nio.file.Files
import java.nio.file.Paths
import java.util.Base64
import java.util.concurrent.TimeUnit
import javax.jms.ConnectionFactory
import javax.jms.Session

const val MPC_ID_HEADER: String = "MPC_ID"
const val PRIORITY_HEADER: String = "PRIORITY"

const val METRICS_NAMESPACE = "sdpkanal"
const val SEND_NORMAL_ROUTE_NAME = "sendSDPNormal"
const val SEND_PRIORITY_ROUTE_NAME = "sendSDPPriority"
val objectMapper: ObjectMapper = ObjectMapper().registerKotlinModule()

val datahandler: EbmsAktoer = EbmsAktoer.avsender("889640782")
val receiver: EbmsAktoer = EbmsAktoer.meldingsformidler("984661185")

fun createCamelContext(
        config: SdpConfiguration,
        sdpKeys: SdpKeys,
        mqConnection: ConnectionFactory,
        sftpConnectionPool: ConnectionPool<ChannelSftp>,
        legalArchiveLogger: LegalArchiveLogger
): DefaultCamelContext = DefaultCamelContext().apply {
    val jmsConfig = createJmsConfig(mqConnection, config.mqConcurrentConsumers)
    val session = mqConnection.createConnection().apply {
        start()
    }.createSession(false, Session.AUTO_ACKNOWLEDGE)

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

    val ebmsSender = EbmsSender(EbmsEndpointUriBuilder.statiskUri(URI(config.ebmsEndpointUrl)), sdpKeys, receiver, legalArchiveLogger)
    val ebmsPull = EbmsPull(receiver, ebmsSender)
    val ebmsPush = EbmsPush(config.maxRetries, config.retryIntervalInSeconds, datahandler, receiver, ebmsSender)
    val backoutReason = BackoutReason()
    val documentPackageCreator = DocumentPackageCreator(sdpKeys, sftpConnectionPool, config.documentDirectory)

    shutdownStrategy = DefaultShutdownStrategy().apply { timeout  = config.shutdownTimeout }
    disableJMX()
    addRoutes(createReceiptPollingRoute("pullReceiptsPriority", config.mpcPrioritert, EbmsOutgoingMessage.Prioritet.NORMAL, config.receiptPollIntervalNormal, ebmsPull, receiptQueueNormal))
    addRoutes(createReceiptPollingRoute("pullReceiptsNormal", config.mpcNormal, EbmsOutgoingMessage.Prioritet.PRIORITERT, config.receiptPollIntervalNormal, ebmsPull, receiptQueuePriority))

    addRoutes(createDeadLetterRoute("backoutMessageNormal", inputQueueNormalBackout, backoutReason))
    addRoutes(createDeadLetterRoute("backoutMessagePriority", inputQueuePriorityBackout, backoutReason))

    addRoutes(createSendRoute(SEND_NORMAL_ROUTE_NAME, config.mpcNormal, EbmsOutgoingMessage.Prioritet.NORMAL, inputQueueNormal, "backoutMessageNormal", documentPackageCreator, ebmsPush))
    addRoutes(createSendRoute(SEND_PRIORITY_ROUTE_NAME, config.mpcPrioritert, EbmsOutgoingMessage.Prioritet.PRIORITERT, inputQueuePriority, "backoutMessagePriority", documentPackageCreator, ebmsPush))
}

fun main(args: Array<String>) {
    Base64.getDecoder().wrap(Files.newInputStream(Paths.get("/var/run/secrets/nais.io/vault/srvsdpkanal.jks.b64"))).use {
        Files.copy(it, Paths.get("/tmp/srvsdpkanal.jks"))
    }


    val config = SdpConfiguration()
    val vaultCredentials: VaultCredentials = objectMapper.readValue(File(config.credentialsPath))

    System.setProperty("javax.xml.soap.SAAJMetaFactory", "com.sun.xml.messaging.saaj.soap.SAAJMetaFactoryImpl")
    System.setProperty("javax.net.ssl.keyStore", "/tmp/srvsdpkanal.jks")
    System.setProperty("javax.net.ssl.keyStoreType", "jks")
    System.setProperty("javax.net.ssl.keyStorePassword", vaultCredentials.applicationCertificatePassword)

    val sdpKeys = SdpKeys(config.keystorePath, config.truststorePath, vaultCredentials)

    val mqConnection = createConnectionFactory(config.mqHostname, config.mqPort, config.mqQueueManager, config.mqChannel, vaultCredentials)

    val jschSession = JSch().apply {
        addIdentity(config.sftpKeyPath, vaultCredentials.sftpKeyPassword)
        setKnownHosts(config.knownHostsFile)
    }.getSession(vaultCredentials.sftpUsername, config.sftpUrl)
    jschSession.connect()
    val sftpConnectionPool = ConnectionPool({
        (jschSession.openChannel("sftp") as ChannelSftp).apply {
            connect()
        }
    }, { it.disconnect() })
    val legalArchiveLogger = LegalArchiveLogger(config.legalArchiveUrl, vaultCredentials.serviceuserUsername, vaultCredentials.serviceuserPassword)

    val camelContext = createCamelContext(config, sdpKeys, mqConnection, sftpConnectionPool, legalArchiveLogger)
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
