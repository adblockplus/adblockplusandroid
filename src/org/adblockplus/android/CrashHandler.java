package org.adblockplus.android;

import java.io.PrintWriter;
import java.lang.Thread.UncaughtExceptionHandler;

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
  private Context mContext;

  public CrashHandler(Context context)
  {
    defaultUEH = Thread.getDefaultUncaughtExceptionHandler();
    mContext = context;
    notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
  }

  public UncaughtExceptionHandler getDefault()
  {
    return defaultUEH;
  }

  @Override
  public void uncaughtException(Thread t, Throwable e)
  {
    writeToFile(e, REPORT_FILE);
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

  private void writeToFile(Throwable error, String filename)
  {
    Log.e("DCR", "Writing crash report");
    int versionCode = -1;
    try
    {
      PackageInfo pi = mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0);
      versionCode = pi.versionCode;
    }
    catch (NameNotFoundException ex)
    {
    }
    try
    {
      PrintWriter pw = new PrintWriter(mContext.openFileOutput(filename, Context.MODE_WORLD_READABLE));
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
}
