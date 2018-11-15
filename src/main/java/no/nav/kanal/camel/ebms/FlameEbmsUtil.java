package no.nav.kanal.camel.ebms;

import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * Utility class for doing checks on EBMS response messages
 *
 */
public final class FlameEbmsUtil {

	protected static final String NS_EBMS = "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/";
	protected static final String ELEMENT_EBMS_ERROR = "Error";
	protected static final String ELEMENT_EBMS_ERROR_DESCRIPTION = "Description";	
	protected static final String ELEMENT_EBMS_ERROR_DESCRIPTION_DUPLICATE = "Duplicate messageId";
	protected static final String ATTR_EBMS_ERRORCODE = "errorCode";
	protected static final String ELEMENT_EBMS_USERMESSAGE = "UserMessage";
	protected static final String ELEMENT_EBMS_SIGNALMESSAGE = "SignalMessage";
	protected static final String ELEMENT_EBMS_SIGNAL_RECEIPT = "Receipt";
	protected static final String EBMS_ERROR_0003 = "EBMS:0003";
	protected static final String EBMS_ERROR_0004 = "EBMS:0004";
	protected static final String EBMS_ERROR_0006 = "EBMS:0006";

	private FlameEbmsUtil() {
	}
	
	/* Returns value of the Description element in EBMS signalmessage with error */
	protected static String getEbmsErrorDescription(SOAPMessage soap) throws SOAPException {
		Node ebmsErrorDescription = soap.getSOAPHeader().getElementsByTagNameNS(NS_EBMS, ELEMENT_EBMS_ERROR_DESCRIPTION).item(0);
		
		if(ebmsErrorDescription != null) {
			return ebmsErrorDescription.getTextContent();
		} else {
			return "";
		}
	}
	
	/* Returns value of errorCode attribute in EBMS signalmessage with error */
	protected static String getEbmsErrorCode(SOAPMessage soap) throws SOAPException {
		Node ebmsError = soap.getSOAPHeader().getElementsByTagNameNS(NS_EBMS, ELEMENT_EBMS_ERROR).item(0);
		
		NamedNodeMap attrs = ebmsError.getAttributes();
		return attrs.getNamedItem(ATTR_EBMS_ERRORCODE).getNodeValue();
	}
		
	/* Returns true if SOAPMessage is EBMS usermessage */
	protected static boolean isEbmsUserMessage(SOAPMessage soap) throws SOAPException {
		Node ebmsMessaging = soap.getSOAPHeader().getElementsByTagNameNS(NS_EBMS, ELEMENT_EBMS_USERMESSAGE).item(0);
		
		return ebmsMessaging != null;
	}
	
	/* Returns true if SOAPMessage is EBMS signalmessage */
	protected static boolean isEbmsSignalMessage(SOAPMessage soap) throws SOAPException {
		Node ebmsMessaging = soap.getSOAPHeader().getElementsByTagNameNS(NS_EBMS, ELEMENT_EBMS_SIGNALMESSAGE).item(0);
		
		return ebmsMessaging != null;
	}
	
	/* Returns true if SOAPMessage is EBMS signalmessage with Duplicate */
	protected static boolean isEbmsSignalMessageWithDuplicate(SOAPMessage soap) throws SOAPException {
		if(isEbmsSignalMessage(soap)
				&& hasEbmsError(soap)
				&& (EBMS_ERROR_0003.equalsIgnoreCase(getEbmsErrorCode(soap)))
				&& ELEMENT_EBMS_ERROR_DESCRIPTION_DUPLICATE.equalsIgnoreCase(getEbmsErrorDescription(soap))) {
			return true;			
		}
		else {
			return false;
		}
		
	}
	
	/* Returns true if SOAPMessage is EBMS signalmessage with ebms error */
	protected static boolean hasEbmsError(SOAPMessage soap) throws SOAPException {
		Node ebmsError = soap.getSOAPHeader().getElementsByTagNameNS(NS_EBMS, ELEMENT_EBMS_ERROR).item(0);
		return ebmsError != null;
	}
	
	/* Returns true if SOAPMessage is EBMS signalmessage with receipt */
	protected static boolean isEbmsReceipt(SOAPMessage soap) throws SOAPException {
		Node ebmsReceipt = soap.getSOAPHeader().getElementsByTagNameNS(NS_EBMS, ELEMENT_EBMS_SIGNAL_RECEIPT).item(0);
		return ebmsReceipt != null;
	}
	
}
