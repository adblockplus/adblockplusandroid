/*
 * HttpRequest.java
 *
 * Brazil project web application toolkit,
 * export version: 2.3 
 * Copyright (c) 1999-2007 Sun Microsystems, Inc.
 *
 * Sun Public License Notice
 *
 * The contents of this file are subject to the Sun Public License Version 
 * 1.0 (the "License"). You may not use this file except in compliance with 
 * the License. A copy of the License is included as the file "license.terms",
 * and also available at http://www.sun.com/
 * 
 * The Original Code is from:
 *    Brazil project web application toolkit release 2.3.
 * The Initial Developer of the Original Code is: cstevens.
 * Portions created by cstevens are Copyright (C) Sun Microsystems, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): cstevens, drach, suhler.
 *
 * Version:  2.7
 * Created by cstevens on 99/09/15
 * Last modified by suhler on 07/03/26 13:53:18
 *
 * Version Histories:
 *
 * 13/04/09-12:44:12 (andrey@adblockplus.org)
 *   implemented proxying chunked request body
 *
 * 2.7 07/03/26-13:53:18 (suhler)
 *   doc updates
 *
 * 2.6 07/03/26-13:44:17 (suhler)
 *   add sample main() to act as a simple "wget"
 *
 * 2.5 04/11/30-15:19:40 (suhler)
 *   fixed sccs version string
 *
 * 2.4 03/08/01-16:18:01 (suhler)
 *   fixes for javadoc
 *
 * 2.3 03/05/12-16:26:13 (suhler)
 *   Merged changes between child workspace "/home/suhler/brazil/naws" and
 *   parent workspace "/net/mack.eng/export/ws/brazil/naws".
 *
 * 1.20.1.1 03/04/17-10:03:47 (suhler)
 *   no changes made
 *
 * 2.2 03/04/15-17:29:08 (drach)
 *   Add protected modifier to variable connected so subclasses outside
 *   package can access it.
 *
 * 2.1 02/10/01-16:36:54 (suhler)
 *   version change
 *
 * 1.20 02/07/23-08:31:15 (suhler)
 *   check for no content type
 *
 * 1.19 02/07/11-15:37:33 (suhler)
 *   add encoding diagnostics
 *
 * 1.18 02/07/11-15:03:40 (suhler)
 *   add getContent() and getEncoding() convenience methods for dealing
 *   with charset encoding
 *
 * 1.17 02/04/29-17:04:41 (suhler)
 *   added public static boolean displayAllHeaders to turn on
 *   http header debugging during development.
 *
 * 1.16 02/04/24-13:36:21 (suhler)
 *   doc lint
 *
 * 1.15 02/02/26-14:42:14 (suhler)
 *   doc lint
 *
 * 1.14 02/02/26-14:32:38 (suhler)
 *   typo
 *
 * 1.13 02/02/26-14:25:52 (suhler)
 *   added "addHeaders" convenience method for adding http headers from
 *   properties objects
 *
 * 1.12 00/07/11-11:23:47 (cstevens)
 *   Some servers send "HTTP/1.0 100 Continue" in response to an HTTP/1.1 POST!
 *
 * 1.11 00/07/06-15:03:10 (cstevens)
 *   Although HTTP/1.1 chunking spec says that there is one "\r\n" between
 *   chunks, some servers (for example, maps.yahoo.com) send more than one blank
 *   line between chunks.  So, read and skip all the blank lines seen between
 *   chunks.
 *
 * 1.10 99/11/30-09:48:14 (suhler)
 *   remove diagnostics
 *
 * 1.9 99/11/09-20:23:23 (cstevens)
 *   bugs revealed by writing tests.
 *
 * 1.8 99/10/26-18:56:38 (cstevens)
 *   Change MimeHeaders so it uses "put" instead of "set", to be compatible with
 *   names chosen by Hashtable and StringMap.
 *
 * 1.7 99/10/14-14:16:31 (cstevens)
 *   merge issues.
 *
 * 1.6 99/10/14-13:19:18 (cstevens)
 *   Merged changes between child workspace "/home/cstevens/ws/brazil/naws" and
 *   parent workspace "/export/ws/brazil/naws".
 *
 * 1.4.1.2 99/10/14-13:01:06 (cstevens)
 *   Documentation.
 *   Fold TimedThread and the default HttpSocketPool into this file, since they are
 *   not used outside of this file (at this time).
 *
 * 1.5 99/10/11-12:38:38 (suhler)
 *   Merged changes between child workspace "/home/suhler/brazil/naws" and
 *   parent workspace "/net/mack.eng/export/ws/brazil/naws".
 *
 * 1.4.1.1 99/10/08-16:54:45 (cstevens)
 *   documentation
 *   Move logic for removing point-to-point headers into the HttpRequest as a
 *   static method.
 *
 * 1.4 99/10/07-13:17:55 (cstevens)
 *   Documentation for HttpRequest (in progress).
 *
 * 1.3.1.1 99/10/06-12:31:57 (suhler)
 *   comment out debugging
 *
 * 1.3 99/09/15-15:57:16 (cstevens)
 *   debugging
 *
 * 1.2 99/09/15-14:52:02 (cstevens)
 *   import *;
 *
 * 1.2 99/09/15-14:39:36 (Codemgr)
 *   SunPro Code Manager data about conflicts, renames, etc...
 *   Name history : 2 1 request/HttpRequest.java
 *   Name history : 1 0 util/http/HttpRequest.java
 *
 * 1.1 99/09/15-14:39:35 (cstevens)
 *   date and time created 99/09/15 14:39:35 by cstevens
 *
 */

package sunlabs.brazil.util.http;

import sunlabs.brazil.server.Server;

import sunlabs.brazil.util.SocketFactory;
import sunlabs.brazil.util.regexp.Regexp;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.UnsupportedEncodingException;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PushbackInputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Properties;
import java.util.Vector;
import java.util.StringTokenizer;

