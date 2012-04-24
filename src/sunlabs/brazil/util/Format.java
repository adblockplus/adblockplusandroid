/*
 * Format.java
 *
 * Brazil project web application toolkit,
 * export version: 2.3 
 * Copyright (c) 2000-2006 Sun Microsystems, Inc.
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
 * Contributor(s): cstevens, drach, suhler.
 *
 * Version:  2.7
 * Created by cstevens on 00/04/17
 * Last modified by suhler on 06/11/13 15:08:02
 *
 * Version Histories:
 *
 * 2.7 06/11/13-15:08:02 (suhler)
 *   add "unsubst()" for protecting html
 *
 * 2.6 05/06/16-08:00:31 (suhler)
 *   add public String dequoting method
 *
 * 2.5 04/11/30-14:59:57 (suhler)
 *   fixed sccs version string
 *
 * 2.4 04/04/28-15:54:50 (suhler)
 *   added \s for spaces and \v for ' (\q was taken)
 *
 * 2.3 03/08/01-16:17:14 (suhler)
 *   fixes for javadoc
 *
 * 2.2 03/07/10-09:24:59 (suhler)
 *   Add static methods to check for the boolean value of a string
 *
 * 2.1 02/10/01-16:36:58 (suhler)
 *   version change
 *
 * 1.18 01/10/16-15:59:54 (suhler)
 *   add "\t" for TAB
 *
 * 1.17 01/09/03-20:34:46 (suhler)
 *   add \r to esape list
 *
 * 1.16 01/08/28-20:39:59 (suhler)
 *   added \a \l and \g for & < and >
 *
 * 1.15 01/08/20-16:38:25 (suhler)
 *   lint
 *
 * 1.14 01/07/19-20:58:41 (suhler)
 *   doc lint
 *
 * 1.13 01/07/18-10:40:03 (suhler)
 *   allow escape behavior to be spicified on a per-call basis
 *
 * 1.12 01/07/16-16:54:17 (suhler)
 *   rationalize escaping mechanism, remove unused code
 *
 * 1.11 01/06/29-11:08:25 (drach)
 *   Hopefully, easier to understand substitution mechanism
 *
 * 1.10 01/03/23-14:50:03 (cstevens)
 *
 * 1.9 01/01/11-17:28:52 (cstevens)
 *   Merged changes between child workspace "/home/cstevens/ws/brazil/naws" and
 *   parent workspace "/export/ws/brazil/naws".
 *
 * 1.6.1.1 01/01/11-17:22:24 (cstevens)
 *   default values in properties (temporary)
 *
 * 1.8 01/01/07-19:07:01 (suhler)
 *   allow escaping of ${...} with $\{...\}
 *
 * 1.7 00/12/18-11:24:14 (suhler)
 *   allow ${...} to be escaped.
 *
 * 1.6 00/12/11-13:33:30 (suhler)
 *   doc typo
 *
 * 1.5 00/12/05-13:18:05 (suhler)
 *   cleanup
 *
 * 1.4 00/10/05-15:52:16 (cstevens)
 *   PropsTemplate.subst() and PropsTemplate.getProperty() moved to the Format
 *   class.
 *
 * 1.3 00/04/26-16:14:47 (suhler)
 *   doc update
 *
 * 1.2 00/04/17-16:40:17 (cstevens)
 *   Truncating result
 *
 * 1.2 00/04/17-15:01:38 (Codemgr)
 *   SunPro Code Manager data about conflicts, renames, etc...
 *   Name history : 1 0 util/Format.java
 *
 * 1.1 00/04/17-15:01:37 (cstevens)
 *   date and time created 00/04/17 15:01:37 by cstevens
 *
 */

package sunlabs.brazil.util;

import java.util.Dictionary;
import java.util.Properties;
import java.util.StringTokenizer;

/**
 * Format a string by substituting values into it
 * from a properties object.
 *
 * @author colin stevens
 * @author stephen uhler
 */

