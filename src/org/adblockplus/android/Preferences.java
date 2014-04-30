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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.Date;

import org.jraf.android.backport.switchwidget.SwitchPreference;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

/**
 * Main settings UI.
 */
public class Preferences extends SummarizedPreferences
{
  private static final String TAG = Utils.getTag(Preferences.class);
  private static final int ABOUT_DIALOG = 1;
  private static final int HIDEICONWARNING_DIALOG = 2;

  private static ProxyService proxyService = null;
  private static boolean firstRunActionsPending = true;

  private RefreshableListPreference subscriptionList;
  private String subscriptionSummary;

  @Override
  public void onCreate(final Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);

    PreferenceManager.setDefaultValues(this, R.xml.preferences, true);
    PreferenceManager.setDefaultValues(this, R.xml.preferences_advanced, true);
    setContentView(R.layout.preferences);
    addPreferencesFromResource(R.xml.preferences);

    final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

    // Check if we need to update assets
    final int lastVersion = prefs.getInt(getString(R.string.pref_version), 0);
    try
    {
      final int thisVersion = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
      if (lastVersion != thisVersion)
      {
        copyAssets();
        final SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(getString(R.string.pref_version), thisVersion);
        editor.commit();
      }
    }
    catch (final NameNotFoundException e)
    {
      copyAssets();
    }
  }

  @Override
  protected void onStart()
  {
    super.onStart();
    final AdblockPlus application = AdblockPlus.getApplication();
    application.startEngine();

    // Initialize subscription list
    subscriptionList = (RefreshableListPreference) findPreference(getString(R.string.pref_subscription));
    final Subscription[] subscriptions = application.getRecommendedSubscriptions();
    final String[] entries = new String[subscriptions.length];
    final String[] entryValues = new String[subscriptions.length];
    int i = 0;
    for (final Subscription subscription : subscriptions)
    {
      entries[i] = subscription.title;
      entryValues[i] = subscription.url;
      i++;
    }
    subscriptionList.setEntries(entries);
    subscriptionList.setEntryValues(entryValues);

    // Set Acceptable Ads FAQ link
    final HelpfulCheckBoxPreference acceptableAdsCheckBox =
        (HelpfulCheckBoxPreference) findPreference(getString(R.string.pref_acceptableads));
    acceptableAdsCheckBox.setHelpUrl(AdblockPlus.getApplication().getAcceptableAdsUrl());
  }

  @Override
  public void onResume()
  {
    super.onResume();
    final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

    final AdblockPlus application = AdblockPlus.getApplication();

    Subscription current = null;
    final Subscription[] subscriptions = application.getListedSubscriptions();
    if (subscriptions.length > 0)
    {
      current = subscriptions[0];
    }

    final boolean firstRun = firstRunActionsPending && application.isFirstRun();
    firstRunActionsPending = false;

    if (firstRun && current != null)
    {
      showNotificationDialog(getString(R.string.install_name),
          String.format(getString(R.string.msg_subscription_offer), current.title),
          application.getAcceptableAdsUrl());
      application.setNotifiedAboutAcceptableAds(true);
      setAcceptableAdsEnabled(true);
    }
    else if (!application.isNotifiedAboutAcceptableAds())
    {
      showNotificationDialog(getString(R.string.acceptableads_name),
          getString(R.string.msg_acceptable_ads), application.getAcceptableAdsUrl());
      application.setNotifiedAboutAcceptableAds(true);
      setAcceptableAdsEnabled(true);
    }

    // Enable manual subscription refresh
    subscriptionList.setOnRefreshClickListener(new View.OnClickListener()
    {
      @Override
      public void onClick(final View v)
      {
        application.refreshSubscriptions();
      }
    });

    // Set subscription status message
    if (subscriptionSummary != null)
      subscriptionList.setSummary(subscriptionSummary);
    else
      setPrefSummary(subscriptionList);

    // Time to start listening for events
    registerReceiver(receiver, new IntentFilter(AdblockPlus.BROADCAST_SUBSCRIPTION_STATUS));
    registerReceiver(receiver, new IntentFilter(ProxyService.BROADCAST_STATE_CHANGED));
    registerReceiver(receiver, new IntentFilter(ProxyService.BROADCAST_PROXY_FAILED));

    // Update service and UI state according to user settings
    if (current != null)
    {
      subscriptionList.setValue(current.url);
      application.updateSubscriptionStatus(current.url);
    }
    final boolean enabled = prefs.getBoolean(getString(R.string.pref_enabled), false);
    final boolean proxyenabled = prefs.getBoolean(getString(R.string.pref_proxyenabled), true);
    final boolean autoconfigured = prefs.getBoolean(getString(R.string.pref_proxyautoconfigured), false);

    // This is weird but UI does not update on back button (when returning from advanced preferences)
    ((SwitchPreference) findPreference(getString(R.string.pref_enabled))).setChecked(enabled);

    if (enabled || firstRun)
      setFilteringEnabled(true);
    if (enabled || firstRun || (proxyenabled && !autoconfigured))
      setProxyEnabled(true);

    bindService(new Intent(this, ProxyService.class), proxyServiceConnection, 0);
  }

  private void showNotificationDialog(final String title, String message, String url)
  {
    url = TextUtils.htmlEncode(url);
    message = TextUtils.htmlEncode(message)
        .replaceAll("&lt;a&gt;(.*?)&lt;/a&gt;", "<a href=\"" + url + "\">$1</a>");
    final TextView messageView = new TextView(this);
    messageView.setText(Html.fromHtml(message));
    messageView.setMovementMethod(LinkMovementMethod.getInstance());
    final int padding = 10;
    messageView.setPadding(padding, padding, padding, padding);
    new AlertDialog.Builder(this).setTitle(title)
        .setView(messageView)
        .setIcon(android.R.drawable.ic_dialog_info)
        .setPositiveButton(R.string.ok, null).create().show();
  }

  @Override
  public void onPause()
  {
    super.onPause();
    try
    {
      unregisterReceiver(receiver);
    }
    catch (final IllegalArgumentException e)
    {
      // ignore - it is thrown if receiver is not registered but it can not be
      // true in normal conditions
    }
    unbindService(proxyServiceConnection);
    proxyService = null;

    hideConfigurationMsg();
  }

  @Override
  protected void onStop()
  {
    super.onStop();
    final AdblockPlus application = AdblockPlus.getApplication();
    if (!application.isFilteringEnabled())
      application.stopEngine();
  }

  @Override
  public boolean onCreateOptionsMenu(final Menu menu)
  {
    final MenuInflater inflater = getSupportMenuInflater();
    inflater.inflate(R.menu.menu_preferences, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(final MenuItem item)
  {
    switch (item.getItemId())
    {
      case R.id.menu_help:
        final Uri uri = Uri.parse(getString(R.string.configuring_url));
        final Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        startActivity(intent);
        return true;
      case R.id.menu_about:
        showDialog(ABOUT_DIALOG);
        return true;
      case R.id.menu_advanced:
        startActivity(new Intent(this, AdvancedPreferences.class));
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  private void setAcceptableAdsEnabled(final boolean enabled)
  {
    final CheckBoxPreference acceptableAdsPreference =
        (CheckBoxPreference) findPreference(getString(R.string.pref_acceptableads));
    acceptableAdsPreference.setChecked(enabled);
    final AdblockPlus application = AdblockPlus.getApplication();
    application.setAcceptableAdsEnabled(enabled);
  }

  private void setFilteringEnabled(final boolean enabled)
  {
    final SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
    editor.putBoolean(getString(R.string.pref_enabled), enabled);
    editor.commit();
    ((SwitchPreference) findPreference(getString(R.string.pref_enabled))).setChecked(enabled);
    final AdblockPlus application = AdblockPlus.getApplication();
    application.setFilteringEnabled(enabled);
  }

  private void setProxyEnabled(final boolean enabled)
  {
    final SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
    editor.putBoolean(getString(R.string.pref_proxyenabled), enabled);
    editor.commit();
    final AdblockPlus application = AdblockPlus.getApplication();
    if (enabled && !application.isServiceRunning())
      startService(new Intent(this, ProxyService.class));
  }

  /**
   * Copies file assets from installation package to filesystem.
   */
  private void copyAssets()
  {
    final AssetManager assetManager = getAssets();
    String[] files = null;
    try
    {
      files = assetManager.list("install");
    }
    catch (final IOException e)
    {
      Log.e(TAG, "Failed to get assets list", e);
    }
    for (int i = 0; i < files.length; i++)
    {
      try
      {
        Log.d(TAG, "Copy: install/" + files[i]);
        final InputStream in = assetManager.open("install/" + files[i]);
        final OutputStream out = openFileOutput(files[i], MODE_PRIVATE);
        final byte[] buffer = new byte[1024];
        int read;
        while ((read = in.read(buffer)) != -1)
        {
          out.write(buffer, 0, read);
        }
        in.close();
        out.flush();
        out.close();
      }
      catch (final Exception e)
      {
        Log.e(TAG, "Asset copy error", e);
      }
    }
  }

  public void showProxySettings(final View v)
  {
    startActivity(new Intent(this, ProxyConfigurationActivity.class).putExtra("port", proxyService.port));
  }

  @Override
  protected Dialog onCreateDialog(final int id)
  {
    Dialog dialog = null;
    switch (id)
    {
      case ABOUT_DIALOG:
        dialog = new AboutDialog(this);
        break;
      case HIDEICONWARNING_DIALOG:
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.warning);
        builder.setIcon(android.R.drawable.ic_dialog_alert);
        builder.setCancelable(false);
        final StringBuffer message = new StringBuffer();
        message.append(getString(R.string.msg_hideicon_warning));
        builder.setPositiveButton(R.string.gotit, new DialogInterface.OnClickListener()
        {
          @Override
          public void onClick(final DialogInterface dialog, final int id)
          {
            dialog.cancel();
          }
        });
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
        {
          message.append("<br/><br/>");
          message.append(getString(R.string.msg_hideicon_native));
          builder.setNeutralButton(R.string.showme, new DialogInterface.OnClickListener()
          {
            @Override
            public void onClick(final DialogInterface dialog, final int id)
            {
              AdblockPlus.showAppDetails(getApplicationContext());
              dialog.cancel();
            }
          });
        }
        builder.setMessage(Html.fromHtml(message.toString()));
        dialog = builder.create();
        break;
    }
    return dialog;
  }

  @Override
  public void onSharedPreferenceChanged(final SharedPreferences sharedPreferences, final String key)
  {
    final AdblockPlus application = AdblockPlus.getApplication();
    if (getString(R.string.pref_enabled).equals(key))
    {
      final boolean enabled = sharedPreferences.getBoolean(key, false);
      final boolean autoconfigured = sharedPreferences.getBoolean(getString(R.string.pref_proxyautoconfigured), false);
      final boolean serviceRunning = application.isServiceRunning();
      application.setFilteringEnabled(enabled);
      if (enabled)
      {
        // If user has enabled filtering, enable proxy as well
        setProxyEnabled(true);
      }
      else if (serviceRunning && autoconfigured)
      {
        // If user disabled filtering disable proxy only if it was autoconfigured
        stopService(new Intent(this, ProxyService.class));
      }
    }
    else if (getString(R.string.pref_acceptableads).equals(key))
    {
      final boolean enabled = sharedPreferences.getBoolean(key, false);
      application.setAcceptableAdsEnabled(enabled);
    }
    else if (getString(R.string.pref_subscription).equals(key))
    {
      final String url = sharedPreferences.getString(key, null);
      if (url != null)
        application.setSubscription(url);
    }
    else if (getString(R.string.pref_hideicon).equals(key))
    {
      final boolean hideIcon = sharedPreferences.getBoolean(key, false);
      if (hideIcon)
        showDialog(HIDEICONWARNING_DIALOG);
      if (proxyService != null)
        proxyService.setEmptyIcon(hideIcon);
    }
    super.onSharedPreferenceChanged(sharedPreferences, key);
  }

  private void showConfigurationMsg(final String message)
  {
    final ViewGroup grp = (ViewGroup) findViewById(R.id.grp_configuration);
    final TextView msg = (TextView) findViewById(R.id.txt_configuration);
    msg.setText(Html.fromHtml(message));
    grp.setVisibility(View.VISIBLE);
  }

  private void hideConfigurationMsg()
  {
    final ViewGroup grp = (ViewGroup) findViewById(R.id.grp_configuration);
    grp.setVisibility(View.GONE);
  }

  private final BroadcastReceiver receiver = new BroadcastReceiver()
  {
    @Override
    public void onReceive(final Context context, final Intent intent)
    {
      final String action = intent.getAction();
      final Bundle extra = intent.getExtras();
      if (action.equals(ProxyService.BROADCAST_STATE_CHANGED))
      {
        if (extra.getBoolean("enabled"))
        {
          // Service is enabled in manual mode
          if (extra.getBoolean("manual"))
          {
            // Proxy is properly configured
            if (extra.getBoolean("configured"))
              hideConfigurationMsg();
            else
              showConfigurationMsg(getString(R.string.msg_configuration));
          }
        }
        else
        {
          setFilteringEnabled(false);
          hideConfigurationMsg();
        }
      }
      if (action.equals(ProxyService.BROADCAST_PROXY_FAILED))
      {
        final String msg = extra.getString("msg");
        new AlertDialog.Builder(Preferences.this).setTitle(R.string.error).setMessage(msg).setIcon(android.R.drawable.ic_dialog_alert).setPositiveButton(R.string.ok, null).create().show();
        setFilteringEnabled(false);
      }
      if (action.equals(AdblockPlus.BROADCAST_SUBSCRIPTION_STATUS))
      {
        // TODO Should check if url matches active subscription
        final String text = extra.getString("status");
        final long time = extra.getLong("time");
        runOnUiThread(new Runnable()
        {
          @Override
          public void run()
          {
            setSubscriptionStatus(text, time);
          }
        });
      }
    }
  };

  /**
   * Constructs and updates subscription status text.
   *
   * @param text
   *          status message
   * @param time
   *          time of last change
   */
  private void setSubscriptionStatus(final String text, final long time)
  {
    final ListPreference subscriptionList = (ListPreference) findPreference(getString(R.string.pref_subscription));
    final CharSequence summary = subscriptionList.getEntry();
    final StringBuilder builder = new StringBuilder();
    if (summary != null)
    {
      builder.append(summary);
      if (text != "")
      {
        builder.append(" (");
        final int id = getResources().getIdentifier(text, "string", getPackageName());
        if (id > 0)
          builder.append(getString(id, text));
        else
          builder.append(text);
        if (time > 0)
        {
          builder.append(": ");
          final Calendar calendar = Calendar.getInstance();
          calendar.setTimeInMillis(time);
          final Date date = calendar.getTime();
          builder.append(DateFormat.getDateFormat(this).format(date));
          builder.append(" ");
          builder.append(DateFormat.getTimeFormat(this).format(date));
        }
        builder.append(")");
      }
      subscriptionSummary = builder.toString();
      subscriptionList.setSummary(subscriptionSummary);
    }
  }

  @Override
  protected void onRestoreInstanceState(final Bundle state)
  {
    super.onRestoreInstanceState(state);
    subscriptionSummary = state.getString("subscriptionSummary");
  }

  @Override
  protected void onSaveInstanceState(final Bundle outState)
  {
    outState.putString("subscriptionSummary", subscriptionSummary);
    super.onSaveInstanceState(outState);
  }

  private final ServiceConnection proxyServiceConnection = new ServiceConnection()
  {
    @Override
    public void onServiceConnected(final ComponentName className, final IBinder service)
    {
      proxyService = ((ProxyService.LocalBinder) service).getService();
      Log.d(TAG, "Proxy service connected");

      if (proxyService.isManual() && proxyService.noTraffic())
        showConfigurationMsg(getString(R.string.msg_configuration));
    }

    @Override
    public void onServiceDisconnected(final ComponentName className)
    {
      proxyService = null;
      Log.d(TAG, "Proxy service disconnected");
    }
  };
}
