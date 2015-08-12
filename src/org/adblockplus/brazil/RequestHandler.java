/*
 * This file is part of Adblock Plus <https://adblockplus.org/>,
 * Copyright (C) 2006-2015 Eyeo GmbH
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

import java.io.EOFException;
import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

import org.adblockplus.ChunkedOutputStream;
import org.adblockplus.android.AdblockPlus;
import org.apache.commons.lang.StringUtils;
import org.literateprograms.BoyerMoore;

import sunlabs.brazil.server.Request;
import sunlabs.brazil.server.Server;
import sunlabs.brazil.util.MatchString;
import sunlabs.brazil.util.http.HttpInputStream;
import sunlabs.brazil.util.http.HttpRequest;
import android.util.Log;

/**
 * The <code>RequestHandler</code> implements a proxy service optionally
 * modifying output.
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
 * <dt>proxylog
 * <dd>If set all http headers will be logged to the console. This is for
 * debugging.
 *
 * </dl>
 *
 * A sample set of configuration parameters illustrating how to use this
 * handler follows:
 *
 * <pre>
 * handler=adblock
 * adblock.class=org.adblockplus.brazil.RequestHandler
 * </pre>
 *
 * See the description under {@link sunlabs.brazil.server.Handler#respond
 * respond} for a more detailed explanation.
 */

public class RequestHandler extends BaseRequestHandler
{
  private AdblockPlus application;
  private String via;
  static final Pattern RE_HTTP = Pattern.compile("^https?:");

  private static final AtomicLong BLOCKED_REQUESTS = new AtomicLong();
  private static final AtomicLong UNBLOCKED_REQUESTS = new AtomicLong();

  public static long getBlockedRequestCount()
  {
    return BLOCKED_REQUESTS.get();
  }

  public static long getUnblockedRequestCount()
  {
    return UNBLOCKED_REQUESTS.get();
  }

  @Override
  public boolean init(final Server server, final String prefix)
  {
    super.init(server, prefix);

    application = AdblockPlus.getApplication();
    via = " " + server.hostName + ":" + server.listen.getLocalPort() + " (" + server.name + ")";

    return true;
  }