/**
 * Sends an HTTP request to some target host and gets the answer back.
 * Similar to the <code>URLConnection</code> class.
 * <p>
 * Caches connections to hosts, and reuses them if possible.  Talks
 * HTTP/1.1 to the hosts, in order to keep alive connections as much
 * as possible.
 * <p>
 * The sequence of events for using an <code>HttpRequest</code> is similar
 * to how <code>URLConnection</code> is used:
 * <ol>
 * <li> A new <code>HttpRequest</code> object is constructed.
 * <li> The setup parameters are modified:
 *     <ul>
 *     <li> {@link #setMethod setMethod}
 *     <li> {@link #setRequestHeader setRequestHeader}
 *     <li> {@link #getOutputStream getOutputStream}
 *     </ul>
 * <li> The host (or proxy) is contacted and the HTTP request is issued:
 *     <ul>
 *     <li> {@link #connect connect}
 *     <li> {@link #getInputStream getInputStream}
 *     </ul>
 * <li> The response headers and body are examined:
 *     <ul>
 *     <li> {@link #getResponseCode getResponseCode}
 *     <li> {@link #getResponseHeader getResponseHeader}
 *     <li> {@link #getContentLength getContentLength}
 *     </ul>
 * <li> The connection is closed:
 *     <ul>
 *     <li> {@link #close close}
 *     </ul>
 * </ol>
 * <p>
 * In the common case, all the setup parameters are initialized to sensible
 * values and won't need to be modified.  Most users will only need to
 * construct a new <code>HttpRequest</code> object and then call
 * <code>getInputStream</code> to read the contents.  The rest of the
 * member variables and methods are only needed for advanced behavior.
 * <p>
 * The <code>HttpRequest</code> class is intended to be a replacement for the
 * <code>URLConnection</code> class.  It operates at a lower level and makes
 * fewer decisions on behavior.  Some differences between the
 * <code>HttpRequest</code> class and the <code>URLConnection</code> class
 * follow: <ul>
 * <li> there are no undocumented global variables (specified in
 *	<code>System.getProperties</code>) that modify the behavior of
 *	<code>HttpRequest</code>.
 * <li> <code>HttpRequest</code> does not automatically follow redirects.
 * <li> <code>HttpRequest</code> does not turn HTTP responses with a status
 *	code other than "200 OK" into <code>IOExceptions</code>.  Sometimes
 *	it may be necessary and even quite useful to examine the results of
 *	an "unsuccessful" HTTP request.
 * <li> <code>HttpRequest</code> issues HTTP/1.1 requests and handles
 *	HTTP/0.9, HTTP/1.0, and HTTP/1.1 responses.
 * <li> the <code>URLConnection</code> class leaks open sockets if there is
 *	an error reading the response or if the target does not use
 *	Keep-Alive, and depends upon the garabge collector to close and
 *	release the open socket in these cases, which is unreliable because
 *	it may lead to intermittently running out of sockets if the garbage
 *	collector doesn't run often enough.
 * <li> If the user doesn't read all the data from an
 *	<code>URLConnection</code>, there are bugs in its implementation
 *	(as of JDK1.2) that may cause the program to block forever and/or
 *	read an insufficient amount of data before trying to reuse the
 *	underlying socket.
 * </ul>
 * <p>
 * A number of the fields in the <code>HttpRequest</code> object are public,
 * by design.  Most of the methods mentioned above are convenience methods;
 * the underlying data fields are meant to be accessed for more complicated
 * operations, such as changing the socket factory or accessing the raw HTTP
 * response line.  Note however, that the order of the methods described
 * above is important.  For instance, the user cannot examine the response
 * headers (by calling <code>getResponseHeader</code> or by examining the
 * variable <code>responseHeaders</code>) without first having connected to
 * the host.
 * <p>
 * However, if the user wants to modify the default behavior, the
 * <code>HttpRequest</code> uses the value of a number of variables and
 * automatically sets some HTTP headers when sending the request.  The user
 * can change these settings up until the time <code>connect</code> is
 * called, as follows: <dl>
 * <dt> variable {@link #version}
 *	<dd> By default, the <code>HttpRequest</code> issues HTTP/1.1
 *	requests.  The user can set <code>version</code> to change this to
 *	HTTP/1.0.
 * <dt> variable {@link #method}
 *	<dd> If <code>method</code> is <code>null</code> (the default),
 *	the <code>HttpRequest</code> decides what the HTTP request method
 *	should be as follows:  If the user has called
 *	<code>getOutputStream</code>, then the method will be "POST",
 *	otherwise the method will be "GET".
 * <dt> variable {@link #proxyHost}
 *	<dd> If the proxy host is specified, the HTTP request will be
 *	sent via the specified proxy: <ul>
 *	<li> <code>connect</code> opens a connection to the proxy.
 *	<li> uses the "Proxy-Connection" header to keep alive the connection.
 *	<li> sends a fully qualified URL in the request line, for example
 *	    "http://www.foo.com/index.html".  The fully qualified URL
 *	    tells the proxy to forward the request to the specified host.
 *      </ul>
 *	Otherwise, the HTTP request will go directly to the host: <ul>
 *	<li> <code>connect</code> opens a connection to the remote host.
 *	<li> uses the "Connection" header to keep alive the connection.
 *	<li> sends a host-relative URL in the request line, for example
 *	    "/index.html".  The relative URL is derived from the fully
 *	    qualified URL used to construct this <code>HttpRequest</code>.
 *	</ul>
 * <dt> header "Connection" or "Proxy-Connection"
 *	<dd> The <code>HttpRequest</code> sets the appropriate connection
 *	header to "Keep-Alive" to keep alive the connection to the host or
 *	proxy (respectively).  By setting the appropriate connection header,
 *	the user can control whether the <code>HttpRequest</code> tries to
 *	use Keep-Alives.
 * <dt> header "Host"
 *	<dd> The HTTP/1.1 protocol requires that the "Host" header be set
 *	to the name of the machine being contacted.  By default, this is
 *	derived from the URL used to construct the <code>HttpRequest</code>,
 *	and is set automatically if the user does not set it.
 * <dt> header "Content-Length"
 *	<dd> If the user calls <code>getOutputStream</code> and writes some
 *	data to it, the "Content-Length" header will be set to the amount of
 *	data that has been written at the time that <code>connect</code>
 *	is called.
 * </dl>
 * <hr>
 * Once all data has been read from the remote host, the underlying socket
 * may be automatically recycled and used again for subsequent requests to
 * the same remote host.  If the user is not planning on reading all the data
 * from the remote host, the user should call <code>close</code> to release
 * the socket.  Although it happens under the covers, the user should be
 * aware that if an IOException occurs or once data has been read normally
 * from the remote host, <code>close</code> is called automatically.  This
 * is to ensure that the minimal number of sockets are left open at any time.
 * <p>
 * The input stream that <code>getInputStream</code> provides automatically
 * hides whether the remote host is providing HTTP/1.1 "chunked" encoding or
 * regular streaming data.  The user can simply read until reaching the
 * end of the input stream, which signifies that all the available data from
 * this request has been read.  If reading from a "chunked" source, the
 * data is automatically de-chunked as it is presented to the user.  Currently,
 * no access is provided to the underlying raw input stream.
 *
 * @author      Colin Stevens (colin.stevens@sun.com)
 * @version		2.7
 */
