/*
 * This file is part of the Adblock Plus,
 * Copyright (C) 2006-2012 Eyeo GmbH
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

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Locale;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.adblockplus.android.AdblockPlus;
import org.adblockplus.android.R;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.util.Log;

/**
 * Processes Alarm event to check for update availability.
 */
public class AlarmReceiver extends BroadcastReceiver
{

  private static final String TAG = "AlarmReceiver";
  private static final int NOTIFICATION_ID = R.string.app_name + 1;

  @Override
  public void onReceive(final Context context, final Intent intent)
  {
    Log.i(TAG, "Alarm; requesting updater service");

    final AdblockPlus application = AdblockPlus.getApplication();

    // Indicates manual (immediate) update check which requires response to user.
    final boolean notify = intent.getBooleanExtra("notifynoupdate", false);

    // Check network availability
    boolean connected = false;
    ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    NetworkInfo networkInfo = null;
    if (connectivityManager != null)
    {
      networkInfo = connectivityManager.getActiveNetworkInfo();
      connected = networkInfo == null ? false : networkInfo.isConnected();
    }

    // Prepare notification
    final NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    final Notification notification = new Notification();
    notification.icon = R.drawable.ic_stat_warning;
    notification.when = System.currentTimeMillis();
    notification.flags |= Notification.FLAG_AUTO_CANCEL | Notification.FLAG_ONLY_ALERT_ONCE;
    if (notify)
      notification.flags |= Notification.DEFAULT_SOUND;
    final PendingIntent emptyIntent = PendingIntent.getActivity(context, 0, new Intent(), 0);

    // Get update info
    if (application.checkWriteExternalPermission() && connected)
    {
      Thread thread = new Thread(new Runnable() {
        @Override
        public void run()
        {
          boolean success = false;
          try
          {
            // Read updates manifest
            DefaultHttpClient httpClient = new DefaultHttpClient();

            String locale = Locale.getDefault().toString().toLowerCase();
            String device = AdblockPlus.getDeviceName();
            boolean releaseBuild = context.getResources().getBoolean(R.bool.def_release);
            String updateUrlTemplate = context.getString(releaseBuild ? R.string.update_url : R.string.devbuild_update_url);
            URL updateUrl = new URL(String.format(updateUrlTemplate, Build.VERSION.SDK_INT, AdblockPlus.getApplication().getBuildNumber(), locale, device));
            // The following line correctly url-encodes query string parameters
            URI uri = new URI(updateUrl.getProtocol(), updateUrl.getUserInfo(), updateUrl.getHost(), updateUrl.getPort(), updateUrl.getPath(), updateUrl.getQuery(), updateUrl.getRef());
            HttpGet httpGet = new HttpGet(uri);

            HttpResponse httpResponse = httpClient.execute(httpGet);
            HttpEntity httpEntity = httpResponse.getEntity();
            String xml = EntityUtils.toString(httpEntity);

            // Parse XML
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();

            InputSource is = new InputSource();
            is.setCharacterStream(new StringReader(xml));
            Document doc = db.parse(is);

            // Find best match
            NodeList nl = doc.getElementsByTagName("updatecheck");
            int newBuild = -1;
            int newApi = -1;
            String newUrl = null;
            for (int i = 0; i < nl.getLength(); i++)
            {
              Element e = (Element) nl.item(i);
              String url = e.getAttribute("package");
              int build = Integer.parseInt(e.getAttribute("build"));
              int api = Integer.parseInt(e.getAttribute("api"));
              if (api > android.os.Build.VERSION.SDK_INT)
                continue;
              if ((build > newBuild) || (build == newBuild && api > newApi))
              {
                newBuild = build;
                newApi = api;
                newUrl = url;
              }
            }

            int thisBuild = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode;

            // Notify user if newer update was found
            if (thisBuild < newBuild)
            {
              notification.icon = R.drawable.ic_stat_download;
              Intent intent = new Intent(context, UpdaterActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
              intent.setAction("download");
              intent.putExtra("url", newUrl);
              intent.putExtra("build", newBuild);
              PendingIntent updateIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
              notification.setLatestEventInfo(context, context.getText(R.string.app_name), context.getString(R.string.msg_update_available), updateIntent);
              notificationManager.notify(NOTIFICATION_ID, notification);
            }
            // Notify user that no update was found
            else if (notify)
            {
              notification.setLatestEventInfo(context, context.getText(R.string.app_name), context.getString(R.string.msg_update_missing), emptyIntent);
              notificationManager.notify(NOTIFICATION_ID, notification);
            }
            success = true;
            // Schedule next check
            application.scheduleUpdater(0);
          }
          catch (IOException e)
          {
          }
          catch (NumberFormatException e)
          {
          }
          catch (NameNotFoundException e)
          {
          }
          catch (ParserConfigurationException e)
          {
          }
          catch (SAXException e)
          {
            Log.e(TAG, "Error", e);
          }
          catch (URISyntaxException e)
          {
            Log.e(TAG, "Error", e);
          }
          finally
          {
            if (!success)
            {
              // Notify user about failure
              if (notify)
              {
                notification.setLatestEventInfo(context, context.getText(R.string.app_name), context.getString(R.string.msg_update_fail), emptyIntent);
                notificationManager.notify(NOTIFICATION_ID, notification);
              }
              // Schedule retry in 1 hour - this is most probably problem on server
              application.scheduleUpdater(60);
            }
          }
        }
      });
      thread.start();
    }
    else
    {
      // Notify user about failure
      if (notify)
      {
        notification.setLatestEventInfo(context, context.getText(R.string.app_name), context.getString(R.string.msg_update_fail), emptyIntent);
        notificationManager.notify(NOTIFICATION_ID, notification);
      }
      // Schedule retry in 30 minutes - there is no connection available at this time
      application.scheduleUpdater(30);
    }
  }
}
