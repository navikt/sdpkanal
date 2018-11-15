package no.nav.kanal.camel;

import javax.naming.InitialContext;
import javax.transaction.UserTransaction;
import javax.transaction.Status;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.RuntimeCamelException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransactionTimeoutDetector implements Processor  {
	
	private static Logger log = LoggerFactory.getLogger(TransactionTimeoutDetector.class);
	
	@Override
	public void process(Exchange exchange) throws Exception {
		InitialContext context = new InitialContext();
		UserTransaction ut = (UserTransaction) context.lookup("java:jboss/UserTransaction");
		log.info("Transaction status: " + ut.getStatus());
		
		// if status=4 (STATUS_ROLLEDBACK) så stopper vi her (kaster CamelException)
		if(ut.getStatus() == Status.STATUS_ROLLEDBACK) {
			log.error("ROLLBACK DETECTED. Status: " + ut.getStatus());
			throw new RuntimeCamelException("Aborting because of rollback");
		}

	}
	
}
