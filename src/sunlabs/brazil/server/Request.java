/*
 * Request.java
 *
 * Brazil project web application toolkit,
 * export version: 2.3 
 * Copyright (c) 1998-2007 Sun Microsystems, Inc.
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
 * The Initial Developer of the Original Code is: suhler.
 * Portions created by suhler are Copyright (C) Sun Microsystems, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): cstevens, drach, rinaldo, suhler.
 *
 * Version:  2.11
 * Created by suhler on 98/09/14
 * Last modified by suhler on 07/03/07 09:15:59
 *
 * Version Histories:
 *
 * 2.11 07/03/07-09:15:59 (suhler)
 *   added log messages for post failures
 *
 * 2.10 06/11/13-14:17:10 (suhler)
 *   tweak error message
 *
 * 2.9 06/04/28-16:16:26 (suhler)
 *   don't send content for "204" results
 *   .
 *
 * 2.8 06/01/17-09:42:21 (suhler)
 *   doc fixes
 *
 * 2.7 05/12/08-13:03:19 (suhler)
 *   protect against DOS from bogus http requests
 *
 * 2.6 04/11/30-15:19:41 (suhler)
 *   fixed sccs version string
 *
 * 2.5 04/11/03-08:35:16 (suhler)
 *   set "url.orig" in request
 *
 * 2.4 03/08/01-16:17:47 (suhler)
 *   fixes for javadoc
 *
 * 2.3 03/05/12-16:27:37 (suhler)
 *   Merged changes between child workspace "/home/suhler/brazil/naws" and
 *   parent workspace "/net/mack.eng/export/ws/brazil/naws".
 *
 * 1.71.1.1 03/05/12-16:24:16 (suhler)
 *   added a "serverProtocol" variable to allow handlers to change the
 *   protocol (e.g. http) the server claims to be using on a per-request basis.
 *   If "serverProtocol" is unchanged (the default), then the Server.protocol
 *   is used instead.
 *   This is useful when fronting Brazil servers with ssl gateways; the protocol
 *   can be changed from "http" to "https" to allow redirects to work
 *   properly
 *
 * 2.2 02/10/18-15:04:49 (drach)
 *   Add shared props after Request.props rather than before server.props
 *
 * 2.1 02/10/01-16:34:52 (suhler)
 *   version change
 *
 * 1.71 02/08/21-18:25:46 (suhler)
 *   doc update
 *
 * 1.70 02/06/27-18:38:47 (suhler)
 *   don't override content type if its already been set
 *
 * 1.69 02/05/07-07:04:18 (drach)
 *   Add no arg constructor.
 *
 * 1.68 02/04/23-14:02:36 (suhler)
 *   bug fix exposed by tests
 *
 * 1.67 02/04/18-11:17:23 (suhler)
 *   add processing for "HEAD" requests in sendResponse(),  This might not
 *   cover all the cases yet
 *
 * 1.66 02/02/07-14:06:10 (suhler)
 *   sendResponse() now does the "right think" for HEAD requests
 *
 * 1.65 01/09/13-09:26:22 (suhler)
 *   remove uneeded import
 *
 * 1.64 01/08/22-16:23:07 (drach)
 *   Change comments regarding shared properties.
 *
 * 1.63 01/08/21-10:59:16 (suhler)
 *   add check for maximum request length
 *
 * 1.62 01/08/20-17:32:21 (suhler)
 *   change readFully() to read()
 *
 * 1.61 01/08/13-09:09:29 (drach)
 *   Change server.props back to Properties object.
 *
 * 1.60 01/08/07-14:19:30 (drach)
 *   Move PropertiesList debug initialization to earliest possible point.
 *
 * 1.59 01/08/03-15:29:31 (drach)
 *   Remove SharedProps and add PropertiesList
 *
 * 1.58 01/07/10-10:37:58 (drach)
 *   Account for semantic change for equals() in Dictionary objects.
 *
 * 1.57 01/06/05-22:14:06 (drach)
 *   Reduce access control for brazil.servlet package
 *
 * 1.56 01/05/30-08:33:43 (drach)
 *   Reduce access control on selected fields and methods.
 *
 * 1.55 01/05/24-19:53:51 (drach)
 *   Count all bytes written.
 *
 * 1.54 01/03/12-17:42:20 (cstevens)
 *   Merged changes between child workspace "/home/cstevens/ws/brazil/naws" and
 *   parent workspace "/export/ws/brazil/naws".
 *
 * 1.52.1.1 01/03/12-17:29:31 (cstevens)
 *   Change log() methods to make testing easier.
 *
 * 1.53 01/03/06-11:16:47 (suhler)
 *   chage Vector.add() to Vector.addElement() for compatibility with java 1.1
 *
 * 1.52 01/03/05-16:21:46 (cstevens)
 *   test suite:
 *   Timeout during request gave error "null 408 Timeout".  Should be
 *   "HTTP/1.1 408 Timeout"
 *
 * 1.51 01/02/20-16:29:11 (cstevens)
 *   docs
 *
 * 1.50 01/02/20-16:16:24 (cstevens)
 *   docs.
 *
 * 1.49 01/02/20-15:52:32 (cstevens)
 *   Shared Dictionaries are searched after request.props and before server.props.
 *
 * 1.48 01/02/12-17:15:02 (cstevens)
 *   Get rid of Request.RechainableProperties public inner class.
 *   Add method to Request to add an existing Properties object to a request w/o
 *   exposing a special class or requiring all users of Request to use a special
 *   class.  Works for multithreaded sharing of the same end-user provided
 *   Properties object also, which the RechainableProperties didn't do.
 *
 * 1.47 00/12/05-13:12:36 (suhler)
 *   added getStatus() and getReuseCount() methods for statistics gathering
 *   - changed semantics of bytesWritten: resets at each request
 *
 * 1.46 00/11/20-13:22:13 (suhler)
 *   doc fixes
 *
 * 1.45 00/11/15-09:50:35 (suhler)
 *   added start-of-request timestamp
 *
 * 1.44 00/10/05-09:18:48 (suhler)
 *   remove dead code
 *
 * 1.43 00/07/07-17:02:18 (suhler)
 *   remove System.out.println(s)
 *
 * 1.42 00/07/06-15:49:29 (suhler)
 *   doc update
 *
 * 1.41 00/06/29-10:47:15 (suhler)
 *   HttpOutputStream counts the # of bytes written
 *
 * 1.40 00/05/31-13:51:32 (suhler)
 *   docs
 *
 * 1.39 00/05/15-12:02:23 (suhler)
 *   remove dependency on regexp package by putting in ugly code
 *
 * 1.38 00/03/29-16:43:43 (cstevens)
 *   Request was missing documentation of basic functionality.
 *
 * 1.37 00/03/10-17:11:33 (cstevens)
 *   Eliminated DataOutputStream from Request.java.
 *   Added a rechainable Properties object to Request.java
 *
 * 1.36 99/11/16-14:35:29 (cstevens)
 *   Request.java:
 *   1. If the "Content-Length" provided in the request was malformed, the
 *   log message should display how it was malformed.
 *   2. If Request.sendError() was called, it was logging the word "null"
 *   if the user passed in null for the detail message.  Aesthetically
 *   displeasing.
 *   3. If Request.sendError() is called when part of a response has already
 *   been sent, it cannot send the error headers and error message, because
 *   that would confuse things, so it must just close the connection.  That
 *   was done right, but it wasn't bumping the Server.errorCount.  It still
 *   should do that always.
 *
 * 1.35 99/11/03-17:52:31 (cstevens)
 *   MultiHostHandler.
 *
 * 1.34 99/10/26-18:54:35 (cstevens)
 *   Eliminate public methods Server.initHandler() and Server.initObject().
 *   Get rid of public variables Request.server and Request.sock:
 *   A. In all cases, Request.server was not necessary; it was mainly used for
 *   constructing the absolute URL for a redirect, so Request.redirect() was
 *   rewritten to take an absolute or relative URL and do the right thing.
 *   B. Request.sock was changed to Request.getSock(); it is still rarely used
 *   for diagnostics and logging (e.g., ChainSawHandler).
 *   HTTP request returned ungracefully if "Content-Length" in a POST was
 *   negative or too much to allocate.
 *
 * 1.33 99/10/25-15:48:03 (cstevens)
 *   Merged changes between child workspace "/home/cstevens/ws/brazil/naws" and
 *   parent workspace "/export/ws/brazil/naws".
 *
 * 1.31.1.1 99/10/25-15:38:48 (cstevens)
 *   String.equals() can take null.
 *
 * 1.32 99/10/25-14:32:44 (suhler)
 *   missing content length
 *
 * 1.25.2.1 99/10/23-19:49:32 (rinaldo)
 *
 * 1.31 99/10/19-18:57:53 (cstevens)
 *   toString
 *
 * 1.30 99/10/14-13:14:59 (cstevens)
 *   Merged changes between child workspace "/home/cstevens/ws/brazil/naws" and
 *   parent workspace "/export/ws/brazil/naws".
 *
 * 1.28.1.3 99/10/14-13:11:04 (cstevens)
 *   @author & @version
 *
 * 1.28.1.2 99/10/14-12:54:36 (cstevens)
 *   don't display socket information twice when logging.
 *   rewrite sendResponse() to take InputStream rather than HttpInputStream.
 *   Maybe this is a bad idea.
 *
 * 1.29 99/10/11-12:36:25 (suhler)
 *   Merged changes between child workspace "/home/suhler/brazil/naws" and
 *   parent workspace "/net/mack.eng/export/ws/brazil/naws".
 *
 * 1.27.1.1 99/10/11-12:32:00 (suhler)
 *   we were getting duplicate mime headers
 *
 * 1.28.1.1 99/10/08-16:53:26 (cstevens)
 *   Handle malformed URLs that have spaces in the name.  These type of URLS are
 *   sent by RealAudio clients.  HTTP request line is actually simpler now.
 *
 * 1.28 99/10/07-13:01:25 (cstevens)
 *   javadoc lint
 *   accessor methods for setting an integer mime header.
 *
 * 1.27 99/10/04-16:05:37 (cstevens)
 *   Merged changes between child workspace "/home/cstevens/ws/brazil/naws" and
 *   parent workspace "/export/ws/brazil/naws".
 *
 * 1.26 99/10/01-11:41:52 (suhler)
 *   Merged changes between child workspace "/home/suhler/brazil/naws" and
 *   parent workspace "/net/mack.eng/export/ws/brazil/naws".
 *
 * 1.24.1.2 99/10/01-11:40:05 (suhler)
 *   no query string == "" not null
 *
 * 1.25.1.1 99/10/01-11:27:04 (cstevens)
 *   Change logging to show prefix of Handler generating the log message.
 *
 * 1.25 99/09/30-10:45:43 (cstevens)
 *   Multiple instances of "Server" and "Date" in response: fixed.
 *
 * 1.24.1.1 99/09/22-16:02:49 (suhler)
 *   commented out debugging code
 *
 * 1.24 99/09/15-14:51:24 (cstevens)
 *   import *;
 *
 * 1.23 99/09/15-14:41:24 (cstevens)
 *   Rewritign http server to make it easier to proxy requests.
 *
 * 1.22 99/09/15-13:32:29 (cstevens)
 *   Rewriting http server to allow easier proxying.
 *
 * 1.21 99/08/17-14:50:33 (suhler)
 *   Added convenience method for sending binary data as a response
 *
 * 1.20 99/07/30-10:52:14 (suhler)
 *   always flush after emitting headers
 *   This is used by the tailHandler as a "signal" that all the headers
 *   have been output
 *
 * 1.19 99/06/29-14:34:16 (suhler)
 *   Added public serverUrl method to return a url suitable for redirects
 *   back to the same server
 *   .
 *
 * 1.18 99/06/28-10:53:39 (suhler)
 *   added getServer() method
 *
 * 1.17 99/04/08-09:52:21 (suhler)
 *   Make sure "responseCode" is correct after request is complete
 *
 * 1.16 99/04/07-13:03:18 (suhler)
 *   make responseCode public (why not?)
 *   .
 *
 * 1.15 99/04/01-09:06:02 (suhler)
 *   - Allow PUT requests
 *   - Added 2 more http return codes to support PUT
 *
 * 1.14 99/03/30-09:42:03 (suhler)
 *   Merged changes between child workspace "/home/suhler/brazil/naws" and
 *   parent workspace "/net/smartcard.eng/export/ws/brazil/naws".
 *
 * 1.11.1.1 99/03/30-09:26:27 (suhler)
 *   - documentation updates
 *   - fixed non-initialized bug in "connection" headers and request type
 *
 * 1.13 99/03/23-17:21:43 (cstevens)
 *   resolve wildcard imports.
 *
 * 1.12 99/03/23-15:11:28 (cstevens)
 *   Issuing an illegal HTTP request to brazil server resulted in HTTP error
 *   response: "null 400 Bad Request" instead of "HTTP/1.0 400 Bad Request"
 *
 * 1.11 99/03/09-11:27:23 (suhler)
 *   Merged changes between child workspace "/home/suhler/brazil/naws" and
 *   parent workspace "/net/smartcard.eng/export/ws/brazil/naws".
 *
 * 1.9.1.1 99/03/09-11:22:36 (suhler)
 *   handle 100 continue
 *
 * 1.10 99/02/17-17:27:41 (cstevens)
 *   Merged changes between child workspace "/home/cstevens/ws/brazil/naws" and
 *   parent workspace "/net/smartcard.eng/export/ws/brazil/naws".
 *
 * 1.6.1.1 99/02/17-17:17:26 (cstevens)
 *   Brazil server has never correctly handled "%XX" in the url or query data.  It
 *   treated it as a decimal number.
 *
 * 1.9 99/02/10-10:36:01 (suhler)
 *   - changed mime processing to allow un-collapsing of headers
 *   - made a couple of private variables public, for the proxyHandler
 *
 * 1.8 99/02/03-14:12:34 (suhler)
 *   removed superplusous output
 *
 * 1.7 99/01/29-11:48:44 (suhler)
 *   redo connection headers
 *
 * 1.6 98/11/18-13:42:46 (suhler)
 *   The already removed keep-alive stuff creapt back in some how
 *
 * 1.5 98/11/16-16:04:03 (suhler)
 *   Added flag to turn off keep-alives for http/1.0
 *   ./
 *   .
 *
 * 1.4 98/10/27-14:26:30 (suhler)
 *   Added constructor to getQueryData to take pre-existing hash table
 *
 * 1.3 98/10/13-12:05:27 (suhler)
 *   typo
 *
 * 1.2 98/09/21-14:51:36 (suhler)
 *   changed the package names
 *
 * 1.2 98/09/14-18:03:09 (Codemgr)
 *   SunPro Code Manager data about conflicts, renames, etc...
 *   Name history : 2 1 server/Request.java
 *   Name history : 1 0 Request.java
 *
 * 1.1 98/09/14-18:03:08 (suhler)
 *   date and time created 98/09/14 18:03:08 by suhler
 *
 */

