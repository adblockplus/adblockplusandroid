package org.paw.handler;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Properties;

import sunlabs.brazil.server.Handler;
import sunlabs.brazil.server.Request;
import sunlabs.brazil.server.Server;

/**
 * Handler for implementing a SSL tunnel as described in the INTERNET-DRAFT
 * "Tunneling TCP based protocols through Web proxy servers". A copy can be
 * found here:
 * http://tools.ietf.org/id/draft-luotonen-web-proxy-tunneling-01.txt
 * 
 * <p>
 * Properties:
 * <dl class=props>
 * <dt>auth
 * <dd>The value of the proxy-authenticate header (if any) sent to the upstream
 * proxy
 * <dt>proxyHost
 * <dd>If specified, the name of the upstream proxy
 * <dt>proxyPort
 * <dd>The up stream proxys port, if a proxyHost is specified (defaults to
 * 8080)
 * </dl>
 * 
 * @author Jochen Luell
 * @version 0.1, 05-Feb-2008
 */
public class SSLConnectionHandler implements Handler {
	private static final String PROXY_HOST = "proxyHost";

	private static final String PROXY_PORT = "proxyPort";

	private static final String AUTH = "auth";

	private String proxyHost = null;

	private int proxyPort = 8080;

	private String auth = null;

	private String prefix;

	private Object waitForThread = new Object();

	/*
	 * (non-Javadoc)
	 * 
	 * @see sunlabs.brazil.server.Handler#init(sunlabs.brazil.server.Server,
	 *      java.lang.String)
	 */
	public boolean init(Server server, String prefix) {
		this.prefix = prefix;
		// this.server = server;

		Properties props = server.props;

		proxyHost = props.getProperty(prefix + PROXY_HOST);
		String str = props.getProperty(prefix + PROXY_PORT);
		try {
			proxyPort = Integer.decode(str).intValue();
		} catch (Exception e) {
		}
		;
		auth = props.getProperty(prefix + AUTH);

		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see sunlabs.brazil.server.Handler#respond(sunlabs.brazil.server.Request)
	 */
	public boolean respond(Request request) throws IOException {
		String host;
		int port;

		/*
		 * Check if method specified in the request is the CONNECT method
		 */
		if (request.method.equals("CONNECT")) {

			/*
			 * If an upstream proxy is specify use the proxy host/port. If no
			 * proxy is specified host/port are extracted from the url. Format
			 * is <hostname>:<port>
			 */
			if (proxyHost != null) {
				if (auth != null) {
					request.headers.add("Proxy-Authorization", auth);
				}
				host = proxyHost;
				port = proxyPort;
			} else {
				try {
					int colon = request.url.indexOf(":");
					host = request.url.substring(0, colon);
					port = Integer.parseInt(request.url.substring(colon + 1));
				} catch (NumberFormatException e1) {
					return false;
				}
			}

			/*
			 * Connect to the server or proxy
			 */
			Socket sslServerSocket;
			try {
				InetAddress addr = InetAddress.getByName(host);
				sslServerSocket = new Socket(addr, port);
			} catch (Exception e) {
				return false;
			}
			OutputStream sslServerOut = sslServerSocket.getOutputStream();

			/*
			 * If an upstream proxy is given, pass the CONNECT string to the
			 * upstream proxy. If no proxy is specified send a HTTP 200 message
			 * to the client.
			 */
			if (proxyHost != null) {
				sslServerOut.write((request.method + " " + request.url + " "
						+ request.protocol + "\r\n").getBytes());
				request.headers.print(sslServerOut);
				sslServerOut.write("\r\n".getBytes());
			} else {
				
				request.sock.getOutputStream().write(
						(request.protocol + " 200 Connection established\r\n")
								.getBytes());
				request.sock.getOutputStream().write(("Proxy-agent: " + request.server.name + "\r\n").getBytes());
				request.sock.getOutputStream().write("\r\n".getBytes());
			}

			/*
			 * Start two threads. One for the outgoing and one for incoming and
			 * one for outgoing messages.
			 */
			SSLConnection outThread = new SSLConnection(request.sock,
					sslServerSocket);
			SSLConnection inThread = new SSLConnection(sslServerSocket,
					request.sock);
			outThread.start();
			inThread.start();

			/*
			 * Wait for connection close.
			 */

			synchronized (waitForThread) {
				try {
					waitForThread.wait();
				} catch (InterruptedException e) {
				}
			}

			outThread.setTerminated(true);
			inThread.setTerminated(true);

			/*
			 * Close the SSL socket
			 */
			sslServerSocket.close();

			/*
			 * Write LOG message
			 */
			request.log(Server.LOG_LOG, prefix, "SSL connection close");

			return true;
		}

		return false;
	}

	/**
	 * Class representing one direction of the SSL connection.
	 * 
	 */
	class SSLConnection extends Thread {
		private Socket sockIn;

		private Socket sockOut;

		private boolean isConnected = true;

		private boolean isTerminated;

		private int BUFFER = 1024;

		/**
		 * Constructor
		 * 
		 * @param sockIn
		 * @param sockOut
		 */
		public SSLConnection(Socket sockIn, Socket sockOut) {
			this.sockIn = sockIn;
			this.sockOut = sockOut;
		}

		/**
		 * Sets the thread sate to terminated
		 * 
		 * @param b
		 *            Specifies if the thread should be terminated
		 */
		public void setTerminated(boolean b) {
			isTerminated = b;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Thread#run()
		 */
		public void run() {
			byte[] b = new byte[BUFFER];
			try {
				int c;
				while (!isTerminated
						&& (c = sockIn.getInputStream().read(b)) != -1) {
					sockOut.getOutputStream().write(b, 0, c);
				}
			} catch (IOException e) {
			}
			isConnected = false;

			synchronized (waitForThread) {
				waitForThread.notifyAll();
			}

		}

		/**
		 * Returns the connection state of the thread
		 * 
		 * @return
		 */
		boolean isConnected() {
			return isConnected;
		}
	}
}
