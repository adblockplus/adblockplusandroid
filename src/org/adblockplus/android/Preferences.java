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

package org.adblockplus.android;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.ServiceConnection;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

/**
 * Main settings UI.
 */
public class Preferences extends SummarizedPreferences
{
  private static final String TAG = "Preferences";

  private static ProxyService proxyService = null;

  private RefreshableListPreference subscriptionList;
  
  private AboutDialog aboutDialog;
  private boolean showAbout = false;
  private boolean trafficDetected = false;
  private String subscriptionSummary;

  @Override
  public void onCreate(Bundle savedInstanceState)
  {
    requestWindowFeature(Window.FEATURE_NO_TITLE);

    super.onCreate(savedInstanceState);

    PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
    PreferenceManager.setDefaultValues(this, R.xml.preferences_advanced, false);
    setContentView(R.layout.preferences);
    addPreferencesFromResource(R.xml.preferences);

    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

    // Check if we need to update assets
    int lastVersion = prefs.getInt(getString(R.string.pref_version), 0);
    try
    {
      int thisVersion = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
      if (lastVersion != thisVersion)
      {
        copyAssets();
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(getString(R.string.pref_version), thisVersion);
        editor.commit();
      }
    }
    catch (NameNotFoundException e)
    {
      copyAssets();
    }

    AdblockPlus application = AdblockPlus.getApplication();

    // Initialize subscription list
    subscriptionList = (RefreshableListPreference) findPreference(getString(R.string.pref_subscription));
    List<Subscription> subscriptions = application.getSubscriptions();
    String[] entries = new String[subscriptions.size()];
    String[] entryValues = new String[subscriptions.size()];
    int i = 0;
    for (Subscription subscription : subscriptions)
    {
      entries[i] = subscription.title;
      entryValues[i] = subscription.url;
      i++;
    }
    subscriptionList.setEntries(entries);
    subscriptionList.setEntryValues(entryValues);  
  }

  @Override
  protected void onStart()
  {
    super.onStart();
    AdblockPlus application = AdblockPlus.getApplication();
    application.startEngine();
    application.startInteractive();
  }

