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

public class UpdaterActivity extends Activity
{
	@Override
	public void onCreate(Bundle savedInstanceState)
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
			
			new AlertDialog.Builder(this)
			.setTitle(R.string.msg_update_available)
			.setMessage(getString(R.string.msg_update_description))
			.setIcon(android.R.drawable.ic_dialog_info)
			.setPositiveButton(R.string.ok, new OnClickListener() {
				@Override
				public void onClick(DialogInterface arg0, int arg1)
				{
					startService(new Intent(UpdaterActivity.this, UpdaterService.class).putExtras(extras));
					finish();
				}
			})
			.setNegativeButton(R.string.cancel, new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which)
				{
					finish();
				}
			})
			.setOnCancelListener(new OnCancelListener() {
				@Override
				public void onCancel(DialogInterface dialog)
				{
					finish();
				}
			})
			.create()
			.show();
		}
		// Install downloaded update
		else
		{
			String file = getIntent().getStringExtra("path");
			File updateFile = new File(file);
			try
			{
				Intent installerIntent = new Intent();
				installerIntent.setAction(Intent.ACTION_VIEW);
				installerIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				installerIntent.setDataAndType(Uri.fromFile(updateFile),"application/vnd.android.package-archive");
				startActivity(installerIntent);
				android.os.Process.killProcess(android.os.Process.myPid());
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}

}