public class HttpRequest
{
    /**
     * Timeout (in msec) to drain an input stream that has been closed before
     * the entire HTTP response has been read.
     * <p>
     * If the user closes the <code>HttpRequest</code> before reading all of
     * the data, but the remote host has agreed to keep this socket alive, we
     * need to read and discard the rest of the response before issuing a new
     * request.  If it takes longer than <code>DRAIN_TIMEOUT</code> to read
     * and discard the data, we will just forcefully close the connection to
     * the remote host rather than waiting to read any more.
     * <p>
     * Default value is 10000.
     */
    public static int DRAIN_TIMEOUT = 10000;

    /**
     * Maximum length of a line in the HTTP response headers (sanity check).
     * <p>
     * If an HTTP response line is longer than this, the response is
     * considered to be malformed.
     * <p>
     * Default value is 1000.
     */
    public static int LINE_LIMIT = 1000;

    /**
     * The default HTTP version string to send to the remote host when
     * issuing requests.
     * <p>
     * The default value can be overridden on a per-request basis by
     * setting the <code>version</code> instance variable.
     * <p>
     * Default value is "HTTP/1.1".
     *
     * @see #version
     */
    public static String defaultHTTPVersion = "HTTP/1.1";

    /**
     * The default proxy host for HTTP requests.  If non-<code>null</code>,
     * then all new HTTP requests will be sent via this proxy.  If
     * <code>null</code>, then all new HTTP requests are sent directly to
     * the host specified when the <code>HttpRequest</code> object was
     * constructed.
     * <p>
     * The default value can be overridden on a per-request basis by
     * calling the <code>setProxy</code> method or setting the
     * <code>proxyHost</code> instance variables.
     * <p>
     * Default value is <code>null</code>.
     *
     * @see #defaultProxyPort
     * @see #proxyHost
     * @see #setProxy
     */
    public static String defaultProxyHost = null;

    /**
     * The default proxy port for HTTP requests.  
     * <p>
     * Default value is <code>80</code>.
     *
     * @see #defaultProxyHost
     * @see #proxyPort
     */
    public static int defaultProxyPort = 80;

    /**
     * The factory for constructing new Sockets objects used to connect to
     * remote hosts when issuing HTTP requests.  The user can set this
     * to provide a new type of socket, such as SSL sockets.
     * <p>
     * Default value is <code>null</code>, which signifies plain sockets.
     */
    public static SocketFactory socketFactory = null;

    /**
     * The cache of idle sockets.  Once a request has been handled, the
     * now-idle socket can be remembered and reused later if another HTTP
     * request is made to the same remote host.  
     */
    public static HttpSocketPool pool = new SimpleHttpSocketPool();

    /**
     * The URL used to construct this <code>HttpRequest</code>.
     */
    public URL url;

    /**
     * The host extracted from the URL used to construct this
     * <code>HttpRequest</code>.
     *
     * @see #url
     */
    public String host;

    /**
     * The port extracted from the URL used to construct this
     * <code>HttpRequest</code>.
     *
     * @see #url
     */
    public int port;

    /**
     * If non-<code>null</code>, sends this HTTP request via the specified
     * proxy host and port.
     * <p>
     * Initialized from <code>defaultProxyHost</code>, but may be changed
     * by the user at any time up until the HTTP request is actually sent.
     * 
     * @see #defaultProxyHost
     * @see #proxyPort
     * @see #setProxy
     * @see #connect
     */
    public String proxyHost;

    /**
     * The proxy port.
     *
     * @see #proxyHost
     */
    public int proxyPort;

    protected boolean connected;
    boolean eof;
    HttpSocket hs;

    /**
     * The HTTP method, such as "GET", "POST", or "HEAD".
     * <p>
     * May be set by the user at any time up until the HTTP request is
     * actually sent.
     */
    public String method;

    /**
     * The HTTP version string.
     * <p>
     * Initialized from <code>defaultHTTPVersion</code>, but may be changed
     * by the user at any time up until the HTTP request is actually sent.
     */
    public String version;

    /**
     * The headers for the HTTP request.  All of these headers will be sent
     * when the connection is actually made.
     */
    public MimeHeaders requestHeaders;

    /**
     * setting this to "true" causing all http headers to be printed
     * on the standard error stream;  useful for debugging client/server
     * interactions.
     */
    public static boolean displayAllHeaders = false;

    ByteArrayOutputStream postData;

    String uri;
    String connectionHeader;

    HttpInputStream in;
    InputStream under;
    HttpInputStream cs;

    /**
     * The status line from the HTTP response.  This field is not valid until
     * after <code>connect</code> has been called and the HTTP response has
     * been read.
     */
    public String status;

    /**
     * The headers that were present in the HTTP response.  This field is
     * not valid until after <code>connect</code> has been called and the
     * HTTP response has been read.
     */
    public MimeHeaders responseHeaders;

    /*
     * Cached value of keep-alive from the response headers.
     */
    boolean keepAlive;

    /**
     * An artifact of HTTP/1.1 chunked encoding.  At the end of an HTTP/1.1
     * chunked response, there may be more MimeHeaders.  It is only possible
     * to access these MimeHeaders after all the data from the input stream
     * returned by <code>getInputStream</code> has been read.  At that point,
     * this field will automatically be initialized to the set of any headers
     * that were found.  If not reading from an HTTP/1.1 chunked source, then
     * this field is irrelevant and will remain <code>null</code>.
     */
    public MimeHeaders responseTrailers;

    /**
     * Creates a new <code>HttpRequest</code> object that will send an
     * HTTP request to fetch the resource represented by the URL.
     * <p>
     * The host specified by the URL is <b>not</b> contacted at this time.
     *
     * @param	url
     *		A fully qualified "http:" URL.
     *
     * @throws	IllegalArgumentException
     *		if <code>url</code> is not an "http:" URL.
     */
    public
    HttpRequest(URL url)
    {
	if (url.getProtocol().equals("http") == false) {
	    throw new IllegalArgumentException(url.toString());
	}

	this.url = url;

	this.host = url.getHost();
	this.port = url.getPort();
	if (this.port < 0) {
	    this.port = 80;
	}
	this.proxyHost = defaultProxyHost;
	this.proxyPort = defaultProxyPort;

	this.version = defaultHTTPVersion;
	this.requestHeaders = new MimeHeaders();
	this.responseHeaders = new MimeHeaders();
    }

