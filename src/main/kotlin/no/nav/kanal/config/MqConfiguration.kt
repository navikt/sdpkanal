package no.nav.kanal.config

import com.ibm.mq.jms.MQConnectionFactory
import com.ibm.msg.client.wmq.WMQConstants
import org.apache.camel.component.jms.JmsConfiguration
import org.springframework.jms.connection.UserCredentialsConnectionFactoryAdapter
import javax.jms.ConnectionFactory
import javax.net.ssl.SSLSocketFactory

fun createConnectionFactory(
        mqHost: String,
        mqPort: Int,
        mqQueueManager: String,
        channelName: String,
        credentials: VaultCredentials
): ConnectionFactory = UserCredentialsConnectionFactoryAdapter().apply {
        setTargetConnectionFactory(MQConnectionFactory().apply {
            hostName = mqHost
            port = mqPort
            queueManager = mqQueueManager
            transportType = WMQConstants.WMQ_CM_CLIENT
            channel = channelName
            sslSocketFactory = SSLSocketFactory.getDefault()
            sslCipherSuite = "TLS_RSA_WITH_AES_256_CBC_SHA"
        })
        setUsername(credentials.mqUsername)
        setPassword(credentials.mqPassword)
    }


fun createJmsConfig(
        connectionFactory: ConnectionFactory,
        concurrentConsumers: Int
): JmsConfiguration {
    val jmsConfig = JmsConfiguration()
    jmsConfig.connectionFactory = connectionFactory
    jmsConfig.isTransacted = true
    jmsConfig.transactionTimeout = 1800
    jmsConfig.concurrentConsumers = concurrentConsumers
    return jmsConfig
}
