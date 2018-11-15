package no.nav.kanal.camel.ebms;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

import org.apache.camel.EndpointInject;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.model.language.SimpleExpression;
import org.apache.camel.test.spring.CamelSpringJUnit4ClassRunner;
import org.apache.camel.test.spring.MockEndpoints;
import org.hamcrest.core.IsInstanceOf;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.slf4j.MDC;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;

import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.junit.WireMockRule;

import no.nav.kanal.KanalConstants;

@RunWith(CamelSpringJUnit4ClassRunner.class)
@ContextConfiguration({ "classpath:applicationContext-flameebms-pull.xml" })
@DirtiesContext(classMode=ClassMode.AFTER_EACH_TEST_METHOD)
@MockEndpoints("log:*")
public class FlameEbmsPullTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();
    @Rule
    public WireMockRule wireMockRule = new WireMockRule(wireMockConfig().port(16384));

    @EndpointInject(uri = "direct:start")
    ProducerTemplate producer;
    @EndpointInject(uri = "mock:result")
    MockEndpoint result;

    
    @Test
    public void testFlameEbmsPullNormal() throws InterruptedException {
    	stubFor(post(urlEqualTo("/pull"))
    			.willReturn(aResponse()
    					.withStatus(200)
    					.withHeader("Content-Type", "application/soap+xml")
    					.withBodyFile("pull-response-ok.xml")));
    	
    	result.expectedHeaderReceived(KanalConstants.CAMEL_HEADER_MPC, KanalConstants.SDP_MPC_NORMAL);
    	result.expectedBodyReceived().body(String.class);
        // simple check to see of expected response was received
        result.expectedMessagesMatches(new SimpleExpression("${body} contains '<ns3:Type>kvittering</ns3:Type>'"));
        
        producer.sendBodyAndHeader("dummymessage", KanalConstants.CAMEL_HEADER_MPC, KanalConstants.SDP_MPC_NORMAL);
        Assert.assertTrue("Check that MDC is set from responsemessage", "SikkerDigitalPost/1.0".equals(MDC.get("callId")));
        result.assertIsSatisfied();        
    }
    
    @Test
    public void testFlameEbmsPullResponseEbms006() throws InterruptedException {
    	stubFor(post(urlEqualTo("/pull"))
    			.willReturn(aResponse()
    					.withStatus(200)
    					.withHeader("Content-Type", "application/soap+xml")
    					.withBodyFile("pull-response-ebms006.xml")));
    	
    	result.expectedHeaderReceived(KanalConstants.CAMEL_HEADER_MPC, KanalConstants.SDP_MPC_NORMAL);
    	result.expectedHeaderReceived(KanalConstants.CAMEL_HEADER_LOOP_INDICATOR_NORMAL, null);
    	result.expectedHeaderReceived(KanalConstants.CAMEL_HEADER_MESSAGE_TO_ACK, null);
    	result.expectedMessagesMatches(new SimpleExpression("${body} == null"));
        
        producer.sendBodyAndHeader("dummymessage", KanalConstants.CAMEL_HEADER_MPC, KanalConstants.SDP_MPC_NORMAL);
        result.assertIsSatisfied();        
    }
    
    @Test
    public void testFlameEbmsPullResponseEbmsUnknownErrorCode() throws InterruptedException {
    	stubFor(post(urlEqualTo("/pull"))
    			.willReturn(aResponse()
    					.withStatus(200)
    					.withHeader("Content-Type", "application/soap+xml")
    					.withBodyFile("pull-response-ebms-unknownerrorcode.xml")));
    	
    	result.expectedHeaderReceived(KanalConstants.CAMEL_HEADER_MPC, KanalConstants.SDP_MPC_NORMAL);
    	result.expectedHeaderReceived(KanalConstants.CAMEL_HEADER_LOOP_INDICATOR_NORMAL, null);
    	result.expectedHeaderReceived(KanalConstants.CAMEL_HEADER_MESSAGE_TO_ACK, null);
    	result.expectedMessagesMatches(new SimpleExpression("${body} == null"));
    	
        producer.sendBodyAndHeader("dummymessage", KanalConstants.CAMEL_HEADER_MPC, KanalConstants.SDP_MPC_NORMAL);
        result.assertIsSatisfied();        
    }
    
    @Test
    public void testFlameEbmsPullResponseEbmsSignalWithoutError() throws InterruptedException {
    	stubFor(post(urlEqualTo("/pull"))
    			.willReturn(aResponse()
    					.withStatus(200)
    					.withHeader("Content-Type", "application/soap+xml")
    					.withBodyFile("pull-response-ebms-signalwithouterror.xml")));
    	
    	result.expectedHeaderReceived(KanalConstants.CAMEL_HEADER_MPC, KanalConstants.SDP_MPC_NORMAL);
    	result.expectedHeaderReceived(KanalConstants.CAMEL_HEADER_LOOP_INDICATOR_NORMAL, null);
    	result.expectedHeaderReceived(KanalConstants.CAMEL_HEADER_MESSAGE_TO_ACK, null);
    	result.expectedMessagesMatches(new SimpleExpression("${body} == null"));
    	
        producer.sendBodyAndHeader("dummymessage", KanalConstants.CAMEL_HEADER_MPC, KanalConstants.SDP_MPC_NORMAL);
        result.assertIsSatisfied();        
    }
    
    @Test
    public void testFlameEbmsPullClientException() throws InterruptedException {
    	stubFor(post(urlEqualTo("/pull"))
    			.willReturn(aResponse()
    					.withFault(Fault.RANDOM_DATA_THEN_CLOSE)));
    	
    	thrown.expect(org.apache.camel.CamelExecutionException.class);
		thrown.expectCause(IsInstanceOf.<Throwable>instanceOf(RuntimeCamelException.class));
		
        result.expectedHeaderReceived(KanalConstants.CAMEL_HEADER_MPC, KanalConstants.SDP_MPC_PRIORITERT);
        producer.sendBodyAndHeader("dummymessage", KanalConstants.CAMEL_HEADER_MPC, KanalConstants.SDP_MPC_PRIORITERT);
        result.assertIsSatisfied();
    }
}
