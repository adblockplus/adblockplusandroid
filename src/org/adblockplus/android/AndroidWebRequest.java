/*
 * This file is part of Adblock Plus <http://adblockplus.org/>,
 * Copyright (C) 2006-2014 Eyeo GmbH
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

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import org.adblockplus.libadblockplus.AdblockPlusException;
import org.adblockplus.libadblockplus.HeaderEntry;
import org.adblockplus.libadblockplus.ServerResponse;
import org.adblockplus.libadblockplus.WebRequest;
import org.adblockplus.libadblockplus.ServerResponse.NsStatus;

import android.util.Log;

public class AndroidWebRequest extends WebRequest
{
  public final String TAG = Utils.getTag(WebRequest.class);

  private static final int INITIAL_BUFFER_SIZE = 65536;
  private static final int BUFFER_GROWTH_DELTA = 65536;

  @Override
  public ServerResponse httpGET(final String urlStr, final List<HeaderEntry> headers)
  {
    try
    {
      final URL url = new URL(urlStr);
      Log.d(this.TAG, "Downloading from: " + url);

      final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      connection.setRequestMethod("GET");
      connection.connect();

      final ServerResponse response = new ServerResponse();
      response.setResponseStatus(connection.getResponseCode());

      if (response.getResponseStatus() == 200)
      {
        final InputStream in = connection.getInputStream();

        final byte[] buffer = new byte[4096];

        byte[] out = new byte[INITIAL_BUFFER_SIZE];

        int pos = 0;
        for (;;)
        {
          final int read = in.read(buffer);
          if (read < 0)
          {
            break;
          }
          if (pos + read > out.length)
          {
            final byte[] old = out;
            out = new byte[out.length + BUFFER_GROWTH_DELTA];
            System.arraycopy(old, 0, out, 0, pos);
          }
          System.arraycopy(buffer, 0, out, pos, read);
          pos += read;
        }

        connection.disconnect();

        response.setStatus(NsStatus.OK);
        response.setResponse(new String(out, 0, pos, "utf-8"));
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
