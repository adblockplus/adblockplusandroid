package org.adblockplus.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;

public final class CrashReportDialog extends Activity
{
	private String report;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_LEFT_ICON);
		setContentView(R.layout.crashreport);
		
		Bundle extras = getIntent().getExtras();
		if (extras == null)
		{
			finish();
			return;
		}
		report = extras.getString("report");
		
		getWindow().setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, android.R.drawable.ic_dialog_alert);
	}
	
	public void onOk(View v)
	{
		Intent share = new Intent(Intent.ACTION_SEND);
		share.setType("text/plain");
		share.putExtra(Intent.EXTRA_TEXT, report);
		startActivity(Intent.createChooser(share, getString(R.string.crash_dialog_chooser_title)));
		finish();
	}

	public void onCancel(View v)
	{
		finish();
	}
}
