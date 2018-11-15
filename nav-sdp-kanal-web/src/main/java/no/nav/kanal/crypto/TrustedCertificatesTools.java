package no.nav.kanal.crypto;

import iaik.hlapi.CertValidator;
import iaik.hlapi.PkixCertValidator;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TrustedCertificatesTools {
	
	private static Logger log = LoggerFactory.getLogger(TrustedCertificatesTools.class);

	private TrustedCertificatesTools() {
		
	}
	
	public static  CertValidator loadTrustedCertificates(File trustStoreFile, String trustStorePassword, boolean certificateRevocationChecking){
		
		return loadTrustedCertificates(trustStoreFile, trustStorePassword, certificateRevocationChecking, null);
	}
		
	public static  CertValidator loadTrustedCertificates(File trustStoreFile, String trustStorePassword, boolean certificateRevocationChecking, String ignoreCertAlias){
		
		try {
			List<X509Certificate> certificateList = loadTrustStoreCertificates(trustStoreFile, trustStorePassword, ignoreCertAlias);
			
			CertValidator validator = new PkixCertValidator();
			for (X509Certificate x509Certificate : certificateList) {
				validator.addTrustedCertificate(x509Certificate);
				validator.addCertificate(x509Certificate);
			}
			validator.setRevocationChecking(certificateRevocationChecking);
			return validator;
			
		} catch (Exception e) {
			log.error("Could not load certificate truststore: " + e.getMessage());
			throw new RuntimeException("Could not load truststore", e);
		}
	}
	
	private static  List<X509Certificate> loadTrustStoreCertificates(File trustStoreFile, String trustStorePassword, String ignoreCertAlias) throws Exception{
		log.info("Loading trustore [" + trustStoreFile + "] ...");
		final KeyStore keyStore = KeyStore.getInstance("JKS");
		try(InputStream keystoreFileInputStream = new FileInputStream(trustStoreFile)) {
			keyStore.load(keystoreFileInputStream, trustStorePassword.toCharArray());
		}
		
		ArrayList<X509Certificate> trustedCertificates = new ArrayList<X509Certificate>();
		Enumeration<String> aliases =keyStore.aliases();
		while (aliases.hasMoreElements()) {
			String alias = aliases.nextElement();
			X509Certificate x509Certificate = (X509Certificate) keyStore.getCertificate(alias);
			if(!alias.equals(ignoreCertAlias)){
				log.info("\tAdding '" + x509Certificate.getSubjectDN() + "' to trusted certifcates");				
				trustedCertificates.add(x509Certificate);
			} else{
				log.info("\tSkipping certificate '" + x509Certificate.getSubjectDN() +"'" + " because the it has the ignored alias " + ignoreCertAlias);
			}
		}
		log.info("OK");
		return trustedCertificates;
    }
}
