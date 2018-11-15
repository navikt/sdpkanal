package no.nav.kanal.log;

import org.apache.camel.Exchange;

public interface LegalArchiveLogger {

	void logEvent(Exchange exchangeIn, LogEvent logEvent, String extraInfo, byte[] attachment);
	void logEvent(Exchange exchangeIn, LogEvent logEvent, byte[] attachment);
	void logEvent(Exchange exchangeIn, LogEvent logEvent);
	void logEvent(Exchange exchangeIn, LogEvent logEvent, String extraInfo);
	void logFirstEvent(String messageID, Exchange exchangeIn, LogEvent logEvent, byte[] attachment);
	void logFirstEvent(String messageID, Exchange exchangeIn, LogEvent logEvent);
	void setLogAction(String logAction);
}