public class Format {
    /**
     * Allow a property name to contain the value of another
     * property, permitting nested variable substitution in attribute
     * values.  The name of the embedded property to be substituted is
     * bracketted by "${" and "}".  See {@link #subst}.
     * <blockquote>
     * "ghi"		=> "foo"<br>
     * "deffoojkl"	=> "baz"<br>
     * "abcbazmno"	=> "garply"<br>
     * 
     * getProperty("ghi")			=> "foo"<br>
     * getProperty("def${ghi}jkl")		=> "baz"<br>
     * getProperty("abc${def${ghi}jkl}mno")	=> "garply"<br>
     * </blockquote>
     *
     * @param	props
     *		The table of variables to use when substituting.
     *
     * @param	expr
     *		The property name, prossibly containing substitutions.
     *
     * @param	defaultValue
     *		The value to use if the top-level substitution does not
     *		exist.  May be <code>null</code>.
     */

    public static String
    getProperty(Properties props, String expr, String defaultValue) {
	return props.getProperty(subst(props, expr), defaultValue);
    }

    /**
     * Allow a tag attribute value to contain the value of another
     * property, permitting nested variable substitution in attribute
     * values. To escape ${XXX}, use \${XXX}.
     * <p>
     * The sequence "\X" is identical to "X", except when "X" is one of:
     * <dl>
     * <dt>$<dd>A literal "$", that will not introduce a variable
     *      substitution if it is followed by "{".
     * <dt>n<dd>Insert a NL (newline) character
     * <dt>r<dd>Insert a CR (Carriage return) character
     * <dt>"<dd>Insert a single quote (").
     * <dt>l<dd>Insert a (<).
     * <dt>g<dd>Insert a (>).
     * <dt>a<dd>Insert a (&).
     * <dt>end of value<dd>Insert a "\".
     * </dl>
     * <p>
     * <blockquote>
     * "ghi"		= "foo"<br>
     * "deffoojkl"	= "baz"<br>
     * "abcbazmno"	= "garply"<br>
     * 
     * subst("ghi")			    = "ghi"<br>
     * subst("def${ghi}jkl")		    = "deffoojkl"<br>
     * subst("def\${ghi}jkl")		    = "def${ghi}jkl"<br>
     * subst("abc${def${ghi}jkl}mno")	    = "abcbazmno"<br>
     * subst("${abc${def${ghi}jkl}mno}")    = "garply"<br>
     * </blockquote>
     *
     * @param props	The table of variables to substitute.
     *			If this is a Properties object, then the
     *			getProperty() method is used instead of the
     *			Dictionary class get() method.
     * @param str	The expression containing the substitutions.
     *			Embedded property names, bracketted by "${" and "}" 
     *			are looked up in the props table and replaced with
     *			their value.  Nested substitutions are allowed. 
     *
     * @return		The substituted string.  If a variable is not 
     *			found in the table, the empty string is used.
     */

    public static String
    subst(Dictionary props, String str) {
	return subst(props, str, false);
    }

    /**
     * Allow a tag attribute value to contain the value of another
     * property, permitting nested variable substitution in attribute
     * values. To escape ${XXX}, use \${XXX}.
     * See {@link #subst(Dictionary props, String str) above}.
     * <p>
     * if <code>noEsc</code> is true, then
     * The sequence "\X" is identical to "\X" for all X except X=$.
     */

    public static String
    subst(Dictionary props, String str, boolean noEsc) {
	if (str == null) {
	    return null;
	}
	return subst(props, new Chars(str), noEsc);
    }


