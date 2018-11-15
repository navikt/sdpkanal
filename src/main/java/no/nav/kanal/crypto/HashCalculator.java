package no.nav.kanal.crypto;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import javax.xml.bind.DatatypeConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class HashCalculator {
	
	private static Logger log = LoggerFactory.getLogger(HashCalculator.class);
	
	private static final int FILE_BUFFER_SIZE = 4096;

	private HashCalculator() {
		
	}
	
	public static byte[] getSHA256Sum(File sourceFile){
		return getHashSum(sourceFile, "SHA256");
	}
	
	private static byte[] getHashSum(File sourceFile, String algorithm){
		
		try {
			log.debug("Calculating " + algorithm + " hash of file (" + sourceFile.getPath() + ")");
			MessageDigest digest = MessageDigest.getInstance(algorithm, "IAIK");
			digest.reset();			
			try (FileInputStream fIn = new FileInputStream(sourceFile))
	        {
	            byte[] buffer = new byte[FILE_BUFFER_SIZE];
	            int readLength = fIn.read(buffer);
	            while (readLength != -1) {
	            	digest.update(buffer, 0, readLength);
	                readLength = fIn.read(buffer);
	            }
	            byte[] digestResult = digest.digest();
	            if(log.isDebugEnabled()){
	            	log.debug(algorithm + " hash of (" + sourceFile.getPath() + ") is [" + DatatypeConverter.printHexBinary(digestResult) + "]");
	            }
	            return digestResult;

	        } catch (FileNotFoundException e) {
	            throw new RuntimeException("File could not be read(" + sourceFile.getPath() + ")", e);
	        } catch (IOException e) {
	            throw new RuntimeException("Error copying file (" + sourceFile.getPath() + ")", e);
	        }
			
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("Could not find hash algorithm: " + algorithm, e);
		} catch (NoSuchProviderException e) {
			throw new RuntimeException("Could not find hash algorithm provider for " + algorithm, e);
		}
		
	}

}
