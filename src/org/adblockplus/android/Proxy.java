package org.adblockplus.android;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;

import android.util.Log;

public class Proxy implements Runnable
{
	private static final String TAG = "Proxy";

	/** HTTP Response: blocked. */
	private static final String HTTP_BLOCK = "HTTP/1.1 500 blocked by Adblock Plus";
	/** HTTP Response: error. */
	private static final String HTTP_ERROR = "HTTP/1.1 500 error by Adblock Plus";
	/** HTTP Response: connected. */
	private static final String HTTP_CONNECTED = "HTTP/1.1 200 connected";
	/** HTTP Response: flush. */
	private static final String HTTP_RESPONSE = "\n\n";

	/** Default Port for HTTP. */
	private static final int PORT_HTTP = 80;
	/** Default Port for HTTPS. */
	private static final int PORT_HTTPS = 443;
	
	/** Proxy's filter. */
	private ArrayList<String> filter = new ArrayList<String>();

	// cache object.refs
	// no private object.refs accessed by inner classes
	// TODO: reduce object creation

	/** Local Socket. */
	private final Socket local;
	/** Remote Socket. */
	private Socket remote;

	/** State: normal. */
	private static final short STATE_NORMAL = 0;
	/** State: closed by local side. */
	private static final short STATE_CLOSED_IN = 1;
	/** State: closed by remote side. */
	private static final short STATE_CLOSED_OUT = 2;
	/** Connections state. */
	private short state = STATE_NORMAL;

	/**
	 * CopyStream reads one stream and writes it's data into an other stream.
	 * Run this as a Thread.
	 * 
	 * @author Felix Bechstein
	 */
	private class CopyStream implements Runnable
	{
		/** Reader. */
		private final InputStream reader;
		/** Writer. */
		private final OutputStream writer;

		/** Size of buffer. */
		private static final int BUFFSIZE = 32768;
		/** Size of header buffer. */
		private static final int HEADERBUFFSIZE = 1024;

		/**
		 * Constructor.
		 * 
		 * @param r
		 *            reader
		 * @param w
		 *            writer
		 */
		public CopyStream(final InputStream r, final OutputStream w)
		{
			this.reader = new BufferedInputStream(r, BUFFSIZE);
			this.writer = w;
		}

		/**
		 * Run by Thread.start().
		 */
		@Override
		public void run()
		{
			byte[] buf = new byte[BUFFSIZE];
			int read = 0;
			final InputStream r = this.reader;
			final OutputStream w = this.writer;
			try
			{
				while (true)
				{
					read = r.available();
					if (read < 1 || read > BUFFSIZE)
					{
						read = BUFFSIZE;
					}
					read = r.read(buf, 0, read);
					if (read < 0)
					{
						break;
					}
					w.write(buf, 0, read);
					if (r.available() < 1)
					{
						w.flush();
					}
				}
				Proxy.this.close(Proxy.STATE_CLOSED_OUT);
				// this.writer.close();
			}
			catch (IOException e)
			{
				// FIXME: java.net.SocketException: Broken pipe
				// no idea, what causes this :/
				// Connection c = Connection.this;
				// String s = new String(buf, 0, read);
				Log.e(TAG, null, e);
			}
		}
	}

	/**
	 * Constructor.
	 * 
	 * @param socket
	 *            local Socket
	 */
	public Proxy(final Socket socket)
	{
		this.local = socket;
	}

