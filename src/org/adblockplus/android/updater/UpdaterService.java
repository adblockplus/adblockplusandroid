package org.adblockplus.android.updater;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

import org.adblockplus.android.R;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

public class UpdaterService extends Service
{
	private final static String TAG = "UpdaterService";
	
	private File updateDir;
	
	@Override
	public void onCreate()
	{
		super.onCreate();
		updateDir = new File(Environment.getExternalStorageDirectory().getPath(), "downloads");
	}

	@Override
	public void onStart(Intent intent, int startId)
	{
		super.onStart(intent, startId);

		// Stop if media not available
		if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()))
		{
			stopSelf();
			return;
		}
		updateDir.mkdirs();
		
		// Start download
		new DownloadTask(this).execute(intent.getStringExtra("url"));
	}

	@Override
	public IBinder onBind(Intent intent)
	{
		return null;
	}

	private class DownloadTask extends AsyncTask<String, Integer, String>
	{
		private Context context;
		private Notification notification;
		private PendingIntent contentIntent;
		private NotificationManager notificationManager;
		private int notificationId = R.string.app_name + 2;

		public DownloadTask(Context context)
		{
			this.context = context;
			notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notification = new Notification();
            contentIntent = PendingIntent.getActivity(context, 0, new Intent(), PendingIntent.FLAG_UPDATE_CURRENT);
		}

		@Override
		protected void onPreExecute()
		{
            notification.flags |= Notification.FLAG_ONGOING_EVENT;
            notification.when = 0;
    		notification.icon = R.drawable.ic_stat_download;
    		notification.setLatestEventInfo(context, getString(R.string.app_name), String.format(getString(R.string.msg_update_downloading), 0), contentIntent);
            notificationManager.notify(notificationId, notification);
		}

		@Override
		protected String doInBackground(String... sUrl)
		{
			try
			{
				// Create connection
				URL url = new URL(sUrl[0]);
				Log.e(TAG, "D: " + sUrl[0]);
				URLConnection connection = url.openConnection();
				connection.connect();
				int fileLength = connection.getContentLength();
				Log.e(TAG, "S: " + fileLength);

				// Check if file already exists
				File updateFile = new File(updateDir, "AdblockPlus-update.apk");
				if (updateFile.exists())
				{
					Log.e(TAG, "L: " + updateFile.length());
					// TODO Should use md5 checksums, not lengths
					if (updateFile.length() == fileLength)
						return updateFile.getAbsolutePath();
					else
						updateFile.delete();
				}

				// Download the file
				InputStream input = new BufferedInputStream(url.openStream());
				OutputStream output = new FileOutputStream(updateFile);

				byte data[] = new byte[1024];
				long total = 0;
				int count;
				int progress = 0;
				while ((count = input.read(data)) != -1)
				{
					total += count;
					output.write(data, 0, count);

					int p = (int) (total * 100 / fileLength);
					if (p != progress)
					{
						publishProgress(p);
						progress = p;
					}
				}

				output.flush();
				output.close();
				input.close();
				return updateFile.getAbsolutePath();
			}
			catch (Exception e)
			{
				Log.e(TAG, "Download error", e);
				return null;
			}
		}

		@Override
		protected void onProgressUpdate(Integer... progress)
		{
    		notification.setLatestEventInfo(context, getString(R.string.app_name), String.format(getString(R.string.msg_update_downloading), progress[0]), contentIntent);
            notificationManager.notify(notificationId, notification);
		}

		@Override
		protected void onPostExecute(String result)
		{
			notificationManager.cancel(notificationId);
			if (result != null)
			{
				Notification notification = new Notification();
				notification.icon = R.drawable.ic_stat_download;
				notification.when = System.currentTimeMillis();
				notification.flags |= Notification.FLAG_AUTO_CANCEL;
				Intent intent = new Intent(context, UpdaterActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				intent.setAction("update");
				intent.putExtra("path", result);
				PendingIntent contentIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
				notification.setLatestEventInfo(context, context.getText(R.string.app_name), context.getString(R.string.msg_update_ready), contentIntent);
				notificationManager.notify(R.string.app_name + 1, notification);
			}
		}
	}
}
