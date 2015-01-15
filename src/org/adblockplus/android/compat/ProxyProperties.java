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

package org.adblockplus.android.compat;

import android.content.Context;
import android.net.ConnectivityManager;

/**
 * Compatibility wrapper for various {@code ProxyProperties}.
 */
public class ProxyProperties
{
  private final static String DEFAULT_EXCL_LIST = null;

  private final String host;
  private final int port;
  private final String exclusionList;

  public ProxyProperties(final String host, final int port, final String excl)
  {
    this.host = host;
    this.port = port;
    this.exclusionList = excl;
  }

  public ProxyProperties(final String host, final int port)
  {
    this(host, port, DEFAULT_EXCL_LIST);
  }

  public String getHost()
  {
    return this.host;
  }

  public int getPort()
  {
    return this.port;
  }

  public String getExclusionList()
  {
    return this.exclusionList;
  }

  public Object toAndroidNetProxyProperties() throws CompatibilityException
  {
    try
    {
      return Class.forName("android.net.ProxyProperties")
          .getConstructor(String.class, int.class, String.class)
          .newInstance(this.host, Integer.valueOf(this.port), this.exclusionList);
    }
    catch (final Throwable t)
    {
      throw new CompatibilityException(t);
    }
  }

  public Object toCmProxyProperties() throws CompatibilityException
  {
    try
    {
      return Class.forName("org.cyanogenmod.support.proxy.CmProxyProperties")
          .getConstructor(String.class, int.class, String.class)
          .newInstance(this.host, Integer.valueOf(this.port), this.exclusionList);
    }
    catch (final Throwable t)
    {
      throw new CompatibilityException(t);
    }
  }

  public static ProxyProperties fromObject(final Object proxyProperties) throws CompatibilityException
  {
    if (proxyProperties == null)
    {
      return null;
    }

    try
    {
      final Class<?> clazz = proxyProperties.getClass();

      final String host = (String) clazz.getMethod("getHost").invoke(proxyProperties);
      final int port = ((Number) clazz.getMethod("getPort").invoke(proxyProperties)).intValue();
      final String exlc = (String) clazz.getMethod("getExclusionList").invoke(proxyProperties);

      return new ProxyProperties(host, port, exlc);
    }
    catch (final Throwable t)
    {
      throw new CompatibilityException(t);
    }
  }

  public static ProxyProperties fromConnectivityManager(final ConnectivityManager manager) throws CompatibilityException
  {
    try
    {
      return fromObject(manager.getClass()
          .getMethod("getProxy")
          .invoke(manager));
    }
    catch (final Throwable t)
    {
      throw new CompatibilityException(t);
    }
  }

  public static ProxyProperties fromContext(final Context context)
  {
    try
    {
      return fromConnectivityManager((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE));
    }
    catch (final CompatibilityException e)
    {
      return null;
    }
  }

  @Override
  public String toString()
  {
    // Copied from android.net.ProxyProperties
    final StringBuilder sb = new StringBuilder();

    if (this.host != null)
    {
      sb.append("[");
      sb.append(this.host);
      sb.append("] ");
      sb.append(Integer.toString(this.port));
      if (this.exclusionList != null)
      {
        sb.append(" xl=").append(this.exclusionList);
      }
    }
    else
    {
      sb.append("[ProxyProperties.mHost == null]");
    }

    return sb.toString();
  }
}
