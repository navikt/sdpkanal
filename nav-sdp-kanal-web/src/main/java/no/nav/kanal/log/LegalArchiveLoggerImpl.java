package no.nav.kanal.log;

import javax.xml.ws.BindingProvider;

import org.apache.camel.Exchange;
import org.w3._2005._08.addressing.AttributedURIType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.emottak.archiver.ArchiveType;
import no.nav.emottak.archiver.EventType;
import no.nav.emottak.archiver.LogAndArchiveRequest;
import no.nav.emottak.archiver.LogAndArchiveResponse;
import no.nav.emottak.archiver.MessageType;
import no.nav.emottak.services.archiver.ArchiverPortType;
import no.nav.emottak.services.archiver.ArchiverService;
import no.nav.kanal.KanalConstants;


public class LegalArchiveLoggerImpl implements LegalArchiveLogger {
	
	private Logger log = LoggerFactory.getLogger(LegalArchiveLogger.class);
	
	private String logCaller = null;
	private String logAction = null;
	private String logServiceConsumer = null;
	private String logServiceProducer = null;
	private String logEndpoint = null;
	private String logUsername = null;
	private String logPassword = null;

	private int maxRetries;
	private int retryIntervalInSeconds;

	
	protected ArchiverPortType archiver;
	
	public void logEvent(Exchange exchangeIn, LogEvent logEvent, byte[] attachment){
		
		logEvent(exchangeIn, logEvent, "", attachment);
	}
	
	public void logEvent(Exchange exchangeIn, LogEvent logEvent, String extraInfo, byte[] attachment) {
		
		setUp();
		
		String logId = getLogId(exchangeIn);
		
		log.info("Logging event: " + logEvent.getDescription() + " with attachment of length " + attachment.length + " for message with existing logId: " + logId);
		
		LogAndArchiveRequest request = new LogAndArchiveRequest();
		
		EventType eventType = createEventType(logEvent.getEventNo(), logEvent.getDescription() + extraInfo, logCaller);
		request.setLogId(logId);
		request.getEventOrArchive().add(eventType);
		ArchiveType archiveType = createArchiveAttachment(attachment);
		request.getEventOrArchive().add(archiveType);
		archive(request);
		
	}
	
	public void logEvent(Exchange exchangeIn, LogEvent logEvent){
		
		logEvent(exchangeIn, logEvent, "");
	}
	
	public void logEvent(Exchange exchangeIn, LogEvent logEvent, String extraInfo) {
		setUp();
		
		String logId = getLogId(exchangeIn);
		
		log.info("Logging event: " + logEvent.getDescription() + " for message with existing logId: " + logId);
		
		LogAndArchiveRequest request = new LogAndArchiveRequest();
		
		EventType eventType = createEventType(logEvent.getEventNo(), logEvent.getDescription() + extraInfo, logCaller);
		request.setLogId(logId);
		request.getEventOrArchive().add(eventType);	
		archive(request);
	}
	
	public void logFirstEvent(String messageID, Exchange exchangeIn, LogEvent logEvent, byte[] attachment){
		setUp();
				
		log.info("Logging event: " + logEvent.getDescription() + " with attachment of length " + attachment.length + " for messageId: " + messageID);
		
		LogAndArchiveRequest request = new LogAndArchiveRequest();
		
		MessageType message = createMessageType(logAction, messageID, logServiceConsumer, logServiceProducer);
		EventType eventType = createEventType(logEvent.getEventNo(), logEvent.getDescription(), logCaller);
		request.setMessage(message);
		request.getEventOrArchive().add(eventType);	
		ArchiveType archiveType = createArchiveAttachment(attachment);
		request.getEventOrArchive().add(archiveType);

		String logId = archive(request);

		setLogId(exchangeIn, logId);
		
	}
	
	public void logFirstEvent(String messageID, Exchange exchangeIn, LogEvent logEvent){
		setUp();
				
		log.info("Logging event: " + logEvent.getDescription() + " for message: " + messageID);
		
		LogAndArchiveRequest request = new LogAndArchiveRequest();
		
		MessageType message = createMessageType(logAction, messageID, logServiceConsumer, logServiceProducer);
		EventType eventType = createEventType(logEvent.getEventNo(), logEvent.getDescription(), logCaller);
		request.setMessage(message);
		request.getEventOrArchive().add(eventType);	
		
		String logId = archive(request);
		setLogId(exchangeIn, logId);
		
	}
	private void setLogId(Exchange exchangeIn, String logId){
		log.debug("Setting " + KanalConstants.CAMEL_HEADER_LEGAL_ARCHIVE_LOG_ID + " with value " + logId);
		exchangeIn.getIn().setHeader(KanalConstants.CAMEL_HEADER_LEGAL_ARCHIVE_LOG_ID, logId);
	}
	