package sunlabs.brazil.server;

import sunlabs.brazil.properties.PropertiesList;
import sunlabs.brazil.util.http.HttpInputStream;
import sunlabs.brazil.util.http.HttpUtil;
import sunlabs.brazil.util.http.MimeHeaders;

import java.io.InputStream;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FilterOutputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Dictionary;
import java.util.NoSuchElementException;
import java.util.Hashtable;
import java.util.Properties;
import java.util.StringTokenizer;

/**
 * Represents an HTTP transaction.   A new instance is created
 * by the server for each connection.
 * <p>
 * Provides a set of accessor functions to fetch the individual fields
 * of the HTTP request.
 * <p>
 * Utility methods that are generically useful for manipulating HTTP
 * requests are included here as well.  An instance of this class is
 * passed to handlers.  There will be exactly one request object per thead
 * at any time.
 * <p>
 * The fields
 * {@link #headers}, 
 * {@link #query}, and
 * {@link #url}, and the method
 * {@link #getQueryData()}
 * are most often used to examine the content of the request.
 * The field
 * {@link #props}
 * contains information about the server, or up-stream handlers.
 * <p>
 * The methods
 * {@link #sendResponse(String, String, int)} and
 * {@link Request#sendError(int, String)}
 * are commonly used to return content to the client.  The methods
 * {@link #addHeader(String)} and
 * {@link #setStatus(int)} can be used to modify the response headers
 * and return code respectively before the response is sent.
 * <p>
 * Many of the other methods are used internally, but can be useful to
 * handlers that need finer control over the output that the above methods
 * provide.  Note that the order of the methods is important.  For instance,
 * the user cannot change the HTTP response headers (by calling the
 * <code>addHeader</code> method or by modifying the
 * <code>responseHeaders</code> field) after having already sent an HTTP
 * response.
 * <p>
 * A number of the fields in the <code>Request</code> object are public,
 * by design.  Many of the methods are convenience methods; the underlying
 * data fields are meant to be accessed for more complicated operations,
 * such as changing the URL or deleting HTTP response headers.  
 *
 * @see		Handler
 * @see		Server
 *
 * @author	Stephen Uhler (stephen.uhler@sun.com)
 * @author	Colin Stevens (colin.stevens@sun.com)
 * @version		2.11
 */
