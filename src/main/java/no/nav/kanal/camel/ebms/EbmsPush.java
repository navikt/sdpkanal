package no.nav.kanal.camel.ebms;

import java.net.ConnectException;
import java.net.UnknownHostException;
import java.util.Map;

import javax.xml.ws.soap.SOAPFaultException;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.digipost.api.MessageSender;
import no.digipost.api.representations.*;
import no.nav.kanal.camel.DocumentPackageCreator;
import no.nav.kanal.camel.XmlExtractor;
import no.nav.kanal.log.LegalArchiveLogger;
import no.nav.kanal.log.LogEvent;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.RuntimeCamelException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.unece.cefact.namespaces.standardbusinessdocumentheader.StandardBusinessDocument;

@Service
public class EbmsPush implements Processor {

	protected static final Logger log = LoggerFactory.getLogger(EbmsPush.class);
	private final String mpcNormal;
	private final String mpcPrioritert;
	private final LegalArchiveLogger legalArchive;
	private final long maxRetries;
	private final long retryIntervalInSeconds;
	private final DigipostEbms digipostEbms;

	@Autowired
	public EbmsPush(
			@Value("${ebms.mpc.normal}") String mpcNormal,
			@Value("${ebms.mpc.prioritert}") String mpcPrioritert,
			@Value("${ebms.push.maxRetries}") Long maxRetries,
			@Value("${ebms.push.retryIntervalInSeconds}") Long retryIntervalInSeconds,
			LegalArchiveLogger legalArchive,
			DigipostEbms digipostEbms
	) {
		this.mpcNormal = mpcNormal;
		this.mpcPrioritert = mpcPrioritert;
		this.legalArchive = legalArchive;
		this.maxRetries = maxRetries;
		this.retryIntervalInSeconds = retryIntervalInSeconds;
		this.digipostEbms = digipostEbms;
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
					log.debug("Waiting " + retryIntervalInSeconds + " before retrying push because of connection problems");
					Thread.sleep(retryIntervalInSeconds*1000);
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
		StandardBusinessDocument sbd = exchangeIn.getIn().getHeader(XmlExtractor.STANDARD_BUSINESS_DOCUMENT, StandardBusinessDocument.class);
		Boolean isPrioritized = exchangeIn.getIn().getHeader(XmlExtractor.HAS_PRIORITY, Boolean.class);

		Dokumentpakke documentPackage = exchangeIn.getIn().getHeader(DocumentPackageCreator.DOCUMENT_PACKAGE, Dokumentpakke.class);

		Organisasjonsnummer sbdhMottaker = Organisasjonsnummer.of(sbd.getStandardBusinessDocumentHeader().getReceivers().get(0).getIdentifier().getValue());
		String mpc = (isPrioritized) ? mpcPrioritert : mpcNormal;

		MessageSender messageSender = digipostEbms.getMessageSender();
		return messageSender.send(EbmsForsendelse
				.create(digipostEbms.getDatabehandler(), digipostEbms.getMottaker(), sbdhMottaker, sbd, documentPackage)
				.withMpcId(mpc)
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
