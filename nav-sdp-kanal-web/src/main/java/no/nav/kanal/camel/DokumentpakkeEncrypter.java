package no.nav.kanal.camel;

import iaik.hlapi.CertValidator;
import iaik.x509.X509Certificate;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.cert.CertificateException;

import no.nav.kanal.KanalConstants;
import no.nav.kanal.crypto.DocumentEncrypter;
import no.nav.kanal.crypto.TrustedCertificatesTools;
import no.nav.kanal.log.LegalArchiveLogger;
import no.nav.kanal.log.LogEvent;
import no.nav.tjeneste.virksomhet.digitalpost.senddigitalpost.v1.SendDigitalPost;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.RuntimeCamelException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DokumentpakkeEncrypter implements Processor  {

	private static Logger log = LoggerFactory.getLogger(DokumentpakkeEncrypter.class);
	
	private LegalArchiveLogger legalArchive = null;
	private File trustStoreFile = null;
	private String trustStorePassword = null;
	private boolean certificateRevocationChecking = true;

	private CertValidator certValidator;
	
	private static final String ASICE_FILE_NAME = "post.asice";
	private static final String ENCRYPTED_DOKUMENTPAKKE_FILE_NAME = "encryptedDokumentpakke.bin";
	
	
	public void setupTruststore(){
		
		certValidator = TrustedCertificatesTools.loadTrustedCertificates(trustStoreFile, trustStorePassword, certificateRevocationChecking);
	}
	
	@Override
	public void process(Exchange exchangeIn) throws Exception {

		byte[] sertifikat = ((SendDigitalPost) exchangeIn.getIn().getBody()).getSendDigitalPostRequest().getSertifikat();
		String tempDirectoryPath = (String) exchangeIn.getIn().getHeader(KanalConstants.CAMEL_HEADER_TEMP_DIRECTORY);
		String encryptedDokumentpakkePath = tempDirectoryPath + ENCRYPTED_DOKUMENTPAKKE_FILE_NAME;
		
		File asiceFile = new File(tempDirectoryPath + ASICE_FILE_NAME);
		try (FileInputStream fis = new FileInputStream(asiceFile);
				FileOutputStream fos = new FileOutputStream(new File(encryptedDokumentpakkePath))) {

			validateCertificate(sertifikat);
			DocumentEncrypter.encryptDocument(fis, sertifikat, fos);
		}
		
		if(!asiceFile.delete()){
			throw new RuntimeCamelException("Could not delete asice file: " + asiceFile.getPath());
		}
		archiveReceiverCertificateInformation(exchangeIn, sertifikat);
		
		exchangeIn.getIn().setHeader(KanalConstants.CAMEL_HEADER_DOKUMENTPAKKE, encryptedDokumentpakkePath);
		
	}
	
	private void validateCertificate(byte[] certificate) throws CertificateException{
		log.info("Validating receiver certificate with revocationChecking set to " + certificateRevocationChecking);
		X509Certificate cert = new X509Certificate(certificate);
		try{
			certValidator.validate(cert);
			log.info("Certificate validated OK");
		} catch(Exception e){
			log.error("Could not validate cerificate for signing");
			throw new RuntimeException("Could not verfy signature cerificate", e);
		}
	}
	
	private void archiveReceiverCertificateInformation(Exchange exchangeIn, byte[] certificate) throws CertificateException{
		
		X509Certificate receiver = new X509Certificate(certificate);
		legalArchive.logEvent(exchangeIn, LogEvent.ASICE_KRYPTERT, receiver.getSubjectDN().toString()
				+ " serienummer på sertifikat=" + receiver.getSerialNumber().toString()
				, receiver.getEncoded());
		
	}
	
	public LegalArchiveLogger getLegalArchive() {
		return legalArchive;
	}

	public void setLegalArchive(LegalArchiveLogger legalArchive) {
		this.legalArchive = legalArchive;
	}

	public void setTrustStoreFile(File trustStoreFile) {
		this.trustStoreFile = trustStoreFile;
	}

	public void setTrustStorePassword(String trustStorePassword) {
		this.trustStorePassword = trustStorePassword;
	}

	public void setCertificateRevocationChecking(
			boolean certificateRevocationChecking) {
		this.certificateRevocationChecking = certificateRevocationChecking;
	}
}
