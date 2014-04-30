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

import java.io.IOException;
import java.io.StringWriter;
import java.util.regex.Pattern;

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
import android.text.TextUtils;
import android.util.Log;
import android.util.Xml;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.Toast;

/**
 * Shows crash report dialog asking user to submit crash report together with comments.
 */
public final class CrashReportDialog extends Activity
{
  private static final String TAG = Utils.getTag(CrashReportDialog.class);
  private String report;

  @Override
  protected void onCreate(final Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    requestWindowFeature(Window.FEATURE_LEFT_ICON);
    setContentView(R.layout.crashreport);

    final Bundle extras = getIntent().getExtras();
    if (extras == null)
    {
      finish();
      return;
    }
    report = extras.getString("report");

    getWindow().setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, android.R.drawable.ic_dialog_alert);
  }

  public void onOk(final View v)
  {
    final String comment = ((EditText) findViewById(R.id.comments)).getText().toString();

    try
    {
      final String[] reportLines = report.split(System.getProperty("line.separator"));
      final int api = Integer.parseInt(reportLines[0]);
      final int build = Integer.parseInt(reportLines[1]);

      final XmlSerializer xmlSerializer = Xml.newSerializer();
      final StringWriter writer = new StringWriter();

      xmlSerializer.setOutput(writer);
      xmlSerializer.startDocument("UTF-8", true);
      xmlSerializer.startTag("", "crashreport");
      xmlSerializer.attribute("", "version", "1");
      xmlSerializer.attribute("", "api", String.valueOf(api));
      xmlSerializer.attribute("", "build", String.valueOf(build));
      xmlSerializer.startTag("", "error");
      xmlSerializer.attribute("", "type", reportLines[2]);
      xmlSerializer.startTag("", "message");
      xmlSerializer.text(reportLines[3]);
      xmlSerializer.endTag("", "message");
      xmlSerializer.startTag("", "stacktrace");
      final Pattern p = Pattern.compile("\\|");
      boolean hasCause = false;
      int i = 4;
      while (i < reportLines.length)
      {
        if ("cause".equals(reportLines[i]))
        {
          xmlSerializer.endTag("", "stacktrace");
          xmlSerializer.startTag("", "cause");
          hasCause = true;
          i++;
          xmlSerializer.attribute("", "type", reportLines[i]);
          i++;
          xmlSerializer.startTag("", "message");
          xmlSerializer.text(reportLines[i]);
          i++;
          xmlSerializer.endTag("", "message");
          xmlSerializer.startTag("", "stacktrace");
          continue;
        }
        Log.e(TAG, "Line: " + reportLines[i]);
        final String[] element = TextUtils.split(reportLines[i], p);
        xmlSerializer.startTag("", "frame");
        xmlSerializer.attribute("", "class", element[0]);
        xmlSerializer.attribute("", "method", element[1]);
        xmlSerializer.attribute("", "isnative", element[2]);
        xmlSerializer.attribute("", "file", element[3]);
        xmlSerializer.attribute("", "line", element[4]);
        xmlSerializer.endTag("", "frame");
        i++;
      }
      xmlSerializer.endTag("", "stacktrace");
      if (hasCause)
        xmlSerializer.endTag("", "cause");
      xmlSerializer.endTag("", "error");
      xmlSerializer.startTag("", "comment");
      xmlSerializer.text(comment);
      xmlSerializer.endTag("", "comment");
      xmlSerializer.endTag("", "crashreport");
      xmlSerializer.endDocument();

      final String xml = writer.toString();
      final HttpClient httpclient = new DefaultHttpClient();
      final HttpPost httppost = new HttpPost(getString(R.string.crash_report_url));
      httppost.setHeader("Content-Type", "text/xml; charset=UTF-8");
      httppost.addHeader("X-Adblock-Plus", "yes");
      httppost.setEntity(new StringEntity(xml));
      final HttpResponse httpresponse = httpclient.execute(httppost);
      final StatusLine statusLine = httpresponse.getStatusLine();
      Log.e(TAG, statusLine.getStatusCode() + " " + statusLine.getReasonPhrase());
      Log.e(TAG, EntityUtils.toString(httpresponse.getEntity()));
      if (statusLine.getStatusCode() != 200)
        throw new ClientProtocolException();
      final String response = EntityUtils.toString(httpresponse.getEntity());
      if (!"saved".equals(response))
        throw new ClientProtocolException();
      deleteFile(CrashHandler.REPORT_FILE);
    }
    catch (final ClientProtocolException e)
    {
      Log.e(TAG, "Failed to submit a crash", e);
      Toast.makeText(this, R.string.msg_crash_submission_failure, Toast.LENGTH_LONG).show();
    }
    catch (final IOException e)
    {
      Log.e(TAG, "Failed to submit a crash", e);
      Toast.makeText(this, R.string.msg_crash_submission_failure, Toast.LENGTH_LONG).show();
    }
    catch (final Exception e)
    {
      Log.e(TAG, "Failed to create report", e);
      // Assuming corrupted report file, just silently deleting it
      deleteFile(CrashHandler.REPORT_FILE);
    }
    finish();
  }

  public void onCancel(final View v)
  {
    deleteFile(CrashHandler.REPORT_FILE);
    finish();
  }
}
