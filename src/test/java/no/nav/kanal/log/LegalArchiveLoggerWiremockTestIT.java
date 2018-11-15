package no.nav.kanal.log;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;


import java.net.UnknownHostException;
import java.util.*;
import java.util.function.Supplier;

import javax.activation.DataHandler;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.soap.SOAPFaultException;


import org.apache.camel.*;
import org.apache.camel.spi.Synchronization;
import org.apache.camel.spi.UnitOfWork;
import org.hamcrest.core.IsInstanceOf;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.junit.WireMockRule;

import no.nav.kanal.KanalConstants;

public class LegalArchiveLoggerWiremockTestIT {
	
	@Rule
    public ExpectedException thrown = ExpectedException.none();
	@Rule
    public WireMockRule wireMockRule = new WireMockRule(wireMockConfig().port(8888));
	
	private String logCaller = "JBOSS: SDP-kanal";
	private String logAction = "urn:skatteetaten:fastsetting:innsamling:meldingsproduksjon:a-nav-inntekt-spoerring:soap:v1:hentInntektsmottakersInntekt";
	private String logServiceConsumer = "urn:sdp:avsender";
	private String logServiceProducer = "urn:sdp:meldingsformidler";
	private String logEndpoint = "http://localhost:8888/nav-emottak-archiver-web/services/archiver";
	private String logUsername = "srvsdpgw_u";
	private String logPassword = "dfwYokIJ83-suyq";
	private int maxRetries = 4;
	private int retryIntervalInSeconds = 1;
	
	@Test
	public void logEvent(){
		stubFor(post(urlEqualTo("/nav-emottak-archiver-web/services/archiver"))
    			.willReturn(aResponse()
    					.withStatus(200)
    					.withHeader("Content-Type", "text/xml")
    					.withBodyFile("archive_ok.xml")));
		
		LegalArchiveLogger lal = getLogger();
		
		Exchange ex = getMockExchange();
		
		lal.logFirstEvent("RANDOM" + Math.random(), ex, LogEvent.MELDING_HENTET_FRA_KO);
		
		Assert.assertNotNull(ex.getIn().getHeader(KanalConstants.CAMEL_HEADER_LEGAL_ARCHIVE_LOG_ID));
		
	}

	@Test
	public void logEventSoapFault(){
		stubFor(post(urlEqualTo("/nav-emottak-archiver-web/services/archiver"))
				.willReturn(aResponse()
						.withStatus(500)
						.withHeader("Content-Type", "text/xml")
						.withBodyFile("archive_soapfault.xml")));

		thrown.expect(RuntimeException.class);
		thrown.expectCause(IsInstanceOf.<Throwable>instanceOf(SOAPFaultException.class));

		LegalArchiveLogger lal = getLogger();

		Exchange ex = getMockExchange();

		lal.logFirstEvent("RANDOM" + Math.random(), ex, LogEvent.MELDING_HENTET_FRA_KO);
		Assert.assertNotNull(null);

	}

	@Test
	public void logEventWebServiceException(){
		stubFor(post(urlEqualTo("/nav-emottak-archiver-web/services/archiver"))
				.willReturn(aResponse()
						.withFault(Fault.MALFORMED_RESPONSE_CHUNK)
						.withStatus(500)));

		thrown.expect(RuntimeException.class);
		thrown.expectCause(IsInstanceOf.<Throwable>instanceOf(WebServiceException.class));

		LegalArchiveLogger lal = getLogger();

		Exchange ex = getMockExchange();

		lal.logFirstEvent("RANDOM" + Math.random(), ex, LogEvent.MELDING_HENTET_FRA_KO);

	}

	@Test
	public void logEventClientTransportException(){
		stubFor(post(urlEqualTo("/nav-emottak-archiver-web/services/archiver"))
				.willReturn(aResponse()
						.withFault(Fault.RANDOM_DATA_THEN_CLOSE)
						.withStatus(500)
						.withHeader("Content-Type", "text/xml")
						.withBodyFile("archive_soapfault.xml")));

		thrown.expect(RuntimeException.class);
		thrown.expectMessage("Could not log to legal archive: The server sent HTTP status code -1: null");


		LegalArchiveLogger lal = getLogger();

		Exchange ex = getMockExchange();

		lal.logFirstEvent("RANDOM" + Math.random(), ex, LogEvent.MELDING_HENTET_FRA_KO);

	}

