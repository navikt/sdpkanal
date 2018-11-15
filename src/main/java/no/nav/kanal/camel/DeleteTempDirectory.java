package no.nav.kanal.camel;

import java.io.File;

import no.nav.kanal.KanalConstants;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class DeleteTempDirectory implements Processor  {

	private static final Logger log = LoggerFactory.getLogger(DeleteTempDirectory.class);
	
	@Override
	public void process(Exchange exchange) throws Exception {
		String tempDirectoryPath = (String) exchange.getIn().getHeader(KanalConstants.CAMEL_HEADER_TEMP_DIRECTORY);
		if(tempDirectoryPath != null){
			log.info("Deleting working temp directory");
			if(!delete(new File(tempDirectoryPath))){
				log.error("Could not delete working directory " + tempDirectoryPath);
			} else{
				exchange.getIn().setHeader(KanalConstants.CAMEL_HEADER_TEMP_DIRECTORY, null);
			}
		} else {
			log.warn("Temp directory header is not set. Therefore not removing temp directory");
		}
	}
	
	private boolean delete(File file){
		boolean statusOK = true;
		if(file.isDirectory()){
			for(File child : file.listFiles()){
				if(!delete(child)){
					log.warn("Could not delete file/directory " + child.getAbsolutePath());
					statusOK = false;
				}
			}
		}
		log.debug("Deleting file/folder " + file.getAbsolutePath());
		if(!file.delete()){
			log.warn("Could not delete file/directory " + file.getAbsolutePath());
			statusOK = false;
		}
		return statusOK;
	}

}
