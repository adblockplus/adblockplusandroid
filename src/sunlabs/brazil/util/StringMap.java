/*
 * StringMap.java
 *
 * Brazil project web application toolkit,
 * export version: 2.3 
 * Copyright (c) 1999-2007 Sun Microsystems, Inc.
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
 * Version:  2.5
 * Created by cstevens on 99/09/15
 * Last modified by suhler on 07/01/14 15:12:01
 *
 * Version Histories:
 *
 * 2.5 07/01/14-15:12:01 (suhler)
 *   add get(key, dflt)
 *
 * 2.4 05/05/26-08:59:24 (suhler)
 *   add append() to combite 2 string maps
 *
 * 2.3 04/11/30-15:19:45 (suhler)
 *   fixed sccs version string
 *
 * 2.2 04/04/28-15:55:32 (suhler)
 *   doc format fixes
 *
 * 2.1 02/10/01-16:36:58 (suhler)
 *   version change
 *
 * 1.11 00/03/29-16:46:29 (cstevens)
 *
 * 1.10 99/11/16-14:47:27 (cstevens)
 *   Rename "getValue" to "get" to make it more compatible with Dictionary
 *   naming scheme.
 *
 * 1.9 99/10/19-19:00:19 (cstevens)
 *   toString crashed if nothing in stringMap.
 *
 * 1.8 99/10/14-12:57:39 (cstevens)
 *   Documentation for StringMap.toString.
 *
 * 1.7 99/10/07-13:02:35 (cstevens)
 *   javadoc lint.
 *
 * 1.6 99/10/04-17:16:38 (cstevens)
 *   tostring
 *
 * 1.5 99/10/04-16:13:39 (cstevens)
 *   Merged changes between child workspace "/home/cstevens/ws/brazil/naws" and
 *   parent workspace "/export/ws/brazil/naws".
 *
 * 1.3.1.1 99/10/04-16:03:18 (cstevens)
 *   Documentation for LexML and StringMap.
 *
 * 1.4 99/10/01-11:21:00 (suhler)
 *   added toString()
 *
 * 1.3 99/09/15-15:56:38 (cstevens)
 *   bug
 *
 * 1.2 99/09/15-14:51:43 (cstevens)
 *   lines ended with CRLF.
 *
 * 1.2 99/09/15-14:31:03 (Codemgr)
 *   SunPro Code Manager data about conflicts, renames, etc...
 *   Name history : 1 0 util/StringMap.java
 *
 * 1.1 99/09/15-14:31:02 (cstevens)
 *   date and time created 99/09/15 14:31:02 by cstevens
 *
 */

package sunlabs.brazil.util;

import java.util.Dictionary;    
import java.util.Enumeration;
import java.util.Vector;

/**
 * The <code>StringMap</code> class is a substitute for the Hashtable.
 * The StringMap has the following properties: <ul>
 * <li> Maps case-insensitive string keys to string values.
 * <li> The case of the keys is preserved.
 * <li> Values may be <code>null</code>.
 * <li> Preserves the relative order of the data.  
 * <li> The same key may appear multiple times in a single map.
 * <li> This map is implemented via a Vector, and as such, as the number of
 *      keys increases, the time required to search will go up.
 * </ul>
 *
 * @author	Colin Stevens (colin.stevens@sun.com)
 * @version		2.5
 */