public class Request
{
    /**
     * Maximum number of blank lines allowed between requests before
     * aborting the connecion.  The spec allows 0, but some clients add
     * one or more.
     */
    public static final int MAX_BLANKS = 10;
    /**
     * The server that initiated this request.  Only under rare circumstances
     * should this be modified.
     */
    public Server server;
    /**
     * Our connection to the client. Only under rare circumstances would this
     * need to be modified.
     */
    public Socket sock;
    public HttpInputStream in;

    /**
     * A set of properties local to this request.  The property is wrapped
     * in a <code>PropertiesList</code> object and initially is the head
     * of a linked list of properties that are searched in order.
     * This is useful for handlers that wish to communicate via properties
     * to down-stream handlers, such as modifying a server property for a
     * particular request.  Some handlers may even add entire new sets of
     * properties onto the front of <code>request.props</code> to temporarily
     * modify the properties seen by downstream handlers. 
     */
    public PropertiesList props;

    /**
     * A <code>PropertiesList</code> object that wraps
     * <code>server.props</code>.  When this <code>request</code> is
     * created, a new <code>PropertiesList</code> wrapping 
     * <code>server.props</code> is created and added to a list consisting
     * only of <code>props</code> and <code>serverProps</code>.
     */
    public PropertiesList serverProps;

    /**
     * The HTTP response to the client is written to this stream.  Normally
     * the convenience methods, such as <code>sendResponse</code>, are used
     * to send the response, but this field is available if a handler
     * needs to generate the response specially.
     * <p>
     * If the user chooses to write the response directly to this stream, the
     * user is still encouraged to use the convenience methods, such as
     * <code>sendHeaders</code>, to first send the HTTP response headers.
     * The {@link sunlabs.brazil.filter.FilterHandler}
     * examines the HTTP response headers
     * set by the convenience methods to determine whether to filter the
     * output.  
     * <p>
     * Note that the HTTP response headers will <b>not</b> automatically be
     * sent as a side effect if the user writes to this stream.  The user
     * would either need to call the convenience method
     * <code>sendHeaders</code> or need to generate the HTTP response headers
     * themselves.
     * <p>
     * This variable is declared as a <code>Request.HttpOutputStream</code>,
     * which provides the convenience method <code>writeBytes</code> to write
     * the byte representation of a string back to the client.  If the user
     * does not need this functionality, this variable may be accessed
     * simply as a normal <code>OutputStream</code>.
     *
     * @see	#sendResponse(String, String, int)
     * @see	#sendHeaders(int, String, int)
     */
    public HttpOutputStream out;

    /*
     * How many requests this <code>Request</code> will handle.  If this goes
     * to 0, then the connection will be closed even if
     * <code>keepAlive</code> is true.
     */
    protected int requestsLeft;


    //-----------------------------------------------------------------------

    /**
     * The HTTP request method, such as "GET", "POST", or "PUT".
     */
    public String method;

    /**
     * The URL specified in the request, not including any "?" query
     * string.
     * <p>NOTE: Traditionally handlers modify this as needed.  The request
     * property "url.orig" is set to match the url at creation time, and
     * should be considered "Read only", for those cases where the original
     * url is required.
     */
    public String url;

    /**
     * The query string specified after the URL, or <code>""</code> if no
     * query string was specified.
     */
    public String query;

    /**
     * The HTTP protocol specified in the request, either "HTTP/1.0" or
     * "HTTP/1.1".
     *
     * @see	#version
     */
    public String protocol;

