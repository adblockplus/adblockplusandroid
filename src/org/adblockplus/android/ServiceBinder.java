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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

public class ServiceBinder
{
  private final Context context;
  private volatile ProxyService proxyService;
  private final ProxyServiceConnection proxyServiceConnection;
  private OnConnectHandler onConnectHandler = null;

  public ServiceBinder(final Context context)
  {
    this.context = context;
    this.proxyServiceConnection = new ProxyServiceConnection(this);
  }

  public ServiceBinder setOnConnectHandler(final OnConnectHandler handler)
  {
    this.onConnectHandler = handler;
    return this;
  }

  public synchronized void bind()
  {
    if (this.proxyService == null)
    {
      this.context.bindService(new Intent(this.context, ProxyService.class), this.proxyServiceConnection, 0);
    }
  }

  public ProxyService get()
  {
    return this.proxyService;
  }

  public void unbind()
  {
    this.context.unbindService(this.proxyServiceConnection);
    this.proxyService = null;
  }

  private static class ProxyServiceConnection implements ServiceConnection
  {
    private final ServiceBinder binder;

    private ProxyServiceConnection(final ServiceBinder binder)
    {
      this.binder = binder;
    }

    @Override
    public void onServiceConnected(final ComponentName name, final IBinder service)
    {
      final ProxyService proxyService = ((ProxyService.LocalBinder) service).getService();
      final OnConnectHandler handler = this.binder.onConnectHandler;

      this.binder.proxyService = proxyService;

      if (handler != null)
      {
        handler.onConnect(proxyService);
      }
    }

    @Override
    public void onServiceDisconnected(final ComponentName name)
    {
      this.binder.proxyService = null;
    }
  }

  public interface OnConnectHandler
  {
    public void onConnect(final ProxyService proxyService);
  }
}
