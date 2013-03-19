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
  public void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.proxyconfiguration);
    int port = getIntent().getIntExtra("port", 0);

    StringBuilder info = new StringBuilder();
    int textId = ProxyService.NATIVE_PROXY_SUPPORTED ? R.raw.proxysettings : R.raw.proxysettings_old;
    AdblockPlus.appendRawTextFile(this, info, textId);
    String msg = String.format(info.toString(), port);

    TextView tv = (TextView) findViewById(R.id.message_text);
    tv.setText(Html.fromHtml(msg));
    tv.setMovementMethod(LinkMovementMethod.getInstance());

    Button buttonToHide = (Button) findViewById(ProxyService.NATIVE_PROXY_SUPPORTED ? R.id.gotit : R.id.opensettings);
    buttonToHide.setVisibility(View.GONE);
  }

  public void onGotit(View view)
  {
    finish();
  }

  public void onSettings(View view)
  {
    startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
    finish();
  }
}
