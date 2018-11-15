package no.nav.kanal.crypto;

import java.io.File;

import org.hamcrest.core.IsInstanceOf;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class TrustedCertificatesToolsTest {

	@Rule
    public ExpectedException thrown = ExpectedException.none();
	
	@Test
	public void testLoadTrustedCertificates() {
		TrustedCertificatesTools.loadTrustedCertificates(new File("src/test/resources/crypto/u-truststore.jks"), "changeit", false);
	}
	
	@Test
	public void testLoadTrustedCertificatesMissingTruststore() {
		thrown.expect(java.lang.RuntimeException.class);
		thrown.expectCause(IsInstanceOf.<Throwable>instanceOf(java.io.FileNotFoundException.class));
		TrustedCertificatesTools.loadTrustedCertificates(new File("missingfile.jks"), "changeit", false);
	}
	
	@Test
	public void testLoadTrustedCertificatesWrongPassword() {
		thrown.expect(java.lang.RuntimeException.class);
		thrown.expectCause(IsInstanceOf.<Throwable>instanceOf(java.io.IOException.class));
		TrustedCertificatesTools.loadTrustedCertificates(new File("src/test/resources/crypto/u-truststore.jks"), "wrongpassword", false);
	}

	@Test
	public void testLoadTrustedCertificatesWithIgore() {
		TrustedCertificatesTools.loadTrustedCertificates(new File("src/test/resources/crypto/u-truststore.jks"), "changeit", false,"buypass");
	}

}
