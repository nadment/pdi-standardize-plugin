package org.pentaho.di.trans.steps.standardize;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.IDN;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap.SimpleEntry;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.pentaho.di.core.util.Utils;

import com.google.common.net.InetAddresses;

/**
 * <p>
 * The general idea behind URL normalization is to make different URLs
 * "equivalent" (i.e. eliminate URL variations pointing to the same resource).
 * To achieve this, <code>URLNormalizer</code> takes a URL and modifies it to
 * its most basic or standard form (for the context in which it is used). Of
 * course <code>URLNormalizer</code> can simply be used as a generic URL
 * manipulation tool for your needs.
 * </p>
 * <p>
 * You would typically "build" your normalized URL by invoking each method of
 * interest, in the relevant order, using a similar approach:
 * </p>
 * 
 * <pre>
 * String url = "Http://Example.com:80//foo/index.html";
 * URL normalizedURL = new URLNormalizer(url).lowerCaseSchemeHost().removeDefaultPort().removeDuplicateSlashes()
 * 		.removeDirectoryIndex().addWWW().toURL();
 * System.out.println(normalizedURL.toString());
 * // Output: http://www.example.com/foo/
 * </pre>
 * <p>
 * Several normalization methods implemented come from the
 * <a href="http://tools.ietf.org/html/rfc3986">RFC 3986</a> standard. These
 * standards and several more normalization techniques are very well summarized
 * on the Wikipedia article titled
 * <i><a href="http://en.wikipedia.org/wiki/URL_normalization"> URL
 * Normalization</a></i>. This class implements most normalizations described on
 * that article and borrows several of its examples, as well as a few additional
 * ones.
 * </p>
 * <p>
 * The normalization methods available can be broken down into three categories:
 * </p>
 * 
 * <h3>Preserving Semantics</h3>
 * <p>
 * The following normalizations are part of the
 * <a href="http://tools.ietf.org/html/rfc3986">RFC 3986</a> standard and should
 * result in equivalent URLs (one that identifies the same resource):
 * </p>
 * <ul>
 * <li>{@link #lowerCaseSchemeHost() Convert scheme and host to lower case}</li>
 * <li>{@link #upperCaseEscapeSequence() Convert escape sequence to upper
 * case}</li>
 * <li>{@link #decodeUnreservedCharacters() Decode percent-encoded unreserved
 * characters}</li>
 * <li>{@link #removeDefaultPort() Removing default ports}</li>
 * <li>{@link #encodeNonURICharacters() URL-Encode non-ASCII characters}</li>
 * <li>{@link #encodeSpaces() Encode spaces to plus sign}</li>
 * </ul>
 * 
 * <h3>Usually Preserving Semantics</h3>
 * <p>
 * The following techniques will generate a semantically equivalent URL for the
 * majority of use cases but are not enforced as a standard.
 * </p>
 * <ul>
 * <li>{@link #addTrailingSlash() Add trailing slash}</li>
 * <li>{@link #removeDotSegments() Remove .dot segments}</li>
 * </ul>
 * 
 * <h3>Not Preserving Semantics</h3>
 * <p>
 * These normalizations will fail to produce semantically equivalent URLs in
 * many cases. They usually work best when you have a good understanding of the
 * web site behind the supplied URL and whether for that site, which
 * normalizations can be be considered to produce semantically equivalent URLs
 * or not.
 * </p>
 * <ul>
 * <li>{@link #removeDirectoryIndex() Remove directory index}</li>
 * <li>{@link #removeFragment() Remove fragment (#)}</li>
 * <li>{@link #replaceIPWithDomainName() Replace IP with domain name}</li>
 * <li>{@link #unsecureScheme() Unsecure schema (https &rarr; http)}</li>
 * <li>{@link #secureScheme() Secure schema (http &rarr; https)}</li>
 * <li>{@link #removeDuplicateSlashes() Remove duplicate slashes}</li>
 * <li>{@link #removeWWW() Remove "www."}</li>
 * <li>{@link #addWWW() Add "www."}</li>
 * <li>{@link #sortQueryParameters() Sort query parameters}</li>
 * <li>{@link #removeEmptyParameters() Remove empty query parameters}</li>
 * <li>{@link #removeTrailingQuestionMark() Remove trailing question mark
 * (?)}</li>
 * <li>{@link #removeSessionIds() Remove session IDs}</li>
 * </ul>
 * <p>
 * Refer to each methods below for description and examples (or click on a
 * normalization name above).
 * </p>
 * 
 * @author Pascal Essiembre
 */
