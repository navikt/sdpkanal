package no.nav.kanal.camel.ebms;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.util.Map;

import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.soap.SOAPFaultException;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.digipost.api.MessageSender;
import no.digipost.api.representations.*;
import no.nav.kanal.KanalConstants;
import no.nav.kanal.camel.DocumentPackageCreator;
import no.nav.kanal.camel.XmlExtractor;
import no.nav.kanal.log.LegalArchiveLogger;
import no.nav.kanal.log.LogEvent;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.RuntimeCamelException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.unece.cefact.namespaces.standardbusinessdocumentheader.StandardBusinessDocument;

public class FlameEbmsPush implements Processor {

	protected static final Logger log = LoggerFactory.getLogger(FlameEbmsPush.class);
	private String mpcNormal;
	private String mpcPrioritert;
	private Map<String, String> partProperties;
	private LegalArchiveLogger legalArchive = null;
	private int maxRetries = 3;
	private int retryIntervalInSeconds = 5;
	private DigipostEbms digipostEbms;

	@Override
    public void process(Exchange exchangeIn) throws Exception {
        log.info("FlameEbms is pushing");

		TransportKvittering reply;
		int retryCounter= 0;
		while(true) {
			try {
				if(retryCounter > 0){
					log.info("Push retry number " + retryCounter);
				}
				reply = createAndSendMessage(exchangeIn);
				break;
				// check if we should retry based on EBMS response
				//if(FlameEbmsUtil.isEbmsSignalMessage(reply) &&
				//		FlameEbmsUtil.hasEbmsError(reply) &&
				//		FlameEbmsUtil.getEbmsErrorCode(reply).equalsIgnoreCase(FlameEbmsUtil.EBMS_ERROR_0004)) {
				//	if(++retryCounter == maxRetries) {
				//		log.warn("Maximum retries reached.");
				//		throw new RuntimeCamelException("Maximum retries reached and EBMS0004");
				//	}
				//	log.debug("Waiting " + retryIntervalInSeconds + " before retrying push because of EBMS0004");
				//	Thread.sleep(retryIntervalInSeconds*1000);
				//} else {
				//	break;
				//}

			} catch (SOAPFaultException e) {
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

		System.out.println(reply.messageId);
		System.out.println(new ObjectMapper().writeValueAsString(reply));
		//handleResponse(reply, exchangeIn);
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


	private TransportKvittering createAndSendMessage(Exchange exchangeIn) {
		StandardBusinessDocument sbd = exchangeIn.getIn().getHeader(XmlExtractor.STANDARD_BUSINESS_DOCUMENT, StandardBusinessDocument.class);
		Boolean isPrioritized = exchangeIn.getIn().getHeader(XmlExtractor.HAS_PRIORITY, Boolean.class);

		Dokumentpakke documentPackage = exchangeIn.getIn().getHeader(DocumentPackageCreator.DOCUMENT_PACKAGE, Dokumentpakke.class);

		Organisasjonsnummer sbdhMottaker = Organisasjonsnummer.of(sbd.getStandardBusinessDocumentHeader().getReceivers().get(0).getIdentifier().getValue());
		String mpc = (isPrioritized) ? mpcPrioritert : mpcNormal;

		MessageSender messageSender = digipostEbms.getMessageSender();
		return messageSender.send(EbmsForsendelse
				.create(digipostEbms.getDatabehandler(), digipostEbms.getMottaker(), sbdhMottaker, sbd, documentPackage)
				.withMpcId(mpc)
				.build());
	}


	/* Iterates through exception to see this is a connection problem. Breaks to make sure we don't check too deep */
	private boolean isConnectionProblem(Exception e) {
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

	public void setDigipostEbms(DigipostEbms digipostEbms) {
		this.digipostEbms = digipostEbms;
	}

	public DigipostEbms getDigipostEbms() {
		return digipostEbms;
	}
}