	private String getLogId(Exchange exchangeIn){
		String logId = (String) exchangeIn.getIn().getHeader(KanalConstants.CAMEL_HEADER_LEGAL_ARCHIVE_LOG_ID);
		if(logId == null){
			throw new RuntimeException("LogId is not set in camel header (" +KanalConstants.CAMEL_HEADER_LEGAL_ARCHIVE_LOG_ID+ "). This is probably due to calling logEvent before logFirstEvent");
		}
		
		return (String) exchangeIn.getIn().getHeader(KanalConstants.CAMEL_HEADER_LEGAL_ARCHIVE_LOG_ID);
	}
	
	private String archive(LogAndArchiveRequest request ) {
		String logId= null;
		int retryIntervalInSecondsSecondTime= 0;

		for (int retryCounter=0; retryCounter<maxRetries; retryCounter++)
		{
			try {
				if (retryCounter>0){
					log.info("Trying to log event to legal archive, retry number: " + retryCounter);
				}
				else {
					log.info("Trying to log event to legal archive");
				}


				LogAndArchiveResponse resp = archiver.archive(request);
				logId = resp.getLogId();
				log.info("Loged event to legal archive", logId);
				break;

			} catch (Exception e) {

				if(++retryCounter == maxRetries) {
						log.warn("Maximum retries reached.");
						log.error("Failed to log message to legal archive with event" + e.getMessage());
						throw new RuntimeException("Could not log to legal archive: " + e.getMessage(), e);
					}


				}
					try {
						if (retryCounter==1) {
						Thread.sleep(retryIntervalInSecondsSecondTime + 100);
					}
						else {
							Thread.sleep(retryIntervalInSeconds * 1000 + (long) (Math.random() * 50));
							}
					}
					catch (InterruptedException ie){
						log.error("Failed to log message to legal archive with event due to: " + ie.getCause());
					}
				}

		return logId;
	}


	
	private ArchiveType createArchiveAttachment(byte[] attachment){
		
		ArchiveType archiveType = new ArchiveType();
		archiveType.setType("ATTACHMENT");
		archiveType.setBlob(attachment);
		
		return archiveType;
	}
	
	private EventType createEventType(int eventNo, String description, String caller){
		
		EventType event = new EventType();
		event.setEventNo(eventNo);
		event.setDesc(description);
		event.setCaller(caller);
		
		return event;
	}
	
	private MessageType createMessageType(String action, String messageID, String serviceConsumer, String serviceProducer){
		
		MessageType message = new MessageType();
		
		AttributedURIType messageId = new AttributedURIType();
		messageId.setValue(messageID);
		message.setMessageID(messageId);
		
		AttributedURIType actionUri = new AttributedURIType();
		actionUri.setValue(action);
		message.setAction(actionUri);
		
		message.setServiceConsumer(serviceConsumer);
		message.setServiceProducer(serviceProducer);
		
		return message;
	}
	
	protected void setUp(){
		if(archiver==null){
			ArchiverService archiverService = new ArchiverService();
			archiver = archiverService.getArchiver();
			BindingProvider bp = (BindingProvider) archiver;
			bp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, logEndpoint);
			bp.getRequestContext().put(BindingProvider.USERNAME_PROPERTY, logUsername);
			bp.getRequestContext().put(BindingProvider.PASSWORD_PROPERTY, logPassword);			
		}
	}

	public String getLogCaller() {
		return logCaller;
	}

	public void setLogCaller(String logCaller) {
		this.logCaller = logCaller;
	}

	public String getLogAction() {
		return logAction;
	}

	public void setLogAction(String logAction) {
		this.logAction = logAction;
	}

	public String getLogServiceConsumer() {
		return logServiceConsumer;
	}

	public void setLogServiceConsumer(String logServiceConsumer) {
		this.logServiceConsumer = logServiceConsumer;
	}

	public String getLogServiceProducer() {
		return logServiceProducer;
	}

	public void setLogServiceProducer(String logServiceProducer) {
		this.logServiceProducer = logServiceProducer;
	}

	public String getLogEndpoint() {
		return logEndpoint;
	}

	public void setLogEndpoint(String logEndpoint) {
		this.logEndpoint = logEndpoint;
	}

	public String getLogUsername() {
		return logUsername;
	}

	public void setLogUsername(String logUsername) {
		this.logUsername = logUsername;
	}

	public String getLogPassword() {
		return logPassword;
	}

	public void setLogPassword(String logPassword) {
		this.logPassword = logPassword;
	}


	public int getMaxRetries() {
		return maxRetries;
	}

	public void setMaxRetries(int maxRetries) {
		this.maxRetries = maxRetries;
	}

	public int getRetryIntervalInSeconds() {
		return retryIntervalInSeconds;
	}

	public void setRetryIntervalInSeconds(int retryIntervalInSeconds) {
		this.retryIntervalInSeconds = retryIntervalInSeconds;
	}

}
