package no.nav.kanal.crypto;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NavKeysAndCertificates {
	
	private static Logger log = LoggerFactory.getLogger(NavKeysAndCertificates.class);
	
	private File keystoreFile = null;
	private String keystorePassword = null;
	private String serviceOwnerPrivateKeyAlias = null;
	private String serviceOwnerPrivateKeyPassword = null;

	private List<X509Certificate> certificateChain;
	private Key signingKey;
	
	
	public NavKeysAndCertificates(){
		
	}
	
	public void setupKeystore(){
		
		try {
			KeyStore keystore = loadKeystore(keystoreFile, keystorePassword);
			log.info("Loading NAV certificate chain from alias: " + serviceOwnerPrivateKeyAlias);
			Certificate[] certs = keystore.getCertificateChain(serviceOwnerPrivateKeyAlias);
			certificateChain = new ArrayList<X509Certificate>();
			for (int i = 0; i < certs.length; i++) {
				X509Certificate cert = (X509Certificate) certs[i];
				cert.checkValidity();
				certificateChain.add(cert);
				log.info("Imported certificate: " + cert.getSubjectDN());
			}
			signingKey = keystore.getKey(serviceOwnerPrivateKeyAlias, serviceOwnerPrivateKeyPassword.toCharArray());
			log.info("Loaded private key of type " + signingKey.getAlgorithm() + " in " + signingKey.getFormat() + " format");
			
		} catch (KeyStoreException e) {
			throw new RuntimeException("Could not load NAV certificate.", e);
		} catch (Exception e) {
			throw new RuntimeException("Could not load keystore.", e);
		}
		
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

	public void setKeystoreFile(File keystoreFile) {
		this.keystoreFile = keystoreFile;			
	}

	public void setKeystorePassword(String keystorePassword) {
		this.keystorePassword = keystorePassword;
	}


	public void setServiceOwnerPrivateKeyAlias(String serviceOwnerPrivateKeyAlias) {
		this.serviceOwnerPrivateKeyAlias = serviceOwnerPrivateKeyAlias;			
	}


	public void setServiceOwnerPrivateKeyPassword(
			String serviceOwnerPrivateKeyPassword) {
		this.serviceOwnerPrivateKeyPassword = serviceOwnerPrivateKeyPassword;
	}
}
