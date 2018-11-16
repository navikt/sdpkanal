package no.nav.kanal.camel;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.kanal.KanalConstants;
import org.springframework.stereotype.Service;

@Service
public class BackoutReason implements Processor  {
	
	private static Logger log = LoggerFactory.getLogger(BackoutReason.class);
	
	@Override
	public void process(Exchange exchange) throws Exception {

		// set original message as In
		Message orginalMsg = exchange.getUnitOfWork().getOriginalInMessage();
		exchange.setIn(orginalMsg);
		log.info("Original message set on In");

		if (exchange.getProperty(KanalConstants.CAMEL_PROPERTY_ERROR_TO_PROPAGATE) != null) {
			// set header that will propagate to message on BOQ
			exchange.getIn().setHeader(KanalConstants.CAMEL_HEADER_BOQ_MESSAGE, exchange.getProperty(KanalConstants.CAMEL_PROPERTY_ERROR_TO_PROPAGATE));
		}

	}

}