    /**
     * Creates a new <code>HttpRequest</code> object that will send an
     * HTTP request to fetch the resource represented by the URL.
     * <p>
     * The host specified by the URL is <b>not</b> contacted at this time.
     *
     * @param	url
     *		A string representing a fully qualified "http:" URL.
     *
     * @throws	IllegalArgumentException
     *		if <code>url</code> is not a well-formed "http:" URL.
     */
    public
    HttpRequest(String url)
    {
	this(toURL(url));
    }

    /*
     * Artifact of Java: cannot implement HttpRequest(String) as follows
     * because <code>this(new URL(url))</code> must be first line in
     * constructor; it can't be inside of try statement:
     *
     * public HttpRequest(String url) {
     *     try {
     *         this(new URL(url));
     *     } catch (MalformedURLException e) {
     *         throw new IllegalArgumentException(url);
     *     }
     * }
     */
    private static URL
    toURL(String url)
    {
	try {
	    return new URL(url);
	} catch (MalformedURLException e) {
	    throw new IllegalArgumentException(url);
	}
    }

    /**
     * Sets the HTTP method to the specified value.  Some of the normal
     * HTTP methods are "GET", "POST", "HEAD", "PUT", "DELETE", but the
     * user can set the method to any value desired.
     * <p>
     * If this method is called, it must be called before <code>connect</code>
     * is called.  Otherwise it will have no effect.
     *
     * @param	method
     *		The string for the HTTP method, or <code>null</code> to
     *		allow this <code>HttpRequest</code> to pick the method for
     *		itself.
     */
    public void
    setMethod(String method)
    {
	this.method = method;
    }

    /**
     * Sets the proxy for this request.  The HTTP proxy request will be sent
     * to the specified proxy host.
     * <p>
     * If this method is called, it must be called before <code>connect</code>
     * is called.  Otherwise it will have no effect.
     *
     * @param	proxyHost
     *		The proxy that will handle the request, or <code>null</code>
     *		to not use a proxy.
     *
     * @param	proxyPort
     *		The port on the proxy, for the proxy request.  Ignored if
     *		<code>proxyHost</code> is <code>null</code>.
     */
    public void
    setProxy(String proxyHost, int proxyPort)
    {
	this.proxyHost = proxyHost;
	this.proxyPort = proxyPort;
    }

    /**
     * Sets a request header in the HTTP request that will be issued.  In
     * order to do fancier things like appending a value to an existing
     * request header, the user may directly access the 
     * <code>requestHeaders</code> variable.
     * <p>
     * If this method is called, it must be called before <code>connect</code>
     * is called.  Otherwise it will have no effect.
     *
     * @param	key
     *		The header name.  
     *
     * @param	value
     *		The value for the request header.
     *
     * @see	#requestHeaders
     */
    public void
    setRequestHeader(String key, String value)
    {
	requestHeaders.put(key, value);
    }

    /**
     * Gets an output stream that can be used for uploading data to the
     * host.  
     * <p>
     * If this method is called, it must be called before <code>connect</code>
     * is called.  Otherwise it will have no effect.
     * <p>
     * Currently the implementation is not as good as it could be.  The
     * user should avoid uploading huge amounts of data, for some definition
     * of huge.
     */
    public OutputStream
    getOutputStream()
	throws IOException
    {
	if (postData == null) {
	    postData = new ByteArrayOutputStream();
	}
	return postData;
    }

    public void
    setHttpInputStream(HttpInputStream cs)
    {
	this.cs = cs;
    }

    /**
     * Connect to the target host (or proxy), send the request, and read the
     * response headers.  Any setup routines must be called before the call
     * to this method, and routines to examine the result must be called after
     * this method.
     * <p>
     *
     * @throws	UnknownHostException
     *		if the target host (or proxy) could not be contacted.
     *
     * @throws	IOException
     *		if there is a problem writing the HTTP request or reading
     *		the HTTP response headers.
     */
    public void
    connect()
	throws UnknownHostException, IOException
    {
	if (connected) {
	    return;
	}
	connected = true;
	
	prepareHeaders();
	openSocket(true);
	try {
	    try {
		sendRequest();
		readStatusLine();
	    } catch (IOException e) {
		if (hs.firstTime) {
		    throw e;
		}
		closeSocket(false);
		openSocket(false);
		sendRequest();
		readStatusLine();
	    }
	    responseHeaders.read(in);

            if (displayAllHeaders) {
               System.err.println(status);
               responseHeaders.print(System.err);
               System.err.println();
	    }
	} catch (IOException e) {
	    closeSocket(false);
	    throw e;
	}
	parseResponse();
    }

    void
    prepareHeaders()
    {
	if (postData != null) {
	    if (method == null) {
		method = "POST";
	    }
	    setRequestHeader("Content-Length",
		    Integer.toString(postData.size()));
	}
	if (method == null) {
	    method = "GET";
	}

	if (proxyHost == null) {
	    uri = url.getFile();
	    connectionHeader = "Connection";
	} else {
	    uri = url.toString();
	    connectionHeader = "Proxy-Connection";
	}

	requestHeaders.putIfNotPresent(connectionHeader, "Keep-Alive");
	requestHeaders.putIfNotPresent("Host", host + ":" + port);
    }

    void
    openSocket(boolean reuse)
	throws IOException
    {
	String targetHost;
	int targetPort;

	if (proxyHost != null) {
	    targetHost = proxyHost;
	    targetPort = proxyPort;
	} else {
	    targetHost = host;
	    targetPort = port;
	}

	hs = pool.get(targetHost, targetPort, reuse);
	under = hs.in;
	in = new HttpInputStream(under);
    }

    void
    closeSocket(boolean reuse)
    {
	if (hs != null) {
	    HttpSocket tmp = hs;
	    hs = null;

	    keepAlive &= reuse;

	    /*
	     * Before we can reuse a keep-alive socket, we must first drain
	     * the input stream if there is any data left in it.  The soft
	     * 'eof' flag will have been set if we have already read all the
	     * data that we're supposed to read and the socket is ready to be
	     * recycled now.
	     */

	    if (keepAlive && !eof) {
		new BackgroundCloser(tmp, under, DRAIN_TIMEOUT).start();
	    } else {
		pool.close(tmp, keepAlive);
	    }
	}
    }

    class BackgroundCloser extends Thread 
    {
	HttpSocket hs;
	InputStream in;
	int timeout;
	Killer killer;

	BackgroundCloser(HttpSocket hs, InputStream in, int timeout)
	{
	    this.hs = hs;
	    this.in = in;
	    this.timeout = timeout;
	}

	public void start()
	{
	    killer = new Killer(this);
	    killer.start();
	    super.start();
	}

