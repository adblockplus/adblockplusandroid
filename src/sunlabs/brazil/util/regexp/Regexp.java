/*
 * Regexp.java
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
 * Last modified by suhler on 04/12/30 12:42:56
 *
 * Version Histories:
 *
 * 2.3 04/12/30-12:42:56 (suhler)
 *   add toString()
 *
 * 2.2 04/11/30-15:19:46 (suhler)
 *   fixed sccs version string
 *
 * 2.1 02/10/01-16:37:03 (suhler)
 *   version change
 *
 * 1.10 00/11/06-10:45:53 (suhler)
 *   make serializable
 *
 * 1.9 00/05/31-13:52:58 (suhler)
 *   docs
 *
 * 1.8 99/11/17-10:20:17 (suhler)
 *   fixed wildcarded imports
 *
 * 1.7 99/10/14-13:04:11 (cstevens)
 *   Documentation for Regexp.Filter
 *
 * 1.6 99/10/07-13:19:37 (cstevens)
 *   javadoc lint.
 *
 * 1.5 99/09/03-11:34:10 (cstevens)
 *   Change Regexp.sub(String, String) so that if there were 0 matches it returns
 *   null.
 *   Regexp.match with a pattern that had '$' at the end was broken.
 *
 * 1.4 99/08/27-16:18:05 (cstevens)
 *   "\\", "\&", "\" followed by anything not a digit should be a literal char in
 *   subspec, not an error.
 *
 * 1.3 99/08/27-13:12:23 (cstevens)
 *   Consolidate RegexpFilter into Regexp, making Regexp.Filter a public inner
 *   interface.
 *
 * 1.2 99/08/27-12:32:52 (cstevens)
 *   Passes tcl test suite.
 *   Added support for case-insensitive match
 *   Put RegexpFilter back in.
 *   Fixed exceptions revealed by test suite.
 *
 * 1.1.1.1 99/08/18-08:41:04 (suhler)
 *   lint
 *
 * 1.2 99/08/10-16:14:33 (Codemgr)
 *   SunPro Code Manager data about conflicts, renames, etc...
 *   Name history : 1 0 util/regexp/Regexp.java
 *
 * 1.1 99/08/10-16:14:32 (cstevens)
 *   date and time created 99/08/10 16:14:32 by cstevens
 *
 */

package sunlabs.brazil.util.regexp;

/**
 * The <code>Regexp</code> class can be used to match a pattern against a
 * string and optionally replace the matched parts with new strings.
 * <p>
 * Regular expressions were implemented by translating Henry Spencer's
 * regular expression package for <a href="http://www.scriptics.com">tcl8.0</a>.
 * Much of the description below is copied verbatim from the tcl8.0 regsub
 * manual entry.
 * <hr>
 * REGULAR EXPRESSIONS
 * <p>
 * A regular expression is zero or more <code>branches</code>, separated by
 * "|".  It matches anything that matches one of the branches.
 * <p>
 * A branch is zero or more <code>pieces</code>, concatenated.
 * It matches a match for the first piece, followed by a match for the
 * second piece, etc.
 * <p>
 * A piece is an <code>atom</code>, possibly followed by "*", "+", or
 * "?". <ul>
 * <li> An atom followed by "*" matches a sequence of 0 or more matches of
 * the atom.
 * <li> An atom followed by "+" matches a sequence of 1 or more matches of
 * the atom.
 * <li> An atom followed by "?" matches either 0 or 1 matches of the atom.
 * </ul>
 * <p>
 * An atom is <ul>
 * <li> a regular expression in parentheses (matching a match for the
 * regular expression)
 * <li> a <code>range</code> (see below)
 * <li> "." (matching any single character)
 * <li> "^" (matching the null string at the beginning of the input string)
 * <li> "$" (matching the null string at the end of the input string)
 * <li> a "\" followed by a single character (matching that character)
 * <li> a single character with no other significance (matching that
 * character).
 * </ul>
 * <p>
 * A <code>range</code> is a sequence of characters enclosed in "[]".
 * The range normally matches any single character from the sequence.
 * If the sequence begins with "^", the range matches any single character
 * <b>not</b> from the rest of the sequence.
 * If two characters in the sequence are separated by "-", this is shorthand
 * for the full list of characters between them (e.g. "[0-9]" matches any
 * decimal digit).  To include a literal "]" in the sequence, make it the
 * first character (following a possible "^").  To include a literal "-",
 * make it the first or last character.
 * <p>
 * In general there may be more than one way to match a regular expression
 * to an input string.  For example, consider the command
 * <pre>
 * String[] match = new String[2];
 * Regexp.match("(a*)b*", "aabaaabb", match);
 * </pre>
 * Considering only the rules given so far, <code>match[0]</code> and
 * <code>match[1]</code> could end up with the values <ul>
 * <li> "aabb" and "aa"
 * <li> "aaab" and "aaa"
 * <li> "ab" and "a"
 * </ul>
 * or any of several other combinations.  To resolve this potential ambiguity,
 * Regexp chooses among alternatives using the rule "first then longest".
 * In other words, it considers the possible matches in order working
 * from left to right across the input string and the pattern, and it
 * attempts to match longer pieces of the input string before shorter
 * ones.  More specifically, the following rules apply in decreasing
 * order of priority: <ol>
 * <li> If a regular expression could match two different parts of an input
 * string then it will match the one that begins earliest.
 * <li> If a regular expression contains "|" operators then the
 * leftmost matching sub-expression is chosen.
 * <li> In "*", "+", and "?" constructs, longer matches are chosen in
 * preference to shorter ones.
 * <li>
 * In sequences of expression components the components are considered
 * from left to right.
 * </ol>
 * <p>
 * In the example from above, "(a*)b*" therefore matches exactly "aab"; the
 * "(a*)" portion of the pattern is matched first and it consumes the leading
 * "aa", then the "b*" portion of the pattern consumes the next "b".  Or,
 * consider the following example:
 * <pre>
 * String match = new String[3];
 * Regexp.match("(ab|a)(b*)c", "abc", match);
 * </pre>
 * After this command, <code>match[0]</code> will be "abc",
 * <code>match[1]</code> will be "ab", and <code>match[2]</code> will be an
 * empty string.
 * Rule 4 specifies that the "(ab|a)" component gets first shot at the input
 * string and Rule 2 specifies that the "ab" sub-expression
 * is checked before the "a" sub-expression.
 * Thus the "b" has already been claimed before the "(b*)"
 * component is checked and therefore "(b*)" must match an empty string.
 * <hr>
 * <a name=regsub></a>
 * REGULAR EXPRESSION SUBSTITUTION
 * <p>
 * Regular expression substitution matches a string against a regular
 * expression, transforming the string by replacing the matched region(s)
 * with new substring(s).
 * <p>
 * What gets substituted into the result is controlled by a
 * <code>subspec</code>.  The subspec is a formatting string that specifies
 * what portions of the matched region should be substituted into the
 * result.
 * <ul>
 * <li> "&amp;" or "\0" is replaced with a copy of the entire matched region.
 * <li> "\<code>n</code>", where <code>n</code> is a digit from 1 to 9,
 * is replaced with a copy of the <code>n</code><i>th</i> subexpression.
 * <li> "\&amp;" or "\\" are replaced with just "&amp;" or "\" to escape their
 * special meaning.
 * <li> any other character is passed through.
 * </ul>
 * In the above, strings like "\2" represents the two characters
 * <code>backslash</code> and "2", not the Unicode character 0002.
 * <hr>
 * Here is an example of how to use Regexp
 * <pre>
 *
 *    public static void
 *    main(String[] args)
 *	throws Exception
 *    {
 *	Regexp re;
 *	String[] matches;
 *	String s;
 *
 *	&#47;*
 *	 * A regular expression to match the first line of a HTTP request.
 *	 *
 *	 * 1. ^               - starting at the beginning of the line
 *	 * 2. ([A-Z]+)        - match and remember some upper case characters
 *	 * 3. [ \t]+          - skip blank space
 *	 * 4. ([^ \t]*)       - match and remember up to the next blank space
 *	 * 5. [ \t]+          - skip more blank space
 *	 * 6. (HTTP/1\\.[01]) - match and remember HTTP/1.0 or HTTP/1.1
 *	 * 7. $		      - end of string - no chars left.
 *	 *&#47;
 *
 *	s = "GET http://a.b.com:1234/index.html HTTP/1.1";
 *
 *	re = new Regexp("^([A-Z]+)[ \t]+([^ \t]+)[ \t]+(HTTP/1\\.[01])$");
 *	matches = new String[4];
 *	if (re.match(s, matches)) {
 *	    System.out.println("METHOD  " + matches[1]);
 *	    System.out.println("URL     " + matches[2]);
 *	    System.out.println("VERSION " + matches[3]);
 *	}
 *
 *	&#47;*
 *	 * A regular expression to extract some simple comma-separated data,
 *	 * reorder some of the columns, and discard column 2.
 *	 *&#47;
 *
 *	s = "abc,def,ghi,klm,nop,pqr";
 *
 *	re = new Regexp("^([^,]+),([^,]+),([^,]+),(.*)");
 *	System.out.println(re.sub(s, "\\3,\\1,\\4"));
 *    }
 * </pre>
 *
 * @author	Colin Stevens (colin.stevens@sun.com)
 * @version		2.3
 * @see		Regsub
 */

