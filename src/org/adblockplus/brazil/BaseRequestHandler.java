/*
 * This file is part of the Adblock Plus,
 * Copyright (C) 2006-2012 Eyeo GmbH
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
