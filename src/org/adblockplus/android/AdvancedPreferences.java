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

import java.util.ArrayList;
import java.util.List;

import org.adblockplus.android.compat.ProxyProperties;
import org.adblockplus.android.configurators.IptablesProxyConfigurator;
import org.apache.commons.lang.StringUtils;

import org.jraf.android.backport.switchwidget.SwitchPreference;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.text.ClipboardManager;
import android.text.TextUtils;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Advanced settings UI.
 */
public class AdvancedPreferences extends SummarizedPreferences
{
  private static final String TAG = Utils.getTag(AdvancedPreferences.class);

  private static final int CONFIGURATION_DIALOG = 1;

  private ServiceBinder serviceBinder = null;

  @Override
  public void onCreate(final Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);

    addPreferencesFromResource(R.xml.preferences_advanced);

    final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

    this.serviceBinder = new ServiceBinder(this);

    final AdblockPlus application = AdblockPlus.getApplication();
    final PreferenceScreen screen = getPreferenceScreen();
    if (ProxyService.GLOBAL_PROXY_USER_CONFIGURABLE)
    {
      screen.removePreference(findPreference(getString(R.string.pref_proxy)));
      if (prefs.getBoolean(getString(R.string.pref_proxyautoconfigured), false))
      {
        final Preference loggingPreferenceCategory = findPreference(getString(R.string.pref_logging));
        // Remove dependency on pref_proxyenabled
        loggingPreferenceCategory.setDependency(null);
        loggingPreferenceCategory.setEnabled(application.isServiceRunning());

        screen.removePreference(findPreference(getString(R.string.pref_proxyenabled)));
      }
    }
    if (getResources().getBoolean(R.bool.def_release))
    {
      screen.removePreference(findPreference(getString(R.string.pref_support)));
    }
    else
    {
      final Preference prefUpdate = findPreference(getString(R.string.pref_checkupdate));
      prefUpdate.setOnPreferenceClickListener(new OnPreferenceClickListener()
      {
        @Override
        public boolean onPreferenceClick(final Preference preference)
        {
          final AdblockPlus application = AdblockPlus.getApplication();
          application.checkUpdates();
          return true;
        }
      });

      final Preference prefConfiguration = findPreference(getString(R.string.pref_configuration));
      prefConfiguration.setOnPreferenceClickListener(new OnPreferenceClickListener()
      {
        @Override
        public boolean onPreferenceClick(final Preference preference)
        {
          showDialog(CONFIGURATION_DIALOG);
          return true;
        }
      });
    }
    ((SwitchPreference) findPreference(getString(R.string.pref_loggingenabled))).
        setChecked(application.isLoggingEnabled());
  }

  @Override
  public void onResume()
  {
    super.onResume();
    final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
    final int refresh = Integer.valueOf(prefs.getString(getString(R.string.pref_refresh), "0"));
    findPreference(getString(R.string.pref_wifirefresh)).setEnabled(refresh > 0);
    this.serviceBinder.bind();
  }

  @Override
  public void onPause()
  {
    super.onPause();
    this.serviceBinder.unbind();
  }

  @Override
  public void onSharedPreferenceChanged(final SharedPreferences sharedPreferences, final String key)
  {
    if (getString(R.string.pref_proxyenabled).equals(key))
    {
      final AdblockPlus application = AdblockPlus.getApplication();
      final boolean enabled = sharedPreferences.getBoolean(key, false);
      final boolean serviceRunning = application.isServiceRunning();
      if (enabled)
      {
        if (!serviceRunning)
          startService(new Intent(this, ProxyService.class));
      }
      else
      {
        if (serviceRunning)
          stopService(new Intent(this, ProxyService.class));
        // If disabled, disable filtering as well
        final SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(getString(R.string.pref_enabled), false);
        editor.commit();
        application.setFilteringEnabled(false);

        ((SwitchPreference) findPreference(getString(R.string.pref_loggingenabled))).setChecked(false);
      }
    }
    else if (getString(R.string.pref_refresh).equals(key))
    {
      final int refresh = Integer.valueOf(sharedPreferences.getString(key, "0"));
      findPreference(getString(R.string.pref_wifirefresh)).setEnabled(refresh > 0);
    }
    else if (getString(R.string.pref_crashreport).equals(key))
    {
      final boolean report = sharedPreferences.getBoolean(key, getResources().getBoolean(R.bool.def_crashreport));
      try
      {
        final CrashHandler handler = (CrashHandler) Thread.getDefaultUncaughtExceptionHandler();
        handler.generateReport(report);
      }
      catch (final ClassCastException e)
      {
        // ignore - default handler in use
      }
    }
    else if (getString(R.string.pref_loggingenabled).equals(key))
    {
      final boolean enabled = sharedPreferences.getBoolean(key, false);
      AdblockPlus.getApplication().setLoggingEnabled(enabled);
    }
    super.onSharedPreferenceChanged(sharedPreferences, key);
  }

  @Override
  protected Dialog onCreateDialog(final int id)
  {
    Dialog dialog = null;
    switch (id)
    {
      case CONFIGURATION_DIALOG:
        final List<String> items = new ArrayList<String>();
        items.add(AdblockPlus.getDeviceName());
        items.add(String.format("API: %d Build: %d", Build.VERSION.SDK_INT, AdblockPlus.getApplication().getBuildNumber()));

        final ProxyService proxyService = this.serviceBinder.get();

        if (proxyService != null)
        {
          items.add(String.format("Local port: %d", proxyService.port));
          if (proxyService.isIptables())
          {
            items.add("Running in root mode");
            items.add("iptables output:");
            final List<String> output = IptablesProxyConfigurator.getIptablesOutput(getApplicationContext());
            if (output != null)
            {
              for (final String line : output)
              {
                if (StringUtils.isNotEmpty(line))
                  items.add(line);
              }
            }
          }
          if (proxyService.isNativeProxyAutoConfigured())
          {
            items.add("Has native proxy auto configured");
          }
          if (ProxyService.GLOBAL_PROXY_USER_CONFIGURABLE)
          {
            final ProxyProperties pp = ProxyProperties.fromContext(getApplicationContext());
            if (pp != null)
            {
              items.add("System settings:");
              items.add(String.format("Host: [%s] Port: [%d] Excl: [%s]", pp.getHost(), pp.getPort(), pp.getExclusionList()));
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

        final ScrollView scrollPane = new ScrollView(this);
        final TextView messageText = new TextView(this);
        messageText.setPadding(12, 6, 12, 6);
        messageText.setText(TextUtils.join("\n", items));
        messageText.setOnClickListener(new View.OnClickListener()
        {
          @Override
          public void onClick(final View v)
          {
            final ClipboardManager manager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            final TextView showTextParam = (TextView) v;
            manager.setText(showTextParam.getText());
            Toast.makeText(v.getContext(), R.string.msg_clipboard, Toast.LENGTH_SHORT).show();
          }
        });
        scrollPane.addView(messageText);

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(scrollPane)
            .setTitle(R.string.configuration_name)
            .setIcon(android.R.drawable.ic_dialog_info)
            .setCancelable(false)
            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener()
            {
              @Override
              public void onClick(final DialogInterface dialog, final int id)
              {
                dialog.cancel();
              }
            });
        dialog = builder.create();
        break;
    }
    return dialog;
  }
}
