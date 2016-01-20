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

package org.adblockplus.android;

import org.adblockplus.libadblockplus.Notification;
import org.adblockplus.libadblockplus.ShowNotificationCallback;

import android.app.NotificationManager;
import android.content.Context;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class AndroidShowNotificationCallback extends ShowNotificationCallback
{
  private static final String TAG = Utils.getTag(AndroidShowNotificationCallback.class);

  private final Context context;

  public AndroidShowNotificationCallback(final Context context)
  {
    this.context = context;
  }

  @Override
  public void showNotificationCallback(final Notification notification)
  {
    if (notification != null)
    {
      Log.i(TAG, "Received notification: " + notification);

      final NotificationManager notificationManager = (NotificationManager) this.context
          .getSystemService(Context.NOTIFICATION_SERVICE);

      notificationManager.notify(AdblockPlus.SERVER_NOTIFICATION_ID,
          new NotificationCompat.Builder(this.context.getApplicationContext())
              .setSmallIcon(R.drawable.ic_stat_blocking)
              .setContentTitle(notification.getTitle())
              .setContentText(notification.getMessageString())
              .getNotification());
    }
  }
}