	public void run()
	{
	    try {
		byte[] buf = new byte[4096];

		while (true) {
		    if (in.read(buf, 0, buf.length) < 0) {
			break;
		    }
		}
	    } catch (IOException e) {
		keepAlive = false;
	    }
	    pool.close(hs, keepAlive);
	    killer.interrupt();
	}

	public void abort() {
		pool.close(hs, false);
		this.interrupt();
	}
    }

    static class Killer extends Thread
    {
	BackgroundCloser b;
	int timeout;
	
	Killer(BackgroundCloser b)
	{
	    this.b = b;
	}
	public void run()
	{
	    try {
		Thread.sleep(b.timeout);
		b.abort();
	    } catch (Exception e) {}
	}
    }

    void
    sendRequest()
	throws IOException
    {
        if (displayAllHeaders) {
            System.err.print(method + " " + uri + " " + version + "\r\n");
            requestHeaders.print(System.err);
            System.err.print("\r\n");
        }

	PrintStream p = new PrintStream(hs.out);
	p.print(method + " " + uri + " " + version + "\r\n");
	requestHeaders.print(p);
	p.print("\r\n");

	if (postData != null) {
	    postData.writeTo(p);
	    postData = null;			// Release memory.
	}
	
	// Pass any data left in client stream (in case of chunked request content)
	String encoding = requestHeaders.get("Transfer-Encoding", "");
	if ("chunked".equals(encoding) && cs != null)
	{
		byte[] buf = new byte[4096];
		int bytesLeft = -1;
		while (true)
		{
			// Read chunk size
			if (bytesLeft <= 0)
			{
				bytesLeft = getChunkSize(cs);
				// Output chunk size
				p.print(Integer.toHexString(bytesLeft) + "\r\n");
			}
			if (bytesLeft == 0)
				break;
			// Pass chunk data
			int count = cs.read(buf, 0, Math.min(bytesLeft, buf.length));
			if (count < 0)
			{
				// This shouldn't occur - no final zero chunk
				bytesLeft = -1;
				break;
			}
        		p.write(buf, 0, count);
			bytesLeft -= count;
			if (bytesLeft == 0)
				p.print("\r\n");
		}
		// Pass the trailer
		if (bytesLeft == 0)
		{
			while (true)
			{
				String line = cs.readLine(LINE_LIMIT);
				if (line == null)
					break;
				p.print(line + "\r\n");
				if (line.length() == 0)
					break;
			}
		}
	}
	
	p.flush();
    }

    /*
     * Copied (with some amendmends) from UnchunkingInputStream / andrey@adblockplus.org
     */
    private int
    getChunkSize(HttpInputStream is)
	throws IOException
    {
	/*
	* Although HTTP/1.1 chunking spec says that there is one "\r\n"
	* between chunks, some servers (for example, maps.yahoo.com) 
	* send more than one blank line between chunks.  So, read and skip
	* all the blank lines seen between chunks.
	*/

	int bytesLeft = 0;
	String line;
	do {
		// Sanity check: limit chars when expecting a chunk size.
		line = is.readLine(HttpRequest.LINE_LIMIT);
	} while ((line != null) && (line.length() == 0));

	try {
		bytesLeft = Integer.parseInt(line.trim(), 16);
	} catch (Exception e) {
		throw new IOException("malformed chunk");
	}
	return bytesLeft;
    }

    void
    readStatusLine()
	throws IOException
    {
	while (true) {
	    status = in.readLine(LINE_LIMIT);
	    if (status == null) {
		throw new EOFException();
	    }
	    if (status.startsWith("HTTP/1.1 100")
                    || status.startsWith("HTTP/1.0 100")) {
		/*
		 * Ignore the "100 Continue" response that some HTTP/1.1
		 * servers send.  We can't depend upon it being sent, because
		 * we might be talking to an HTTP/1.0 server or an HTTP/1.1
		 * server that doesn't send the "100 Continue" response, so
		 * we can't use the response for any decision making, such as
		 * not sending the post data.
                 *
                 * www.u-net.com sends "HTTP/1.0 100 Continue"!
		 */

		while (true) {
		    status = in.readLine();
		    if ((status == null) || (status.length() == 0)) {
			break;
		    }
		}
	    } else if (status.startsWith("HTTP/1.")) {
		return;
	    } else if (status.length() == 0) {
//		System.out.println(this + ": got a blank line");
	    } else if (status.length() == LINE_LIMIT) {
		throw new IOException("malformed server response");
	    } else if (hs.firstTime) {
		/*
		 * Some servers don't send back any headers, even if they
		 * accept a HTTP/1.0 or greater request!  We have to push
		 * back this line, so it can be re-read as the body.
		 * Since this is coming back with no headers, the content
		 * length will be unknown and so the socket will be closed.
		 */

// System.out.println("receiving HTTP/0.9 response");
		PushbackInputStream pin = new PushbackInputStream(hs.in,
			status.length() + 4);

		pin.unread('\n'); 
		pin.unread('\r');
		for (int i = status.length(); --i >= 0; ) {
		    pin.unread(status.charAt(i));
		}

		/*
		 * And push back a blank line, so the user thinks it got to
		 * the end of the headers
		 */
		pin.unread('\n');
		pin.unread('\r');

		status = "HTTP/1.0 200 OK";
		hs.in = pin;
		under = pin;
		in = new HttpInputStream(under);
		break;
	    } else {
		/*
		 * If we see funny responses (missing headers, etc.) from a
		 * socket that we've reused, then we probably got out of sync
		 * with the remote host (e.g., didn't read enough from the
		 * last response), and should abort this request.
		 */

		throw new IOException("malformed server response");
	    }
	}
    }

    void
    parseResponse()
    {
	String str;

	str = getResponseHeader(connectionHeader);
	if (str != null) {
	    keepAlive = str.equalsIgnoreCase("Keep-Alive");
	} else if (status.startsWith("HTTP/1.1")) {
	    keepAlive = true;
	} else {
	    keepAlive = false;
	}

	str = getResponseHeader("Transfer-Encoding");
	if ((str != null) && str.equals("chunked")) {
	    under = new UnchunkingInputStream(this);
	    in = new RecycleInputStream(this, under);
	    return;
	}

	int contentLength = getContentLength();
	if (contentLength < 0) {
	    /*
	     * Some servers leave off the content length for return codes
	     * known to require no content.
	     */

	    if (status.indexOf("304") > 0 || status.indexOf("204") > 0) {
		responseHeaders.put("Content-Length", "0");
		contentLength = 0;
	    }
	}

	if ((contentLength == 0) || method.equals("HEAD")) {
	    under = new NullInputStream();
	    in = new HttpInputStream(under);
	    closeSocket(keepAlive);
	} else if (contentLength > 0) {
	    under = new LimitInputStream(this, contentLength);
	    in = new RecycleInputStream(this, under);
	} else {
	    keepAlive = false;
	    in = new RecycleInputStream(this, under);
	}
    }

