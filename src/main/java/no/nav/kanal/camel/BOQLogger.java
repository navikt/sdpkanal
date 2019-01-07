package no.nav.kanal.camel;

import no.nav.kanal.log.LegalArchiveLogger;
import no.nav.kanal.log.LogEvent;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BOQLogger implements Processor  {
	
	private static Logger log = LoggerFactory.getLogger(BOQLogger.class);
	
	private final LegalArchiveLogger legalArchive;
	public BOQLogger(LegalArchiveLogger legalArchive) {
		this.legalArchive = legalArchive;
	}
	
	@Override
	public void process(Exchange exchange) throws Exception {
		Exception e = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
		log.error("Sending message to backout, exception caught was", e);
		
		String cause ="";
		int causeNo = 1;
		Throwable parentThrowable = e;
		while(parentThrowable != null){
			cause +="\r\nCause " + causeNo + ":[" + parentThrowable.getMessage() + "]";
			causeNo = causeNo +1 ;
			parentThrowable = parentThrowable.getCause();
		}
		legalArchive.logEvent(exchange, LogEvent.MELDING_SKAL_SENDES_TIL_BOQ, e.getMessage() + cause);
		log.debug("Logged event for sending message to BOQ");
	}
}
