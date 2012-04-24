/*
 * Server.java
 *
 * Brazil project web application toolkit,
 * export version: 2.3 
 * Copyright (c) 1998-2005 Sun Microsystems, Inc.
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
 * Version:  2.4
 * Created by suhler on 98/09/14
 * Last modified by suhler on 05/07/12 12:46:41
 *
 * Version Histories:
 *
 * 2.4 05/07/12-12:46:41 (suhler)
 *   better failure diags
 *
 * 2.3 04/11/30-15:11:26 (suhler)
 *   fixed sccs version string
 *
 * 2.2 04/09/14-13:48:15 (suhler)
 *   added "restart" method to enable server restarts with new configuration
 *
 * 2.1 02/10/01-16:34:53 (suhler)
 *   version change
 *
 * 1.46 02/02/12-09:17:47 (suhler)
 *   Don't exit silently if localHost can't resolve.
 *   .
 *
 * 1.45 01/08/21-10:59:39 (suhler)
 *   add "maxPost" to limit size of request data
 *
 * 1.44 01/08/13-09:09:30 (drach)
 *   Change server.props back to Properties object.
 *
 * 1.43 01/08/07-14:19:29 (drach)
 *   Move PropertiesList debug initialization to earliest possible point.
 *
 * 1.42 01/08/03-15:29:58 (drach)
 *   Add PropertiesList
 *
 * 1.41 01/07/16-16:51:09 (suhler)
 *   bump default server id
 *
 * 1.40 01/06/05-22:13:06 (drach)
 *   Reduce access control for brazil.servlet package
 *
 * 1.39 01/04/12-11:28:46 (suhler)
 *   remove shadow "name"
 *
 * 1.38 01/04/12-10:23:48 (suhler)
 *   Fixed a performance bug (introduced version 1.18) that caused a
 *   reverse DNS lookup on every connection bu calling:
 *   sock.getInetAddress().getHostName()
 *
 * 1.37 01/03/12-17:30:19 (cstevens)
 *   Close listening socket when Server finished running, to make testing easier.
 *
 * 1.36 01/03/05-16:22:06 (cstevens)
 *   log message consistency
 *
 * 1.35 00/11/06-10:53:17 (suhler)
 *   Make initFailure public
 *
 * 1.34 00/10/17-09:31:42 (suhler)
 *   Added instance variable "initFailure" which may be set externally, causing
 *   the server to stop after initialization completes.
 *
 * 1.33 00/06/28-15:22:24 (cstevens)
 *   Merged changes between child workspace "/home/cstevens/ws/brazil/naws" and
 *   parent workspace "/export/ws/brazil/naws".
 *
 * 1.30.1.1 00/06/28-15:16:59 (cstevens)
 *
 * 1.32 00/05/31-13:51:41 (suhler)
 *   docs
 *
 * 1.31 00/05/17-10:39:09 (suhler)
 *   Added null constructor so Server can be instantiaded with newInstance()
 *
 * 1.30 00/04/12-15:59:41 (cstevens)
 *   Server uses initHandler to init initial handler.
 *
 * 1.29 00/03/29-16:43:59 (cstevens)
 *   documentation
 *
 * 1.28 00/03/10-17:11:49 (cstevens)
 *   Formatting of log messages.
 *
 * 1.27 99/11/17-15:37:17 (cstevens)
 *   init
 *
 * 1.26 99/11/16-19:09:18 (cstevens)
 *   expunge Server.initHandler and Server.initObject.
 *
 * 1.25 99/11/16-14:37:07 (cstevens)
 *   Server.java:
 *   1. Name printed for thread in log message when a connection is accepted
 *   should agree with the name printed for the thread when the Request does
 *   something.
 *   2. Thread.interrupt() issues interacting with the test scripts.
 *
 * 1.24 99/11/09-20:24:30 (cstevens)
 *   bugs revealed by writing tests.
 *
 * 1.23 99/11/03-17:52:45 (cstevens)
 *   MultiHostHandler.
 *
 * 1.22 99/10/26-18:54:59 (cstevens)
 *   Eliminate public methods Server.initHandler() and Server.initObject().
 *   Get rid of public variables Request.server and Request.sock:
 *   A. In all cases, Request.server was not necessary; it was mainly used for
 *   constructing the absolute URL for a redirect, so Request.redirect() was
 *   rewritten to take an absolute or relative URL and do the right thing.
 *   B. Request.sock was changed to Request.getSock(); it is still rarely used
 *   for diagnostics and logging (e.g., ChainSawHandler).
 *
 * 1.21 99/10/25-15:40:34 (cstevens)
 *   spelling
 *
 * 1.20 99/10/25-15:39:16 (cstevens)
 *   Don't make initObject and initHandler public
 *
 * 1.19 99/10/14-14:57:38 (cstevens)
 *   resolve wilcard imports.
 *
 * 1.18 99/10/14-14:16:21 (cstevens)
 *   merge issues.  logging "null" should be ignored.
 *
 * 1.17 99/10/14-13:17:32 (cstevens)
 *   Merged changes between child workspace "/home/cstevens/ws/brazil/naws" and
 *   parent workspace "/export/ws/brazil/naws".
 *
 * 1.15.1.3 99/10/14-13:11:08 (cstevens)
 *   @author & @version
 *
 * 1.15.1.2 99/10/14-12:56:36 (cstevens)
 *   Server.initFields removed
 *   Server.initHandler and Server.initObject have extensive documentation now
 *   that will hopefully make their existence compelling.
 *
 * 1.16 99/10/11-12:37:10 (suhler)
 *   Merged changes between child workspace "/home/suhler/brazil/naws" and
 *   parent workspace "/net/mack.eng/export/ws/brazil/naws".
 *
 * 1.14.1.1 99/10/11-12:32:42 (suhler)
 *   Change to server, allowing it to stop.  This uses undocumented
 *   behavior of the JDK, so I'm ntsure its a good idea
 *
 * 1.15.1.1 99/10/08-16:53:35 (cstevens)
 *
 * 1.15 99/10/07-13:01:39 (cstevens)
 *   javadoc lint.
 *
 * 1.14 99/10/04-17:16:28 (cstevens)
 *   merge conflict
 *
 * 1.13 99/10/04-16:11:09 (cstevens)
 *   Merged changes between child workspace "/home/cstevens/ws/brazil/naws" and
 *   parent workspace "/export/ws/brazil/naws".
 *
 * 1.11.1.2 99/10/01-13:07:06 (cstevens)
 *   Getting better with server.initHandler();
 *
 * 1.12 99/10/01-11:44:05 (suhler)
 *   Merged changes between child workspace "/home/suhler/brazil/naws" and
 *   parent workspace "/net/mack.eng/export/ws/brazil/naws".
 *
 * 1.11.1.1 99/10/01-11:27:15 (cstevens)
 *   Change logging to show prefix of Handler generating the log message.
 *
 * 1.11 99/09/30-12:12:14 (cstevens)
 *   better logging.
 *   handler= line in config file can contain either name of class, in which
 *   case prefix will be "" or a symbolic name, in which case prefix will be
 *   name + ".".  This preserved backwards compatibility, but allows symbolic
 *   names to be used.
 *
 * 1.10 99/09/29-16:10:56 (cstevens)
 *   Consistent way of initializing Handlers and other things that want to get
 *   attributes from the config file.  Convenience method that constructs the
 *   object, sets (via reflection) all the variables in the object that correspond
 *   to values specified in the config file, and then calls init() on the object.
 *
 * 1.9.1.1 99/09/22-16:04:08 (suhler)
 *   - put each server in its own thread group
 *   - change the way log messages are generated (the "socket" argument is
 *   no longer used and should be removed
 *   - added a close() to stop the server (untested)
 *
 * 1.9 99/07/22-15:04:39 (suhler)
 *   Added public maxThreads field to limit the max # of threads that
 *   may be spawned by the server at once
 *
 * 1.8 99/06/29-14:35:09 (suhler)
 *   added serverUrl method that overrides the default host name
 *
 * 1.7 99/06/04-13:55:01 (suhler)
 *   modify log level
 *
 * 1.6 99/03/30-09:27:19 (suhler)
 *   - documentation update
 *   - bug fix: too many open files would kill server
 *
 * 1.5 99/02/03-14:13:09 (suhler)
 *   added defaultPrefix to override the default, ("")
 *
 * 1.4 98/12/09-15:04:12 (suhler)
 *   added thread count to log message
 *
 * 1.3 98/09/21-14:52:05 (suhler)
 *   changed the package names
 *
 * 1.2 98/09/17-17:59:14 (rinaldo)
 *
 * 1.2 98/09/14-18:03:10 (Codemgr)
 *   SunPro Code Manager data about conflicts, renames, etc...
 *   Name history : 2 1 server/Server.java
 *   Name history : 1 0 Server.java
 *
 * 1.1 98/09/14-18:03:09 (suhler)
 *   date and time created 98/09/14 18:03:09 by suhler
 *
 */

