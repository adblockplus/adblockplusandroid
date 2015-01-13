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

import java.util.Properties;

import sunlabs.brazil.server.Handler;
import sunlabs.brazil.server.Request;
import sunlabs.brazil.server.Server;
import sunlabs.brazil.util.http.MimeHeaders;

public abstract class BaseRequestHandler implements Handler
{

  public static final String PROXY_HOST = "proxyHost";
  public static final String PROXY_PORT = "proxyPort";
  public static final String AUTH = "auth";
  protected String proxyHost;
  protected int proxyPort = 80;
  protected String auth;
  protected boolean shouldLogHeaders;

  protected String prefix;

  @Override
  public boolean init(final Server server, final String prefix)
  {
    this.prefix = prefix;

    final Properties props = server.props;

    proxyHost = props.getProperty(prefix + PROXY_HOST);

    final String s = props.getProperty(prefix + PROXY_PORT);
    try
    {
      proxyPort = Integer.decode(s).intValue();
    }
    catch (final Exception e)
    {
    }

    auth = props.getProperty(prefix + AUTH);

    shouldLogHeaders = (server.props.getProperty(prefix + "proxylog") != null);

    return true;
  }

  /**
   * Dump the headers on stderr
   */
  public static String dumpHeaders(final int count, final Request request, final MimeHeaders headers, final boolean sent)
  {
    String prompt;
    final StringBuffer sb = new StringBuffer();
    String label = "   " + count;
    label = label.substring(label.length() - 4);
    if (sent)
    {
      prompt = label + "> ";
      sb.append(prompt).append(request.toString()).append("\n");
    }
    else
    {
      prompt = label + "< ";
    }

    for (int i = 0; i < headers.size(); i++)
    {
      sb.append(prompt).append(headers.getKey(i));
      sb.append(": ").append(headers.get(i)).append("\n");
    }
    return (sb.toString());
  }
}
