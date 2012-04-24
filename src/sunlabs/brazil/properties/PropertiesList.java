/*
 * PropertiesList.java
 *
 * Brazil project web application toolkit,
 * export version: 2.3 
 * Copyright (c) 2001-2006 Sun Microsystems, Inc.
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
 * The Initial Developer of the Original Code is: drach.
 * Portions created by drach are Copyright (C) Sun Microsystems, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): drach, suhler.
 *
 * Version:  2.8
 * Created by drach on 01/08/03
 * Last modified by suhler on 06/07/31 14:45:24
 *
 * Version Histories:
 *
 * 2.8 06/07/31-14:45:24 (suhler)
 *   added public removeProperty methods that will remove a property anywhere in
 *   a properties list chain
 *
 * 2.7 04/11/30-15:11:26 (suhler)
 *   fixed sccs version string
 *
 * 2.6 04/04/28-13:53:45 (suhler)
 *   addAfter skips all "transient properties" before inserting a new
 *   propertiesList into the chain.  The method "isTransient()" may be
 *   overridden in child classes to make a PropertiesList transient.
 *   This should be a backward compatible change
 *
 * 2.5 03/07/25-16:34:28 (drach)
 *   Create just one PropertiesList.java.  This will on compile on
 *   Java 2 compilers.
 *
 * 2.4 02/11/25-13:33:49 (drach)
 *   Minor cosmetic changes
 *
 * 2.3 02/11/25-12:31:05 (suhler)
 *   doc changes
 *
 * 2.2 02/11/25-12:28:14 (suhler)
 *   Merged changes between child workspace "/home/suhler/brazil/naws" and
 *   parent workspace "/net/mack.eng/export/ws/brazil/naws".
 *
 * 1.10.1.1 02/11/25-11:49:53 (suhler)
 *   Changed the semantics of getProperty() to look in all wrapped dictionaries, not just those that are\
 *   also Properties
 *   Changed the semantics of getNames() similar to the above
 *   .
 *
 * 2.1 02/10/01-16:39:52 (suhler)
 *   version change
 *
 * 1.10 02/05/17-09:42:54 (drach)
 *   Minor cosmetic fix
 *
 * 1.9 02/05/02-12:59:12 (drach)
 *   Update documentation.
 *
 * 1.8 02/05/01-08:52:25 (drach)
 *   Oops, forgot to change one method
 *
 * 1.7 02/05/01-08:45:41 (drach)
 *   Turn all private methods into protected methods
 *
 * 1.6 02/05/01-07:40:29 (drach)
 *   Change from PropertiesList to BasePropertiesList
 *
 * 1.5 01/11/21-11:43:42 (suhler)
 *   doc fixes
 *
 * 1.4 01/08/22-16:23:48 (drach)
 *   Change some methods so they correspond to Request documentation
 *
 * 1.3 01/08/22-14:41:53 (drach)
 *   Add comments to PropertiesList and change some methods.
 *
 * 1.2 01/08/07-14:21:23 (drach)
 *   Fix null pointer exception and add case where PL invokes PL
 *
 * 1.2 01/08/03-15:24:59 (Codemgr)
 *   SunPro Code Manager data about conflicts, renames, etc...
 *   Name history : 3 2 properties/PropertiesList.java
 *   Name history : 2 1 properties/BasePropertiesList.java
 *   Name history : 1 0 properties/PropertiesList.java
 *
 * 1.1 01/08/03-15:24:58 (drach)
 *   date and time created 01/08/03 15:24:58 by drach
 *
 */

package sunlabs.brazil.properties;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;

import java.util.Collection;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import sunlabs.brazil.util.Glob;

