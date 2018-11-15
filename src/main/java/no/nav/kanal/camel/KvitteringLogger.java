package no.nav.kanal.camel;

import no.nav.kanal.log.LegalArchiveLogger;
import no.nav.kanal.log.LogEvent;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KvitteringLogger implements Processor  {
	
	private static Logger log = LoggerFactory.getLogger(KvitteringLogger.class);
	
	private LegalArchiveLogger legalArchive = null;
	
	@Override
	public void process(Exchange exchange) throws Exception {

		legalArchive.logEvent(exchange, LogEvent.KVITTERING_LAGT_PA_KO);
		log.debug("Logged event for putting receipt on queue");
	}
	
	public void setLegalArchive(LegalArchiveLogger legalArchive) {
		this.legalArchive = legalArchive;
	}
	
}
