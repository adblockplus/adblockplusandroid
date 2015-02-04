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

package org.adblockplus.android;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashSet;
import java.util.List;

import org.adblockplus.libadblockplus.AdblockPlusException;
import org.adblockplus.libadblockplus.FilterEngine;
import org.adblockplus.libadblockplus.HeaderEntry;
import org.adblockplus.libadblockplus.ServerResponse;
import org.adblockplus.libadblockplus.ServerResponse.NsStatus;
import org.adblockplus.libadblockplus.WebRequest;

import android.util.Log;

public class AndroidWebRequest extends WebRequest
{
  public final static String TAG = Utils.getTag(WebRequest.class);

  private final HashSet<String> subscriptionURLs = new HashSet<String>();

  private boolean isListedSubscriptionUrl(final URL url)
  {
    String toCheck = url.toString();

    final int idx = toCheck.indexOf('?');
    if (idx != -1)
    {
      toCheck = toCheck.substring(0, idx);
    }

    return this.subscriptionURLs.contains(toCheck);
  }

  protected void updateSubscriptionURLs(final FilterEngine engine)
  {
    for (final org.adblockplus.libadblockplus.Subscription s : engine.fetchAvailableSubscriptions())
    {
      this.subscriptionURLs.add(s.getProperty("url").toString());
    }
    this.subscriptionURLs.add(engine.getPref("subscriptions_exceptionsurl").toString());
  }

  @Override
  public ServerResponse httpGET(final String urlStr, final List<HeaderEntry> headers)
  {
    try
    {
      final URL url = new URL(urlStr);
      Log.d(TAG, "Downloading from: " + url);

      final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      connection.setRequestMethod("GET");
      connection.connect();

      final ServerResponse response = new ServerResponse();
      response.setResponseStatus(connection.getResponseCode());

      if (response.getResponseStatus() == 200)
      {
        final BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
        final StringBuilder sb = new StringBuilder();

        if (isListedSubscriptionUrl(url))
        {
          Log.d(TAG, "Removing element hiding rules from: '" + url + "'");

          String line;
          while ((line = reader.readLine()) != null)
          {
            // We're only appending non-element-hiding filters here.
            //
            // See:
            //      https://issues.adblockplus.org/ticket/303
            //
            // Follow-up issue for removing this hack:
            //      https://issues.adblockplus.org/ticket/1541
            //
            if (line.indexOf('#') == -1)
            {
              sb.append(line);
              sb.append('\n');
            }
          }
        }
        else
        {
          int character;

          while ((character = reader.read()) != -1)
          {
            sb.append((char) character);
          }
        }

        connection.disconnect();

        response.setStatus(NsStatus.OK);
        response.setResponse(sb.toString());
      }
      else
      {
        response.setStatus(NsStatus.ERROR_FAILURE);
      }
      return response;
    }
    catch (final Throwable t)
    {
      throw new AdblockPlusException("WebRequest failed", t);
    }
  }
}
