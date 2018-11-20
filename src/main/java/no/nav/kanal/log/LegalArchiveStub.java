package no.nav.kanal.log;

import org.apache.camel.Exchange;
import org.springframework.stereotype.Service;

@Service("legalArchive")
public class LegalArchiveStub implements LegalArchiveLogger {
    @Override
    public void logEvent(Exchange exchangeIn, LogEvent logEvent, String extraInfo, byte[] attachment) {

    }

    @Override
    public void logEvent(Exchange exchangeIn, LogEvent logEvent, byte[] attachment) {

    }

    @Override
    public void logEvent(Exchange exchangeIn, LogEvent logEvent) {

    }

    @Override
    public void logEvent(Exchange exchangeIn, LogEvent logEvent, String extraInfo) {

    }

    @Override
    public void logFirstEvent(String messageID, Exchange exchangeIn, LogEvent logEvent, byte[] attachment) {

    }

    @Override
    public void logFirstEvent(String messageID, Exchange exchangeIn, LogEvent logEvent) {

    }

    @Override
    public void setLogAction(String logAction) {

    }
}
