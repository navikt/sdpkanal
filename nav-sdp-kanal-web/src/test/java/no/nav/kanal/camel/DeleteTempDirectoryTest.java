package no.nav.kanal.camel;

import java.io.File;

import no.nav.kanal.KanalConstants;

import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.spring.CamelSpringJUnit4ClassRunner;
import org.apache.camel.test.spring.MockEndpoints;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;

@RunWith(CamelSpringJUnit4ClassRunner.class)
@ContextConfiguration({ "classpath:applicationContext-deleteTempDirectory.xml" })
@MockEndpoints("log:*")
public class DeleteTempDirectoryTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @EndpointInject(uri = "direct:start")
    ProducerTemplate producer;
    @EndpointInject(uri = "mock:result")
    MockEndpoint result;

    @Test
    public void testSendDigitalPostEnricher() throws InterruptedException {
        result.expectedMessageCount(1);
        producer.sendBody(new File("src/test/resources/sdpRequest-test.xml"));
        result.assertIsSatisfied();
        Exchange ex = result.assertExchangeReceived(0);
        Assert.assertNull("Temp directory should be null", ex.getIn().getHeader(KanalConstants.CAMEL_HEADER_TEMP_DIRECTORY));
        
    }
}