/**
 * A <code>PropertiesList</code> instance is intended to be an element of
 * a doubly linked list consisting of other <code>PropertiesList</code>
 * instances.  Each <code>PropertiesList</code> instance "wraps" a
 * <code>Dictionary</code> object.  A <code>PropertiesList</code> is a
 * subclass of <code>Properties</code> and therefore provides the same
 * API, including the methods and fields of <code>Dictionary</code> and
 * <code>Hashtable</code>.  The <code>PropertiesList</code> class
 * overrides all methods of the <code>Properties</code> API and delegates
 * the method evaluation to the wrapped <code>Properties</code> object.
 * <p>
 * The linked list of <code>PropertiesList</code> objects is constructed
 * by <code>Request</code> for each incoming request.  That is, there is
 * a unique <code>PropertiesList</code> linked list for each request.
 * The head of the initial list constructed by <code>request</code> is
 * <code>Request.props</code> and the tail of the two element list is
 * <code>Request.serverProps</code>.  The former wraps an empty
 * <code>Properties</code> object, while the latter wraps
 * <code>Server.props</code>.  Other <code>PropertiesList</code> objects
 * can be added, and removed, from this initial list as required.
 * <p>
 * Given a reference to a <code>PropertiesList</code> object on the
 * linked list (e.g. <code>request.props</code>), one typically "looks
 * up" the value associated with a name using the
 * <code>getProperty</code> method, which delegates to the wrapped
 * <code>Properties.getProperty</code> method.  If the result is
 * <code>null</code>, meaning the name/value pair is not stored in the
 * wrapped <code>Properties</code> object, the request is "forwarded" to
 * the next object on the linked list, and so on until either the
 * name/value pair is found (and the value is returned) or the end of the
 * list is reached (and <code>null</code> is returned).
 * <p>
 * It may be desirable for the name/value lookup to be delayed until
 * after the lookup request has been passed on to subsequent objects on
 * the list.  This can be done by using the two parameter constructor and
 * setting the second, boolean, parameter to <code>true</code>.  Then the
 * <code>getProperty</code> request is forwarded to the next object in
 * the list rather than delegated to the wrapped <code>Properties</code>
 * object.  If the result of the forwarded request is <code>null</code>,
 * the request is then passed to the wrapped <code>Properties</code>
 * object and it's result is returned.
 * 
 * @author	Steve Drach &lt;drach@sun.com&gt;
 * @version	2.8
 *
 * @see java.util.Dictionary
 * @see java.util.Hashtable
 * @see java.util.Properties
 */
public class PropertiesList extends Properties {

    private Dictionary wrapped;
    private boolean searchNextFirst;

    private PropertiesList next, prior;

    /**
     * Constructs a new <code>PropertiesList</code> object that wraps
     * an empty new <code>Properties</code> object.
     */
    public PropertiesList() {
	if (debug) {
	    log("*** PL@" + id(this) + " created " + caller());
	}
	wrapped = new Properties();
    }

    /**
     * Constructs a new <code>PropertiesList</code> object that wraps
     * the input <code>Dictionary</code>.
     *
     * @param dict        The <code>Dictionary</code> object wrapped
     *                    by this <code>PropertiesList</code>.
     */
    public PropertiesList(Dictionary dict) {
	if (debug) {
	    log("*** PL@" + id(this) + " created with dict " + id(dict)
		+ " " + caller());
	}
	wrapped = dict;
    }

    /**
     * Constructs a new <code>PropertiesList</code> object that wraps
     * the input <code>Dictionary</code>.  If the boolean parameter
     * is set <code>true</code>, the wrapped <code>Dictionary</code>
     * is searched after subsequent <code>PropertiesList</code>
     * objects in the linked list are searched, and only if the
     * result of that search was <code>null</code>.
     *
     * @param dict             The <code>Dictionary</code> object wrapped
     *                         by this <code>PropertiesList</code>.
     * 
     * @param searchNextFirst  If <code>true</code> all the following
     *                         objects in the list are searched before
     *                         this one.
     */
    public PropertiesList(Dictionary dict, boolean searchNextFirst) {
	this(dict);
	this.searchNextFirst = searchNextFirst;
    }

    /**
     * Set <code>true</code> to turn on debug output.  It's alot
     * of output and probably of use only to the author.  Note,
     * if <code>server.props</code> contains the name <code>debugProps</code>
     * this variable will be set <code>true</code> by <code>Server</code>.
     */
    public static boolean debug;

