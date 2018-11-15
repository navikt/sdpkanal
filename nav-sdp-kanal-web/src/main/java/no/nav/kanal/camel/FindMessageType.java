package no.nav.kanal.camel;

import no.difi.begrep.sdp.schema_v10.DigitalPost;
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
import org.slf4j.MDC;
import org.unece.cefact.namespaces.standardbusinessdocumentheader.StandardBusinessDocument;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

public class FindMessageType implements Processor {
	
	private static final String STANDARD_BUSINESS_DOCUMENT_NAMESPACE = "http://www.unece.org/cefact/namespaces/StandardBusinessDocumentHeader";
	private static final String STANDARD_BUSINESS_DOCUMENT_LOCAL_NAME = "StandardBusinessDocument";
	
	private static Logger log = LoggerFactory.getLogger(FindMessageType.class);
	private LegalArchiveLogger legalArchive = null;

	@Override
	public void process(Exchange exchangeIn) throws Exception {
		log.debug("FindMessageType is processing");

        SendDigitalPost melding = (SendDigitalPost) exchangeIn.getIn().getBody();
        SendDigitalPostRequest requestMessage = melding.getSendDigitalPostRequest();
        
        String messageID = extractMessageInstanceIdentifier(requestMessage);
        MDC.put("callId", messageID);
        
        log.debug("Attempting to parse the request message to find message type/action");
        
        StandardBusinessDocument standardBusinessDocument = melding.getSendDigitalPostRequest().getStandardBusinessDocument(); 
		
		JAXBElement<StandardBusinessDocument> sbdJaxb = new JAXBElement<StandardBusinessDocument>(new QName(STANDARD_BUSINESS_DOCUMENT_NAMESPACE, STANDARD_BUSINESS_DOCUMENT_LOCAL_NAME), StandardBusinessDocument.class, standardBusinessDocument);	
        JAXBElement<?> sdbAny = (JAXBElement<?>) sbdJaxb.getValue().getAny();
        DigitalPost digitalpost = (DigitalPost)sdbAny.getValue();
      
        if (digitalpost.getFysiskPostInfo() != null) {
        	//Melding er fysisk post. Setter denne informasjonen i headeren og logger.
        	legalArchive.setLogAction(KanalConstants.LEGAL_ARCHIVE_LOG_ACTION_FYSISK);
        	legalArchive.logFirstEvent(messageID, exchangeIn, LogEvent.MELDING_HENTET_FRA_KO);
        	exchangeIn.getIn().setHeader(KanalConstants.CAMEL_HEADER_TYPE, KanalConstants.CAMEL_HEADER_TYPE_FYSISK_POST);
        } else {
			//Melding er digital post (default). Setter denne informasjonen i headeren og logger.
			legalArchive.setLogAction(KanalConstants.LEGAL_ARCHIVE_LOG_ACTION_DIGITAL);
        	legalArchive.logFirstEvent(messageID, exchangeIn, LogEvent.MELDING_HENTET_FRA_KO);
			exchangeIn.getIn().setHeader(KanalConstants.CAMEL_HEADER_TYPE, KanalConstants.CAMEL_HEADER_TYPE_DIGITAL_POST);
		}  
		
	}
	
    private String extractMessageInstanceIdentifier(SendDigitalPostRequest requestMessage){
    	String messageID = requestMessage.getStandardBusinessDocument().getStandardBusinessDocumentHeader().getBusinessScope().getScope().get(0).getInstanceIdentifier();
    	if(messageID == null || messageID.equals("")){
    		log.error("Message must have a populated InstanceIdentifier");
    		throw new RuntimeCamelException("Message must have a populated InstanceIdentifier");
    	}
    	return messageID;
    	
    }
    
	public LegalArchiveLogger getLegalArchive() {
		return legalArchive;
	}

	public void setLegalArchive(LegalArchiveLogger legalArchive) {
		this.legalArchive = legalArchive;
	}
}
