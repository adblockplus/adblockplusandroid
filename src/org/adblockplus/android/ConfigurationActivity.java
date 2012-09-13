package org.adblockplus.android;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class ConfigurationActivity extends Activity
{
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.configuration);
		int port = getIntent().getIntExtra("port", 0);
		String msg1 = getString(R.string.msg_notraffic);
		String msg2 = getString(R.string.msg_configuration, port);
		((TextView) findViewById(R.id.message_text)).setText(msg1 + " " + msg2);
	}

	public void onOk(View view)
	{
		finish();
	}

	public void onHelp(View view)
	{
		Uri uri = Uri.parse(getString(R.string.configuring_proxy_url));
		final Intent intent = new Intent(Intent.ACTION_VIEW, uri);
		startActivity(intent);
		finish();
	}
}
