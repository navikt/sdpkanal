package no.nav.kanal.config;

import org.apache.camel.component.jms.JmsConfiguration;
import org.apache.camel.component.jms.JmsEndpoint;
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
    public MqQueueConfiguration(JmsConfiguration jmsConfiguration, ConnectionFactory connectionFactory) throws JMSException {
        this.jmsConfiguration = jmsConfiguration;
        this.session = connectionFactory.createConnection().createSession(false, Session.AUTO_ACKNOWLEDGE);
    }

    @Bean
    public JmsEndpoint inputQueue(PlatformTransactionManager transactionManager) throws JMSException {
        JmsEndpoint endpoint = JmsEndpoint.newInstance(session.createQueue("DEV.QUEUE.INPUT"));
        endpoint.setTransactionManager(transactionManager);
        endpoint.setConfiguration(jmsConfiguration);
        return endpoint;
    }

    @Bean
    public JmsEndpoint inputQueuePriority(PlatformTransactionManager transactionManager) throws JMSException {
        JmsEndpoint endpoint = JmsEndpoint.newInstance(session.createQueue("DEV.QUEUE.INPUT_PRIORITY"));
        endpoint.setTransactionManager(transactionManager);
        endpoint.setConfiguration(jmsConfiguration);
        return endpoint;
    }

    @Bean
    public JmsEndpoint inputBackoutQueue(PlatformTransactionManager transactionManager) throws JMSException {
        JmsEndpoint endpoint = JmsEndpoint.newInstance(session.createQueue("DEV.DEAD.LETTER.QUEUE"));
        endpoint.setTransactionManager(transactionManager);
        endpoint.setConfiguration(jmsConfiguration);
        return endpoint;
    }

    @Bean
    public JmsEndpoint inputPriorityBackoutQueue(PlatformTransactionManager transactionManager) throws JMSException {
        JmsEndpoint endpoint = JmsEndpoint.newInstance(session.createQueue("DEV.DEAD.LETTER.QUEUE"));
        endpoint.setTransactionManager(transactionManager);
        endpoint.setConfiguration(jmsConfiguration);
        return endpoint;
    }

    @Bean
    public JmsEndpoint receiptQueue(PlatformTransactionManager transactionManager) throws JMSException {
        JmsEndpoint endpoint = JmsEndpoint.newInstance(session.createQueue("DEV.QUEUE.RECEIPT"));
        endpoint.setTransactionManager(transactionManager);
        endpoint.setConfiguration(jmsConfiguration);
        return endpoint;
    }

    @Bean
    public JmsEndpoint receiptPriorityQueue(PlatformTransactionManager transactionManager) throws JMSException {
        JmsEndpoint endpoint = JmsEndpoint.newInstance(session.createQueue("DEV.QUEUE.RECEIPT_PRIORITY"));
        endpoint.setTransactionManager(transactionManager);
        endpoint.setConfiguration(jmsConfiguration);
        return endpoint;
    }

    @Bean
    public JmsEndpoint receiptBackoutQueue(PlatformTransactionManager transactionManager) throws JMSException {
        JmsEndpoint endpoint = JmsEndpoint.newInstance(session.createQueue("DEV.DEAD.LETTER.QUEUE"));
        endpoint.setTransactionManager(transactionManager);
        endpoint.setConfiguration(jmsConfiguration);
        return endpoint;
    }

    @Bean
    public JmsEndpoint receiptPriorityBackoutQueue(PlatformTransactionManager transactionManager) throws JMSException {
        JmsEndpoint endpoint = JmsEndpoint.newInstance(session.createQueue("DEV.DEAD.LETTER.QUEUE"));
        endpoint.setTransactionManager(transactionManager);
        endpoint.setConfiguration(jmsConfiguration);
        return endpoint;
    }
}
