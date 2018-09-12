package org.pentaho.di.trans.steps.standardize;

import org.junit.Test;

public class EmailAddressTest {
	
	public void check(String a, String b) {}
	
	@Test
	public void test() throws Exception {

		check("John Doe <john.doe@pentaho.com>", "john.doe@pentaho.com");
		check("John Doe <JOHN.DOE@PENTAHO.COM>", "john.doe@pentaho.com");
		check("\"John Doe\" <JOHN.DOE@PENTAHO.COM>", "john.doe@pentaho.com");
		check("JOHN .DOE@PENTAHO.COM", "john.doe@pentaho.com");
		// check("JOHN..DOE@PENTAHO.COM","john.doe@pentaho.com");
		check("JOHN.DOE@@PENTAHO.COM","john.doe@pentaho.com");
	}

}
