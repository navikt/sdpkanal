package no.nav.kanal.camel.ebms;

import static org.junit.Assert.fail;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.hamcrest.core.IsInstanceOf;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runners.MethodSorters;

// Need to control order of execution so the success method is done last. Otherwise Flame does not fail.
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class FlameEbmsTest {

	@Rule
    public ExpectedException thrown = ExpectedException.none();

	Method methodToTest;
	
	@Ignore("Does not fail when another test has initialized Flame API (which is done by the tests for FlameEbmsPush/Pull)")
	@Test
	public void test1_KeystoreErrorDuringSetup() throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		thrown.expect(java.lang.reflect.InvocationTargetException.class);
		thrown.expectCause(IsInstanceOf.<Throwable>instanceOf(org.apache.camel.RuntimeCamelException.class));
		
		FlameEbms fe = new FlameEbms();
		fe.setKeyStoreLocation("src/test/resources/flame/certs");
		fe.setKeyStorePassword("feil");
		fe.setKeyPassword("feil");
		
		invokeMethodToTest(fe);
	}
	
	@Test
	public void test2_EmptyStringPModeSetup() throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		
		FlameEbms fe = new FlameEbms();
		fe.setKeyStoreLocation("src/test/resources/flame/certs");
		fe.setKeyStorePassword("changeit");
		fe.setKeyPassword("keypw123");
		fe.setPmodePullConfigFile("");
		fe.setpmodePushDigitalConfigFile("");
		fe.setpmodePushFysiskConfigFile("");
		
		invokeMethodToTest(fe);
		
		Assert.assertNull(fe.getPullMode());
		Assert.assertNull(fe.getPushModeDigital());
		Assert.assertNull(fe.getPushModeFysisk());
	}

	@Test
	public void test3_NullPModeSetup() throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		
		FlameEbms fe = new FlameEbms();
		fe.setKeyStoreLocation("src/test/resources/flame/certs");
		fe.setKeyStorePassword("changeit");
		fe.setKeyPassword("keypw123");
		
		invokeMethodToTest(fe);
		
		Assert.assertNull(fe.getPullMode());
		Assert.assertNull(fe.getPushModeDigital());
		Assert.assertNull(fe.getPushModeFysisk());
	}

	@Test
	public void test99_NormalSetup() {
		FlameEbms fe = new FlameEbms();
		fe.setKeyStoreLocation("src/test/resources/flame/certs");
		fe.setKeyStorePassword("changeit");
		fe.setKeyPassword("keypw123");
		fe.setPmodePullConfigFile("src/test/resources/flame/as4pull-nav-flame.pmode");
		fe.setpmodePushDigitalConfigFile("src/test/resources/flame/as4push-nav-flame.pmode");
		fe.setpmodePushFysiskConfigFile("src/test/resources/flame/as4push-fysisk-nav-flame.pmode");
		
		try {
			invokeMethodToTest(fe);
		} catch (Exception e) {
			e.printStackTrace();
			fail("Error during initialize");
		}
		Assert.assertNotNull(fe.getPullMode());
		Assert.assertNotNull(fe.getPushModeDigital());
		Assert.assertNotNull(fe.getPushModeFysisk());
		Assert.assertNotNull(fe.getClientFactory());
		Assert.assertNotNull(fe.getKeyPassword());
		Assert.assertNotNull(fe.getKeyStoreLocation());
		Assert.assertNotNull(fe.getKeyStorePassword());
		Assert.assertNotNull(fe.getPmodePullConfigFile());
		Assert.assertNotNull(fe.getPmodePushDigitalConfigFile());
		Assert.assertNotNull(fe.getPmodePushFysiskConfigFile());
		
		Assert.assertNull(fe.getEbmsEndpoint());
		Assert.assertNull(fe.getLicenseFile());
	}
	

	
	private void invokeMethodToTest(FlameEbms fe) throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		methodToTest = fe.getClass().getDeclaredMethod("initialize");
		methodToTest.setAccessible(true);
		methodToTest.invoke(fe);
	}

}
