package no.nav.kanal.selftest;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.net.ssl.HttpsURLConnection;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.kanal.selftest.SelfTestResult.Status;

public class SelfTest {
	
	private static final String DESC_NFS = "Tester at NFS område er tilgjengelig.";
	private static final String DESC_DIFI = "Tester at DIFI AS4 grensesnitt er tilgjengelig.";
	private static final String DESC_JURIDISKLOGG = "Tester at Juridisk logg webservice er tilgjengelig.";
	private static final String DESC_MQQMGR = "Tester tilgjengelighet for MQ QueueManager.";
	private static final String DESC_TRUSTSTORE = "Tester trustore";
	private static final String DESC_KEYSTORE = "Tester keystore";
	private static final String DESC_FLAME_KEYSTORE = "Tester keystore for Flame AS4 Client";
	
	// Must match alias for MQ in app-config.xml
	private static final String MQGATEWAY_ALIAS = "mqGateway04";
	
	private static Logger log = LoggerFactory.getLogger(SelfTest.class);
	
	List<SelfTestResult> results = new ArrayList<SelfTestResult>();
	int returnCode = 200;
	
	public SelfTest() {		
	}
	
	public void refresh(HttpServletRequest request) {

		results.clear();
		setReturnCode(200);

		results.add(testNFS());
		results.add(testJuridiskLogg());
		if(!System.getProperty("environment.class").equalsIgnoreCase("u")) {
			results.add(testEbmdMSH());
		}
		results.add(testMQ());
		if(request.getParameter("testKeystores") != null) 
		{
			results.add(testTrustStore());
			results.add(testKeyStore());
			results.add(testFlameKeyStore());			
		}
		
		if(hasErrors(results)){
			setReturnCode(500);
		}
	}
	
	private boolean hasErrors(List<SelfTestResult> results) {
		Iterator<SelfTestResult> it = results.iterator();
		while(it.hasNext()) {
			if(it.next().getStatus() != Status.OK) {
				return true;
			}
		}
		return false;
	}
	
	private SelfTestResult testNFS() {
		long start = System.currentTimeMillis();

		Status status = Status.OK;
		String message = "";
		String nfspath = null;
		String stacktrace = null;
		try {
			nfspath = System.getProperty("no.nav.kanal.dokument.path.prefix");
			File nfs = new File(nfspath);
			boolean ok = nfs.isDirectory() && nfs.exists();
			if(ok) {
				message = "NFS available on path : " + nfspath;
			} else {
				message = "NFS unavailable on path : " + nfspath;
				status = Status.ERROR;
			}
			
		} catch (Exception e) {
			status = Status.ERROR;
			message = e.getMessage();
			stacktrace = getStackTrace(e);			
		}
		long stop = System.currentTimeMillis();
		log.debug(message);
		return new SelfTestResult(nfspath, status, "NFS", message, (status==Status.ERROR?message:null), stacktrace, stop-start + " ms", DESC_NFS);
	}
	
	private SelfTestResult testJuridiskLogg() {
		long start = System.currentTimeMillis();

		Status status = Status.OK;
		String message = "";
		String endpoint = System.getProperty("no.nav.sdp-kanal.legal.archive.service.endpoint.url");
		String stacktrace = null;
		try {
			
			int responseCode = 0;
			if(endpoint.startsWith("https")) {
				responseCode = sendHttpsGet(endpoint + "?wsdl");
			} else {
				responseCode = sendHttpGet(endpoint + "?wsdl");
			}

			if(responseCode == -1) {
				status = Status.ERROR;
				message = "Juridisk logg unavailable on: " + endpoint;
			} else {
				message = "Juridisk logg available on: " + endpoint + "\n HTTP returncode= " + responseCode;
			}
		} catch (Exception e) {
			status = Status.ERROR;
			message = "URL:" + endpoint + " Errormessage: " + e.getMessage();
			stacktrace = getStackTrace(e);
		}
		long stop = System.currentTimeMillis();
		log.debug(message);
		return new SelfTestResult(endpoint, status, "JURIDISK_LOGG", message, (status==Status.ERROR?message:null), stacktrace, stop-start + " ms", DESC_JURIDISKLOGG);
	}
	