    /**
     * Returns the <code>Dictionary</code> object wrapped by this
     * <code>PropertiesList</code>.
     */
    public Dictionary getWrapped() {
	return wrapped;
    }

    /**
     * Adds this <code>PropertiesList</code> object into
     * a linked list following the object referenced by
     * the <code>cursor</code> parameter.  The result is
     * a list that could look like:
     *
     * request.props -> cursor -> this -> serverProps
     * <p>
     * Any transient properties lists's are skipped over before
     * this one is inserted into the list
     *
     * @param cursor      The list object that will precede this object.
     */
    public void addAfter(PropertiesList cursor) {
	while (cursor!=null && cursor.isTransient()) {
	    if (debug) {
		log("*** addAfter skipping transient: " + cursor);
	    }
	    cursor = cursor.next;
	}
	if (cursor != null) {
	    next = cursor.next;
	    if (next != null) {
		next.prior = (PropertiesList)this;
	    }
	    prior = cursor;
	    cursor.next = (PropertiesList)this;
	}
	if (debug) {
	    log("*** addAfter " + cursor.toString());
	    getHead().dump(true, null);
	}
    }

    /**
     * Adds this <code>PropertiesList</code> object into
     * a linked list preceding the object referenced by
     * the <code>cursor</code> parameter.  The result is
     * a list that could look like:
     *
     * request.props -> this -> cursor -> serverProps
     *
     * @param cursor      The list object that will succede this object.
     */
    public void addBefore(PropertiesList cursor) {
	if (cursor != null) {
	    prior = cursor.prior;
	    if (prior != null) {
		prior.next = (PropertiesList)this;
	    }
	    next = cursor;
	    cursor.prior = (PropertiesList)this;
	}
	if (debug) {
	    log("*** addBefore " + cursor.toString());
	    getHead().dump(true, null);
	}
    }

    /**
     * Remove this object from the list in which it's a member.
     *
     * @return            <code>true</code>.
     */
    public boolean remove() {
	PropertiesList head = null;
	if (debug) {
	    if ((head = getHead()) == (PropertiesList)this) {
		head = head.next;
	    }
	}
	if (next != null) {
	    next.prior = prior;
	}
	if (prior != null) {
	    prior.next = next;
	}
	next = prior = null;
	if (debug) {
	    log("*** remove " + toString());
	    head.dump(true, null);
	}
	return true;
    }

    /**
     * Returns the <code>PropertiesList</code> object that succedes this
     * object on the list of which this object is a member.
     *
     * @return            A <code>PropertiesList</code> object or 
     *                    <code>null</code>.
     */
    public PropertiesList getNext() {
	if (debug) {
	    log("*** getNext: PropertyList@" + id(next));
	}
	return next;
    }

    /**
     * Returns the <code>PropertiesList</code> object that precedes this
     * object on the list of which this object is a member.
     *
     * @return            A <code>PropertiesList</code> object or
     *                    <code>null</code>.
     */
    public PropertiesList getPrior() {
	if (debug) {
	    log("*** getPrior: PropertyList@" + id(prior));
	}
	return prior;
    }

    /**
     * Returns the <code>PropertiesList</code> object that is the first object
     * on the list of which this object is a member.  Note that the first
     * object may be this object.
     *
     * @return            A <code>PropertiesList</code> object.
     */
    public PropertiesList getHead() {
	PropertiesList head = (PropertiesList)this;
	while (head.prior != null) {
	    head = head.prior;
	}
	return head;
    }

    /**
     * Find the first <code>PropertiesList</code> object on the list of which
     * this object is a member that wraps the <code>Dictionary</code>
     * parameter.
     *
     * @param d           The <code>Dictionary</code> that is compared with the
     *                    wrapped <code>Dictionary</code>'s for a match.
     *
     * @return            <code>PropertiesList</code> object that wraps the
     *                    input parameter, otherwise <code>null</code>.
     */
    public PropertiesList wraps(Dictionary d) {
	PropertiesList cursor = getHead();
	do {
	    if (cursor.wrapped == d) {
		return cursor;
	    }
	    cursor = cursor.next;
	} while (cursor != null);
	return null;
    }