  @Override
  public boolean respond(final Request request) throws IOException
  {
    boolean block = false;

    try
    {
      block = application.matches(request.url, request.query, request.getRequestHeader("referer"), request.getRequestHeader("accept"));
    }
    catch (final Exception e)
    {
      Log.e(prefix, "Filter error", e);
    }

    request.log(Server.LOG_LOG, prefix, block + ": " + request.url);

    int count = request.server.requestCount;
    if (shouldLogHeaders)
    {
      // FIXME Don't log to "err"
      System.err.println(dumpHeaders(count, request, request.headers, true));
    }

    if (block)
    {
      request.sendHeaders(204, null, 0);
      BLOCKED_REQUESTS.incrementAndGet();
      return true;
    }

    UNBLOCKED_REQUESTS.incrementAndGet();

    // Do not further process non-http requests
    if (!RE_HTTP.matcher(request.url).find())
    {
      return false;
    }

    String url = request.url;

    if ((request.query != null) && (request.query.length() > 0))
    {
      url += "?" + request.query;
    }

    /*
     * "Proxy-Connection" may be used (instead of just "Connection")
     * to keep alive a connection between a client and this proxy.
     */
    final String pc = request.headers.get("Proxy-Connection");
    if (pc != null)
    {
      request.connectionHeader = "Proxy-Connection";
      request.keepAlive = pc.equalsIgnoreCase("Keep-Alive");
    }

    HttpRequest.removePointToPointHeaders(request.headers, false);

    final HttpRequest target = new HttpRequest(url);
    try
    {
      target.setMethod(request.method);
      request.headers.copyTo(target.requestHeaders);

      if (proxyHost != null)
      {
        target.setProxy(proxyHost, proxyPort);
        if (auth != null)
        {
          target.requestHeaders.add("Proxy-Authorization", auth);
        }
      }

      if (request.postData != null)
      {
        final OutputStream out = target.getOutputStream();
        out.write(request.postData);
        out.close();
      }
      else
      {
        target.setHttpInputStream(request.in);
      }
      target.connect();

      if (shouldLogHeaders)
      {
        System.err.println("      " + target.status + "\n" + dumpHeaders(count, request, target.responseHeaders, false));
      }
      HttpRequest.removePointToPointHeaders(target.responseHeaders, true);

      request.setStatus(target.getResponseCode());
      target.responseHeaders.copyTo(request.responseHeaders);
      try
      {
        request.responseHeaders.add("Via", target.status.substring(0, 8) + via);
      }
      catch (final StringIndexOutOfBoundsException e)
      {
        request.responseHeaders.add("Via", via);
      }

      // Detect if we need to add ElemHide filters
      final String type = request.responseHeaders.get("Content-Type");

      String[] selectors = null;
      if (type != null && type.toLowerCase().startsWith("text/html"))
      {
        String reqHost = "";

        try
        {
          reqHost = (new URL(request.url)).getHost();
        }
        catch (final MalformedURLException e)
        {
          // We are transparent, it's not our deal if it's malformed.
        }

        selectors = application.getSelectorsForDomain(reqHost);
      }
      // If no filters are applicable just pass through the response
      if (selectors == null || target.getResponseCode() != 200)
      {
        final int contentLength = target.getContentLength();
        if (contentLength == 0)
        {
          // we do not use request.sendResponse to avoid arbitrary
          // 200 -> 204 response code conversion
          request.sendHeaders(-1, null, -1);
        }
        else
        {
          request.sendResponse(target.getInputStream(), contentLength, null, -1);
        }
      }
      // Insert filters otherwise
      else
      {
        final HttpInputStream his = target.getInputStream();
        int size = target.getContentLength();
        if (size < 0)
        {
          size = Integer.MAX_VALUE;
        }

        FilterInputStream in = null;
        FilterOutputStream out = null;

        // Detect if content needs decoding
        String encodingHeader = request.responseHeaders.get("Content-Encoding");
        if (encodingHeader != null)
        {
          encodingHeader = encodingHeader.toLowerCase();
          if (encodingHeader.equals("gzip") || encodingHeader.equals("x-gzip"))
          {
            in = new GZIPInputStream(his);
          }
          else if (encodingHeader.equals("compress") || encodingHeader.equals("x-compress"))
          {
            in = new InflaterInputStream(his);
          }
          else
          {
            // Unsupported encoding, proxy content as-is
            in = his;
            out = request.out;
            selectors = null;
          }
        }
        else
        {
          in = his;
        }
        // Use chunked encoding when injecting filters in page
        if (out == null)
        {
          request.responseHeaders.remove("Content-Length");
          request.responseHeaders.remove("Content-Encoding");
          out = new ChunkedOutputStream(request.out);
          request.responseHeaders.add("Transfer-Encoding", "chunked");
          size = Integer.MAX_VALUE;
        }

        String charsetName = "utf-8";
        final String contentType = request.responseHeaders.get("Content-Type");
        if (contentType != null)
        {
          final Matcher matcher = Pattern.compile("charset=([^;]*)").matcher(contentType);
          if (matcher.matches())
          {
            try
            {
              final String extractedCharsetName = matcher.group(0);
              Charset.forName(extractedCharsetName);
              charsetName = extractedCharsetName;
            }
            catch (final IllegalArgumentException e)
            {
              Log.e(prefix, "Unsupported site charset, falling back to " + charsetName, e);
            }
          }
        }

        request.sendHeaders(-1, null, -1);

        final byte[] buf = new byte[Math.min(4096, size)];

        boolean sent = selectors == null;
        final BoyerMoore matcher = new BoyerMoore("<html".getBytes());

        while (size > 0)
        {
          out.flush();

          count = in.read(buf, 0, Math.min(buf.length, size));
          if (count < 0)
          {
            break;
          }
          size -= count;
          try
          {
            // Search for <html> tag
            if (!sent && count > 0)
            {
              final List<Integer> matches = matcher.match(buf, 0, count);
              if (!matches.isEmpty())
              {
                // Add filters right before match
                final int m = matches.get(0);
                out.write(buf, 0, m);
                out.write("<style type=\"text/css\">\n".getBytes());
                out.write(StringUtils.join(selectors, ",\r\n").getBytes(charsetName));
                out.write("{ display: none !important }</style>\n".getBytes());
                out.write(buf, m, count - m);
                sent = true;
                continue;
              }
            }
            out.write(buf, 0, count);
          }
          catch (final IOException e)
          {
            break;
          }
        }
        // The correct way would be to close ChunkedOutputStream
        // but we can not do it because underlying output stream is
        // used later in caller code. So we use this ugly hack:
        if (out instanceof ChunkedOutputStream)
          ((ChunkedOutputStream) out).writeFinalChunk();
      }
    }
    catch (final InterruptedIOException e)
    {
      /*
       * Read timeout while reading from the remote side. We use a
       * read timeout in case the target never responds.
       */
      request.sendError(408, "Timeout / No response");
    }
    catch (final EOFException e)
    {
      request.sendError(500, "No response");
    }
    catch (final UnknownHostException e)
    {
      request.sendError(500, "Unknown host");
    }
    catch (final ConnectException e)
    {
      request.sendError(500, "Connection refused");
    }
    catch (final IOException e)
    {
      /*
       * An IOException will happen if we can't communicate with the
       * target or the client. Rather than attempting to discriminate,
       * just send an error message to the client, and let the send
       * fail if the client was the one that was in error.
       */

      String msg = "Error from proxy";
      if (e.getMessage() != null)
      {
        msg += ": " + e.getMessage();
      }
      request.sendError(500, msg);
      Log.e(prefix, msg, e);
    }
    finally
    {
      target.close();
    }
    return true;
  }
}
