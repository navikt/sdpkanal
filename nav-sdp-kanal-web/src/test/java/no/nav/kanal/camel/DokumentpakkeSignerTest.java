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
@ContextConfiguration({ "classpath:applicationContext-dokumentpakkeSigner.xml" })
@MockEndpoints("log:*")
public class DokumentpakkeSignerTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @EndpointInject(uri = "direct:start")
    ProducerTemplate producer;
    @EndpointInject(uri = "mock:result")
    MockEndpoint result;

    
    @Test
    public void testDokumentpakkeSigner() throws InterruptedException {
        result.expectedMessageCount(1);
        producer.sendBody(new File("src/test/resources/sdpRequest-test.xml"));
        result.assertIsSatisfied();
        Exchange ex = result.assertExchangeReceived(0);
        Assert.assertNotNull("Temp directory should not be null", ex.getIn().getHeader(KanalConstants.CAMEL_HEADER_TEMP_DIRECTORY));
        String tempPath = (String) ex.getIn().getHeader(KanalConstants.CAMEL_HEADER_TEMP_DIRECTORY);
        File asice = new File(tempPath + "post.asice");
        Assert.assertTrue("post.asice should exist", asice.exists());
        Assert.assertTrue("post.asice should not be empty", asice.length() > 0);
    }
}
