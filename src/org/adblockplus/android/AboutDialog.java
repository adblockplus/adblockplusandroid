package org.adblockplus.android;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.app.Dialog;
import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.text.Html;
import android.text.util.Linkify;
import android.view.Window;
import android.widget.TextView;

public class AboutDialog extends Dialog
{
	private static Context mContext = null;

	public AboutDialog(Context context)
	{
		super(context);
		mContext = context;
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.about);
		String versionName = null;
		try
		{
			versionName = mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0).versionName;
		}
		catch (NameNotFoundException ex)
		{
			versionName = "unable to retreive version";
		}

		TextView tv = (TextView) findViewById(R.id.legal_text);
		tv.setText(Html.fromHtml(readRawTextFile(R.raw.legal).replace("\\n", "<br/>")));
		tv = (TextView) findViewById(R.id.info_text);
		String info = "<h4>" + mContext.getString(R.string.app_name) + "</h4>" + mContext.getString(R.string.version) + ": " + versionName + "<br/><br/>" + readRawTextFile(R.raw.info);
		tv.setText(Html.fromHtml(info));
		Linkify.addLinks(tv, Linkify.ALL);
	}

	public static String readRawTextFile(int id)
	{
		InputStream inputStream = mContext.getResources().openRawResource(id);
		InputStreamReader in = new InputStreamReader(inputStream);
		BufferedReader buf = new BufferedReader(in);
		String line;
		StringBuilder text = new StringBuilder();
		try
		{
			while ((line = buf.readLine()) != null)
				text.append(line);
		}
		catch (IOException e)
		{
			return null;
		}
		return text.toString();
	}
}
