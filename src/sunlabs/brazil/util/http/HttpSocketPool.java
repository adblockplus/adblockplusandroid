/*
 * HttpSocketPool.java
 *
 * Brazil project web application toolkit,
 * export version: 2.3 
 * Copyright (c) 1999-2004 Sun Microsystems, Inc.
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
 * Contributor(s): cstevens, suhler.
 *
 * Version:  2.2
 * Created by cstevens on 99/09/15
 * Last modified by suhler on 04/11/30 15:19:40
 *
 * Version Histories:
 *
 * 2.2 04/11/30-15:19:40 (suhler)
 *   fixed sccs version string
 *
 * 2.1 02/10/01-16:36:55 (suhler)
 *   version change
 *
 * 1.6 99/10/14-13:23:44 (cstevens)
 *   Accepted child's version in workspace "/home/cstevens/ws/brazil/naws".
 *
 * 1.4.1.1 99/10/14-13:03:55 (cstevens)
 *   Documentation.
 *   Move implementation of this interface into HttpRequest, since that is the
 *   only place that uses the concrete implementation.
 *
 * 1.5 99/10/11-12:39:25 (suhler)
 *   Merged changes between child workspace "/home/suhler/brazil/naws" and
 *   parent workspace "/net/mack.eng/export/ws/brazil/naws".
 *
 * 1.4 99/10/07-13:19:23 (cstevens)
 *   Make HttpSocketPool a public interface so that users of HttpRequest can
 *   implement it to provide their own cache and policy.
 *
 * 1.3.1.1 99/10/06-12:34:25 (suhler)
 *   reformat debugging
 *
 * 1.3 99/10/01-11:21:35 (suhler)
 *   added toString
 *
 * 1.2 99/09/15-14:52:25 (cstevens)
 *   import *;
 *
 * 1.2 99/09/15-14:39:37 (Codemgr)
 *   SunPro Code Manager data about conflicts, renames, etc...
 *   Name history : 2 1 request/HttpSocketPool.java
 *   Name history : 1 0 util/http/HttpSocketPool.java
 *
 * 1.1 99/09/15-14:39:36 (cstevens)
 *   date and time created 99/09/15 14:39:36 by cstevens
 *
 */

package sunlabs.brazil.util.http;

import java.io.IOException;

/**
 * This interface represents a cache of idle sockets.  Once a request has
 * been handled, the now-idle socket can be remembered and reused later in
 * case another HTTP request is made to the same remote host.  Currently, the
 * only instance of this interface is used by the <code>HttpRequest</code>
 * class.
 * 
 * @author      Colin Stevens (colin.stevens@sun.com)
 * @version		2.2
 */
public interface HttpSocketPool
{
    /**
     * Returns an <code>HttpSocket</code> that can be used to communicate
     * with the specified port on the named host.
     * <p>
     * It is this method's responsibility to to fill in all the public
     * member variables of the <code>HttpSocket</code> before returning.
     * <p>
     * For each call to this method, there should eventually be a call to
     * <code>close</code> when the <code>HttpSocket</code> isn't needed
     * anymore.
     *
     * @param	host
     *		The host name.
     *
     * @param	port
     *		The port number.
     *
     * @param	reuse
     *		<code>true</code> to request that this pool attempt to find
     *		and reuse an existing idle connection, <code>false</code>
     *		to request that this pool establish a new connection to
     *		the named host.
     *
     * @return	The <code>HttpSocket</code>.
     *		
     * @throws	IOException
     *		if there is a problem connecting to the specified port on
     *		the named host.  The <code>IOException</code>s (and 
     *		subclasses) that might be thrown depend upon how the
     *		socket connection is established.  See the socket
     *		documentation for further details.  Some subclasses that
     *		might be thrown are as follows:
     *
     * @throws	java.io.UnknownHostException
     *		if the host name cannot be resolved.
     *
     * @throws	java.io.ConnectionException
     *		if the named host is not listening on the specified port.
     *
     * @throws	java.io.InterruptedIOException
     *		if the connection times out or this thread is interrupted by
     *		<code>Thread.interrupt</code>.
     */
    public HttpSocket get(String host, int port, boolean reuse)
	throws IOException;

    /**
     * Releases an <code>HttpSocket</code> to this pool when it is not
     * in use any more.
     * <p>
     * It is this method's responsibility to release resources used
     * by the <code>HttpSocket</code>, such as closing the underlying socket.
     * <p>
     * After calling this method, the user should not refer to the specified
     * <code>HttpSocket</code> any more.
     *
     * @param	hs
     *		The <code>HttpSocket</code> to release.
     *
     * @param	reuse
     *		<code>true</code> if the specified <code>HttpSocket</code>
     *		should be put back into the idle pool, <code>false</code>
     *		if it should be released immediately.
     */
    public void close(HttpSocket hs, boolean reuse);
}
