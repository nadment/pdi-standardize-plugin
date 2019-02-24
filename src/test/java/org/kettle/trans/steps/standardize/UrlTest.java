package org.kettle.trans.steps.standardize;

import org.junit.Test;
import org.kettle.trans.steps.standardize.Url;

import static org.junit.Assert.assertEquals;

/**
 * Test Cases
 * 
 * http://test.greenbytes.de/tech/tc/uris/
 * https://github.com/cweb/url-testing/blob/master/urls.json
 * 
 * @author Nicolas Adment
 *
 */
public class UrlTest {

	protected void testASCII(String expected, Url actual) {
		assertEquals(expected, actual.toString());
	}

	protected void testUNICODE(String expected, Url actual) {
		assertEquals(expected, actual.toString());
	}

	@Test
	public void components() throws Exception {		
		Url url = new Url("https://john.doe@www.example.com:123/forum/questions/?tag=networking&order=newest#top");
		
		assertEquals("https", url.getScheme());
		assertEquals("john.doe", url.getUserInfo());
		assertEquals(123, url.getPort());
		assertEquals("/forum/questions/", url.getPath());
		assertEquals("tag=networking&order=newest", url.getQuery());
		assertEquals("top", url.getFragment());
	}
	
	@Test
	public void none() throws Exception {
		testASCII("https://john.doe@www.example.com:123/forum/questions/?tag=networking&order=newest#top", new Url("https://john.doe@www.example.com:123/forum/questions/?tag=networking&order=newest#top"));
		// Lower case scheme
		testASCII("https://www.example.com", new Url("HTTps://WwW.example.com"));
		// Lower case host
		testASCII("http://www.example.com", new Url("HTTP://WWW.EXAMPLE.COM"));
		// Lower case host with IP V6
		testASCII("http://[2001:db8:0:85a3:0:0:ac1f:8001]", new Url("HTTP://[2001:DB8:0:85A3:0:0:AC1F:8001]"));				
		// Preserve trailing slash
		testASCII("http://www.example.com/", new Url("WwW.example.com/"));
		testASCII("http://www.example.com/test/", new Url("WwW.example.com/test/"));
		
		// Preserve duplicate slashes
		testASCII("http://www.example.com//test/", new Url("WwW.example.com//test/"));
		testASCII("http://example.com/file.txt;parameter", new Url("http://example.com/file.txt;parameter"));		
		
		testASCII("http://user%40@example.com", new Url("http://user%40@example.com"));
		
		// Preserve user info case
		testASCII("http://UsEr:PASSWORD@example.com", new Url("http://UsEr:PASSWORD@example.com"));

		// Preserve empty query
		testASCII("http://example.com/index.html?", new Url("http://example.com/index.html?"));
		testASCII("http://example.com?", new Url("http://example.com?"));
	}

	@Test
	public void repairSpace() throws Exception {
		testASCII("http://www.example.com/a%20b%20c", new Url("http://www.example.com/a b c"));		
		testASCII("http://www.example.com?a+b+c", new Url("http://www.example.com?a b c"));		
		testASCII("http://www.example.com/test#a+b+c", new Url("http://www.example.com/test#a b c"));		
	}

	@Test
	public void idna() throws Exception {		
		// Punycode is used to encode internationalized domain names (IDN).
		testASCII("http://www.xn--acadmie-franaise-npb1a.fr", new Url("www.xn--acadmie-franaise-npb1a.fr"));
		testASCII("http://www.xn--acadmie-franaise-npb1a.fr", new Url("www.académie-française.fr"));
		testASCII("http://xn--fsqu00a.xn--0zwm56d/", new Url("http://例子.测试/ "));	
	}
	
	
	@Test
	public void port() throws Exception {
		testASCII("http://www.example.com:8080/", new Url("http://www.example.com:8080/"));
		testASCII("http://www.example.com/", new Url("http://www.example.com:/"));

	}
	
	@Test
	public void query() throws Exception {
		testASCII("http://www.example.com/?foo=bar", new Url("http://www.example.com/?foo=bar"));
		testASCII("http://www.example.com/?as?df", new Url("http://www.example.com/?as?df"));
		
		//testASCII("http://www.example.com/?q=%3Casdf%3E", new Url("http://www.example.com/?q=&lt;asdf&gt;"));
	
	}
	
	@Test
	public void fragment() throws Exception {
		// Extra white space characters
		/**testASCII("http://www.example.com/#abcd", new Url("http://www.example.com/#a\u000Ab\u000Dc\u0009d"));*/
		
	}
	
