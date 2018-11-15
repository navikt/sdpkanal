package no.nav.kanal.log;

import java.util.*;
import java.util.Properties;
import java.util.function.Supplier;

import javax.activation.DataHandler;

import org.apache.camel.*;
import org.apache.camel.spi.Synchronization;
import org.apache.camel.spi.UnitOfWork;
import org.junit.Assert;
import org.junit.Test;

import no.nav.kanal.KanalConstants;
import no.nav.kanal.log.LegalArchiveLogger;
import no.nav.kanal.log.LegalArchiveLoggerImpl;
import no.nav.kanal.log.LogEvent;

public class LegalArchiveLoggerTest {
	
	private static final boolean MOCK_LEGAL_ARCHIVE_SERVICE = true;
	
	private String logCaller = "JBOSS: SDP-kanal";
	private String logAction = "urn:skatteetaten:fastsetting:innsamling:meldingsproduksjon:a-nav-inntekt-spoerring:soap:v1:hentInntektsmottakersInntekt";
	private String logServiceConsumer = "urn:sdp:avsender";
	private String logServiceProducer = "urn:sdp:meldingsformidler";
	private String logEndpoint = "http://e26apvl028.test.local:9089/nav-emottak-archiver-web/services/archiver";
	private String logUsername = "srvsdpgw_u";
	private String logPassword = "dfwYokIJ83-suyq";
	private int maxRetries = 4;
	private int retryIntervalInSeconds = 1;

	public LegalArchiveLoggerTest() {
		Properties props = System.getProperties();
		props.setProperty("org.apache.cxf.stax.allowInsecureParser", "true");
	}
		
	@Test
	public void logEvent(){
		
		LegalArchiveLogger lal = getLogger();
		
		Exchange ex = getMockExchange();
		
		lal.logFirstEvent("RANDOM" + Math.random(), ex, LogEvent.MELDING_HENTET_FRA_KO);
		
		Assert.assertNotNull(ex.getIn().getHeader(KanalConstants.CAMEL_HEADER_LEGAL_ARCHIVE_LOG_ID));
		
	}
	
	@Test
	public void logMultipleEvents(){
		
		
		LegalArchiveLogger lal = getLogger();
		Exchange ex = getMockExchange();
		
		lal.logFirstEvent("RANDOM" + Math.random(), ex, LogEvent.MELDING_HENTET_FRA_KO);
		String logId = (String) ex.getIn().getHeader(KanalConstants.CAMEL_HEADER_LEGAL_ARCHIVE_LOG_ID);
		lal.logEvent(ex, LogEvent.MELDING_HENTET_FRA_KO);
		String logId2 = (String) ex.getIn().getHeader(KanalConstants.CAMEL_HEADER_LEGAL_ARCHIVE_LOG_ID);
		Assert.assertNotNull("LogId should have been set",ex.getIn().getHeader(KanalConstants.CAMEL_HEADER_LEGAL_ARCHIVE_LOG_ID));
		Assert.assertEquals("Log-id should not change", logId, logId2);
		
	}
	
	@Test
	public void logEventsWithAttachment(){
		
		
		LegalArchiveLogger lal = getLogger();
		Exchange ex = getMockExchange();
		byte[] attachment1 = {7,3,3,1};
		byte[] attachment2 = {1,3,3,7};
		lal.logFirstEvent("RANDOM" + Math.random(), ex, LogEvent.MELDING_HENTET_FRA_KO, attachment1);
		String logId = (String) ex.getIn().getHeader(KanalConstants.CAMEL_HEADER_LEGAL_ARCHIVE_LOG_ID);
		lal.logEvent(ex, LogEvent.MELDING_HENTET_FRA_KO, attachment2);
		String logId2 = (String) ex.getIn().getHeader(KanalConstants.CAMEL_HEADER_LEGAL_ARCHIVE_LOG_ID);
		Assert.assertNotNull("LogId should have been set",ex.getIn().getHeader(KanalConstants.CAMEL_HEADER_LEGAL_ARCHIVE_LOG_ID));
		Assert.assertEquals("Log-id should not change", logId, logId2);
		
	}
	
	@Test
	public void logEventsInParallell(){
		
		
		LegalArchiveLogger lal = getLogger();
		Exchange ex1 = getMockExchange();
		Exchange ex2 = getMockExchange();
		Exchange ex3 = getMockExchange();

		// MESSAGE 1
		lal.logFirstEvent("RANDOM1" + Math.random(), ex1, LogEvent.MELDING_HENTET_FRA_KO);
		String logId11 = (String) ex1.getIn().getHeader(KanalConstants.CAMEL_HEADER_LEGAL_ARCHIVE_LOG_ID);
		
		//MESSAGE 2
		lal.logFirstEvent("RANDOM2" + Math.random(), ex2, LogEvent.MELDING_HENTET_FRA_KO);
		String logId21 = (String) ex2.getIn().getHeader(KanalConstants.CAMEL_HEADER_LEGAL_ARCHIVE_LOG_ID);
		
		//MESSAGE 3
		lal.logFirstEvent("RANDOM3" + Math.random(), ex3, LogEvent.MELDING_HENTET_FRA_KO);
		String logId31 = (String) ex3.getIn().getHeader(KanalConstants.CAMEL_HEADER_LEGAL_ARCHIVE_LOG_ID);
		
		// MESSAGE 1
		lal.logEvent(ex1, LogEvent.MELDING_HENTET_FRA_KO);
		String logId12 = (String) ex1.getIn().getHeader(KanalConstants.CAMEL_HEADER_LEGAL_ARCHIVE_LOG_ID);
		
		// MESSAGE 2
		lal.logEvent(ex2, LogEvent.MELDING_HENTET_FRA_KO);
		String logId22 = (String) ex2.getIn().getHeader(KanalConstants.CAMEL_HEADER_LEGAL_ARCHIVE_LOG_ID);
		
		// MESSAGE 3
		lal.logEvent(ex3, LogEvent.MELDING_HENTET_FRA_KO);
		String logId32 = (String) ex3.getIn().getHeader(KanalConstants.CAMEL_HEADER_LEGAL_ARCHIVE_LOG_ID);
		
		Assert.assertNotNull("LogId should have been set",ex1.getIn().getHeader(KanalConstants.CAMEL_HEADER_LEGAL_ARCHIVE_LOG_ID));
		Assert.assertNotNull("LogId should have been set",ex2.getIn().getHeader(KanalConstants.CAMEL_HEADER_LEGAL_ARCHIVE_LOG_ID));
		Assert.assertNotNull("LogId should have been set",ex3.getIn().getHeader(KanalConstants.CAMEL_HEADER_LEGAL_ARCHIVE_LOG_ID));
		
		Assert.assertEquals("Log-id should not change", logId11, logId12);
		Assert.assertEquals("Log-id should not change", logId21, logId22);
		Assert.assertEquals("Log-id should not change", logId31, logId32);
		
		Assert.assertNotEquals("Log-id for different messages should not be equal", logId11, logId21);
		Assert.assertNotEquals("Log-id for different messages should not be equal", logId11, logId31);
		Assert.assertNotEquals("Log-id for different messages should not be equal", logId21, logId31);
		
	}
	
	private LegalArchiveLogger getLogger(){
		
		if(MOCK_LEGAL_ARCHIVE_SERVICE){
			return getMockLogger();
		}
		
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
	
	private LegalArchiveLogger getMockLogger(){
		
		LegalArchiveLoggerMock lal = new LegalArchiveLoggerMock();
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
