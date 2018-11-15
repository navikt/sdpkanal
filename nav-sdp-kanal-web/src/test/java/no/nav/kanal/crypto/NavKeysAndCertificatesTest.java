package no.nav.kanal.crypto;

import java.io.File;

import org.junit.Assert;
import org.junit.Test;

public class NavKeysAndCertificatesTest {

	@Test
	public void testNavKeysAndCertificates() throws InterruptedException {
		
		File keystoreFile = new File("src/test/resources/crypto/keystore.jks");
		String keystorePassword = "changeit";
		String serviceOwnerPrivateKeyAlias = "app-key";
		String serviceOwnerPrivateKeyPassword = "changeit";
		NavKeysAndCertificates nKC = null;
		try {
			nKC = new NavKeysAndCertificates();
			nKC.setKeystoreFile(keystoreFile);
			nKC.setKeystorePassword(keystorePassword);
			nKC.setServiceOwnerPrivateKeyAlias(serviceOwnerPrivateKeyAlias);
			nKC.setServiceOwnerPrivateKeyPassword(serviceOwnerPrivateKeyPassword);
			nKC.setupKeystore();
		} catch (Exception e) {
			Assert.fail("Execption thrown wile creating NavKeysAndCertificates");
		}
		
		Assert.assertEquals("Keychain lengt", 3, nKC.getCertificateChain().size());
		Assert.assertEquals("Certificate type", "X.509", nKC.getCertificateChain().get(0).getType());
		Assert.assertEquals("Private key algortihm", "RSA", nKC.getSigningKey().getAlgorithm());
		Assert.assertEquals("Private key format", "PKCS#8", nKC.getSigningKey().getFormat());
	}
	
	@Test
	public void testNavKeysAndCertificatesWrongKeystorePassword() throws InterruptedException {
		
		File keystoreFile = new File("src/test/resources/crypto/keystore.jks");
		String keystorePassword = "wrong";
		String serviceOwnerPrivateKeyAlias = "app-key";
		String serviceOwnerPrivateKeyPassword = "changeit";
		NavKeysAndCertificates nKC = null;
		try {
			nKC = new NavKeysAndCertificates();
			nKC.setKeystoreFile(keystoreFile);
			nKC.setKeystorePassword(keystorePassword);
			nKC.setServiceOwnerPrivateKeyAlias(serviceOwnerPrivateKeyAlias);
			nKC.setServiceOwnerPrivateKeyPassword(serviceOwnerPrivateKeyPassword);
			nKC.setupKeystore();
			Assert.fail("Exception should be thrown");
		} catch (Exception e) {
			Assert.assertTrue(true);
		}
	}
	
	@Test
	public void testNavKeysAndCertificatesWrongPrivateKeyPassword() throws InterruptedException {
		
		File keystoreFile = new File("src/test/resources/crypto/keystore.jks");
		String keystorePassword = "changeit";
		String serviceOwnerPrivateKeyAlias = "app-key";
		String serviceOwnerPrivateKeyPassword = "wrong";
		NavKeysAndCertificates nKC = null;
		try {
			nKC = new NavKeysAndCertificates();
			nKC.setKeystoreFile(keystoreFile);
			nKC.setKeystorePassword(keystorePassword);
			nKC.setServiceOwnerPrivateKeyAlias(serviceOwnerPrivateKeyAlias);
			nKC.setServiceOwnerPrivateKeyPassword(serviceOwnerPrivateKeyPassword);
			nKC.setupKeystore();
			Assert.fail("Exception should be thrown");
		} catch (Exception e) {
			Assert.assertTrue(true);
		}
	}
}