	/**
	 * Check if URL is blocked.
	 * 
	 * @param url
	 *            URL
	 * @return if URL is blocked?
	 */
	private boolean checkURL(final String url)
	{
		final ArrayList<String> f = Proxy.this.filter;
		final int s = f.size();
		for (int i = 0; i < s; i++)
		{
			if (url.indexOf(f.get(i)) >= 0)
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * Read in HTTP Header. Parse for URL to connect to.
	 * 
	 * @param reader
	 *            buffer reader from which we read the header
	 * @param buffer
	 *            buffer into which the header is written
	 * @return URL to which we should connect, port other than 80 is given
	 *         explicitly
	 * @throws IOException
	 *             inner IOException
	 */
	private URL readHeader(final BufferedInputStream reader, final StringBuilder buffer) throws IOException
	{
		URL ret = null;
		String[] strings;
		int avail;
		byte[] buf = new byte[CopyStream.HEADERBUFFSIZE];
		// read first line
		if (this.state == STATE_CLOSED_OUT)
		{
			return null;
		}
		avail = reader.available();
		if (avail > CopyStream.HEADERBUFFSIZE)
		{
			avail = CopyStream.HEADERBUFFSIZE;
		}
		else if (avail == 0)
		{
			avail = CopyStream.HEADERBUFFSIZE;
		}
		avail = reader.read(buf, 0, avail);
		if (avail < 1)
		{
			return null;
		}
		String line = new String(buf, 0, avail);
		String testLine = line;
		int i = line.indexOf(" http://");
		if (i > 0)
		{
			// remove "http://host:port" from line
			int j = line.indexOf('/', i + 9);
			if (j > i)
			{
				testLine = line.substring(0, i + 1) + line.substring(j);
			}
		}
		buffer.append(testLine);
		strings = line.split(" ");
		if (strings.length > 1)
		{
			if (strings[0].equals("CONNECT"))
			{
				String targetHost = strings[1];
				int targetPort = PORT_HTTPS;
				strings = targetHost.split(":");
				if (strings.length > 1)
				{
					targetPort = Integer.parseInt(strings[1]);
					targetHost = strings[0];
				}
				ret = new URL("https://" + targetHost + ":" + targetPort);
			}
			else if (strings[0].equals("GET") || strings[0].equals("POST"))
			{
				String path = null;
				if (strings[1].startsWith("http://"))
				{
					ret = new URL(strings[1]);
					path = ret.getPath();
				}
				else
				{
					path = strings[1];
				}
				// read header
				String lastLine = line;
				do
				{
					testLine = lastLine + line;
					i = testLine.indexOf("\nHost: ");
					if (i >= 0)
					{
						int j = testLine.indexOf("\n", i + 6);
						if (j > 0)
						{
							String tHost = testLine.substring(i + 6, j).trim();
							ret = new URL("http://" + tHost + path);
							break;
						}
						else
						{
							// test for "Host:" again with longer buffer
							line = lastLine + line;
						}
					}
					if (line.indexOf("\r\n\r\n") >= 0)
					{
						break;
					}
					lastLine = line;
					avail = reader.available();
					if (avail > 0)
					{
						if (avail > CopyStream.HEADERBUFFSIZE)
						{
							avail = CopyStream.HEADERBUFFSIZE;
						}
						avail = reader.read(buf, 0, avail);
						// FIXME: this may break
						line = new String(buf, 0, avail);
						buffer.append(line);
					}
				}
				while (avail > 0);
			}
			else
			{
				Log.d(TAG, "unknown method: " + strings[0]);
			}
		}
		strings = null;

		// copy rest of reader's buffer
		avail = reader.available();
		while (avail > 0)
		{
			if (avail > CopyStream.HEADERBUFFSIZE)
			{
				avail = CopyStream.HEADERBUFFSIZE;
			}
			avail = reader.read(buf, 0, avail);
			// FIXME: this may break!
			buffer.append(new String(buf, 0, avail));
			avail = reader.available();
		}
		return ret;
	}

	/**
	 * Close local and remote socket.
	 * 
	 * @param nextState
	 *            state to go to
	 * @return new state
	 * @throws IOException
	 *             IOException
	 */
	private synchronized short close(final short nextState) throws IOException
	{
		Log.d(TAG, "close(" + nextState + ")");
		short mState = this.state;
		if (mState == STATE_NORMAL || nextState == STATE_NORMAL)
		{
			mState = nextState;
		}
		Socket mSocket;
		if (mState != STATE_NORMAL)
		{
			// close remote socket
			mSocket = this.remote;
			if (mSocket != null && mSocket.isConnected())
			{
				try
				{
					mSocket.shutdownInput();
					mSocket.shutdownOutput();
				}
				catch (IOException e)
				{
					Log.d(TAG, null, e);
				}
				mSocket.close();
			}
			this.remote = null;
		}
		if (mState == STATE_CLOSED_OUT)
		{
			// close local socket
			mSocket = this.local;
			if (mSocket.isConnected())
			{
				try
				{
					mSocket.shutdownOutput();
					mSocket.shutdownInput();
				}
				catch (IOException e)
				{
					Log.d(TAG, null, e);
				}
				mSocket.close();
			}
		}
		this.state = mState;
		return mState;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void run()
	{
		BufferedInputStream lInStream;
		OutputStream lOutStream;
		BufferedWriter lWriter;
		try
		{
			lInStream = new BufferedInputStream(this.local.getInputStream(), CopyStream.BUFFSIZE);
			lOutStream = this.local.getOutputStream();
			lWriter = new BufferedWriter(new OutputStreamWriter(lOutStream), // .
			CopyStream.BUFFSIZE);
		}
		catch (IOException e)
		{
			Log.e(TAG, null, e);
			return;
		}
		try
		{
			InputStream rInStream = null;
			OutputStream rOutStream = null;
			BufferedWriter remoteWriter = null;
			Thread rThread = null;
			StringBuilder buffer = new StringBuilder();
			boolean block = false;
			String tHost = null;
			int tPort = -1;
			URL url;
			boolean connectSSL = false;
			while (this.local.isConnected())
			{
				buffer = new StringBuilder();
				url = this.readHeader(lInStream, buffer);
				if (buffer.length() == 0)
				{
					break;
				}
				if (this.local.isConnected() && rThread != null && !rThread.isAlive())
				{
					// socket should be closed already..
					Log.d(TAG, "close dead remote");
					if (connectSSL)
					{
						this.local.close();
					}
					tHost = null;
					rInStream = null;
					rOutStream = null;
					rThread = null;
				}
				if (url != null)
				{
					block = this.checkURL(url.toString());
					Log.d(TAG, "new url: " + url.toString());
					if (!block)
					{
						// new connection needed?
						int p = url.getPort();
						if (p < 0)
						{
							p = PORT_HTTP;
						}
						if (tHost == null || !tHost.equals(url.getHost()) || tPort != p)
						{
							// create new connection
							Log.d(TAG, "shutdown old remote");
							this.close(STATE_CLOSED_IN);
							if (rThread != null)
							{
								rThread.join();
								rThread = null;
							}

							tHost = url.getHost();
							tPort = p;
							Log.d(TAG, "new socket: " + url.toString());
							this.state = STATE_NORMAL;
							this.remote = new Socket();
							this.remote.connect(new InetSocketAddress(tHost, tPort));
							rInStream = this.remote.getInputStream();
							rOutStream = this.remote.getOutputStream();
							rThread = new Thread(new CopyStream(rInStream, lOutStream));
							rThread.start();
							if (url.getProtocol().startsWith("https"))
							{
								connectSSL = true;
								lWriter.write(HTTP_CONNECTED + HTTP_RESPONSE);
								lWriter.flush();
								// copy local to remote by blocks
								Thread t2 = new Thread(new CopyStream(lInStream, rOutStream));

								t2.start();
								remoteWriter = null;
								break; // copy in separate thread. break
								// while here
							}
							else
							{
								remoteWriter = new BufferedWriter(new OutputStreamWriter(rOutStream), CopyStream.BUFFSIZE);
							}
						}
					}
				}
				// push data to remote if not blocked
				if (block)
				{
					lWriter.append(HTTP_BLOCK + HTTP_RESPONSE + "BLOCKED by AdBlock!");
					lWriter.flush();
				}
				else
				{
					Socket mSocket = this.remote;
					if (mSocket != null && mSocket.isConnected() && remoteWriter != null)
					{
						try
						{
							remoteWriter.append(buffer);
							remoteWriter.flush();
						}
						catch (IOException e)
						{
							Log.d(TAG, buffer.toString(), e);
						}
					}
				}
			}
			if (rThread != null && rThread.isAlive())
			{
				rThread.join();
			}
		}
		catch (InterruptedException e)
		{
			Log.e(TAG, null, e);
		}
		catch (IOException e)
		{
			Log.e(TAG, null, e);
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
		}
		Log.d(TAG, "close connection");
	}
}
