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

import org.adblockplus.libadblockplus.UpdateAvailableCallback;

import android.content.Context;

public class AndroidUpdateAvailableCallback extends UpdateAvailableCallback
{
  private final Context context;

  public AndroidUpdateAvailableCallback(final Context context)
  {
    this.context = context;
  }

  @Override
  public void updateAvailableCallback(String url)
  {
    Utils.showUpdateNotification(this.context, url, null);
  }
}
