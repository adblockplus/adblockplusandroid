/*
 * Handler.java
 *
 * Brazil project web application toolkit,
 * export version: 2.3 
 * Copyright (c) 1998-2004 Sun Microsystems, Inc.
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
 * Version:  2.3
 * Created by suhler on 98/09/14
 * Last modified by suhler on 04/11/30 15:19:41
 *
 * Version Histories:
 *
 * 2.3 04/11/30-15:19:41 (suhler)
 *   fixed sccs version string
 *
 * 2.2 03/07/28-09:17:39 (suhler)
 *   streamline docs (this is included a lot)
 *
 * 2.1 02/10/01-16:34:51 (suhler)
 *   version change
 *
 * 1.8 01/08/21-10:58:46 (suhler)
 *   doc lint
 *
 * 1.7 01/08/07-11:38:36 (suhler)
 *   doc updates
 *
 * 1.6 01/08/07-11:03:07 (suhler)
 *   added documentation
 *
 * 1.5 99/11/17-15:36:59 (cstevens)
 *   documentation
 *
 * 1.4 99/10/14-13:10:59 (cstevens)
 *   @author & @version
 *
 * 1.3 99/03/30-09:24:49 (suhler)
 *   documentation updates
 *
 * 1.2 98/09/21-14:51:08 (suhler)
 *   changed the package names
 *
 * 1.2 98/09/14-18:03:06 (Codemgr)
 *   SunPro Code Manager data about conflicts, renames, etc...
 *   Name history : 2 1 server/Handler.java
 *   Name history : 1 0 Handler.java
 *
 * 1.1 98/09/14-18:03:05 (suhler)
 *   date and time created 98/09/14 18:03:05 by suhler
 *
 */

package sunlabs.brazil.server;

import java.io.IOException;

/**
 * The interface for writing HTTP handlers.  Provides basic functionality
 * to accept HTTP requests and dispatch to methods that handle the request.
 * <p>
 * The {@link #init(Server, String)} method is called before this
 * <code>Handler</code> processes the first HTTP request, to allow it to
 * prepare itself, such as by allocating any resources needed for the
 * lifetime of the <code>server</code>.
 * <p>
 * The {@link #respond(Request)} method is called to handle an HTTP request.
 * This method, and all methods it calls must be thread-safe since they may
 * handle HTTP requests from multiple sockets concurrently.  However, each
 * concurrent request gets its own individual {@link Request} object.
 * <p>
 * Any instance variables should be initialized in the
 * {@link #init(Server, String)}, and only referenced, but not set in the
 * {@link #respond(Request)} method.  If any state needs to be retained, 
 * it should be done either by associating it with the {@link Request}
 * object, or using the
 * {@link sunlabs.brazil.session.SessionManager session manager}.
 * Class statics should be avoided, as it is possible, and even common to
 * run multiple unrelated Brazil servers in the same JVM.  As above, the
 * {@link sunlabs.brazil.session.SessionManager session manager}
 * should be used instead.
 *
 * @author	Stephen Uhler (stephen.uhler@sun.com)
 * @author	Colin Stevens (colin.stevens@sun.com)
 * @version		2.3
 */

public interface Handler {
    /**
     * Initializes the handler.
     *
     * @param	server
     *		The HTTP server that created this <code>Handler</code>.
     *		Typical <code>Handler</code>s will use {@link Server#props}
     *		to obtain run-time configuration information.
     *
     * @param	prefix
     *		The handlers <i>name</i>.
     *		The string this <code>Handler</code> may prepend to all
     *		of the keys that it uses to extract configuration information
     *		from {@link Server#props}.  This is set (by the {@link Server}
     *		and {@link ChainHandler}) to help avoid configuration parameter
     *		namespace collisions.
     *
     * @return	<code>true</code> if this <code>Handler</code> initialized
     *		successfully, <code>false</code> otherwise.  If
     *		<code>false</code> is returned, this <code>Handler</code>
     *		should not be used.
     */
    boolean init(Server server, String prefix);

    /**
     * Responds to an HTTP request.
     *
     * @param	request
     *		The <code>Request</code> object that represents the HTTP
     *		request.
     *
     * @return	<code>true</code> if the request was handled.  A request was
     *		handled if a response was supplied to the client, typically
     *		by calling <code>Request.sendResponse()</code> or
     *		<code>Request.sendError</code>.
     *
     * @throws	IOException
     *		if there was an I/O error while sending the response to
     *		the client.  Typically, in that case, the <code>Server</code>
     *		will (try to) send an error message to the client and then
     *		close the client's connection.
     *		<p>
     *		The <code>IOException</code> should not be used to silently
     *		ignore problems such as being unable to access some
     *		server-side resource (for example getting a
     *		<code>FileNotFoundException</code> due to not being able
     *		to open a file).  In that case, the <code>Handler</code>'s
     *		duty is to turn that <code>IOException</code> into a
     *		HTTP response indicating, in this case, that a file could
     *		not be found.
     */
    boolean respond(Request request) throws IOException;
}