  @Override
  public void onResume()
  {
    super.onResume();
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

    final AdblockPlus application = AdblockPlus.getApplication();

    boolean firstRun = false;

    // Get current subscription
    String current = prefs.getString(getString(R.string.pref_subscription), (String) null);
    
    // If there is no current subscription autoselect one
    if (current == null)
    {
      firstRun = true;
      Subscription offer = application.offerSubscription();
      current = offer.url;
      if (offer != null)
      {
        subscriptionList.setValue(offer.url);
        application.setSubscription(offer);
        new AlertDialog.Builder(this).setTitle(R.string.app_name).setMessage(String.format(getString(R.string.msg_subscription_offer, offer.title))).setIcon(android.R.drawable.ic_dialog_info)
            .setPositiveButton(R.string.ok, null).create().show();
      }
    }

    // Enable manual subscription refresh
    subscriptionList.setOnRefreshClickListener(new View.OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        application.refreshSubscription();
      }
    });

    // Set subscription status message
    if (subscriptionSummary != null)
      subscriptionList.setSummary(subscriptionSummary);
    else
      setPrefSummary(subscriptionList);

    // Time to start listening for events
    registerReceiver(receiver, new IntentFilter(AdblockPlus.BROADCAST_SUBSCRIPTION_STATUS));
    registerReceiver(receiver, new IntentFilter(AdblockPlus.BROADCAST_FILTER_MATCHES));
    registerReceiver(receiver, new IntentFilter(ProxyService.BROADCAST_STATE_CHANGED));
    registerReceiver(receiver, new IntentFilter(ProxyService.BROADCAST_PROXY_FAILED));

    final String url = current;

    // Initialize subscription verification
    (new Thread()
    {
      @Override
      public void run()
      {
        if (!application.verifySubscriptions())
        {
          Subscription subscription = application.getSubscription(url);
          application.setSubscription(subscription);
        }
      }
    }).start();

    // Check if service is running and update UI accordingly
    boolean enabled = prefs.getBoolean(getString(R.string.pref_enabled), false);
    if (enabled && !isServiceRunning())
    {
      setEnabled(false);
    }
    // Run service if this is first application run
    else if (!enabled && firstRun)
    {
      startService(new Intent(this, ProxyService.class));
      setEnabled(true);
    }

    if (showAbout)
      onAbout(findViewById(R.id.btn_about));
    
    bindService(new Intent(this, ProxyService.class), proxyServiceConnection, 0);
  }

  @Override
  public void onPause()
  {
    super.onPause();
    try
    {
      unregisterReceiver(receiver);
    }
    catch (IllegalArgumentException e)
    {
      // ignore - it is thrown if receiver is not registered but it can not be true in normal conditions
    }
    unbindService(proxyServiceConnection);
    proxyService = null;
  }

  @Override
  protected void onStop()
  {
    super.onStop();
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
    boolean enabled = prefs.getBoolean(getString(R.string.pref_enabled), false);
    AdblockPlus application = AdblockPlus.getApplication();
    application.stopInteractive();
    if (!enabled)
      application.stopEngine(true);

    if (aboutDialog != null)
      aboutDialog.dismiss();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu)
  {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.menu_preferences, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item)
  {
    switch (item.getItemId())
    {
      case R.id.menu_advanced:
        startActivity(new Intent(this, AdvancedPreferences.class));
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  private void setEnabled(boolean enabled)
  {
    SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
    editor.putBoolean(getString(R.string.pref_enabled), enabled);
    editor.commit();
    ((CheckBoxPreference) findPreference(getString(R.string.pref_enabled))).setChecked(enabled);
  }

  /**
   * Checks if ProxyService is running.
   *
   * @return true if service is running
   */
  private boolean isServiceRunning()
  {
    ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
    // Actually it returns not only running services, so extra check is required
    for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE))
    {
      if ("org.adblockplus.android.ProxyService".equals(service.service.getClassName()) && service.pid > 0)
        return true;
    }
    return false;
  }

  /**
   * Copies file assets from installation package to filesystem.
   */
  private void copyAssets()
  {
    AssetManager assetManager = getAssets();
    String[] files = null;
    try
    {
      files = assetManager.list("install");
    }
    catch (IOException e)
    {
      Log.e(TAG, "Failed to get assets list", e);
    }
    for (int i = 0; i < files.length; i++)
    {
      try
      {
        Log.d(TAG, "Copy: install/" + files[i]);
        InputStream in = assetManager.open("install/" + files[i]);
        OutputStream out = openFileOutput(files[i], MODE_PRIVATE);
        byte[] buffer = new byte[1024];
        int read;
        while ((read = in.read(buffer)) != -1)
        {
          out.write(buffer, 0, read);
        }
        in.close();
        out.flush();
        out.close();
      }
      catch (Exception e)
      {
        Log.e(TAG, "Asset copy error", e);
      }
    }
  }

  /**
   * Redirects user to configuration URL.
   */
  public void onHelp(View view)
  {
    Uri uri = Uri.parse(getString(R.string.configuring_url));
    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
    startActivity(intent);
  }

  /**
   * Shows about dialog.
   */
  public void onAbout(View view)
  {
    aboutDialog = new AboutDialog(this);
    aboutDialog.setOnDismissListener(new OnDismissListener()
    {

      @Override
      public void onDismiss(DialogInterface dialog)
      {
        showAbout = false;
        aboutDialog = null;
      }
    });
    showAbout = true;
    aboutDialog.show();
  }

  @Override
  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
  {
    if (getString(R.string.pref_enabled).equals(key))
    {
      boolean enabled = sharedPreferences.getBoolean(key, false);
      boolean serviceRunning = isServiceRunning();
      if (enabled && !serviceRunning)
        startService(new Intent(this, ProxyService.class));
      else if (!enabled && serviceRunning)
        stopService(new Intent(this, ProxyService.class));
    }
    if (getString(R.string.pref_subscription).equals(key))
    {
      String current = sharedPreferences.getString(key, null);
      AdblockPlus application = AdblockPlus.getApplication();
      Subscription subscription = application.getSubscription(current);
      application.setSubscription(subscription);
    }
    super.onSharedPreferenceChanged(sharedPreferences, key);
  }

  private void showConfigurationMsg(String message)
  {
    TextView msg = (TextView) findViewById(R.id.txt_configuration);
    msg.setText(message);
    msg.setVisibility(View.VISIBLE);
  }

  private void hideConfigurationMsg()
  {
    TextView msg = (TextView) findViewById(R.id.txt_configuration);
    msg.setVisibility(View.GONE);
  }

  private BroadcastReceiver receiver = new BroadcastReceiver()
  {
    @Override
    public void onReceive(Context context, Intent intent)
    {
      String action = intent.getAction();
      Bundle extra = intent.getExtras();
      if (action.equals(ProxyService.BROADCAST_STATE_CHANGED))
      {
        if (extra.getBoolean("enabled"))
        {
          // If service is enabled in manual mode, show configuration message
          if (extra.getBoolean("manual"))
          {
            showConfigurationMsg(getString(R.string.msg_configuration, extra.getInt("port")));
          }
        }
        else
        {
          setEnabled(false);
          hideConfigurationMsg();
        }
      }
      if (action.equals(AdblockPlus.BROADCAST_FILTER_MATCHES))
      {
        // Hide configuration message if traffic is detected for the first time
        if (!trafficDetected)
          hideConfigurationMsg();
        trafficDetected = true;
      }
      if (action.equals(ProxyService.BROADCAST_PROXY_FAILED))
      {
        String msg = extra.getString("msg");
        new AlertDialog.Builder(Preferences.this).setTitle(R.string.error).setMessage(msg).setIcon(android.R.drawable.ic_dialog_alert).setPositiveButton(R.string.ok, null).create().show();
        setEnabled(false);
      }
      if (action.equals(AdblockPlus.BROADCAST_SUBSCRIPTION_STATUS))
      {
        final String text = extra.getString("text");
        final long time = extra.getLong("time");
        runOnUiThread(new Runnable()
        {
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
  private void setSubscriptionStatus(String text, long time)
  {
    ListPreference subscriptionList = (ListPreference) findPreference(getString(R.string.pref_subscription));
    CharSequence summary = subscriptionList.getEntry();
    StringBuilder builder = new StringBuilder();
    if (summary != null)
    {
      builder.append(summary);
      if (text != "")
      {
        builder.append(" (");
        int id = getResources().getIdentifier(text, "string", getPackageName());
        if (id > 0)
          builder.append(getString(id, text));
        else
          builder.append(text);
        if (time > 0)
        {
          builder.append(": ");
          Calendar calendar = Calendar.getInstance();
          calendar.setTimeInMillis(time);
          Date date = calendar.getTime();
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
  protected void onRestoreInstanceState(Bundle state)
  {
    super.onRestoreInstanceState(state);
    showAbout = state.getBoolean("showAbout");
    trafficDetected = state.getBoolean("trafficDetected");
    subscriptionSummary = state.getString("subscriptionSummary");
  }

  @Override
  protected void onSaveInstanceState(Bundle outState)
  {
    outState.putString("subscriptionSummary", subscriptionSummary);
    outState.putBoolean("trafficDetected", trafficDetected);
    outState.putBoolean("showAbout", showAbout);
    super.onSaveInstanceState(outState);
  }

  private ServiceConnection proxyServiceConnection = new ServiceConnection()
  {
    public void onServiceConnected(ComponentName className, IBinder service)
    {
      proxyService = ((ProxyService.LocalBinder) service).getService();
      Log.d(TAG, "Proxy service connected");

      if (!trafficDetected && proxyService.isManual())
          showConfigurationMsg(getString(R.string.msg_configuration, proxyService.port));
    }

    public void onServiceDisconnected(ComponentName className)
    {
      proxyService = null;
      Log.d(TAG, "Proxy service disconnected");
    }
  };
}
