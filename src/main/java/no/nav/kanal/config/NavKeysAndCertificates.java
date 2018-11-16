package no.nav.kanal.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.Key;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import no.nav.kanal.config.model.VaultCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;

@Service
public class NavKeysAndCertificates {
	private static Logger log = LoggerFactory.getLogger(NavKeysAndCertificates.class);

	private List<X509Certificate> certificateChain;
	private Key signingKey;

	@Autowired
	public NavKeysAndCertificates(
			@Value("${no.nav.sdpkanal.keystore}") String keystoreFile,
			VaultCredentials credentials
	) throws Exception {
			KeyStore keystore = loadKeystore(new File(keystoreFile), credentials.getVirksomhetKeystorePassword());
			log.info("Loading NAV certificate chain from alias: " + credentials.getVirksomhetKeystoreAlias());
			Certificate[] certs = keystore.getCertificateChain(credentials.getVirksomhetKeystoreAlias());
			certificateChain = new ArrayList<>();
			for (int i = 0; i < certs.length; i++) {
				X509Certificate cert = (X509Certificate) certs[i];
				cert.checkValidity();
				certificateChain.add(cert);
				log.info("Imported certificate: " + cert.getSubjectDN());
			}
			signingKey = keystore.getKey(credentials.getVirksomhetKeystoreAlias(), credentials.getVirksomhetKeystorePassword().toCharArray());
			log.info("Loaded private key of type " + signingKey.getAlgorithm() + " in " + signingKey.getFormat() + " format");

	}
	
	private KeyStore loadKeystore(File keystoreFile, String keystorePassword) throws Exception{
		log.info("Loading keystore [" + keystoreFile + "] ...");
		KeyStore keyStore = KeyStore.getInstance("JKS");
		try(InputStream keystoreFileInputStream = new FileInputStream(keystoreFile)) {
			keyStore.load(keystoreFileInputStream, keystorePassword.toCharArray());
		}
		log.info("OK");
		return keyStore;
	}
	
	public List<X509Certificate> getCertificateChain(){
		return certificateChain;
	}
	
	public Key getSigningKey(){
		return signingKey;
	}
}
