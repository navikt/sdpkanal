package no.nav.kanal.config;

import org.apache.camel.component.jms.JmsConfiguration;
import org.apache.camel.component.jms.JmsEndpoint;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Session;

@Configuration
public class MqQueueConfiguration {
    private final JmsConfiguration jmsConfiguration;
    private final Session session;
    private final PlatformTransactionManager transactionManager;
    public MqQueueConfiguration(
            JmsConfiguration jmsConfiguration,
            ConnectionFactory connectionFactory,
            PlatformTransactionManager platformTransactionManager
    ) throws JMSException {
        this.jmsConfiguration = jmsConfiguration;
        this.session = connectionFactory.createConnection().createSession(false, Session.AUTO_ACKNOWLEDGE);
        this.transactionManager = platformTransactionManager;
    }

    @Bean
    public JmsEndpoint inputQueueNormal(@Value("${sdp.send.standard.queuename}") String queueName) throws JMSException {
        return createJmsEndpoint(queueName);
    }

    @Bean
    public JmsEndpoint inputQueuePriority(@Value("${sdp.send.prioritert.queuename}") String queueName) throws JMSException {
        return createJmsEndpoint(queueName);
    }

    @Bean
    public JmsEndpoint inputQueueNormalBackout(@Value("${sdp.send.standard.boq.queuename}") String queueName) throws JMSException {
        return createJmsEndpoint(queueName);
    }

    @Bean
    public JmsEndpoint inputQueuePriorityBackout(@Value("${sdp.send.prioritert.boq.queuename}") String queueName) throws JMSException {
        return createJmsEndpoint(queueName);
    }

    @Bean
    public JmsEndpoint receiptQueueNormal(@Value("${sdp.kvittering.standard.queuename}") String queueName) throws JMSException {
        return createJmsEndpoint(queueName);
    }

    @Bean
    public JmsEndpoint receiptQueuePriority(@Value("${sdp.kvittering.prioritert.queuename}") String queueName) throws JMSException {
        return createJmsEndpoint(queueName);
    }

    @Bean
    public JmsEndpoint receiptNormalBackoutQueue(@Value("${sdp.kvittering.standard.boq.queuename}") String queueName) throws JMSException {
        return createJmsEndpoint(queueName);
    }

    @Bean
    public JmsEndpoint receiptPriorityBackoutQueue(@Value("${sdp.kvittering.prioritert.boq.queuename}") String queueName) throws JMSException {
        return createJmsEndpoint(queueName);
    }

    private JmsEndpoint createJmsEndpoint(String queueName) throws JMSException {
        JmsEndpoint endpoint = JmsEndpoint.newInstance(session.createQueue(queueName));
        endpoint.setTransactionManager(transactionManager);
        endpoint.setConfiguration(jmsConfiguration);
        return endpoint;
    }
}
