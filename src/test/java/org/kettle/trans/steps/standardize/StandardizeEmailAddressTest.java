package org.kettle.trans.steps.standardize;

import javax.mail.internet.AddressException;

import org.junit.Test;

public class StandardizeEmailAddressTest {
	
	public void check(String a, String b) throws AddressException {
	//	assertEquals(new EmailAddress(a),new EmailAddress(b));
	}
	
	@Test
	public void whenOK() throws Exception {
		check("John Doe <john.doe@pentaho.com>", "john.doe@pentaho.com");
		check("John Doe <JOHN.DOE@PENTAHO.COM>", "john.doe@pentaho.com");
		check("\"John Doe\" <JOHN.DOE@PENTAHO.COM>", "john.doe@pentaho.com");
		check("JOHN.DOE@PENTAHO.COM (John Doe)", "john.doe@pentaho.com");
		check("JOHN..DOE@PENTAHO.COM","john..doe@pentaho.com");
	}
	
	@Test(expected = AddressException.class)
	public void whenExceptionSpace() throws Exception {
//		new EmailAddress("JOHN .DOE@PENTAHO.COM");
	}
	
	@Test(expected = AddressException.class)
	public void whenExceptionArobase() throws Exception {
//		new EmailAddress("JOHN.DOE@@PENTAHO.COM");
	}
}
