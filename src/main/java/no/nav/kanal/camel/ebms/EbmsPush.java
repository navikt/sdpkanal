package no.nav.kanal.camel.ebms;

import java.net.ConnectException;
import java.net.UnknownHostException;

import javax.xml.ws.soap.SOAPFaultException;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.difi.begrep.sdp.schema_v10.SDPDigitalPost;
import no.difi.begrep.sdp.schema_v10.SDPMelding;
import no.digipost.api.MessageSender;
import no.digipost.api.PMode;
import no.digipost.api.representations.*;
import no.nav.kanal.SdpPayload;
import no.nav.kanal.camel.DocumentPackageCreator;
import no.nav.kanal.camel.XmlExtractor;
import no.nav.kanal.config.EbmsKt;
import no.nav.kanal.log.LegalArchiveLogger;
import no.nav.kanal.log.LogEvent;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.RuntimeCamelException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.unece.cefact.namespaces.standardbusinessdocumentheader.StandardBusinessDocument;

public class EbmsPush implements Processor {

	protected static final Logger log = LoggerFactory.getLogger(EbmsPush.class);
	private final LegalArchiveLogger legalArchive;
	private final long maxRetries;
	private final long retryInterval;
	private final MessageSender messageSender;
	private final EbmsAktoer datahandler;
	private final EbmsAktoer receiver;

	public EbmsPush(
			Long maxRetries,
			Long retryInterval,
			LegalArchiveLogger legalArchive,
			MessageSender messageSender,
			EbmsAktoer datahandler,
			EbmsAktoer receiver
	) {
		this.legalArchive = legalArchive;
		this.maxRetries = maxRetries;
		this.retryInterval = retryInterval;
		this.messageSender = messageSender;
		this.datahandler = datahandler;
		this.receiver = receiver;
	}

	@Override
    public void process(Exchange exchangeIn) throws Exception {
        log.info("EBMS is pushing");

		TransportKvittering reply;
		int retryCounter= 0;
		while(true) {
			try {
				if(retryCounter > 0){
					log.info("Push retry number " + retryCounter);
				}
				reply = createAndSendMessage(exchangeIn);
				break;
			} catch (SOAPFaultException e) {
				log.error("Error during transmit: " + e.getMessage());
				legalArchive.logEvent(exchangeIn, LogEvent.MELDING_SENDT_TIL_DIFI_FEILET, ". Retry number " + retryCounter + ". Error: " + e.getCause());
				// check if we should retry based on connection problems
				if(isConnectionProblem(e)) {
					if(++retryCounter == maxRetries) {
						log.warn("Maximum retries reached.");
						throw new RuntimeCamelException("Maximum retries reached and ClientException during push: " + e.getMessage(), e);
					}
					log.debug("Waiting " + retryInterval + " before retrying push because of connection problems");
					Thread.sleep(retryInterval);
				} else {
					log.warn("Not a temporary problem. No retry performed.");
					throw new RuntimeCamelException("ClientException during push: " + e.getMessage(), e);
				}
			}
		}

		System.out.println(reply.messageId);
		System.out.println(new ObjectMapper().writeValueAsString(reply));
		//handleResponse(reply, exchangeIn);
    }


	private TransportKvittering createAndSendMessage(Exchange exchangeIn) {
		StandardBusinessDocument sbd = exchangeIn.getIn().getHeader(XmlExtractor.SDP_PAYLOAD, SdpPayload.class).standardBusinessDocument;

		Dokumentpakke documentPackage = exchangeIn.getIn().getHeader(DocumentPackageCreator.DOCUMENT_PACKAGE, Dokumentpakke.class);

		Organisasjonsnummer sbdhMottaker = Organisasjonsnummer.of(sbd.getStandardBusinessDocumentHeader().getReceivers().get(0).getIdentifier().getValue());
		SDPDigitalPost sdpMelding = (SDPDigitalPost) sbd.getAny();

		return messageSender.send(EbmsForsendelse
				.create(datahandler, receiver, sbdhMottaker, sbd, documentPackage)
				.withMessageId(sbd.getStandardBusinessDocumentHeader().getBusinessScope().getScopes().get(0).getInstanceIdentifier())
				.withAction(sdpMelding.getDigitalPostInfo() == null ? PMode.Action.FORMIDLE_FYSISK : PMode.Action.FORMIDLE_DIGITAL)
				.withMpcId(exchangeIn.getIn().getHeader(EbmsKt.MPC_ID, String.class))
				.build());
	}


	/* Iterates through exception to see this is a connection problem. Breaks to make sure we don't check too deep */
	private boolean isConnectionProblem(Exception e) {
		int depth=0;
		Throwable cause = e.getCause();
		while(cause != null) {
			if(cause instanceof ConnectException || cause instanceof UnknownHostException){
				return true;
			}
			if(depth++ > 3){
				break;
			}
			cause = cause.getCause();
		}
		return false;
	}
}