	@Test
	public void logEventConnectException(){

		stubFor(post(urlEqualTo("/nav-emottak-archiver-web/services/archiver"))
				.willReturn(aResponse()
						.withStatus(500)));

		thrown.expect(RuntimeException.class);
		thrown.expectMessage("HTTP transport error: java.net.ConnectException: Connection refused: connect");

		LegalArchiveLogger lal = getLogger();
		Exchange ex = getMockExchange();

		wireMockRule.shutdown();
		lal.logFirstEvent("RANDOM" + Math.random(), ex, LogEvent.MELDING_HENTET_FRA_KO);
	}

	@Test
	public void logEventUnknownHostException(){
		stubFor(post(urlEqualTo("/nav-emottak-archiver-web/services/archiver"))
				.willReturn(aResponse()
						.withHeader("Content-Type", "text/xml")
						.withBodyFile("archive_soapfault.xml")));


		thrown.expect(RuntimeException.class);
		thrown.expectCause(IsInstanceOf.<Throwable>instanceOf(UnknownHostException.class));

		LegalArchiveLogger lal = getLogger();

		Exchange ex = getMockExchange();

		lal.logFirstEvent("RANDOM" + Math.random(), ex, LogEvent.MELDING_HENTET_FRA_KO);
	}
	@Test
	public void logEventSocketException(){
		stubFor(post(urlEqualTo("/nav-emottak-archiver-web/services/archiver"))
				.willReturn(aResponse()
						.withFault(Fault.EMPTY_RESPONSE)
						.withHeader("Content-Type", "text/xml")));

		thrown.expect(RuntimeException.class);
		thrown.expectMessage("java.net.SocketException: Unexpected end of file from server");

		LegalArchiveLogger lal = getLogger();

		Exchange ex = getMockExchange();

		lal.logFirstEvent("RANDOM" + Math.random(), ex, LogEvent.MELDING_HENTET_FRA_KO);
	}

	
	@Test
	public void logEventEmptyResponse(){
		stubFor(post(urlEqualTo("/nav-emottak-archiver-web/services/archiver"))
    			.willReturn(aResponse()
    					.withStatus(500)
    					.withHeader("Content-Type", "text/xml")
    					.withBodyFile("archive_empty.xml")));

		thrown.expect(RuntimeException.class);
		
		LegalArchiveLogger lal = getLogger();
		
		Exchange ex = getMockExchange();
		
		lal.logFirstEvent("RANDOM" + Math.random(), ex, LogEvent.MELDING_HENTET_FRA_KO);
	}
	
	@Test
	public void logEventEmptyResponse2(){
		stubFor(post(urlEqualTo("/nav-emottak-archiver-web/services/archiver"))
				.willReturn(aResponse()
    					.withFault(Fault.EMPTY_RESPONSE)));

		thrown.expect(RuntimeException.class);
		
		LegalArchiveLogger lal = getLogger();
		
		Exchange ex = getMockExchange();
		
		lal.logFirstEvent("RANDOM" + Math.random(), ex, LogEvent.MELDING_HENTET_FRA_KO);
	}
	
	@Test
	public void logEventMalformedResponse(){
		stubFor(post(urlEqualTo("/nav-emottak-archiver-web/services/archiver"))
				.willReturn(aResponse()
    					.withFault(Fault.MALFORMED_RESPONSE_CHUNK)));

		thrown.expect(RuntimeException.class);
		
		LegalArchiveLogger lal = getLogger();
		
		Exchange ex = getMockExchange();
		
		lal.logFirstEvent("RANDOM" + Math.random(), ex, LogEvent.MELDING_HENTET_FRA_KO);
	}
	
