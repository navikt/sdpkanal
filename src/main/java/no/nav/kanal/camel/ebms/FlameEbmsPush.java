package no.nav.kanal.camel.ebms;


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import javax.activation.URLDataSource;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.transform.stream.StreamSource;

import no.nav.kanal.KanalConstants;
import no.nav.kanal.log.LegalArchiveLogger;
import no.nav.kanal.log.LogEvent;
import no.nav.tjeneste.virksomhet.digitalpost.senddigitalpost.v1.SendDigitalPost;
import no.nav.tjeneste.virksomhet.digitalpost.senddigitalpost.v1.meldinger.SendDigitalPostRequest;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.RuntimeCamelException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.flame.client.as4.api.Client;
import com.flame.client.as4.api.ClientFactory.ClientException;

public class FlameEbmsPush implements Processor {

	protected static final Logger log = LoggerFactory.getLogger(FlameEbmsPush.class);
	
	
	private String ebmsTo;
	private String ebmsFrom;
	private String mpcNormal;
	private String mpcPrioritert;
	private Map<String, String> partProperties;
	private LegalArchiveLogger legalArchive = null;
	private int maxRetries = 3;
	private int retryIntervalInSeconds = 5;
	private FlameEbms flamefactory;
	
	@Override
    public void process(Exchange exchangeIn) throws Exception {
        log.info("FlameEbms is pushing");
        		
		SOAPMessage reply;
		int retryCounter= 0;
		while(true) {
			try {
				if(retryCounter > 0){
					log.info("Push retry number " + retryCounter);
				}
				reply = createAndSendMessage(exchangeIn);
				// check if we should retry based on EBMS response
				if(FlameEbmsUtil.isEbmsSignalMessage(reply) && 
						FlameEbmsUtil.hasEbmsError(reply) && 
						FlameEbmsUtil.getEbmsErrorCode(reply).equalsIgnoreCase(FlameEbmsUtil.EBMS_ERROR_0004)) {
					if(++retryCounter == maxRetries) {
						log.warn("Maximum retries reached.");
						throw new RuntimeCamelException("Maximum retries reached and EBMS0004");
					}
					log.debug("Waiting " + retryIntervalInSeconds + " before retrying push because of EBMS0004");
					Thread.sleep(retryIntervalInSeconds*1000);				
				} else {
					break;
				}
				
			} catch (ClientException e) {
				log.error("Error during transmit: " + e.getMessage());
				legalArchive.logEvent(exchangeIn, LogEvent.MELDING_SENDT_TIL_DIFI_FEILET, ". Retry number " + retryCounter + ". Error: " + e.getCause());
				// check if we should retry based on connection problems
				if(isConnectionProblem(e)) {
					if(++retryCounter == maxRetries) {
						log.warn("Maximum retries reached.");
						throw new RuntimeCamelException("Maximum retries reached and ClientException during push: " + e.getMessage(), e);
					}
					log.debug("Waiting " + retryIntervalInSeconds + " before retrying push because of connection problems");
					Thread.sleep(retryIntervalInSeconds*1000);					
				} else {
					log.warn("Not a temporary problem. No retry performed.");
					throw new RuntimeCamelException("ClientException during push: " + e.getMessage(), e);					
				}				
			}
		}
		
		handleResponse(reply, exchangeIn);
    }

	/* Handles EBMS reply */
	private void handleResponse(SOAPMessage reply, Exchange exchangeIn) throws SOAPException, IOException {
		String replyString = soapMessageToString(reply);
		if(FlameEbmsUtil.isEbmsReceipt(reply)  || FlameEbmsUtil.isEbmsSignalMessageWithDuplicate(reply)) {
			log.debug("Reply: \n{}", replyString);
			legalArchive.logEvent(exchangeIn, LogEvent.MELDING_SENDT_OK_TIL_DIFI, replyString.getBytes());
			exchangeIn.getIn().setBody(replyString);

		} else {
			log.error("Unknown EBMS response: " + replyString);
			legalArchive.logEvent(exchangeIn, LogEvent.MELDING_SENDT_TIL_DIFI_FEILET_UKJENT_TRANSPORTKVITTERING," Error: Unknown EBMS response", replyString.getBytes());
			exchangeIn.setProperty(KanalConstants.CAMEL_PROPERTY_ERROR_TO_PROPAGATE, FlameEbmsUtil.getEbmsErrorCode(reply) + " " + FlameEbmsUtil.getEbmsErrorDescription(reply));
			exchangeIn.getIn().setBody(replyString);
			throw new RuntimeCamelException("Unhandled response to push: " + replyString);
		}
	}

