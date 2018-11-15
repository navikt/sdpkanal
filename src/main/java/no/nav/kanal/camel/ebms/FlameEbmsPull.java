package no.nav.kanal.camel.ebms;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;

import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;

import no.nav.kanal.KanalConstants;
import no.nav.kanal.log.LegalArchiveLogger;
import no.nav.kanal.log.LogEvent;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.builder.xml.XPathBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.w3c.dom.NodeList;

import com.flame.client.as4.api.Client;
import com.flame.client.as4.api.ClientFactory.ClientException;

public class FlameEbmsPull implements Processor {

	protected static final Logger log = LoggerFactory.getLogger(FlameEbmsPull.class);

	private FlameEbms flamefactory;
	private LegalArchiveLogger legalArchive = null;
	private String mpcNormal;
	private String mpcPrioritert;

	private static final String NS_SBDH = "http://www.unece.org/cefact/namespaces/StandardBusinessDocumentHeader";
	private static final String ELEMENT_INSTANCE_IDENTIFIER = "InstanceIdentifier";
	private static final String SCOPE_INSTANCE_IDENTIFIER = "Scope";
	
	@Override
    public void process(Exchange exchangeIn) throws Exception {
        log.debug("FlameEbms is pulling");
        
        // clear MDC callId from previous iteration
        MDC.remove("callId");
        
        // reset body. We do not want to see earlier messages in the body but we must allow headers as they are used in the logic 
        exchangeIn.getIn().setBody(null);
        
		Client client = flamefactory.getClientFactory().createClient(null, null, flamefactory.getPullMode(), KanalConstants.EBMS_PMODE_EVENTID_PULL, null);
		client.setSecurityContext(FlameEbms.readSecurityContext("/flame/sc.pull.xml"));

		// sets password for key used when signing messages
		client.setSignAliasPW(flamefactory.getKeyPassword());
		
		client.setConversationID(UUID.randomUUID().toString());
		client.setMessageID(UUID.randomUUID().toString());
				
		// set MPC to pull
		String mpc = (String) exchangeIn.getIn().getHeader(KanalConstants.CAMEL_HEADER_MPC);
		String environmentSpecificMpc = (mpc.contains(KanalConstants.SDP_MPC_PRIORITERT)) ? mpcPrioritert : mpcNormal;
		client.setMPC(environmentSpecificMpc);
		log.info("MPC: " + environmentSpecificMpc);
		
		// get previous message from exchange if it exists. Used for ACK (NonRepudiationInformation in message)
		SOAPMessage inRecptTo = (SOAPMessage) exchangeIn.getIn().getHeader(KanalConstants.CAMEL_HEADER_MESSAGE_TO_ACK);
		if(inRecptTo != null) {
			client.setInReceiptTo(inRecptTo);
			// clear header
			exchangeIn.getIn().setHeader(KanalConstants.CAMEL_HEADER_MESSAGE_TO_ACK, null);
		}
		
		SOAPMessage reply = null; 
		try {
			reply = client.transmit(null, flamefactory.getEbmsEndpoint());
		} catch (ClientException e) {
			log.error("Error during transmit: " + e.getMessage());
			log.error("Cause: " + e.getCause());
			// indicate end of loop
			signalExit(exchangeIn, environmentSpecificMpc);
			throw new RuntimeCamelException("Flame ClientException: " + e.getMessage(), e);
		}
		
		// store reply on exchange to be able to send ack for this specific message
		// set message body to pass to next component
		if(reply!=null) {
			String replyString = soapMessageToString(reply);
			log.debug("Reply: \n" + replyString);
			if(FlameEbmsUtil.isEbmsUserMessage(reply)) {
				// business message. Just pass on
				String instanceId = getInstanceIdentifier(reply);
				MDC.put("callId", instanceId);
				log.debug("MDC added: " + instanceId);
				legalArchive.logFirstEvent(instanceId, exchangeIn, LogEvent.KVITTERING_MOTTATT_FRA_DIFI, replyString.getBytes());
				exchangeIn.getIn().setHeader(KanalConstants.CAMEL_HEADER_MESSAGE_TO_ACK, reply);
				exchangeIn.getIn().setBody(extractSBD(exchangeIn, replyString));
				return;
			} else if(FlameEbmsUtil.isEbmsSignalMessage(reply) && FlameEbmsUtil.hasEbmsError(reply)) {
				// only indicate exit loop if the reply indicates no more messages (EBMS:0006)
				if(FlameEbmsUtil.getEbmsErrorCode(reply).equalsIgnoreCase(FlameEbmsUtil.EBMS_ERROR_0006)) {
					signalExit(exchangeIn, environmentSpecificMpc);
					log.info(FlameEbmsUtil.EBMS_ERROR_0006 + ". Controlled exit from pull.");
				} else {
					signalExit(exchangeIn, environmentSpecificMpc);
					log.error("Unhandled EBMS error. Exiting pull-loop." + FlameEbmsUtil.getEbmsErrorCode(reply));
				}
			} else {
				signalExit(exchangeIn, environmentSpecificMpc);
				log.error("Unhandled reply when pulling. Exiting pull-loop.");
			}
		}

    }

