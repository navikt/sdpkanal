package no.nav.kanal.config;

import com.ibm.mq.jms.MQConnectionFactory;
import com.ibm.msg.client.wmq.WMQConstants;
import no.nav.kanal.config.model.VaultCredentials;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.connection.UserCredentialsConnectionFactoryAdapter;

import javax.jms.ConnectionFactory;
import javax.jms.JMSException;

@Configuration
public class MqConnectionConfiguration {
    @Bean
    public ConnectionFactory connectionFactory(
            @Value("${mqgateway04.hostname}") String mqHost,
            @Value("${mqgateway04.port}") String mqPort,
            @Value("${mqgateway04.name}") String mqQueueManager,
            @Value("${sdpkanal.channel.name}") String channelName,
            VaultCredentials credentials
    ) throws JMSException {
        MQConnectionFactory cf = new MQConnectionFactory();
        cf.setHostName(mqHost);
        cf.setPort(Integer.parseInt(mqPort));
        cf.setQueueManager(mqQueueManager);
        cf.setTransportType(WMQConstants.WMQ_CM_CLIENT);
        cf.setChannel(channelName);
        UserCredentialsConnectionFactoryAdapter f = new UserCredentialsConnectionFactoryAdapter();
        f.setTargetConnectionFactory(cf);
        f.setUsername(credentials.getMqUsername());
        f.setPassword(credentials.getMqPassword());
        return f;
    }
}
