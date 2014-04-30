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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Pattern;

import org.adblockplus.android.updater.AlarmReceiver;
import org.apache.commons.lang.StringUtils;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;

public class AdblockPlus extends Application
{
  private static final String TAG = Utils.getTag(AdblockPlus.class);

  private static final Pattern RE_JS = Pattern.compile(".*\\.js$", Pattern.CASE_INSENSITIVE);
  private static final Pattern RE_CSS = Pattern.compile(".*\\.css$", Pattern.CASE_INSENSITIVE);
  private static final Pattern RE_IMAGE = Pattern.compile(".*\\.(?:gif|png|jpe?g|bmp|ico)$", Pattern.CASE_INSENSITIVE);
  private static final Pattern RE_FONT = Pattern.compile(".*\\.(?:ttf|woff)$", Pattern.CASE_INSENSITIVE);
  private static final Pattern RE_HTML = Pattern.compile(".*\\.html?$", Pattern.CASE_INSENSITIVE);

  /**
   * Update notification id.
   */
  public static final int UPDATE_NOTIFICATION_ID = R.string.app_name + 1;
  /**
   * Broadcasted when filtering is enabled or disabled.
   */
  public static final String BROADCAST_FILTERING_CHANGE = "org.adblockplus.android.filtering.status";
  /**
   * Broadcasted when subscription status changes.
   */
  public static final String BROADCAST_SUBSCRIPTION_STATUS = "org.adblockplus.android.subscription.status";
  /**
   * Broadcasted when filter match check is performed.
   */
  public static final String BROADCAST_FILTER_MATCHES = "org.adblockplus.android.filter.matches";
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

  private static class ReferrerMappingCache extends LinkedHashMap<String, String>
  {
    private static final long serialVersionUID = 1L;
    private static final int MAX_SIZE = 5000;

    public ReferrerMappingCache()
    {
      super(MAX_SIZE + 1, 0.75f, true);
    }

    @Override
    protected boolean removeEldestEntry(final Map.Entry<String, String> eldest)
    {
      return size() > MAX_SIZE;
    }
  };