    /**
     * Gets an input stream that can be used to read the body of the
     * HTTP response.  Unlike the other convenience methods for accessing
     * the HTTP response, this one automatically connects to the
     * target host if not already connected.
     * <p>
     * The input stream that <code>getInputStream</code> provides
     * automatically hides the differences between "Content-Length", no
     * "Content-Length", and "chunked" for HTTP/1.0 and HTTP/1.1 responses.
     * In all cases, the user can simply read until reaching the end of the
     * input stream, which signifies that all the available data from this
     * request has been read.  (If reading from a "chunked" source, the data
     * is automatically de-chunked as it is presented to the user.  There is
     * no way to access the raw underlying stream that contains the HTTP/1.1
     * chunking packets.)
     *
     * @throws	IOException
     *		if there is problem connecting to the target.
     *
     * @see	#connect
     */
    public HttpInputStream
    getInputStream()
	throws IOException
    {
	connect();
	return in;
    }

    /**
     * Gracefully closes this HTTP request when user is done with it.
     * <p>
     * The user can either call this method or <code>close</code> on the
     * input stream obtained from the <code>getInputStream</code>
     * method -- the results are the same.
     * <p>
     * When all the response data is read from the input stream, the
     * input stream is automatically closed (recycled).  If the user is
     * not going to read all the response data from input stream, the user
     * must call <code>close</code> to
     * release the resources associated with the open request.  Otherwise
     * the program may consume all available sockets, waiting forever for
     * the user to finish reading.
     * <p>
     * Note that the input stream is automatically closed if the input
     * stream throws an exception while reading.
     * <p>
     * In order to interrupt a pending I/O operation in another thread
     * (for example, to stop a request that is taking too long), the user
     * should call <code>disconnect</code> or interrupt the blocked thread.
     * The user should not call <code>close</code> in this case because
     * <code>close</code> will not interrupt the pending I/O operation.
     * <p>
     * Closing the request multiple times is allowed.
     * <p>
     * In order to make sure that open sockets are not left lying around
     * the user should use code similar to the following:
     * <pre>
     * OutputStream out = ...
     * HttpRequest http = new HttpRequest("http://bob.com/index.html");
     * try {
     *     HttpInputStream in = http.getInputStream();
     *     in.copyTo(out);
     * } finally {
     *     // Copying to "out" could have failed.  Close "http" in case
     *     // not all the data has been read from it yet.
     *     http.close();
     * }
     * </pre>
     */

    public void
    close()
    {
	closeSocket(true);
    }

    /**
     * Interrupts this HTTP request.  Can be used to halt an in-progress
     * HTTP request from another thread, by causing it to
     * throw an <code>InterruptedIOException</code> during the connect
     * or while reading from the input stream, depending upon what state
     * this HTTP request is in when it is disconnected.
     *
     * @see	#close
     */
    public void
    disconnect()
    {
	closeSocket(false);
    }

    /**
     * Gets the HTTP response status code.  From responses like:
     * <pre>
     * HTTP/1.0 200 OK
     * HTTP/1.0 401 Unauthorized
     * </pre>
     * this method extracts the integers <code>200</code> and <code>401</code>
     * respectively.  Returns <code>-1</code> if the response status code
     * was malformed.
     * <p>
     * If this method is called, it must be called after <code>connect</code>
     * has been called.  Otherwise the information is not yet available and
     * this method will return <code>-1</code>.
     * <p>
     * For advanced features, the user can directly access the
     * <code>status</code> variable.
     *
     * @return	The integer status code from the HTTP response.
     *
     * @see	#connect
     * @see	#status
     */
    public int
    getResponseCode()
    {
	try {
	    int start = status.indexOf(' ') + 1;
	    int end = status.indexOf(' ', start + 1);
	    if (end < 0) {
		/*
		 * Sometimes the status line has the status code but no
		 * status phrase.
		 */
		end = status.length();
	    }
	    return Integer.parseInt(status.substring(start, end));
	} catch (Exception e) {
	    return -1;
	}
    }

    /**
     * Gets the value associated with the given case-insensitive header name
     * from the HTTP response.
     * <p>
     * If this method is called, it must be called after <code>connect</code>
     * has been called.  Otherwise the information is not available and
     * this method will return <code>null</code>.
     * <p>
     * For advanced features, such as enumerating over all response headers,
     * the user should directly access the <code>responseHeaders</code>
     * variable.
     *
     * @param	key
     *		The case-insensitive name of the response header.
     *
     * @return	The value associated with the given name, or <code>null</code>
     *		if there is no such header in the response.
     *
     * @see	#connect
     * @see	#responseHeaders
     */
    public String
    getResponseHeader(String key)
    {
	return responseHeaders.get(key);
    }

    /**
     * Convenience method to get the "Content-Length" header from the
     * HTTP response.
     * <p>
     * If this method is called, it must be called after <code>connect</code>
     * has been called.  Otherwise the information is not available and
     * this method will return <code>-1</code>.
     *
     * @return	The content length specified in the response headers, or
     *		<code>-1</code> if the length was not specified or malformed
     *		(not a number).
     *
     * @see	#connect
     * @see	#getResponseHeader
     */
    public int
    getContentLength()
    {
	try {
	    return Integer.parseInt(responseHeaders.get("Content-Length"));
	} catch (Exception e) {
	    return -1;
	}
    }

    /**
     * Removes all the point-to-point (hop-by-hop) headers from
     * the given mime headers.
     *
     * @param	headers
     *		The mime headers to be modified.
     *
     * @param	response
     *		<code>true</code> to remove the point-to-point <b>response</b>
     *		headers, <code>false</code> to remove the point-to-point
     *		<b>request</b> headers.
     *
     * @see <a href="http://www.cis.ohio-state.edu/htbin/rfc/rfc2068.html">RFC 2068</a>
     */
    public static void
    removePointToPointHeaders(MimeHeaders headers, boolean response)
    {
	headers.remove("Connection");
	headers.remove("Proxy-Connection");
	headers.remove("Keep-Alive");
	headers.remove("Upgrade");

	if (response == false) {
	    headers.remove("Proxy-Authorization");
	} else {
	    headers.remove("Proxy-Authenticate");
	    headers.remove("Public");
	    headers.remove("Transfer-Encoding");
	}
    }

