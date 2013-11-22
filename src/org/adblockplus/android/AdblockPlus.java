/*
 * This file is part of Adblock Plus <http://adblockplus.org/>,
 * Copyright (C) 2006-2013 Eyeo GmbH
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.regex.Pattern;

import org.adblockplus.android.updater.AlarmReceiver;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

public class AdblockPlus extends Application
{
  private final static String TAG = "Application";

  private final static Pattern RE_JS = Pattern.compile(".*\\.js$", Pattern.CASE_INSENSITIVE);
  private final static Pattern RE_CSS = Pattern.compile(".*\\.css$", Pattern.CASE_INSENSITIVE);
  private final static Pattern RE_IMAGE = Pattern.compile(".*\\.(?:gif|png|jpe?g|bmp|ico)$", Pattern.CASE_INSENSITIVE);
  private final static Pattern RE_FONT = Pattern.compile(".*\\.(?:ttf|woff)$", Pattern.CASE_INSENSITIVE);

  /**
   * Broadcasted when filtering is enabled or disabled.
   */
  public static final String BROADCAST_FILTERING_CHANGE = "org.adblockplus.android.filtering.status";
  /**
   * Broadcasted when subscription status changes.
   */
  public final static String BROADCAST_SUBSCRIPTION_STATUS = "org.adblockplus.android.subscription.status";
  /**
   * Broadcasted when filter match check is performed.
   */
  public final static String BROADCAST_FILTER_MATCHES = "org.adblockplus.android.filter.matches";

  /**
   * Cached list of recommended subscriptions.
   */
  private Subscription[] subscriptions;

  /**
   * Indicates whether filtering is enabled or not.
   */
  private boolean filteringEnabled = false;

  private ABPEngine abpEngine;
  
  private static AdblockPlus instance;

  /**
   * Returns pointer to itself (singleton pattern).
   */
  public static AdblockPlus getApplication()
  {
    return instance;
  }

  public int getBuildNumber()
  {
    int buildNumber = -1;
    try
    {
      PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
      buildNumber = pi.versionCode;
    }
    catch (NameNotFoundException e)
    {
      // ignore - this shouldn't happen
      Log.e(TAG, e.getMessage(), e);
    }
    return buildNumber;
  }

  /**
   * Opens Android application settings
   */
  public static void showAppDetails(Context context)
  {
    String packageName = context.getPackageName();
    Intent intent = new Intent();
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD)
    {
      // above 2.3
      intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
      Uri uri = Uri.fromParts("package", packageName, null);
      intent.setData(uri);
    }
    else
    {
      // below 2.3
      final String appPkgName = (Build.VERSION.SDK_INT == Build.VERSION_CODES.FROYO ? "pkg" : "com.android.settings.ApplicationPkgName");
      intent.setAction(Intent.ACTION_VIEW);
      intent.setClassName("com.android.settings", "com.android.settings.InstalledAppDetails");
      intent.putExtra(appPkgName, packageName);
    }
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    context.startActivity(intent);
  }

  /**
   * Returns device name in user-friendly format
   */
  public static String getDeviceName()
  {
    String manufacturer = Build.MANUFACTURER;
    String model = Build.MODEL;
    if (model.startsWith(manufacturer))
      return capitalize(model);
    else
      return capitalize(manufacturer) + " " + model;
  }

  public static void appendRawTextFile(Context context, StringBuilder text, int id)
  {
    InputStream inputStream = context.getResources().openRawResource(id);
    InputStreamReader in = new InputStreamReader(inputStream);
    BufferedReader buf = new BufferedReader(in);
    String line;
    try
    {
      while ((line = buf.readLine()) != null)
        text.append(line);
    }
    catch (IOException e)
    {
      e.printStackTrace();
    }
  }

  private static String capitalize(String s)
  {
    if (s == null || s.length() == 0)
      return "";
    char first = s.charAt(0);
    if (Character.isUpperCase(first))
      return s;
    else
      return Character.toUpperCase(first) + s.substring(1);
  }

  /**
   * Checks if device has a WiFi connection available.
   */
  public static boolean isWiFiConnected(Context context)
  {
    ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    NetworkInfo networkInfo = null;
    if (connectivityManager != null)
    {
      networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
    }
    return networkInfo == null ? false : networkInfo.isConnected();
  }
  
  /**
   * Checks if ProxyService is running.
   * 
   * @return true if service is running
   */
  public boolean isServiceRunning()
  {
    ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
    // Actually it returns not only running services, so extra check is required
    for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE))
    {
      if (service.service.getClassName().equals(ProxyService.class.getCanonicalName()) && service.pid > 0)
        return true;
    }
    return false;
  }

  /**
   * Checks if application can write to external storage.
   */
  public boolean checkWriteExternalPermission()
  {
    String permission = "android.permission.WRITE_EXTERNAL_STORAGE";
    int res = checkCallingOrSelfPermission(permission);
    return res == PackageManager.PERMISSION_GRANTED;
  }

  public boolean isFirstRun()
  {
    return abpEngine.isFirstRun();
  }

  /**
   * Returns list of known subscriptions.
   */
  public Subscription[] getRecommendedSubscriptions()
  {
    if (subscriptions == null)
      subscriptions = abpEngine.getRecommendedSubscriptions();
    return subscriptions;
  }

  /**
   * Returns list of enabled subscriptions.
   */
  public Subscription[] getListedSubscriptions()
  {
    return abpEngine.getListedSubscriptions();    
  }

  /**
   * Adds provided subscription and removes previous subscriptions if any.
   * 
   * @param url
   *          URL of subscription to add
   */
  public void setSubscription(String url)
  {
    Subscription[] subscriptions = abpEngine.getListedSubscriptions();    
    for (Subscription subscription : subscriptions)
    {
      abpEngine.removeSubscription(subscription.url);
    }
    abpEngine.addSubscription(url);
  }

  /**
   * Forces subscriptions refresh.
   */
  public void refreshSubscriptions()
  {
    Subscription[] subscriptions = abpEngine.getListedSubscriptions();
    for (Subscription subscription : subscriptions)
    {
      abpEngine.refreshSubscription(subscription.url);
    }
  }
  
  /**
   * Enforces subscription status update.
   * 
   * @param url Subscription url
   */
  public void actualizeSubscriptionStatus(String url)
  {
    abpEngine.actualizeSubscriptionStatus(url);
  }


  /**
   * Enables or disables Acceptable Ads
   */
  public void setAcceptableAdsEnabled(boolean enabled)
  {
    abpEngine.setAcceptableAdsEnabled(enabled);
  }

  /**
   * Returns ElemHide selectors for domain.
   *
   * @param domain The domain
   * @return A list of CSS selectors
   */
  public String[] getSelectorsForDomain(final String domain)
  {
    if (!filteringEnabled)
      return null;

    return abpEngine.getSelectorsForDomain(domain);
  }

  /**
   * Checks if filters match request parameters.
   * 
   * @param url
   *          Request URL
   * @param query
   *          Request query string
   * @param referrer
   *          Request referrer header
   * @param accept
   *          Request accept header
   * @return true if matched filter was found
   * @throws Exception
   */
  public boolean matches(String url, String query, String referrer, String accept)
  {
    if (!filteringEnabled)
      return false;
    
    String contentType = null;

    if (accept != null)
    {
      if (accept.contains("text/css"))
        contentType = "STYLESHEET";
      else if (accept.contains("image/*"))
        contentType = "IMAGE";
    }

    if (contentType == null)
    {
      if (RE_JS.matcher(url).matches())
        contentType = "SCRIPT";
      else if (RE_CSS.matcher(url).matches())
        contentType = "STYLESHEET";
      else if (RE_IMAGE.matcher(url).matches())
        contentType = "IMAGE";
      else if (RE_FONT.matcher(url).matches())
        contentType = "FONT";
    }
    if (contentType == null)
      contentType = "OTHER";

    if (!"".equals(query))
      url = url + "?" + query;

    return abpEngine.matches(url, contentType, referrer);
  }

  /**
   * Checks if filtering is enabled.
   */
  public boolean isFilteringEnabled()
  {
    return filteringEnabled;
  }

  /**
   * Enables or disables filtering.
   */
  public void setFilteringEnabled(boolean enable)
  {
    filteringEnabled = enable;
    sendBroadcast(new Intent(BROADCAST_FILTERING_CHANGE).putExtra("enabled", filteringEnabled));
  }
  
  /**
   * Starts ABP engine. It also initiates subscription refresh if it is enabled
   * in user settings.
   */
  public void startEngine()
  {
    if (abpEngine == null)
    {
      File basePath = getFilesDir();
      abpEngine = new ABPEngine(this, basePath.getAbsolutePath());
    }
  }

  /**
   * Stops ABP engine.
   */
  public void stopEngine()
  {
    if (abpEngine != null)
    {
      abpEngine.release();
      abpEngine = null;
      Log.i(TAG, "stopEngine");
    }
  }

  /**
   * Initiates immediate interactive check for available update.
   */
  public void checkUpdates()
  {
    abpEngine.checkUpdates();
  }

  /**
   * Sets Alarm to call updater after specified number of minutes or after one
   * day if
   * minutes are set to 0.
   * 
   * @param minutes
   *          number of minutes to wait
   */
  public void scheduleUpdater(int minutes)
  {
    Calendar updateTime = Calendar.getInstance();

    if (minutes == 0)
    {
      // Start update checks at 10:00 GMT...
      updateTime.setTimeZone(TimeZone.getTimeZone("GMT"));
      updateTime.set(Calendar.HOUR_OF_DAY, 10);
      updateTime.set(Calendar.MINUTE, 0);
      // ...next day
      updateTime.add(Calendar.HOUR_OF_DAY, 24);
      // Spread out the “mass downloading” for 6 hours
      updateTime.add(Calendar.MINUTE, (int) (Math.random() * 60 * 6));
    }
    else
    {
      updateTime.add(Calendar.MINUTE, minutes);
    }

    Intent updater = new Intent(this, AlarmReceiver.class);
    PendingIntent recurringUpdate = PendingIntent.getBroadcast(this, 0, updater, PendingIntent.FLAG_CANCEL_CURRENT);
    // Set non-waking alarm
    AlarmManager alarms = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
    alarms.set(AlarmManager.RTC, updateTime.getTimeInMillis(), recurringUpdate);
  }

  @Override
  public void onCreate()
  {
    super.onCreate();
    instance = this;

    // Check for crash report
    try
    {
      InputStreamReader reportFile = new InputStreamReader(openFileInput(CrashHandler.REPORT_FILE));
      final char[] buffer = new char[0x1000];
      StringBuilder out = new StringBuilder();
      int read;
      do
      {
        read = reportFile.read(buffer, 0, buffer.length);
        if (read > 0)
          out.append(buffer, 0, read);
      }
      while (read >= 0);
      String report = out.toString();
      if (!"".equals(report))
      {
        final Intent intent = new Intent(this, CrashReportDialog.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("report", report);
        startActivity(intent);
      }
    }
    catch (FileNotFoundException e)
    {
      // ignore
    }
    catch (IOException e)
    {
      // TODO Auto-generated catch block
      Log.e(TAG, e.getMessage(), e);
    }

    // Set crash handler
    Thread.setDefaultUncaughtExceptionHandler(new CrashHandler(this));

    // Initiate update check
    scheduleUpdater(0);
  }
}
