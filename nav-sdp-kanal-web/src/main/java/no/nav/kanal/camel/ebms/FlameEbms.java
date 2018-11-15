package no.nav.kanal.camel.ebms;

import java.io.FileReader;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.annotation.PostConstruct;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPathExpressionException;

import no.nav.kanal.KanalConstants;

import org.apache.camel.RuntimeCamelException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

import com.flame.client.as4.api.ClientFactory;
import com.flame.client.as4.api.ClientFactory.FMSArgumentException;
import com.flame.utils.StringHolder;

public class FlameEbms {

	protected static final Logger log = LoggerFactory.getLogger(FlameEbms.class);
	
	// initialized in this class
	protected ClientFactory clientFactory;
	protected Node pushModeDigital;
	protected Node pushModeFysisk;
	protected Node pullMode;

	// initialized by spring config
	private String ebmsEndpoint;
	private String keyStoreLocation;
	private String keyStorePassword;
	private String keyPassword;
	private String licenseFile;
	private String pmodePushDigitalConfigFile;
	private String pmodePushFysiskConfigFile;
	private String pmodePullConfigFile;

	public FlameEbms() {
		super();
	}

	public ClientFactory getClientFactory() {
		return clientFactory;
	}
	
	public Node getPushModeDigital() {
		return pushModeDigital;
	}
	
	public Node getPushModeFysisk() {
		return pushModeFysisk;
	}
	
	public Node getPullMode() {
		return pullMode;
	}
	
	@PostConstruct
	private void initialize() {
		log.info("FlameEbms is initializing");
		try {
			clientFactory = ClientFactory.getInstance(keyStoreLocation, keyStorePassword, keyPassword);
			ClientFactory.setLicenceFile(licenseFile);

			if(pmodePushFysiskConfigFile != null && !pmodePushFysiskConfigFile.isEmpty()){
				StringHolder pushEventHolder = new StringHolder(KanalConstants.EBMS_PMODE_EVENTID_FYSISK_PUSH);
				pushModeFysisk = clientFactory.createProcessingMode(new StreamSource(new FileReader(pmodePushFysiskConfigFile)), pushEventHolder);
			}
			if(pmodePushDigitalConfigFile != null && !pmodePushDigitalConfigFile.isEmpty()){
				StringHolder pushEventHolder = new StringHolder(KanalConstants.EBMS_PMODE_EVENTID_DIGITAL_PUSH);
				pushModeDigital = clientFactory.createProcessingMode(new StreamSource(new FileReader(pmodePushDigitalConfigFile)), pushEventHolder);
			}
			if(pmodePullConfigFile != null && !pmodePullConfigFile.isEmpty()){
				StringHolder pullEventHolder = new StringHolder(KanalConstants.EBMS_PMODE_EVENTID_PULL);
				pullMode = clientFactory.createProcessingMode(new StreamSource(new FileReader(pmodePullConfigFile)), pullEventHolder);
			}
			
		} catch (UnrecoverableKeyException | KeyManagementException
				| NoSuchAlgorithmException | CertificateException
				| KeyStoreException | IOException |XPathExpressionException | TransformerException
				| FMSArgumentException e) {
			throw new RuntimeCamelException("Error during initialize:" + e.getMessage(), e);
		}
	}
	
	public String getEbmsEndpoint() {
		return ebmsEndpoint;
	}

	public void setEbmsEndpoint(String ebmsEndpoint) {
		this.ebmsEndpoint = ebmsEndpoint;
	}

	public String getKeyStoreLocation() {
		return keyStoreLocation;
	}

	public void setKeyStoreLocation(String keyStoreLocation) {
		this.keyStoreLocation = keyStoreLocation;
	}

	public String getKeyStorePassword() {
		return keyStorePassword;
	}

	public void setKeyStorePassword(String keyStorePassword) {
		this.keyStorePassword = keyStorePassword;
	}

	public String getKeyPassword() {
		return keyPassword;
	}

	public void setKeyPassword(String keyPassword) {
		this.keyPassword = keyPassword;
	}

	public String getLicenseFile() {
		return licenseFile;
	}

	public void setLicenseFile(String licenseFile) {
		this.licenseFile = licenseFile;
	}

	public String getPmodePushDigitalConfigFile() {
		return pmodePushDigitalConfigFile;
	}

	public void setpmodePushDigitalConfigFile(String pmodePushConfigFile) {
		this.pmodePushDigitalConfigFile = pmodePushConfigFile;
	}
	
	public String getPmodePushFysiskConfigFile() {
		return pmodePushFysiskConfigFile;
	}

	public void setpmodePushFysiskConfigFile(String pmodePushConfigFile) {
		this.pmodePushFysiskConfigFile = pmodePushConfigFile;
	}

	public String getPmodePullConfigFile() {
		return pmodePullConfigFile;
	}

	public void setPmodePullConfigFile(String pmodePullConfigFile) {
		this.pmodePullConfigFile = pmodePullConfigFile;
	}

}