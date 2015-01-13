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

package org.adblockplus.android.configurators;

import java.net.InetAddress;

import org.adblockplus.android.compat.CompatibilityException;
import org.adblockplus.android.compat.LinkProperties;
import org.adblockplus.android.compat.ProxyProperties;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Parcelable;

/**
 * Proxy registrator setting native proxy using {@code android.net.LinkProperties} via reflection.
 */
public class NativeProxyConfigurator implements ProxyConfigurator
{
  private final Context context;
  private final WiFiChangeReceiver wiFiChangeReceiver;
  private ProxyProperties proxyProperties;
  private boolean isRegistered = false;

  public NativeProxyConfigurator(final Context context)
  {
    this.context = context;
    this.wiFiChangeReceiver = new WiFiChangeReceiver(this);
  }

  /**
   * Reliably checks for setHttpProxy hack using {@code android.net.wifi.LINK_CONFIGURATION_CHANGED}.
   *
   * @param context
   *          The context used for querying the {@code ConnectivityManager}
   * @return {@code true} if we can set a WiFi proxy using this method
   */
  public static boolean canUse(final Context context)
  {
    try
    {
      final ConnectivityManager conMan = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
      final NetworkInfo ni = conMan.getActiveNetworkInfo();

      final Object lp;

      // Check if we're currently running on WiFi
      if (ni != null && ni.getType() == ConnectivityManager.TYPE_WIFI)
      {
        // We're using reflection directly here to keep this method self-contained.
        lp = conMan.getClass()
            .getMethod("getActiveLinkProperties")
            .invoke(conMan);
      }
      else
      // We're not running on WiFi so get the last used WiFi link properties
      {
        // We're using reflection directly here to keep this method self-contained.
        lp = conMan.getClass()
            .getMethod("getLinkProperties", int.class)
            .invoke(conMan, ConnectivityManager.TYPE_WIFI);
      }

      if (lp == null)
      {
        // Is this even possible?
        throw new IllegalStateException("No WiFi?");
      }

      context.sendBroadcast(
          new Intent("android.net.wifi.LINK_CONFIGURATION_CHANGED")
              .putExtra("linkProperties", (Parcelable) lp));
    }
    catch (final Throwable t)
    {
      return false;
    }

    return true;
  }

  @Override
  public boolean initialize()
  {
    if (canUse(this.context))
    {
      this.context.registerReceiver(this.wiFiChangeReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
      return true;
    }

    return false;
  }

  private boolean sendIntent(final LinkProperties lp, final ProxyProperties proxyProperties)
  {
    final ConnectivityManager conMan = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    final NetworkInfo ni = conMan.getActiveNetworkInfo();

    if (ni != null && ni.getType() == ConnectivityManager.TYPE_WIFI)
    {
      final Intent intent = new Intent("android.net.wifi.LINK_CONFIGURATION_CHANGED");

      if (lp.isValid())
      {
        try
        {
          lp.setHttpProxy(proxyProperties);
          intent.putExtra("linkProperties", (Parcelable) lp.getLinkProperties());
          context.sendBroadcast(intent);
        }
        catch (final Exception e)
        {
          // Catch all, again
          return false;
        }
      }
    }

    return true;
  }

  private boolean registerProxy(final ProxyProperties proxyProperties)
  {
    try
    {
      final ConnectivityManager conMan = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

      return this.sendIntent(LinkProperties.getActiveLinkProperties(conMan), proxyProperties);
    }
    catch (final CompatibilityException e)
    {
      return false;
    }
  }

  private boolean reRegisterProxy()
  {
    return this.registerProxy(this.proxyProperties);
  }

  @Override
  public boolean registerProxy(final InetAddress address, final int port)
  {
    this.proxyProperties = new ProxyProperties(address.getHostName(), port, "");

    return this.isRegistered = this.registerProxy(this.proxyProperties);
  }

  @Override
  public void unregisterProxy()
  {
    this.proxyProperties = null;
    this.isRegistered = false;
    this.registerProxy(this.proxyProperties);
  }

  @Override
  public void shutdown()
  {
    this.context.unregisterReceiver(this.wiFiChangeReceiver);
  }

  @Override
  public ProxyRegistrationType getType()
  {
    return ProxyRegistrationType.NATIVE;
  }

  @Override
  public boolean isRegistered()
  {
    return this.isRegistered;
  }

  @Override
  public boolean isSticky()
  {
    return false;
  }

  @Override
  public String toString()
  {
    return "[ProxyConfigurator: " + this.getType() + "]";
  }

  private final static class WiFiChangeReceiver extends BroadcastReceiver
  {
    private final NativeProxyConfigurator configurator;

    private WiFiChangeReceiver(final NativeProxyConfigurator configurator)
    {
      this.configurator = configurator;
    }

    @Override
    public void onReceive(final Context context, final Intent intent)
    {
      if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction()))
      {
        final ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkInfo ni = connectivityManager.getActiveNetworkInfo();

        if (ni != null && ni.getType() == ConnectivityManager.TYPE_WIFI && ni.isAvailable() && ni.isConnected())
        {
          this.configurator.reRegisterProxy();
        }
      }
    }
  }
}
