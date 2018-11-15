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
@ContextConfiguration({ "classpath:applicationContext-SBDSigner.xml" })
@MockEndpoints("log:*")
public class SBDSignerTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @EndpointInject(uri = "direct:start")
    ProducerTemplate producer;
    @EndpointInject(uri = "mock:result")
    MockEndpoint result;

    @Test
    public void testSDPSignerMulipleMessages() throws InterruptedException {
        result.expectedMessageCount(2);
        producer.sendBody(new File("src/test/resources/sdpRequestSBDSigner-test.xml"));
        producer.sendBody(new File("src/test/resources/sdpRequestSBDSigner-test.xml"));
        result.assertIsSatisfied();
        Exchange ex = result.assertExchangeReceived(0);
        String ssbdPath = (String) ex.getIn().getHeader(KanalConstants.CAMEL_HEADER_STANDARD_BUSINESS_DOCUMENT);
        Assert.assertNotNull("Signed standard business document header should not be null", ssbdPath);
        File ssbd = new File(ssbdPath);
        Assert.assertTrue("Signed standard business document should exist", ssbd.exists());
        Assert.assertTrue("Signed standard business document should not be empty", ssbd.length() > 0);
        
        Exchange ex2 = result.assertExchangeReceived(1);
        String ssbdPath2 = (String) ex2.getIn().getHeader(KanalConstants.CAMEL_HEADER_STANDARD_BUSINESS_DOCUMENT);
        Assert.assertNotNull("Signed standard business document header should not be null", ssbdPath2);
        File ssbd2 = new File(ssbdPath);
        Assert.assertTrue("Signed standard business document should exist", ssbd2.exists());
        Assert.assertTrue("Signed standard business document should not be empty", ssbd2.length() > 0);
        
        Assert.assertNotEquals("Signed standard business document path should not be equal for two different messages", ssbdPath, ssbdPath2);
        
    }
}
