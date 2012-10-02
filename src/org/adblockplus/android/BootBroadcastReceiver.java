package org.adblockplus.android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class BootBroadcastReceiver extends BroadcastReceiver
{
  @Override
  public void onReceive(Context context, Intent intent)
  {
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    boolean enabled = prefs.getBoolean(context.getString(R.string.pref_enabled), false);
    boolean startAtBoot = prefs.getBoolean(context.getString(R.string.pref_startatboot), context.getResources().getBoolean(R.bool.def_startatboot));
    if (enabled && startAtBoot)
      context.startService(new Intent(context, ProxyService.class));
  }
}
