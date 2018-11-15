package no.nav.kanal.camel;

import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.spring.CamelSpringJUnit4ClassRunner;
import org.apache.camel.test.spring.MockEndpoints;
import org.hamcrest.core.IsInstanceOf;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;

@RunWith(CamelSpringJUnit4ClassRunner.class)
@ContextConfiguration({"src/test/resources/applicationContext-boqLogger.xml"})
@DirtiesContext(classMode=ClassMode.AFTER_EACH_TEST_METHOD)
@MockEndpoints("log:*")
public class BOQLoggerTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @EndpointInject(uri = "direct:start")
    ProducerTemplate producer;
    @EndpointInject(uri = "mock:result")
    MockEndpoint result;

    @Test
    public void testBOQLoggerNoException() throws InterruptedException {
        
    	thrown.expect(org.apache.camel.CamelExecutionException.class);
		thrown.expectCause(IsInstanceOf.<Throwable>instanceOf(java.lang.NullPointerException.class));
		
		result.expectedMessageCount(1);
        producer.sendBodyAndProperty("dummy", Exchange.EXCEPTION_CAUGHT, null);
        result.assertIsSatisfied();
    }
    
    @Test
    public void testBOQLoggerWithException() throws InterruptedException {
        
    	// expect this error because Camel header with logid is not set
    	thrown.expect(org.apache.camel.CamelExecutionException.class);
		thrown.expectCause(IsInstanceOf.<Throwable>instanceOf(java.lang.RuntimeException.class));
		
		result.expectedMessageCount(1);
        producer.sendBodyAndProperty("dummy", Exchange.EXCEPTION_CAUGHT, new Exception("Error", new Exception("Cause")));
        result.assertIsSatisfied();
    }
}
