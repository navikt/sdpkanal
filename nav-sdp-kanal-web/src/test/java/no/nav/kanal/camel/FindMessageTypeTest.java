package no.nav.kanal.camel;

import java.io.File;

import no.nav.kanal.KanalConstants;

import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.spring.CamelSpringJUnit4ClassRunner;
import org.apache.camel.test.spring.MockEndpoints;
import org.hamcrest.core.IsInstanceOf;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;

@RunWith(CamelSpringJUnit4ClassRunner.class)
@ContextConfiguration({ "classpath:applicationContext-FindMessageType.xml" })
@MockEndpoints("log:*")
public class FindMessageTypeTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @EndpointInject(uri = "direct:start")
    ProducerTemplate producer;
    @EndpointInject(uri = "mock:result")
    MockEndpoint result;

    @Test
    public void testFindMessageType() throws InterruptedException {
	
    	result.expectedMessageCount(2);
        producer.sendBody(new File("src/test/resources/sdpRequest-FindMessageType_digitalpost.xml"));
        producer.sendBody(new File("src/test/resources/sdpRequest-FindMessageType_fysiskpost.xml"));
        result.assertIsSatisfied();
        Exchange ex = result.assertExchangeReceived(0);
        Assert.assertEquals("Check if camel-header has 'type=digital' when message is digitalpost", ex.getIn().getHeader(KanalConstants.CAMEL_HEADER_TYPE), 
        		KanalConstants.CAMEL_HEADER_TYPE_DIGITAL_POST);
        
        Exchange ex2 = result.assertExchangeReceived(1);
        Assert.assertEquals("Check if camel-header has 'type=fysisk' when message is fysisk post", ex2.getIn().getHeader(KanalConstants.CAMEL_HEADER_TYPE), 
        		KanalConstants.CAMEL_HEADER_TYPE_FYSISK_POST);
    }
    
    @Test
    public void testFindMessageTypeNoMessageId() throws InterruptedException {
	
    	thrown.expect(org.apache.camel.CamelExecutionException.class);
		thrown.expectCause(IsInstanceOf.<Throwable>instanceOf(RuntimeCamelException.class));
		
    	result.expectedMessageCount(1);
        producer.sendBody(new File("src/test/resources/sdpRequest-FindMessageType_digitalpost_no_messageid.xml"));
        result.assertIsSatisfied();
        
    }
    
    @Test
    public void testFindMessageTypeEmptyMessageId() throws InterruptedException {
	
    	thrown.expect(org.apache.camel.CamelExecutionException.class);
		thrown.expectCause(IsInstanceOf.<Throwable>instanceOf(RuntimeCamelException.class));
		
    	result.expectedMessageCount(1);
        producer.sendBody(new File("src/test/resources/sdpRequest-FindMessageType_digitalpost_empty_messageid.xml"));
        result.assertIsSatisfied();
        
    }
}