public final class Url {

	public enum Protocol {
		HTTP(80), ACAP(674), AFP(548), DICT(2628), DNS(53), FTP(21), GIT(9418), GOPHER(70), HTTPS(443), IMAP(143), IPP(
				631), IPPS(631), IRC(194), IRCS(6697), LDAP(389), LDAPS(636), MMS(1755), MSRP(2855), MTQP(1038), NFS(
						111), NNTP(119), NNTPS(563), POP(110), PROSPERO(1525), REDIS(6379), RSYNC(873), RTSP(
								554), RTSPS(322), RTSPU(5005), SFTP(22), SMB(445), SMTP(25), SNMP(161), SSH(22), SVN(
										3690), TELNET(23), VENTRILO(3784), VNC(5900), WAIS(210), WS(80), WSS(443);

		private final int defaultPort;

		Protocol(final int port) {
			this.defaultPort = port;
		}

		public int getDefaultPort() {
			return defaultPort;
		}

		public static Protocol getByName(final String scheme) {
			if (scheme == null)
				return null;

			try {
				return valueOf(scheme.toUpperCase());
			} catch (Exception e) {
				return null;
			}
		}

		/**
		 * Category consisting of regular and special functions.
		 *
		 * <p>
		 * Consists of regular functions {@link #OTHER_FUNCTION} and special
		 * functions {@link #ROW}, {@link #TRIM}, {@link #CAST},
		 * {@link #JDBC_FN}.
		 */
		public static final Set<Protocol> PROTOCOLS = EnumSet.of(HTTP, ACAP);

	}

	/**
	 * Enumeration used to identify the allowed characters per URI component.
	 * <p>
	 * Contains methods to indicate whether a given character is valid in a
	 * specific URI component.
	 * 
	 * @see <a href="http://www.ietf.org/rfc/rfc3986.txt">RFC 3986</a>
	 */
	enum Component {

		SCHEME {
			@Override
			public boolean isAllowed(int c) {
				return isAlpha(c) || isDigit(c) || '+' == c || '-' == c || '.' == c;
			}
		},
		AUTHORITY {
			@Override
			public boolean isAllowed(int c) {
				return isUnreserved(c) || isSubDelimiter(c) || ':' == c || '@' == c;
			}
		},
		USER_INFO {
			@Override
			public boolean isAllowed(int c) {
				return isUnreserved(c) || isSubDelimiter(c) || ':' == c;
			}
		},
		HOST_IPV4 {
			@Override
			public boolean isAllowed(int c) {
				return isUnreserved(c) || isSubDelimiter(c);
			}
		},
		HOST_IPV6 {
			@Override
			public boolean isAllowed(int c) {
				return isUnreserved(c) || isSubDelimiter(c) || '[' == c || ']' == c || ':' == c;
			}
		},
		PORT {
			@Override
			public boolean isAllowed(int c) {
				return isDigit(c);
			}
		},
		PATH {
			@Override
			public boolean isAllowed(int c) {
				return isPchar(c) || '/' == c;
			}
		},
		PATH_SEGMENT {
			@Override
			public boolean isAllowed(int c) {
				return isPchar(c);
			}
		},
		QUERY {
			@Override
			public boolean isAllowed(int c) {
				return isPchar(c) || '/' == c || '?' == c;
			}
		},
		QUERY_PARAM {
			@Override
			public boolean isAllowed(int c) {
				if ('=' == c || '&' == c) {
					return false;
				} else {
					return isPchar(c) || '/' == c || '?' == c;
				}
			}
		},
		FRAGMENT {
			@Override
			public boolean isAllowed(int c) {
				return isPchar(c) || '/' == c || '?' == c;
			}

		};

		/**
		 * Indicates whether the given character is allowed in this URI
		 * component.
		 * 
		 * @return {@code true} if the character is allowed; {@code false}
		 *         otherwise
		 */
		public abstract boolean isAllowed(int c);
	}

