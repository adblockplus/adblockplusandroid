/*
 * ProxyHandler.java
 *
 * Brazil project web application toolkit,
 * export version: 2.3 
 * Copyright (c) 1999-2006 Sun Microsystems, Inc.
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
 * Version:  2.6
 * Created by cstevens on 99/09/15
 * Last modified by suhler on 06/11/08 10:27:34
 *
 * Version Histories:
 *
 * 2.6 06/11/08-10:27:34 (suhler)
 *   fixed stupid type, docs, and added rewriteHeaders
 *   ,
 *   .
 *   .
 *
 * 2.5 05/07/12-10:37:33 (suhler)
 *   add "https" support
 *
 * 2.4 05/06/08-10:53:03 (suhler)
 *   add "proxylog" option to print http headers
 *
 * 2.3 04/11/30-15:19:40 (suhler)
 *   fixed sccs version string
 *
 * 2.2 03/07/26-16:55:07 (drach)
 *   Move return out of finally clause to eliminate warning about finally clause not able
 *   to complete normally.
 *
 * 2.1 02/10/01-16:37:09 (suhler)
 *   version change
 *
 * 1.22 02/07/24-10:47:23 (suhler)
 *   doc updates
 *
 * 1.21 01/08/03-18:22:17 (suhler)
 *   remove training  ws from classnames before trying to instantiate
 *
 * 1.20 00/12/11-13:31:25 (suhler)
 *   add class=props for automatic property extraction
 *
 * 1.19 00/10/31-10:20:10 (suhler)
 *   doc fixes
 *
 * 1.18 00/04/12-15:56:08 (cstevens)
 *   imports
 *
 * 1.17 00/03/29-16:16:40 (cstevens)
 *   Code to check if proxy server should be used had been removed.
 *
 * 1.16 00/03/10-17:09:43 (cstevens)
 *   Removing unused member variables:
 *
 * 1.15 00/02/25-11:00:15 (suhler)
 *   - added docs
 *   - changed useProxy to do only simple initializations
 *
 * 1.14 99/11/17-10:26:48 (suhler)
 *   Merged changes between child workspace "/home/suhler/brazil/naws" and
 *   parent workspace "/net/mack.eng/export/ws/brazil/naws".
 *
 * 1.13 99/11/16-19:08:29 (cstevens)
 *   wildcard imports.
 *
 * 1.12.1.2 99/11/01-11:57:01 (suhler)
 *   fixed imports
 *
 * 1.12.1.1 99/10/27-13:20:52 (suhler)
 *   Merged changes between child workspace "/home/suhler/brazil/naws" and
 *   parent workspace "/net/mack.eng/export/ws/brazil/naws".
 *
 * 1.11.1.1 99/10/27-13:15:21 (suhler)
 *
 * 1.12 99/10/26-18:13:09 (cstevens)
 *   Eliminate public methods Server.initHandler() and Server.initObject().
 *
 * 1.11 99/10/14-12:46:44 (cstevens)
 *   Remove Server.initFields()
 *
 * 1.10 99/10/11-11:49:10 (cstevens)
 *   Server.initFields has been banished.
 *
 * 1.9 99/10/08-16:43:23 (cstevens)
 *   Move HTTP/1.0 vs. HTTP/1.1 logic for sending HTTP responses into Request.
 *   RemoteHandler correctly handles "Via" header, both ingoing and outgoing.
 *   Move logic for removing point-to-point headers into the HttpRequest as a
 *   static method.
 *
 * 1.8 99/10/07-13:00:03 (cstevens)
 *   javadoc lint
 *
 * 1.7 99/10/04-16:02:48 (cstevens)
 *   Documentation for LexML and StringMap.
 *
 * 1.6 99/10/01-13:06:04 (cstevens)
 *   getting better with server.initHandler()
 *
 * 1.5 99/10/01-11:26:50 (cstevens)
 *   Change logging to show prefix of Handler generating the log message.
 *
 * 1.4 99/09/30-14:11:26 (cstevens)
 *   Better error message if couldn't connect because machine wasn't listening
 *   on specified port, rather than saying "unknown host".
 *
 * 1.3 99/09/30-12:10:12 (cstevens)
 *   better logging
 *
 * 1.2 99/09/29-16:07:03 (cstevens)
 *   Rewrite RemoteHandler (proxy) to use HttpRequest.
 *
 * 1.2 99/09/15-13:31:24 (Codemgr)
 *   SunPro Code Manager data about conflicts, renames, etc...
 *   Name history : 2 1 proxy/ProxyHandler.java
 *   Name history : 1 0 proxy/RemoteHandler.java
 *
 * 1.1 99/09/15-13:31:23 (cstevens)
 *   date and time created 99/09/15 13:31:23 by cstevens
 *
 */

