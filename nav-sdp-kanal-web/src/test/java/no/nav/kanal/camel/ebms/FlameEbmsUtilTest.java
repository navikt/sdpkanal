package no.nav.kanal.camel.ebms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;

import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPConstants;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;

import org.junit.BeforeClass;
import org.junit.Test;

public class FlameEbmsUtilTest {

	static SOAPMessage ebmsUserMessage;
	static SOAPMessage ebmsUserMessageWithReceipt;
	static SOAPMessage ebmsSignalMessage;
	static SOAPMessage ebmsSignalMessageWithDuplicate;
	static SOAPMessage ebmsSignalMessageWithEbms004;
	
	@BeforeClass
	public static void setUp() throws Exception {
		MessageFactory mf = MessageFactory.newInstance(SOAPConstants.SOAP_1_2_PROTOCOL);

		FileInputStream usermsg = new FileInputStream(new File("src/test/resources/ebmsUserMessage.xml"));
		ebmsUserMessage = mf.createMessage(null, usermsg);
		
		FileInputStream usermsgWithReceipt = new FileInputStream(new File("src/test/resources/ebmsSignalMessageWithReceipt.xml"));
		ebmsUserMessageWithReceipt = mf.createMessage(null, usermsgWithReceipt);
		
		FileInputStream signalmsg = new FileInputStream(new File("src/test/resources/ebmsSignalMessage.xml"));
		ebmsSignalMessage = mf.createMessage(null, signalmsg);
		
		FileInputStream signalmsgWithDuplicate = new FileInputStream(new File("src/test/resources/__files/push-ack-ebms003duplicate.xml"));
		ebmsSignalMessageWithDuplicate = mf.createMessage(null, signalmsgWithDuplicate);
		
		FileInputStream signalmsgWithebms004 = new FileInputStream(new File("src/test/resources/__files/push-ack-ebms004.xml"));
		ebmsSignalMessageWithEbms004 = mf.createMessage(null, signalmsgWithebms004);
	}

	@Test
	public void testGetEbmsErrorCode() throws SOAPException {
		assertEquals(FlameEbmsUtil.getEbmsErrorCode(ebmsSignalMessage), "EBMS:0003");
	}

	@Test
	public void testIsEbmsUserMessage() throws SOAPException {
		assertTrue(FlameEbmsUtil.isEbmsUserMessage(ebmsUserMessage));
	}
	
	@Test
	public void testIsNotEbmsUserMessage() throws SOAPException {
		assertTrue(!FlameEbmsUtil.isEbmsUserMessage(ebmsSignalMessage));
	}

	@Test
	public void testIsEbmsSignalMessage() throws SOAPException {
		assertTrue(FlameEbmsUtil.isEbmsSignalMessage(ebmsSignalMessage));
	}
	
	@Test
	public void testIsNotEbmsSignalMessage() throws SOAPException {
		assertTrue(!FlameEbmsUtil.isEbmsSignalMessage(ebmsUserMessage));
	}

	@Test
	public void testHasEbmsError() throws SOAPException {
		assertTrue(FlameEbmsUtil.hasEbmsError(ebmsSignalMessage));
	}
	
	@Test
	public void testHasNoEbmsError() throws SOAPException {
		assertTrue(!FlameEbmsUtil.hasEbmsError(ebmsUserMessage));
	}

	@Test
	public void testIsEbmsReceipt() throws SOAPException {
		assertTrue(FlameEbmsUtil.isEbmsReceipt(ebmsUserMessageWithReceipt));
	}
	
	@Test
	public void testIsNotEbmsReceipt() throws SOAPException {
		assertTrue(!FlameEbmsUtil.isEbmsReceipt(ebmsUserMessage));
	}
	
	@Test
	public void testIsEbmsSignalMessageWithDuplicate() throws SOAPException {
		assertTrue(!FlameEbmsUtil.isEbmsSignalMessageWithDuplicate(ebmsUserMessage));
		assertTrue(!FlameEbmsUtil.isEbmsSignalMessageWithDuplicate(ebmsSignalMessage));
		assertTrue(!FlameEbmsUtil.isEbmsSignalMessageWithDuplicate(ebmsSignalMessageWithEbms004));
		assertTrue(FlameEbmsUtil.isEbmsSignalMessageWithDuplicate(ebmsSignalMessageWithDuplicate));
	}

}
