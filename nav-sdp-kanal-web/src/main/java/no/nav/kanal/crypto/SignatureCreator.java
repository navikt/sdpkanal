package no.nav.kanal.crypto;

import iaik.security.provider.IAIK;
import iaik.xml.crypto.XSecProvider;
import iaik.xml.crypto.utils.DOMUtils;
import iaik.xml.crypto.xades.CertID;
import iaik.xml.crypto.xades.DataObjectFormat;
import iaik.xml.crypto.xades.QualifyingProperties;
import iaik.xml.crypto.xades.QualifyingPropertiesFactory;
import iaik.xml.crypto.xades.SignedDataObjectProperties;
import iaik.xml.crypto.xades.SignedProperties;
import iaik.xml.crypto.xades.SignedSignatureProperties;
import iaik.xml.crypto.xades.SigningCertificate;
import iaik.xml.crypto.xades.SigningTime;
import iaik.xml.crypto.xades.XAdESSignature;
import iaik.xml.crypto.xades.impl.dom.XAdESSignatureFactory;
import iaik.xml.crypto.xades.impl.dom.properties.SigningTimeImpl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.xml.crypto.MarshalException;
import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignatureMethod;
import javax.xml.crypto.dsig.SignedInfo;
import javax.xml.crypto.dsig.Transform;
import javax.xml.crypto.dsig.XMLObject;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureException;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.keyinfo.X509Data;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class SignatureCreator {
	
	private static Logger log = LoggerFactory.getLogger(SignatureCreator.class);
	
	private static final String SHA256_DIGEST_IDENTIFIER = "http://www.w3.org/2001/04/xmlenc#sha256";
	private static final String RSA_SHA256_SIGNATURE_IDENTIFIER = "http://www.w3.org/2001/04/xmldsig-more#rsa-sha256";
	private static final String C14N11_CANONICALIZATION_IDENTIFIER = "http://www.w3.org/2006/12/xml-c14n11";
	private static final String C14N_TRANSFORM_IDENTIFIER = "http://www.w3.org/TR/2001/REC-xml-c14n-20010315";
	private static final String XADES_IDENTIFIER = "http://uri.etsi.org/2918/v1.2.1#";
	private static final String DIGITALPOST_NODE_LOCAL_NAME = "digitalPost";
	private static final String AVSENDER_NODE_LOCAL_NAME = "avsender";
	private static final String SIGNATURE_NODE_LOCAL_NAME = "Signature";
	private static final String XADES_FILE_REFERENCE_PREFIX = "FileID_";
	private static final String IAIK_PROVIDER_NAME = "XSECT";

	private XMLSignatureFactory sfac;
	private QualifyingPropertiesFactory qfac;
	private KeyInfoFactory kfac;
	private List<Reference> references;
	private List<DataObjectFormat> dataObjectFormats;
	private SignatureMethod signatureMethod;
	private CanonicalizationMethod canonicalizationMetod;
	private NavKeysAndCertificates navKeyAndCert;


	
	public SignatureCreator(NavKeysAndCertificates navKeyAndCert) {
		log.info("Adding IAIK as security provider");
		IAIK.addAsProvider();

		Security.addProvider(new XSecProvider());
		
		this.navKeyAndCert = navKeyAndCert;
			
		references = new ArrayList<>();
		dataObjectFormats = new ArrayList<>();

		Provider provider = Security.getProvider(IAIK_PROVIDER_NAME);
		// Temporary logging for debugging purposes
		log.info("TMP_DEBUG provider=" + provider + " class of provider=" + provider.getClass());
		log.info("TMP_DEBUG signature_factory=" + provider.getProperty("QualifyingPropertiesFactory.DOM"));
		try {
			Object instance = Class.forName(provider.getProperty("QualifyingPropertiesFactory.DOM")).newInstance();
			log.info("TMP_DEBUG instance=" + instance + "class=" + instance.getClass() + " instanceof=" + (instance instanceof QualifyingPropertiesFactory));
			qfac = (QualifyingPropertiesFactory) instance;
			log.info("TMP_DEBUG current classloader=\"" + getClass().getClassLoader() + "\" provider classloader=\"" + provider.getClass().getClassLoader() + "\" is same=" + (getClass().getClassLoader() == provider.getClass().getClassLoader()));
			//qfac = (QualifyingPropertiesFactory) provider.getClass().getClassLoader().loadClass(provider.getProperty("QualifyingPropertiesFactory.DOM")).newInstance();
		} catch (Exception e) {
			log.error("TMP_DEBUG Exception caught creating a QualifyingPropertiesFactory", e);
		}

		sfac = XAdESSignatureFactory.getInstance("DOM", provider);

		qfac = QualifyingPropertiesFactory.getInstance("DOM", provider);
		kfac = KeyInfoFactory.getInstance("DOM", provider);

		try {
			signatureMethod = sfac.newSignatureMethod(RSA_SHA256_SIGNATURE_IDENTIFIER, null);
			canonicalizationMetod = sfac.newCanonicalizationMethod(C14N11_CANONICALIZATION_IDENTIFIER, (C14NMethodParameterSpec) null);
		} catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException e) {
			throw new RuntimeException("Could not find hash algorithm.", e);
		}
		
	}
	
	public byte[] createSBDSignature(byte[] message){

		try {
			ByteArrayInputStream bis = new ByteArrayInputStream(message);
			
			Reference ref = sfac.newReference("",sfac.newDigestMethod(SHA256_DIGEST_IDENTIFIER, null),
				    Collections.nCopies(1,sfac.newTransform(Transform.ENVELOPED, (TransformParameterSpec) null)), null, null);
			
			SignedInfo si = sfac.newSignedInfo(canonicalizationMetod, signatureMethod, Collections.nCopies(1, ref));
			
			KeyInfo ki = createKeyInfo();
			
			@SuppressWarnings("deprecation")
			Document doc = DOMUtils.parse(bis);
			
			Node digitalPostNode = extractDigitalPostNode(doc);
			Node avsenderNode = extractAvsenderNode(digitalPostNode);
			
			removeSignatureNode(digitalPostNode);
			
			DOMSignContext dsc = new DOMSignContext(navKeyAndCert.getSigningKey(), digitalPostNode, avsenderNode);
			XMLSignature signature = sfac.newXMLSignature(si, ki);
			signature.sign(dsc);
			
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			
			DOMUtils.serialize(doc, bos);
			
			log.debug("SBD signature created");
			
			return bos.toByteArray();
			
		} catch (ParserConfigurationException | SAXException | IOException e) {
			throw new RuntimeException("Could not create SBD signature", e);
		} catch (NoSuchAlgorithmException |InvalidAlgorithmParameterException e) {
			throw new RuntimeException("Could not find algorithm to create SBD signature", e);
		} catch (MarshalException | XMLSignatureException | TransformerException e) {
			throw new RuntimeException("Could not sign SBD signature", e);
		}	
	}
	
	private Node extractDigitalPostNode(Document doc){
		Node digitalPostNode = null;
		NodeList sbdNodeChildren = doc.getFirstChild().getChildNodes();
		for (int i = 0; i < sbdNodeChildren.getLength(); i++) {
			if(DIGITALPOST_NODE_LOCAL_NAME.equals(sbdNodeChildren.item(i).getLocalName())){
				digitalPostNode = sbdNodeChildren.item(i);
				break;
			}
		}
		if(digitalPostNode == null){
			throw new RuntimeException("Could not create SBD signature: Missing required element 'digitalPost' in message");
		}
		return digitalPostNode;
	}
	
	private Node extractAvsenderNode(Node digitalPostNode){
		
		NodeList digitalPostNodeChildren = digitalPostNode.getChildNodes();
		
		Node avsenderNode = null; 
		for (int i = 0; i < digitalPostNodeChildren.getLength(); i++) {
			if(AVSENDER_NODE_LOCAL_NAME.equals(digitalPostNodeChildren.item(i).getLocalName())){
				avsenderNode = digitalPostNodeChildren.item(i);
				break;
			}
		}
		if(avsenderNode == null){
			throw new RuntimeException("Could not create SBD signature: Missing required element 'avsender' in message");
		}
		return avsenderNode;
	}
	
	private void removeSignatureNode(Node digitalPostNode){
		NodeList digitalPostNodeChildren = digitalPostNode.getChildNodes();
		Node signatureNode = null;
		for (int i = 0; i < digitalPostNodeChildren.getLength(); i++) {
			if(SIGNATURE_NODE_LOCAL_NAME.equals(digitalPostNodeChildren.item(i).getLocalName())){
				signatureNode = digitalPostNodeChildren.item(i);
			}
		}
		if(signatureNode != null){
			digitalPostNode.removeChild(signatureNode);
		}
	}
	
	

	public void addXADESFileReference(File file, String mime){
		
		byte[] digest = HashCalculator.getSHA256Sum(file);
		try {
			String referenceID = XADES_FILE_REFERENCE_PREFIX + references.size();
			Reference ref = sfac.newReference(file.getName(), sfac.newDigestMethod(SHA256_DIGEST_IDENTIFIER, null), null, null, referenceID, digest);
			references.add(ref);
		
			DataObjectFormat dobjformat = qfac.newDataObjectFormat(null, null, mime, null, "#" + referenceID);
			dataObjectFormats.add(dobjformat);
			
		} catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException e) {
			throw new RuntimeException("Could not find hash algorithm.", e);
		}
		log.debug("XADES filereference added");
	}
	
	public void writeXADESSignature(File signatureFile){
		
		Document mainifsetDOM = createXADESSignatureDOM();
			
		try(FileOutputStream fos = new FileOutputStream(signatureFile)) {
			
			TransformerFactory tf2 = TransformerFactory.newInstance();
			Transformer trans2;
			try {
				trans2 = tf2.newTransformer();
				trans2.transform(new DOMSource(mainifsetDOM), new StreamResult(fos));
			} catch (TransformerException e) {
				throw new RuntimeException("Could not transform manifest DOM into XML.", e);
			}
		} catch (IOException e1) {
			throw new RuntimeException("Could not write manifest to file.", e1);
		}
		log.debug("XADES signature written");
	}
	
	private Document createXADESSignatureDOM(){
		
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setNamespaceAware(true);
			Document doc = dbf.newDocumentBuilder().newDocument();
			doc.setXmlStandalone(true);
			Element root = doc.createElementNS(XADES_IDENTIFIER, "XAdESSignatures");
			root.appendChild(doc.adoptNode(calculateXADESSignatureNode()));
			doc.appendChild(root);
			return doc;
			
			
		} catch (ParserConfigurationException e) {
			throw new RuntimeException("Could not create manifest DOM.", e);
		}
		
	}
	
	private Node calculateXADESSignatureNode(){
		
		SignedInfo si = createXADESSignedInfo();
		KeyInfo ki = createKeyInfo();
		QualifyingProperties qp = createXADESQualifyingProperties();
		
		
		XMLObject qpXMLObject = sfac.newXMLObject(Collections.nCopies(1, qp), null, null, null);
		List<XMLObject> xmlObjects = new ArrayList<XMLObject>();
		xmlObjects.add(qpXMLObject);
		
		DOMSignContext context = createXADESDOMSignContext();
		XAdESSignature signature = (XAdESSignature) sfac.newXMLSignature(si, ki, xmlObjects, "Signature", "SignatureValue");
		try {
			signature.sign(context);
		} catch (MarshalException | XMLSignatureException e) {
			throw new RuntimeException("Error while signing", e);
		} 
		return context.getParent().getFirstChild();
		
	}
	
	private KeyInfo createKeyInfo(){
		
		X509Data x509Data = kfac.newX509Data(navKeyAndCert.getCertificateChain());
		List<X509Data> keyInfoContent = new ArrayList<X509Data>();
		keyInfoContent.add(x509Data);
		return kfac.newKeyInfo(keyInfoContent, "KeyInfo");
	}
	
	private SignedInfo createXADESSignedInfo(){
		
		Reference spRef;
		try {
			Transform transform = sfac.newTransform(C14N_TRANSFORM_IDENTIFIER, (TransformParameterSpec) null);
			spRef = sfac.newReference("#SignedProperties", sfac.newDigestMethod(SHA256_DIGEST_IDENTIFIER, null), Collections.nCopies(1, transform), SignedProperties.REFERENCE_TYPE, null);
			references.add(spRef);
		} catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException e) {
			throw new RuntimeException("Could not find hash algorithm.", e);
		}
		return sfac.newSignedInfo(canonicalizationMetod, signatureMethod, references, "SignedInfo");
	}
	
	private QualifyingProperties createXADESQualifyingProperties(){
		try {
			List<CertID> certIDs = new ArrayList<CertID>();
			for (Iterator<X509Certificate> iterator = navKeyAndCert.getCertificateChain().iterator(); iterator.hasNext();) {
				certIDs.add(qfac.newCertID(null, (X509Certificate) iterator.next(), sfac.newDigestMethod(DigestMethod.SHA1, null)));
				break;
			}
			SigningCertificate sc = qfac.newSigningCertificate(certIDs);

			SigningTime st = new SigningTimeImpl();
			SignedSignatureProperties ssp = qfac.newSignedSignatureProperties(st, sc, null,	null, null, null);
			
			SignedDataObjectProperties sdp = qfac.newSignedDataObjectProperties(dataObjectFormats, null, null, null, null);
			
			SignedProperties sp = qfac.newSignedProperties(ssp, sdp, "SignedProperties");
			return qfac.newQualifyingProperties(sp, "#Signature", null);
		} catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException e) {
			throw new RuntimeException("Could not find hash algorithm.", e);
		}
	}
	
	private DOMSignContext createXADESDOMSignContext(){
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setNamespaceAware(true);
			
			Document doc = dbf.newDocumentBuilder().newDocument();	

			DOMSignContext context = new DOMSignContext(navKeyAndCert.getSigningKey(), doc);
			context.putNamespacePrefix(XMLSignature.XMLNS, "");
			context.putNamespacePrefix(XAdESSignature.XMLNS_1_3_2, "");
			
			return context;
			
		} catch (ParserConfigurationException e) {
			throw new RuntimeException("Could not create documentBuilder.", e);
		}
	}
}
