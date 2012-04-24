/*
 * UseProxy.java
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
 * Created by cstevens on 99/09/29
 * Last modified by suhler on 04/11/30 15:19:40
 *
 * Version Histories:
 *
 * 2.2 04/11/30-15:19:40 (suhler)
 *   fixed sccs version string
 *
 * 2.1 02/10/01-16:37:09 (suhler)
 *   version change
 *
 * 1.4 00/05/22-14:06:11 (suhler)
 *   doc updates
 *
 * 1.3 99/10/14-14:32:24 (cstevens)
 *   documentation
 *
 * 1.2 99/10/04-16:02:54 (cstevens)
 *   Documentation for LexML and StringMap.
 *
 * 1.2 99/09/29-16:14:25 (Codemgr)
 *   SunPro Code Manager data about conflicts, renames, etc...
 *   Name history : 1 0 proxy/UseProxy.java
 *
 * 1.1 99/09/29-16:14:24 (cstevens)
 *   date and time created 99/09/29 16:14:24 by cstevens
 *
 */

package sunlabs.brazil.proxy;

/**
 * This interface is used by the {@link ProxyHandler} class to
 * decide whether to issue an HTTP request directly to the
 * specified host, or to issue the request via an HTTP proxy.
 *
 * @author	Colin Stevens (colin.stevens@sun.com)
 * @version		2.2
 */
public interface UseProxy
{
    /**
     * Determines if the user can issue a direct or proxy request to the
     * specified host and port.
     * <p>
     * The actual HTTP proxy to use is specified external to this routine by
     * some other mechanism.
     *
     * @param	host
     *		The host name.
     *
     * @param	port
     *		The port number.
     *
     * @return	<code>true</code> if the user should send the HTTP request
     *		via an HTTP proxy, <code>false</code> if the user can
     *		send the HTTP request directly to the specified named host.
     */
    public boolean useProxy(String host, int port);
}
    
