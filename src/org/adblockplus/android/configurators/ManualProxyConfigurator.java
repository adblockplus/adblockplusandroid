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
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReentrantLock;

import org.adblockplus.android.ConfigurationActivity;
import org.adblockplus.android.ProxyService;
import org.adblockplus.android.R;
import org.adblockplus.android.compat.ProxyProperties;
import org.adblockplus.brazil.RequestHandler;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.support.v4.app.NotificationCompat;

/**
 * A dummy registrator only holding callbacks and checks.
 */
public class ManualProxyConfigurator implements ProxyConfigurator
{
  final Context context;
  private ProxyProperties proxyProperties = null;
  private static final int NOTRAFFIC_NOTIFICATION_ID = R.string.proxysettings_name;
  private static final int NO_TRAFFIC_TIMEOUT = 5 * 60 * 1000; // 5 minutes
  private volatile boolean isRegistered = false;
  private NoTrafficWorker noTrafficWorker = null;
  private final ReentrantLock noTrafficAccessLock = new ReentrantLock();
  private final Handler uiHandler;

  public ManualProxyConfigurator(final Context context)
  {
    this.uiHandler = new Handler();
    this.context = context;
  }

  @Override
  public boolean initialize()
  {
    return true;
  }

  @Override
  public boolean registerProxy(final InetAddress address, final int port)
  {
    this.proxyProperties = new ProxyProperties(address.getHostName(), port, "");
    this.startNoTrafficCheck();
    return true;
  }

  @Override
  public void unregisterProxy()
  {
    this.isRegistered = false;
    this.abortNoTrafficCheck();
  }

  @Override
  public void shutdown()
  {
    this.removeErrorNotification();
  }

  @Override
  public ProxyRegistrationType getType()
  {
    return ProxyRegistrationType.MANUAL;
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

  private void removeErrorNotification()
  {
    final NotificationManager notificationManager =
        (NotificationManager) this.context.getSystemService(Context.NOTIFICATION_SERVICE);
    notificationManager.cancel(NOTRAFFIC_NOTIFICATION_ID);
  }

  private void startNoTrafficCheck()
  {
    this.noTrafficAccessLock.lock();
    try
    {
      if (this.noTrafficWorker == null)
      {
        this.noTrafficWorker = new NoTrafficWorker(this);
        final Thread t = new Thread(this.noTrafficWorker);
        t.setDaemon(true);
        t.start();
      }
    }
    finally
    {
      this.noTrafficAccessLock.unlock();
    }
  }

  private void abortNoTrafficCheck()
  {
    this.noTrafficAccessLock.lock();
    try
    {
      if (this.noTrafficWorker != null)
      {
        this.noTrafficWorker.stop();
      }
    }
    finally
    {
      this.noTrafficWorker = null;
      this.noTrafficAccessLock.unlock();
    }
  }

  private synchronized void trafficReceived()
  {
    this.isRegistered = true;
    this.abortNoTrafficCheck();

    this.uiHandler.post(new Runnable()
    {
      @Override
      public void run()
      {
        ManualProxyConfigurator.this.removeErrorNotification();
        ManualProxyConfigurator.this.context.sendBroadcast(new Intent(ProxyService.PROXY_STATE_CHANGED_ACTION));
      }
    });
  }

  private synchronized void noTrafficReceived()
  {
    this.isRegistered = false;
    this.abortNoTrafficCheck();

    this.uiHandler.post(new Runnable()
    {
      @Override
      public void run()
      {
        final Context context = ManualProxyConfigurator.this.context;
        // Show warning notification
        final Intent intent =
            new Intent(context, ConfigurationActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra("port", ManualProxyConfigurator.this.proxyProperties.getPort());

        final PendingIntent contentIntent =
            PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        final NotificationCompat.Builder builder =
            new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.ic_stat_warning)
                .setWhen(System.currentTimeMillis())
                .setAutoCancel(true)
                .setContentIntent(contentIntent)
                .setContentTitle(context.getText(R.string.app_name))
                .setContentText(context.getText(R.string.notif_notraffic));

        final NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTRAFFIC_NOTIFICATION_ID, builder.getNotification());

        context.sendBroadcast(new Intent(ProxyService.PROXY_STATE_CHANGED_ACTION));
      }
    });
  }

  private static class NoTrafficWorker implements Runnable
  {
    private volatile boolean running = true;
    private final Semaphore finished = new Semaphore(1);
    private final ManualProxyConfigurator manualProxyConfigurator;

    public NoTrafficWorker(final ManualProxyConfigurator manualProxyConfigurator)
    {
      this.manualProxyConfigurator = manualProxyConfigurator;
      this.finished.acquireUninterruptibly();
    }

    @Override
    public void run()
    {
      try
      {
        final long endTime = System.currentTimeMillis() + NO_TRAFFIC_TIMEOUT;

        final long blockedStart = RequestHandler.getBlockedRequestCount();
        final long unblockedStart = RequestHandler.getUnblockedRequestCount();

        while (this.running)
        {
          try
          {
            if (System.currentTimeMillis() >= endTime)
            {
              this.running = false;
              this.manualProxyConfigurator.noTrafficReceived();
              break;
            }

            if (RequestHandler.getBlockedRequestCount() != blockedStart
                || RequestHandler.getUnblockedRequestCount() != unblockedStart)
            {
              this.running = false;
              this.manualProxyConfigurator.trafficReceived();
              break;
            }

            Thread.sleep(100);
          }
          catch (final Throwable t)
          {
            // Swallow everything to keep this thread alive at all cost
          }
        }
      }
      finally
      {
        this.finished.release();
      }
    }

    public synchronized void stop()
    {
      if (this.running)
      {
        this.running = false;
        this.finished.acquireUninterruptibly();
      }
    }
  }
}