	/* Creates and sends an EBMS message. */
	private SOAPMessage createAndSendMessage(Exchange exchangeIn) throws Exception {

		SendDigitalPost melding = (SendDigitalPost) exchangeIn.getIn().getBody();
        SendDigitalPostRequest requestMessage = melding.getSendDigitalPostRequest();
		
        Client client; 
        
        if (exchangeIn.getIn().getHeader(KanalConstants.CAMEL_HEADER_TYPE).equals(KanalConstants.CAMEL_HEADER_TYPE_FYSISK_POST)) {
        	//Melding er fysisk post
        	client = flamefactory.getClientFactory().createClient(ebmsTo, ebmsFrom, flamefactory.getPushModeFysisk(), KanalConstants.EBMS_PMODE_EVENTID_FYSISK_PUSH, null);
        } else {
        	//Melding er digital post
        	client = flamefactory.getClientFactory().createClient(ebmsTo, ebmsFrom, flamefactory.getPushModeDigital(), KanalConstants.EBMS_PMODE_EVENTID_DIGITAL_PUSH, null);
        }
        
		client.setSecurityContext(FlameEbms.readSecurityContext("/flame/sc.push-wts.xml"));
		
		// sets password for key used when signing messages
		client.setSignAliasPW(flamefactory.getKeyPassword());
		
		String conversationId = requestMessage.getStandardBusinessDocument().getStandardBusinessDocumentHeader().getBusinessScope().getScope().get(0).getInstanceIdentifier();
		client.setConversationID(conversationId);
		log.info("ConversationID: " + conversationId);
		String messageId = requestMessage.getStandardBusinessDocument().getStandardBusinessDocumentHeader().getDocumentIdentification().getInstanceIdentifier();
		client.setMessageID(messageId);
		log.debug("MessageID: " + messageId);
				
		String mpc = (requestMessage.isErPrioritert()) ? mpcPrioritert : mpcNormal;
		client.setMPC(mpc);
		log.debug("MPC:" + mpc);
		
		String dokpakkefilename = (String) exchangeIn.getIn().getHeader(KanalConstants.CAMEL_HEADER_DOKUMENTPAKKE);
		client.addAttachment((HashMap<String, String>) partProperties, new URLDataSource(new File(dokpakkefilename).toURI().toURL()));
		log.debug("Preparing to push dokumentpakke with name " + dokpakkefilename);

		StreamSource payload = new StreamSource(new File((String) exchangeIn.getIn().getHeader(KanalConstants.CAMEL_HEADER_STANDARD_BUSINESS_DOCUMENT)));
		client.setPayload(payload);
		
		return client.transmit(null, flamefactory.getEbmsEndpoint());
	}
	

	/* Iterates through exception to see this is a connection problem. Breaks to make sure we don't check too deep */
	private boolean isConnectionProblem(ClientException e) {
		int depth=0;
		Throwable cause = e.getCause();
		while(cause != null) {			
			if(cause instanceof ConnectException || cause instanceof UnknownHostException){
				return true;
			}
			if(depth++ > 3){
				break;
			}
			cause = cause.getCause();
		}
		return false;
	}

	/* Converts SOAPMessage to String */
	private String soapMessageToString(SOAPMessage soap) throws SOAPException, IOException {
		ByteArrayOutputStream bo = new ByteArrayOutputStream();
		soap.writeTo(bo);
		return bo.toString();
	}

	public Map<String, String> getPartProperties() {
		return partProperties;
	}

	public void setPartProperties(Map<String, String> partProperties) {
		this.partProperties = partProperties;
	}

	public FlameEbms getFlamefactory() {
		return flamefactory;
	}

	public void setFlamefactory(FlameEbms flamefactory) {
		this.flamefactory = flamefactory;
	}

	public String getEbmsTo() {
		return ebmsTo;
	}

	public void setEbmsTo(String ebmsTo) {
		this.ebmsTo = ebmsTo;
	}

	public String getEbmsFrom() {
		return ebmsFrom;
	}

	public void setEbmsFrom(String ebmsFrom) {
		this.ebmsFrom = ebmsFrom;
	}
	
	public void setLegalArchive(LegalArchiveLogger legalArchive) {
		this.legalArchive = legalArchive;
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

	public String getMpcNormal() {
		return mpcNormal;
	}

	public void setMpcNormal(String mpcNormal) {
		this.mpcNormal = mpcNormal;
	}

	public String getMpcPrioritert() {
		return mpcPrioritert;
	}

	public void setMpcPrioritert(String mpcPrioritert) {
		this.mpcPrioritert = mpcPrioritert;
	}
}