package sunlabs.brazil.proxy;

import sunlabs.brazil.server.Handler;
import sunlabs.brazil.server.Request;
import sunlabs.brazil.server.Server;

import sunlabs.brazil.util.http.HttpInputStream;
import sunlabs.brazil.util.http.HttpRequest;
import sunlabs.brazil.util.http.MimeHeaders;
/**/
import java.io.EOFException;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.UnknownHostException;
import java.net.ConnectException;
import java.util.Properties;

/**
 * Handler for implementing a web proxy.
 * By default, this is a dumb proxy.  It can be combined with other
 * handlers to generate side effects, such as content rewriting.
 * <p>
 * Properties:
 * <dl class=props>
 * <dt>useproxy	<dd>The name of the SocketFactory class to use for
 * this handler.  If additional properties are required to set up
 * the SocketFactory, it should be configured as a handler instead.
 * This is here for convenience only.
 * <dt>auth	<dd>The value of the proxy-authenticate header (if any) sent to the upstream proxy
 * <dt>proxyHost<dd>If specified, the name of the upstream proxy
 * <dt>proxyPort<dd>The up stream proxys port, if a proxyHost is specified (defaults to 80)
 * <dt>proxylog<dd>If set all http headers will be logged to the console.  This
 * is for debugging.
 * </dl>
 *
 * @author      Stephen Uhler
 * @version		2.6
 */
