package org.adblockplus.brazil;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.adblockplus.android.AdblockPlus;

import sunlabs.brazil.server.Handler;
import sunlabs.brazil.server.Request;
import sunlabs.brazil.server.Server;
import android.util.Log;

public class RequestHandler implements Handler
{
	private AdblockPlus application;
	private String prefix;

	@Override
	public boolean init(Server server, String prefix)
	{
		this.prefix = prefix;
		application = AdblockPlus.getApplication();
		return true;
	}

	@Override
	public boolean respond(Request request) throws IOException
	{
		boolean block = false;
		try
		{
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
			block = application.matches(request.url, request.query, reqHost, refHost, request.getRequestHeader("accept"));
		}
		catch (Exception e)
		{
			Log.e(prefix, "Filter error", e);
			return false;
		}
		request.log(Server.LOG_LOG, prefix, request.query);
		request.log(Server.LOG_LOG, prefix, block + ": " + request.url);
		if (block)
		{
			request.sendError(403, "Blocked by Adblock Plus");
			return true;
		}

		return false;
	}

}