    /**
     * Derived from {@link #protocol}, the version of the HTTP protocol
     * used for this request.  Either <code>10</code> for "HTTP/1.0" or
     * <code>11</code> for "HTTP/1.1".
     */
    public int version;

    /**
     * The HTTP request headers.  Keys and values in this table correspond
     * the field names and values from each line in the HTTP header;
     * field names are case-insensitive, but the case of the values is
     * preserved.  The order of entries in this table corresponds to the
     * order in which the request headers were seen.  Multiple header lines
     * with the same key are stored as separate entries in the table.
     */
    public MimeHeaders headers;

    /**
     * The uploaded content of this request, usually from a POST.  Set to
     * <code>null</code> if the request has no content.
     */
    public byte[] postData;

    /**
     * <code>true</code> if the client requested a persistent connection,
     * <code>false</code> otherwise.  Derived from the {@link #protocol} and
     * the {@link #headers},
     * <p>
     * When "Keep-Alive" is requested, the client can issue multiple,
     * consecutive requests via a single socket connection.  By default: <ul>
     * <li> HTTP/1.0 requests are not Keep-Alive, unless the
     * "Connection: Keep-Alive" header was present.
     * <li> HTTP/1.1 requests are Keep-Alive, unless the "Connection: close"
     * header was present.
     * </ul>
     * The user can change this value from <code>true</code> to
     * <code>false</code> to forcefully close the connection to the client
     * after sending the response.  The user can change this value from
     * <code>false</code> to <code>true</code> if the client is using a
     * different header to request a persistent connection.  See
     * {@link #connectionHeader}.
     * <p>
     * Regardless of this value, if an error is detected while receiving
     * or responding to an HTTP request, the connection will be closed.
     */
    public boolean keepAlive;

    /**
     * The header "Connection" usually controls whether the client
     * connection will be of type "Keep-Alive" or "close".  The same
     * header is written back to the client in the response headers.
     * <p>
     * The field {@link #keepAlive} is set based on the value of the
     * "Connection" header.  However, not all clients use "Connection"
     * to request that the connection be kept alive.  For instance (although
     * it does not appear in the HTTP/1.0 or HTTP/1.1 documentation) both
     * Netscape and IE use the "Proxy-Connection" header when issuing
     * requests via an HTTP proxy.  If a <code>Handler</code> is written to
     * respond to HTTP proxy requests, it should set <code>keepAlive</code>
     * depending on the value of the "Proxy-Connection" header, and set
     * <code>connectionHeader</code> to "Proxy-Connection", since the
     * convenience methods like <code>setResponse()</code> use these fields
     * when constructing the response.  The server does not handle the
     * "Proxy-Connection" header by default, since trying to pre-anticipate
     * all the exceptions to the specification is a "slippery slope".
     */
    public String connectionHeader;

    /**
     * This is the server's protocol.  It is normally null, but
     * may be overriden to change the protocol on a per-request
     * basis.  If not set then server.protocol should be used instead.
     */

    public String serverProtocol;

    //-----------------------------------------------------------------------

    protected int statusCode;
    protected String statusPhrase;

    /**
     * The HTTP response headers.  Keys and values in this table correspond
     * to the HTTP headers that will be written back to the client when
     * the response is sent.  The order of entries in this table corresponds
     * to the order in which the HTTP headers will be sent.  Multiple header
     * lines with the same key will be stored as separate entries in the
     * table.
     *
     * @see	#addHeader(String, String)
     */
    public MimeHeaders responseHeaders;

    /*
     * True if the headers have already been sent, so that if sendError()
     * is called in the middle of sending a response, it won't send the
     * headers again, but will cause the connection to be closed afterwards.
     */
    protected boolean headersSent;

    /**
     * Time stamp for start of this request - set, but not used.
     */
    public long startMillis;

    /**
     * Create a new http request.  Requests are created by the server for
     * use by handlers.
     *
     * @param	server
     *		The server that owns this request.
     *
     * @param	sock
     *		The socket of the incoming HTTP request.
     */
    protected Request(Server server, Socket sock)
    {
	this.server = server;
	this.sock = sock;

	try {
	    in = new HttpInputStream(
		    new BufferedInputStream(sock.getInputStream()));
	    out = new HttpOutputStream(
		    new BufferedOutputStream(sock.getOutputStream()));
	} catch (IOException e) {
	    /*
	     * Logically we shouldn't get an error obtaining the streams from
	     * the socket, but if it does, it will be caught later as a
	     * NullPointerException by the Connection.run() method the first
	     * time we attempt to read from the socket.
	     */
	}
	
	requestsLeft = server.maxRequests;
	keepAlive = true;

	headers = new MimeHeaders();
	responseHeaders = new MimeHeaders();
	serverProtocol = null;
    }

    /**
     * Needed by VelocityFilter.Vrequest.  Should not be used to create
     * a <code>Request</code> object.
     */
    protected Request() {}

    /**
     * Returns a string representation of this <code>Request</code>.
     * The string representation is the first line (the method line) of the
     * HTTP request that this <code>Request</code> is handling.  Useful for
     * debugging.
     *
     * @return	The string representation of this <code>Request</code>.
     */
    public String
    toString()
    {
	StringBuffer sb = new StringBuffer();

	sb.append(method).append(' ').append(url);
	if ((query != null) && (query.length() > 0)) {
	    sb.append('?').append(query);
	}
	sb.append(' ').append(protocol);
	sb.append(" (").append(sock.toString()).append(")");
	return sb.toString();
    }
	
