package org.adblockplus.android;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.Thread.UncaughtExceptionHandler;

import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.util.Log;

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
		final Writer result = new StringWriter();
		final PrintWriter printWriter = new PrintWriter(result);
		e.printStackTrace(printWriter);
		String stacktrace = result.toString();
		printWriter.close();

		writeToFile(stacktrace, REPORT_FILE);
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

	private void writeToFile(String stacktrace, String filename)
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
			pw.println(Build.VERSION.SDK_INT);
			pw.println(versionCode);
			pw.print(stacktrace);
			pw.flush();
			pw.close();
		}
		catch (Throwable e)
		{
			e.printStackTrace();
		}
	}

}