    /**
     * Starting with this object, print the contents of this and
     * succeeding objects that are on the same list as this object
     * is.
     *
     * @param full        If <code>true</code> also print the contents of the
     *                    wrapped <code>Dictionary</code> object.
     *
     * @param msg         If not <code>null</code>, add this message to the 
     *                    header line.
     */
    public void dump(boolean full, String msg) {
	boolean debug = this.debug;
	this.debug = full;
	if (msg == null) {
	    log("***\ndumping PropertiesList");
	} else {
	    log("***\ndumping PropertiesList " + msg);
	}
	dump2();
	this.debug = debug;
    }

    private void dump2() {
	log("-----\n" + toString());
	if (next != null) {
	    next.dump2();
	}
    }

    /*
     * Dictionary methods
     */

    /**
     * Invokes the same method on the wrapped <code>Dictionary</code> object.
     */
    public synchronized Enumeration elements() {
	return wrapped.elements();
    }

    /**
     * Invokes the same method on the wrapped <code>Dictionary</code> object.
     */
    public synchronized Object get(Object key) {
	return wrapped.get(key);
    }

    /**
     * Invokes the same method on the wrapped <code>Dictionary</code> object.
     */
    public boolean isEmpty() {
	return wrapped.isEmpty();
    }

    /**
     * Invokes the same method on the wrapped <code>Dictionary</code> object.
     */
    public synchronized Enumeration keys() {
	return wrapped.keys();
    }

    /**
     * Invokes the same method on the wrapped <code>Dictionary</code> object.
     */
    public synchronized Object put(Object key, Object value) {
	if (debug) {
	    log("*** PL@" + id(this) + " put(" + key + ", " + value + ")");
	}
	return wrapped.put(key, value);
    }

    /**
     * Invokes the same method on the wrapped <code>Dictionary</code> object.
     */
    public synchronized Object remove(Object key) {
	return wrapped.remove(key);
    }

    /**
     * Invokes the same method on the wrapped <code>Dictionary</code> object.
     */
    public int size() {
	return wrapped.size();
    }

    /*
     * Hashtable methods
     */

    /**
     * Invokes the same method on the wrapped <code>Hashtable</code> object.
     */
    public synchronized void clear() {
	((Hashtable)wrapped).clear();
    }

    /**
     * Invokes the same method on the wrapped <code>Hashtable</code> object.
     */
    public synchronized Object clone() {
	return ((Hashtable)wrapped).clone();
    }

    /**
     * Invokes the same method on the wrapped <code>Hashtable</code> object.
     */
    public synchronized boolean contains(Object value) {
	return ((Hashtable)wrapped).contains(value);
    }

    /**
     * Invokes the same method on the wrapped <code>Hashtable</code> object.
     */
    public synchronized boolean containsKey(Object key) {
	return ((Hashtable)wrapped).containsKey(key);
    }

    /**
     * Invokes the same method on the wrapped <code>Hashtable</code> object.
     */
    public Set entrySet() {
	return ((Hashtable)wrapped).entrySet();
    }

    /**
     * Invokes the same method on the wrapped <code>Hashtable</code> object.
     */
    public boolean equals(Object o) {
	return ((Hashtable)wrapped).equals(o);
    }

    /**
     * Invokes the same method on the wrapped <code>Hashtable</code> object.
     */
    public int hashCode() {
	return ((Hashtable)wrapped).hashCode();
    }    

    /**
     * Invokes the same method on the wrapped <code>Hashtable</code> object.
     */
    public Set keySet() {
	return ((Hashtable)wrapped).keySet();
    }

    /**
     * Invokes the same method on the wrapped <code>Hashtable</code> object.
     */
    public void putAll(Map t) {
	((Hashtable)wrapped).putAll(t);
    }

    /**
     * Invokes the same method on the wrapped <code>Hashtable</code> object.
     */
    public Collection values() {
	return ((Hashtable)wrapped).values();
    }

    // There is no rational way to override rehash(), so it's not here

