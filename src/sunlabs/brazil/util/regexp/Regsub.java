/*
 * Regsub.java
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
 * Version:  2.3
 * Created by cstevens on 99/08/10
 * Last modified by suhler on 04/12/30 12:43:30
 *
 * Version Histories:
 *
 * 2.3 04/12/30-12:43:30 (suhler)
 *   check for bogus call to submatch(), add getRegexp method
 *
 * 2.2 04/11/30-15:19:46 (suhler)
 *   fixed sccs version string
 *
 * 2.1 02/10/01-16:37:03 (suhler)
 *   version change
 *
 * 1.4 99/10/14-13:11:16 (cstevens)
 *   @author & @version
 *
 * 1.3 99/08/27-16:51:02 (cstevens)
 *   Regsub.submatch would crash if specified submatch didn't exist.
 *
 * 1.2 99/08/27-16:19:00 (cstevens)
 *   regsub entered infinite loop if pattern matched the empty string.
 *
 * 1.2 99/08/10-16:14:33 (Codemgr)
 *   SunPro Code Manager data about conflicts, renames, etc...
 *   Name history : 1 0 util/regexp/Regsub.java
 *
 * 1.1 99/08/10-16:14:32 (cstevens)
 *   date and time created 99/08/10 16:14:32 by cstevens
 *
 */

package sunlabs.brazil.util.regexp;

/**
 * The <code>Regsub</code> class provides an iterator-like object to
 * extract the matched and unmatched portions of a string with respect to
 * a given regular expression.
 * <p>
 * After each match is found, the portions of the string already
 * checked are not searched again -- searching for the next match will
 * begin at the character just after where the last match ended.
 * <p>
 * Here is an example of using Regsub to replace all "%XX" sequences in
 * a string with the ASCII character represented by the hex digits "XX":
 * <pre>
 * public static void
 * main(String[] args)
 *     throws Exception
 * {
 *     Regexp re = new Regexp("%[a-fA-F0-9][a-fA-F0-9]");
 *     Regsub rs = new Regsub(re, args[0]);
 *
 *     StringBuffer sb = new StringBuffer();
 *
 *     while (rs.nextMatch()) {
 *         sb.append(rs.skipped());
 *
 *         String match = rs.matched();
 *
 *         int hi = Character.digit(match.charAt(1), 16);
 *         int lo = Character.digit(match.charAt(2), 16);
 *         sb.append((char) ((hi &lt;&lt; 4) | lo));
 *     }
 *     sb.append(rs.rest());
 *
 *     System.out.println(sb);
 * }
 * </pre>
 *
 * @author	Colin Stevens (colin.stevens@sun.com)
 * @version		2.3
 * @see		Regexp
 */
public class Regsub
{
    Regexp r;
    String str;
    int ustart;
    int mstart;
    int end;
    Regexp.Match m;   

    /**
     * Construct a new <code>Regsub</code> that can be used to step 
     * through the given string, finding each substring that matches
     * the given regular expression.
     * <p>
     * <code>Regexp</code> contains two substitution methods,
     * <code>sub</code> and <code>subAll</code>, that can be used instead
     * of <code>Regsub</code> if just simple substitutions are being done.
     *
     * @param	r
     *		The compiled regular expression.
     *
     * @param	str
     *		The string to search.
     *
     * @see	Regexp#sub
     * @see	Regexp#subAll
     */
    public
    Regsub(Regexp r, String str)
    {
	this.r = r;
	this.str = str;
	this.ustart = 0;
	this.mstart = -1;
	this.end = 0;
    }

    /**
     * Searches for the next substring that matches the regular expression.
     * After calling this method, the caller would call methods like
     * <code>skipped</code>, <code>matched</code>, etc. to query attributes
     * of the matched region.
     * <p>
     * Calling this function again will search for the next match, beginning
     * at the character just after where the last match ended.
     *
     * @return	<code>true</code> if a match was found, <code>false</code>
     *		if there are no more matches.
     */
    public boolean
    nextMatch()
    {
	ustart = end;

	/*
	 * Consume one character if the last match didn't consume any
	 * characters, to avoid an infinite loop.
	 */

	int off = ustart;
	if (off == mstart) {
	    off++;
	    if (off >= str.length()) {
	        return false;
	    }
	}


	m = r.exec(str, 0, off);
	if (m == null) {
	    return false;
	}

	mstart = m.indices[0];
	end = m.indices[1];

	return true;
    }

    /**
     * Returns a substring consisting of all the characters skipped
     * between the end of the last match (or the start of the original
     * search string) and the start of this match.
     * <p>
     * This method can be used extract all the portions of string that
     * <b>didn't</b> match the regular expression.
     *
     * @return	The characters that didn't match.
     */
    public String
    skipped()
    {
	return str.substring(ustart, mstart);
    }

    /**
     * Returns a substring consisting of the characters that matched
     * the entire regular expression during the last call to
     * <code>nextMatch</code>.  
     *
     * @return	The characters that did match.
     *
     * @see	#submatch
     */
    public String
    matched()
    {
	return str.substring(mstart, end);
    }

    /**
     * Returns a substring consisting of the characters that matched
     * the given parenthesized subexpression during the last call to
     * <code>nextMatch</code>.
     *
     * @param	i
     *		The index of the parenthesized subexpression.
     *
     * @return	The characters that matched the subexpression, or
     *		<code>null</code> if the given subexpression did not
     *		exist or did not match.
     */
    public String
    submatch(int i)
    {
	if (m==null || (i * 2 + 1 >= m.indices.length)) {
	    return null;
	}
	int start = m.indices[i * 2];
	int end = m.indices[i * 2 + 1];
	if ((start < 0) || (end < 0)) {
	    return null;
	}
	return str.substring(start, end);
    }

    /**
     * Returns a substring consisting of all the characters that come
     * after the last match.  As the matches progress, the <code>rest</code>
     * gets shorter.  When <code>nextMatch</code> returns <code>false</code>,
     * then this method will return the rest of the string that can't be
     * matched.
     *
     * @return	The rest of the characters after the last match.
     */
    public String
    rest()
    {
	return str.substring(end);
    }

    /**
     * Return the regexp used by this regsub.
     */
    public Regexp getRegexp()
    {
	return r;
    }
}
