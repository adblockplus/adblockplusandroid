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

import java.util.List;

import org.adblockplus.libadblockplus.EventCallback;
import org.adblockplus.libadblockplus.JsValue;
import org.apache.commons.lang.StringUtils;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;

public class AndroidUpdateAvailableCallback extends EventCallback
{
  private final Context context;

  public AndroidUpdateAvailableCallback(final Context context)
  {
    this.context = context;
  }

  @Override
  public void eventCallback(final List<JsValue> params)
  {
    final String updateUrl = params.size() > 0 && !params.get(0).isNull() ? params.get(0).toString() : "";
    if (StringUtils.isNotEmpty(updateUrl))
    {
      final Notification notification = Utils.createUpdateNotification(this.context, updateUrl, null);
      final NotificationManager notificationManager = (NotificationManager)this.context.getSystemService(Context.NOTIFICATION_SERVICE);
      notificationManager.notify(AdblockPlus.UPDATE_NOTIFICATION_ID, notification);
    }
  }
}