    /**
     * Returns a <code>String</code> containing the
     * <code>System.identityHashCode</code>s of this object, the wrapped
     * object, and the preceding and succeding objects on the list of
     * which this object is a member.  Additionally, if <code>debug</code>
     * is <code>true</code>, the result of invoking <code>toString</code>
     * on the wrapped <code>Dictionary</code> is appended.
     *
     * @return            <code>String</code> representation of this object.
     */
    public synchronized String toString() {
	StringBuffer sb = new StringBuffer("PropertiesList@").append(id(this));
	sb.append("\n    next: ").append(id(next));
	sb.append("\n    prior: ").append(id(prior));
	sb.append("\n    wrapped: ").append(id(wrapped));
	if (debug) {
	    sb.append("\n    ").append(wrapped.toString());
	}
	return sb.toString();
    }

    /*
     * Properties methods
     */

    /**
     * Looks up <code>key</code> in the wrapped object.  If the result is
     * <code>null</code> 
     * the request is forwarded to the succeeding object in the list
     * of which this object is a member.  If the search order was changed
     * by constructing this object with the two parameter constructor, the
     * request is first forwarded and then, if the result of the
     * forwarded request is <code>null</code>, the <code>key</code> is looked
     * up in the wrapped <code>Properties</code> object.
     *
     * @param key         The key whose value is sought.
     *
     * @return            The value or <code>null</code>.
     */
    public String getProperty(String key) {
	String value = null;
	if (searchNextFirst) {
	    if (next != null) {
		value = next.getProperty(key);
	    }
	    if (value == null) {
		value = getValue(key);
	    }
	} else {
	    value = getValue(key);
	    if (value == null && next != null) {
		value = next.getProperty(key);
	    }
	}
	if (debug) {
	    log("*** PL@" + id(this) + " getProperty(" + key + ") => " + value);
	}
	return value;
    }

    /**
     * Uses <code>getProperty(String)</code> to look up the value associated
     * with the key.  If the result is <code>null</code>, returns the default
     * value.
     *
     * @param key          The key whose value is sought.
     *
     * @param defaultValue The default value.
     *
     * @return             The value or <code>null</code>.
     */
    public String getProperty(String key, String defaultValue) {
	String value = getProperty(key);
	if (value == null) {
	    value = defaultValue;
	}
	if (debug) {
	    log("*** PL@" + id(this) + " get(" + key + ") => " + value);
	}
	return value;
    }

    /**
     * Remove a property from a a chain of properties lists.
     * if "all" is specified, then remove all the keys and values from
     * all property lists in the chain instead of just the first one found.
     * @param key       The key whose value is to be removed
     * @param all	remove all matching keys.
     * @return		true, if at least one key/value pair was removed.
     */

    public boolean removeProperty(String key, boolean all) {
	boolean removed = false;
	if (searchNextFirst) {
	    if (next != null) {
		removed = next.removeProperty(key, all);
	    }
	    if (removed==false || all) {
		removed = (remove(key) != null) || removed;
	    }
	} else {
	    removed = (remove(key) != null);
	    if (removed==false || all) {
		removed = next.removeProperty(key, all) || removed;
	    }
	}
	if (debug) {
	    log("*** PL@" + id(this) + " removeProperty(" + key + ") => " + removed);
	}
	return removed;
    }

    /**
     * Remove the key and its associated value from the first properties
     * object in the chain that contains this key.
     * @return	true, if the key was removed.
     */
    public boolean removeProperty(String key) {
	return removeProperty(key, false);
    }

    /**
     * Invokes the same method on the wrapped <code>Properties</code> object.
     */
    public void list(PrintStream out) {
	if (wrapped instanceof Properties) {
	    ((Properties)wrapped).list(out);
	}
    }

    /**
     * Invokes the same method on the wrapped <code>Properties</code> object.
     */
    public void list(PrintWriter out) {
	if (wrapped instanceof Properties) {
	    ((Properties)wrapped).list(out);
	}
    }

