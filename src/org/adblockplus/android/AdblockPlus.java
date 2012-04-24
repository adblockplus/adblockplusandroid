package org.adblockplus.android;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.xml.sax.SAXException;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.AssetManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

public class AdblockPlus extends Application
{
	private final static String TAG = "Application";

	private final static int MSG_TOAST = 1;

	public final static String BROADCAST_SUBSCRIPTION_STATUS = "org.adblockplus.android.subscription.status";
	
	private final static long REFRESH_PERIOD = 60 * 60 * 1000;
	
	private List<Subscription> subscriptions;
	private JSThread js;
	private boolean interactive = false;
	private Timer refreshTimer = null;

	private static AdblockPlus myself;

	public static AdblockPlus getApplication()
	{
		return myself;
	}

	public static boolean isConnected(Context context)
	{
	    ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
	    NetworkInfo networkInfo = null;
	    if (connectivityManager != null)
	    {
	        networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
	    }
	    return networkInfo == null ? false : networkInfo.isConnected();
	}

	public List<Subscription> getSubscriptions()
	{
		if (subscriptions == null)
		{
			subscriptions = new ArrayList<Subscription>();

			SAXParserFactory factory = SAXParserFactory.newInstance();
			SAXParser parser;
			try
			{
				parser = factory.newSAXParser();
				parser.parse(getAssets().open("subscriptions.xml"), new SubscriptionParser(subscriptions));
			}
			catch (ParserConfigurationException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			catch (SAXException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			catch (IOException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return subscriptions;
	}
	
	public Subscription getSubscription(String url)
	{
		List<Subscription> subscriptions = getSubscriptions();

		for (Subscription subscription : subscriptions)
		{
			if (subscription.url.equals(url))
				return subscription;
		}
		return null;
	}
	
	public void setSubscription(Subscription subscription)
	{
		/*
		Subscription test = new Subscription();
		test.url = "https://easylist-downloads.adblockplus.org/exceptionrules.txt";
		test.title = "Test";
		test.homepage = "https://easylist-downloads.adblockplus.org/";
		selectedItem = test;
		*/
		
		if (subscription != null)
		{
			final JSONObject jsonSub = new JSONObject();
			try
			{
				jsonSub.put("url", subscription.url);
				jsonSub.put("title", subscription.title);
				jsonSub.put("homepage", subscription.homepage);
				js.execute(new Runnable(){
					@Override
					public void run()
					{
						js.evaluate("clearSubscriptions()");
						js.evaluate("addSubscription(\"" + StringEscapeUtils.escapeJavaScript(jsonSub.toString()) + "\")");
					}
				});
			}
			catch (JSONException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public void refreshSubscription()
	{
		js.execute(new Runnable(){
			@Override
			public void run()
			{
				js.evaluate("refreshSubscriptions()");
			}
		});
	}

	public Subscription offerSubscription()
	{
		Subscription selectedItem = null;
		String selectedPrefix = null;
		int matchCount = 0;
		for (Subscription subscription : getSubscriptions())
		{
			if (selectedItem == null)
				selectedItem = subscription;

			String prefix = checkLocalePrefixMatch(subscription.prefixes);
			if (prefix != null)
			{
				if (selectedPrefix == null || selectedPrefix.length() < prefix.length())
				{
					selectedItem = subscription;
					selectedPrefix = prefix;
					matchCount = 1;
				}
				else if (selectedPrefix != null && selectedPrefix.length() == prefix.length())
				{
					matchCount++;

					// If multiple items have a matching prefix of the
					// same length select one of the items randomly,
					// probability should be the same for all items.
					// So we replace the previous match here with
					// probability 1/N (N being the number of matches).
					if (Math.random() * matchCount < 1)
					{
						selectedItem = subscription;
						selectedPrefix = prefix;
					}
				}
			}
		}
		return selectedItem;
	}
	
	public void checkSubscriptions()
	{
		Log.e(TAG, "checkSubsdcriptions()");
		js.execute(new Runnable(){
			@Override
			public void run()
			{
				js.evaluate("checkSubscriptions()");
			}
		});
	}

	public boolean verifySubscriptions()
	{
		Future<Boolean> future = js.submit(new Callable<Boolean>(){
			@Override
			public Boolean call() throws Exception
			{
				Boolean result = (Boolean) js.evaluate("verifySubscriptions()");
				return result;
			}
		});
		try
		{
			return future.get().booleanValue();
		}
		catch (InterruptedException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (ExecutionException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}
	
	private class MatchesCallable implements Callable<Boolean>
	{
		private String url;
		
		MatchesCallable(String url)
		{
			this.url = url;
		}
		
		@Override
		public Boolean call() throws Exception
		{
			// SCRIPT
		    // OTHER
		    // IMAGE
		    // STYLESHEET
		    // SUBDOCUMENT
		    // MEDIA
		    // FONT
			Boolean result = (Boolean) js.evaluate("defaultMatcher.matchesAny('" + url + "', 'SCRIPT', null, false) != null;");
			return result;
		}
	}
	
	public boolean matches(String url) throws Exception
	{
		Callable<Boolean> callable = new MatchesCallable(url);		
		Future<Boolean> future = js.submit(callable);		
		return future.get().booleanValue();
	}
	
	public void startInteractive()
	{
		// TODO not used
		interactive = true;
	}
	
	public void stopInteractive()
	{
		// TODO not used
		interactive = false;
	}
	
	public String checkLocalePrefixMatch(String[] prefixes)
	{
		if (prefixes == null || prefixes.length == 0)
			return null;

		String locale = Locale.getDefault().toString().toLowerCase();

		for (int i = 0; i < prefixes.length; i++)
			if (locale.startsWith(prefixes[i].toLowerCase()))
				return prefixes[i];

		return null;
	}
	
	public void startEngine()
	{
		if (js == null)
		{
			Log.e(TAG, "startEngine");
			js = new JSThread(this);
			js.start();
			updateRefresh(true);
		}
	}
	
	public void stopEngine(boolean implicitly)
	{
		if ((implicitly || ! interactive) && js != null)
		{
			Log.e(TAG, "stopEngine " + implicitly + " " + interactive);
			stopRefresh();
			js.stopEngine();
			try
			{
				js.join();
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}
			js = null;
		}
	}
	
	public void updateRefresh()
	{
		updateRefresh(false);
	}

	private void updateRefresh(boolean starting)
	{
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		final int refresh = Integer.valueOf(prefs.getString(getString(R.string.pref_refresh), "0"));
		final boolean wifionly = prefs.getBoolean(getString(R.string.pref_wifirefresh), getResources().getBoolean(R.bool.def_wifirefresh));
		if (refresh == 1 && starting && (! wifionly || isConnected(this)))
		{
			refreshSubscription();
		}
		else if (refresh == 2 && refreshTimer == null)
		{
			refreshTimer = new Timer();
			refreshTimer.schedule(new TimerTask() {
				@Override
				public void run()
				{
					if (! wifionly || isConnected(AdblockPlus.this))
						checkSubscriptions();					
					
				}
			}, 100, REFRESH_PERIOD);
		}
		else if (refresh < 2)
		{
			stopRefresh();
		}
	}
	
	private void stopRefresh()
	{
		if (refreshTimer != null)
		{
			refreshTimer.cancel();
			refreshTimer.purge();
			refreshTimer = null;
		}
	}
	
	@Override
	public void onCreate()
	{
		super.onCreate();
		myself = this;
	}

	private final Handler messageHandler = new Handler()
	{
		public void handleMessage(Message msg)
		{
			if (msg.what == MSG_TOAST)
			{
				Toast.makeText(AdblockPlus.this, msg.getData().getString("message"), Toast.LENGTH_LONG).show();
			}
		}
	};

	private final class JSThread extends Thread
	{
		private JSEngine jsEngine;
		private volatile boolean run = true;
		private Context context;
		private final LinkedList<Runnable> queue = new LinkedList<Runnable>();
		private long delay = -1;
		
		JSThread(Context context)
		{
			this.context = context;
		}

		// JS helper
		@SuppressWarnings("unused")
		public String readJSFile(String name)
		{
			String result = "";
			AssetManager assetManager = getAssets();
			try
			{
				InputStreamReader reader = new InputStreamReader(assetManager.open("js" + File.separator + name));
				final char[] buffer = new char[0x10000];
				StringBuilder out = new StringBuilder();
				int read;
				do
				{
					read = reader.read(buffer, 0, buffer.length);
					if (read > 0)
						out.append(buffer, 0, read);
				}
				while (read >= 0);
				result = out.toString();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
			return result;
		}

		// JS helper
		public FileInputStream getInputStream(String path)
		{
			Log.e(TAG, path);
			File f = new File(path);
			try
			{
				return openFileInput(f.getName());
			}
			catch (FileNotFoundException e)
			{
				e.printStackTrace();
			}
			return null;
		}

		// JS helper
		public FileOutputStream getOutputStream(String path)
		{
			Log.e(TAG, path);
			File f = new File(path);
			try
			{
				return openFileOutput(f.getName(), MODE_PRIVATE);
			}
			catch (FileNotFoundException e)
			{
				e.printStackTrace();
			}
			return null;
		}

		// JS helper
		public String getVersion()
		{
			String versionName = null;
			try
			{
				versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
			}
			catch (NameNotFoundException ex)
			{
				versionName = "n/a";
			}
			return versionName;
		}

		// JS helper
		@SuppressWarnings("unused")
		public void httpSend(final String method, final String url, final String[][] headers, final boolean async, final long callback)
		{
			Log.e(TAG, "httpSend('" + method + "', '" + url + "')");
			messageHandler.post(new Runnable(){
				@Override
				public void run() {
					try
					{
						Task task = new Task();
						task.callback = callback;
						task.connection = (HttpURLConnection) new URL(url).openConnection();
						task.connection.setRequestMethod(method);
						for (int i = 0; i < headers.length; i++)
						{
							task.connection.setRequestProperty(headers[i][0], headers[i][1]);
						}
						DownloadTask downloadTask = new DownloadTask(context);
						downloadTask.execute(task);
						if (! async)
						{
							downloadTask.get();
						}
					}
					catch (Exception e)
					{
						e.printStackTrace();
						js.callback(callback, null);
					}
				}
			});
		}

		// JS helper
		@SuppressWarnings("unused")
		public void setStatus(String text, long time)
		{
			sendBroadcast(new Intent(BROADCAST_SUBSCRIPTION_STATUS).putExtra("text", text).putExtra("time", time));
		}

		// JS helper
		@SuppressWarnings("unused")
		public void showToast(String text)
		{
			Log.e(TAG, "Toast: " + text);
			Message msg = messageHandler.obtainMessage(MSG_TOAST);
			Bundle data = new Bundle();
			data.putString("message", text);
			msg.setData(data);
			messageHandler.sendMessage(msg);
		}
		
		//JS helper
		@SuppressWarnings("unused")
		public void notify(long delay)
		{
			if (this.delay < 0 || delay < this.delay)
			{
				this.delay = delay;
		        synchronized (queue)
		        {
		        	queue.notify();
		        }
			}
		}
		
		public Object evaluate(String script)
		{
			return jsEngine.evaluate(script);
		}
		
		public void callback(long callback, Object[] params)
		{
			jsEngine.callback(callback, params);
		}

		public final void stopEngine()
		{
			run = false;
	        synchronized (queue)
	        {
	            queue.notify();
	        }
		}

	    public void execute(Runnable r)
	    {
	        synchronized (queue)
	        {
	            queue.addLast(r);
	            queue.notify();
	        }
	    }
	    
	    public <T> Future<T> submit(Callable<T> callable)
	    {
	    	FutureTask<T> ftask = new FutureTask<T>(callable);
	        execute(ftask);
	        return ftask;
	    }

		@Override
		public final void run()
		{			
			jsEngine = new JSEngine(this);
			
			jsEngine.put("_locale", Locale.getDefault().toString());
			jsEngine.put("_datapath", getFilesDir().getAbsolutePath());
			jsEngine.put("_separator", File.separator);
			jsEngine.put("_version", getVersion());

			try
			{
				jsEngine.evaluate("Android.load(\"start.js\");");
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}

			while (run)
			{
				try
				{
					Runnable r = null;
					synchronized (queue)
					{
						// Log.e(TAG, "run " + queue.isEmpty() + " " + delay);
						r = queue.poll();
					}
					if (r != null)
					{
						r.run();
					}
					else if (delay > 0)
					{
						synchronized (queue)
						{
							try
							{
								queue.wait(delay);
							}
							catch (InterruptedException e)
							{
							}
						}
						delay = 0;
					}
					else if (delay == 0)
					{
						delay = jsEngine.runCallbacks();
					}
					else
					{
						synchronized (queue)
						{
							try
							{
								queue.wait();
							}
							catch (InterruptedException e)
							{
							}
						}
					}
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}

			jsEngine.release();
		}
	}
	
	private class Task
	{
		HttpURLConnection connection;
		long callback;
	}
	
	private class Result
	{
		long callback;
		int code;
		String message;
		String data;
	    Map<String, List<String>> headers;
	}
	
	private class DownloadTask extends AsyncTask<Task, Integer, Result>
	{
	    public DownloadTask(Context context)
	    {
	    }

	    @Override
	    protected void onPreExecute()
	    {
	    }

	    @Override
		protected void onPostExecute(Result result)
	    {
	    	if (result != null)
	    	{
		    	final long callback = result.callback;
				final Object[] params = new Object[4];
				
				String[][] headers = null;
				if (result.headers != null)
				{
					headers = new String[result.headers.size()][2];
					int i = 0;
			        for (String header: result.headers.keySet())
			        { 
			        	headers[i][0] = header;
			        	headers[i][1] = StringUtils.join(result.headers.get(header).toArray(), "; ");
			        	i++;
			        }
				}
		        params[0] = result.code;
		        params[1] = result.message;
				params[2] = headers;
				params[3] = result.data;
				js.execute(new Runnable(){
					@Override
					public void run()
					{
						js.callback(callback, params);
					}
					
				});
	    	}
	    }

		@Override
	    protected void onCancelled()
	    {
	    }

	    @Override
	    protected Result doInBackground(Task... tasks)
	    {
	    	Task task = tasks[0];
        	Result result = new Result();
        	result.callback = task.callback;
	        try {
	        	HttpURLConnection connection = task.connection;
	            connection.connect();
	            int lenghtOfFile = connection.getContentLength();
	            Log.w("D", "S: " + lenghtOfFile);

	            result.code = connection.getResponseCode();
	            result.message = connection.getResponseMessage();
	            result.headers = connection.getHeaderFields();

	            // download the file
	            String encoding = connection.getContentEncoding();
	            if (encoding == null)
	            	encoding = "utf-8";
	            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream(), encoding));

				final char[] buffer = new char[0x10000];
				StringBuilder out = new StringBuilder();
	            long total = 0;
				int read;
				do
				{
					read = in.read(buffer, 0, buffer.length);
					if (read > 0)
					{
						out.append(buffer, 0, read);
		                total += read;
		                publishProgress((int)(total*100./lenghtOfFile));
					}
				}
				while (! isCancelled() && read >= 0);
				result.data = out.toString();
	            in.close();
	        }
	        catch (Exception e)
	        {
				e.printStackTrace();
	        	result.data = "";
	        	result.code = HttpURLConnection.HTTP_INTERNAL_ERROR;
	        	result.message = e.toString();
	        }
	        return result;
	    }
	    
	    protected void onProgressUpdate(Integer... progress)
	    {
	    	Log.i("HTTP", "Progress: " + progress[0].intValue());
	    }	
	}
}
