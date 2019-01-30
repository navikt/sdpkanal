package no.nav.kanal.config

import com.natpryce.konfig.Configuration
import com.natpryce.konfig.ConfigurationProperties
import com.natpryce.konfig.ConfigurationProperties.Companion.systemProperties
import com.natpryce.konfig.EnvironmentVariables
import com.natpryce.konfig.Key
import com.natpryce.konfig.Misconfiguration
import com.natpryce.konfig.intType
import com.natpryce.konfig.longType
import com.natpryce.konfig.overriding
import com.natpryce.konfig.stringType
import java.lang.RuntimeException
import java.util.Properties

val config = systemProperties() overriding
        EnvironmentVariables() overriding
        ConfigurationProperties.fromResourceAsStream("/application.properties")

private fun ConfigurationProperties.Companion.fromResourceAsStream(resourceName: String): ConfigurationProperties {
    val input = SdpConfiguration::class.java.getResourceAsStream(resourceName)
    return (input ?: throw Misconfiguration("resource $resourceName not found")).use {
        ConfigurationProperties(Properties().apply { load(input) })
    }
}

data class SdpConfiguration(
        val ebmsEndpointUrl: String = config["ebms.msh.url"],
        val credentialsPath: String = config["no.nav.sdpkanal.credentialsPath"],
        val keystorePath: String = config["no.nav.sdpkanal.keystore"],
        val keystoreCredentialsPath: String = config["no.nav.sdpkanal.keystore.credentials"],
        val truststorePath: String = config["no.nav.sdpkanal.truststore"],
        val mqHostname: String = config["mqgateway04.hostname"],
        val mqPort: Int = config["mqgateway04.port"],
        val mqQueueManager: String = config["mqgateway04.name"],
        val mqChannel: String = config["sdpkanal.channel.name"],
        val mqConcurrentConsumers: Int = config["no.nav.sdpkanal.mq.concurrentConsumers"],
        val inputQueueNormal: String = config["sdp.send.standard.queuename"],
        val inputQueuePriority: String = config["sdp.send.prioritert.queuename"],
        val inputQueueNormalBackout: String = config["sdp.send.standard.boq.queuename"],
        val inputQueuePriorityBackout: String = config["sdp.send.prioritert.boq.queuename"],
        val receiptQueueNormal: String = config["sdp.kvittering.standard.queuename"],
        val receiptQueuePriority: String = config["sdp.kvittering.prioritert.queuename"],
        val legalArchiveUrl: String = config["no.nav.legalarchive.url"],
        val receiptPollIntervalNormal: Long = config["ebms.pullinterval.normal"],
        val receiptPollIntervalPriority: Long = config["ebms.pullinterval.normal"],
        val mpcNormal: String = config["ebms.mpc.normal"],
        val mpcPrioritert: String = config["ebms.mpc.prioritert"],
        val maxRetries: Long = config["ebms.push.maxRetries"],
        val retryIntervalInSeconds: Long = config["ebms.push.retryInterval"],
        val documentDirectory: String = config["no.nav.sdpkanal.dokument.path.prefix"],
        val sftpUrl: String = config["no.nav.sdpkanal.sftp.url"],
        val sftpKeyPath: String = config["no.nav.sdpkanal.sftp.key.path"],
        val knownHostsFile: String = config["no.nav.sdpkanal.sftp.known.hosts"],
        val shutdownTimeout: Long = config["no.nav.sdpkanal.shutdown.timeout"]
)

inline operator fun <reified T> Configuration.get(key: String): T = when (T::class) {
    String::class -> this[Key(key, stringType)].replace("\${user.home}", System.getProperty("user.home")).replace("\${java.io.tmpdir}", System.getProperty("java.io.tmpdir")) as T
    Integer::class -> this[Key(key, intType)] as T
    Long::class -> this[Key(key, longType)] as T
    else -> throw RuntimeException("Unknown class type ${T::class}")
}
