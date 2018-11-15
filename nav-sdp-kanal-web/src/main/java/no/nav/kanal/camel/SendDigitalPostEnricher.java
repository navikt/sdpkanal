package no.nav.kanal.camel;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;

import no.difi.begrep.sdp.schema_v10.Dokument;
import no.difi.begrep.sdp.schema_v10.Manifest;
import no.nav.kanal.KanalConstants;
import no.nav.kanal.log.LegalArchiveLogger;
import no.nav.kanal.log.LogEvent;
import no.nav.tjeneste.virksomhet.digitalpost.senddigitalpost.v1.SendDigitalPost;
import no.nav.tjeneste.virksomhet.digitalpost.senddigitalpost.v1.meldinger.SendDigitalPostRequest;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.RuntimeCamelException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SendDigitalPostEnricher implements Processor {

    private static Logger log = LoggerFactory.getLogger(SendDigitalPostEnricher.class);

    private String remoteDokumentsPathPrefix = null;
    private String localDokumentsTempPath = null;
    private LegalArchiveLogger legalArchive = null;

    private static final String HREF_FILE_NAME_SPLITTER_REGEX = "/";

    @Override
    public void process(Exchange exchangeIn) throws Exception {
        log.debug("SendDigitalPostEnricher is processing");

        SendDigitalPost melding = (SendDigitalPost) exchangeIn.getIn().getBody();
        SendDigitalPostRequest requestMessage = melding.getSendDigitalPostRequest();
        
        String tempDirectoryPath = createTempDirectory(exchangeIn.getContext().getUuidGenerator().generateUuid());
        log.debug("Setting temporary folder for this exchange to '" + tempDirectoryPath + "'");
        exchangeIn.getIn().setHeader(KanalConstants.CAMEL_HEADER_TEMP_DIRECTORY, tempDirectoryPath);

        hentEksterneDokumenter(requestMessage.getManifest(), tempDirectoryPath);
        skrivManifestTilFil(requestMessage.getManifest(), tempDirectoryPath);
       
        legalArchive.logEvent(exchangeIn, LogEvent.MELDING_POPULERT_MED_DOKUMENTER);    	
        
    }

    private String createTempDirectory(String uuid) {
        File tempDirectory = new File(localDokumentsTempPath + "/" + uuid);
        if(!tempDirectory.mkdirs()){
        	throw new RuntimeCamelException("Could not create temp directory " + tempDirectory.getPath());
        }
        
        return tempDirectory.getAbsolutePath() + "/";
    }

    private void hentEksterneDokumenter(Manifest manifest, String tempDirectoryPath) {

        hentDokumentFil(manifest.getHoveddokument(), tempDirectoryPath);
        List<Dokument> vedleggListe = manifest.getVedlegg();
        for (Iterator<Dokument> iterator = vedleggListe.iterator(); iterator.hasNext();) {
            hentDokumentFil(iterator.next(), tempDirectoryPath);
        }
    }

    private void hentDokumentFil(Dokument dokument, String tempDirectoryPath) {

        File sourceFile = new File(remoteDokumentsPathPrefix + "/" + dokument.getHref());
        String dokumentFilnavn = extractFilenameFromHref(dokument.getHref());
        File destinationFile = new File(tempDirectoryPath + dokumentFilnavn);
        dokument.setHref(dokumentFilnavn);

        if (!sourceFile.canRead()) {
            throw new RuntimeCamelException("Remote file (" + sourceFile.getAbsolutePath() + ") could not be read for document " + dokument.getTittel().getValue());
        }

        try (FileInputStream fIn = new FileInputStream(sourceFile); FileOutputStream fOut = new FileOutputStream(destinationFile))
        {
            log.debug("Copying remote file (" + sourceFile.getCanonicalPath() + ") to local temp folder (" + destinationFile.getCanonicalPath() + ")");
            byte[] buffer = new byte[KanalConstants.SYSTEM_BLOCK_READ_SIZE];
            int readLength = fIn.read(buffer);
            while (readLength != -1) {
                fOut.write(buffer, 0, readLength);
                readLength = fIn.read(buffer);
            }
            fOut.flush();

        } catch (FileNotFoundException e) {
            throw new RuntimeCamelException("File could not be read.", e);
        } catch (IOException e) {
            throw new RuntimeCamelException("Error copying file (" + sourceFile.getAbsolutePath() + ") to (" + destinationFile.getAbsolutePath() + ")", e);
        }

    }

    private String extractFilenameFromHref(String href) {
        String[] splittedHref = href.split(HREF_FILE_NAME_SPLITTER_REGEX);
        log.debug("Extracting filename from href (" + href + ") to (" + splittedHref[splittedHref.length - 1] + ")");
        return splittedHref[splittedHref.length - 1];
    }

    private void skrivManifestTilFil(Manifest manifest, String tempDirectoryPath) {

        try {
            Marshaller marshaller = JAXBContext.newInstance(Manifest.class).createMarshaller();
            JAXBElement<Manifest> jaxbManifestElement = new JAXBElement<Manifest>(new QName("http://begrep.difi.no/sdp/schema_v10","manifest"), Manifest.class, manifest);
            marshaller.marshal(jaxbManifestElement, new File(tempDirectoryPath + "/" + KanalConstants.DOKUMENTPAKKE_MANIFEST_FILE_NAME));
        } catch (JAXBException e) {
            throw new RuntimeCamelException("Could not export manifest to file", e);
        }
    }

    public String getRemoteDokumentsPathPrefix() {
        return remoteDokumentsPathPrefix;
    }

    public void setRemoteDokumentsPathPrefix(String dokumentPathPrefix) {
        this.remoteDokumentsPathPrefix = dokumentPathPrefix;
    }

    public String getLocalDokumentsTempPath() {
        return localDokumentsTempPath;
    }

    public void setLocalDokumentsTempPath(String localDokumentsTempPath) {
        this.localDokumentsTempPath = localDokumentsTempPath;
    }

	public LegalArchiveLogger getLegalArchive() {
		return legalArchive;
	}

	public void setLegalArchive(LegalArchiveLogger legalArchive) {
		this.legalArchive = legalArchive;
	}

}