    /**
     * Reads an HTTP request from the socket.
     *
     * @return	<code>true</code> if the request was successfully read and
     *		parsed, <code>false</code> if the request was malformed.
     *
     * @throws	IOException
     *		if there was an IOException reading from the socket.  See
     *		the socket documentation for a description of socket
     *		exceptions.
     */
    public boolean
    getRequest()
	throws IOException
    {
	if (server.props.get("debugProps") != null) {
	    if (props != null) {
		props.dump(false, "at beginning of getRequest");
	    }
	}

	/*
	 * Reset state.
	 */
	 
	requestsLeft--;
	connectionHeader = "Connection";
	
	/* I don't think we need to do this
	while ((props = serverProps.getPrior()) != null) {
	    props.remove();
	}
	*/

	method = null;
	url = null;
	query = null;
	protocol = "HTTP/1.1";
	headers.clear();
	postData = null;

	statusCode = 200;
	statusPhrase = "OK";
	responseHeaders.clear();
	startMillis = System.currentTimeMillis();
	out.bytesWritten=0;

	serverProtocol=null;

	/*
	 * Get first line of HTTP request (the method line).
	 */
	 
	String line = null;
	int count = 0;
	while (count++ < MAX_BLANKS) {
	    line = in.readLine(MimeHeaders.MAX_LINE);
	    if (line == null) {
		return false;
	    } else if (line.length() > 0) {
		break;
	    }
	    log(Server.LOG_INFORMATIONAL, "Skipping blank line");
	}

	if (count >= MAX_BLANKS) {
	    throw new IOException("Too many leading blanks in HTTP request");
	}

	log(Server.LOG_LOG, "Request " + requestsLeft + " " + line);

	try {
	    StringTokenizer st = new StringTokenizer(line);
	    method = st.nextToken();
	    url = st.nextToken();
	    protocol = st.nextToken();
	} catch (NoSuchElementException e) {
	    sendError(400, line, null);
	    return false;
	}

/*
	if ((method.equals("GET") == false)
		&& (method.equals("POST") == false)
		&& (method.equals("PUT") == false)) {
	    sendError(501, method, null);
	    return false;
	}
*/	

	if (protocol.equals("HTTP/1.0")) {
	    version = 10;
	} else if (protocol.equals("HTTP/1.1")) {
	    version = 11;
	} else {
	    sendError(505, line, null);
	    return false;
	}
	    
	/*
	 * Separate query string from URL.
	 */

	int index = url.indexOf('?');
	if (index >= 0) {
	    query = url.substring(index + 1);
	    url = url.substring(0, index);
	} else {
	    query = "";
	}

	headers.read(in);

	/*
	 * Remember POST data.  "Transfer-Encoding: chunked" is not handled
	 * yet.
	 */

	String str;

	str = getRequestHeader("Content-Length");
	if (str != null) {
	    int len;
	    try {
		len=Integer.parseInt(str);
		if (len > server.maxPost) {
		    log(Server.LOG_DIAGNOSTIC, "Request", "too much post data");
		    sendError(413, len + " bytes is too much data to post", null);
		    return false;
		}
		postData = new byte[len];
	    } catch (Exception e) {
		sendError(411, str, null);
		return false;
	    } catch (OutOfMemoryError e) {
		log(Server.LOG_DIAGNOSTIC, "Request", "out of memory for post data");
		sendError(413, str, null);
		return false;
	    }
	    log(Server.LOG_DIAGNOSTIC, "Request", "Reading content: " + str);
	    in.readFully(postData);
	}

	str = getRequestHeader(connectionHeader);
	if ("Keep-Alive".equalsIgnoreCase(str)) {
	    keepAlive = true;
	} else if ("close".equalsIgnoreCase(str)) {
	    keepAlive = false;
	} else if (version > 10) {
	    keepAlive = true;
	} else {
	    keepAlive = false;
	}

	/*
         * Delay initialization until we know we need these things
         */
	serverProps = new PropertiesList(server.props);
	props = new PropertiesList();
	props.addBefore(serverProps);

	/*
	 * Keep track of the original url.  This is backwards (for
	 * historical reasons
	 */

        props.put("url.orig", url);

	return true;
    }

    boolean
    shouldKeepAlive()
    {
	return (requestsLeft > 0) && keepAlive;
    }

    /**
     * The socket from which the HTTP request was received, and to where the
     * HTTP response will be written.  The user should not directly read from
     * or write to this socket.  The socket is provided other purposes, for
     * example, imagine a handler that provided different content depending
     * upon the IP address of the client.
     *
     * @return	The client socket that issued this HTTP request.
     */
    public Socket
    getSocket()
    {
	return sock;
    }

    /**
     * Logs a message by calling <code>Server.log</code>.  Typically a
     * message is generated on the console or in a log file, if the
     * <code>level</code> is less than the current server log setting. 
     *
     * @param	level
     *		The severity of the message.  
     *
     * @param	message
     *		The message that will be logged.
     *
     * @see	Server#log(int, Object, String)
     */
    public void
    log(int level, String message)
    {
	log(level, null, message);
    }

    /**
     * Logs a message by calling <code>Server.log</code>.  Typically a
     * message is generated on the console or in a log file, if the
     * <code>level</code> is less than the current server log setting. 
     *
     * @param	level
     *		The severity of the message.  
     *
     * @param	obj
     *		The object that the message relates to.
     *
     * @param	message
     *		The message that will be logged.
     *
     * @see	Server#log(int, Object, String)
     */
    public void
    log(int level, Object obj, String message)
    {
	server.log(level, obj, message);
    }

    /*
     *-----------------------------------------------------------------------
     * Request methods.
     *-----------------------------------------------------------------------
     */

    /**
     * Returns the value that the given case-insensitive key maps to
     * in the HTTP request headers.  In order to do fancier things like
     * changing or deleting an existing request header, the user may directly
     * access the <code>headers</code> field.
     *
     * @param	key
     *		The key to look for in the HTTP request headers.  May not
     *		be <code>null</code>.
     *
     * @return	The value to which the given key is mapped, or
     *		<code>null</code> if the key is not in the headers.
     *
     * @see	#headers
     */
    public String
    getRequestHeader(String key)
    {
	return headers.get(key);
    }

   /**
     * Retrieves the query data as a hashtable.
     * This includes both the query information included as part of the url 
     * and any posted "application/x-www-form-urlencoded" data.
     *
     * @param   table
     *		An existing hashtable in which to put the query data as
     *		name/value pairs.  May be <code>null</code>, in which case
     *		a new hashtable is allocated.
     *
     * @return	The hashtable in which the query data was stored.
     */
    public Hashtable
    getQueryData(Hashtable table)
    {
	if (table == null) {
	    table = new Hashtable();
	}
	HttpUtil.extractQuery(query, table);
	if (postData != null) {
	    String contentType = headers.get("Content-Type");
	    if ("application/x-www-form-urlencoded".equals(contentType)) {
		HttpUtil.extractQuery(new String(postData), table);
	    }
	}
	return table;
    }

   /**
     * Retrieves the query data as a hashtable.
     * This includes both the query information included as part of the url 
     * and any posted "application/x-www-form-urlencoded" data.
     *
     * @return	The hashtable in which the query data was stored.
     */
    public Hashtable
    getQueryData()
    {
	return getQueryData(null);
    }

    /*
     *-----------------------------------------------------------------------
     * Response methods.
     *-----------------------------------------------------------------------
     */

    /**
     * Sets the status code of the HTTP response.  The default status
     * code for a response is <code>200</code> if this method is not
     * called.
     * <p>
     * An HTTP status phrase will be chosen based on the given
     * status code.  For example, the status code <code>404</code> will get
     * the status phrase "Not Found".
     * <p>
     * If this method is called, it must be called before
     * <code>sendHeaders</code> is either directly or indirectly called.
     * Otherwise, it will have no effect.
     *
     * @param	code
     *		The HTTP status code, such as <code>200</code> or
     *		<code>404</code>.  If &lt; 0, the HTTP status code will
     *		not be changed.
     *
     * @see	#sendHeaders(int, String, int)
     */
    public void
    setStatus(int code)
    {
	if (code >= 0) {
	    setStatus(code, HttpUtil.getStatusPhrase(code));
	} 
    }