    /**
     * Convenience method for adding request headers by looking them
     * up in a properties object.
     * @param tokens	a white space delimited set of tokens that refer
     *			to headers that will be added to the HTTP request.
     * @param props	Keys of the form <code>[token].name</code> and
     *			<code>[token].value</code> are used to lookup additional
     *			HTTP headers to be added to the request.
     * @return		The number of headers added to the request
     * @see #setRequestHeader
     */

    public int
    addHeaders(String tokens, Properties props) {
	int count = 0;
	StringTokenizer st = new StringTokenizer(tokens);
	while (st.hasMoreTokens()) {
	    String token = st.nextToken();
	    String name = props.getProperty(token + ".name");
	    String value = props.getProperty(token + ".value");
	    if (name!=null && value!=null) {
		setRequestHeader(name, value);
		count++;
	    }
	}
	return count;
    }

    /**
     * Get the content as a string.  Uses the character
     * encoding specified in the HTTP headers if available.
     * Otherwise the supplied encoding is used, or (if
     * encoding is null), the platform default encoding.
     * @param encoding		The ISO character encoding to use, if
     *				the encoding can't be determined by
     *				context.
     * @return			The content as a string.
     */

    public String getContent(String encoding)
		throws IOException, UnsupportedEncodingException {
	HttpInputStream in = getInputStream();
	ByteArrayOutputStream out = new ByteArrayOutputStream();
	in.copyTo(out);
	in.close();

	String enc = getEncoding();
	if (enc == null) {
	    enc = encoding;
	}
	String result;
	if (enc != null) {
	   result = out.toString(enc);
	} else {
	   result = out.toString();
	}
	out.close();
	return result;
    }

    /**
     * Return the content as a string.
     */

    public String getContent()
		throws IOException, UnsupportedEncodingException {
	    return getContent(null);
    }

    /**
     * Get the ISO character encoding (if any) associated with this
     * text stream,
     * or "null" if none found.  Response headers must be available.
     */

    Regexp encExp = new Regexp("^text/.*;[ \t]*charset=([^ \t;]*)",true);
    public String getEncoding() {
	String type = getResponseHeader("content-type");
	if (type == null) {
	    return null;
	} else {
	    type = type.trim();
	}
	if (displayAllHeaders) {
	    System.err.println("Looking for encoding in: " + type);
	}
	String subs[] = new String[2];
	if (type != null && encExp.match(type, subs)) {
	    return subs[1];
	} else {
	    return null;
	}
    }

    /**
     * Grab http document(s) and save them in the filesystem.
     * This is a simple batch HTTP url fetcher.  Usage:
     * <pre>
     * java ... sunlabs.brazil.request.HttpRequest [-v(erbose)] [-h(headers)] [-p<http://proxyhost:port>] url...
     * </pre>
     * <dl>
     * <dt>-v<dd>Verbose.  Print the target URL and destination file on stderr
     * <dt>-h<dd>Print all the HTTP headers on stderr
     * <dt>-phttp://proxyhost:port<dd>The following url's are to be fetched
     * via a proxy.
     * </dl>
     * The options and url's may be given in any order.  Use "-p" by itself
     * to disable the proxy for all following requests.
     * <p>
     * There are many limitations: only HTTP GET requests are supported, the
     * output filename is derived autmatically from the URL and can't be 
     * overridden, if a destination file already exists, it is overwritten.
     */

    public static void
    main(String[] args) throws Exception {
	String proxyHost = null;
	int proxyPort = 80;
	boolean isVerbose = false;

	if (args.length == 0) {
	    System.err.println("Usage: [-v(erbose) -h(headers) -p<http://proxyhost:port>] url...");
	    System.exit(1);
	}
	for (int i=0; i<args.length;i++) {
	    if (args[i].charAt(0) == '-') {
		String arg = args[i].substring(1);
		switch(arg.charAt(0)) {
		    case 'h':
		        displayAllHeaders = true;
			break;
		    case 'v':
		        isVerbose = true;
			break;
		    case 'p':
			if (arg.length() > 7) {
			    URL url = new URL(arg.substring(1));
			    proxyHost = url.getHost();
			    proxyPort = url.getPort();
			    if (proxyPort < 0) {
				proxyPort = 80;
			    }
			} else {
			    proxyHost = null;
			}
			break;
		    default:
			System.err.println("Invalid argument, ignored: -" +
			    arg);
		}
		continue;
	    }
	    try {
		HttpRequest target = new HttpRequest(args[i]);
		String name = url2file(args[i]);
		if (isVerbose) {
		    System.err.println("Fetching (" + args[i] + ") to (" +
			name + ")");
		}
		target.setProxy(proxyHost, proxyPort);
		HttpInputStream in = target.getInputStream();
		FileOutputStream out = new FileOutputStream(name);
		in.copyTo(out);
		in.close();
		out.close();
	    } catch (IOException e) {
		System.err.println("Error fetching " + args[i] +
			": " + e.getMessage());
	    }
	}
    }

    /* Invent a url from a file name. */

    static String url2file(String url) throws IOException {
	String path = HttpUtil.extractUrlPath(url);
	if (path==null) {
	    throw new IOException("Invalid url: " + url);
	}
	return path.substring(1).replace('/', '_');
    }
}

class RecycleInputStream
    extends HttpInputStream
{
    HttpRequest target;
    boolean closed;

    public
    RecycleInputStream(HttpRequest target, InputStream in)
    {
	super(in);
	this.target = target;
    }

    /**
     * Reads from the underlying input stream, which might be a raw
     * input stream, a limit input stream, or an unchunking input stream.
     * If we get EOF or there is an error reading, close the socket.
     */
    public int
    read()
	throws IOException
    {
	if (closed) {
	    return -1;
	}
	try {
	    int ch = in.read();
	    if (ch < 0) {
		close(false);
	    }
	    return ch;
	} catch (IOException e) {
	    close(false);
	    throw e;
	}
    }

    public int
    read(byte[] buf, int off, int len)
	throws IOException
    {
	if (closed) {
	    return -1;
	}
	try {
	    int count = in.read(buf, off, len);
	    if (count < 0) {
		close(false);
	    }
	    return count;
	} catch (IOException e) {
	    close(false);
	    throw e;
	}
    }

    private void
    close(boolean reuse)
    {
	if (closed == false) {
	    closed = true;
	    target.closeSocket(reuse);
	}
    }

    public void
    close()
    {
	close(true);
    }
}

