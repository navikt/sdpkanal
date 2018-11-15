package no.nav.kanal.crypto;

import iaik.hlapi.CertValidator;
import iaik.hlapi.HlApiException;
import iaik.hlapi.XMLDecrypterVerifier;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import no.nav.kanal.KanalConstants;
import no.nav.tjeneste.virksomhet.digitalpost.senddigitalpost.v1.SendDigitalPost;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SignatureValidator {
	
	private static Logger log = LoggerFactory.getLogger(SignatureValidator.class);
	
	private static final String SCHEMA_LOCATION_SOAP = "/wsdl/xsd/xmlsoap/envelope.xsd";
	private static final String SCHEMA_LOCATION_SBDH = "/wsdl/xsd/SBDH20040506-02/StandardBusinessDocumentHeader.xsd";
	
	private File trustStoreFile = null;
	private String trustStorePassword = null;
	private boolean certificateRevocationChecking = true;
	
	private CertValidator certValidator;
	private List<String> schemaURLs;
	
	public void setupTruststore(){
		
		certValidator = TrustedCertificatesTools.loadTrustedCertificates(trustStoreFile, trustStorePassword, certificateRevocationChecking);
		schemaURLs = getSchemaURLs();
	}
		
	private List<String> getSchemaURLs() {
		ArrayList<String> schemas = new ArrayList<String>();
		schemas.add(SendDigitalPost.class.getResource(SCHEMA_LOCATION_SOAP).toString());
		schemas.add(SendDigitalPost.class.getResource(SCHEMA_LOCATION_SBDH).toString());
		return schemas;
	}

	public String verifySBDSignature(String message) {
		
		try {
			XMLDecrypterVerifier verifier = new XMLDecrypterVerifier();
			
			for (int i = 0; i < schemaURLs.size(); i++) {
				verifier.addSchemaURL(schemaURLs.get(i));
			}
			
			ByteArrayInputStream signatureStream = new ByteArrayInputStream(message.getBytes());
			InputStream signedInputStream = verifier.process(signatureStream);
			ByteArrayOutputStream sigedOutputStream = new ByteArrayOutputStream();
			
			byte[] buffer = new byte[KanalConstants.SYSTEM_BLOCK_READ_SIZE];
			int readLength = signedInputStream.read(buffer);
			while (readLength != -1) {
				sigedOutputStream.write(buffer, 0, readLength);
				readLength = signedInputStream.read(buffer);
			}
			sigedOutputStream.flush();
			sigedOutputStream.close();
			log.info("Signature of message is valid, checking certificate with checkRevocation set to " + certificateRevocationChecking);
			X509Certificate[] signedCerts;
			try{
				signedCerts = verifier.verify(certValidator);				
			} catch(Exception e){
				log.error("Could not verfy signature cerificate");
				throw new RuntimeException("Could not verfy signature cerificate", e);
			}
			log.info("Certificate of signature is valid");
			String signatureCertificateInfo = "";
			for (int i = 0; i < signedCerts.length; i++) {
				if(i > 0){
					signatureCertificateInfo += "\r\n";
				}
				signatureCertificateInfo += "Cert " + (i+1)+ ":[Subject DN:(" +signedCerts[i].getSubjectDN() + ") + Issuer DN:(" + signedCerts[i].getIssuerDN() + ")]";
			}
			
			return signatureCertificateInfo;
			
		} catch (IOException | HlApiException e) {
			log.error("Signature could not be verified: " + e.getMessage());
			throw new RuntimeException("SBD signature could not be verified" , e);
		}
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
