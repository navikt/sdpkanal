package no.nav.kanal.camel;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import no.difi.begrep.sdp.schema_v10.Dokument;
import no.difi.begrep.sdp.schema_v10.Manifest;
import no.nav.kanal.KanalConstants;
import no.nav.kanal.config.NavKeysAndCertificates;
import no.nav.kanal.crypto.SignatureCreator;
import no.nav.kanal.log.LegalArchiveLogger;
import no.nav.kanal.log.LogEvent;
import no.nav.tjeneste.virksomhet.digitalpost.senddigitalpost.v1.SendDigitalPost;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.RuntimeCamelException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DokumentpakkeSigner implements Processor {

	private static Logger log = LoggerFactory.getLogger(DokumentpakkeSigner.class);

	private LegalArchiveLogger legalArchive = null;

	private static final String ASICE_FILE_NAME = "post.asice";
	private static final String SIGNATURE_FOLDER_NAME = "META-INF";
	private static final String SIGNATURE_FILE_NAME = "signatures.xml";
	
	private static final String MANIFEST_MIME_TYPE = "application/xml";
	private NavKeysAndCertificates navKeyAndCert = null;
	

	@Override
	public void process(Exchange exchangeIn) throws Exception {

		Manifest manifest = ((SendDigitalPost) exchangeIn.getIn().getBody()).getSendDigitalPostRequest().getManifest();
		String tempDirectoryPath = (String) exchangeIn.getIn().getHeader(KanalConstants.CAMEL_HEADER_TEMP_DIRECTORY);
		addXAdESSignature(tempDirectoryPath, manifest);
		
		File asiceFile = new File(tempDirectoryPath + ASICE_FILE_NAME);
		createAsiceFile(new File(tempDirectoryPath), asiceFile);
		logAsicToLegalArchive(exchangeIn, asiceFile);
		
	}

	private void addXAdESSignature(String directoryPath, Manifest manifest) {

		SignatureCreator signatureCreator = new SignatureCreator(navKeyAndCert);

		log.info("Creating signatures.xml");
		addDocumentToSignatures(manifest.getHoveddokument(), signatureCreator,directoryPath);
		
		for (Iterator<Dokument> iterator = manifest.getVedlegg().iterator(); iterator.hasNext();) {
			addDocumentToSignatures(iterator.next(), signatureCreator,directoryPath);
		}
		
		addManifestToSignatures(signatureCreator, directoryPath);
		
		String metaInfPath = directoryPath + SIGNATURE_FOLDER_NAME + "/";
		File metaInf = new File(metaInfPath);
		if(!metaInf.mkdirs()){
			throw new RuntimeCamelException("Could not create signature folder: " + metaInf.getPath());
		}
		File signatures = new File(metaInfPath + SIGNATURE_FILE_NAME);
		signatureCreator.writeXADESSignature(signatures);
		log.info(SIGNATURE_FILE_NAME +" successfully created");
	}

	private void addDocumentToSignatures(Dokument dokument, SignatureCreator signatureCreator, String tempDirectoryPath) {
		log.debug("Adding document {} with href {} of type {}" , new Object[]{
				(dokument.getTittel()==null?null:dokument.getTittel().getValue()),
				dokument.getHref(),
				dokument.getMime()});
		signatureCreator.addXADESFileReference(
				new File(tempDirectoryPath + dokument.getHref()),
				dokument.getMime());
	}
	
	private void addManifestToSignatures(SignatureCreator signatureCreator, String tempDirectoryPath){
		log.debug("Adding " + KanalConstants.DOKUMENTPAKKE_MANIFEST_FILE_NAME);
		signatureCreator.addXADESFileReference(new File(tempDirectoryPath + KanalConstants.DOKUMENTPAKKE_MANIFEST_FILE_NAME), MANIFEST_MIME_TYPE);
	}
	
	private void createAsiceFile(File inputDir, File outputFile) {
		
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try (ZipOutputStream out = new ZipOutputStream(bos)) {
			addFolder(out, inputDir.getCanonicalPath(), inputDir.getCanonicalPath());
		} catch (Exception e) {
			throw new RuntimeCamelException("Cold not create asice (zip-archive)", e);
		}
		deleteFiles(inputDir);
		try(FileOutputStream fos = new FileOutputStream(outputFile)) {
			bos.writeTo(fos);
		} catch (Exception e) {
			throw new RuntimeCamelException("Cold not write asice (zip-archive) to file", e);
		}
	}

	private void addFolder(ZipOutputStream zos,String fileName,String baseFolderName) throws IOException{
		File file = new File(fileName);
		if(file.exists()){
			if(file.isDirectory()){
				File[] directoryFiles = file.listFiles();
				for(int i=0; i<directoryFiles.length; i++){
					addFolder(zos,directoryFiles[i].getAbsolutePath(),baseFolderName);
				}
			} else {
				String entryName = fileName.substring(baseFolderName.length()+1,fileName.length());
				log.debug("Adding file " + entryName + " to asice (zip) archive");
				ZipEntry ze= new ZipEntry(entryName);
				zos.putNextEntry(ze);
				try (FileInputStream in = new FileInputStream(fileName)) {
					int lenght;
					byte[] buffer = new byte[4096];
					while ((lenght = in.read(buffer))> 0) {
						zos.write(buffer, 0, lenght);
					}
				}
				zos.closeEntry();
			}
		} else {
			throw new RuntimeCamelException("Could not add file to archive: " + file.getAbsolutePath());
		}
	}	
	
	private void deleteFiles(File folder){
		File[] files = folder.listFiles();
		for (int i = 0; i < files.length; i++) {
			if(files[i].isDirectory()){
				deleteFiles(files[i]);
			}
			if(!files[i].delete()){
				throw new RuntimeCamelException("Could not delete file: " + files[i].getPath());
			}
		}
	}
	
	private void logAsicToLegalArchive(Exchange exchangeIn, File asiceFile){
		
		try(FileInputStream fis = new FileInputStream(asiceFile); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
			byte[] buffer = new byte[KanalConstants.SYSTEM_BLOCK_READ_SIZE];
			int readLength = fis.read(buffer);
			while (readLength != -1) {
				bos.write(buffer, 0, readLength);
				readLength = fis.read(buffer);
			}
			bos.flush();
			
			legalArchive.logEvent(exchangeIn, LogEvent.ASICE_PRODUSERT, bos.toByteArray());				
			
			
		} catch (IOException e) {
			log.error("Can't log asice to legal archive. Cause: " + e);
			throw new RuntimeCamelException("Can't log asice to legal archive", e);
		}
	}
	
	public LegalArchiveLogger getLegalArchive() {
		return legalArchive;
	}

	public void setLegalArchive(LegalArchiveLogger legalArchive) {
		this.legalArchive = legalArchive;
	}

	public void setNavKeyAndCert(NavKeysAndCertificates navKeyAndCert) {
		this.navKeyAndCert = navKeyAndCert;
	}
}