	@Test
	public void logEventMalformedResponseRandomData(){
		stubFor(post(urlEqualTo("/nav-emottak-archiver-web/services/archiver"))
				.willReturn(aResponse()
    					.withFault(Fault.RANDOM_DATA_THEN_CLOSE)));

		thrown.expect(RuntimeException.class);
		
		LegalArchiveLogger lal = getLogger();
		
		Exchange ex = getMockExchange();
		
		lal.logFirstEvent("RANDOM" + Math.random(), ex, LogEvent.MELDING_HENTET_FRA_KO);
	}
	
	private LegalArchiveLogger getLogger(){
		
		LegalArchiveLoggerImpl lal = new LegalArchiveLoggerImpl();
		lal.setLogAction(logAction);
		lal.setLogCaller(logCaller);
		lal.setLogEndpoint(logEndpoint);
		lal.setLogPassword(logPassword);
		lal.setLogServiceConsumer(logServiceConsumer);
		lal.setLogServiceProducer(logServiceProducer);
		lal.setLogUsername(logUsername);
		lal.setMaxRetries(maxRetries);
		lal.setRetryIntervalInSeconds(retryIntervalInSeconds);
		return lal;
		
	}
	

	private Exchange getMockExchange(){
		return new Exchange() {
			
			Message message = new Message() {
				
				HashMap<String,Object> map = new HashMap<String,Object>();
				
				@Override
				public void setMessageId(String messageId) {

					
				}
				
				@Override
				public void setHeaders(Map<String, Object> headers) {
					
					
				}
				
				@Override
				public void setHeader(String name, Object value) {
					
					map.put(name, value);
				}
				
				@Override
				public void setFault(boolean fault) {
					
					
				}
				
				@Override
				public <T> void setBody(Object body, Class<T> type) {
					
					
				}
				
				@Override
				public void setBody(Object body) {
					
					
				}
				
				@Override
				public void setAttachments(Map<String, DataHandler> attachments) {
					
					
				}

                @Override
                public void setAttachmentObjects(Map<String, Attachment> attachments) {

                }

                @Override
				public boolean removeHeaders(String pattern, String... excludePatterns) {
					
					return false;
				}
				
				@Override
				public boolean removeHeaders(String pattern) {
					
					return false;
				}
				
				@Override
				public Object removeHeader(String name) {
					
					return null;
				}
				
				@Override
				public void removeAttachment(String id) {
					
					
				}
				
				@Override
				public boolean isFault() {
					
					return false;
				}
				
				@Override
				public boolean hasHeaders() {
					
					return false;
				}
				
				@Override
				public boolean hasAttachments() {
					
					return false;
				}
				
				@Override
				public String getMessageId() {
					
					return null;
				}
				
				@Override
				public <T> T getMandatoryBody(Class<T> type) throws InvalidPayloadException {
					
					return null;
				}
				
				@Override
				public Object getMandatoryBody() throws InvalidPayloadException {
					
					return null;
				}
				
				@Override
				public Map<String, Object> getHeaders() {
					
					return null;
				}
				
				@Override
				public <T> T getHeader(String name, Object defaultValue, Class<T> type) {
					
					return null;
				}

                @Override
                public <T> T getHeader(String name, Supplier<Object> defaultValueSupplier, Class<T> type) {
                    return null;
                }

                @Override
				public <T> T getHeader(String name, Class<T> type) {
					
					return null;
				}
				
				@Override
				public Object getHeader(String name, Object defaultValue) {
					
					return null;
				}

                @Override
                public Object getHeader(String name, Supplier<Object> defaultValueSupplier) {
                    return null;
                }

                @Override
				public Object getHeader(String name) {
					return map.get(name);
				}
				
				@Override
				public Exchange getExchange() {
					
					return null;
				}
				
				@Override
				public <T> T getBody(Class<T> type) {
					
					return null;
				}
				
				@Override
				public Object getBody() {
					
					return null;
				}
				
				@Override
				public Map<String, DataHandler> getAttachments() {
					
					return null;
				}

                @Override
                public Map<String, Attachment> getAttachmentObjects() {
                    return null;
                }

                @Override
				public Set<String> getAttachmentNames() {
					
					return null;
				}
				
				@Override
				public DataHandler getAttachment(String id) {
					
					return null;
				}

                @Override
                public Attachment getAttachmentObject(String id) {
                    return null;
                }

                @Override
				@Deprecated
				public String createExchangeId() {
					
					return null;
				}
				
				@Override
				public void copyFrom(Message message) {
					
					
				}

                @Override
                public void copyFromWithNewBody(Message message, Object newBody) {

                }

                @Override
				public Message copy() {
					
					return null;
				}
				
				@Override
				public void addAttachment(String id, DataHandler content) {
					
					
				}

                @Override
                public void addAttachmentObject(String id, Attachment content) {

                }

                @Override
				public void copyAttachments(Message arg0) {
					
					
				}
			};
			
			@Override
			public void setUnitOfWork(UnitOfWork unitOfWork) {
				
				
			}
			
			@Override
			public void setProperty(String name, Object value) {
				
				
			}
			
			@Override
			public void setPattern(ExchangePattern pattern) {
				
				
			}
			
			@Override
			public void setOut(Message out) {
				
				
			}
			
			@Override
			public void setIn(Message in) {
				
				
			}
			
			@Override
			public void setFromRouteId(String fromRouteId) {
				
				
			}
			
			@Override
			public void setFromEndpoint(Endpoint fromEndpoint) {
				
				
			}
			
			@Override
			public void setExchangeId(String id) {
				
				
			}
			
			@Override
			public void setException(Throwable t) {
				
				
			}
			
			@Override
			public Object removeProperty(String name) {
				
				return null;
			}
			
			@Override
			public boolean isTransacted() {
				
				return false;
			}
			
			@Override
			public boolean isRollbackOnly() {
				
				return false;
			}
			
			@Override
			public boolean isFailed() {
				
				return false;
			}
			
			@Override
			public Boolean isExternalRedelivered() {
				
				return null;
			}
			
			@Override
			public boolean hasProperties() {
				
				return false;
			}
			
			@Override
			public boolean hasOut() {
				return false;
			}
			
			@Override
			public void handoverCompletions(Exchange target) {
			}
			
			@Override
			public List<Synchronization> handoverCompletions() {
				return null;
			}

            @Override
            public Date getCreated() {
                return null;
            }

            @Override
			public UnitOfWork getUnitOfWork() {
				return null;
			}
			
			@Override
			public <T> T getProperty(String name, Object defaultValue, Class<T> type) {
				
				return null;
			}
			
			@Override
			public <T> T getProperty(String name, Class<T> type) {
				return null;
			}
			
			@Override
			public Object getProperty(String name, Object defaultValue) {
				return null;
			}
			
			@Override
			public Object getProperty(String name) {
				return null;
			}
			
			@Override
			public Map<String, Object> getProperties() {
				return null;
			}
			
			@Override
			public ExchangePattern getPattern() {
				return null;
			}
			
			@Override
			public <T> T getOut(Class<T> type) {
				return null;
			}
			
			@Override
			public Message getOut() {
				return null;
			}
			
			@Override
			public <T> T getIn(Class<T> type) {
				return null;
			}
			
			@Override
			public Message getIn() {
				return message;
			}

            @Override
            public Message getMessage() {
                return null;
            }

            @Override
            public <T> T getMessage(Class<T> type) {
                return null;
            }

            @Override
            public void setMessage(Message message) {

            }

            @Override
			public String getFromRouteId() {
				return null;
			}
			
			@Override
			public Endpoint getFromEndpoint() {
				return null;
			}
			
			@Override
			public String getExchangeId() {
				return null;
			}
			
			@Override
			public <T> T getException(Class<T> type) {
				return null;
			}
			
			@Override
			public Exception getException() {
				
				return null;
			}
			
			@Override
			public CamelContext getContext() {
				return null;
			}
			
			@Override
			public Exchange copy() {
				return null;
			}
			
			@Override
			public boolean containsOnCompletion(Synchronization onCompletion) {
				return false;
			}
			
			@Override
			public void addOnCompletion(Synchronization onCompletion) {
			}

			@Override
			public boolean removeProperties(String pattern) {
				return false;
			}

			@Override
			public boolean removeProperties(String pattern,
					String... excludePatterns) {
				return false;
			}

			@Override
			public Exchange copy(boolean arg0) {
				return null;
			};
		};
	}
}
