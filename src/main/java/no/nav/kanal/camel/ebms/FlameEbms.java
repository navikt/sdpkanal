package no.nav.kanal.camel.ebms;

import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

import javax.xml.transform.stream.StreamSource;

import no.nav.kanal.KanalConstants;

import no.nav.kanal.config.model.VaultCredentials;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.w3c.dom.Node;

import com.flame.client.as4.api.ClientFactory;
import com.flame.utils.StringHolder;

@Service
public class FlameEbms {

	protected static final Logger log = LoggerFactory.getLogger(FlameEbms.class);
	
	// initialized in this class
	private final ClientFactory clientFactory;
	private Node pushModeDigital;
	private Node pushModeFysisk;
	private Node pullMode;

	private final String keyPassword;
	private final String ebmsEndpoint;

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

	@Autowired
	public FlameEbms(
			@Value("${no.nav.sdpkanal.as4keystore}") String keyStoreLocation,
			@Value("${no.nav.sdpkanal.flameLicenseFile}") String licenseFile,
			@Value("${ebms.msh.url}") String ebmsEndpoint,

			VaultCredentials credentials
	) throws Exception {
		this.ebmsEndpoint = ebmsEndpoint;
		keyPassword = credentials.getFlameKeyPassword();
		log.info("FlameEbms is initializing");
		clientFactory = ClientFactory.getInstance(keyStoreLocation, credentials.getFlameKeystorePassword(), credentials.getFlameKeyPassword());
		ClientFactory.setLicenceFile(licenseFile);

		StringHolder pushPhysicalEventHolder = new StringHolder(KanalConstants.EBMS_PMODE_EVENTID_FYSISK_PUSH);
		pushModeFysisk = clientFactory.createProcessingMode(new StreamSource(new InputStreamReader(FlameEbms.class.getResourceAsStream("/flame/as4push-fysisk-nav-difi.pmode"))), pushPhysicalEventHolder);

		StringHolder pushDigitalEventHolder = new StringHolder(KanalConstants.EBMS_PMODE_EVENTID_DIGITAL_PUSH);
		pushModeDigital = clientFactory.createProcessingMode(new StreamSource(new InputStreamReader(FlameEbms.class.getResourceAsStream("/flame/as4push-digital-nav-difi.pmode"))), pushDigitalEventHolder);

		StringHolder pullEventHolder = new StringHolder(KanalConstants.EBMS_PMODE_EVENTID_PULL);
		pullMode = clientFactory.createProcessingMode(new StreamSource(new InputStreamReader(FlameEbms.class.getResourceAsStream("/flame/as4pull-nav-difi.pmode"))), pullEventHolder);
	}

	public String getEbmsEndpoint() {
		return ebmsEndpoint;
	}

	public String getKeyPassword() {
		return keyPassword;
	}

	static String readSecurityContext(String path) throws IOException {
		return IOUtils.toString(FlameEbms.class.getResourceAsStream(path), Charset.forName("UTF-8"));
	}
}
