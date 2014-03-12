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

import org.adblockplus.android.updater.UpdaterActivity;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build.VERSION;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class ABPEngine
{
  private final static String TAG = "ABPEngine";
  private static final int NOTIFICATION_ID = R.string.app_name + 1;

  private Context context;

  public ABPEngine(Context context, String basePath)
  {
    this.context = context;
    String version;
    try
    {
      final PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
      version = info.versionName + "." + info.versionCode;
    } catch (NameNotFoundException e)
    {
      Log.e(TAG, "Failed to get the application version number", e);
      version = "0";
    }
    final String sdkVersion = String.valueOf(VERSION.SDK_INT);
    final String locale = context.getResources().getConfiguration().locale.toString();
    final boolean developmentBuild = !context.getResources().getBoolean(R.bool.def_release);
    initialize(basePath, version, sdkVersion, locale, developmentBuild);
  }

  public void onFilterChanged(String url, String status, long time)
  {
    context.sendBroadcast(new Intent(AdblockPlus.BROADCAST_SUBSCRIPTION_STATUS).putExtra("url", url).putExtra("status", status).putExtra("time", time));
  }
  
  /**
   * Called when update event occurred.
   * @param url Update download address
   */
  public void onUpdateEvent(String url, String error)
  {
    Notification notification = getNotification(url, error);
    NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    notificationManager.notify(NOTIFICATION_ID, notification);
  }
  
  private native void initialize(String basePath, String version, String sdkVersion, String locale, boolean developmentBuild);

  public native void release();
  
  public native boolean isFirstRun();

  public native Subscription[] getListedSubscriptions();

  public native Subscription[] getRecommendedSubscriptions();

  public native void addSubscription(String url);

  public native void removeSubscription(String url);

  public native void refreshSubscription(String url);

  public native void actualizeSubscriptionStatus(String url);
  
  public native void setAcceptableAdsEnabled(boolean enabled);

  public native String getDocumentationLink();

  public native boolean matches(String url, String contentType, String[] documentUrls);

  public native String[] getSelectorsForDomain(String domain);

  public native void checkUpdates();
  
  private Notification getNotification(String url, String error)
  {
    final PendingIntent emptyIntent = PendingIntent.getActivity(context, 0, new Intent(), 0);

    NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
    builder.setContentTitle(context.getText(R.string.app_name));
    builder.setSmallIcon(R.drawable.ic_stat_warning);
    builder.setWhen(System.currentTimeMillis());
    builder.setAutoCancel(true);
    builder.setOnlyAlertOnce(true);
    builder.setContentIntent(emptyIntent);
    
    if (url != null)
    {
      builder.setSmallIcon(R.drawable.ic_stat_download);

      
      Intent intent = new Intent(context, UpdaterActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      intent.setAction("download");
      intent.putExtra("url", url);
      PendingIntent updateIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
      builder.setContentIntent(updateIntent);
      builder.setContentText(context.getString(R.string.msg_update_available));
    }
    else if (error != null)
    {
      //TODO Should we show error message to the user?
      builder.setContentText(context.getString(R.string.msg_update_fail));
    }
    else
    {
      builder.setContentText(context.getString(R.string.msg_update_missing));
    }

    Notification notification = builder.getNotification();
    return notification;
  }

  static
  {
    System.loadLibrary("abpEngine");
  }
}
