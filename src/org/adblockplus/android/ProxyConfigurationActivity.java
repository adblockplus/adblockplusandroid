package org.adblockplus.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class ProxyConfigurationActivity extends Activity
{
  @Override
  public void onCreate(final Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.proxyconfiguration);
    final int port = getIntent().getIntExtra("port", 0);

    final StringBuilder info = new StringBuilder();
    final int textId = ProxyService.NATIVE_PROXY_SUPPORTED ? R.raw.proxysettings : R.raw.proxysettings_old;
    AdblockPlus.appendRawTextFile(this, info, textId);
    final String msg = String.format(info.toString(), port);

    final TextView tv = (TextView) findViewById(R.id.message_text);
    tv.setText(Html.fromHtml(msg));
    tv.setMovementMethod(LinkMovementMethod.getInstance());

    final Button buttonToHide = (Button) findViewById(ProxyService.NATIVE_PROXY_SUPPORTED ? R.id.gotit : R.id.opensettings);
    buttonToHide.setVisibility(View.GONE);
  }

  public void onGotit(final View view)
  {
    finish();
  }

  public void onSettings(final View view)
  {
    startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
    finish();
  }
}