	private static final Pattern PATTERN_PERCENT_ENCODED_CHAR = Pattern.compile("(%[0-9a-f]{2})",
			Pattern.CASE_INSENSITIVE);
	private static final Pattern PATTERN_PATH_LAST_SEGMENT = Pattern.compile(
			"(.*/)(index\\.html|index\\.htm|index\\.shtml|index\\.php"
					+ "|default\\.html|default\\.htm|home\\.html|home\\.htm|index\\.php5"
					+ "|index\\.php4|index\\.php3|index\\.cgi|placeholder\\.html" + "|default\\.asp)$",
			Pattern.CASE_INSENSITIVE);
	private static final Pattern PATTERN_DOMAIN = Pattern.compile("^[a-z0-9]+([\\-\\.]{1}[a-z0-9]+)*\\.[a-z]{2,5}$",
			Pattern.CASE_INSENSITIVE);

	private String scheme;
	private String host;
	private String path;
	private String query;
	/** Decoded fragment. */
	private String fragment;
	/** Either 80, 443 or a user-specified port. In range [1..65535]. */
	private int port;
	private String userInfo;
	private boolean isIP = false;
	
	
	public static void main(String[] args) {
		// Url url = new Url("HTTP://WWW.example.com").removeWWW();
		// Url url = new Url("WWW.example.com").removeWWW();
		try {
			Url url = new Url("http://user%40/example.com");
			System.out.println(url.toString());
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	// https://john.doe@www.example.com:123/forum/questions/?tag=networking&order=newest#top

	/**
	 * Create a new <code>Url</code> instance.
	 * 
	 * scheme:[//[user:password@]host[:port]][/]path[?query][#fragment]
	 * 
	 *
	 * @param url
	 *            the url to normalize
	 */
	public Url(String url) throws MalformedURLException {
		super();
		if (Utils.isEmpty(url)) {
			throw new IllegalArgumentException("URL argument cannot be null.");
		}

		int index = 0;
		int end = url.length();

		// Strip leading and trailing space characters
		while (index < end && Character.isWhitespace(url.charAt(index))) {
			index++;
		}
		while (end > 0 && Character.isWhitespace(url.charAt(end - 1))) {
			end--;
		}

		// Scheme
		for (int i = index; i < end; i++) {
			char c = url.charAt(i);
			if (c == ':') {
				String s = url.substring(index, i);
				if (isValidProtocol(s)) {
					scheme = s.toLowerCase();
					index = i + 1;
				}
				break;
			} else if (c == '/') {
				break;
			}
		}

		// Fragment
		int position = url.indexOf('#', index);
		if (position >= 0) {
			end = position;
			if (position + 1 < url.length()) {
				fragment = this.cleanFragment(url.substring(position + 1));
			}
		}

		// Query
		if (index < end) {
			position = url.indexOf('?', index);
			if (position != -1) {
				query = this.cleanQuery(url.substring(position + 1, end));
				if (end > position) {
					end = position;
				}
			}
		}

		// Authority
		if (scheme == null) {
			scheme = "http";
		} else if (url.startsWith("//", index)) {
			index += 2;
		}

		position = url.indexOf('/', index);
		if (position < 0) {
			position = url.indexOf('?', index);
			if (position == -1) {
				position = end;
			}
		}
		String authority = url.substring(index, position);
		index = position;

		position = authority.indexOf('@');
		if (position != -1) {
			userInfo = cleanUserInfo(authority.substring(0, position));
			host = authority.substring(position + 1).toLowerCase();
		} else {
			userInfo = null;
			host = authority.toLowerCase();
		}

		// IPv6 literal
		int startPort = 0;
		if (host != null) {
			if (host.startsWith("[")) {
				int brac = host.indexOf(']', 1);
				if (brac < 0) {
					throw new MalformedURLException("Invalid URL: " + url);
				}
				isIP=true;
				// IPv6 use same separator
				startPort = brac+1;
				host=host.toLowerCase();				
			}
			// host name or IPv4
			else {
				if ( InetAddresses.isInetAddress(host) ) {
					isIP=true;
				}
				else { 
					host = IDN.toASCII(host);
				}
			}
		}

		// Port
		port = -1;
		position = host.indexOf(':',startPort);
		if (position >= 0) {
			// see RFC2396: port can be null
			int portPosition = position + 1;
			if (host.length() > portPosition) {
				port = Integer.parseInt(host.substring(portPosition));
			}
			host = host.substring(0, position);
		}

		// Path
		if (index < end) {
			if (url.charAt(index) == '/') {
				path = url.substring(index, end);
			} else {
				String pathEnd = url.substring(index, end);
				path = !Utils.isEmpty(pathEnd) && pathEnd.charAt(0) != '/' ? "/" + pathEnd : pathEnd;
			}

			path = this.cleanPath(path);
		}
	}

	private boolean isValidProtocol(String protocol) {

		if (protocol.length() == 0 || !isAlpha(protocol.codePointAt(0)))
			return false;

		for (int i = 1; i < protocol.length(); i++) {
			if (!Component.SCHEME.isAllowed(protocol.codePointAt(i))) {
				return false;
			}
		}
		return true;
	}

	private static boolean isEncodedUnreservedCharacter(String chr) {
		// ALPHA (a-zA-Z)
		if ((chr.compareTo("%41") >= 0 && chr.compareTo("%5A") <= 0)
				|| (chr.compareTo("%61") >= 0 && chr.compareTo("%7A") <= 0)) {
			return true;
		}
		// Digit (0-9)
		if (chr.compareTo("%30") >= 0 && chr.compareTo("%39") <= 0) {
			return true;
		}
		// Hyphen
		if (chr.equalsIgnoreCase("%2D"))
			return true;
		// Period
		if (chr.equalsIgnoreCase("%2E"))
			return true;
		// Underscore
		if (chr.equalsIgnoreCase("%5F"))
			return true;
		// Tilde
		if (chr.equalsIgnoreCase("%7E"))
			return true;

		return false;
	}

	/**
	 * Converts letters in URL-encoded escape sequences to upper case and
	 * decodes percent-encoded unreserved characters.
	 * <p>
	 * <code>http://www.example.com/%7Eusername/ &rarr;
	  *       http://www.example.com/~username/</code>
	 * 
	 * <code>http://www.example.com/a%c2%b1b &rarr; 
	 *       http://www.example.com/a%C2%B1b</code>
	 * 
	 * @return this instance
	 */
	protected String cleanEscapeSequence(final String url) {
		if (url.contains("%")) {
			StringBuffer sb = new StringBuffer();
			Matcher m = PATTERN_PERCENT_ENCODED_CHAR.matcher(url);
			try {
				while (m.find()) {
					// Converts letters in URL-encoded escape sequences to upper
					// case.
					String enc = m.group(1).toUpperCase();
					// Decodes percent-encoded unreserved characters.
					if (isEncodedUnreservedCharacter(enc)) {
						m.appendReplacement(sb, URLDecoder.decode(enc, StandardCharsets.UTF_8.toString()));
					} else {
						m.appendReplacement(sb, enc);
					}
				}
			} catch (UnsupportedEncodingException e) {
				// LOG.debug("UTF-8 is not supported by your system. URL will
				// remain unchanged:" + url, e);
			}
			return m.appendTail(sb).toString();
		}
		return url;
	}

	protected String cleanUserInfo(final String value) {
		return decode(value);
	}

	/**
	 * Encodes space characters into %20
	 * 
	 * @param value
	 * @return
	 */
	protected String cleanPath(final String value) {
		return decode(value); // .replace(" ", "%20");
	}

	/**
	 * Encodes space characters into plus signs (+)
	 * 
	 * @param value
	 * @return
	 */
	protected String cleanQuery(final String value) {
		return this.decode(value).replace(" ", "+");
	}

	/**
	 * Encodes space characters into plus signs (+)
	 * 
	 * @param value
	 * @return
	 */
	protected String cleanFragment(final String value) {
		return this.decode(value).replace(" ", "+");
	}

	/**
	 * Removes the default port (80 for http, and 443 for https).
	 * <p>
	 * <code>http://www.example.com:80/bar.html &rarr; 
	 *       http://www.example.com/bar.html</code>
	 * 
	 * @return this instance
	 */
	public Url removeDefaultPort() {

		Protocol protocol = Protocol.getByName(scheme);
		if (protocol != null) {
			if (port == protocol.getDefaultPort()) {
				port = -1;
			}
		}
		return this;
	}

	/**
	 * <p>
	 * Removes duplicate slashes. Two or more adjacent slash ("/") characters
	 * will be converted into one.
	 * </p>
	 * <code>http://www.example.com/foo//bar.html 
	 *       &rarr; http://www.example.com/foo/bar.html </code>
	 * 
	 * @return this instance
	 */
	public Url removeDuplicateSlashes() {
		if (path != null) {			
			path = path.replaceAll("/{2,}", "/");
		}
		return this;
	}

	/**
     * <p>Removes any trailing slash (/) from a URL, before fragment 
     * (#) or query string (?).</p>
     *   
     * <p><b>Please Note:</b> Removing trailing slashes form URLs
     * could potentially break their semantic equivalence.</p>
     * <code>http://www.example.com/alice/ &rarr; 
     *       http://www.example.com/alice</code>
     * @return this instance
     * @since 1.11.0
     */
    public Url removeTrailingSlash() {
        if (StringUtils.endsWith(path, "/")) {
            path = StringUtils.removeEnd(path, "/"); 

        }
        return this;
}
	
	/**
	 * <p>
	 * Removes the unnecessary "." and ".." segments from the URL path.
	 * </p>
	 * <p>
	 * <b>As of 2.3.0</b>, the algorithm used to remove the dot segments is the
	 * one prescribed by
	 * <a href="http://tools.ietf.org/html/rfc3986#section-5.2.4">RFC3986</a>.
	 * </p>
	 * <code>http://www.example.com/../a/b/../c/./d.html &rarr;
	 *       http://www.example.com/a/c/d.html</code>
	 * <p>
	 * <b>Please Note:</b> URLs do not always represent a clean hierarchy
	 * structure and the dots/double-dots may have a different signification on
	 * some sites. Removing them from a URL could potentially break its semantic
	 * equivalence.
	 * </p>
	 * 
	 * @return this instance
	 * @see URI#normalize()
	 */
	public Url removeDotSegments() {

		if (path == null)
			return this;

		// (Bulleted comments are from RFC3986, section-5.2.4)

		// 1. The input buffer is initialized with the now-appended path
		// components and the output buffer is initialized to the empty
		// string.
		StringBuilder in = new StringBuilder(path);
		StringBuilder out = new StringBuilder();

		// 2. While the input buffer is not empty, loop as follows:
		while (in.length() > 0) {

			// A. If the input buffer begins with a prefix of "../" or "./",
			// then remove that prefix from the input buffer; otherwise,
			if (startsWith(in, "../")) {
				deleteStart(in, "../");
			} else if (startsWith(in, "./")) {
				deleteStart(in, "./");
			}

			// B. if the input buffer begins with a prefix of "/./" or "/.",
			// where "." is a complete path segment, then replace that
			// prefix with "/" in the input buffer; otherwise,
			else if (startsWith(in, "/./")) {
				replaceStart(in, "/./", "/");
			} else if (equalStrings(in, "/.")) {
				replaceStart(in, "/.", "/");
			}

			// C. if the input buffer begins with a prefix of "/../" or "/..",
			// where ".." is a complete path segment, then replace that
			// prefix with "/" in the input buffer and remove the last
			// segment and its preceding "/" (if any) from the output
			// buffer; otherwise,
			else if (startsWith(in, "/../")) {
				replaceStart(in, "/../", "/");
				removeLastSegment(out);
			} else if (equalStrings(in, "/..")) {
				replaceStart(in, "/..", "/");
				removeLastSegment(out);
			}

			// D. if the input buffer consists only of "." or "..", then remove
			// that from the input buffer; otherwise,
			else if (equalStrings(in, "..")) {
				deleteStart(in, "..");
			} else if (equalStrings(in, ".")) {
				deleteStart(in, ".");
			}

			// E. move the first path segment in the input buffer to the end of
			// the output buffer, including the initial "/" character (if
			// any) and any subsequent characters up to, but not including,
			// the next "/" character or the end of the input buffer.
			else {
				int nextSlashIndex = in.indexOf("/", 1);
				if (nextSlashIndex > -1) {
					out.append(in.substring(0, nextSlashIndex));
					in.delete(0, nextSlashIndex);
				} else {
					out.append(in);
					in.setLength(0);
				}
			}
		}

		// 3. Finally, the output buffer is returned as the result of
		// remove_dot_segments.
		path = out.toString();
		return this;
	}

	private static boolean equalStrings(StringBuilder b, String str) {
		return b.length() == str.length() && b.indexOf(str) == 0;
	}

	private static boolean startsWith(StringBuilder b, String str) {
		return b.indexOf(str) == 0;
	}

	private void replaceStart(StringBuilder b, String toreplace, String replacement) {
		deleteStart(b, toreplace);
		b.insert(0, replacement);
	}

	private void deleteStart(StringBuilder b, String str) {
		b.delete(0, str.length());
	}

	private void removeLastSegment(StringBuilder b) {
		int index = b.lastIndexOf("/");
		if (index == -1) {
			b.setLength(0);
		} else {
			b.setLength(index);
		}
	}

	/**
	 * <p>
	 * Removes directory index files. They are often not needed in URLs.
	 * </p>
	 * <code>http://www.example.com/a/index.html &rarr;
	 *       http://www.example.com/a/</code>
	 * <p>
	 * Index files must be the last URL path segment to be considered. The
	 * following are considered index files:
	 * </p>
	 * <ul>
	 * <li>index.html</li>
	 * <li>index.htm</li>
	 * <li>index.shtml</li>
	 * <li>index.php</li>
	 * <li>default.html</li>
	 * <li>default.htm</li>
	 * <li>home.html</li>
	 * <li>home.htm</li>
	 * <li>index.php5</li>
	 * <li>index.php4</li>
	 * <li>index.php3</li>
	 * <li>index.cgi</li>
	 * <li>placeholder.html</li>
	 * <li>default.asp</li>
	 * </ul>
	 * <p>
	 * <b>Please Note:</b> There are no guarantees a URL without its index files
	 * will be semantically equivalent, or even be valid.
	 * </p>
	 * 
	 * @return this instance
	 */
	public Url removeDirectoryIndex() {

		if (path == null)
			return this;

		if (PATTERN_PATH_LAST_SEGMENT.matcher(path).matches()) {
			path = StringUtils.substringBeforeLast(path, "/");
		}
		return this;
	}

	/**
	 * <p>
	 * Removes the URL fragment (from the "#" character until the end).
	 * </p>
	 * <code>http://www.example.com/bar.html#section1 &rarr; 
	 *       http://www.example.com/bar.html</code>
	 * 
	 * @return this instance
	 */
	public Url removeFragment() {
		this.fragment = null;
		return this;
	}

	/**
	 * <p>
	 * Replaces IP address with domain name. This is often not reliable due to
	 * virtual domain names and can be slow, as it has to access the network.
	 * </p>
	 * <code>http://208.77.188.166/ &rarr; http://www.example.com/</code>
	 * 
	 * @return this instance
	 */
	public Url replaceIPWithDomainName() {

		if ( isIP ) {
		//if (!PATTERN_DOMAIN.matcher(host).matches()) {
			try {
				InetAddress addr = InetAddress.getByName(host);
				host = addr.getHostName();
			} catch (UnknownHostException e) {
				// LOG.debug("Cannot resolve IP to host for :" + u.getHost(),
				// e);
			}
		}
		return this;
	}

	/**
	 * Unshortens a given URL to its full form
	 *
	 * @param shortUrl
	 *            short URL like bit.ly/abc
	 * @return full URL
	 */
	public Url unshorten() throws IOException {
		URL url = this.toURL();

		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setInstanceFollowRedirects(false);
		connection.setRequestMethod("HEAD");
		int responseCode = connection.getResponseCode();
		String location = connection.getHeaderField("Location");

		// 3xx (Redirection): Further action needs to be taken in order to
		// complete the request
		if (responseCode / 100 == 3 && location != null) {
			return new Url(location);
		}
		return this;
	}

	/**
	 * <p>
	 * Removes "www." domain name prefix.
	 * </p>
	 * <code>http://www.example.com/ &rarr; http://example.com/</code>
	 * 
	 * @return this instance
	 */
	public Url removeWWW() {
		if (host != null) {
			host = host.replaceFirst("^www\\.", "");
		}
		return this;
	}

	/**
	 * <p>
	 * Adds "www." domain name prefix.
	 * </p>
	 * <code>http://example.com/ &rarr; http://www.example.com/</code>
	 * 
	 * @return this instance
	 */
	public Url addWWW() {
		if (host != null) {
			if (!host.startsWith("www.")) {
				host = "www." + host;
			}
		}
		return this;
	}

	/**
	 * <p>
	 * Sorts query parameters.
	 * </p>
	 * <code>http://www.example.com/?z=bb&amp;y=cc&amp;z=aa &rarr;
	 *       http://www.example.com/?y=cc&amp;z=bb&amp;z=aa</code>
	 * 
	 * @return this instance
	 */
	public Url sortQueryParameters() {
		if (query != null) {
			query = Arrays.stream(query.split("&")).map(p -> p.split("="))
					.map(p -> p.length == 2 ? new SimpleEntry<>(p[0], p[1]) : new SimpleEntry<>(p[0], ""))
					.sorted((e1, e2) -> e1.getKey().compareTo(e2.getKey()))
					.map(e -> String.format("%s=%s", e.getKey(), e.getValue())).collect(Collectors.joining("&"));
		}
		return this;
	}

	/**
	 * Returns the normalized URL as string.
	 * 
	 * @return URL
	 */
	@Override
	public String toString() {
		final StringBuilder result = new StringBuilder();

		result.append(scheme);
		result.append(":");

		if (userInfo != null || host != null || port != -1) {
			result.append("//");

			if (userInfo != null) {
				result.append(encode(userInfo, Component.USER_INFO));
				result.append('@');
			}
			if (host != null) {
				result.append(host);
			}
			if (port != -1) {
				result.append(':');
				result.append(port);
			}
		}

		if (path != null) {
			result.append(encode(path, Component.PATH));
		}
		if (query != null) {
			result.append('?');
			result.append(encode(query, Component.QUERY));
		}
		if (fragment != null) {
			result.append('#');
			result.append(encode(fragment, Component.FRAGMENT));
		}

		return result.toString();
	}

	/**
	 * Returns the normalized URI as {@link URI}.
	 * 
	 * @return URI
	 * @throws URISyntaxException
	 */
	public URI toURI() {
		try {
			return new URI(scheme, userInfo, host, port, path, query, fragment);
		} catch (URISyntaxException e) {
			return null;
		}
	}

	/**
	 * Returns the normalized URL as {@link URI}.
	 * 
	 * @return URL
	 * @throws URISyntaxException
	 */
	public URL toURL() throws MalformedURLException {
		return this.toURI().toURL();
	}

	/**
	 * Decode the given encoded URI component value. Based on the following
	 * rules:
	 * <ul>
	 * <li>Alphanumeric characters {@code "a"} through {@code "z"}, {@code "A"}
	 * through {@code "Z"}, and {@code "0"} through {@code "9"} stay the
	 * same.</li>
	 * <li>Special characters {@code "-"}, {@code "_"}, {@code "."}, and
	 * {@code "*"} stay the same.</li>
	 * <li>A sequence "{@code %<i>xy</i>}" is interpreted as a hexadecimal
	 * representation of the character.</li>
	 * </ul>
	 * 
	 * @param source
	 *            the encoded String
	 * @return the decoded value
	 * @throws IllegalArgumentException
	 *             when the given source contains invalid encoded sequences
	 * 
	 * @see java.net.URLDecoder#decode(String, String)
	 */
	protected String decode(String source) {
		int length = source.length();
		if (length == 0) {
			return source;
		}
		// Assert.notNull(charset, "Charset must not be null");

		ByteArrayOutputStream bos = new ByteArrayOutputStream(length);
		boolean changed = false;
		for (int i = 0; i < length; i++) {
			int ch = source.charAt(i);
			if (ch == '%') {
				if (i + 2 < length) {
					char hex1 = source.charAt(i + 1);
					char hex2 = source.charAt(i + 2);
					int u = Character.digit(hex1, 16);
					int l = Character.digit(hex2, 16);
					if (u == -1 || l == -1) {
						throw new IllegalArgumentException("Invalid encoded sequence \"" + source.substring(i) + "\"");
					}
					bos.write((char) ((u << 4) + l));
					i += 2;
					changed = true;
				} else {
					throw new IllegalArgumentException("Invalid encoded sequence \"" + source.substring(i) + "\"");
				}
			} else {
				bos.write(ch);
			}
		}
		return (changed ? new String(bos.toByteArray(), StandardCharsets.UTF_8) : source);
	}

	/**
	 * Gets the scheme.
	 * 
	 * @return scheme
	 */
	public String getScheme() {
		return scheme;
	}

	/**
	 * Gets the decoded user information from the authority.
	 * 
	 * @return
	 */
	public String getUserInfo() {
		return userInfo;
	}

	/**
	 * Get the host from the authority.
	 * 
	 * @return host
	 */
	public String getHost() {
		return host;
	}

	/**
	 * Gets the port from the authority.
	 * 
	 * @return port
	 */
	public int getPort() {
		return port;
	}

	/**
	 * Gets the decoded path.
	 * 
	 * @return path
	 */
	public String getPath() {
		return path;
	}

	/**
	 * Gets the decoded query
	 * 
	 * @return query
	 */
	public String getQuery() {
		return query;
	}

	/**
	 * Gets the decoded fragment.
	 * 
	 * @return fragment
	 */
	public String getFragment() {
		return fragment;
	}

	/**
	 * Encode the given source into an encoded String using the rules specified
	 * by the given component and with the given options.
	 * 
	 * @param source
	 *            the source String
	 * @param type
	 *            the URI component for the source
	 * @return the encoded URI
	 * @throws IllegalArgumentException
	 *             when the given value is not a valid URI component
	 */
	protected String encode(String source, Component type) {
		if (Utils.isEmpty(source)) {
			return source;
		}
		// Assert.notNull(charset, "Charset must not be null");
		// Assert.notNull(type, "Type must not be null");

		byte[] bytes = source.getBytes(StandardCharsets.UTF_8);
		ByteArrayOutputStream bos = new ByteArrayOutputStream(bytes.length);
		boolean changed = false;
		for (byte b : bytes) {
			if (b < 0) {
				b += 256;
			}
			if (type.isAllowed(b)) {
				bos.write(b);
			} else {
				bos.write('%');
				char hex1 = Character.toUpperCase(Character.forDigit((b >> 4) & 0xF, 16));
				char hex2 = Character.toUpperCase(Character.forDigit(b & 0xF, 16));
				bos.write(hex1);
				bos.write(hex2);
				changed = true;
			}
		}
		return (changed ? new String(bos.toByteArray(), StandardCharsets.UTF_8) : source);
	}

	/**
	 * Indicates whether the given character is in the {@code ALPHA} set.
	 * 
	 * @see <a href="http://www.ietf.org/rfc/rfc3986.txt">RFC 3986, appendix
	 *      A</a>
	 */
	protected static boolean isAlpha(int c) {
		return (c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z');
	}

	/**
	 * Indicates whether the given character is in the {@code DIGIT} set.
	 * 
	 * @see <a href="http://www.ietf.org/rfc/rfc3986.txt">RFC 3986, appendix
	 *      A</a>
	 */
	protected static boolean isDigit(int c) {
		return (c >= '0' && c <= '9');
	}

	/**
	 * Indicates whether the given character is in the {@code gen-delims} set.
	 * 
	 * @see <a href="http://www.ietf.org/rfc/rfc3986.txt">RFC 3986, appendix
	 *      A</a>
	 */
	protected static boolean isGenericDelimiter(int c) {
		return (':' == c || '/' == c || '?' == c || '#' == c || '[' == c || ']' == c || '@' == c);
	}

	/**
	 * Indicates whether the given character is in the {@code sub-delims} set.
	 * 
	 * @see <a href="http://www.ietf.org/rfc/rfc3986.txt">RFC 3986, appendix
	 *      A</a>
	 */
	protected static boolean isSubDelimiter(int c) {
		return ('!' == c || '$' == c || '&' == c || '\'' == c || '(' == c || ')' == c || '*' == c || '+' == c
				|| ',' == c || ';' == c || '=' == c);
	}

	/**
	 * Indicates whether the given character is in the {@code reserved} set.
	 * 
	 * @see <a href="http://www.ietf.org/rfc/rfc3986.txt">RFC 3986, appendix
	 *      A</a>
	 */
	protected static boolean isReserved(int c) {
		return (isGenericDelimiter(c) || isSubDelimiter(c));
	}

	/**
	 * Indicates whether the given character is in the {@code unreserved} set.
	 * 
	 * @see <a href="http://www.ietf.org/rfc/rfc3986.txt">RFC 3986, appendix
	 *      A</a>
	 */
	protected static boolean isUnreserved(int c) {
		return (isAlpha(c) || isDigit(c) || '-' == c || '.' == c || '_' == c || '~' == c);
	}

	/**
	 * Indicates whether the given character is in the {@code pchar} set.
	 * 
	 * @see <a href="http://www.ietf.org/rfc/rfc3986.txt">RFC 3986, appendix
	 *      A</a>
	 */
	protected static boolean isPchar(int c) {
		return (isUnreserved(c) || isSubDelimiter(c) || ':' == c || '@' == c);
	}

}
