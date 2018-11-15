package no.nav.kanal.camel.ebms;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.shutdownServer;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.camel.EndpointInject;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.model.language.SimpleExpression;
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

import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.junit.WireMockRule;

import no.nav.kanal.KanalConstants;

@RunWith(CamelSpringJUnit4ClassRunner.class)
@ContextConfiguration({ "classpath:applicationContext-flameebms-push.xml" })
@DirtiesContext(classMode=ClassMode.AFTER_EACH_TEST_METHOD)
@MockEndpoints("log:*")
public class FlameEbmsPushTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();
    @Rule
    public WireMockRule wireMockRule = new WireMockRule(wireMockConfig().port(16384));

    @EndpointInject(uri = "direct:start")
    ProducerTemplate producer;
    @EndpointInject(uri = "mock:result")
    MockEndpoint result;

    @Test
    public void testFlameEbmsPushResponseOk() throws InterruptedException, IOException {
    	stubFor(post(urlEqualTo("/push"))
    			.willReturn(aResponse()
    					.withStatus(200)
    					.withHeader("Content-Type", "application/soap+xml")
    					.withBodyFile("push-ack-ok.xml")));

		result.expectedHeaderReceived(KanalConstants.CAMEL_HEADER_DOKUMENTPAKKE, "src/test/resources/cms_dokumentpakke");
        result.expectedHeaderReceived(KanalConstants.CAMEL_HEADER_STANDARD_BUSINESS_DOCUMENT, "src/test/resources/sdpRequest-test.xml");
        result.expectedBodyReceived().body(String.class);
        // simple check to see of expected response was received
        result.expectedMessagesMatches(new SimpleExpression("${body} contains '<ns6:MessageId>f71051c4-08b9-4042-b908-b9a9b2c442e3</ns6:MessageId>'"));
        result.expectedMessagesMatches(new SimpleExpression("${body} contains '<ns7:NonRepudiationInformation>'"));
        
        Map<String,Object> headers = new HashMap<String,Object>();
        headers.put(KanalConstants.CAMEL_HEADER_DOKUMENTPAKKE, "src/test/resources/cms_dokumentpakke");
        headers.put(KanalConstants.CAMEL_HEADER_LEGAL_ARCHIVE_LOG_ID, "dummyLOGID");
        producer.sendBodyAndHeaders(new File("src/test/resources/sdpRequest-test.xml"),headers);
        
        result.assertIsSatisfied();
    }
    
    @Test
    public void testFlameEbmsPushResponseEbms004() throws InterruptedException {
    	stubFor(post(urlEqualTo("/push"))
    			.willReturn(aResponse()
    					.withStatus(200)
    					.withHeader("Content-Type", "application/soap+xml")
    					.withBodyFile("push-ack-ebms004.xml")));

    	thrown.expect(org.apache.camel.CamelExecutionException.class);
		thrown.expectCause(IsInstanceOf.<Throwable>instanceOf(RuntimeCamelException.class));

		result.expectedHeaderReceived(KanalConstants.CAMEL_HEADER_DOKUMENTPAKKE, "src/test/resources/cms_dokumentpakke");
        result.expectedHeaderReceived(KanalConstants.CAMEL_HEADER_STANDARD_BUSINESS_DOCUMENT, "src/test/resources/sdpRequest-test.xml");
        
        Map<String,Object> headers = new HashMap<String,Object>();
        headers.put(KanalConstants.CAMEL_HEADER_DOKUMENTPAKKE, "src/test/resources/cms_dokumentpakke");
        headers.put(KanalConstants.CAMEL_HEADER_LEGAL_ARCHIVE_LOG_ID, "dummyLOGID");
        producer.sendBodyAndHeaders(new File("src/test/resources/sdpRequest-test.xml"),headers);
        
        result.assertIsSatisfied();
    }
    
    @Test
    public void testFlameEbmsPushResponseEbms003Duplicate() throws InterruptedException {
    	stubFor(post(urlEqualTo("/push"))
    			.willReturn(aResponse()
    					.withStatus(200)
    					.withHeader("Content-Type", "application/soap+xml")
    					.withBodyFile("push-ack-ebms003duplicate.xml")));

        result.expectedBodyReceived().body(String.class);
        result.expectedHeaderReceived(KanalConstants.CAMEL_HEADER_DOKUMENTPAKKE, "src/test/resources/cms_dokumentpakke");
        result.expectedHeaderReceived(KanalConstants.CAMEL_HEADER_STANDARD_BUSINESS_DOCUMENT, "src/test/resources/sdpRequest-test.xml");

        // simple check to see of expected response was received
        result.expectedMessagesMatches(new SimpleExpression("${body} contains '<ns6:Description xml:lang=\"en\">Duplicate messageId</ns6:Description>'"));
        result.expectedMessagesMatches(new SimpleExpression("${body} contains '<env:Value>env:Sender</env:Value>'"));
        
        Map<String,Object> headers = new HashMap<String,Object>();
        headers.put(KanalConstants.CAMEL_HEADER_DOKUMENTPAKKE, "src/test/resources/cms_dokumentpakke");
        headers.put(KanalConstants.CAMEL_HEADER_LEGAL_ARCHIVE_LOG_ID, "dummyLOGID");
        producer.sendBodyAndHeaders(new File("src/test/resources/sdpRequest-test.xml"),headers);
        
        result.assertIsSatisfied();
    }
    
    @Test
    public void testFlameEbmsPushResponseEbmsMissingReceipt() throws InterruptedException {
    	stubFor(post(urlEqualTo("/push"))
    			.willReturn(aResponse()
    					.withStatus(200)
    					.withHeader("Content-Type", "application/soap+xml")
    					.withBodyFile("push-ack-missing-receipt.xml")));

    	thrown.expect(org.apache.camel.CamelExecutionException.class);

		result.expectedHeaderReceived(KanalConstants.CAMEL_HEADER_DOKUMENTPAKKE, "src/test/resources/cms_dokumentpakke");
        result.expectedHeaderReceived(KanalConstants.CAMEL_HEADER_STANDARD_BUSINESS_DOCUMENT, "src/test/resources/sdpRequest-test.xml");
        
        Map<String,Object> headers = new HashMap<String,Object>();
        headers.put(KanalConstants.CAMEL_HEADER_DOKUMENTPAKKE, "src/test/resources/cms_dokumentpakke");
        headers.put(KanalConstants.CAMEL_HEADER_LEGAL_ARCHIVE_LOG_ID, "dummyLOGID");
        producer.sendBodyAndHeaders(new File("src/test/resources/sdpRequest-test.xml"),headers);
        
        result.assertIsSatisfied();
    }
    
    @Test
    public void testFlameEbmsPushClientException() throws InterruptedException {
    	stubFor(post(urlEqualTo("/push"))
    			.willReturn(aResponse()
    					.withFault(Fault.RANDOM_DATA_THEN_CLOSE)));
    	
    	thrown.expect(org.apache.camel.CamelExecutionException.class);
		thrown.expectCause(IsInstanceOf.<Throwable>instanceOf(RuntimeCamelException.class));

		result.expectedHeaderReceived(KanalConstants.CAMEL_HEADER_DOKUMENTPAKKE, "src/test/resources/cms_dokumentpakke");
        result.expectedHeaderReceived(KanalConstants.CAMEL_HEADER_STANDARD_BUSINESS_DOCUMENT, "src/test/resources/sdpRequest-test.xml");
        
        Map<String,Object> headers = new HashMap<String,Object>();
        headers.put(KanalConstants.CAMEL_HEADER_DOKUMENTPAKKE, "src/test/resources/cms_dokumentpakke");
        headers.put(KanalConstants.CAMEL_HEADER_LEGAL_ARCHIVE_LOG_ID, "dummyLOGID");
        producer.sendBodyAndHeaders(new File("src/test/resources/sdpRequest-test.xml"),headers);
        
        result.assertIsSatisfied();
    }
    
    @Test
    public void testFlameEbmsPushConnectionException() throws InterruptedException {
    	shutdownServer();
    	
    	thrown.expect(org.apache.camel.CamelExecutionException.class);
		thrown.expectCause(IsInstanceOf.<Throwable>instanceOf(RuntimeCamelException.class));

		result.expectedHeaderReceived(KanalConstants.CAMEL_HEADER_DOKUMENTPAKKE, "src/test/resources/cms_dokumentpakke");
        result.expectedHeaderReceived(KanalConstants.CAMEL_HEADER_STANDARD_BUSINESS_DOCUMENT, "src/test/resources/sdpRequest-test.xml");
        
        Map<String,Object> headers = new HashMap<String,Object>();
        headers.put(KanalConstants.CAMEL_HEADER_DOKUMENTPAKKE, "src/test/resources/cms_dokumentpakke");
        headers.put(KanalConstants.CAMEL_HEADER_LEGAL_ARCHIVE_LOG_ID, "dummyLOGID");
        producer.sendBodyAndHeaders(new File("src/test/resources/sdpRequest-test.xml"),headers);
        
        result.assertIsSatisfied();
    }

}
