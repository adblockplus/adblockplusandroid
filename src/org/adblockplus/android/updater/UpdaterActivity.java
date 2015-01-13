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

package org.adblockplus.android.updater;

import java.io.File;

import org.adblockplus.android.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Window;

/**
 * Prompts user to download update or installs downloaded update.
 */
public class UpdaterActivity extends Activity
{
  @Override
  public void onCreate(final Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);

    requestWindowFeature(Window.FEATURE_NO_TITLE);

    // Prompt user to download update
    if ("download".equals(getIntent().getAction()))
    {
      final Bundle extras = getIntent().getExtras();
      if (extras == null || extras.getString("url") == null)
      {
        finish();
        return;
      }

      new AlertDialog.Builder(this).setTitle(R.string.msg_update_available).setMessage(getString(R.string.msg_update_description)).setIcon(android.R.drawable.ic_dialog_info)
          .setPositiveButton(R.string.ok, new OnClickListener() {
            @Override
            public void onClick(final DialogInterface arg0, final int arg1)
            {
              // Start download service
              startService(new Intent(UpdaterActivity.this, UpdaterService.class).putExtras(extras));
              finish();
            }
          }).setNegativeButton(R.string.cancel, new OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialog, final int which)
            {
              finish();
            }
          }).setOnCancelListener(new OnCancelListener() {
            @Override
            public void onCancel(final DialogInterface dialog)
            {
              finish();
            }
          }).create().show();
    }
    // Install downloaded update
    else
    {
      final String file = getIntent().getStringExtra("path");
      final File updateFile = new File(file);
      try
      {
        final Intent installerIntent = new Intent();
        installerIntent.setAction(Intent.ACTION_VIEW);
        installerIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        installerIntent.setDataAndType(Uri.fromFile(updateFile), "application/vnd.android.package-archive");
        startActivity(installerIntent);
        android.os.Process.killProcess(android.os.Process.myPid());
      }
      catch (final Exception e)
      {
        e.printStackTrace();
      }
    }
  }

}