class NullInputStream
    extends InputStream
{
    public int
    read()
    {
	return -1;
    }

    public int
    read(char[] buf, int off, int len)
    {
	return -1;
    }
}

class LimitInputStream
    extends HttpInputStream
{
    HttpRequest target;
    int limit;

    public
    LimitInputStream(HttpRequest target, int limit)
    {
	super(target.hs.in);
	this.target = target;
	this.limit = limit;
    }

    public int
    read()
	throws IOException
    {
	if (limit <= 0) {
	    return -1;
	}
	
	int ch = in.read();
	if ((ch >= 0) && (--limit <= 0)) {
	    target.eof = true;
	    target.closeSocket(true);
	}
	return ch;
    }

    public int
    read(byte[] buf, int off, int len)
	throws IOException
    {
	if (limit <= 0) {
	    return -1;
	}

	len = Math.min(len, limit);
	int count = in.read(buf, off, len);
	if (count < 0) {
	    limit = 0;
	    return -1;
	}
	limit -= count;
	if (limit <= 0) {
	    target.eof = true;
	    target.closeSocket(true);
	}
	return count;
    }
}

class UnchunkingInputStream
    extends HttpInputStream
{
    HttpRequest target;
    boolean eof;
    int bytesLeft;

    public
    UnchunkingInputStream(HttpRequest target)
    {
	super(target.in);
	this.target = target;
    }

    public int
    read()
	throws IOException
    {
	if ((bytesLeft <= 0) && (getChunkSize() == false)) {
	    return -1;
	}
	bytesLeft--;
	return in.read();
    }

    public int
    read(byte[] buf, int off, int len)
	throws IOException
    {
	int total = 0;
	while (true) {
	    if ((bytesLeft <= 0) && (getChunkSize() == false)) {
		break;
	    }
	    int count = super.read(buf, off, Math.min(bytesLeft, len));
	    total += count;
	    off += count;
	    bytesLeft -= count;
	    len -= count;

	    if ((len <= 0) || (available() == 0)) {
		break;
	    }
	}

	return (total == 0) ? -1 : total;
    }

    private boolean
    getChunkSize()
	throws IOException
    {
	if (eof) {
	    return false;
	}

	/*
	 * Although HTTP/1.1 chunking spec says that there is one "\r\n"
	 * between chunks, some servers (for example, maps.yahoo.com) 
	 * send more than one blank line between chunks.  So, read and skip
	 * all the blank lines seen between chunks.
	 */

	String line;
	do {
	    // Sanity check: limit chars when expecting a chunk size.

	    line = ((HttpInputStream) in).readLine(HttpRequest.LINE_LIMIT);
	} while ((line != null) && (line.length() == 0));

	try {
	    bytesLeft = Integer.parseInt(line.trim(), 16);
	} catch (Exception e) {
	    throw new IOException("malformed chunk");
	}
	if (bytesLeft == 0) {
	    eof = true;
	    target.responseTrailers = new MimeHeaders((HttpInputStream) in);
	    target.eof = true;
	    target.closeSocket(true);
	    return false;
	}

	return true;
    }
}

class SimpleHttpSocketPool
    implements Runnable, HttpSocketPool
{
    public int maxIdle = 10;	// size of the socket pool
    public int maxAge  = 20000;	// max age of idle socket (mseconds)
    public int reapInterval=10000;// interval (in msec) to run reaper thread

    // pool of idle connections
    Vector idle = new Vector();

    /**
     * Start the background thread that removes old connections
     */

    Thread reaper;

    public
    SimpleHttpSocketPool()
    {
	reaper = new Thread(this);
	reaper.setDaemon(true);
	reaper.start();
    }

    /**
     * Get a potentially "pooled" target object.
     * Call this instead of the constructor to use the pool.
     * @param host	the target content server (or web proxy)
     * @param port	target web server port
     * @param proxy	if true, use telnet passthru mode.
     */

    public HttpSocket
    get(String host, int port, boolean reuse)
	throws IOException, UnknownHostException
    {
	host = host.toLowerCase();

	if (reuse) {
	    synchronized (idle) {
		/*
		 * Start at end to reuse the most recent socket, which is
		 * hopefully the most likely to still be alive.
		 */

		int i = idle.size();
		while (--i >= 0) {
		    HttpSocket hs = (HttpSocket) idle.elementAt(i);
		    if (hs.host.equals(host) && (hs.port == port)) {
			idle.removeElementAt(i);
/*System.out.println("reusing:" + hs);*/
			hs.timesUsed++;
			return hs;
		    }
		}
	    }
	}

	HttpSocket hs = new HttpSocket(host, port);

/*System.out.println("new:" + hs);*/

	return hs;
    }

    public void
    close(HttpSocket hs, boolean reuse)
    {
	if (reuse) {
/*System.out.println("recycling: " + hs);*/
	    synchronized (idle) {
		if (idle.size() >= maxIdle) {
		    HttpSocket bump = (HttpSocket) idle.firstElement();
		    idle.removeElementAt(0);
		    bump.close();
		}
		hs.firstTime = false;
		hs.lastUsed = System.currentTimeMillis();
		idle.addElement(hs);
	    }
	} else {
/*System.out.println("closing: " + hs);*/
	    hs.close();
	}
    }

    int lastSize = -1;

    public void
    run()
    {
	while(true) {
	    try {
		Thread.sleep(reapInterval);
	    } catch (InterruptedException e) {
		break;
	    }

	    /*
	     * expire after age seconds
	     */

	    long expired = System.currentTimeMillis() - maxAge;
	    boolean any = false;
	    synchronized (idle) {
		while (idle.size() > 0) {
		    HttpSocket hs = (HttpSocket) idle.firstElement();
		    if (hs.lastUsed >= expired) {
			break;
		    }
		    any = true;
		    idle.removeElementAt(0);
		    hs.close();
		}
	    }

if (false) {
  if (idle.size() > 0 || lastSize != 0) {
    long now = System.currentTimeMillis();
    System.out.print("socket cache:");
    for (int i = 0; i < idle.size(); i++) {
	HttpSocket hs = (HttpSocket) idle.elementAt(i);
	System.out.print(" (" + hs + " " + (now - hs.lastUsed)/1000 + ")");
    }
    System.out.println();
    lastSize = idle.size();
  }
}
	}
    }

    public String
    toString()
    {
	if (idle == null) {
	    return "(null)";
	}
	StringBuffer sb = new StringBuffer();
	for (int i = 0; i < idle.size(); i++) {
	    HttpSocket hs = (HttpSocket) idle.elementAt(i);
	    sb.append(hs.toString() + ", ");
	}
	return sb.toString();
    }
}