public class Regexp
implements java.io.Serializable 
{
    public static void
    main(String[] args)
	throws Exception
    {
	if ((args.length == 2) && (args[0].equals("compile"))) {
	    System.out.println(new Regexp(args[1]));
	} else if ((args.length == 3) && (args[0].equals("match"))) {
	    Regexp r = new Regexp(args[1]);
	    String[] substrs = new String[r.subspecs()];
	    boolean match = r.match(args[2], substrs);
	    System.out.println("match:\t" + match);
	    for (int i = 0; i < substrs.length; i++) {
		System.out.println((i + 1) + ":\t" + substrs[i]);
	    }
	} else if ((args.length == 4) && (args[0].equals("sub"))) {
	    Regexp r = new Regexp(args[1]);
	    System.out.println(r.subAll(args[2], args[3]));
	} else {
	    System.out.println("usage:");
	    System.out.println("\tRegexp match <pattern> <string>");
	    System.out.println("\tRegexp sub <pattern> <string> <subspec>");
	    System.out.println("\tRegexp compile <pattern>");
	}
    }

    /*
     * Structure for regexp "program".  This is essentially a linear encoding
     * of a nondeterministic finite-state machine (aka syntax charts or
     * "railroad normal form" in parsing technology).  Each node is an opcode
     * plus a "next" pointer, possibly plus an operand.  "Next" pointers of
     * all nodes except BRANCH implement concatenation; a "next" pointer with
     * a BRANCH on both ends of it is connecting two alternatives.  (Here we
     * have one of the subtle syntax dependencies:  an individual BRANCH (as
     * opposed to a collection of them) is never concatenated with anything
     * because of operator precedence.)  The operand of some types of node is
     * a literal string; for others, it is a node leading into a sub-FSM.  In
     * particular, the operand of a BRANCH node is the first node of the branch.
     * (NB this is *not* a tree structure:  the tail of the branch connects
     * to the thing following the set of BRANCHes.)  The opcodes are:
     */

    static final int NSUBEXP = 100;

		/* definition	number	opnd?	meaning */

    static final char END 	= 0;	/* no	End of program. */
    static final char BOL	= 1;	/* no	Match "" at beginning of line. */
    static final char EOL	= 2;	/* no	Match "" at end of line. */
    static final char ANY	= 3;	/* no	Match any one character. */
    static final char ANYOF	= 4;	/* str	Match any character in this string. */
    static final char ANYBUT	= 5;	/* str	Match any character not in this string. */
    static final char BRANCH	= 6;	/* node	Match this alternative, or the next... */
    static final char BACK	= 7;	/* no	Match "", "next" ptr points backward. */
    static final char EXACTLY	= 8;	/* str	Match this string. */
    static final char NOTHING	= 9;	/* no	Match empty string. */
    static final char STAR	= 10;	/* node	Match this (simple) thing 0 or more times. */
    static final char PLUS	= 11;	/* node	Match this (simple) thing 1 or more times. */
    static final char OPEN	= 20;	/* no	Mark this point in input as start of #n. */
					/*	OPEN+1 is number 1, etc. */
    static final char CLOSE	= (char) (OPEN+NSUBEXP);
					/* no	Analogous to OPEN. */
    static final String[] opnames = {
	"END",	   "BOL",     "EOL",     "ANY",     "ANYOF",  "ANYBUT",
	"BRANCH",  "BACK",    "EXACTLY", "NOTHING", "STAR",   "PLUS"
    };

    /*
     * A node is one char of opcode followed by one char of "next" pointer.
     * The value is a positive offset from the opcode of the node containing
     * it.  An operand, if any, simply follows the node.  (Note that much of
     * the code generation knows about this implicit relationship.)
     *
     * Opcode notes:
     *
     * BRANCH	The set of branches constituting a single choice are hooked
     *		together with their "next" pointers, since precedence prevents
     *		anything being concatenated to any individual branch.  The
     *		"next" pointer of the last BRANCH in a choice points to the
     *		thing following the whole choice.  This is also where the
     *		final "next" pointer of each individual branch points; each
     *		branch starts with the operand node of a BRANCH node.
     *
     * ANYOF, ANYBUT, EXACTLY
     *		The format of a string operand is one char of length
     *		followed by the characters making up the string.
     *
     * BACK	Normal "next" pointers all implicitly point forward; BACK
     *		exists to make loop structures possible.
     *
     * STAR, PLUS
     * 		'?', and complex '*' and '+' are implemented as circular
     *		BRANCH structures using BACK.  Simple cases (one character
     *		per match) are implemented with STAR and PLUS for speed
     *		and to minimize recursive plunges.
     *
     * OPENn, CLOSEn
     *		are numbered at compile time.
     */


    /**
     * The bytecodes making up the regexp program.
     */
    char[] program;

    /**
     * Whether the regexp matching should be case insensitive.
     */
    boolean ignoreCase;

    /**
     * The number of parenthesized subexpressions in the regexp pattern,
     * plus 1 for the match of the whole pattern itself.
     */
    int npar;

    /**
     * <code>true</code> if the pattern must match the beginning of the
     * string, so we don't have to waste time matching against all possible
     * starting locations in the string.
     */
    boolean anchored;
    
    int startChar;
    String must;

    /**
     * Compiles a new Regexp object from the given regular expression
     * pattern.
     * <p>
     * It takes a certain amount of time to parse and validate a regular
     * expression pattern before it can be used to perform matches
     * or substitutions.  If the caller caches the new Regexp object, that
     * parsing time will be saved because the same Regexp can be used with
     * respect to many different strings.
     *
     * @param	pat
     *          The string holding the regular expression pattern.
     *
     * @throws	IllegalArgumentException if the pattern is malformed.
     *		The detail message for the exception will be set to a
     *		string indicating how the pattern was malformed.
     */
    public
    Regexp(String pat)
	throws IllegalArgumentException
    {
	compile(pat);
    }

    /**
     * Compiles a new Regexp object from the given regular expression
     * pattern.
     *
     * @param	pat
     *          The string holding the regular expression pattern.
     *
     * @param	ignoreCase
     *		If <code>true</code> then this regular expression will
     *		do case-insensitive matching.  If <code>false</code>, then
     *		the matches are case-sensitive.  Regular expressions
     *		generated by <code>Regexp(String)</code> are case-sensitive.
     *
     * @throws	IllegalArgumentException if the pattern is malformed.
     *		The detail message for the exception will be set to a
     *		string indicating how the pattern was malformed.
     */
    public
    Regexp(String pat, boolean ignoreCase)
	throws IllegalArgumentException
    {
        this.ignoreCase = ignoreCase;
	if (ignoreCase) {
	    pat = pat.toLowerCase();
	}
	compile(pat);
    }

    /**
     * Returns the number of parenthesized subexpressions in this regular
     * expression, plus one more for this expression itself.
     *
     * @return	The number.
     */
    public int
    subspecs()
    {
	return npar;
    }

    /**
     * Matches the given string against this regular expression.
     *
     * @param	str
     *		The string to match.
     *
     * @return	The substring of <code>str</code> that matched the entire
     *		regular expression, or <code>null</code> if the string did not
     *		match this regular expression.
     */
    public String
    match(String str)
    {
	Match m = exec(str, 0, 0);

	if (m == null) {
	    return null;
	}
	return str.substring(m.indices[0], m.indices[1]);
    }

    /**
     * Matches the given string against this regular expression, and computes
     * the set of substrings that matched the parenthesized subexpressions.
     * <p>
     * <code>substrs[0]</code> is set to the range of <code>str</code>
     * that matched the entire regular expression.
     * <p>
     * <code>substrs[1]</code> is set to the range of <code>str</code>
     * that matched the first (leftmost) parenthesized subexpression.
     * <code>substrs[n]</code> is set to the range that matched the
     * <code>n</code><i>th</i> subexpression, and so on.
     * <p>
     * If subexpression <code>n</code> did not match, then
     * <code>substrs[n]</code> is set to <code>null</code>.  Not to
     * be confused with "", which is a valid value for a
     * subexpression that matched 0 characters.
     * <p>
     * The length that the caller should use when allocating the
     * <code>substr</code> array is the return value of
     * <code>Regexp.subspecs</code>.  The array
     * can be shorter (in which case not all the information will
     * be returned), or longer (in which case the remainder of the
     * elements are initialized to <code>null</code>), or
     * <code>null</code> (to ignore the subexpressions).
     *
     * @param	str
     *		The string to match.
     *
     * @param	substrs
     * 		An array of strings allocated by the caller, and filled in
     *		with information about the portions of <code>str</code> that
     *		matched the regular expression.  May be <code>null</code>.
     *
     * @return	<code>true</code> if <code>str</code> that matched this
     *		regular expression, <code>false</code> otherwise.
     *		If <code>false</code> is returned, then the contents of
     *		<code>substrs</code> are unchanged.
     *
     * @see	#subspecs
     */
    public boolean
    match(String str, String[] substrs)
    {
	Match m = exec(str, 0, 0);

	if (m == null) {
	    return false;
	}
	if (substrs != null) {
	    int max = Math.min(substrs.length, npar);
	    int i;
	    int j = 0;
	    for (i = 0; i < max; i++) {
		int start = m.indices[j++];
		int end = m.indices[j++];
		if (start < 0) {
		    substrs[i] = null;
		} else {
		    substrs[i] = str.substring(start, end);
		}
	    }
	    for ( ; i < substrs.length; i++) {
		substrs[i] = null;
	    }
	}
	return true;
    }

    /**
     * Matches the given string against this regular expression, and computes
     * the set of substrings that matched the parenthesized subexpressions.
     * <p>
     * For the indices specified below, the range extends from the character
     * at the starting index up to, but not including, the character at the
     * ending index.
     * <p>
     * <code>indices[0]</code> and <code>indices[1]</code> are set to
     * starting and ending indices of the range of <code>str</code>
     * that matched the entire regular expression.
     * <p>
     * <code>indices[2]</code> and <code>indices[3]</code> are set to the
     * starting and ending indices of the range of <code>str</code> that
     * matched the first (leftmost) parenthesized subexpression.
     * <code>indices[n * 2]</code> and <code>indices[n * 2 + 1]</code>
     * are set to the range that matched the <code>n</code><i>th</i>
     * subexpression, and so on.
     * <p>
     * If subexpression <code>n</code> did not match, then
     * <code>indices[n * 2]</code> and <code>indices[n * 2 + 1]</code>
     * are both set to <code>-1</code>.
     * <p>
     * The length that the caller should use when allocating the
     * <code>indices</code> array is twice the return value of
     * <code>Regexp.subspecs</code>.  The array
     * can be shorter (in which case not all the information will
     * be returned), or longer (in which case the remainder of the
     * elements are initialized to <code>-1</code>), or
     * <code>null</code> (to ignore the subexpressions).
     *
     * @param	str
     *		The string to match.
     *
     * @param	indices
     * 		An array of integers allocated by the caller, and filled in
     *		with information about the portions of <code>str</code> that
     *		matched all the parts of the regular expression.
     *		May be <code>null</code>.
     *
     * @return	<code>true</code> if the string matched the regular expression,
     *		<code>false</code> otherwise.  If <code>false</code> is
     *		returned, then the contents of <code>indices</code> are
     *		unchanged.
     *
     * @see	#subspecs
     */
    public boolean
    match(String str, int[] indices)
    {
	Match m = exec(str, 0, 0);

	if (m == null) {
	    return false;
	}
	if (indices != null) {
	    int max = Math.min(indices.length, npar * 2);
	    System.arraycopy(m.indices, 0, indices, 0, max);

	    for (int i = max; i < indices.length; i++) {
		indices[i] = -1;
	    }
	}
	return true;
    }

    /**
     * Matches a string against a regular expression and replaces the first
     * match with the string generated from the substitution parameter.
     *
     * @param	str
     *		The string to match against this regular expression.
     *
     * @param	subspec
     * 		The substitution parameter, described in <a href=#regsub>
     *		REGULAR EXPRESSION SUBSTITUTION</a>.
     *
     * @return	The string formed by replacing the first match in
     *		<code>str</code> with the string generated from
     *		<code>subspec</code>.  If no matches were found, then
     *		the return value is <code>null</code>.
     */
    public String
    sub(String str, String subspec)
    {
	Regsub rs = new Regsub(this, str);
	if (rs.nextMatch()) {
	    StringBuffer sb = new StringBuffer(rs.skipped());
	    applySubspec(rs, subspec, sb);
	    sb.append(rs.rest());

	    return sb.toString();
	} else {
	    return null;
	}
    }

    /**
     * Matches a string against a regular expression and replaces all
     * matches with the string generated from the substitution parameter.
     * After each substutition is done, the portions of the string already
     * examined, including the newly substituted region, are <b>not</b> checked
     * again for new matches -- only the rest of the string is examined.
     *
     * @param	str
     *		The string to match against this regular expression.
     *
     * @param	subspec
     * 		The substitution parameter, described in <a href=#regsub>
     *		REGULAR EXPRESSION SUBSTITUTION</a>.
     *
     * @return	The string formed by replacing all the matches in
     *		<code>str</code> with the strings generated from
     *		<code>subspec</code>.  If no matches were found, then
     *		the return value is a copy of <code>str</code>.
     */
    public String
    subAll(String str, String subspec)
    {
	return sub(str, new SubspecFilter(subspec, true));
    }

    /**
     * Utility method to give access to the standard substitution algorithm
     * used by <code>sub</code> and <code>subAll</code>.  Appends to the
     * string buffer the string generated by applying the substitution
     * parameter to the matched region.
     *
     * @param	rs
     *		Information about the matched region.
     *
     * @param	subspec
     *		The substitution parameter.
     *
     * @param	sb
     *		StringBuffer to which the generated string is appended.
     */
    public static void
    applySubspec(Regsub rs, String subspec, StringBuffer sb)
    {
	try {
	    int len = subspec.length();
	    for (int i = 0; i < len; i++) {
		char ch = subspec.charAt(i);
		switch (ch) {
		    case '&': {
			sb.append(rs.matched());
			break;
		    }
		    case '\\': {
			i++;
			ch = subspec.charAt(i);
			if ((ch >= '0') && (ch <= '9')) {
			    String match = rs.submatch(ch - '0');
			    if (match != null) {
				sb.append(match);
			    }
			    break;
			}
			// fall through.
		    }
		    default: {
			sb.append(ch);
		    }
		}
	    }
	} catch (IndexOutOfBoundsException e) {
	    /*
	     * Ignore malformed substitution pattern.
	     * Return string matched so far.
	     */
	}
    }

    public String
    sub(String str, Filter rf)
    {
	Regsub rs = new Regsub(this, str);
	if (rs.nextMatch() == false) {
	    return str;
	}

	StringBuffer sb = new StringBuffer();
	do {
	    sb.append(rs.skipped());
	    if (rf.filter(rs, sb) == false) {
		break;
	    }
	} while (rs.nextMatch());
	sb.append(rs.rest());
	return sb.toString();
    }

    /**
     * This interface is used by the <code>Regexp</code> class to generate
     * the replacement string for each pattern match found in the source
     * string.
     *
     * @author	Colin Stevens (colin.stevens@sun.com)
     * @version	2.3, 04/12/30
     */
    public interface Filter
    {
	/**
	 * Given the current state of the match, generate the replacement
	 * string.  This method will be called for each match found in
	 * the source string, unless this filter decides not to handle any
	 * more matches.
	 * <p>
	 * The implementation can use whatever rules it chooses
	 * to generate the replacement string.  For example, here is an
	 * example of a filter that replaces the first <b>5</b>
	 * occurrences of "%XX" in a string with the ASCII character
	 * represented by the hex digits "XX":
	 * <pre>
	 * String str = ...;
	 *
	 * Regexp re = new Regexp("%[a-fA-F0-9][a-fA-F0-9]");
	 *
	 * Regexp.Filter rf = new Regexp.Filter() {
	 *     int count = 5;
	 *     public boolean filter(Regsub rs, StringBuffer sb) {
	 *         String match = rs.matched();
	 *         int hi = Character.digit(match.charAt(1), 16);
	 *         int lo = Character.digit(match.charAt(2), 16);
	 *         sb.append((char) ((hi &lt;&lt; 4) | lo));
	 *         return (--count > 0);
	 *     }
	 * }
	 *
	 * String result = re.sub(str, rf);
	 * </pre>
	 *
	 * @param   rs
	 *	    <code>Regsub</code> containing the state of the current
	 *	    match.
	 *
	 * @param   sb
	 *	    The string buffer that this filter should append the
	 *	    generated string to.  This string buffer actually
	 *	    contains the results the calling <code>Regexp</code> has
	 *	    generated up to this point.
	 *
	 * @return  <code>false</code> if no further matches should be
	 *	    considered in this string, <code>true</code> to allow
	 *	    <code>Regexp</code> to continue looking for further
	 *	    matches.
	 */
	public boolean filter(Regsub rs, StringBuffer sb);
    }

    private static class SubspecFilter implements Filter
    {
	String subspec;
	boolean all;

	public
	SubspecFilter(String subspec, boolean all)
	{
	    this.subspec = subspec;
	    this.all = all;
	}

	public boolean
	filter(Regsub rs, StringBuffer sb)
	{
	    applySubspec(rs, subspec, sb);
	    return all;
	}
    }

    /**
     * Returns a string representation of this compiled regular
     * expression.  The format of the string representation is a
     * symbolic dump of the bytecodes.
     *
     * @return	A string representation of this regular expression.
     */
    public String
    toString()
    {
	StringBuffer sb = new StringBuffer();

	sb.append("# subs:  " + npar + "\n");
	sb.append("anchor:  " + anchored + "\n");
	sb.append("start:   " + (char) startChar + "\n");
	sb.append("must:    " + must + "\n");

	for (int i = 0; i < program.length; ) {
	    sb.append(i + ":\t");
	    int op = program[i];
	    if (op >= CLOSE) {
		sb.append("CLOSE" + (op - CLOSE));
	    } else if (op >= OPEN) {
		sb.append("OPEN" + (op - OPEN));
	    } else {
		sb.append(opnames[op]);
	    }
	    int line;
	    int offset = (int) program[i + 1];
	    if (offset == 0) {
		sb.append('\t');
	    } else if (op == BACK) {
		sb.append("\t-" + offset + "," + (i - offset));
	    } else {
		sb.append("\t+" + offset + "," + (i + offset));
	    }

	    if ((op == ANYOF) || (op == ANYBUT) || (op == EXACTLY)) {
		sb.append("\t'");
		sb.append(program, i + 3, program[i + 2]);
		sb.append("'");
		i += 3 + program[i + 2];
	    } else {
		i += 2;
	    }
	    sb.append('\n');
	}
	return sb.toString();
    }


    private void
    compile(String exp)
	throws IllegalArgumentException
    {
	Compiler rcstate = new Compiler();
	rcstate.parse = exp.toCharArray();
	rcstate.off = 0;
	rcstate.npar = 1;
	rcstate.code = new StringBuffer();

	rcstate.reg(false);

	program = rcstate.code.toString().toCharArray();
	npar = rcstate.npar;
	startChar = -1;

	/* optimize */
	if (program[rcstate.regnext(0)] == END) {
	    if (program[2] == BOL) {
		anchored = true;
	    } else if (program[2] == EXACTLY) {
		startChar = (int) program[5];
	    }
	}

	/*
	 * If there's something expensive in the r.e., find the
	 * longest literal string that must appear and make it the
	 * regmust.  Resolve ties in favor of later strings, since
	 * the regstart check works with the beginning of the r.e.
	 * and avoiding duplication strengthens checking.  Not a
	 * strong reason, but sufficient in the absence of others.
	 */
/*
	if ((rcstate.flagp & Compiler.SPSTART) != 0) {
	    int index = -1;
	    int longest = 0;

	    for (scan = 0; scan < program.length; ) {
		switch (program[scan]) {
		    case EXACTLY:
			int length = program[scan + 2];
			if (length > longest) {
			    index = scan;
			    longest = length;
			}
			// fall through;

		    case ANYOF:
		    case ANYBUT:
			scan += 3 + program[scan + 2];
			break;

		    default:
			scan += 2;
			break;
		}
	    }
	    if (longest > 0) {
		must = new String(program, index + 3, longest);
	    }
	}
*/

    }

    Match
    exec(String str, int start, int off)
    {
	if (ignoreCase) {
	    str = str.toLowerCase();
	}
	
	Match match = new Match();

	match.program = program;

	/* Mark beginning of line for ^ . */
	match.str = str;
	match.bol = start;
	match.length = str.length();

	match.indices = new int[npar * 2];

	if (anchored) {
	    /* Simplest case:  anchored match need be tried only once. */
	    if (match.regtry(off)) {
		return match;
	    }
	} else if (startChar >= 0) {
	    /* We know what char it must start with. */
	    while (off < match.length) {
		off = str.indexOf(startChar, off);
		if (off < 0) {
		    break;
		}
		if (match.regtry(off)) {
		    return match;
		}
		off++;
	    }
	} else {
	    /* Messy cases:  unanchored match. */
	    do {
		if (match.regtry(off)) {
		    return match;
		}
	    } while (off++ < match.length);
	}
	return null;
    }

    static class Compiler {
	char[] parse;
	int off;
	int npar;
	StringBuffer code;
	int flagp;


	static final String META = "^$.[()|?+*\\";
	static final String MULT = "*+?";

	static final int WORST		= 00;	/* Worst case. */
	static final int HASWIDTH 	= 01;	/* Known never to match null string. */
	static final int SIMPLE		= 02;	/* Simple enough to be STAR/PLUS operand. */
	static final int SPSTART	= 04;	/* Starts with * or +. */

	/*
	 - reg - regular expression, i.e. main body or parenthesized thing
	 *
	 * Caller must absorb opening parenthesis.
	 *
	 * Combining parenthesis handling with the base level of regular expression
	 * is a trifle forced, but the need to tie the tails of the branches to what
	 * follows makes it hard to avoid.
	 */
	int reg(boolean paren) throws IllegalArgumentException
	{
	    int netFlags = HASWIDTH;
	    int parno = 0;

	    int ret = -1;
	    if (paren) {
		parno = npar++;
		if (npar >= NSUBEXP) {
		    throw new IllegalArgumentException("too many ()");
		}
		ret = regnode((char) (OPEN + parno));
	    }

	    /* Pick up the branches, linking them together. */
	    int br = regbranch();
	    if (ret >= 0) {
		regtail(ret, br);
	    } else {
		ret = br;
	    }

	    if ((flagp & HASWIDTH) == 0) {
		netFlags &= ~HASWIDTH;
	    }
	    netFlags |= (flagp & SPSTART);
	    while ((off < parse.length) && (parse[off] == '|')) {
		off++;
		br = regbranch();
		regtail(ret, br);
		if ((flagp & HASWIDTH) == 0) {
		    netFlags &= ~HASWIDTH;
		}
		netFlags |= (flagp & SPSTART);
	    }

	    /* Make a closing node, and hook it on the end. */
	    int ender = regnode((paren) ? (char) (CLOSE + parno) : END);
	    regtail(ret, ender);

	    /* Hook the tails of the branches to the closing node. */
	    for (br = ret; br >= 0; br = regnext(br)) {
		regoptail(br, ender);
	    }

	    /* Check for proper termination. */
	    if (paren && ((off >= parse.length) || (parse[off++] != ')'))) {
		throw new IllegalArgumentException("missing )");
	    } else if ((paren == false) && (off < parse.length)) {
		throw new IllegalArgumentException("unexpected )");
	    }

	    flagp = netFlags;
	    return ret;
	}

	/*
	 - regbranch - one alternative of an | operator
	 *
	 * Implements the concatenation operator.
	 */
	int regbranch() throws IllegalArgumentException
	{
	    int netFlags = WORST;	/* Tentatively. */

	    int ret = regnode(BRANCH);
	    int chain = -1;
	    while ((off < parse.length) && (parse[off] != '|')
			&& (parse[off] != ')')) {
		int latest = regpiece();
		netFlags |= flagp & HASWIDTH;
		if (chain < 0) {	/* First piece. */
		    netFlags |= (flagp & SPSTART);
		} else {
		    regtail(chain, latest);
		}
		chain = latest;
	    }
	    if (chain < 0) {	/* Loop ran zero times. */
		regnode(NOTHING);
	    }

	    flagp = netFlags;
	    return ret;
	}

	/*
	 - regpiece - something followed by possible [*+?]
	 *
	 * Note that the branching code sequences used for ? and the general cases
	 * of * and + are somewhat optimized:  they use the same NOTHING node as
	 * both the endmarker for their branch list and the body of the last branch.
	 * It might seem that this node could be dispensed with entirely, but the
	 * endmarker role is not redundant.
	 */
	int regpiece() throws IllegalArgumentException
	{
	    int netFlags;

	    int ret = regatom();

	    if ((off >= parse.length) || (isMult(parse[off]) == false)) {
		return ret;
	    }
	    char op = parse[off];

	    if (((flagp & HASWIDTH) == 0) && (op != '?')) {
		throw new IllegalArgumentException("*+ operand could be empty");
	    }
	    netFlags = (op != '+') ? (WORST | SPSTART) : (WORST | HASWIDTH);

	    if ((op == '*') && ((flagp & SIMPLE) != 0)) {
		reginsert(STAR, ret);
	    } else if (op == '*') {
		/* Emit x* as (x&|), where & means "self". */
		reginsert(BRANCH, ret);			/* Either x */
		regoptail(ret, regnode(BACK));		/* and loop */
		regoptail(ret, ret);			/* back */
		regtail(ret, regnode(BRANCH));		/* or */
		regtail(ret, regnode(NOTHING));		/* null. */
	    } else if ((op == '+') && ((flagp & SIMPLE) != 0)) {
		reginsert(PLUS, ret);
	    } else if (op == '+') {
		/* Emit x+ as x(&|), where & means "self". */
		int next = regnode(BRANCH);		/* Either */
		regtail(ret, next);
		regtail(regnode(BACK), ret);		/* loop back */
		regtail(next, regnode(BRANCH));		/* or */
		regtail(ret, regnode(NOTHING));		/* null. */
	    } else if (op == '?') {
		/* Emit x? as (x|) */
		reginsert(BRANCH, ret);			/* Either x */
		regtail(ret, regnode(BRANCH));		/* or */
		int next = regnode(NOTHING);		/* null. */
		regtail(ret, next);
		regoptail(ret, next);
	    }
	    off++;
	    if ((off < parse.length) && isMult(parse[off])) {
		throw new IllegalArgumentException("nested *?+");
	    }

	    flagp = netFlags;
	    return ret;
	}

	/*
	 - regatom - the lowest level
	 *
	 * Optimization:  gobbles an entire sequence of ordinary characters so that
	 * it can turn them into a single node, which is smaller to store and
	 * faster to run.  Backslashed characters are exceptions, each becoming a
	 * separate node; the code is simpler that way and it's not worth fixing.
	 */
	int regatom() throws IllegalArgumentException
	{
	    int netFlags = WORST;		/* Tentatively. */
	    int ret;

	    switch (parse[off++]) {
		case '^':
		    ret = regnode(BOL);
		    break;
		case '$':
		    ret = regnode(EOL);
		    break;
		case '.':
		    ret = regnode(ANY);
		    netFlags |= (HASWIDTH | SIMPLE);
		    break;
		case '[': {
		    try {
			if (parse[off] == '^') {
			    ret = regnode(ANYBUT);
			    off++;
			} else {
			    ret = regnode(ANYOF);
			}

			int pos = reglen();
			regc('\0');

			if ((parse[off] == ']') || (parse[off] == '-')) {
			    regc(parse[off++]);
			}
			while (parse[off] != ']') {
			    if (parse[off] == '-') {
				off++;
				if (parse[off] == ']') {
				    regc('-');
				} else {
				    int start = parse[off - 2];
				    int end = parse[off++];
				    if (start > end) {
					throw new IllegalArgumentException(
						"invalid [] range");
				    }
				    for (int i = start + 1; i <= end; i++) {
					regc((char) i);
				    }
				}
			    } else {
				regc(parse[off++]);
			    }
			}
			regset(pos, (char) (reglen() - pos - 1));
			off++;
			netFlags |= HASWIDTH | SIMPLE;
		    } catch (ArrayIndexOutOfBoundsException e) {
			throw new IllegalArgumentException("missing ]");
		    }
		    break;
		}
		case '(':
		    ret = reg(true);
		    netFlags |= (flagp & (HASWIDTH | SPSTART));
		    break;
		case '|':
		case ')':
		    throw new IllegalArgumentException("internal urp");
		case '?':
		case '+':
		case '*':
		    throw new IllegalArgumentException("?+* follows nothing");
		case '\\':
		    if (off >= parse.length) {
			throw new IllegalArgumentException("trailing \\");
		    }
		    ret = regnode(EXACTLY);
		    regc((char) 1);
		    regc(parse[off++]);
		    netFlags |= HASWIDTH | SIMPLE;
		    break;
		default: {
		    off--;
		    int end;
		    for (end = off; end < parse.length; end++) {
			if (META.indexOf(parse[end]) >= 0) {
			    break;
			}
		    }
		    if ((end > off + 1) && (end < parse.length)
				&& isMult(parse[end])) {
			end--;		/* Back off clear of ?+* operand. */
		    }
		    netFlags |= HASWIDTH;
		    if (end == off + 1) {
			netFlags |= SIMPLE;
		    }
		    ret = regnode(EXACTLY);
		    regc((char) (end - off));
		    for ( ; off < end; off++) {
			regc(parse[off]);
		    }
		}
		break;
	    }

	    flagp = netFlags;
	    return ret;
	}

	/*
	 - regnode - emit a node
	 */
	int regnode(char op)
	{
	    int ret = code.length();
	    code.append(op);
	    code.append('\0');

	    return ret;
	}

	/*
	 - regc - emit (if appropriate) a byte of code
	 */
	void regc(char b)
	{
	    code.append(b);
	}

	int reglen()
	{
	    return code.length();
	}

	void regset(int pos, char ch)
	{
	    code.setCharAt(pos, ch);
	}


	/*
	 - reginsert - insert an operator in front of already-emitted operand
	 *
	 * Means relocating the operand.
	 */
	void reginsert(char op, int pos)
	{
	    char[] tmp = new char[] {op, '\0'};
	    code.insert(pos, tmp);
	}

	/*
	 - regtail - set the next-pointer at the end of a node chain
	 */
	void regtail(int pos, int val)
	{
	    /* Find last node. */

	    int scan = pos;
	    while (true) {
		int tmp = regnext(scan);
		if (tmp < 0) {
		    break;
		}
		scan = tmp;
	    }

	    int offset = (code.charAt(scan) == BACK) ? scan - val : val - scan;
	    code.setCharAt(scan + 1, (char) offset);
	}

	/*
	 - regoptail - regtail on operand of first argument; nop if operandless
	 */
	void regoptail(int pos, int val)
	{
	    if ((pos < 0) || (code.charAt(pos) != BRANCH)) {
		return;
	    }
	    regtail(pos + 2, val);
	}


	/*
	 - regnext - dig the "next" pointer out of a node
	 */
	int regnext(int pos)
	{
	    int offset = code.charAt(pos + 1);
	    if (offset == 0) {
		return -1;
	    }
	    if (code.charAt(pos) == BACK) {
		return pos - offset;
	    } else {
		return pos + offset;
	    }
	}

	static boolean isMult(char ch)
	{
	    return (ch == '*') || (ch == '+') || (ch == '?');
	}

    }

    static class Match {
	char[] program;

	String str;
	int bol;
	int input;
	int length;

	int[] indices;

	boolean regtry(int off)
	{
	    this.input = off;

	    for (int i = 0; i < indices.length; i++) {
		indices[i] = -1;
	    }

	    if (regmatch(0)) {
		indices[0] = off;
		indices[1] = input;
		return true;
	    } else {
		return false;
	    }
	}

	/*
	 - regmatch - main matching routine
	 *
	 * Conceptually the strategy is simple:  check to see whether the current
	 * node matches, call self recursively to see whether the rest matches,
	 * and then act accordingly.  In practice we make some effort to avoid
	 * recursion, in particular by going through "ordinary" nodes (that don't
	 * need to know whether the rest of the match failed) by a loop instead of
	 * by recursion.
	 */
	boolean regmatch(int scan)
	{
	    while (true) {
		int next = regnext(scan);
		int op = program[scan];
		switch (op) {
		    case BOL:
			if (input != bol) {
			    return false;
			}
			break;

		    case EOL:
			if (input != length) {
			    return false;
			}
			break;

		    case ANY:
			if (input >= length) {
			    return false;
			}
			input++;
			break;

		    case EXACTLY: {
			if (compare(scan) == false) {
			    return false;
			}
			break;
		    }

		    case ANYOF:
			if (input >= length) {
			    return false;
			}
			if (present(scan) == false) {
			    return false;
			}
			input++;
			break;

		    case ANYBUT:
			if (input >= length) {
			    return false;
			}
			if (present(scan)) {
			    return false;
			}
			input++;
			break;

		    case NOTHING:
		    case BACK:
			break;

		    case BRANCH: {
			if (program[next] != BRANCH) {
			    next = scan + 2;
			} else {
			    do {
				int save = input;
				if (regmatch(scan + 2)) {
				    return true;
				}
				input = save;
				scan = regnext(scan);
			    } while ((scan >= 0) && (program[scan] == BRANCH));
			    return false;
			}
			break;
		    }

		    case STAR:
		    case PLUS: {
			/*
			 * Lookahead to avoid useless match attempts
			 * when we know what character comes next.
			 */

			int ch = -1;
			if (program[next] == EXACTLY) {
			    ch = program[next + 3];
			}

			int min = (op == STAR) ? 0 : 1;
			int save = input;
			int no = regrepeat(scan + 2);

			while (no >= min) {
			    /* If it could work, try it. */
			    if ((ch < 0) || ((input < length)
			            && (str.charAt(input) == ch))) {
				if (regmatch(next)) {
				    return true;
				}
			    }
			    /* Couldn't or didn't -- back up. */
			    no--;
			    input = save + no;
			}
			return false;
		    }

		    case END:
			return true;

		    default:
			if (op >= CLOSE) {
			    int no = op - CLOSE;
			    int save = input;

			    if (regmatch(next)) {
				/*
				 * Don't set endp if some later
				 * invocation of the same parentheses
				 * already has.
				 */
				if (indices[no * 2 + 1] <= 0) {
				    indices[no * 2 + 1] = save;
				}
				return true;
			    }
			} else if (op >= OPEN) {
			    int no = op - OPEN;
			    int save = input;

			    if (regmatch(next)) {
				/*
				 * Don't set startp if some later invocation of the
				 * same parentheses already has.
				 */
				if (indices[no * 2] <= 0) {
				    indices[no * 2] = save;
				}
				return true;
			    }
			}
			return false;
		}
		scan = next;
	    }
	}

	boolean compare(int scan)
	{
	    int count = program[scan + 2];
	    if (input + count > length) {
		return false;
	    }
	    int start = scan + 3;
	    int end = start + count;
	    for (int i = start; i < end; i++) {
		if (str.charAt(input++) != program[i]) {
		    return false;
		}
	    }
	    return true;
	}

	boolean present(int scan)
	{
	    char ch = str.charAt(input);

	    int count = program[scan + 2];
	    int start = scan + 3;
	    int end = start + count;

	    for (int i = start; i < end; i++) {
		if (program[i] == ch) {
		    return true;
		}
	    }
	    return false;
	}


	/*
	 - regrepeat - repeatedly match something simple, report how many
	 */
	int regrepeat(int scan)
	{
	    int op = program[scan];
	    int count = 0;

	    switch (op) {
		case ANY:
		    // '.*' matches all the way to the end.

		    count = length - input;
		    input = length;
		    break;

		case EXACTLY: {
		    // 'g*' matches all the following 'g' characters.

		    char ch = program[scan + 3];
		    while ((input < length) && (str.charAt(input) == ch)) {
			input++;
			count++;
		    }
		    break;
		}

		case ANYOF:
		    // [abc]*

		    while ((input < length) && present(scan)) {
			input++;
			count++;
		    }
		    break;


		case ANYBUT:
		    while ((input < length) && !present(scan)) {
			input++;
			count++;
		    }
		    break;

	    }
	    return count;
	}

	/*
	 - regnext - dig the "next" pointer out of a node
	 */
	int regnext(int scan)
	{
	    int offset = program[scan + 1];
	    if (program[scan] == BACK) {
		return scan - offset;
	    } else {
		return scan + offset;
	    }
	}

	public String toString() {
	    String result = "Match: str=" + str + " ";
	    for (int i=0;i<indices.length;i++) {
		result += " " + indices[i];
	    }
	    return result;
	}
    }
}
