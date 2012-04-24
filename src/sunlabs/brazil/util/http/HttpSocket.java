/*
 * HttpSocket.java
 *
 * Brazil project web application toolkit,
 * export version: 2.3 
 * Copyright (c) 1999-2002 Sun Microsystems, Inc.
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
 * Version:  2.1
 * Created by cstevens on 99/09/15
 * Last modified by suhler on 02/10/01 16:36:54
 *
 * Version Histories:
 *
 * 2.1 02/10/01-16:36:54 (suhler)
 *   version change
 *
 * 1.5 99/10/14-14:16:55 (cstevens)
 *   way to differentiate amongst httpsockets to same target host.
 *
 * 1.4 99/10/14-13:19:50 (cstevens)
 *   Merged changes between child workspace "/home/cstevens/ws/brazil/naws" and
 *   parent workspace "/export/ws/brazil/naws".
 *
 * 1.2.1.1 99/10/14-13:01:47 (cstevens)
 *   Documentation.
 *
 * 1.3 99/10/06-12:33:17 (suhler)
 *   changed toString
 *
 * 1.2 99/10/01-11:38:16 (suhler)
 *
 * 1.2 99/09/15-14:39:37 (Codemgr)
 *   SunPro Code Manager data about conflicts, renames, etc...
 *   Name history : 2 1 request/HttpSocket.java
 *   Name history : 1 0 util/http/HttpSocket.java
 *
 * 1.1 99/09/15-14:39:36 (cstevens)
 *   date and time created 99/09/15 14:39:36 by cstevens
 *
 */

package sunlabs.brazil.util.http;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import sunlabs.brazil.util.SocketFactory;

/**
 * This class is used as the bag of information kept about a open, idle
 * socket.  It is not meant to be used externally by anyone except someone
 * writing a new implementation of an <code>HttpSocketPool</code> for
 * the <code>HttpRequest</code> object.
 * <p>
 * This class should not be visible at this scope.  It is only here until
 * a better place for it is found.
 */
public class HttpSocket
{
    public String host;
    public int port;

    public boolean firstTime = true;
    public long lastUsed;
    public int timesUsed = 1;

    public Socket sock;
    public InputStream in;
    public OutputStream out;

    private static int count = 0;
    private int serial;

    public
    HttpSocket(String host, int port)
	throws IOException, UnknownHostException 
    {
	this.host = host;
	this.port = port;

	SocketFactory socketFactory = HttpRequest.socketFactory;
	if (socketFactory == null) {
	    socketFactory = SocketFactory.defaultFactory;
	}

	sock = socketFactory.newSocket(host, port);
	in = new BufferedInputStream(sock.getInputStream());
	out = new BufferedOutputStream(sock.getOutputStream());

	serial = count++;
    }

    void
    close()
    {
	in = null;
	out = null;

	if (sock != null) {
	    try {
		sock.close();
	    } catch (IOException e) {}
	}

	sock = null;
    }

    public String
    toString()
    {
	return host + ":" + port + "-" + serial + "-" + timesUsed;
    }
}
