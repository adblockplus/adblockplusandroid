package org.adblockplus.brazil;

import java.io.IOException;

import org.adblockplus.android.AdblockPlus;

import android.util.Log;

import sunlabs.brazil.server.Handler;
import sunlabs.brazil.server.Request;
import sunlabs.brazil.server.Server;

public class RequestHandler implements Handler
{
	private AdblockPlus application;

	String prefix;

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
			block = application.matches(request.url);
		}
		catch (Exception e)
		{
			Log.e(prefix, "Filter error", e);
			return false;
		}
		request.log(Server.LOG_LOG, prefix, block + ": " + request.url);
		if (block)
		{
			request.sendError(403, "Blocked by Adblock Plus");
			return true;
		}

		return false;
	}

}
