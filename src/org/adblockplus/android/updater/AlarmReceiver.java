package org.adblockplus.android.updater;

import java.io.IOException;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.adblockplus.android.R;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

public class AlarmReceiver extends BroadcastReceiver
{

	private static final String TAG = "AlarmReceiver";

	@Override
	public void onReceive(final Context context, final Intent intent)
	{
		Log.i(TAG, "Recurring alarm; requesting updater service");
		// Check network availability
		boolean connected = false;
		ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = null;
		if (connectivityManager != null)
		{
			networkInfo = connectivityManager.getActiveNetworkInfo();
			connected = networkInfo == null ? false : networkInfo.isConnected();
		}

		// Get update info
		if (connected)
		{
			Thread thread = new Thread(new Runnable() {
				@Override
				public void run()
				{
					try
					{
						// Read updates manifest
						DefaultHttpClient httpClient = new DefaultHttpClient();
			            HttpGet httpGet = new HttpGet(context.getString(R.string.update_url));
			 
			            HttpResponse httpResponse = httpClient.execute(httpGet);
			            HttpEntity httpEntity = httpResponse.getEntity();
			            String xml = EntityUtils.toString(httpEntity);
			            
						// Parse XML
			            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			            DocumentBuilder db = dbf.newDocumentBuilder();
			            
			            InputSource is = new InputSource();
		                is.setCharacterStream(new StringReader(xml));
		                Document doc = db.parse(is); 

		                // Find best match
		                NodeList nl = doc.getElementsByTagName("updatecheck");
		                int newBuild = -1;
		                int newApi = -1;
		                String newUrl = null;
		                for (int i = 0; i < nl.getLength(); i++)
		                {
		                	Element e = (Element) nl.item(i);
		                	String url = e.getAttribute("package");
		                	int build = Integer.parseInt(e.getAttribute("build"));
		                	int api = Integer.parseInt(e.getAttribute("api"));
		                	if (api > android.os.Build.VERSION.SDK_INT)
		                		continue;
		                	if ((build > newBuild) || (build == newBuild && api > newApi))
		                	{
		                		newBuild = build;
		                		newApi = api;
		                		newUrl = url;
		                	}
		                }
		                
						int thisBuild = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode;

						// Run updater service if newer update found
						if (thisBuild < newBuild)
						{
							NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
							Notification notification = new Notification();
							notification.icon = R.drawable.ic_stat_download;
							notification.when = System.currentTimeMillis();
							notification.flags |= Notification.FLAG_AUTO_CANCEL;
							Intent intent = new Intent(context, UpdaterActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
							intent.setAction("download");
							intent.putExtra("url", newUrl);
							intent.putExtra("build", newBuild);
							PendingIntent contentIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
							notification.setLatestEventInfo(context, context.getText(R.string.app_name), context.getString(R.string.msg_update_available), contentIntent);
							notificationManager.notify(R.string.app_name + 1, notification);
						}
					}
					catch (IOException e)
					{
					}
					catch (NumberFormatException e)
					{
					}
					catch (NameNotFoundException e)
					{
					}
					catch (ParserConfigurationException e)
					{
					}
					catch (SAXException e)
					{
						Log.e(TAG, "Error", e);
					}
				}
			});
			thread.start();
		}
	}
}