	private String soapMessageToString(SOAPMessage soap) throws SOAPException, IOException {
		ByteArrayOutputStream bo = new ByteArrayOutputStream();
		soap.writeTo(bo);
		return bo.toString();
	}

	private String extractSBD(Exchange exchangeIn, String replyString) {
		XPathBuilder xp = XPathBuilder.xpath("//env:Envelope/env:Body/sbd:StandardBusinessDocument");
		xp.namespace("sbd", "http://www.unece.org/cefact/namespaces/StandardBusinessDocumentHeader");
		xp.namespace("env", "http://www.w3.org/2003/05/soap-envelope");
		String sbd = xp.evaluate(exchangeIn.getContext(), replyString, java.lang.String.class);
		return sbd;
	}
	
	private void signalExit(Exchange exchangeIn, String mpc) {
		
		if(mpc.contains(KanalConstants.SDP_MPC_PRIORITERT)) {
			exchangeIn.getIn().setHeader(KanalConstants.CAMEL_HEADER_LOOP_INDICATOR_PRIORITERT, null);
			log.debug("Signalling end of pollcycle for MPC " + mpc);
		} else {
			exchangeIn.getIn().setHeader(KanalConstants.CAMEL_HEADER_LOOP_INDICATOR_NORMAL, null);
			log.debug("Signalling end of pollcycle for MPC " + mpc);
		}
		// set this header to null when exiting from loop
		exchangeIn.getIn().setHeader(KanalConstants.CAMEL_HEADER_MESSAGE_TO_ACK, null);
		
	}
	
	private String getInstanceIdentifier(SOAPMessage soap) throws SOAPException{
		NodeList instanceIdentifierList = soap.getSOAPBody().getElementsByTagNameNS(NS_SBDH, ELEMENT_INSTANCE_IDENTIFIER);
		String instanceIdentifier = null;
		for (int i = 0; i < instanceIdentifierList.getLength(); i++) {
			if(instanceIdentifierList.item(i).getParentNode().getLocalName().equals(SCOPE_INSTANCE_IDENTIFIER)){
				instanceIdentifier = instanceIdentifierList.item(i).getTextContent();
				log.debug("Setting log identifier to ("  + instanceIdentifier + ")");
				break;
			}
		}
		if(instanceIdentifier == null){
			throw new RuntimeCamelException("Could not find instanceIdentifier to use for logging");
		}
		
		return instanceIdentifier;
	}
	
	public FlameEbms getFlamefactory() {
		return flamefactory;
	}

	public void setFlamefactory(FlameEbms flamefactory) {
		this.flamefactory = flamefactory;
	}

	public void setLegalArchive(LegalArchiveLogger legalArchive) {
		this.legalArchive = legalArchive;
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