package sunlabs.brazil.server;

import sunlabs.brazil.properties.PropertiesList;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Properties;

/**
 * Yet another HTTP/1.1 server.
 * This class is the core of a light weight Web Server.  This server
 * is started as a Thread listening on the supplied port, and
 * dispatches to an implementation of
 * a {@link Handler} to service http requests.  If no handler is
 * supplied, then the {@link FileHandler} is used.
 * A {@link ChainHandler} is provided to allow multiple handlers in one server.
 * <p>
 * Limitations:
 * <ul>
 * <li>Starts a new thread for each connection.  This may be expensive.  
 * </ul>
 *
 * @author	Stephen Uhler (stephen.uhler@sun.com)
 * @author	Colin Stevens (colin.stevens@sun.com)
 * @version	2.4
 */

public class Server 
    extends Thread 
{
    /**
     * The listening socket.  Every time a new socket is accepted,
     * a new thread is created to read the HTTP requests from it.  
     */
    public ServerSocket listen;

    /**
     * The main Handler whose <code>respond</code> method is called for
     * every HTTP request.  The <code>respond</code> method must be
     * thread-safe since it handles HTTP requests concurrently from all the
     * accepted sockets.
     *
     * @see	Handler#respond
     */
    private String handlerName;
    public Handler handler;

    /**
     * Hashtable containing arbitrary information that may be of interest to
     * a Handler.  This table is available to both methods of the
     * {@link Handler} interface, as {@link Server#props} in the
     * {@link Handler#init(Server, String)}
     * method, and as the default properties of
     * {@link Request#props} in the {@link Handler#respond(Request)}
     * method.
     */
    
    public Properties props = null;

    /**
     * The hostname that this Server should use to identify itself in
     * an HTTP Redirect.  If <code>null</code>, the hostname is derived
     * by calling <code>InetAddress.getHostAddress</code>.
     * <p>
     * <code>InetAddress.getHostName</code> would generally be the wrong
     * thing to return because it returns only the base machine name
     * <code>xxx</code> and not the machine name as it needs to appear
     * to the rest of the network, such as <code>xxx.yyy.com</code>.
     * <p>
     * The default value is <code>null</code>.
     */

    public String hostName = null;

    /**
     * The protocol used to access this resource.  Normally <code>http</code>, but
     * can be changed for <code>ssl</code> to <code>https</code>
     */

    public String protocol = "http";

    /**
     * If non-null, restrict connections to just the specified ip addresses.
     * <p>
     * The default value is <code>null</code>.
     */
    public InetAddress[] restrict = null;

    /**
     * The string to return as the value for the "Server:" line in the HTTP
     * response header.  If <code>null</code>, then no "Server:" line is
     * returned.
     */
    public String name = "Brazil/2.0";

    /**
     * The handler is passed a prefix to identify which items in the
     * properties object are relevent.  By convention, non-empty strings
     * end with ".", allowing nested prefixes to be easily distinguished.
     */

    public String prefix = "";

    /**
     * Time in milliseconds before this Server closes an idle socket or
     * in-progress request.
     * <p>
     * The default value is <code>30000</code>.
     */
    public int timeout = 30000;

    /**
     * Maximum number of consecutive requests allowed on a single
     * kept-alive socket.
     * <p>
     * The default value is <code>25</code>.
     */
    public int maxRequests = 25;

    /**
     * The max number of threads allowed for the entire VM
     * (default is 250).
     */
    public int maxThreads = 250;

    /**
     * Maximum amout of POST data allowed per request (in bytes)
     * (default = 2Meg).
     */
    public int maxPost = 2097152;		// 2 Meg

    /**
     * Default buffer size for copies to and from client sockets.  
     * (default is 8192)
     */
    public int bufsize = 8192;
    
    /**
     * Count of accepted connections so far.
     */
    public int acceptCount = 0;
    
    /**
     * Count of HTTP requests received so far.
     */
    public int requestCount = 0;

    /**
     * Count of errors that occurred so far.
     */
    public int errorCount = 0;

    /**
     * The diagnostic level. 0->least, 5->most
     */

    public int logLevel = LOG_LOG;

    /**
     * If set, the server will terminate with an initialization failure
     * just before creating the listen socket.
     */

    public boolean initFailure = false;

    ThreadGroup group;

    /**
     * Create a server using the provided listener socket.  
     * <p>
     * This server will call the <code>Handler.respond</code> method
     * of the specified handler.  The specified handler should either
     * respond to the request or perform further dispatches to other
     * handlers.
     *
     * @param	listen
     *		The socket this server should listen to.
     *		For ordinary sockets, this is simply: <code>
     *		new ServerSocket(port)</code>, where <code>port</code>
     *		is the network port to listen on.  Alternate implementations
     *		of <code>ServerSocket</code>, such as <b>ssl</b> versions
     *		may be used instead.
     * @param   handlerName
     *		The name of the handler used to process http requests.
     *		It must implement the {@link Handler} interface.
     * @param	props
     *		Arbitrary information made available to the handler.
     *		May be <code>null</code>.
     *
     * @see	FileHandler
     * @see	ChainHandler
     */

    public
    Server(ServerSocket listen, String handlerName, Properties props)
    {
	setup(listen, handlerName, props);
    }

    /**
     * Set up the server.  this allows a server to be created with 
     * newInstance() followed by setup(), instead of using the
     * above initializer, making it easier to start sub-classes
     * of the server.
     */
    public Server() {}

    public boolean
    setup(ServerSocket listen, String handlerName, Properties props)
    {
	if (this.props != null) {
	    return false;	// alreasdy initialized
	}
	if (props == null) {
	    props = new Properties();
	}
	this.listen = listen;
	this.handlerName = handlerName;
	this.props=props;
	if (props.get("debugProps") != null) {
	    PropertiesList.debug = true;
	}
	return true;
    }

    public boolean
    init() {
	if (props == null) {
	    log(LOG_ERROR, "server", "Not properly initialized!");
	    return false;
	}
        group = new ThreadGroup(prefix);
	if (hostName == null) {
	    try {
	       hostName = InetAddress.getLocalHost().getHostAddress();
	    } catch (UnknownHostException e) {
	       log(LOG_ERROR, "server",
	           "Can't find my own name, using \"localhost\"" +
		   " (redirects may not work)");
	       hostName="localhost";
	    }

	}
	if (Thread.currentThread().getName().startsWith("Thread-")) {
	    Thread.currentThread().setName("server");
	}

	handler = ChainHandler.initHandler(this, prefix, handlerName);

	if (handler == null) {
	    return false;
	}
	if (initFailure) {
	    log(LOG_ERROR, handlerName, "Initilization failure");
	    return false;
	}
	return true;
    }

    /**
     * Restart the server with a new handler.
     * @param newHandler	Name of the handler to restart the server with
     */

    public synchronized boolean restart(String newHandler) {
	String oldHandlerName=handlerName;
	Handler oldHandler=handler;

	handlerName=newHandler;
	if (init()) {
	    log(LOG_INFORMATIONAL, this, "restarting with: " + newHandler);
	    return true;
	} else {
	    log(LOG_WARNING, this, newHandler +
		    " is invalid, retaining old handler");
	    handlerName = oldHandlerName;
	    handler=oldHandler;
	    return false;
	}
    }

    /**
     * Loops, accepting socket connections and replying to HTTP requests.
     * This is called indirectly via Thread.start().
     * <p>
     * Many things in the server are not initialized until this point,
     * because the user may have set some related configuration options
     * between the time this server was allocated and the time it was
     * started.  For instance, the main <code>Handler</code> is not
     * initialized until now, because its <code>Handler.init</code> method
     * may have wanted to examine server member variables such as
     * <code>hostName</code> or <code>bufsize</code>.
     */
    public void
    run()
    {
	try {
	    if (init() == false) {
		return;
	    }

	    listen.setSoTimeout(0);
	    while (true) {
		/*
		 * Blocks until we have a connection on the socket.
		 */
		Socket sock = listen.accept();
		String threadName = sock.getInetAddress().getHostAddress();
		log(LOG_INFORMATIONAL, threadName, "new connection");

		allowed:
		if (restrict != null) {
		    InetAddress addr = sock.getInetAddress();
		    for (int i = 0; i < restrict.length; i++) {
			if (restrict[i].equals(addr)) {
			    break allowed;
			}
		    }
		    log(LOG_DIAGNOSTIC, addr, "rejected request");
		    sock.close();
		    continue;
		}

		// A pseudo-busy loop!!!

		boolean warn=false;
		while (Thread.activeCount() > maxThreads) {
		    if (!warn) {
			log(LOG_WARNING, sock, 
				"Too many threads: " + maxThreads);
		    }
		    Thread.yield();
		    warn = true;
		}

		new Thread(group, new Connection(this, sock),
			threadName + "-" + acceptCount).start();
		acceptCount++;
	    }
	} catch (IOException e) {
	     System.err.println("Server failed to start: " + e);
	} finally {
	    try {
		listen.close();

		Thread[] sub = new Thread[100];
		int count;
		while ((count = group.enumerate(sub, true)) > 0) {
		    for (int i = 0; i < count; i++) {
			sub[i].interrupt();
			sub[i].join();
		    }
		    yield();
		}
	    } catch (Exception e) {}

	    group = null;
	}
    }

    /**
     * Stop the server, and kill all pending requests
     */
    public void
    close()
    {
	try {
	    this.interrupt();
	    this.join();
	} catch (Exception e) {}

	log(LOG_WARNING, null, "server stopped");
    }

    public static final int LOG_ERROR=1;		// most severe
    public static final int LOG_WARNING=2;
    public static final int LOG_LOG=3;
    public static final int LOG_INFORMATIONAL=4;
    public static final int LOG_DIAGNOSTIC=5;	// least useful

    /**
     * Logs information about the socket to <code>System.out</code>.  
     *
     * @param	level	    Controls the verbosity (0=least 5=most)
     * @param	obj	    The object that the message relates to.
     * @param	message	    The message to be logged.
     */

    public void 
    log(int level, Object obj, String message)
    {
	if (level <= logLevel) {
	    System.out.print("LOG: " + level + " " + prefix
		    + listen.getLocalPort() + "-"
		    + Thread.currentThread().getName() + ": ");
	    if (obj != null) {
		System.out.print(obj);
		System.out.print(": ");
	    }
	    System.out.println(message);
	}
    }
}
