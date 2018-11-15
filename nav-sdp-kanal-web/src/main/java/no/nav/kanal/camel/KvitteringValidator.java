package no.nav.kanal.camel;

import no.nav.kanal.crypto.SignatureValidator;
import no.nav.kanal.log.LegalArchiveLogger;
import no.nav.kanal.log.LogEvent;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KvitteringValidator implements Processor {

	private static Logger log = LoggerFactory.getLogger(KvitteringValidator.class);
	
	private LegalArchiveLogger legalArchive = null;
	private SignatureValidator signatureValidator = null;
	
	@Override
	public void process(Exchange exchange) throws Exception {

		String message = (String) exchange.getIn().getBody();
				
		String signerInfo = signatureValidator.verifySBDSignature(message);
		legalArchive.logEvent(exchange, LogEvent.KVITTERING_SIGNATUR_VALIDERT_OK, "Kvittering signert av: " + signerInfo);
		log.debug("Logged event for receipt signature OK");
	}
	
	public void setLegalArchive(LegalArchiveLogger legalArchive) {
		this.legalArchive = legalArchive;
	}

	public void setSignatureValidator(SignatureValidator signatureValidator) {
		this.signatureValidator = signatureValidator;
	}
	

}
