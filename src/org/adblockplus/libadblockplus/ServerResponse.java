/*
 * This file is part of Adblock Plus <https://adblockplus.org/>,
 * Copyright (C) 2006-2016 Eyeo GmbH
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

package org.adblockplus.libadblockplus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public final class ServerResponse
{
  public static enum NsStatus
  {
    OK(0), ERROR_FAILURE(0x80004005), ERROR_OUT_OF_MEMORY(0x8007000e), ERROR_MALFORMED_URI(0x804b000a), ERROR_CONNECTION_REFUSED(0x804b000d), ERROR_NET_TIMEOUT(
        0x804b000e), ERROR_NO_CONTENT(0x804b0011), ERROR_UNKNOWN_PROTOCOL(0x804b0012), ERROR_NET_RESET(0x804b0014), ERROR_UNKNOWN_HOST(0x804b001e), ERROR_REDIRECT_LOOP(
        0x804b001f), ERROR_UNKNOWN_PROXY_HOST(0x804b002a), ERROR_NET_INTERRUPT(0x804b0047), ERROR_UNKNOWN_PROXY_CONNECTION_REFUSED(0x804b0048), CUSTOM_ERROR_BASE(
        0x80850000), ERROR_NOT_INITIALIZED(0xc1f30001);

    private final long statusCode;
    private final static HashMap<Long, NsStatus> ENUM_MAP = new HashMap<Long, ServerResponse.NsStatus>();

    static
    {
      for (final NsStatus e : NsStatus.values())
      {
        ENUM_MAP.put(e.statusCode, e);
      }
    }

    private NsStatus(final long value)
    {
      this.statusCode = value;
    }

    public long getStatusCode()
    {
      return this.statusCode;
    }

    public static NsStatus fromStatusCode(final long code)
    {
      final NsStatus status = ENUM_MAP.get(code);
      return status != null ? status : ERROR_FAILURE;
    }
  }

  private long status = NsStatus.OK.getStatusCode();
  private int responseStatus = 400;
  private String[] headers = null;
  // TODO: This (and the whole downloading) is a waste of memory, change String
  // to something more suitable
  private String response = null;

  public NsStatus getStatus()
  {
    return NsStatus.fromStatusCode(this.status);
  }

  public void setStatus(final NsStatus status)
  {
    this.status = status.getStatusCode();
  }

  public int getResponseStatus()
  {
    return this.responseStatus;
  }

  public void setResponseStatus(final int status)
  {
    this.responseStatus = status;
  }

  public String getResponse()
  {
    return this.response;
  }

  public void setResponse(final String response)
  {
    this.response = response;
  }

  public List<HeaderEntry> getResponseHeaders()
  {
    final ArrayList<HeaderEntry> ret = new ArrayList<HeaderEntry>();

    if (this.headers != null)
    {
      for (int i = 0; i < this.headers.length; i += 2)
      {
        ret.add(HeaderEntry.of(this.headers[i], this.headers[i + 1]));
      }
    }

    return ret;
  }

  public void setReponseHeaders(final List<HeaderEntry> headers)
  {
    if (headers.isEmpty())
    {
      this.headers = null;
    }
    else
    {
      this.headers = new String[headers.size() * 2];

      int i = 0;
      for (final HeaderEntry e : headers)
      {
        this.headers[i] = e.getKey();
        this.headers[i + 1] = e.getValue();
        i += 2;
      }
    }
  }
}