    private static String subst(Dictionary dict, Chars chars, boolean esc) {
	StringBuffer sb = new StringBuffer();
	char c;
	char save;
	String result;
	String value;

	loop: while (true) {
	    c = chars.get();
	    switch (c) {
	    case Chars.NUL:
		break loop;
	    case '$':
		c = chars.get();
		switch (c) {
		case Chars.NUL:
		    sb.append('$');
		    break loop;
		case '{':
		    save = chars.setend('}');
		    result = subst(dict, chars, esc);
		    chars.setend(save);
		    value = getProperty(dict, result);
		    sb.append(value);
		    break;
		default:
		    chars.pushback();
		    sb.append('$');
		    break;
		}
		break;
	    case '\\':
		c = chars.getraw();
		if (c == Chars.NUL) {
		    sb.append('\\');
		    break loop;
		}
		if (esc) {   // only \$ is special
		    switch (c) {
		    case '$':
			break;
		    default:
			chars.pushback();
			c = '\\';
			break;
		    }
		} else {	// other stuff is special too
		    switch (c) {
		    case 'a':
			c = '&';
			break;
		    case 'g':
			c = '>';
			break;
		    case 'l':
			c = '<';
			break;
		    case 'q':
			c = '"';
			break;
		    case 's':
			c = ' ';
			break;
		    case 'v':
			c = '\'';
			break;
		    case 'n':
			c = '\n';
			break;
		    case 'r':
			c = '\r';
			break;
		    case 't':
			c = '\t';
			break;
		    default:
			break;
		    }
		}
		sb.append(c);
		break;
	    default:
		sb.append(c);
		break;
	    }
	}
	return sb.toString();
    }
    private static String getProperty(Dictionary dict, String name) {
	int hash = name.indexOf('#');
	String def = "";
	
	if (hash >= 0) {
	    def = name.substring(hash + 1);
	    name = name.substring(0, hash);
	}

	Object obj;
	if (dict instanceof Properties) {
	    obj = ((Properties) dict).getProperty(name);
	} else {
	    obj = dict.get(name);
	}
	String value = (obj == null) ? null : obj.toString();

	if (value == null || value.length() == 0)
	    value = def;

	return value;
    }

    /**
     * See if a String represents a "true" boolean value, which consists of:
     * "yes", "true", "on", or "1", in any case.
     */

    public static boolean isTrue(String s) {
	if (s != null) {
	    String v = s.trim().toLowerCase();
	    return v.equals("true") || v.equals("yes") ||
	           v.equals("on") || v.equals("1");
	}
	return false;
    }

    /**
     * See if a String represents a "false" boolean value, which consists of:
     * "no", "false", "off", or "0", in any case.
     */

    public static boolean isFalse(String s) {
	if (s != null) {
	    String v = s.trim().toLowerCase();
	    return v.equals("false") || v.equals("no") ||
	           v.equals("off") || v.equals("0");
	}
	return false;
    }

    /**
     * Remove surrounding quotes (" or ') from a string.
     */

    public static String deQuote(String str) {
	int len;
	if (str==null || (len=str.length()) < 2) {
	    return str;
	}
	char ch = str.charAt(0);
	if (((ch == '"') || (ch == '\'')) && (str.charAt(len-1) == ch)) {
	    return str.substring(1, len-1);
	}
	return str;
    }

    /**
     * Make an html string suitable for including as an attribute value.
     * Convert '<', '>', '&', '"', and ''' to \l, \g, \a, \q and \a.
     */

    public static String unsubst(String data) {
	String TOKENS = "<>&\"\'\n\t";

	StringTokenizer st = new StringTokenizer(data, TOKENS, true);
	StringBuffer sb = new StringBuffer();
	while (st.hasMoreTokens()) {
	    String token = st.nextToken();
	    if (token.length() > 0) {
		sb.append(token);
	    } else {
		switch(token.charAt(0)) {
		    case '<': sb.append("\\l"); break;
		    case '>': sb.append("\\g"); break;
		    case '&': sb.append("\\a"); break;
		    case '\"': sb.append("\\q"); break;
		    case '\'': sb.append("\\v"); break;
		    case '\n': sb.append("\\n"); break;
		    case '\t': sb.append("\\t"); break;
		    default: sb.append(token); break;
		}
	    }
	}
	return sb.toString();
    }

}

class Chars {
    public static final char NUL = '\0';
    private int i;
    private char end;
    private char[] chars;

    Chars(String s) {
	i = 0;
	end = NUL;
	chars = s.toCharArray();
    }

    char getraw() {
	if (i >= chars.length)
	    return NUL;
	return chars[i++];
    }

    char get() {
	char c = getraw();
	if (c == end)
	    return NUL;
	return c;
    }

    void pushback() {
	if (--i < 0)
	    i = 0;
    }

    char setend(char end) {
	char save = this.end;
	this.end = end;
	return save;
    }
}
