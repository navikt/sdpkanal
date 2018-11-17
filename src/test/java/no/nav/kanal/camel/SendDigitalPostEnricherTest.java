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
@ContextConfiguration({"src/test/resources/applicationContext-sendDigitalPostEnricher.xml"})
@MockEndpoints("log:*")
public class SendDigitalPostEnricherTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @EndpointInject(uri = "direct:start")
    ProducerTemplate producer;
    @EndpointInject(uri = "mock:result")
    MockEndpoint result;

    //@Test
    //public void testSendDigitalPostEnricher() throws InterruptedException {
    //    result.expectedMessageCount(1);
    //    producer.sendBody(new File("src/test/resources/sdpRequest-test.xml"));
    //    result.assertIsSatisfied();
    //    Exchange ex = result.assertExchangeReceived(0);
    //    Assert.assertNotNull("Temp directory should not be null", ex.getIn().getHeader(KanalConstants.CAMEL_HEADER_TEMP_DIRECTORY));
    //    String tempPath = (String) ex.getIn().getHeader(KanalConstants.CAMEL_HEADER_TEMP_DIRECTORY);
    //    SendDigitalPost melding = (SendDigitalPost) ex.getIn().getBody();
    //    Assert.assertEquals("Check if hoveddokument is set", melding.getSendDigitalPostRequest().getManifest().getHoveddokument().getHref(), "S140744-72169056-210214-1848-268.pdf");
    //    Assert.assertEquals("The test message should have 3 vedlegg", melding.getSendDigitalPostRequest().getManifest().getVedlegg().size(), 3);
    //    Assert.assertEquals("Href should be updated", melding.getSendDigitalPostRequest().getManifest().getVedlegg().get(0).getHref(), "viktigePassord.txt");
    //    File mainfest = new File(tempPath + "manifest.xml");
    //    Assert.assertTrue("Manifest.xml should exist", mainfest.exists());
    //    Assert.assertTrue("Manifest.xml should not be empty", mainfest.length() > 0);
    //}
    
    @Test
    public void testSendDigitalPostEnricherMissingFile() throws InterruptedException {
    	thrown.expect(org.apache.camel.CamelExecutionException.class);
		thrown.expectCause(IsInstanceOf.<Throwable>instanceOf(RuntimeCamelException.class));
    	result.expectedMessageCount(1);
        producer.sendBody(new File("src/test/resources/sdpRequest-test-nonexisting-files.xml"));
        result.assertIsSatisfied();
    }
}
