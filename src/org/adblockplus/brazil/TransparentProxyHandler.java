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
