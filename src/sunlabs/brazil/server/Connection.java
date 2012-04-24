/*
 * Connection.java
 *
 * Brazil project web application toolkit,
 * export version: 2.3 
 * Copyright (c) 1998-2006 Sun Microsystems, Inc.
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
 * Contributor(s): cstevens, suhler.
 *
 * Version:  2.5
 * Created by suhler on 98/09/14
 * Last modified by suhler on 06/04/25 14:27:23
 *
 * Version Histories:
 *
 * 2.5 06/04/25-14:27:23 (suhler)
 *   better error diagnostics
 *
 * 2.4 05/12/08-13:02:50 (suhler)
 *   better diagnostics
 *
 * 2.3 05/07/14-11:47:20 (suhler)
 *   add more diagnostic output at log>=5
 *
 * 2.2 04/11/30-15:19:41 (suhler)
 *   fixed sccs version string
 *
 * 2.1 02/10/01-16:34:50 (suhler)
 *   version change
 *
 * 1.17 02/04/08-15:58:58 (suhler)
 *   change log level of request done from 4 to 3
 *
 * 1.16 01/09/13-09:26:31 (suhler)
 *   remove uneeded import
 *
 * 1.15 01/03/12-17:26:47 (cstevens)
 *   Unused argument
 *
 * 1.14 00/05/15-12:01:47 (suhler)
 *   reset request.props defaults
 *
 * 1.13 99/11/16-13:33:55 (cstevens)
 *   Connection.java:
 *   1. Name printed for thread in log message when a connection is accepted
 *   should agree with the name printed for the thread when the Request does
 *   something.
 *   2. When sending 404 to client, the log message should indicate the filename.
 *   3. Thread.interrupt() issues.
 *
 * 1.12 99/11/09-20:23:33 (cstevens)
 *   bugs revealed by writing tests.
 *
 * 1.11 99/10/14-13:13:34 (cstevens)
 *   Merged changes between child workspace "/home/cstevens/ws/brazil/naws" and
 *   parent workspace "/export/ws/brazil/naws".
 *
 * 1.9.1.2 99/10/14-12:50:54 (cstevens)
 *   log message shouldn't display socket twice.
 *
 * 1.10 99/10/11-12:31:33 (suhler)
 *   chnge connection to be a runnable, not a thread
 *
 * 1.9.1.1 99/10/08-16:43:31 (cstevens)
 *   leaking threads.
 *
 * 1.9 99/10/01-11:26:58 (cstevens)
 *   Change logging to show prefix of Handler generating the log message.
 *
 * 1.8 99/09/15-14:40:51 (cstevens)
 *   Rewritign http server to make it easier to proxy requests.
 *
 * 1.7 99/09/15-13:24:40 (cstevens)
 *   Rewriting http server to make proxying easier.
 *
 * 1.6 99/03/30-09:22:59 (suhler)
 *   documentation updates
 *
 * 1.5 99/01/29-11:48:32 (suhler)
 *   new proxy interface
 *
 * 1.4 99/01/27-12:08:47 (suhler)
 *   added log message for socket I/O error
 *
 * 1.3 98/11/04-17:28:59 (suhler)
 *   Close sockets in a "finally" clause, to insure they
 *   REALLY close
 *   .
 *
 * 1.2 98/09/21-14:50:39 (suhler)
 *   changed the package names
 *
 * 1.2 98/09/14-18:03:04 (Codemgr)
 *   SunPro Code Manager data about conflicts, renames, etc...
 *   Name history : 2 1 server/Connection.java
 *   Name history : 1 0 Connection.java
 *
 * 1.1 98/09/14-18:03:03 (suhler)
 *   date and time created 98/09/14 18:03:03 by suhler
 *
 */

package sunlabs.brazil.server;

import java.net.Socket;
import java.io.IOException;
import java.io.InterruptedIOException;

/**
 * Internal "helper" class to manage http connections.
 * Create a thread that lives for the duration of the client socket and handles
 * multiple HTTP requests.  Packages each HTTP request from the socket into
 * an HttpRequest object and passes it to the <code>respond</code> method of
 * the server's main HttpHandler.  If an error occurs while handling a
 * request, the socket is closed and this thread ends.
 *
 * @author	Colin Stevens
 * @version		2.5
 */
class Connection implements Runnable
{
    /**
     * The Server that created this handler.
     */
    Server server;

    /**
     * The client socket.
     */
    Socket sock;
    
    /**
     * The current request state
     */
    Request request;

    /**
     * Constructs a new Connection and starts it running.
     */
    Connection(Server server, Socket sock)
    {
	this.server = server;
	this.sock = sock;

	request = new Request(server, sock);
    }

    /**
     * Loop reading HTTP requests from the socket until there is an error,
     * the client requests that the socket be closed, or the client exceeds
     * the maximum number of requests allowed on a single socket.
     */
    public void
    run()
    {
	try {
	    sock.setSoTimeout(server.timeout);

	    while (request.shouldKeepAlive()) {
		if (request.getRequest() == false) {
		    break;
		}
		server.requestCount++;
		if (server.handler.respond(request) == false) {
		    request.sendError(404, null, request.url);
		}
		request.out.flush();
		server.log(Server.LOG_LOG, null, "request done");
	    }
	} catch (InterruptedIOException e) {
	    /*
	     * A read timed out, or (rarely) this thread was interrupted.
	     *
	     * Thread.interrupt() generates an InterruptedIOException that
	     * cannot be 100% discriminated from an InterruptedIOException
	     * caused by a read timeout.
	     *
	     * Under jdk-1.1, a Thread.interrupt() call generates an
	     * InterruptedIOException with the detail message
	     * "operation interrupted".
	     *
	     * Under jdk-1.2, a Thread.interrupt() call generates an
	     * InterruptedIOException with the detail message
	     * "Interrupted system call".
	     *
	     * In order to make the automated test scripts easier to write
	     * and run under both jdk-1.2 and jdk-1.1, suppress the varying
	     * InterruptedIOException log messages due to Thread.interrupt(),
	     * which only happens when the server is being shut down by
	     * the test script anyhow.
	     */

	    String msg = e.getMessage();
	    if ((msg == null) || (msg.indexOf("terrupted") < 0)) { 
		request.sendError(408, msg, null);
	    }
	} catch (IOException e) {
	    /*
	     * Expected exception, due to not being able to write back to
	     * client, etc.
	     */
	    server.log(Server.LOG_LOG, null, "Connection broken by client: " +
		e.getMessage());
	    if (server.logLevel >= Server.LOG_DIAGNOSTIC) {
		e.printStackTrace();
	    }
	} catch (Exception e) {
	    /* 
	     * Unexpected exception.
	     */

	    if (server.logLevel >= Server.LOG_DIAGNOSTIC) {
		e.printStackTrace();
	    }
	    request.sendError(500, e.toString(), "unexpected error");
	} finally {
	    server.log(Server.LOG_INFORMATIONAL, null, "socket close");
	    try {
	    	request.out.flush();
	    } catch (IOException e) {}
	    try {
		sock.close();
	    } catch (IOException e) {}
	}
    }
}