    /**
     * Set the HTTP status code and status phrase of this request.  The given
     * status will be sent to the client when the user directly or indirectly
     * calls the method <code>sendHeaders</code>.  The given status phrase
     * replaces the default HTTP status phrase normally associated with the
     * given status code.
     *
     * @param	code
     *		The HTTP status code, such as <code>200</code> or
     *		<code>404</code>.
     *
     * @param	message
     *		The HTTP status phrase, such as <code>"Okey dokey"</code> or
     *		<code>"I don't see it"</code>.
     *
     * @see	#sendHeaders(int, String, int)
     */
    protected void
    setStatus(int code, String message)
    {
	this.statusCode = code;
	this.statusPhrase = message;
    }

    /**
     * Return the status code.
     */

    public int getStatus() {
        return statusCode;
    }

    /**
     * Return uses of this socket
     */

    public int getReuseCount() {
	return server.maxRequests - requestsLeft;
    }

    /**
     * Adds a response header to the HTTP response.  In order to do fancier
     * things like appending a value to an existing response header, the
     * user may directly access the <code>responseHeaders</code> field.
     * <p>
     * If this method is called, it must be called before
     * <code>sendHeaders</code> is either directly or indirectly called.
     * Otherwise, it will have no effect.
     *
     * @param	key
     *		The header name.  
     *
     * @param	value
     *		The value for the request header.
     *
     * @see	#sendHeaders(int, String, int)
     * @see	#responseHeaders
     */
    public void
    addHeader(String key, String value)
    {
	responseHeaders.add(key, value);
    }

    /**
     * Adds a response header to the HTTP response.  In order to do fancier
     * things like appending a value to an existing response header, the
     * user may directly access the <code>responseHeaders</code> field.
     * <p>
     * If this method is called, it must be called before
     * <code>sendHeaders</code> is either directly or indirectly called.
     * Otherwise, it will have no effect.
     *
     * @param	line
     *		The HTTP response header, of the form
     *		"<code>key</code>: <code>value</code>".
     *
     * @see	#sendHeaders(int, String, int)
     * @see	#responseHeaders
     */
    public void
    addHeader(String line)
    {
	int dots = line.indexOf(':');
	String key = line.substring(0, dots);
	String value = line.substring(dots + 1).trim();
	addHeader(key, value);
    }

    /**
     * Sends an HTTP response to the client.  
     * <p>
     * This method first calls <code>sendHeaders</code> to send the HTTP
     * response headers, then sends the given byte array as the HTTP
     * response body. If the request method is HEAD, or the result
     * code is "204", the body is not sent.
     * <p>
     * The "Content-Length" will be set to the length of the given byte array.
     * The "Content-Type" will be set to the given MIME type.
     *
     * @param	body
     *		The array of bytes to send as the HTTP response body.  May
     *		not be <code>null</code>.
     *
     * @param	type
     *		The MIME type of the response, such as "text/html".  May be
     *		<code>null</code> to use the existing "Content-Type"
     *		response header (if any).
     *
     * @throws	IOException
     *		if there was an I/O error while sending the response to
     *		the client.
     *
     * @see	#sendHeaders(int, String, int)
     */
    public void
    sendResponse(byte[] body, String type)
	throws IOException
    {
	if (statusCode == 204) {
	    sendHeaders(-1, type, 0);
	} else {
	    sendHeaders(-1, type, body.length);
	    if (!method.equals("HEAD")) {
		out.write(body);
	    }
	}
    }

    /**
     * Sends an HTTP response to the client.  
     * <p>
     * This method first calls <code>sendHeaders</code> to send the HTTP
     * response headers.  It then writes out the given string to the client
     * as a sequence of bytes.  Each character in the string is written out
     * by discarding its high eight bits.
     * <p>
     * The "Content-Length" will be set to the length of the string.
     * The "Content-Type" will be set to the given MIME type.
     * <p>
     * Note: to use a different character encoding, use
     * <code>sendResponse(body.getBytes(encoding)...)</code> instead.
     *
     * @param	body
     *		The string to send as the HTTP response body.  May
     *		not be <code>null</code>. If the request method is HEAD,
     *          the body is not sent.
     *
     * @param	type
     *		The MIME type of the response, such as "text/html".  May be
     *		<code>null</code> to preserve the existing "Content-Type"
     *		response header (if any).
     *
     * @param	code
     *		The HTTP status code for the response, such as
     *		<code>200</code>.  May be &lt; 0 to preserve the existing
     *		status code.
     *
     * @throws	IOException
     *		if there was an I/O error while sending the response to
     *		the client. 
     *
     * @see	#sendHeaders(int, String, int)
     */
    public void
    sendResponse(String body, String type, int code)
	throws IOException
    {
	if (statusCode == 204) {
	    sendHeaders(-1, type, 0);
	} else {
	    sendHeaders(code, type, body.length());
	    if (!"HEAD".equals(method)) {
		out.writeBytes(body);
	    }
	}
    }

    /**
     * Convenience method that sends an HTTP response to the client
     * with a "Content-Type" of "text/html" and the default HTTP status
     * code.
     *
     * @param	body
     *		The string to send as the HTTP response body.
     *
     * @see	#sendResponse(String, String, int)
     */
    public void
    sendResponse(String body)
	throws IOException
    {
	sendResponse(body, "text/html", -1);
    }

    /**
     * Convenience method that sends an HTTP response to the client
     * with the default HTTP status code.
     * 
     * @param	body
     *		The string to send as the HTTP response body.  
     *		If the request method is HEAD,
     *          only the headers are sent to the client.
     *
     * @param	type
     *		The MIME type of the response.
     *
     * @see	#sendResponse(String, String, int)
     */
    public void
    sendResponse(String body, String type)
	throws IOException
    {
	sendResponse(body, type, -1);
    }

    /**
     * Sends the contents of the given input stream as the HTTP response.
     * <p>
     * This method first calls <code>sendHeaders</code> to send the HTTP
     * response headers.  It then transfers a total of <code>length</code>
     * bytes of data from the given input stream to the client as the
     * HTTP response body.
     * <p>
     * This method takes care of setting the "Content-Length" header
     * if the actual content length is known, or the "Transfer-Encoding"
     * header if the content length is not known (for HTTP/1.1 clients only).
     * <p>
     * This method may set the <code>keepAlive</code> to <code>false</code>
     * before returning, if fewer than <code>length</code> bytes could be
     * read. If the request method is HEAD, only the headers are sent.
     *
     * @param	in
     *		The input stream to read from.  
     *
     * @param	length
     *		The content length.  The number of bytes to send to the
     *		client.  May be &lt; 0, in which case this method will read
     *		until reaching the end of the input stream.
     * 
     * @param	type
     *		The MIME type of the response, such as "text/html".  May be
     *		<code>null</code> to preserve the existing "Content-Type"
     *		response header (if any).
     *
     * @param	code
     *		The HTTP status code for the response, such as
     *		<code>200</code>.  May be &lt; 0 to preserve the existing
     *		status code.
     *
     * @throws	IOException
     *		if there was an I/O error while sending the response to
     *		the client. 
     */
    public void
    sendResponse(InputStream in, int length, String type, int code)
	throws IOException
    {
	HttpInputStream hin = new HttpInputStream(in);

	byte[] buf = new byte[server.bufsize];
	    
	if (length >= 0) {
	    sendHeaders(code, type, length);
	    if (!method.equals("HEAD")) {
	       if (hin.copyTo(out, length, buf) != length) {
		   keepAlive = false;
	       }
	    }
	} else if (version <= 10) {
	    keepAlive = false;
	    sendHeaders(code, type, -1);
	    if (!method.equals("HEAD")) {
	       hin.copyTo(out, -1, buf);
	    }
	} else {
	    if (method.equals("HEAD")) {
	        sendHeaders(code, type, -1);
		return;
	    }

	    addHeader("Transfer-Encoding", "chunked");
	    sendHeaders(code, type, -1);

	    while (true) {
		int count = hin.read(buf);
		if (count < 0) {
		    out.writeBytes("0\r\n\r\n");
		    break;
		}
		out.writeBytes(Integer.toHexString(count) + "\r\n");
		out.write(buf, 0, count);
		out.writeBytes("\r\n");
	    }
	}
    }

