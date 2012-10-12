package org.adblockplus.android;

import java.util.ArrayList;
import java.util.List;

import org.adblockplus.android.updater.AlarmReceiver;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.text.ClipboardManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Advanced settings UI.
 */
public class AdvancedPreferences extends SummarizedPreferences
{
  private static final String TAG = "AdvancedPreferences";

  private static final int CONFIGURATION_DIALOG = 1;

  private static ProxyService proxyService = null;
  
  private boolean hasNativeProxy;

  @Override
  public void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    
    hasNativeProxy = Build.VERSION.SDK_INT >= 12; // Honeycomb 3.1

    addPreferencesFromResource(R.xml.preferences_advanced);

    PreferenceScreen screen = getPreferenceScreen();
    if (hasNativeProxy) 
    {
      screen.removePreference(findPreference(getString(R.string.pref_proxy)));
    }
    if (getResources().getBoolean(R.bool.def_release))
    {
      screen.removePreference(findPreference(getString(R.string.pref_support)));
    }
    else
    {
      Preference prefUpdate = findPreference(getString(R.string.pref_checkupdate));
      prefUpdate.setOnPreferenceClickListener(new OnPreferenceClickListener()
      {
        public boolean onPreferenceClick(Preference preference)
        {
          Intent updater = new Intent(getApplicationContext(), AlarmReceiver.class).putExtra("notifynoupdate", true);
          sendBroadcast(updater);
          return true;
        }
      });

      Preference prefConfiguration = findPreference(getString(R.string.pref_configuration));
      prefConfiguration.setOnPreferenceClickListener(new OnPreferenceClickListener()
      {
        public boolean onPreferenceClick(Preference preference)
        {
          showDialog(CONFIGURATION_DIALOG);
          return true;
        }
      });
    }
  }

  @Override
  public void onResume()
  {
    super.onResume();
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
    int refresh = Integer.valueOf(prefs.getString(getString(R.string.pref_refresh), "0"));
    findPreference(getString(R.string.pref_wifirefresh)).setEnabled(refresh > 0);
    connect();
  }

  @Override
  public void onPause()
  {
    super.onPause();
    disconnect();
  }

  @Override
  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
  {
    if (getString(R.string.pref_refresh).equals(key))
    {
      int refresh = Integer.valueOf(sharedPreferences.getString(getString(R.string.pref_refresh), "0"));
      findPreference(getString(R.string.pref_wifirefresh)).setEnabled(refresh > 0);
    }
    if (getString(R.string.pref_crashreport).equals(key))
    {
      AdblockPlus application = AdblockPlus.getApplication();
      application.updateCrashReportStatus();
    }
    super.onSharedPreferenceChanged(sharedPreferences, key);
  }

  @Override
  protected Dialog onCreateDialog(int id)
  {
    Dialog dialog = null;
    switch (id)
    {
      case CONFIGURATION_DIALOG:
        List<String> items = new ArrayList<String>();
        int buildNumber = -1;
        try
        {
          PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
          buildNumber = pi.versionCode;
        }
        catch (NameNotFoundException e)
        {
          // ignore - this shouldn't happen
          e.printStackTrace();
        }
        items.add(String.format("API: %d Build: %d", Build.VERSION.SDK_INT, buildNumber));
        if (proxyService != null)
        {
          items.add(String.format("Local port: %d", proxyService.port));
          if (proxyService.isTransparent())
          {
            items.add("Running in root mode");
            items.add("iptables output:");
            List<String> output = proxyService.getIptablesOutput();
            if (output != null)
            {
              for (String line : output)
              {
                if (!"".equals(line))
                  items.add(line);
              }
            }
          }
          if (proxyService.isNativeProxy())
          {
            items.add("Uses native proxy");
          }
          if (hasNativeProxy)
          {
            String[] px = proxyService.getUserProxy();
            if (px != null)
            {
              items.add("System settings:");
              items.add(String.format("Host: [%s] Port: [%s] Excl: [%s]", px[0], px[1], px[2]));
            }
          }
          items.add("Proxy settings:");
          items.add(String.format("Host: [%s] Port: [%s] Excl: [%s]", proxyService.proxy.props.getProperty("adblock.proxyHost"), proxyService.proxy.props.getProperty("adblock.proxyPort"),
              proxyService.proxy.props.getProperty("adblock.proxyExcl")));
          if (proxyService.proxy.props.getProperty("adblock.auth") != null)
            items.add("Auth: yes");
        }
        else
        {
          items.add("Service not running");
        }

        ScrollView scrollPane = new ScrollView(this);
        TextView messageText = new TextView(this);
        messageText.setPadding(12, 6, 12, 6);
        messageText.setText(TextUtils.join("\n", items));
        messageText.setOnClickListener(new View.OnClickListener()
        {

          @Override
          public void onClick(View v)
          {
            ClipboardManager manager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            TextView showTextParam = (TextView) v;
            manager.setText(showTextParam.getText());
            Toast.makeText(v.getContext(), R.string.msg_clipboard, Toast.LENGTH_SHORT).show();
          }
        });
        scrollPane.addView(messageText);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(scrollPane).setTitle(R.string.configuration_name).setIcon(android.R.drawable.ic_dialog_info).setCancelable(false)
            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener()
            {
              public void onClick(DialogInterface dialog, int id)
              {
                dialog.cancel();
              }
            });
        dialog = builder.create();
        break;
    }
    return dialog;
  }

  private void connect()
  {
    bindService(new Intent(this, ProxyService.class), proxyServiceConnection, 0);
  }

  private void disconnect()
  {
    unbindService(proxyServiceConnection);
    proxyService = null;
  }

  private ServiceConnection proxyServiceConnection = new ServiceConnection()
  {
    public void onServiceConnected(ComponentName className, IBinder service)
    {
      proxyService = ((ProxyService.LocalBinder) service).getService();
      Log.d(TAG, "Proxy service connected");
    }

    public void onServiceDisconnected(ComponentName className)
    {
      proxyService = null;
      Log.d(TAG, "Proxy service disconnected");
    }
  };
}
