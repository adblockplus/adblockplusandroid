/*
 * MatchString.java
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
 * The Initial Developer of the Original Code is: suhler.
 * Portions created by suhler are Copyright (C) Sun Microsystems, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): suhler.
 *
 * Version:  2.3
 * Created by suhler on 01/07/16
 * Last modified by suhler on 06/11/13 15:08:19
 *
 * Version Histories:
 *
 * 2.3 06/11/13-15:08:19 (suhler)
 *   move MatchString to package "util" from "handler"
 *
 * 2.2 06/11/13-10:30:25 (suhler)
 *   add "invert" property to invert the sense of a match
 *
 * 2.1 02/10/01-16:36:35 (suhler)
 *   version change
 *
 * 1.6 02/05/01-11:22:22 (suhler)
 *   fix sccs version info
 *
 * 1.5 02/01/29-14:30:46 (suhler)
 *   doc lint
 *
 * 1.4 01/08/07-15:12:15 (suhler)
 *   doc lint
 *
 * 1.3 01/07/20-11:35:14 (suhler)
 *   MatchUrl -> MatchString
 *
 * 1.2 01/07/17-14:17:03 (suhler)
 *   add prefix() method
 *
 * 1.2 01/07/16-16:41:31 (Codemgr)
 *   SunPro Code Manager data about conflicts, renames, etc...
 *   Name history : 3 2 util/MatchString.java
 *   Name history : 2 1 handlers/MatchString.java
 *   Name history : 1 0 handlers/MatchUrl.java
 *
 * 1.1 01/07/16-16:41:30 (suhler)
 *   date and time created 01/07/16 16:41:30 by suhler
 *
 */

package sunlabs.brazil.util;

import sunlabs.brazil.util.regexp.Regexp;
import sunlabs.brazil.util.Glob;
import sunlabs.brazil.util.Format;
import java.util.Properties;

/**
 * Utility class for handlers to determine, based on the URL,
 * if the current request should be processed.
 * <p>
 * Properties:
 * <dl class=props>
 * <dt>prefix
 * <dd>The url prefix the url must match (defaults to "/").
 * <dt>suffix
 * <dd>The url suffix the url must match (defaults to "").
 * <dt>glob
 * <dd>The glob pattern the url must match. If defined, this
 * overrides both <code>prefix</code> and <code>suffix</code>.
 * <dt>match
 * <dd>The reqular expression pattern the url must match.  If defined, 
 * this overrides <code>glob</code>.
 * <dt>ignoreCase
 * <dd>If present and <code>match</code> is defined, this causes the
 * regular expression match to be case insensitive. By default, case counts.
 * <dt>invert
 * <dd>If true, the sense of the comparison is reversed
 * </dl>
 */

public class MatchString {
    String propsPrefix;		// our properties prefix
    Regexp re = null;		// our expression to match
    String glob = null;		// our glob match (if no re)
    boolean invert = false;	// should we invert the result

    static final String PREFIX = "prefix";
    static final String SUFFIX = "suffix";
    static final String MATCH =  "match";
    static final String GLOB =   "glob";
    static final String CASE =   "ignoreCase";
    static final String INVERT = "invert";

    /**
     * Create a matcher for per-request URL checking.
     * This constructer is used if the
     * properties are to be evaluated on each request.
     *
     * @param propsPrefix	The prefix to use in the properties object.
     */

    public MatchString(String propsPrefix) {
        this.propsPrefix = propsPrefix;
    }

    /**
     * Create a matcher for one-time-only checking. 
     * This constructor is used if the
     * properties are to be computed only once, at "init" time.
     *
     * @param propsPrefix	The prefix to use in the properties object.
     * @param props	The table to find the properties in.
     */

    public MatchString(String propsPrefix, Properties props) {
        this.propsPrefix = propsPrefix;
        setup(props);
    }

    /**
     * Extract and setup the properties
     */

    private void 
    setup(Properties props) {
	invert = Format.isTrue(propsPrefix + INVERT);
        String exp = props.getProperty(propsPrefix + MATCH);
        if (exp != null) {
	    boolean ignoreCase = (props.getProperty(propsPrefix + CASE)!=null);
	    try {
		re = new Regexp(exp, ignoreCase);
	    } catch (Exception e) {}
	}

	if (re == null) {
	    glob = props.getProperty(propsPrefix + GLOB,
		   props.getProperty(propsPrefix + PREFIX, "/") +
		   "*" +
		   props.getProperty(propsPrefix + SUFFIX, ""));
        }
    }

    /**
     * See if this is our url.  Use this version for properties
     * evaluated only at init time.
     */

    public boolean
    match(String url) {
	if (re != null) {
	    return (invert ^ (re.match(url) != null));
	} else if (glob != null) {
	    return invert ^ Glob.match(glob, url);
	} else {
	    throw new IllegalArgumentException("no properties provided");
	}
    }

    /**
     * See if this is our url.  Use this version for properties
     * evaluated at each request.
     */

    public boolean
    match(String url, Properties props) {
        setup(props);
        return match(url);
    }

    /**
     * Return our prefix
     */

    public String prefix() {
	return propsPrefix;
    }

    /**
     * print nicely
     */

    public String
    toString() {
        return  (glob + ", " + re);
    }
}
