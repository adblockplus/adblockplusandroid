package org.adblockplus.android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class Starter extends BroadcastReceiver
{

  @Override
  public void onReceive(Context context, Intent intent)
  {
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    boolean enabled = prefs.getBoolean(context.getString(R.string.pref_enabled), false);
    if (Intent.ACTION_PACKAGE_REPLACED.equals(intent.getAction()))
    {
      enabled &= "org.adblockplus.android".equals(intent.getData().getSchemeSpecificPart());
    }
    if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()))
    {
      boolean startAtBoot = prefs.getBoolean(context.getString(R.string.pref_startatboot), context.getResources().getBoolean(R.bool.def_startatboot));
      enabled &= startAtBoot;
    }
    if (enabled)
      context.startService(new Intent(context, ProxyService.class));
  }

}
