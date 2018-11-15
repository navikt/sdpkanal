package no.nav.kanal.crypto;

import iaik.asn1.CodingException;
import iaik.asn1.structures.Attribute;
import iaik.cms.CMSException;
import iaik.cms.EnvelopedDataStream;
import iaik.cms.attributes.CMSContentType;
import iaik.security.provider.IAIK;
import iaik.utils.Util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;

import no.nav.kanal.config.NavKeysAndCertificates;
import no.nav.kanal.config.model.VaultCredentials;
import org.junit.Assert;
import org.junit.Test;

public class DocumentEncrypterTest {

	private VaultCredentials vaultCredentials = createVaultCredentials();

	public VaultCredentials createVaultCredentials() {
		VaultCredentials vaultCredentials = new VaultCredentials();
		vaultCredentials.setVirksomhetKeystorePassword("changeit");
		vaultCredentials.setVirksomhetKeystoreAlias("app-key");
		return vaultCredentials;
	}

	@Test
    public void testEncryptDocument() throws Exception{
		IAIK.addAsProvider();

		NavKeysAndCertificates nKC = new NavKeysAndCertificates("src/test/resources/crypto/keystore.jks", vaultCredentials);

		X509Certificate cert = nKC.getCertificateChain().get(0);
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		File inputfile = new File("src/test/resources/testSHA256.bmp");
		
		try {
			DocumentEncrypter.encryptDocument(new FileInputStream(inputfile), cert.getEncoded(), bos);
		} catch (CertificateEncodingException | FileNotFoundException e) {
			e.printStackTrace();
			Assert.fail();
		}
		Assert.assertTrue("Ciphertext should be greater than zero", bos.size() > 0);
		
		byte[] decryptedHash =  getDecryptedPlaintextHash(nKC.getSigningKey(), bos.toByteArray());

		byte[] inputHash = HashCalculator.getSHA256Sum(inputfile);
		
		Assert.assertEquals("The length of the source file and the decrypted ciphertext should be equal", inputHash.length, decryptedHash.length);
		for (int i = 0; i < inputHash.length; i++) {
			Assert.assertEquals("The source file and the decrypted ciphertext should be equal", inputHash[i], decryptedHash[i]);
		}
	}
	
	private byte[] getDecryptedPlaintextHash(Key signingKey, byte[] ciphertext){
		byte[] plaintext = decryptCiphertext(ciphertext, (PrivateKey)signingKey, 0);
		ByteArrayInputStream bis = new ByteArrayInputStream(plaintext);
		File outputFile = new File("target/test/plaintext.bmp");
		if(outputFile.exists()){
			outputFile.delete();
		}
		
		try {
			FileOutputStream fos = new FileOutputStream(outputFile);
			Util.copyStream(bis, fos, null);
		} catch ( IOException e) {
			e.printStackTrace();
			Assert.fail();
		}
		
		return HashCalculator.getSHA256Sum(outputFile);
		
	}
	
	private byte[] decryptCiphertext(byte[] encoding, PrivateKey privateKey, int recipientInfoIndex){
		
	    try {
	    	// create the EnvelopedData object from a DER encoded byte array
	    	// we are testing the stream interface
	    	ByteArrayInputStream is = new ByteArrayInputStream(encoding);
	    	
	    	EnvelopedDataStream enveloped_data = new EnvelopedDataStream(is);
	    	
	    	// decrypt the message
	      enveloped_data.setupCipher(privateKey, recipientInfoIndex);
	      InputStream decrypted = enveloped_data.getInputStream();
	      ByteArrayOutputStream os = new ByteArrayOutputStream();
	      Util.copyStream(decrypted, os, null);
	      
	      // get any unprotected attributes:
	      Attribute[] attributes = enveloped_data.getUnprotectedAttributes();
	      if ((attributes != null) && (attributes.length > 0)) {
	        System.out.println("Attributes included: ");
	        // we know we have used content type
	        CMSContentType contentType = (CMSContentType)attributes[0].getAttributeValue();
	        System.out.println(contentType);  
	      }  

	      return os.toByteArray();

	    } catch (InvalidKeyException |NoSuchAlgorithmException | CodingException | CMSException | IOException ex) {
	      throw new RuntimeException();
	    } 
	    
		
	}
}
