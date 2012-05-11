package org.adblockplus.brazil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.adblockplus.android.AdblockPlus;
import org.paw.util.Pack;

import sunlabs.brazil.server.ChainHandler;
import sunlabs.brazil.server.Handler;
import sunlabs.brazil.server.Request;
import sunlabs.brazil.server.Server;
import sunlabs.brazil.util.MatchString;
import sunlabs.brazil.util.http.MimeHeaders;
import android.util.Log;

/**
 * The <code>RequestHandler</code> captures the output of another 
 * <code>Handler</code> and modifies output.
 * The following configuration parameters are used to initialize this
 * <code>Handler</code>: <dl class=props>
 *
 * <dt>prefix, suffix, glob, match
 * <dd>Specify the URL that triggers this handler.
 * (See {@link MatchString}).
 *
 * <dt> <code>handler</code>
 * <dd> The name of the <code>Handler</code> whose output will be captured
 *	and then filtered.  This is called the "wrapped handler".  
 * </dl>
 *
 * A sample set of configuration parameters illustrating how to use this
 * handler follows:
 * <pre>
 * handler=adblock
 * adblock.class=org.adblockplus.brazil.RequestHandler
 * adblock.handler=proxy
 * proxy.class=sunlabs.brazil.proxy.ProxyHandler
 * </pre>
 * See the description under
 * {@link sunlabs.brazil.server.Handler#respond responnd}
 * for a more detailed explaination.
 */

public class RequestHandler implements Handler
{
	private static final String HANDLER = "handler";
	
	private AdblockPlus application;
	private String prefix;
	public Handler handler;

	@Override
	public boolean init(Server server, String prefix)
	{
		this.prefix = prefix;
		application = AdblockPlus.getApplication();

		String str = server.props.getProperty(prefix + HANDLER, "");
		handler = ChainHandler.initHandler(server, prefix + HANDLER + ".", str);
		if (handler == null)
			return false;
		else
			return true;
	}

	@Override
	public boolean respond(Request request) throws IOException
	{
		boolean block = false;
		String reqHost = null;
		String refHost = null;
		try
		{
			reqHost = (new URL(request.url)).getHost();
			refHost = (new URL(request.getRequestHeader("referer"))).getHost();
		}
		catch (MalformedURLException e)
		{
		}
		try
		{
			block = application.matches(request.url, request.query, reqHost, refHost, request.getRequestHeader("accept"));
		}
		catch (Exception e)
		{
			Log.e(prefix, "Filter error", e);
			return handler.respond(request);
		}
		request.log(Server.LOG_LOG, prefix, block + ": " + request.url);
		if (block)
		{
			request.sendError(403, "Blocked by Adblock Plus");
			return true;
		}

		FilterStream out = new FilterStream(request.out);
		out.reqHost = reqHost;
		request.out = out;
		try
		{
			if (handler.respond(request) == false)
			{
				return false;
			}
			if (out.shouldFilter)
				return out.applyFilters(request);
			else
				return true;
		}
		finally
		{
			out.restore(request);
		}
	}

	private class FilterStream extends Request.HttpOutputStream
	{
		Request.HttpOutputStream old;
		String selectors;
		String reqHost;
		boolean shouldFilter = false;

		public FilterStream(Request.HttpOutputStream old)
		{
			super(new ByteArrayOutputStream());
			this.old = old;
		}

		public void sendHeaders(Request request) throws IOException
		{
			String type = request.responseHeaders.get("Content-Type");

			selectors = null;
			if (type != null && type.toLowerCase().startsWith("text/html"))
			{
				selectors = application.getSelectorsForDomain(reqHost);
			}
			if (selectors == null)
			{
				restore(request);
				old.sendHeaders(request);
			}
			else
			{
				shouldFilter = true;
			}
		}

		public boolean applyFilters(Request request) throws IOException
		{
			request.out.flush();
			restore(request);
			
			byte[] content = ((ByteArrayOutputStream) out).toByteArray();
			
			String encodingHeader = request.responseHeaders.get("Content-Encoding");
			if (encodingHeader != null)
			{
				encodingHeader = encodingHeader.toLowerCase();
				if (encodingHeader.equals("gzip") || encodingHeader.equals("x-gzip"))
				{
					content = Pack.unzipData(content);
					request.responseHeaders.remove("Content-Encoding");
				}
				else if (encodingHeader.equals("compress") || encodingHeader.equals("x-compress"))
				{
					content = Pack.deCompressData(content);
					request.responseHeaders.remove("Content-Encoding");
				}
				else
				{
					selectors = null;
				}
			}
			
			boolean chunked = false;
			encodingHeader = request.responseHeaders.get("Transfer-Encoding");
			if (encodingHeader != null)
			{
				if (encodingHeader.toLowerCase().equals("chunked"))
					chunked = true;
				else
					selectors = null;
			}
			
			if (selectors != null && request.getStatus() == 200)
			{
				//TODO Do we need to set encoding here?
				byte[] addon = selectors.getBytes();

				if (chunked)
				{
					String chunkHeader = Integer.toHexString(addon.length) + "\r\n";
					byte[] chunk = chunkHeader.getBytes();
					byte[] newaddon = new byte[addon.length + chunk.length + 2];
					System.arraycopy(chunk, 0, newaddon, 0, chunk.length);
					System.arraycopy(addon, 0, newaddon, chunk.length, addon.length);
					newaddon[newaddon.length - 2] = (byte) "\r".charAt(0);
					newaddon[newaddon.length - 1] = (byte) "\n".charAt(0);
					addon = newaddon;
				}
				
				byte[] newcontent = new byte[content.length + addon.length];
				System.arraycopy(addon, 0, newcontent, 0, addon.length);
				System.arraycopy(content, 0, newcontent, addon.length, content.length);
				content = newcontent;
			}

			String s = new String(content);
			Log.e(prefix, request.url);
			for (int i = 0; i < request.responseHeaders.size(); i++)
			{
			    Log.i(request.responseHeaders.getKey(i), request.responseHeaders.get(i));
			}
			Log.w(prefix, s);
			request.sendResponse(content, null);
			return true;
		}

		public void restore(Request request)
		{
			request.out = old;
		}
    }
}