	private SelfTestResult testEbmdMSH() {
		long start = System.currentTimeMillis();

		Status status = Status.OK;
		String message = "";
		String endpoint = System.getProperty("no.nav.sdp-kanal.ebms.endpoint.url");
		String stacktrace = null;
		try {			
			int responseCode = 0;
			if(endpoint.startsWith("https")) {
				responseCode = sendHttpsGet(endpoint);
			} else {
				responseCode = sendHttpGet(endpoint);
			}
			
			if(responseCode == -1) {
				status = Status.ERROR;
				message = "EBMS MSH unavailable on: " + endpoint;
			} else {
				message = "EBMS MSH available on: " + endpoint + "\n HTTP returncode= " + responseCode;
			}
		} catch (Exception e) {
			status = Status.ERROR;
			message = "URL:" + endpoint + " Errormessage: " + e.getMessage();
			stacktrace = getStackTrace(e);
		}
		long stop = System.currentTimeMillis();
		log.debug(message);
		return new SelfTestResult(endpoint, status, "EBMS_MSH", message, (status==Status.ERROR?message:null), stacktrace, stop-start + " ms", DESC_DIFI);
	}

	private SelfTestResult testMQ() {
		long start = System.currentTimeMillis();

		Status status = Status.OK;
		String message = "";
		String endpoint = "java:jboss/MQQM";
		String stacktrace = null;
		try {
			Context initContext = new InitialContext();
			ConnectionFactory mqCF = (ConnectionFactory) initContext.lookup("java:jboss/MQQM");
			Connection conn = mqCF.createConnection("srvappserver", "");
			conn.close();
			message = "Connected to MQ. "
					+ " Host:" + System.getProperty(MQGATEWAY_ALIAS + ".hostname")
					+ " Port:" + System.getProperty(MQGATEWAY_ALIAS + ".port")
					+ " Channel:" + System.getProperty(MQGATEWAY_ALIAS + ".channel")
					+ " Name:" + System.getProperty(MQGATEWAY_ALIAS + ".name");
		} catch (Exception e) {
			status = Status.ERROR;
			message = e.getMessage();
			stacktrace = getStackTrace(e);
		}
		long stop = System.currentTimeMillis();
		log.debug(message);
		return new SelfTestResult(endpoint, status, "MQ", message, (status==Status.ERROR?message:null), stacktrace, stop-start + " ms", DESC_MQQMGR);
	}
	
	private SelfTestResult testTrustStore() {
		long start = System.currentTimeMillis();

		String fileSeparator = System.getProperty("file.separator");
		
		Status status = Status.OK;
		String endpoint = System.getProperty("no.nav.sdp-kanal.truststore.directory") + fileSeparator
				+ System.getProperty("environment.class") + "-" + System.getProperty("no.nav.sdp-kanal.truststore.basefilename");
		String message = "Path: " + endpoint + "<BR>";
		String stacktrace = null;
		try {
			final KeyStore keyStore = KeyStore.getInstance("JKS");
			try(InputStream keystoreFileInputStream = new FileInputStream(endpoint)) {
				keyStore.load(keystoreFileInputStream, System.getProperty("no.nav.sdp-kanal.truststore.credential.password").toCharArray());
			}
			
			Enumeration<String> aliases = keyStore.aliases();
			while (aliases.hasMoreElements()) {
				String alias = aliases.nextElement();
				X509Certificate x509Certificate = (X509Certificate) keyStore.getCertificate(alias);
				message += "Alias: " + alias + "<BR>"
						+ "Serial: " + x509Certificate.getSerialNumber() + "<BR>"
						+ "IssuerDN " + x509Certificate.getIssuerDN() + "<BR>"
						+ "From: " + x509Certificate.getNotBefore() + "<BR>"
						+ "To: " + x509Certificate.getNotAfter() + "<BR><BR>";
			}
			
		} catch (Exception e) {
			status = Status.ERROR;
			message = e.getMessage();
			stacktrace = getStackTrace(e);
		}
		long stop = System.currentTimeMillis();
		log.debug(message);
		return new SelfTestResult(endpoint, status, "Truststore", message, (status==Status.ERROR?message:null), stacktrace, stop-start + " ms", DESC_TRUSTSTORE);
	}
	
