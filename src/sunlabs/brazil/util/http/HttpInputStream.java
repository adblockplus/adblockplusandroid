/*
 * HttpInputStream.java
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
 * Last modified by suhler on 04/11/30 15:19:46
 *
 * Version Histories:
 *
 * 2.2 04/11/30-15:19:46 (suhler)
 *   fixed sccs version string
 *
 * 2.1 02/10/01-16:37:04 (suhler)
 *   version change
 *
 * 1.8 02/04/18-11:18:42 (suhler)
 *   changed the semantics of copyTo() to make guarantees about the number
 *   of bytes read from the stream.  This *shouldn't* break anything (I hope)
 *
 * 1.7 00/03/29-16:47:15 (cstevens)
 *   Streaming media didn't work because data from server wasn't flushed to client
 *   until the end of the request.
 *
 * 1.6 99/11/09-20:24:44 (cstevens)
 *   bugs revealed by writing tests.
 *
 * 1.5 99/10/14-13:01:39 (cstevens)
 *   Documentation.
 *
 * 1.4 99/10/08-16:56:06 (cstevens)
 *   allow passing the temporary buffer used for copying to the copyTo() method.
 *   Makes it easier to work with the Request.sendResponse(InputStream) method
 *   so that sendResponse can use the server-requested buffer size.
 *
 * 1.3 99/10/07-13:18:20 (cstevens)
 *   Documentation for HttpInputStream
 *
 * 1.2 99/09/15-14:52:08 (cstevens)
 *   import *;
 *
 * 1.2 99/09/15-14:39:36 (Codemgr)
 *   SunPro Code Manager data about conflicts, renames, etc...
 *   Name history : 1 0 util/http/HttpInputStream.java
 *
 * 1.1 99/09/15-14:39:35 (cstevens)
 *   date and time created 99/09/15 14:39:35 by cstevens
 *
 */

package sunlabs.brazil.util.http;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PushbackInputStream;

/**
 * This class is an input stream that provides added methods that are
 * of help when reading the result of an HTTP request.  By setting up
 * this input stream, the user can conveniently read lines of text and
 * copy the contents of an input stream to an output stream.
 * <p>
 * The underlying assumption of this class is that when reading the result
 * of an HTTP request, each byte in the input stream represents an 8-bit
 * ASCII character, and as such, it is perfectly valid to treat each byte
 * as a character.  Locale-based conversion is not appropriate in this
 * circumstance, so the <code>java.io.BufferedReader.readLine</code> method
 * should not be used.
 *
 * @author	Colin Stevens (colin.stevens@sun.com)
 * @version		2.2
 */
