package org.adblockplus.brazil;

import java.io.IOException;

import sunlabs.brazil.server.Handler;
import sunlabs.brazil.server.Request;
import sunlabs.brazil.server.Server;

/**
 * Reconstructs request url to comply with proxy specification if transparent
 * proxy is used.
 */
public class TransparentProxyHandler implements Handler
{

  @Override
  public boolean init(Server server, String prefix)
  {
    return true;
  }

  @Override
  public boolean respond(Request request) throws IOException
  {
    if (!request.url.contains("://"))
    {
      request.url = "http://" + request.headers.get("host") + request.url;
    }
    return false;
  }

}
