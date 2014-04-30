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

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.widget.TextView;

/**
 * Displays configuration warning message.
 */
public class ConfigurationActivity extends Activity
{
  private int port;

  @Override
  public void onCreate(final Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.configuration);
    port = getIntent().getIntExtra("port", 0);
    final String msg1 = getString(R.string.msg_notraffic);
    final String msg2 = getString(R.string.msg_configuration);
    ((TextView) findViewById(R.id.message_text)).setText(Html.fromHtml(msg1 + " " + msg2));
  }

  public void onOk(final View view)
  {
    finish();
  }

  public void onHelp(final View view)
  {
    final Intent intent;
    if (ProxyService.NATIVE_PROXY_SUPPORTED)
    {
      intent = new Intent(this, ProxyConfigurationActivity.class).putExtra("port", port);
    }
    else
    {
      final Uri uri = Uri.parse(getString(R.string.configuring_proxy_url));
      intent = new Intent(Intent.ACTION_VIEW, uri);
    }
    startActivity(intent);
    finish();
  }
}