public class HttpInputStream
    extends FilterInputStream
{
    /**
     * The default size of the temporary buffer used when copying from an
     * input stream to an output stream.
     *
     * @see	#copyTo(OutputStream, int, byte[])
     */
    public static int defaultBufsize = 4096;

    /**
     * Creates a new HttpInputStream that reads its input from the
     * specified input stream.
     *
     * @param	in
     *		The underlying input stream.
     */
    public
    HttpInputStream(InputStream in)
    {
	super(in);
    }

    /**
     * Reads the next line of text from the input stream.
     * <p>
     * A line is terminated by "\r", "\n", "\r\n", or the end of the input
     * stream.  The line-terminating characters are discarded.
     *
     * @return	The next line from the input stream, or <code>null</code>
     *		if the end of the input stream is reached and no bytes
     *		were found.
     *
     * @throws	IOException if the underlying input stream throws an
     *		IOException while being read.
     */
    public String
    readLine()
	throws IOException
    {
	return readLine(Integer.MAX_VALUE);
    }

    /**
     * Reads the next line of text from the input stream, up to the
     * limit specified.
     * <p>
     * A line is terminated by "\r", "\n", "\r\n", the end of the input
     * stream, or when the specified number of characters have been read.
     * The line-terminating characters are discarded.  It is not possible
     * to distinguish, based on the result, between a line that was
     * exactly <code>limit</code> characters long and a line that was
     * terminated because <code>limit</code> characters were read.
     *
     * @return	The next line from the input stream, or <code>null</code>
     *		if the end of the input stream is reached and no bytes
     *		were found.
     *
     * @throws	IOException if the underlying input stream throws an
     *		IOException while being read.
     */
    public String
    readLine(int limit)
	throws IOException
    {
	StringBuffer sb = new StringBuffer();

	while (limit-- > 0) {
	    int ch = read();
	    if (ch == '\r') {
		ch = read();
		if (ch != '\n') {
		    if ((in instanceof PushbackInputStream) == false) {
			in = new PushbackInputStream(in);
		    }
		    ((PushbackInputStream) in).unread(ch);
		}
		break;
	    } else if (ch == '\n') {
		break;
	    } else if (ch < 0) {
		if (sb.length() == 0) {
		    return null;
		}
		break;
	    } else {
		sb.append((char) ch);
	    }
	}
	return sb.toString();
    }

    /**
     * Reads <code>buf.length</code> bytes from the input stream.  This
     * method reads repeatedly from the input stream until the specified
     * number of bytes have been read or the end of the input stream
     * is reached.
     * <p>
     * The standard <code>InputStream.read</code> method will generally
     * return less than the specified number of bytes if the underlying
     * input stream is "bursty", such as from a network source.  Sometimes
     * it is important to read the exact number of bytes desired.
     *
     * @param	buf
     *		Buffer in which the data is stored.  If buffer is of
     *		length 0, this method will return immediately.
     *
     * @return	The number of bytes read.  This will be less than
     *		<code>buf.length</code> if the end of the input stream was
     *		reached.
     *
     * @throws	IOException if the underlying input stream throws an
     *		IOException while being read.
     */
    public int
    readFully(byte[] buf)
	throws IOException
    {
	return readFully(buf, 0, buf.length);
    }

    /**
     * Reads the specified number of bytes from the input stream.  This
     * method reads repeatedly from the input stream until the specified
     * number of bytes have been read or the end of the input stream is
     * reached.
     * <p>
     * The standard <code>InputStream.read</code> method will generally
     * return less than the specified number of bytes if the underlying
     * input stream is "bursty", such as from a network source.  Sometimes
     * it is important to read the exact number of bytes desired.
     *
     * @param	buf
     *		Buffer in which the data is stored.
     *
     * @param	off
     *		The starting offset into the buffer.
     *
     * @param	len
     *		The number of bytes to read.
     *
     * @return	The number of bytes read.  This will be less than
     *		<code>len</code> if the end of the input stream was reached.
     *
     * @throws	IOException if the underlying input stream throws an
     *		IOException while being read.
     */
    public int
    readFully(byte[] buf, int off, int len)
	throws IOException
    {
	int total = 0;

	while (len > 0) {
	    int count = read(buf, off, len);
	    if (count < 0) {
		break;
	    }
	    total += count;
	    off += count;
	    len -= count;
	}

	return total;
    }

    /**
     * Copies bytes from this input stream to the specified output stream
     * until end of the input stream is reached.
     *
     * @param	out
     *		The output stream to copy the data to.
     *
     * @return	The number of bytes copied to the output stream.
     *
     * @throws	IOException if the underlying input stream throws an
     *		IOException while being read or if the output stream
     *		throws an IOException while being written.  It may not be
     *		possible to distinguish amongst the two conditions.
     */
    public int
    copyTo(OutputStream out)
	throws IOException
    {
	return copyTo(out, -1, null);
    }

    /**
     * Copies bytes from this input stream to the specified output stream
     * until the specified number of bytes are copied or the end of the
     * input stream is reached.
     *
     * @param	out
     *		The output stream to copy the data to.
     *
     * @param	len
     *		The number of bytes to copy, or < 0 to copy until the end
     *		of this stream.
     *
     * @return	The number of bytes copied to the output stream.
     *
     * @throws	IOException if the underlying input stream throws an
     *		IOException while being read or if the output stream
     *		throws an IOException while being written.  It may not be
     *		possible to distinguish amongst the two conditions.
     */
    public int
    copyTo(OutputStream out, int len)
	throws IOException
    {
	return copyTo(out, len, null);
    }

    /**
     * Copies bytes from this input stream to the specified output stream
     * until the specified number of bytes are copied or the end of the
     * input stream is reached.
     *
     * @param	out
     *		The output stream to copy the data to.
     *
     * @param	len
     *		The number of bytes to copy, or < 0 to copy until the end
     *		of this stream.
     *
     * @param	buf
     *		The buffer used to for holding the temporary results while
     *		copying data from this input stream to the output stream.
     *		May be <code>null</code> to allow this method copy in
     *		chunks of length <code>defaultBufsize</code>.
     *
     * @return	The number of bytes read from the input stream, and
     *		copied to the output stream.
     *
     * @throws	IOException if the underlying input stream throws an
     *		IOException while being read.
     */

    public int
    copyTo(OutputStream out, int len, byte[] buf)
	throws IOException
    {
	if (len < 0) {
	    len = Integer.MAX_VALUE;
	}
	if (buf == null) {
	    buf = new byte[Math.min(defaultBufsize, len)];
	}

	int total = 0;
	while (len > 0) {
	    int count = read(buf, 0, Math.min(buf.length, len));
	    if (count < 0) {
		break;
	    }
	    total += count;
	    len -= count;
	    try {
		out.write(buf, 0, count);
		out.flush();
	    } catch (IOException e) {
		break;
	    }
	}
	return total;
    }
}
