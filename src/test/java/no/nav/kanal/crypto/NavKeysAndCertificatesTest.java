package no.nav.kanal.crypto;

import no.nav.kanal.config.NavKeysAndCertificates;
import no.nav.kanal.config.model.VaultCredentials;
import org.junit.Assert;
import org.junit.Test;

public class NavKeysAndCertificatesTest {
	private VaultCredentials vaultCredentials = createVaultCredentials();

	public VaultCredentials createVaultCredentials() {
		VaultCredentials vaultCredentials = new VaultCredentials();
		vaultCredentials.setVirksomhetKeystorePassword("changeit");
		vaultCredentials.setVirksomhetKeystoreAlias("app-key");
		return vaultCredentials;
	}

	@Test
	public void testNavKeysAndCertificates() throws Exception {
		NavKeysAndCertificates nKC = new NavKeysAndCertificates("src/test/resources/crypto/keystore.jks", vaultCredentials);

		Assert.assertEquals("Keychain lengt", 3, nKC.getCertificateChain().size());
		Assert.assertEquals("Certificate type", "X.509", nKC.getCertificateChain().get(0).getType());
		Assert.assertEquals("Private key algortihm", "RSA", nKC.getSigningKey().getAlgorithm());
		Assert.assertEquals("Private key format", "PKCS#8", nKC.getSigningKey().getFormat());
	}
}