public class StringMap
    extends Dictionary
{
    Vector keys;
    Vector values;

    /**
     * Creates an empty StringMap.
     */
    public
    StringMap()
    {
	keys = new Vector();
	values = new Vector();
    }

    /**
     * Returns the number of elements in this StringMap.  Every occurrence of
     * keys that appear multiple times is counted.
     *
     * @return	The number of elements in this StringMap.
     *
     * @see	#keys
     *
     * @implements	Dictionary#size
     */
    public int
    size()
    {
	return keys.size();
    }

    /**
     * Tests if there are any elements in this StringMap.
     *
     * @return	Returns <code>true</code> if there are no elements,
     *		<code>false</code> otherwise.
     *
     * @implements	Dictionary#isEmpty
     */
    public boolean
    isEmpty()
    {
	return keys.isEmpty();
    }

    /**
     * Returns an enumeration of the keys in this StringMap.  The elements
     * of the enumeration are strings.
     * <p>
     * The same key may appear multiple times in the enumeration, not
     * necessarily consecutively.  Since <code>get</code> always returns
     * the value associated with the first occurrence of a given key, a
     * StringMap cannot be enumerated in the same fashion as a Hashtable.
     * Instead, the caller should use:
     * <pre>
     * Enumeration keys = map.keys();
     * Enumeration values = map.elements();
     * while (keys.hasMoreElements()) {
     *     String key = (String) keys.nextElement();
     *     String value = (String) values.nextElement();
     * }
     * </pre>
     * or:
     * <pre>
     * for (int i = 0; i &lt; map.size(); i++) {
     *     String key = map.getKey(i);
     *     String value = map.get(i);
     * }
     * </pre>
     *
     * @return	An enumeration of the keys.
     * 
     * @see	#elements
     * @see	#size
     * @see	#getKey
     * @see	#get
     *
     * @implements	Dictionary#keys
     */
    public Enumeration
    keys()
    {
	return keys.elements();
    }

    /**
     * Returns an enumeration of the values in this StringMap.  The elements
     * of the enumeration are strings.
     * 
     * @return	An enumeration of the values.
     *
     * @see	#keys
     *
     * @implements	Dictionary#elements
     */
    public Enumeration
    elements()
    {
	return values.elements();
    }

    /**
     * Returns the key at the specified index.  The index ranges from
     * <code>0</code> to <code>size() - 1</code>.
     * <p>
     * This method can be used to iterate over all the keys in this
     * StringMap in the order in which they were inserted, subject to any
     * intervening deletions.
     *
     * @param	index
     *		The index of the key.
     *
     * @return	The key at the specified index.
     *
     * @throws	IndexOutOfBoundsException
     *		if the index is out of the allowed range.
     */
    public String
    getKey(int index)
	throws IndexOutOfBoundsException
    {
	return (String) keys.elementAt(index);
    }

    /**
     * Returns the value at the specified index.  The index ranges from
     * <code>0</code> to <code>size() - 1</code>.
     * <p>
     * This method can be used to iterate over all the values in this
     * StringMap in the order in which they were inserted, subject to any
     * intervening deletions.
     *
     * @param	index
     *		The index of the key.
     *
     * @return	The value at the specified index.
     *
     * @throws	IndexOutOfBoundsException
     *		if the index is out of the allowed range.
     */
    public String
    get(int index)
	throws IndexOutOfBoundsException
    {
	return (String) values.elementAt(index);
    }

    /**
     * Returns the value that the specified case-insensitive key maps to
     * in this StringMap.
     * <p>
     * The same key may appear multiple times in the enumeration; this
     * method always returns the value associated with the first
     * occurrence of the specified key.  In order to get all the values,
     * it is necessary to iterate over the entire StringMap to retrieve
     * all the values associated with a given key.
     *
     * @param	key
     *		A key in this StringMap.  May not be <code>null</code>.
     *
     * @return	The value to which the specified key is mapped, or
     *		<code>null</code> if the key is not in the StringMap.
     *
     * @see	#keys
     */
    public String
    get(String key) {
	return get(key, null);
    }

    /**
     * Returns the value that the specified case-insensitive key maps to
     * in this StringMap.
     * @param	key
     *		A key in this StringMap.  May not be <code>null</code>.
     * @param	dflt
     *		A default value if the entry for <code>key</code> is not found.
     *
     * @return	The value to which the specified key is mapped, or
     *		<code>dflt</code> if the key is not in the StringMap.
     */

    public String
    get(String key, String dflt) {
	int i = indexOf(key);
	if (i >= 0) {
	    return (String) values.elementAt(i);
	} else {
	    return dflt;
	}
    }

    /**
     * Performs the same job as <code>get(String)</code>.  It exists so
     * this class can extend the <code>Dictionary</code> class.
     * 
     * @param	key
     *		Must be a String.
     *
     * @return	A String value.
     *
     * @throws	ClassCastException
     *		if the <code>key</code> is not a String.
     *
     * @see	#get(String)
     *
     * @implements	Dictionary#get
     */
    public Object
    get(Object key)
    {
	return get((String) key);
    }

    /**
     * Maps the key at the given index to the specified value in this
     * StringMap.  The index ranges from <code>0</code> to
     * <code>size() - 1</code>.
     *
     * @param	index
     *		The index of the key.
     *
     * @return	The value at the specified index.
     *
     * @throws	IndexOutOfBoundsException
     *		if the index is out of the allowed range.
     */
    public void
    put(int index, String value)
    {
	values.setElementAt(value, index);
    }

    /**
     * Maps the given case-insensitive key to the specified value in this
     * StringMap.
     * <p>
     * The value can be retrieved by calling <code>get</code> with a
     * key that is case-insensitive equal to the given key.
     * <p>
     * If this StringMap already contained a mapping for the given key,
     * the old value is forgotten and the new specified value is used.
     * The case of the prior key is retained in that case.  Otherwise
     * the case of the new key is used.
     *
     * @param	key
     *		The new key.  May not be <code>null</code>.
     *
     * @param	value
     *		The new value.  May be <code>null</code>.
     *
     * @return	The previous value to which <code>key</code> was mapped,
     *		or <code>null</code> if the the key did not map to any
     *		value.
     */
    public void
    put(String key, String value)
    {
	int i = indexOf(key);
	if (i < 0) {
	    keys.addElement(key);
	    values.addElement(value);
	} else {
	    values.setElementAt(value, i);
	}
    }

    /**
     * Performs the same job as <code>put(String, String)</code>.  It exists
     * so this class can extend the <code>Dictionary</code> class.
     *
     * @param	key
     *		Must be a String.
     *
     * @param	value
     *		Must be a String.
     *
     * @return	The previous value to which <code>key</code> was mapped,
     *		or <code>null</code> if the the key did not map to any
     *		value.
     *
     * @throws	ClassCastException
     *		if the <code>key</code> or <code>value</code> is not a
     *		String.
     *
     * @see	#put(String, String)
     *
     * @implements	Dictionary#put
     */
    public Object
    put(Object key, Object value)
    {
	String skey = (String) key;
	String svalue = (String) value;

	Object prior;

	int i = indexOf(skey);
	if (i < 0) {
	    prior = null;
	    keys.addElement(skey);
	    values.addElement(svalue);
	} else {
	    prior = values.elementAt(i);
	    values.setElementAt(svalue, i);
	}
	return prior;
    }

    /**
     * Maps the given case-insensitive key to the specified value in this
     * StringMap.
     * <p>
     * The new mapping is added to this StringMap even if the given key
     * already has a mapping.  In this way it is possible to create a key
     * that maps to two or more values.
     * <p>
     * Since the same key may appear multiple times in this StringMap, it
     * is necessary to iterate over the entire StringMap to retrieve all
     * values associated with a given key.
     *
     * @param	key
     *		The new key.  May not be <code>null</code>.
     *
     * @param	value
     *		The new value.  May be <code>null</code>.
     *
     * @see	#put(String, String)
     * @see	#keys
     */
    public void
    add(String key, String value)
    {
	keys.addElement(key);
	values.addElement(value);
    }

    /**
     * Removes the given case-insensitive key and its corresponding value
     * from this StringMap.  This method does nothing if the key is not in
     * this StringMap.
     * <p>
     * The same key may appear in multiple times in this StringMap; this
     * method only removes the first occurrence of the key.
     *
     * @param	key
     *		The key that needs to be removed.  Must not be
     *		<code>null</code>.
     */
    public void
    remove(String key)
    {
	int i = indexOf(key);
	if (i >= 0) {
	    remove(i);
	}
    }

    public void
    remove(int i)
    {
	keys.removeElementAt(i);
	values.removeElementAt(i);
    }

    /**
     * Performs the same job as <code>remove(String)</code>.  It exists so
     * this class can extend the <code>Dictionary</code> class.
     *
     * @param	key
     *		Must be a String.
     *
     * @return	The string value to which the key had been mapped, or
     *		<code>null</code> if the key did not have a mapping.
     *
     * @throws	ClassCastException
     *		if the <code>key</code> is not a String.
     *
     * @implements	Dictionary#remove
     */
    public Object
    remove(Object key)
    {
	int i = indexOf((String) key);
	if (i >= 0) {
	    Object prior = values.elementAt(i);
	    remove(i);
	    return prior;
	}
	return null;
    }

    /**
     * Removes all the keys and values from this StringMap.
     */
    public void
    clear()
    {
	keys.setSize(0);
	values.setSize(0);
    }

    private int
    indexOf(String key)
    {
	int length = keys.size();
	for (int i = 0; i < length; i++) {
	    String got = (String) keys.elementAt(i);
	    if (key.equalsIgnoreCase(got)) {
		return i;
	    }
	}
	return -1;
    }

    /**
     * Append another Stringmap onto this one.
     * @param other	the map to append to this one
     * @param noReplace	should existing values be replaced?
     */

    public void
    append(StringMap other, boolean noReplace) {
	int size = (other != null) ? other.size() : 0;
	for (int i = 0; i < size; i++) {
	    if (noReplace) {
                add(other.getKey(i), other.get(i));
	    } else {
                put(other.getKey(i), other.get(i));
            }
	}
    }

    /**
     * Returns a string representation of this <code>StringMap</code> in the
     * form of a set of entries, enclosed in braces and separated by the
     * characters ", ".  Each entry is rendered as the key, an equals sign
     * "=", and the associated value.
     *
     * @return	The string representation of this <code>StringMap</code>.
     */
    public String
    toString()
    {
	StringBuffer sb = new StringBuffer();

	sb.append('{');

	int length = keys.size();
	for (int i = 0; i < length; i++) {
	    sb.append(getKey(i));
	    sb.append('=');
	    sb.append(get(i));
	    sb.append(", ");
	}
	if (sb.length() > 1) {
	    sb.setLength(sb.length() - 2);
	}
	sb.append('}');

	return sb.toString();
    }
}
