/*
 * SocketFactory.java
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
 * Last modified by suhler on 04/11/30 15:19:45
 *
 * Version Histories:
 *
 * 2.2 04/11/30-15:19:45 (suhler)
 *   fixed sccs version string
 *
 * 2.1 02/10/01-16:37:00 (suhler)
 *   version change
 *
 * 1.8 00/03/10-17:12:29 (cstevens)
 *   Fix wildcard and unneeded imports
 *
 * 1.7 99/10/26-18:55:36 (cstevens)
 *   Eliminate unused SocketFactory.newSocket(InetAddress, int) method.
 *
 * 1.6 99/10/14-12:58:08 (cstevens)
 *   Documentation
 *
 * 1.5 99/10/04-16:12:47 (cstevens)
 *   Merged changes between child workspace "/home/cstevens/ws/brazil/naws" and
 *   parent workspace "/export/ws/brazil/naws".
 *
 * 1.3.1.1 99/10/04-16:03:27 (cstevens)
 *   wildcard imports
 *
 * 1.4 99/10/01-11:38:01 (suhler)
 *   no *'s
 *
 * 1.3 99/09/29-16:12:02 (cstevens)
 *   SocketFactory is an interface.
 *
 * 1.2 99/09/15-14:51:50 (cstevens)
 *   import *;
 *
 * 1.2 99/09/15-14:37:44 (Codemgr)
 *   SunPro Code Manager data about conflicts, renames, etc...
 *   Name history : 1 0 util/SocketFactory.java
 *
 * 1.1 99/09/15-14:37:43 (cstevens)
 *   date and time created 99/09/15 14:37:43 by cstevens
 *
 */

package sunlabs.brazil.util;

import java.io.IOException;
import java.net.Socket;

/**
 * This interface is used as a heap to control the allocation of sockets.
 * <p>
 * An instance of this interface can be passed to methods that allocate
 * sockets.  In this way, the actual, underlying type of socket allocated
 * can be replaced (for instance, with an SSL socket or an firewall-tunnelling
 * socket), without the user of the socket having to explicitly be aware of
 * the underlying implementation.
 * <p>
 * In some ways, this class is a replacement for the
 * <code>SocketImplFactory</code> class.  This class addresses the following
 * issues. <ul>
 * <li>
 * A <code>SocketImplFactory</code> may be installed only once for the entire
 * process, so different policies cannot be used concurrently and/or
 * consectively.  For instance, imagine a situation where the user wants one
 * part of the program talking via SSL to some port on machine A and via
 * standard sockets to some port on machine B.  It is not possible to
 * install separate <code>SocketImplFactory</code> objects to allow both.
 * <li>
 * The standard <code>Socket</code> class presumes a highly-connected network
 * with the ability to resolve hostnames to IP addresses.  The standard
 * <code>Socket</code> class always converts the hostname to an IP address
 * before calling <code>SocketImplFactory</code>.  If the hostname does not
 * have an IP address, then the <code>SocketImplFactory</code> never gets a
 * chance to intercept the host name and perform alternate routing based on
 * the name.  For instance, imagine that the user has implemented a
 * firewall-tunnelling socket; the raw hostname must be passed to the
 * firewall machine, which allows the socket to be established once some
 * out-of-band credentials are supplied.  But we could never get this far
 * because the standard <code>Socket</code> class would have already rejected
 * the request since the IP address of the target machine was unknown.
 * </ul>
 *
 * @author	Colin Stevens (colin.stevens@sun.com)
 * @version		2.2
 */
public interface SocketFactory
{
    /**
     * The default socket factory.  It just creates a standard
     * <code>Socket</code> to the specified host and port, and is exactly
     * equivalent to calling <code>new Socket(host, port)</code>.
     */
    public final SocketFactory defaultFactory = new DefaultSocketFactory();

    /**
     * Creates a new <code>Socket</code> that talks to the specified port
     * on the named host.
     * <p>
     * The implementation may choose any way it wants to provide a
     * socket-like object (essentially any mechanism that supports
     * bidirectional communication).  The returned <code>Socket</code> (or
     * subclass of <code>Socket</code>) might not be based on TCP/IP, or it
     * might involve running a TCP/IP stack over some other protocol, or it
     * might actually redirect all connections via some other proxy machine,
     * etc.
     *
     * @param	host
     *		The host name.
     *
     * @param	port
     *		The port number.
     *
     * @return	An object that provides socket-like communication.
     *
     * @throws	IOException
     *		If there is some problem establishing the socket to the
     *		specified port on the named host.
     */
    public Socket newSocket(String host, int port) throws IOException;
}

class DefaultSocketFactory
    implements SocketFactory
{
    public Socket
    newSocket(String host, int port)
	throws IOException
    {
	return new Socket(host, port);
    }
}
