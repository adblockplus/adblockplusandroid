package org.adblockplus.android;

import java.io.IOException;
import java.io.StringWriter;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.xmlpull.v1.XmlSerializer;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.util.Xml;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.Toast;

public final class CrashReportDialog extends Activity
{
	private final static String TAG = "CrashReportDialog";
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
		String comment = ((EditText) findViewById(R.id.comments)).getText().toString();

		try
		{
			String[] reportLines = report.split(System.getProperty("line.separator"));
			int api = Integer.parseInt(reportLines[0]);
			int build = Integer.parseInt(reportLines[1]);

			XmlSerializer xmlSerializer = Xml.newSerializer();
			StringWriter writer = new StringWriter();

			xmlSerializer.setOutput(writer);
			xmlSerializer.startDocument("UTF-8", true);
			xmlSerializer.startTag("", "crashreport");
			xmlSerializer.attribute("", "version", "1");
			xmlSerializer.startTag("", "api");
			xmlSerializer.text(String.valueOf(api));
			xmlSerializer.endTag("", "api");
			xmlSerializer.startTag("", "build");
			xmlSerializer.text(String.valueOf(build));
			xmlSerializer.endTag("", "build");
			xmlSerializer.startTag("", "stacktrace");
			for (int i = 2; i < reportLines.length; i++)
			{
				xmlSerializer.text(reportLines[i]);
				xmlSerializer.text("\r\n");
			}
			xmlSerializer.endTag("", "stacktrace");
			xmlSerializer.startTag("", "comment");
			xmlSerializer.text(comment);
			xmlSerializer.endTag("", "comment");
			xmlSerializer.endTag("", "crashreport");
			xmlSerializer.endDocument();
			String xml = writer.toString();

			HttpClient httpclient = new DefaultHttpClient();
			HttpPost httppost = new HttpPost(getString(R.string.crash_report_url));
			httppost.setHeader("Content-Type", "text/xml; charset=UTF-8");
			httppost.addHeader("X_ADBLOCK_PLUS", "yes");
			httppost.setEntity(new StringEntity(xml));
			HttpResponse httpresponse = httpclient.execute(httppost);
			StatusLine statusLine = httpresponse.getStatusLine();
			if (statusLine.getStatusCode() != 200)
				throw new ClientProtocolException();
			String response = EntityUtils.toString(httpresponse.getEntity());
			if (! "saved".equals(response))
				throw new ClientProtocolException();
			deleteFile(CrashHandler.REPORT_FILE);
		}
		catch (ClientProtocolException e)
		{
			Log.e(TAG, "Failed to submit a crash", e);
			Toast.makeText(this, R.string.msg_crash_submission_failure, Toast.LENGTH_LONG).show();
		}
		catch (IOException e)
		{
			Log.e(TAG, "Failed to submit a crash", e);
			Toast.makeText(this, R.string.msg_crash_submission_failure, Toast.LENGTH_LONG).show();
		}
		catch (Exception e)
		{
			// Assuming corrupted report file, just silently deleting it
			deleteFile(CrashHandler.REPORT_FILE);			
		}
		finish();
	}

	public void onCancel(View v)
	{
		deleteFile(CrashHandler.REPORT_FILE);
		finish();
	}
}
