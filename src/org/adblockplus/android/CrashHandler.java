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
  private UncaughtExceptionHandler defaultUEH;
  private NotificationManager notificationManager;
  private Context context;

  private boolean generateReport;
  private boolean restoreProxy;
  private String host;
  private String port;
  private String excl;

  public CrashHandler(Context context)
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
  public void uncaughtException(Thread t, Throwable e)
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
      catch (Throwable ex)
      {
        ex.printStackTrace();
      }
    }
    notificationManager = null;

    defaultUEH.uncaughtException(t, e);
  }

  public void generateReport(boolean report)
  {
    generateReport = report;
  }

  @SuppressLint("WorldReadableFiles")
  private void writeToFile(Throwable error, String filename)
  {
    Log.e("DCR", "Writing crash report");
    int versionCode = -1;
    try
    {
      PackageInfo pi = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
      versionCode = pi.versionCode;
    }
    catch (NameNotFoundException ex)
    {
    }
    try
    {
      PrintWriter pw = new PrintWriter(context.openFileOutput(filename, Context.MODE_WORLD_READABLE));
      // Write Android version
      pw.println(Build.VERSION.SDK_INT);
      // Write application build number
      pw.println(versionCode);

      // Write exception data
      printThrowable(error, pw);
      Throwable cause = error.getCause();
      // Write cause data
      if (cause != null)
      {
        pw.println("cause");
        printThrowable(cause, pw);
      }
      pw.flush();
      pw.close();
    }
    catch (Throwable e)
    {
      e.printStackTrace();
    }
  }

  private void printThrowable(Throwable error, PrintWriter pw)
  {
    // Use simplest format for speed - we do not have much time
    pw.println(error.getClass().getName());
    pw.println(error.getMessage());
    StackTraceElement[] trace = error.getStackTrace();
    for (StackTraceElement element : trace)
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

  public void saveProxySettings(String host, String port, String excl)
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
    catch (NumberFormatException e)
    {
      // ignore - no valid port, it will be correctly processed later
    }
    ProxySettings.setConnectionProxy(context, host, p, excl);
  }
}