public class ProxyHandler
    implements Handler
{
    public static final String PROXY_HOST = "proxyHost";
    public static final String PROXY_PORT = "proxyPort";
    public static final String AUTH = "auth";
    public static final String USE_PROXY = "useproxy";

    String via;

    /**
     * The proxy server.
     */
    public String proxyHost;

    /**
     * The proxy server's port.  Default is 80.
     */
    public int proxyPort = 80;

    /**
     * The string to send as the value for the "Proxy-Authorization"
     * HTTP header (if needed).
     */
    public String auth;

    boolean shouldLog;	// if true, log all headers

    UseProxy proxyTester;

    /**
     * Do one-time setup.
     * get and process the properties file options, and make sure
     */
    public boolean
    init(Server server, String prefix)
    {
	String str;
	Properties props = server.props;

	proxyHost = props.getProperty(prefix + PROXY_HOST);

	str = props.getProperty(prefix + PROXY_PORT);
	try {
	    proxyPort = Integer.decode(str).intValue();
	} catch (Exception e) {};
	
	auth = props.getProperty(prefix + AUTH);

	shouldLog = (props.getProperty(prefix + "proxylog") != null);

	/*
	 * Set a proxy.  If more sophisicated initialization is required than newinstance(), 
	 * set up the proxy in a separate handler
	 */

	String useproxy = props.getProperty(prefix + USE_PROXY);
	if (useproxy != null) {
	    try {
		Class type = Class.forName(useproxy.trim());
		proxyTester = (UseProxy) type.newInstance();
	    } catch (Exception e) {
		server.log(Server.LOG_WARNING, prefix,
			"Proxy installation error : " + e);
	    }
	}
	if (proxyTester == null) {
	    proxyTester = new UseProxy() {
		public boolean useProxy(String host, int port) { return true; }
	    };
	}
		
	via = " " + server.hostName + ":" + server.listen.getLocalPort()
		+ " (" + server.name + ")";

	return true;

    }

    /**
     * @see Handler#respond
     */
    public boolean
    respond(Request client)
	throws IOException
    {
	String url = client.url;

	if (url.startsWith("http:") == false &&
		url.startsWith("https:") == false) {
	    return false;
	}
	if ((client.query != null) && (client.query.length() > 0)) {
	    url += "?" + client.query;
	}

	MimeHeaders clientHeaders = client.headers;
	int count = client.server.requestCount;
	if (shouldLog) {
	   System.err.println(dumpHeaders(count, client, clientHeaders, true));
	}

	/*
	 * "Proxy-Connection" may be used (instead of just "Connection")
	 * to keep alive a connection between a client and this proxy.
	 */
	String pc = clientHeaders.get("Proxy-Connection");
	if (pc != null) {
	    client.connectionHeader = "Proxy-Connection";
	    client.keepAlive = pc.equalsIgnoreCase("Keep-Alive");
	}

	HttpRequest.removePointToPointHeaders(clientHeaders, false);

        HttpRequest target = new HttpRequest(url);
	try {
	    MimeHeaders targetHeaders = target.requestHeaders;

	    target.setMethod(client.method);
	    clientHeaders.copyTo(targetHeaders);
/*	    targetHeaders.add("Via", client.protocol + via);*/
	    
	    /*
	     * We might need to authenticate to a target proxy.
	     */

	    if ((proxyHost != null)
		    && proxyTester.useProxy(target.host, target.port)) {
		target.setProxy(proxyHost, proxyPort);
		if (auth != null) {
		    targetHeaders.add("Proxy-Authorization", auth);
		}
	    }

	    if (client.postData != null) {
		OutputStream out = target.getOutputStream();
		out.write(client.postData);
		out.close();
	    }

	    target.connect();

	    targetHeaders = target.responseHeaders;
	    rewriteHeaders(targetHeaders);
	    if (shouldLog) {
	       dumpHeaders(count, client, targetHeaders, false);
	       System.err.println("      " + target.status + "\n" +
			dumpHeaders(count, client, targetHeaders, false));
	    }
	    HttpRequest.removePointToPointHeaders(targetHeaders, true);

	    clientHeaders = client.responseHeaders;
	    targetHeaders.copyTo(clientHeaders);
	    try {
		clientHeaders.add("Via",
			target.status.substring(0, 8) + via);
	    } catch (StringIndexOutOfBoundsException e) {
		clientHeaders.add("Via", via);
	    }

	    client.sendResponse(target.getInputStream(),
		    target.getContentLength(), null, target.getResponseCode());
	} catch (InterruptedIOException e) {
	    /*
	     * Read timeout while reading from the remote side.  We use a
	     * read timeout in case the target never responds.  
	     */
	    client.sendError(408, "Timeout / No response");
	} catch (EOFException e) {
	    client.sendError(500, "No response");
	} catch (UnknownHostException e) {
	    client.sendError(500, "Unknown host");
	} catch (ConnectException e) {
	    client.sendError(500, "Connection refused");
	} catch (IOException e) {
	    /*
	     * An IOException will happen if we can't communicate with the
	     * target or the client.  Rather than attempting to discriminate,
	     * just send an error message to the client, and let the send
	     * fail if the client was the one that was in error.
	     */

	    String msg = "Error from proxy";
	    if (e.getMessage() != null) {
		msg += ": " + e.getMessage();
	    }
	    client.sendError(500, msg);
    	} finally {
	    target.close();
	}
	return true;
    }

    /**
     * Allow sub-classes to rewrite any or all of the target
     * headers, if needed.
     */

    protected MimeHeaders rewriteHeaders(MimeHeaders responseHeaders) {
	return responseHeaders;
    }

    /**
     * Dump the headers on stderr
     */

    public static String
    dumpHeaders(int count, Request request, MimeHeaders headers, boolean sent) {
	String prompt;
	StringBuffer sb = new StringBuffer();
	String label = "   " + count;
	label = label.substring(label.length()-4);
	if (sent) {
	    prompt = label + "> ";
	    sb.append(prompt).append(request.toString()).append("\n");
	} else {
	    prompt = label + "< ";
	}

	for (int i = 0; i < headers.size(); i++) {
	    sb.append(prompt).append(headers.getKey(i));
	    sb.append(": ").append(headers.get(i)).append("\n");
	}
	return(sb.toString());
    }
}
