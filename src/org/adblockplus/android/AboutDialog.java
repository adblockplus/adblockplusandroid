package org.adblockplus.android;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

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
		String versionName = "--";
		int versionCode = -1;
		try
		{
			PackageInfo pi = mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0);
			versionName = pi.versionName;
			versionCode = pi.versionCode;
		}
		catch (NameNotFoundException ex)
		{
		}

		StringBuilder info = new StringBuilder();
		info.append("<h3>");
		info.append(mContext.getString(R.string.app_name));
		info.append("</h3>");
		info.append("<p>");
		info.append(mContext.getString(R.string.version));
		info.append(": ");
		info.append(versionName);
		info.append(" ");
		info.append(mContext.getString(R.string.build));
		info.append(" ");
		info.append(versionCode);
		info.append("</p>");
		appendRawTextFile(info, R.raw.info);
		appendRawTextFile(info, R.raw.legal);
		
		TextView tv = (TextView) findViewById(R.id.about_text);
		tv.setText(Html.fromHtml(info.toString()));
		tv.setMovementMethod(LinkMovementMethod.getInstance());
	}

	public static void appendRawTextFile(StringBuilder text, int id)
	{
		InputStream inputStream = mContext.getResources().openRawResource(id);
		InputStreamReader in = new InputStreamReader(inputStream);
		BufferedReader buf = new BufferedReader(in);
		String line;
		try
		{
			while ((line = buf.readLine()) != null)
				text.append(line);
		}
		catch (IOException e)
		{
		}
	}
}
