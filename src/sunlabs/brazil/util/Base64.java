/*
 * Base64.java
 *
 * Brazil project web application toolkit,
 * export version: 2.3 
 * Copyright (c) 2000-2004 Sun Microsystems, Inc.
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
 * Created by cstevens on 00/04/17
 * Last modified by suhler on 04/11/30 15:19:45
 *
 * Version Histories:
 *
 * 2.3 04/11/30-15:19:45 (suhler)
 *   fixed sccs version string
 *
 * 2.2 03/08/01-16:17:06 (suhler)
 *   fixes for javadoc
 *
 * 2.1 02/10/01-16:37:01 (suhler)
 *   version change
 *
 * 1.9 02/07/24-10:49:48 (suhler)
 *   doc updates
 *
 * 1.8 01/08/20-20:39:44 (suhler)
 *   oops
 *
 * 1.7 01/08/20-17:30:25 (suhler)
 *   can now encode partial byte arrays
 *
 * 1.6 01/08/01-11:07:34 (suhler)
 *   added encode(byte[])
 *
 * 1.5 01/01/07-19:07:58 (suhler)
 *   added a simple decoder, useful for un-encoding basic authentication
 *   credentials
 *
 * 1.4 00/10/31-10:20:41 (suhler)
 *   doc fixes
 *
 * 1.3 00/05/31-13:52:53 (suhler)
 *   docs
 *
 * 1.2 00/04/20-13:14:23 (cstevens)
 *   copyright
 *
 * 1.2 00/04/17-16:41:48 (Codemgr)
 *   SunPro Code Manager data about conflicts, renames, etc...
 *   Name history : 1 0 util/Base64.java
 *
 * 1.1 00/04/17-16:41:47 (cstevens)
 *   date and time created 00/04/17 16:41:47 by cstevens
 *
 */

package sunlabs.brazil.util;

/**
 * Utility to base64 encode and decode a string.
 * @author      Stephen Uhler
 * @version		2.3
 */

public class Base64 {
    static byte[] encodeData;
    static String charSet = 
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
    
    static {
    	encodeData = new byte[64];
	for (int i = 0; i<64; i++) {
	    byte c = (byte) charSet.charAt(i);
	    encodeData[i] = c;
	}
    }

    private Base64() {}

    /**
     * base-64 encode a string
     * @param s		The ascii string to encode
     * @return		The base64 encoded result
     */

    public static String
    encode(String s) {
        return encode(s.getBytes());
    }

    /**
     * base-64 encode a byte array
     * @param src	The byte array to encode
     * @return		The base64 encoded result
     */

    public static String
    encode(byte[] src) {
	return encode(src, 0, src.length);
    }

    /**
     * base-64 encode a byte array
     * @param src	The byte array to encode
     * @param start	The starting index
     * @param len	The number of bytes
     * @return		The base64 encoded result
     */

    public static String
    encode(byte[] src, int start, int length) {
        byte[] dst = new byte[(length+2)/3 * 4 + length/72];
        int x = 0;
        int dstIndex = 0;
        int state = 0;	// which char in pattern
        int old = 0;	// previous byte
        int len = 0;	// length decoded so far
	int max = length + start;
        for (int srcIndex = start; srcIndex<max; srcIndex++) {
	    x = src[srcIndex];
	    switch (++state) {
	    case 1:
	        dst[dstIndex++] = encodeData[(x>>2) & 0x3f];
		break;
	    case 2:
	        dst[dstIndex++] = encodeData[((old<<4)&0x30) | ((x>>4)&0xf)];
		break;
	    case 3:
	        dst[dstIndex++] = encodeData[((old<<2)&0x3C) | ((x>>6)&0x3)];
		dst[dstIndex++] = encodeData[x&0x3F];
		state = 0;
		break;
	    }
	    old = x;
	    if (++len >= 72) {
	    	dst[dstIndex++] = (byte) '\n';
	    	len = 0;
	    }
	}

	/*
	 * now clean up the end bytes
	 */

	switch (state) {
	case 1: dst[dstIndex++] = encodeData[(old<<4) & 0x30];
	   dst[dstIndex++] = (byte) '=';
	   dst[dstIndex++] = (byte) '=';
	   break;
	case 2: dst[dstIndex++] = encodeData[(old<<2) & 0x3c];
	   dst[dstIndex++] = (byte) '=';
	   break;
	}
	return new String(dst);
    }

    /**
     * A Base64 decoder.  This implementation is slow, and 
     * doesn't handle wrapped lines.
     * The output is undefined if there are errors in the input.
     * @param s		a Base64 encoded string
     * @return		The byte array eith the decoded result
     */

    public static byte[]
    decode(String s) {
      int end = 0;	// end state
      if (s.endsWith("=")) {
	  end++;
      }
      if (s.endsWith("==")) {
	  end++;
      }
      int len = (s.length() + 3)/4 * 3 - end;
      byte[] result = new byte[len];
      int dst = 0;
      try {
	  for(int src = 0; src< s.length(); src++) {
	      int code =  charSet.indexOf(s.charAt(src));
	      if (code == -1) {
	          break;
	      }
	      switch (src%4) {
	      case 0:
	          result[dst] = (byte) (code<<2);
	          break;
	      case 1: 
	          result[dst++] |= (byte) ((code>>4) & 0x3);
	          result[dst] = (byte) (code<<4);
	          break;
	      case 2:
	          result[dst++] |= (byte) ((code>>2) & 0xf);
	          result[dst] = (byte) (code<<6);
	          break;
	      case 3:
	          result[dst++] |= (byte) (code & 0x3f);
	          break;
	      }
	  }
      } catch (ArrayIndexOutOfBoundsException e) {}
      return result;
    }

    /**
     * Test the decoder and encoder.
     * Call as <code>Base64 [string]</code>.
     */

    public static void
    main(String[] args) {
    	System.out.println("encode: " + args[0]  + " -> (" + encode(args[0]) +
		")");
    	System.out.println("decode: " + args[0]  + " -> (" +
	    new String(decode(args[0])) + ")");
    }
}
