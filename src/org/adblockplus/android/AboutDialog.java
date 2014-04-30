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

import android.app.Dialog;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.Window;
import android.widget.TextView;

public class AboutDialog extends Dialog
{
  private static Context context = null;

  public AboutDialog(final Context context)
  {
    super(context);
    AboutDialog.context = context;
  }

  @Override
  public void onCreate(final Bundle savedInstanceState)
  {
    requestWindowFeature(Window.FEATURE_NO_TITLE);
    setContentView(R.layout.about);

    // Get package version code and name
    String versionName = "--";
    int versionCode = -1;
    try
    {
      final PackageInfo pi = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
      versionName = pi.versionName;
      versionCode = pi.versionCode;
    }
    catch (final NameNotFoundException ex)
    {
      // ignore - it can not happen because we query information about ourselves
    }

    // Construct html
    final StringBuilder info = new StringBuilder();
    info.append("<h3>");
    info.append(context.getString(R.string.app_name));
    info.append("</h3>");
    info.append("<p>");
    info.append(context.getString(R.string.version));
    info.append(": ");
    info.append(versionName);
    info.append(" ");
    info.append(context.getString(R.string.build));
    info.append(" ");
    info.append(versionCode);
    info.append("</p>");
    AdblockPlus.appendRawTextFile(context, info, R.raw.info);
    AdblockPlus.appendRawTextFile(context, info, R.raw.legal);

    // Show text
    final TextView tv = (TextView) findViewById(R.id.about_text);
    tv.setText(Html.fromHtml(info.toString()));
    tv.setMovementMethod(LinkMovementMethod.getInstance());
  }
}
