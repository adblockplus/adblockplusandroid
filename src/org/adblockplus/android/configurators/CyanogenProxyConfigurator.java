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

import org.adblockplus.android.Utils;
import org.adblockplus.android.compat.CmGlobalProxyManager;
import org.adblockplus.android.compat.ProxyProperties;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

public class CyanogenProxyConfigurator implements ProxyConfigurator
{
  private static final String TAG = Utils.getTag(CyanogenProxyConfigurator.class);

  private final Context context;
  private ProxyProperties proxyProperties = null;
  private final GlobalProxyReceiver globalProxyReceiver;
  private volatile boolean isRegistered = false;

  public CyanogenProxyConfigurator(final Context context)
  {
    this.context = context;
    this.globalProxyReceiver = new GlobalProxyReceiver(this);
  }

  @Override
  public boolean initialize()
  {
    try
    {
      final boolean success = new ProxyProperties("localhost", 8080, "").toCmProxyProperties() != null;

      if (success)
      {
        this.context.registerReceiver(this.globalProxyReceiver, new IntentFilter(CmGlobalProxyManager.GLOBAL_PROXY_STATE_CHANGE_ACTION));
      }

      return success;
    }
    catch (final Throwable t)
    {
      return false;
    }
  }

  @Override
  public boolean registerProxy(final InetAddress address, final int port)
  {
    try
    {
      final CmGlobalProxyManager globalProxyManager = new CmGlobalProxyManager(this.context);
      globalProxyManager.setGlobalProxy(this.proxyProperties = new ProxyProperties(address.getHostName(), port));
      return this.isRegistered = true;
    }
    catch (final Throwable t)
    {
      Log.d(TAG, "Failed to register proxy using CMapi", t);
      return false;
    }
  }

  @Override
  public void unregisterProxy()
  {
    // TODO Do we need to do something here?
  }

  @Override
  public void shutdown()
  {
    this.context.unregisterReceiver(this.globalProxyReceiver);
  }

  @Override
  public ProxyRegistrationType getType()
  {
    return ProxyRegistrationType.CYANOGENMOD;
  }

  @Override
  public boolean isRegistered()
  {
    return this.isRegistered;
  }

  @Override
  public boolean isSticky()
  {
    return true;
  }

  @Override
  public String toString()
  {
    return "[ProxyConfigurator: " + this.getType() + "]";
  }

  private static class GlobalProxyReceiver extends BroadcastReceiver
  {
    private final CyanogenProxyConfigurator cyanogenProxyConfigurator;

    public GlobalProxyReceiver(final CyanogenProxyConfigurator cyanogenProxyConfigurator)
    {
      this.cyanogenProxyConfigurator = cyanogenProxyConfigurator;
    }

    @Override
    public void onReceive(final Context context, final Intent intent)
    {
      if (intent != null && CmGlobalProxyManager.GLOBAL_PROXY_STATE_CHANGE_ACTION.equals(intent.getAction()))
      {
        try
        {
          final CmGlobalProxyManager globalProxyManager = new CmGlobalProxyManager(this.cyanogenProxyConfigurator.context);
          globalProxyManager.setGlobalProxy(this.cyanogenProxyConfigurator.proxyProperties);
        }
        catch (final Throwable t)
        {
          Log.d(TAG, "GlobalProxyReceiver failed to register proxy", t);
        }
      }
    }
  }
}
