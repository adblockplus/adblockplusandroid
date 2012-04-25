package org.adblockplus.brazil;

import java.io.IOException;
import java.net.URL;
import java.util.regex.Pattern;

import org.adblockplus.android.AdblockPlus;

import com.google.common.net.InternetDomainName;

import android.util.Log;

import sunlabs.brazil.server.Handler;
import sunlabs.brazil.server.Request;
import sunlabs.brazil.server.Server;

public class RequestHandler implements Handler
{
	private static final Pattern imagePattern = Pattern.compile("\\.(?:gif|png|jpe?g|bmp|ico)$");

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
		boolean thirdParty = false;
		String contentType = null;
		try
		{
			String mime = request.getRequestHeader("accept");
			if (mime != null)
			{
				if (mime.contains("text/css"))
					contentType = "STYLESHEET";
				else if (mime.contains("image/*"))
					contentType = "IMAGE";
			}
			
			if (contentType == null)
			{
				String lurl = request.url.toLowerCase();
				if (lurl.endsWith(".js"))
					contentType = "SCRIPT";
				else if (lurl.endsWith(".css"))
					contentType = "STYLESHIT";
				else if (imagePattern.matcher(lurl).find())
					contentType = "IMAGE";
			}
			if (contentType == null)
				contentType = "OTHER";

			String referrer = request.getRequestHeader("referer");
			if (referrer != null)
			{
				URL reqUrl = new URL(request.url);
				URL refUrl = new URL(referrer);
				InternetDomainName requestDomain = InternetDomainName.from(reqUrl.getHost());
				InternetDomainName referrerDomain = InternetDomainName.from(refUrl.getHost());
				
				if (requestDomain.hasPublicSuffix() && referrerDomain.hasPublicSuffix())
				{
					thirdParty = ! requestDomain.topPrivateDomain().equals(referrerDomain.topPrivateDomain());
				}
				else
				{
					thirdParty = true;
				}
			}
			block = application.matches(request.url, contentType, thirdParty);
		}
		catch (Exception e)
		{
			Log.e(prefix, "Filter error", e);
			return false;
		}
		request.log(Server.LOG_LOG, prefix, block + ": " + request.url + ", " + contentType + ", " + thirdParty);
		if (block)
		{
			request.sendError(403, "Blocked by Adblock Plus");
			return true;
		}

		return false;
	}

}
