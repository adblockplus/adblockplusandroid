/*
 * This file is part of Adblock Plus <http://adblockplus.org/>,
 * Copyright (C) 2006-2013 Eyeo GmbH
 *
 * Adblock Plus is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3 as
 * published by the Free Software Foundation.
 *
 * Adblock Plus is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Adblock Plus.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.adblockplus.brazil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

import sunlabs.brazil.server.Request;
import sunlabs.brazil.server.Server;
import sunlabs.brazil.util.MatchString;

/**
 * <code>RequestHandler</code> implements a SSL tunnel.
 *
 * The following configuration parameters are used to initialize this
 * <code>Handler</code>:
 * <dl class=props>
 *
 * <dt>prefix, suffix, glob, match
 * <dd>Specify the URL that triggers this handler. (See {@link MatchString}).
 * <dt>auth
 * <dd>The value of the proxy-authenticate header (if any) sent to the upstream
 * proxy
 * <dt>proxyHost
 * <dd>If specified, the name of the upstream proxy
 * <dt>proxyPort
 * <dd>The upstream proxy port, if a proxyHost is specified (defaults to 80)
 *
 * </dl>
 *
 * A sample set of configuration parameters illustrating how to use this
 * handler follows:
 *
 * <pre>
 * handler=https
 * https.class=org.adblockplus.brazil.SSLConnectionHandler
 * </pre>
 *
 * See the description under {@link sunlabs.brazil.server.Handler#respond
 * respond} for a more detailed explanation.
 *
 * Original source by Jochen Luell, PAW (http://paw-project.sourceforge.net/)
 */

public class SSLConnectionHandler extends BaseRequestHandler
{
  @Override
  public boolean respond(Request request) throws IOException
  {
    if (!request.method.equals("CONNECT"))
      return false;

    request.log(Server.LOG_LOG, prefix, "SSL connection to " + request.url);

    String host = null;
    int port = 0;

    Socket serverSocket;
    try
    {
      if (proxyHost != null)
      {
        host = proxyHost;
        port = proxyPort;
        if (auth != null)
        {
          request.headers.add("Proxy-Authorization", auth);
        }
      }
      else
      {
        int c = request.url.indexOf(':');
        host = request.url.substring(0, c);
        port = Integer.parseInt(request.url.substring(c + 1));
      }

      // Connect to server or upstream proxy
      serverSocket = new Socket();
      serverSocket.setKeepAlive(true);
      serverSocket.connect(new InetSocketAddress(host, port));
    }
    catch (Exception e)
    {
      request.sendError(500, "SSL connection failure");
      return true;
    }

    try
    {
      if (proxyHost != null)
      {
        // Forward request to upstream proxy
        OutputStream out = serverSocket.getOutputStream();
        out.write((request.method + " " + request.url + " " + request.protocol + "\r\n").getBytes());
        request.headers.print(out);
        out.write("\r\n".getBytes());
        out.flush();
      }
      else
      {
        // Send response to client
        OutputStream out = request.sock.getOutputStream();
        out.write((request.protocol + " 200 Connection established\r\n\r\n").getBytes());
        out.flush();
      }

      // Start bi-directional data transfer
      ConnectionHandler client = new ConnectionHandler(request.sock, serverSocket);
      ConnectionHandler server = new ConnectionHandler(serverSocket, request.sock);
      client.start();
      server.start();

      // Wait for connections to close
      client.join();
      server.join();
    }
    catch (InterruptedException e)
    {
      request.log(Server.LOG_ERROR, prefix, "Data exchange error: " + e.getMessage());
    }

    // Close connection
    serverSocket.close();
    request.log(Server.LOG_LOG, prefix, "SSL connection closed");

    return true;
  }

  private class ConnectionHandler extends Thread
  {
    private InputStream in;
    private OutputStream out;

    ConnectionHandler(Socket sin, Socket sout) throws IOException
    {
      in = sin.getInputStream();
      out = sout.getOutputStream();
    }

    @Override
    public void run()
    {
      byte[] buf = new byte[4096];
      int count;

      try
      {
        while ((count = in.read(buf, 0, buf.length)) != -1)
        {
          out.write(buf, 0, count);
        }
        out.flush();
      }
      catch (IOException e)
      {
        e.printStackTrace();
      }
    }
  }
}
