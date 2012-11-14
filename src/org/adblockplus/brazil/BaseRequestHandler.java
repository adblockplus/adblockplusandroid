package org.adblockplus.brazil;

import java.util.Properties;

import sunlabs.brazil.server.Handler;
import sunlabs.brazil.server.Server;

public abstract class BaseRequestHandler implements Handler
{

  public static final String PROXY_HOST = "proxyHost";
  public static final String PROXY_PORT = "proxyPort";
  public static final String AUTH = "auth";
  protected String proxyHost;
  protected int proxyPort = 80;
  protected String auth;

  protected String prefix;

  @Override
  public boolean init(Server server, String prefix)
  {
    this.prefix = prefix;

    Properties props = server.props;

    proxyHost = props.getProperty(prefix + PROXY_HOST);

    String s = props.getProperty(prefix + PROXY_PORT);
    try
    {
      proxyPort = Integer.decode(s).intValue();
    }
    catch (Exception e)
    {
    }

    auth = props.getProperty(prefix + AUTH);

    return true;
  }

}
