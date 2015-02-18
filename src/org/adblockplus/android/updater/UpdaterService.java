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

package org.adblockplus.android.updater;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

import org.adblockplus.android.AdblockPlus;
import org.adblockplus.android.R;
import org.adblockplus.android.Utils;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

/**
 * Update downloader.
 */
public class UpdaterService extends Service
{
  private static final String TAG = Utils.getTag(UpdaterService.class);

  private File updateDir;

  @Override
  public void onCreate()
  {
    super.onCreate();
    // Use common Android path for downloads
    updateDir = new File(Environment.getExternalStorageDirectory().getPath(), "downloads");
  }

  @Override
  public void onStart(final Intent intent, final int startId)
  {
    super.onStart(intent, startId);

    // Stop if media not available
    if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()))
    {
      stopSelf();
      return;
    }
    updateDir.mkdirs();

    // Start download
    if (intent != null && intent.hasExtra("url"))
      new DownloadTask(this).execute(intent.getStringExtra("url"));
  }

  @Override
  public IBinder onBind(final Intent intent)
  {
    return null;
  }

  private class DownloadTask extends AsyncTask<String, Integer, String>
  {
    private final Context context;
    private final Notification notification;
    private final PendingIntent contentIntent;
    private final NotificationManager notificationManager;

    public DownloadTask(final Context context)
    {
      this.context = context;
      notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
      notification = new Notification();
      contentIntent = PendingIntent.getActivity(context, 0, new Intent(), PendingIntent.FLAG_UPDATE_CURRENT);
    }

    @Override
    protected void onPreExecute()
    {
      notification.flags |= Notification.FLAG_ONGOING_EVENT;
      notification.when = 0;
      notification.icon = R.drawable.ic_stat_download;
      notification.setLatestEventInfo(context, getString(R.string.app_name), String.format(getString(R.string.msg_update_downloading), 0), contentIntent);
      notificationManager.notify(AdblockPlus.UPDATE_NOTIFICATION_ID, notification);
    }

    @Override
    protected String doInBackground(final String... sUrl)
    {
      try
      {
        // Create connection
        final URL url = new URL(sUrl[0]);
        Log.e(TAG, "D: " + sUrl[0]);
        final URLConnection connection = url.openConnection();
        connection.connect();
        final int fileLength = connection.getContentLength();
        Log.e(TAG, "S: " + fileLength);

        // Check if file already exists
        final File updateFile = new File(updateDir, "AdblockPlus-update.apk");
        if (updateFile.exists())
        {
          // if (updateFile.length() == fileLength)
          // return updateFile.getAbsolutePath();
          // else
          updateFile.delete();
        }

        // Download the file
        final InputStream input = new BufferedInputStream(url.openStream());
        final OutputStream output = new FileOutputStream(updateFile);

        final byte data[] = new byte[1024];
        long total = 0;
        int count;
        int progress = 0;
        while ((count = input.read(data)) != -1)
        {
          total += count;
          output.write(data, 0, count);

          final int p = (int) (total * 100 / fileLength);
          if (p != progress)
          {
            publishProgress(p);
            progress = p;
          }
        }

        output.flush();
        output.close();
        input.close();
        return updateFile.getAbsolutePath();
      }
      catch (final Exception e)
      {
        Log.e(TAG, "Download error", e);
        return null;
      }
    }

    @Override
    protected void onProgressUpdate(final Integer... progress)
    {
      notification.setLatestEventInfo(context, getString(R.string.app_name), String.format(getString(R.string.msg_update_downloading), progress[0]), contentIntent);
      notificationManager.notify(AdblockPlus.UPDATE_NOTIFICATION_ID, notification);
    }

    @Override
    protected void onPostExecute(final String result)
    {
      notificationManager.cancel(AdblockPlus.UPDATE_NOTIFICATION_ID);
      if (result != null)
      {
        final Notification notification = new Notification();
        notification.icon = R.drawable.ic_stat_download;
        notification.when = System.currentTimeMillis();
        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        final Intent intent = new Intent(context, UpdaterActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setAction("update");
        intent.putExtra("path", result);
        final PendingIntent contentIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        notification.setLatestEventInfo(context, context.getText(R.string.app_name), context.getString(R.string.msg_update_ready), contentIntent);
        notificationManager.notify(AdblockPlus.UPDATE_NOTIFICATION_ID, notification);
      }
    }
  }
}
