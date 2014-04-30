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

import java.io.PrintWriter;
import java.lang.Thread.UncaughtExceptionHandler;

import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.util.Log;

/**
 * Writes crash data in file.
 */
public class CrashHandler implements UncaughtExceptionHandler
{
  public static final String REPORT_FILE = "AdblockPlus_Crash_Report.txt";
  private final UncaughtExceptionHandler defaultUEH;
  private NotificationManager notificationManager;
  private final Context context;

  private boolean generateReport;
  private boolean restoreProxy;
  private String host;
  private String port;
  private String excl;

  public CrashHandler(final Context context)
  {
    defaultUEH = Thread.getDefaultUncaughtExceptionHandler();
    this.context = context;
    notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    generateReport = false;
    restoreProxy = false;
  }

  public UncaughtExceptionHandler getDefault()
  {
    return defaultUEH;
  }

  @Override
  public void uncaughtException(final Thread t, final Throwable e)
  {
    if (generateReport)
      writeToFile(e, REPORT_FILE);

    if (restoreProxy)
      clearProxySettings();

    if (notificationManager != null)
    {
      try
      {
        notificationManager.cancel(ProxyService.ONGOING_NOTIFICATION_ID);
      }
      catch (final Throwable ex)
      {
        ex.printStackTrace();
      }
    }
    notificationManager = null;

    defaultUEH.uncaughtException(t, e);
  }

  public void generateReport(final boolean report)
  {
    generateReport = report;
  }

  @SuppressLint("WorldReadableFiles")
  private void writeToFile(final Throwable error, final String filename)
  {
    Log.e("DCR", "Writing crash report");
    int versionCode = -1;
    try
    {
      final PackageInfo pi = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
      versionCode = pi.versionCode;
    }
    catch (final NameNotFoundException ex)
    {
    }
    try
    {
      final PrintWriter pw = new PrintWriter(context.openFileOutput(filename, Context.MODE_WORLD_READABLE));
      // Write Android version
      pw.println(Build.VERSION.SDK_INT);
      // Write application build number
      pw.println(versionCode);

      // Write exception data
      printThrowable(error, pw);
      final Throwable cause = error.getCause();
      // Write cause data
      if (cause != null)
      {
        pw.println("cause");
        printThrowable(cause, pw);
      }
      pw.flush();
      pw.close();
    }
    catch (final Throwable e)
    {
      e.printStackTrace();
    }
  }

  private void printThrowable(final Throwable error, final PrintWriter pw)
  {
    // Use simplest format for speed - we do not have much time
    pw.println(error.getClass().getName());
    pw.println(error.getMessage());
    final StackTraceElement[] trace = error.getStackTrace();
    for (final StackTraceElement element : trace)
    {
      pw.print(element.getClassName());
      pw.print("|");
      pw.print(element.getMethodName());
      pw.print("|");
      pw.print(element.isNativeMethod());
      pw.print("|");
      pw.print(element.getFileName());
      pw.print("|");
      pw.print(element.getLineNumber());
      pw.println();
    }
  }

  public void saveProxySettings(final String host, final String port, final String excl)
  {
    Log.e("DCR", "Saving proxy " + host + ":" + port + "/" + excl);
    this.host = host;
    this.port = port;
    this.excl = excl;
    restoreProxy = true;
  }

  public void clearProxySettings()
  {
    Log.e("DCR", "Clearing proxy");
    restoreProxy = false;
    int p = 0;
    try
    {
      p = Integer.valueOf(port);
    }
    catch (final NumberFormatException e)
    {
      // ignore - no valid port, it will be correctly processed later
    }
    ProxySettings.setConnectionProxy(context, host, p, excl);
  }
}
