package no.nav.kanal.log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.emottak.archiver.LogAndArchiveRequest;
import no.nav.emottak.archiver.LogAndArchiveResponse;
import no.nav.emottak.services.archiver.ArchiverPortType;

public class LegalArchiveLoggerMock extends LegalArchiveLoggerImpl {
	
	private static Logger log = LoggerFactory.getLogger(LegalArchiveLoggerMock.class);
	
	@Override
	protected void setUp(){
		if(archiver==null){
			log.warn("Legal archive is Mocked. This means that no requests is actually stored in Legal Archive");
			archiver = new ArchiverPortType() {
				
				@Override
				public LogAndArchiveResponse archive(LogAndArchiveRequest parameters) {
					LogAndArchiveResponse mockResponse = new LogAndArchiveResponse();
					if(parameters.getLogId() != null){
						mockResponse.setLogId(parameters.getLogId());
					} else{
						mockResponse.setLogId("MOCK-RESPONSE-" + parameters.getMessage().getMessageID().getValue());
					}
					return mockResponse;
				}
			};
		}
	}

}