    /**
     * Sends a HTTP error response to the client.  
     *
     * @param	code
     *		The HTTP status code.
     *
     * @param	clientMessage
     *		A short message to be included in the error response
     *		and logged to the server.
     */
    public void
    sendError(int code, String clientMessage)
    {
	sendError(code, clientMessage, null);
    }

    /**
     * Sends a HTTP error response to the client.  
     *
     * @param	code
     *		The HTTP status code.
     *
     * @param	clientMessage
     *		A short message to be included in the error response.
     *
     * @param	logMessage
     *		A short message to be logged to the server.  This message is
     *		<b>not</b> sent to the client.
     */
    public void
    sendError(int code, String clientMessage, String logMessage)
    {
	setStatus(code);
	server.errorCount++;

	String message = clientMessage;
	if (message == null) {
	    message = logMessage;
	    logMessage = null;
	}
	log(Server.LOG_LOG, "Error",
		statusCode + " " + statusPhrase + ": " + message);
	if (logMessage != null) {
	    log(Server.LOG_LOG, logMessage);
	}

	keepAlive = false;
	if (headersSent) {
	    /*
	     * The headers have already been sent.  We can't send an error
	     * message in the middle of an existing response, so just close
	     * this request.
	     */

	    return;
	}

        String body = "<html>\n<head>\n"
		+ "<title>Error: " + statusCode + "</title>\n"
		+ "<body>\nGot the error: <b>"
		+ statusPhrase
                + "</b><br>\nwhile trying to obtain <b>"
                + ((url == null) ? "unknown URL" : HttpUtil.htmlEncode(url))
                + "</b><br>\n"
                + HttpUtil.htmlEncode(clientMessage)
                + "\n</body>\n</html>";
 
        try {
	    sendResponse(body, "text/html", statusCode);
        } catch (IOException e) {
            /*
             * Don't throw an error in the process of sending an error
             * message!
             */
        }
    }

    /**
     * Sends the HTTP status line and response headers to the client.  This
     * method is automatically invoked by <code>sendResponse</code>, but
     * can be manually invoked if the user needs direct access to the
     * client's output stream.  If this method is not called, then the
     * HTTP status and response headers will not automatically be sent to
     * the client; the user would be responsible for forming the entire
     * HTTP response.
     * <p>
     * The user may call the <code>addHeader</code> method or modify the
     * <code>responseHeaders</code> field before calling this method.
     * This method then adds a number of HTTP headers, as follows: <ul>
     * <li> "Date" - the current time, if this header is not already present.
     * <li> "Server" - the server's name (from <code>server.name</code>), if
     *	    this header is not already present.
     * <li> "Connection" - "Keep-Alive" or "close", depending upon the
     *	    <code>keepAlive</code> field.
     * <li> "Content-Length" - set to the given <code>length</code>.
     * <li> "Content-Type" - set to the given <code>type</code>.
     * </ul>
     * <p>
     * The string used for "Connection" header actually comes from the
     * <code>connectionHeader</code> field.
     *
     * @param	code
     *		The HTTP status code for the response, such as
     *		<code>200</code>.  May be &lt; 0 to preserve the existing
     *		status code.
     *
     * @param	type
     *		The MIME type of the response, such as "text/html".  May be
     *		<code>null</code> to preserve the existing "Content-Type"
     *		response header (if any).
     *
     * @param	length
     *		The length of the response body.  May be &lt; 0 if the length
     *		is unknown and/or to preserve the existing "Content-Length"
     *		response header (if any).
     *
     * @throws	IOException
     *		if there was an I/O error while sending the headers to
     *		the client. 
     *
     * @see	#setStatus(int)
     * @see	#addHeader(String, String)
     * @see	#sendResponse(String, String, int)
     * @see	#connectionHeader
     */
    public void
    sendHeaders(int code, String type, int length)
	throws IOException
    {
	setStatus(code);
        if ((length == 0) && (statusCode == 200)) {
	    /*
	     * No Content.
	     */
            setStatus(204);
        }

	responseHeaders.putIfNotPresent("Date", HttpUtil.formatTime());
	if (server.name != null) {
	    responseHeaders.putIfNotPresent("Server", server.name);
	}
	String str = shouldKeepAlive() ? "Keep-Alive" : "close";
	responseHeaders.put(connectionHeader, str);
        if (length >= 0) {
	    responseHeaders.put("Content-Length", Integer.toString(length));
	}
	if (type != null) {
	    responseHeaders.putIfNotPresent("Content-Type", type);
	}

	out.sendHeaders(this);
	// System.out.println("*** Sending headers: " +url+ " " + responseHeaders);
	headersSent = true;
    }

    /**
     * Send the response headers to the client.
     *  This consists of standard plus added headers.  The handler is reponsible
     *  for sending the reponse body.
     *
     * @param type	The document mime type
     * @param length	the document length
     * 
     * @see Request#addHeader(String)
     * @see Request#sendResponse(String)
     * @see Request#setStatus(int)
     */


    /**
     * Responds to an HTTP request with a redirection reply, telling the
     * client that the requested url has moved.  Generally, this is used if
     * the client did not put a '/' on the end of a directory.
     *
     * @param	url
     *		The URL the client should have requested.  This URL may be
     *		fully-qualified (in the form "http://....") or host-relative
     *		(in the form "/...").
     *
     * @param	body
     *		The body of the redirect response, or <code>null</code> to
     *		send a hardcoded message.
     */
    public void
    redirect(String url, String body)
	throws IOException
    {
	if (url.startsWith("/")) {
	    url = serverUrl() + url;
	}
	addHeader("Location", url);
	if (body == null) {
	    body = "<title>Moved</title><h1>look for <a href=" +
		url + ">" + url + "</h1>";
	}
	sendResponse(body, "text/html", 302);
    }

