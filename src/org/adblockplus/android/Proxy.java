package org.adblockplus.android;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.net.Socket;

import android.util.Log;

public class Proxy implements Runnable
{
	private static final String TAG = "Proxy";

	private static final String HTTP_BLOCK = "HTTP/1.1 403 Blocked by Adblock Plus";
	private static final String HTTP_ERROR = "HTTP/1.1 500 Error by Adblock Plus";
	private static final String HTTP_LINE_SEPARATOR = "\r\n";

	private static final int PORT_HTTP = 80;
	private static final int PORT_HTTPS = 443;
	
	private static final int socketTimeout = 10000;

	private final Socket client;

	public Proxy(final Socket socket)
	{
		this.client = socket;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void run()
	{
		try
		{
			BufferedInputStream clientIn = new BufferedInputStream(client.getInputStream());
			BufferedOutputStream clientOut = new BufferedOutputStream(client.getOutputStream());
	
			StringBuffer url = new StringBuffer();
			byte[] request = getHTTPData(clientIn, url, false);
			
			String hostName = "";
			int hostPort = PORT_HTTP;
	
			// extract host name from url
			String uri = url.toString();
			int pos = uri.indexOf("://");
			if (pos >= 0)
			{
				int end = uri.indexOf("/", pos + 4);
				if (end < 0)
					end = uri.length();
				hostName = uri.substring(pos + 3, end);
			}
			
			pos = hostName.indexOf(":");
			if (pos > 0)
			{
				try
				{
					hostPort = Integer.parseInt(hostName.substring(pos + 1));
				}
				catch (Exception e)
				{
				}
				hostName = hostName.substring(0, pos);
			}
	
			boolean block = false;
			try
			{
				block = AdblockPlus.getApplication().matches(uri);
			}
			catch (Exception e)
			{
				Log.e(TAG, "Filter error", e);
				clientOut.write(HTTP_ERROR.getBytes());
				clientOut.write(HTTP_LINE_SEPARATOR.getBytes());
				clientOut.write(HTTP_LINE_SEPARATOR.getBytes());
				clientOut.flush();
			}
			Log.w(TAG, "url (" + block + "): " + uri);
	
			if (block)
			{
				clientOut.write(HTTP_BLOCK.getBytes());
				clientOut.write(HTTP_LINE_SEPARATOR.getBytes());
				clientOut.write(HTTP_LINE_SEPARATOR.getBytes());
				clientOut.flush();
			}
			else
			{
				Socket server = new Socket(hostName, hostPort);
				
				if (server != null)
				{
					server.setSoTimeout(socketTimeout);
					BufferedInputStream serverIn = new BufferedInputStream(server.getInputStream());
					BufferedOutputStream serverOut = new BufferedOutputStream(server.getOutputStream());
					
					// send the request out
					serverOut.write(request, 0, Array.getLength(request));
					serverOut.flush();
					
					streamHTTPData(serverIn, clientOut, true);
					
					serverIn.close();
					serverOut.close();
				}
			}
			
			clientOut.close();
			clientIn.close();
			client.close();
		}
		catch (IOException e)
		{
			Log.e(TAG, "Proxy error", e);
		}
/*		
			try
			{
				lWriter.append(HTTP_ERROR + " - " + e.toString() + HTTP_RESPONSE + e.toString());
				lWriter.flush();
				lWriter.close();
				this.local.close();
			}
			catch (IOException e1)
			{
				Log.e(TAG, null, e1);
			}
			*/
	}

	private byte[] getHTTPData(InputStream in, StringBuffer url, boolean waitForDisconnect)
	{
		ByteArrayOutputStream bs = new ByteArrayOutputStream();
		streamHTTPData(in, bs, url, waitForDisconnect);
		return bs.toByteArray();
	}

	private void streamHTTPData(InputStream in, OutputStream out, boolean waitForDisconnect)
	{
		streamHTTPData(in, out, null, waitForDisconnect);
	}

	private void streamHTTPData(InputStream in, OutputStream out, StringBuffer url, boolean waitForDisconnect)
	{
		StringBuffer header = new StringBuffer();
		String data;
		int pos;
		int contentLength = -1;
		int byteCount = 0;
		boolean hostKnown = false;
		boolean hostHeader = false;

		// get the first line of the header
		data = readLine(in);
		if (data != null)
		{
			pos = data.indexOf(" ");
			if ((data.toUpperCase().startsWith("HTTP/")) && (pos > 0))
			{
				// this is a response, do nothing
				header.append(data + HTTP_LINE_SEPARATOR);
				hostHeader = true;
			}
			else if (url != null)
			{
				// this is a request, extract host if present
				pos = data.indexOf(" ");
				if (pos >= 0)
				{
					String uri = data.substring(pos + 1, data.indexOf(" ", pos + 1));
					url.setLength(0);
					url.append(uri);
					pos = uri.indexOf("://");
					if (pos >= 0)
					{
						data = data.replace(uri.substring(0, uri.indexOf("/", pos + 4)), "");
						hostKnown = true;
					}
				}
				header.append(data + HTTP_LINE_SEPARATOR);
			}
		}

		// get the rest of the header info
		while ((data = readLine(in)) != null)
		{
			// the header ends at the first blank line
			if (data.length() == 0)
				break;
			
			header.append(data + HTTP_LINE_SEPARATOR);

			// check for the Host header
			pos = data.toLowerCase().indexOf("host:");
			if (pos >= 0)
			{
				hostHeader = true;
				if (! hostKnown)
				{
					String host = data.substring(pos + 5).trim();
					url.insert(0, "http://" + host);
				}
			}

			// check for the Content-Length header
			pos = data.toLowerCase().indexOf("content-length:");
			if (pos >= 0)
				contentLength = Integer.parseInt(data.substring(pos + 15).trim());
		}
		
		if (! hostHeader && url != null)
		{
			String uri = url.toString();
			pos = uri.indexOf("://");
			if (pos >= 0)
			{
				String host = uri.substring(pos + 3, uri.indexOf("/", pos + 4));
				header.append("Host: " + host + HTTP_LINE_SEPARATOR);
			}
		}

		//Log.i(TAG, header.toString());
		// add a blank line to terminate the header info
		header.append(HTTP_LINE_SEPARATOR);

		try
		{
			// convert the header to a byte array, and write it to our stream
			out.write(header.toString().getBytes(), 0, header.length());

			// if the header indicated that this was not a 200 response,
			// just return what we've got if there is no Content-Length,
			// because we may not be getting anything else
			if (contentLength == 0)
			{
				out.flush();
				return;
			}
		}
		catch (IOException e)
		{
			Log.e(TAG, "Error sending HTTP headers", e);
			return;
		}

		// get the body, if any; we try to use the Content-Length header to
		// determine how much data we're supposed to be getting, because
		// sometimes the client/server won't disconnect after sending us
		// information...
		if (contentLength > 0)
			waitForDisconnect = false;

		if ((contentLength > 0) || (waitForDisconnect))
		{
			try
			{
				byte[] buf = new byte[4096];
				int bytesIn = 0;
				while (((byteCount < contentLength) || (waitForDisconnect)) && ((bytesIn = in.read(buf)) >= 0))
				{
					out.write(buf, 0, bytesIn);
					byteCount += bytesIn;
				}
			}
			catch (Exception e)
			{
				Log.e(TAG, "Error sending HTTP body", e);
			}
		}

		// flush the OutputStream and return
		try
		{
			out.flush();
		}
		catch (Exception e)
		{
		}
	}

	private String readLine(InputStream in)
	{
		// reads a line of text from an InputStream
		StringBuffer data = new StringBuffer();
		int c;

		try
		{
			// if we have nothing to read, just return null
			in.mark(1);
			if (in.read() == -1)
				return null;
			else
				in.reset();

			while ((c = in.read()) >= 0)
			{
				// check for an end-of-line character
				if ((c == 0) || (c == 10) || (c == 13))
					break;
				else
					data.append((char) c);
			}

			// deal with the case where the end-of-line terminator is \r\n
			if (c == 13)
			{
				in.mark(1);
				if (in.read() != 10)
					in.reset();
			}
		}
		catch (Exception e)
		{
			Log.e(TAG, "Error getting header", e);
		}

		// and return what we have
		return data.toString();
	}
}