	private SelfTestResult testKeyStore() {
		long start = System.currentTimeMillis();

		String fileSeparator = System.getProperty("file.separator");
		
		Status status = Status.OK;
		String endpoint = System.getProperty("no.nav.sdp-kanal.keystore.virksomhet.directory") + fileSeparator
				+ System.getProperty("environment.class") + "-" + System.getProperty("no.nav.sdp-kanal.keystore.virksomhet.basefilename");
		String message = "Path: " + endpoint + "<BR>";
		String stacktrace = null;
		try {
			final KeyStore keyStore = KeyStore.getInstance("JKS");
			try(InputStream keystoreFileInputStream = new FileInputStream(endpoint)) {
				keyStore.load(keystoreFileInputStream, System.getProperty("no.nav.sdp-kanal.keystore.virksomhet.credential.password").toCharArray());
			}
			
			Enumeration<String> aliases = keyStore.aliases();
			while (aliases.hasMoreElements()) {
				String alias = aliases.nextElement();
				X509Certificate x509Certificate = (X509Certificate) keyStore.getCertificate(alias);
				message += "Alias: " + alias + "<BR>"
						+ "Serial: " + x509Certificate.getSerialNumber() + "<BR>"
						+ "IssuerDN " + x509Certificate.getIssuerDN() + "<BR>"
						+ "From: " + x509Certificate.getNotBefore() + "<BR>"
						+ "To: " + x509Certificate.getNotAfter() + "<BR><BR>";
			}
			
		} catch (Exception e) {
			status = Status.ERROR;
			message = e.getMessage();
			stacktrace = getStackTrace(e);
		}
		long stop = System.currentTimeMillis();
		log.debug(message);
		return new SelfTestResult(endpoint, status, "Keystore", message, (status==Status.ERROR?message:null), stacktrace, stop-start + " ms", DESC_KEYSTORE);
	}
	
	private SelfTestResult testFlameKeyStore() {
		long start = System.currentTimeMillis();

		String fileSeparator = System.getProperty("file.separator");
		
		Status status = Status.OK;		
		String endpoint = System.getProperty("no.nav.sdp-kanal.ebms.configdir")
				+ fileSeparator + "flame" + fileSeparator 
				+ System.getProperty("environment.class") + "-as4clientkeystore.jks";
		String message = "Path: " + endpoint + "<BR>";
		String stacktrace = null;
		try {
			final KeyStore keyStore = KeyStore.getInstance("JKS");
			try(InputStream keystoreFileInputStream = new FileInputStream(endpoint)) {
				keyStore.load(keystoreFileInputStream, System.getProperty("no.nav.sdp-kanal.keystore.ebms.credential.password").toCharArray());
			}
			
			Enumeration<String> aliases = keyStore.aliases();
			while (aliases.hasMoreElements()) {
				String alias = aliases.nextElement();
				X509Certificate x509Certificate = (X509Certificate) keyStore.getCertificate(alias);
				message += "Alias: " + alias + "<BR>"
						+ "Serial: " + x509Certificate.getSerialNumber() + "<BR>"
						+ "IssuerDN " + x509Certificate.getIssuerDN() + "<BR>"
						+ "From: " + x509Certificate.getNotBefore() + "<BR>"
						+ "To: " + x509Certificate.getNotAfter() + "<BR><BR>";
			}
			
		} catch (Exception e) {
			status = Status.ERROR;
			message = e.getMessage();
			stacktrace = getStackTrace(e);
		}
		long stop = System.currentTimeMillis();
		log.debug(message);
		return new SelfTestResult(endpoint, status, "Flame AS4 Keystore", message, (status==Status.ERROR?message:null), stacktrace, stop-start + " ms", DESC_FLAME_KEYSTORE);
	}

	private int sendHttpsGet(String endpoint) throws IOException {
		URL url = new URL(endpoint);
		HttpsURLConnection con = (HttpsURLConnection) url.openConnection();		
		int response = con.getResponseCode();
		con.disconnect();
		return response;
	}
	
	private int sendHttpGet(String endpoint) throws IOException {
		URL url = new URL(endpoint);
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		int response = con.getResponseCode();
		con.disconnect();
		return response;
	}
	
	public int getReturnCode() {
		return returnCode;
	}
	
	public void setReturnCode(int returnCode) {
		this.returnCode = returnCode;
	}
	
	public List<SelfTestResult> getResults() {
		return results;
	}

	private static String getStackTrace(Throwable ex) { 
        StringWriter sw = new StringWriter();
        ex.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }
}
