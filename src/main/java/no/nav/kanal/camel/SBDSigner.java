package no.nav.kanal.camel;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;

import no.difi.begrep.sdp.schema_v10.DigitalPost;
import no.nav.kanal.KanalConstants;
import no.nav.kanal.crypto.HashCalculator;
import no.nav.kanal.config.NavKeysAndCertificates;
import no.nav.kanal.crypto.SignatureCreator;
import no.nav.kanal.log.LegalArchiveLogger;
import no.nav.kanal.log.LogEvent;
import no.nav.tjeneste.virksomhet.digitalpost.senddigitalpost.v1.SendDigitalPost;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.RuntimeCamelException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.unece.cefact.namespaces.standardbusinessdocumentheader.StandardBusinessDocument;
import org.w3._2000._09.xmldsig_.DigestMethodType;
import org.w3._2000._09.xmldsig_.ReferenceType;

public class SBDSigner implements Processor {

	private static Logger log = LoggerFactory.getLogger(SBDSigner.class);
	
	private String keystoreFile = null;
	private String keystorePassword = null;
	private String serviceOwnerPrivateKeyAlias = null;
	private String serviceOwnerPrivateKeyPassword = null;
	private LegalArchiveLogger legalArchive = null;
	
	
	private NavKeysAndCertificates navKeyAndCert = null;
	private static final String SHA256_DIGEST_IDENTIFIER = "http://www.w3.org/2001/04/xmlenc#sha256";
	private static final String STANDARD_BUSINESS_DOCUMENT_FILE_NAME = "StandardBusinessDocument.xml";
	private static final String STANDARD_BUSINESS_DOCUMENT_NAMESPACE = "http://www.unece.org/cefact/namespaces/StandardBusinessDocumentHeader";
	private static final String STANDARD_BUSINESS_DOCUMENT_LOCAL_NAME = "StandardBusinessDocument";
	
	
	@Override
	public void process(Exchange exchangeIn) throws Exception {
				
		log.debug("Started process of signing SBD");
		SendDigitalPost melding = (SendDigitalPost) exchangeIn.getIn().getBody();
		String kryptertDokumentPakkeFilPath = (String) exchangeIn.getIn().getHeader(KanalConstants.CAMEL_HEADER_DOKUMENTPAKKE);
		String tempDirectoryPath = (String) exchangeIn.getIn().getHeader(KanalConstants.CAMEL_HEADER_TEMP_DIRECTORY);
		String signertSBDFilPath = tempDirectoryPath + STANDARD_BUSINESS_DOCUMENT_FILE_NAME;
		File signertSBDFil = new File(signertSBDFilPath);
		
		skrivFingeravtrykkVerdi(melding, kryptertDokumentPakkeFilPath);
		signerSBD(melding, signertSBDFil);
		
		legalArchive.logEvent(exchangeIn, LogEvent.SBD_SIGNERT);
		
		exchangeIn.getIn().setHeader(KanalConstants.CAMEL_HEADER_STANDARD_BUSINESS_DOCUMENT, signertSBDFilPath);
		
	}
	
	private void skrivFingeravtrykkVerdi(SendDigitalPost melding, String kryptertDokumentPakkePath){
		
		log.debug("Calculating dokumentpakkefingeravtrykk");
		File kryptertDokumentpakke = new File(kryptertDokumentPakkePath);
		@SuppressWarnings("unchecked")
		DigitalPost digitalPost = ((JAXBElement<DigitalPost>) melding.getSendDigitalPostRequest().getStandardBusinessDocument().getAny()).getValue();
		ReferenceType fingeravtrykk = new ReferenceType();
		DigestMethodType digest = new DigestMethodType();
		digest.setAlgorithm(SHA256_DIGEST_IDENTIFIER);
		fingeravtrykk.setDigestMethod(digest);
		fingeravtrykk.setDigestValue(HashCalculator.getSHA256Sum(kryptertDokumentpakke));
		digitalPost.setDokumentpakkefingeravtrykk(fingeravtrykk);
		
	}

	private byte[] ekstraherSBDFraMelding(SendDigitalPost melding) throws JAXBException{
		
		log.debug("Extracting SBD from message");
		StandardBusinessDocument standardBusinessDocument = melding.getSendDigitalPostRequest().getStandardBusinessDocument(); 
		
		JAXBElement<StandardBusinessDocument> sbdJaxb = new JAXBElement<StandardBusinessDocument>(new QName(STANDARD_BUSINESS_DOCUMENT_NAMESPACE, STANDARD_BUSINESS_DOCUMENT_LOCAL_NAME), StandardBusinessDocument.class, standardBusinessDocument);	
		JAXBContext jb = JAXBContext.newInstance(no.difi.begrep.sdp.schema_v10.DigitalPost.class, StandardBusinessDocument.class);
		Marshaller mar = jb.createMarshaller();
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		mar.marshal(sbdJaxb, bos);
		return bos.toByteArray();
	}
	
	private void signerSBD(SendDigitalPost melding, File outputFil){
		
		log.debug("Creating SBD signature");
		try {
			byte[] rawSBD = ekstraherSBDFraMelding(melding);
			
			
			
			SignatureCreator signatureCreator = new SignatureCreator(navKeyAndCert);
			byte[] signedRawSBD = signatureCreator.createSBDSignature(rawSBD);
			ByteArrayInputStream bis = new ByteArrayInputStream(signedRawSBD);
			FileOutputStream fos = new FileOutputStream(outputFil);
			
			byte[] buffer = new byte[KanalConstants.SYSTEM_BLOCK_READ_SIZE];
            int readLength = bis.read(buffer);
            while (readLength != -1) {
            	fos.write(buffer, 0, readLength);
            	readLength = bis.read(buffer);
            }
            fos.flush();
            fos.close();
			
		} catch (JAXBException | IOException e) {
			throw new RuntimeCamelException("Could not write SDB signature to file", e);
		}
		log.info("SBD signature created OK");
	}
	
	public String getKeystoreFile() {
		return keystoreFile;
	}

	public void setKeystoreFile(String keystoreFile) {
		this.keystoreFile = keystoreFile;
	}

	public String getKeystorePassword() {
		return keystorePassword;
	}

	public void setKeystorePassword(String keystorePassword) {
		this.keystorePassword = keystorePassword;
	}

	public String getServiceOwnerPrivateKeyAlias() {
		return serviceOwnerPrivateKeyAlias;
	}

	public void setServiceOwnerPrivateKeyAlias(
			String serviceOwnerPrivateKeyAlias) {
		this.serviceOwnerPrivateKeyAlias = serviceOwnerPrivateKeyAlias;
	}

	public String getServiceOwnerPrivateKeyPassword() {
		return serviceOwnerPrivateKeyPassword;
	}

	public void setServiceOwnerPrivateKeyPassword(
			String serviceOwnerPrivateKeyPassword) {
		this.serviceOwnerPrivateKeyPassword = serviceOwnerPrivateKeyPassword;
	}
	
	public LegalArchiveLogger getLegalArchive() {
		return legalArchive;
	}

	public void setLegalArchive(LegalArchiveLogger legalArchive) {
		this.legalArchive = legalArchive;
	}
	
	public void setNavKeyAndCert(NavKeysAndCertificates navKeyAndCert) {
		this.navKeyAndCert = navKeyAndCert;
	}
}