    /**
     * Invokes the same method on the wrapped <code>Properties</code> object.
     */
    public synchronized void load(InputStream in) throws IOException {
	if (wrapped instanceof Properties) {
	    ((Properties)wrapped).load(in);
	}
    }

    /**
     * Invokes the same method on the wrapped <code>Properties</code> object.
     */
    public Enumeration propertyNames() {
	Hashtable h = new Hashtable();
	enumerate(h);
	return h.keys();
    }

    /**
     * Invokes the same method on the wrapped <code>Properties</code> object.
     */
    public synchronized void save(OutputStream out, String header) {
	if (wrapped instanceof Properties) {
	    ((Properties)wrapped).save(out, header);
	}
    }

    /**
     * Invokes the same method on the wrapped <code>Properties</code> object
     * if it exists.  Otherwise invokes <code>put</code> on the wrapped
     * <code>Dictionary</code> object.
     */
    public Object setProperty(String key, String value) {
	if (wrapped instanceof Properties) {
	    return ((Properties)wrapped).setProperty(key, value);
	}
	return wrapped.put(key, value);
    }

    /**
     * Invokes the same method on the wrapped <code>Properties</code> object.
     */
    public void store(OutputStream out, String header) throws IOException {
	if (wrapped instanceof Properties) {
	    ((Properties)wrapped).store(out, header);
	}
    }

    /*
     * Additional method on wrapped object
     */
    
    /**
     * Returns an <code>Enumeration</code> of property names that
     * match a <code>glob</code> pattern.
     *
     * @param pattern     The <code>glob</code> pattern to match.
     *
     * @return            An <code>Enumeration</code> containing
     *                    matching property names, if any.
     */
    public Enumeration propertyNames(String pattern) {
	Hashtable h = new Hashtable();
	enumerate(h, pattern);
	return h.keys();
    }

    /*
     * Helper methods
     */

    private String getValue(String key) {
	if (wrapped instanceof Properties) {
	    return ((Properties)wrapped).getProperty(key);
	} else {
	    return (String)wrapped.get(key);
	}
    }

    private synchronized void enumerate(Hashtable h) {
	enumerate(h, null);
    }
	
    private synchronized void enumerate(Hashtable h, String pattern) {
	if (next != null && ! searchNextFirst) {
	    next.enumerate(h, pattern);
	}
	Enumeration e;
	if (wrapped instanceof Properties) {
	    e = ((Properties)wrapped).propertyNames();
	} else {
	    e = wrapped.keys();
	}
	while (e.hasMoreElements()) {
	    String s = null;
	    try {
		s = (String)e.nextElement();
		if (pattern == null || Glob.match(pattern, s)) {
		    h.put(s, s);
		}
	    } catch (ClassCastException x) {}
	}
	if (next != null && searchNextFirst) {
	    next.enumerate(h, pattern);
	}
    }

    private String id(Object o) {
	if (o == null) {
	    return "null";
	}
	return Integer.toHexString(System.identityHashCode(o));
    }

    private String caller() {
	ByteArrayOutputStream baos = new ByteArrayOutputStream();
	PrintWriter out = new PrintWriter(baos);
	(new Throwable()).printStackTrace(out);
	try {
	    out.close();
	    BufferedReader in = new BufferedReader(
		                new InputStreamReader(
			        new ByteArrayInputStream(baos.toByteArray())));
	    String line = null;
	    boolean found = false;
	    while ((line = in.readLine()) != null) {
		if (line.indexOf("PropertiesList.<init>") != -1) {
		    found = true;
		    continue;
		}
		if (found) {
		    return line.trim();
		}
	    }
	} catch (IOException e) {}
	return "";
    }

    // Can't use server.log since we don't have access to either a
    // Server object or a Request object
    private void log(Object msg) {
	if (msg == null) {
	    return;
	}
	if (!(msg instanceof String)) {
	    msg = msg.toString();
	}
	System.out.println(msg);
    }

    /**
     * Sub-classes of PropertiesList can override this to mark
     * themselves "transient", in which case <code>addAfter</code>
     * will skip this list.
     */

    public boolean isTransient() {
        return false;
    }
}
