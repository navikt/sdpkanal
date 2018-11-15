package no.nav.kanal.crypto;

import iaik.security.provider.IAIK;

import java.io.File;

import javax.xml.bind.DatatypeConverter;

import org.hamcrest.core.IsInstanceOf;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

// Used to control order of execution of test-methods. See names of test-methods.
public class HashCalculatorTest {

	@Rule
    public ExpectedException thrown = ExpectedException.none();
	

	@Test
    public void testMissingFile(){
		
		thrown.expect(java.lang.RuntimeException.class);
		thrown.expectCause(IsInstanceOf.<Throwable>instanceOf(java.io.FileNotFoundException.class));
		
		IAIK.addAsProvider();
		File f = new File("src/test/resources/doesnotexist.bmp");
		Assert.assertEquals("SHA256 sum of test file","d365323b62d123b0afe7cd00aad4262a9f1ae3e23b2a0fbd3b3c0bea07742b8c".toUpperCase(), DatatypeConverter.printHexBinary(HashCalculator.getSHA256Sum(f)));
		
	}
	
	@Test
    public void testInvalidHash(){
		
		IAIK.addAsProvider();
		File f = new File("src/test/resources/testSHA256.bmp");
		Assert.assertNotEquals("SHA256 sum of test file","invalidhash".toUpperCase(), DatatypeConverter.printHexBinary(HashCalculator.getSHA256Sum(f)));
		
	}

	@Test
    public void testGetSHA256Sum(){
		
		IAIK.addAsProvider();
		File f = new File("src/test/resources/testSHA256.bmp");
		Assert.assertEquals("SHA256 sum of test file","d365323b62d123b0afe7cd00aad4262a9f1ae3e23b2a0fbd3b3c0bea07742b8c".toUpperCase(), DatatypeConverter.printHexBinary(HashCalculator.getSHA256Sum(f)));
	}
	
}
