package no.nav.kanal.crypto;

import iaik.asn1.CodingException;
import iaik.asn1.ObjectID;
import iaik.asn1.structures.AlgorithmID;
import iaik.asn1.structures.Attribute;
import iaik.cms.ContentInfoOutputStream;
import iaik.cms.EnvelopedDataOutputStream;
import iaik.cms.KeyTransRecipientInfo;
import iaik.cms.RecipientInfo;
import iaik.cms.attributes.CMSContentType;
import iaik.x509.X509Certificate;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import no.nav.kanal.camel.DokumentpakkeEncrypter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DocumentEncrypter {
	
	private static Logger log = LoggerFactory.getLogger(DokumentpakkeEncrypter.class);
	
	private DocumentEncrypter() {
		
	}
	
	public static void encryptDocument(InputStream document, byte[] receiverx509Certificate, OutputStream ciphertext){
		
		try {
			X509Certificate receiver = new X509Certificate(receiverx509Certificate);
			log.info("Encrypting document to receiver. DN=" + receiver.getSubjectDN());
			log.debug("----------- Receiver certificate -----------\r\n{}", receiver.toString());
			log.debug("----------- END -----------");
			log.debug("Checking certificate validity");
			receiver.checkValidity();
						
			ContentInfoOutputStream contentInfoStream = new ContentInfoOutputStream(ObjectID.cms_envelopedData, ciphertext);
			
			EnvelopedDataOutputStream envelopedData = new EnvelopedDataOutputStream(contentInfoStream, (AlgorithmID)AlgorithmID.aes256_CBC.clone());
			
		    RecipientInfo[] recipients = new RecipientInfo[1];
		    recipients[0] = new KeyTransRecipientInfo(receiver, (AlgorithmID)AlgorithmID.rsaEncryption.clone());
		    envelopedData.setRecipientInfos(recipients);
		    		    
			CMSContentType contentType = new CMSContentType(ObjectID.cms_data);
			Attribute[] attributes = new Attribute[1];
			attributes[0] = new Attribute(contentType);
			envelopedData.setUnprotectedAttributes(attributes);
		    
			int blockSize = 4096;
			byte[] buffer = new byte[blockSize];
			int bytesRead;
			while ((bytesRead = document.read(buffer)) != -1) {
				envelopedData.write(buffer, 0, bytesRead);
			}
		    envelopedData.close();
		    
		} catch (NoSuchAlgorithmException ex) {
		    throw new RuntimeException("No implementation found for AES.", ex);
		} catch (CertificateException e) {
			throw new RuntimeException("Could not parse destination certificate", e);
		} catch (IOException e) {
			throw new RuntimeException("Could not read from source or write to destination", e);
		} catch (CodingException e) {
			throw new RuntimeException("Could not create ASN1 attribute", e);
		}		
	}

}