	@Test
	public void cleanEscapeSequence() throws Exception {

		
		// lower case escape
		testASCII("http://www.example.com/a%C2%B1b", new Url("http://www.example.com/a%c2%b1b"));

		// unreserved alpha
		testASCII("http://www.example.com/a", new Url("http://www.example.com/%61"));
		testASCII("http://www.example.com/A", new Url("http://www.example.com/%41"));

		// unreserved digit
		testASCII("http://www.example.com/9", new Url("http://www.example.com/%39"));

		// unreserved hyphen
		testASCII("http://www.example.com/user-name", new Url("http://www.example.com/user%2dname"));

		// unreserved period
		testASCII("http://www.example.com/user.name", new Url("http://www.example.com/user%2ename"));

		// unreserved underscore
		testASCII("http://www.example.com/user_name", new Url("http://www.example.com/user%5fname"));

		// unreserved tilde
		testASCII("http://www.example.com/~username", new Url("http://www.example.com/%7eusername"));
		
		// escape 
		//testASCII("http://www.example.com/a%5Eb%E2%98%BAc%FFd/?e", new Url("http://www.example.com/a^b☺c%FFd/?e"));

		
		//testASCII("http://www.example.com/?q=%26%2320320%3B%26%2322909%3B", new Url("http://www.example.com/?q=\u4F60\u597D"));



	}
	
	@Test
	public void invalidSequence() throws Exception {

		// ignore invalid encoded sequence 
		//testASCII("http://www.example.com/%z", new Url("http://www.example.com/%z"));
	}

	@Test
	public void removeWWW() throws Exception {
		testASCII("http://example.com", new Url("HTTP://WWW.example.com").removeWWW());
		testASCII("https://example.com", new Url("HTTps://WwW.example.com").removeWWW());
		testASCII("http://example.com", new Url("WwW.example.com").removeWWW());
	}

	@Test
	public void removeFragment() throws Exception {
		testASCII("http://www.example.com/test.html",
				new Url("HTTP://WWW.example.com/test.html#fragment").removeFragment());
		testASCII("https://www.example.com/test",
				new Url("HTTps://WwW.example.com/test#fragment").removeFragment());
		testASCII("https://www.example.com/test", new Url("HTTps://WwW.example.com/test#").removeFragment());
	}

	@Test
	public void removeDefaultPort() throws Exception {

		
		testASCII("http://www.example.com/test.html",
				new Url("HTTP://WWW.example.com:80/test.html").removeDefaultPort());
		testASCII("https://www.example.com/test", new Url("HTTps://WwW.example.com:443/test").removeDefaultPort());
		testASCII("http://www.example.com:8080/test",
				new Url("HTTp://WwW.example.com:8080/test").removeDefaultPort());
		testASCII("ftp://ftp.example.com/file.txt",
				new Url("FTP://FTP.example.com:21/file.txt").removeDefaultPort());

		testASCII("unknown://example.com:9999/file.txt",
				new Url("UNKNOWN://example.com:9999/file.txt").removeDefaultPort());

	}

	@Test
	public void removeDuplicateSlashes() throws Exception {
		// Without path
		testASCII("http://www.example.com",
				new Url("HTTP://WWW.example.com").removeDuplicateSlashes());
		
		testASCII("http://www.example.com/test.html",
				new Url("HTTP://WWW.example.com//test.html").removeDuplicateSlashes());
		testASCII("http://www.example.com/test/index.html",
				new Url("HTTP://WWW.example.com/test//index.html").removeDuplicateSlashes());
		testASCII("http://www.example.com/test/index.html",
				new Url("HTTP://WWW.example.com/test///index.html").removeDuplicateSlashes());
	}

	
	@Test
	public void removeTrailingSlash() throws Exception {		
		testASCII("http://www.example.com",	new Url("HTTP://WWW.example.com").removeTrailingSlash());
		testASCII("http://www.example.com",	new Url("HTTP://WWW.example.com/").removeTrailingSlash());
		testASCII("http://www.example.com/test",	new Url("HTTP://WWW.example.com/test/").removeTrailingSlash());
	}


	
	@Test
	public void removeDotSegments() throws Exception {
		testASCII("http://www.example.com/test.html",
				new Url("HTTP://WWW.example.com/../test.html").removeDotSegments());
		testASCII("http://www.example.com/test/index.html",
				new Url("HTTP://WWW.example.com/test/./index.html").removeDotSegments());
	}

	@Test
	public void removeDirectoryIndex() throws Exception {
		testASCII("http://www.example.com/test",
				new Url("HTTP://WWW.example.com/test/index.html").removeDirectoryIndex());
		testASCII("http://www.example.com/test",
				new Url("HTTP://WWW.example.com/test/index.htm").removeDirectoryIndex());
		testASCII("http://www.example.com/test",
				new Url("HTTP://WWW.example.com/test/index.php").removeDirectoryIndex());
		testASCII("https://www.example.com",
				new Url("HTTps://WwW.example.com/default.html").removeDirectoryIndex());
		testASCII("https://www.example.com",
				new Url("HTTps://WwW.example.com/default.htm").removeDirectoryIndex());
	}

	@Test
	public void sortQueryParameters() throws Exception {
		testASCII("http://www.example.com/query?id=3&param=4",
				new Url("HTTP://WWW.example.com/query?param=4&id=3").sortQueryParameters());
	}

	@Test
	public void replaceIPWithDomainName() throws Exception {
		// test("http://www.pentaho.com",new
		// UrlNormalizer("HTTP://54.19.68.131").replaceIPWithDomainName());
	}
}