  private final ReferrerMappingCache referrerMapping = new ReferrerMappingCache();

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
      final PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
      buildNumber = pi.versionCode;
    }
    catch (final NameNotFoundException e)
    {
      // ignore - this shouldn't happen
      Log.e(TAG, e.getMessage(), e);
    }
    return buildNumber;
  }

  /**
   * Opens Android application settings
   */
  public static void showAppDetails(final Context context)
  {
    final String packageName = context.getPackageName();
    final Intent intent = new Intent();
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD)
    {
      // above 2.3
      intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
      final Uri uri = Uri.fromParts("package", packageName, null);
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
    final String manufacturer = Build.MANUFACTURER;
    final String model = Build.MODEL;
    if (model.startsWith(manufacturer))
      return Utils.capitalizeString(model);
    else
      return Utils.capitalizeString(manufacturer) + " " + model;
  }

  public static void appendRawTextFile(final Context context, final StringBuilder text, final int id)
  {
    // TODO: What about closing the resources?
    final InputStream inputStream = context.getResources().openRawResource(id);
    final InputStreamReader in = new InputStreamReader(inputStream);
    final BufferedReader buf = new BufferedReader(in);
    String line;
    try
    {
      while ((line = buf.readLine()) != null)
        text.append(line);
    }
    catch (final IOException e)
    {
      // TODO: How about real logging?
      e.printStackTrace();
    }
  }

  /**
   * Checks if device has a WiFi connection available.
   */
  public static boolean isWiFiConnected(final Context context)
  {
    final ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
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
    final ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
    // Actually it returns not only running services, so extra check is required
    for (final RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE))
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
    final String permission = "android.permission.WRITE_EXTERNAL_STORAGE";
    final int res = checkCallingOrSelfPermission(permission);
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
    // TODO: Why don't we re-check?
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
  public void setSubscription(final String url)
  {
    abpEngine.setSubscription(url);
  }

  /**
   * Forces subscriptions refresh.
   */
  public void refreshSubscriptions()
  {
    abpEngine.refreshSubscriptions();
  }

  /**
   * Enforces subscription status update.
   *
   * @param url Subscription url
   */
  public void updateSubscriptionStatus(final String url)
  {
    abpEngine.updateSubscriptionStatus(url);
  }

  /**
   * Enables or disables Acceptable Ads
   */
  public void setAcceptableAdsEnabled(final boolean enabled)
  {
    abpEngine.setAcceptableAdsEnabled(enabled);
  }

  public String getAcceptableAdsUrl()
  {
    final String documentationLink = abpEngine.getDocumentationLink();
    final String locale = getResources().getConfiguration().locale.toString().replace("_", "-");
    return documentationLink.replace("%LINK%", "acceptable_ads").replace("%LANG%", locale);
  }

  public void setNotifiedAboutAcceptableAds(final boolean notified)
  {
    final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
    final Editor editor = preferences.edit();
    editor.putBoolean("notified_about_acceptable_ads", notified);
    editor.commit();
  }

  public boolean isNotifiedAboutAcceptableAds()
  {
    final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
    return preferences.getBoolean("notified_about_acceptable_ads", false);
  }

  /**
   * Returns ElemHide selectors for domain.
   *
   * @param domain The domain
   * @return A list of CSS selectors
   */
  public String[] getSelectorsForDomain(final String domain)
  {
    /* We need to ignore element hiding rules here to work around two bugs:
     * 1. CSS is being injected even when there's an exception rule with $elemhide
     * 2. The injected CSS causes blank pages in Chrome for Android
     *
     * Starting with 1.1.2, we ignored element hiding rules after download anyway, to keep the
     * memory usage down. Doing this with libadblockplus is trickier, but would be the clean
     * solution. */
    return null;
/*
    if (!filteringEnabled)
      return null;

    return abpEngine.getSelectorsForDomain(domain);
*/
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
  public boolean matches(final String url, final String query, final String referrer, final String accept)
  {
    final String fullUrl = !"".equals(query) ? url + "?" + query : url;
    if (referrer != null)
      referrerMapping.put(fullUrl, referrer);

    if (!filteringEnabled)
      return false;

    String contentType = null;

    if (accept != null)
    {
      if (accept.contains("text/css"))
        contentType = "STYLESHEET";
      else if (accept.contains("image/*"))
        contentType = "IMAGE";
      else if (accept.contains("text/html"))
        contentType = "SUBDOCUMENT";
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
      else if (RE_HTML.matcher(url).matches())
        contentType = "SUBDOCUMENT";
    }
    if (contentType == null)
      contentType = "OTHER";

    final List<String> referrerChain = buildReferrerChain(referrer);
    Log.d("Referrer chain", fullUrl + ": " + referrerChain.toString());
    final String[] referrerChainArray = referrerChain.toArray(new String[referrerChain.size()]);
    return abpEngine.matches(fullUrl, contentType, referrerChainArray);
  }

  private List<String> buildReferrerChain(String url)
  {
    final List<String> referrerChain = new ArrayList<String>();
    // We need to limit the chain length to ensure we don't block indefinitely if there's
    // a referrer loop.
    final int maxChainLength = 10;
    for (int i = 0; i < maxChainLength && url != null; i++)
    {
      referrerChain.add(0, url);
      url = referrerMapping.get(url);
    }
    return referrerChain;
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
  public void setFilteringEnabled(final boolean enable)
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
      final File basePath = getFilesDir();
      abpEngine = ABPEngine.create(AdblockPlus.getApplication(), ABPEngine.generateAppInfo(this), basePath.getAbsolutePath());
    }
  }

  /**
   * Stops ABP engine.
   */
  public void stopEngine()
  {
    if (abpEngine != null)
    {
      abpEngine.dispose();
      abpEngine = null;
      Log.i(TAG, "stopEngine");
    }
  }

  /**
   * Initiates immediate interactive check for available update.
   */
  public void checkUpdates()
  {
    abpEngine.checkForUpdates();
  }

  /**
   * Sets Alarm to call updater after specified number of minutes or after one
   * day if
   * minutes are set to 0.
   *
   * @param minutes
   *          number of minutes to wait
   */
  public void scheduleUpdater(final int minutes)
  {
    final Calendar updateTime = Calendar.getInstance();

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

    final Intent updater = new Intent(this, AlarmReceiver.class);
    final PendingIntent recurringUpdate = PendingIntent.getBroadcast(this, 0, updater, PendingIntent.FLAG_CANCEL_CURRENT);
    // Set non-waking alarm
    final AlarmManager alarms = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
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
      final InputStreamReader reportFile = new InputStreamReader(openFileInput(CrashHandler.REPORT_FILE));
      final char[] buffer = new char[0x1000];
      final StringBuilder out = new StringBuilder();
      int read;
      do
      {
        read = reportFile.read(buffer, 0, buffer.length);
        if (read > 0)
          out.append(buffer, 0, read);
      }
      while (read >= 0);
      final String report = out.toString();
      if (StringUtils.isNotEmpty(report))
      {
        final Intent intent = new Intent(this, CrashReportDialog.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("report", report);
        startActivity(intent);
      }
    }
    catch (final FileNotFoundException e)
    {
      // ignore
    }
    catch (final IOException e)
    {
      Log.e(TAG, e.getMessage(), e);
    }

    // Set crash handler
    Thread.setDefaultUncaughtExceptionHandler(new CrashHandler(this));

    // Initiate update check
    scheduleUpdater(0);
  }
}