    /**
     * Returns the server's fully-qualified base URL.  This is "http://"
     * followed by the server's hostname and port.
     * <p>
     * If the HTTP request header "Host" is present, it specifies the
     * hostname and port that will be used instead of the server's internal
     * name for itself.  Due bugs in certain browsers, when using the server's
     * internal name, the port number will be elided if it is <code>80</code>.
     *
     * @return	The string representation of the server's URL.
     */
    public String
    serverUrl()
    {
	String host = headers.get("Host");
	if (host == null) {
	    host = server.hostName;
	}
	int index = host.lastIndexOf(":");
	if ((index < 0) && (server.listen.getLocalPort() != 80)) {
	    host += ":" + server.listen.getLocalPort();
	}
	if (serverProtocol != null) {
	    return serverProtocol + "://" + host;
	} else {
	    return server.protocol + "://" + host;
	}
    }

    /**
     * The <code>HttpOutputStream</code> provides the convenience method
     * <code>writeBytes</code> for writing the byte representation of a
     * string, without bringing in the overhead and the deprecated warnings
     * associated with a <code>java.io.DataOutputStream</code>.
     * <p>
     * The other methods in this class are here to allow the 
     * <code>FilterHandler</code> and <code>ChainSawHandler</code> to
     * alter the behavior in an implementation specific way.  This behavior
     * is unfortunate, and might go away when a better strategy comes along.
     */
    public static class HttpOutputStream
	extends FilterOutputStream
    {
        /**
         * Count the number of bytes that are written to this stream
         */

        public int bytesWritten = 0;

	public
	HttpOutputStream(OutputStream out)
	{
	    super(out);
	}

	public void
	writeBytes(String s)
	    throws IOException
	{
	    int len = s.length();
	    for (int i = 0; i < len; i++) {
		this.out.write((byte) s.charAt(i));
	    }
	    bytesWritten += len;
	}

        public void write(byte b) throws IOException {
            this.out.write(b);
	    bytesWritten++;
        }

        public void
        write(byte[] buf, int off, int len) throws IOException {
            this.out.write(buf, off, len);
	    bytesWritten += len;
        }

	public void
	sendHeaders(Request request)
	    throws IOException
	{
	    writeBytes(request.protocol + " " + request.statusCode + " " +
		    request.statusPhrase + "\r\n");
	    request.responseHeaders.print(this.out);
	    writeBytes("\r\n");
	}
    }

    /**
     * Adds the given <code>Dictionary</code> to the set of properties that
     * are searched by <code>request.props.getProperty()</code>.  This method
     * is used to optimize the case when the caller has an existing
     * <code>Dictionary</code> object that should be added to the search
     * chain.
     * <p>
     * Assume the caller is constructing a new <code>Properties</code>
     * object and wants to chain it onto the front of
     * <code>request.props</code>.  The following code is appropriate:
     * <code><pre>
     * /&#42; Push a temporary Dictionary onto request.props. &#42;/
     * PropertiesList old = request.props;
     * (new PropertiesList()).addBefore(request.props);
     * request.props = request.props.getPrior();
     * request.props.put("foo", "bar");
     * request.props.put("baz", "garply");
     *
     * /&#42; Do something that accesses new properties. &#42;/
     *     .
     *     .
     *     .
     *
     * /&#42; Restore old Dictionary when done. &#42;/
     * request.props.remove();
     * request.props = old;
     * </pre></code>
     * However, <code>addSharedProps</code> may be called when the caller
     * has an existing set of <code>Properties</code> and is faced with
     * copying its contents into <code>request.props</code> and/or trying
     * to share the existing <code>Properties</code> object among multiple
     * threads concurrently.
     * <code><pre>
     * /&#42; Some properties created at startup. &#42;/
     * static Properties P = new Properties();
     *     .
     *     .
     *     .
     * /&#42; Share properties at runtime. &#42;/
     * request.addSharedProps(P);
     * </pre></code>is more efficient and esthetically pleasing than:
     * <code><pre>
     * foreach key in P.keys() {
     *     request.props.put(key, P.get(key));
     * }
     * </pre></code>
     * The given <code>Dictionary</code> object is added to the
     * <code>Properties.getProperty()</code> search chain before serverProps;
     * it will be searched after the 
     * <code>request.props</code> and before <code>serverProps</code>.
     * Multiple <code>Dictionary</code> objects can be added and they will
     * be searched in the order given.  The same <code>Dictionary</code>
     * object can be added multiple times safely.  However, the search
     * chain for the given <code>Dictionary</code> must not refer back to
     * <code>request.props</code> itself or a circular chain will be
     * created causing an infinite loop:
     * <code><pre>
     * request.addSharedProps(request.props);	            // Bad
     * request.addSharedProps(request.props.getWrapped());  // Good
     * Properties d1 = new Properties(request.props);
     * request.addSharedProps(d1);                          // Bad
     * Hashtable d2 = new Hashtable();
     * Properties d3 = new Properties();
     * request.addSharedProps(d2);		            // Good
     * request.addSharedProps(d3);		            // Good
     * </pre></code>
     * Subsequent calls to <code>request.props.getProperty()</code> may
     * fetch properties from an added <code>Dictionary</code>, but
     * <code>request.put()</code> will <b>not</b> modify those dictionaries.
     *
     * @param	d
     *		A <code>Dictionary</code> of <code>String</code> key/value
     *		pairs that will be added to the chain searched
     *		when <code>request.props.getProperty()</code> is called.  The
     *		dictionary <code>d</code> is "live", meaning that external
     *		changes to the contents of <code>d</code> will be seen on
     *		subsequent calls to <code>request.props.getProperty()</code>.
     *
     * @return	<code>false</code> if the dictionary had already been added
     *		by a previous call to this method, <code>true</code>
     *		otherwise.
     */
    public boolean
    addSharedProps(Dictionary d)
    {
	boolean debug = server.props.get("debugProps") != null;
	if (props.wraps(d) != null) {
	    if (debug) {
		System.out.println("addSharedProps didn't add dict"
			   + Integer.toHexString(System.identityHashCode(d)));
	    }
	    return false;
	}
	PropertiesList pl = new PropertiesList(d);
	pl.addAfter(props);
	if (debug) {
	    props.dump(true, "at addSharedProps");
	}
	return true;
    }

    /**
     * Removes a <code>Dictionary</code> added by
     * <code>addSharedProps</code>.  <code>Dictionary</code> objects may
     * be removed in any order.  <code>Dictionary</code> objects do not need
     * to be removed; they will automatically get cleaned up at the end of
     * the request.
     *
     * @param	d
     *		The <code>Dictionary</code> object to remove from the
     *		<code>request.props.getProperty()</code> search chain.
     *
     * @return	<code>true</code> if the <code>Dictionary</code> was found
     *		and removed, <code>false</code> if the <code>Dictionary</code>
     *		was not found (it had already been removed or had never been
     *		added).
     */
    public boolean
    removeSharedProps(Dictionary d)
    {
	PropertiesList pl = props.wraps(d);
	if (pl != null && pl != props && pl != serverProps) {
	    pl.remove();
	    if (server.props.get("debugProps") != null) {
		pl.dump(true, "at removeSharedProps");
	    }
	    return true;
	}
	return false;
    }
}
