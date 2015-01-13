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

public class LinkProperties
{
  private final Object linkProperties;

  public LinkProperties(final Object linkProperties)
  {
    this.linkProperties = linkProperties;
  }

  public boolean isValid()
  {
    return this.linkProperties != null;
  }

  public static LinkProperties getActiveLinkProperties(final ConnectivityManager manager) throws CompatibilityException
  {
    try
    {
      return new LinkProperties(
          manager.getClass()
              .getMethod("getActiveLinkProperties")
              .invoke(manager));
    }
    catch (final Throwable t)
    {
      throw new CompatibilityException(t);
    }
  }

  public static LinkProperties getLinkProperties(final ConnectivityManager manager, final int networkType) throws CompatibilityException
  {
    try
    {
      return new LinkProperties(
          manager.getClass()
              .getMethod("getLinkProperties", int.class)
              .invoke(manager, networkType));
    }
    catch (final Throwable t)
    {
      throw new CompatibilityException(t);
    }
  }

  public static LinkProperties fromContext(final Context context)
  {
    try
    {
      return getActiveLinkProperties(((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE)));
    }
    catch (final CompatibilityException e)
    {
      return null;
    }
  }

  public Object getLinkProperties()
  {
    return this.linkProperties;
  }

  public String getInterfaceName() throws CompatibilityException
  {
    try
    {
      return (String) Class.forName("android.net.LinkProperties")
          .getMethod("getInterfaceName")
          .invoke(this.linkProperties);
    }
    catch (final Throwable t)
    {
      throw new CompatibilityException(t);
    }
  }

  public void setHttpProxy(final ProxyProperties proxyProperties) throws CompatibilityException
  {
    try
    {
      Class.forName("android.net.LinkProperties")
          .getMethod("setHttpProxy", Class.forName("android.net.ProxyProperties"))
          .invoke(this.linkProperties, proxyProperties != null ? proxyProperties.toAndroidNetProxyProperties() : null);
    }
    catch (final CompatibilityException e)
    {
      throw e;
    }
    catch (final Throwable t)
    {
      throw new CompatibilityException(t);
    }
  }

  public ProxyProperties getHttpProxy() throws CompatibilityException
  {
    try
    {
      return ProxyProperties.fromObject(
          Class.forName("android.net.LinkProperties")
              .getMethod("getHttpProxy")
              .invoke(this.linkProperties));
    }
    catch (final CompatibilityException e)
    {
      throw e;
    }
    catch (final Throwable t)
    {
      throw new CompatibilityException(t);
    }
  }

  @Override
  public String toString()
  {
    return this.isValid() ? this.linkProperties.toString() : null;
  }
}
