package org.adblockplus.android;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeoutException;

import com.stericson.RootTools.RootTools;
import com.stericson.RootTools.RootToolsException;

import sunlabs.brazil.server.Server;
import sunlabs.brazil.util.Base64;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

public class ProxyService extends Service
{
	static
	{
		RootTools.debugMode = false;
	}
	
	private static final String TAG = "ProxyService";

	public final static int DEFAULT_TIMEOUT = 3000;

	public final static String BROADCAST_PROXY_FAILED = "org.adblockplus.android.proxy.failure";

	private final static String IPTABLES_RETURN = " -t nat -m owner --uid-owner {{UID}} -A OUTPUT -p tcp -j RETURN\n";
	private final static String IPTABLES_ADD_HTTP = " -t nat -A OUTPUT -p tcp --dport 80 -j REDIRECT --to {{PORT}}\n";

	private static final Class<?>[] mSetForegroundSignature = new Class[] { boolean.class };
	private static final Class<?>[] mStartForegroundSignature = new Class[] { int.class, Notification.class };
	private static final Class<?>[] mStopForegroundSignature = new Class[] { boolean.class };

	private ConnectivityManager mCM;
	private NotificationManager mNM;
	private Method mSetForeground;
	private Method mStartForeground;
	private Method mStopForeground;
	private Object[] mSetForegroundArgs = new Object[1];
	private Object[] mStartForegroundArgs = new Object[2];
	private Object[] mStopForegroundArgs = new Object[1];

	private ProxyServer proxy = null;
	private int port;

	private boolean isTransparent = false;
	private boolean isNativeProxy = false;

	private String iptables = null;

	@Override
	public void onCreate()
	{
		super.onCreate();

		initForegroundCompat();

		this.getFilesDir();

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		String p = prefs.getString(getString(R.string.pref_port), null);
		try
		{
			port = p != null ? Integer.valueOf(p): getResources().getInteger(R.integer.def_port);
		}
		catch (NumberFormatException e)
		{
			Toast.makeText(this, getString(R.string.msg_badport) + ": " + p, Toast.LENGTH_LONG).show();
			port = getResources().getInteger(R.integer.def_port);
		}

		mCM = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

		// Try to read user proxy settings
		String proxyHost = null;
		String proxyPort = null;
		String proxyExcl = null;
		String proxyUser = null;
		String proxyPass = null;
		
		if (Build.VERSION.SDK_INT >= 12) // Honeycomb 3.1
		{
			proxyHost = System.getProperty("http.proxyHost");
			proxyPort = System.getProperty("http.proxyPort");
			proxyExcl = System.getProperty("http.nonProxyHosts");

			Log.e(TAG, "PRX: " + proxyHost+":"+proxyPort+"("+proxyExcl+")");			
			String[] px = getUserProxy();
			if (px != null)
				Log.e(TAG, "PRX: " + px[0]+":"+px[1]+"("+px[2]+")");			
		}
		else
		{
			proxyHost = prefs.getString(getString(R.string.pref_proxyhost), null);
			proxyPort = prefs.getString(getString(R.string.pref_proxyport), null);
			proxyUser = prefs.getString(getString(R.string.pref_proxyuser), null);
			proxyPass = prefs.getString(getString(R.string.pref_proxypass), null);
		}

		// Try to set native proxy
		isNativeProxy = setConnectionProxy();
		if (isNativeProxy)
		{
			registerReceiver(connectionReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
			registerReceiver(connectionReceiver, new IntentFilter("android.net.wifi.LINK_CONFIGURATION_CHANGED"));
		}
		
		if (! isNativeProxy && RootTools.isAccessGiven())
		{
			try
			{
				iptables = getIptables();
				if (iptables != null)
				{
					StringBuffer cmd = new StringBuffer();
					int uid = getPackageManager().getPackageInfo(getPackageName(), 0).applicationInfo.uid;
					cmd.append(iptables);
					cmd.append(IPTABLES_RETURN.replace("{{UID}}", String.valueOf(uid)));
					cmd.append(iptables);
					cmd.append(IPTABLES_ADD_HTTP.replace("{{PORT}}", String.valueOf(port)));
					String rules = cmd.toString();
					RootTools.sendShell(rules, DEFAULT_TIMEOUT);
					isTransparent = true;
				}
			}
			catch (NameNotFoundException e)
			{
				e.printStackTrace();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
			catch (RootToolsException e)
			{
				e.printStackTrace();
			}
			catch (TimeoutException e)
			{
				e.printStackTrace();
			}
		}

		// Start engine
		AdblockPlus.getApplication().startEngine();

		registerReceiver(proxyReceiver, new IntentFilter(ProxyService.BROADCAST_PROXY_FAILED));

		// Start proxy
		if (proxy == null)
		{
			ServerSocket listen = null;
			try
			{
				//TODO Add port travel
				listen = new ServerSocket(port, 1024);
			}
			catch (IOException e)
			{
				sendBroadcast(new Intent(BROADCAST_PROXY_FAILED).putExtra("msg", e.getMessage()));
				Log.e(TAG, null, e);
				return;
			}

			Properties config = new Properties();
			config.put("handler", "main");
			config.put("main.prefix", "");
			config.put("main.class", "sunlabs.brazil.server.ChainHandler");
			if (isTransparent)
			{
				config.put("main.handlers", "urlmodifier adblock");
				config.put("urlmodifier.class", "org.adblockplus.brazil.TransparentProxyHandler");
			}
			else
			{
				config.put("main.handlers", "https adblock");
				config.put("https.class", "org.paw.handler.SSLConnectionHandler");
			}
			config.put("adblock.class", "org.adblockplus.brazil.RequestHandler");
			//config.put("adblock.proxylog", "yes");
			
			configureUserProxy(config, proxyHost, proxyPort, proxyExcl, proxyUser, proxyPass);
			
			proxy = new ProxyServer();
			proxy.logLevel = Server.LOG_DIAGNOSTIC;
			proxy.setup(listen, config.getProperty("handler"), config);
			proxy.start();
		}
		
		// Lock service
		String msg = getString(isTransparent ? R.string.notif_transparent : isNativeProxy ? R.string.notif_native : R.string.notif_proxy);
		if (! isTransparent && ! isNativeProxy)
			msg = String.format(msg, port);
		Notification notification = new Notification();
		notification.when = 0;
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, Preferences.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK), 0);
		notification.icon = R.drawable.ic_stat_blocking;
		notification.setLatestEventInfo(getApplicationContext(), getText(R.string.app_name), msg, contentIntent);
		startForegroundCompat(R.string.app_name, notification);
	}

	@Override
	public void onDestroy()
	{
		super.onDestroy();

		unregisterReceiver(proxyReceiver);

		// Stop IP redirecting
		if (isTransparent)
		{
			new Thread() {
				@Override
				public void run()
				{
					try
					{
						RootTools.sendShell(iptables + " -t nat -F OUTPUT", DEFAULT_TIMEOUT);
					}
					catch (Exception e)
					{
						e.printStackTrace();
					}
				}
			}.start();
		}

		// Clear native proxy
		if (isNativeProxy)
		{
			unregisterReceiver(connectionReceiver);
			clearConnectionProxy();
		}
		
		// Stop proxy server
		proxy.close();

		// Stop engine if not in interactive mode
		AdblockPlus.getApplication().stopEngine(false);
		
		// Release service lock
		stopForegroundCompat(R.string.app_name);
	}

	void initForegroundCompat()
	{
		mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		try
		{
			mStartForeground = getClass().getMethod("startForeground", mStartForegroundSignature);
			mStopForeground = getClass().getMethod("stopForeground", mStopForegroundSignature);
			return;
		}
		catch (NoSuchMethodException e)
		{
			// Running on an older platform.
			mStartForeground = mStopForeground = null;
		}
		try
		{
			mSetForeground = getClass().getMethod("setForeground", mSetForegroundSignature);
		}
		catch (NoSuchMethodException e)
		{
			throw new IllegalStateException("OS doesn't have Service.startForeground OR Service.setForeground!");
		}
	}

	/**
	 * This is a wrapper around the new startForeground method, using the older
	 * APIs if it is not available.
	 */
	void startForegroundCompat(int id, Notification notification)
	{
		// If we have the new startForeground API, then use it.
		if (mStartForeground != null)
		{
			mStartForegroundArgs[0] = Integer.valueOf(id);
			mStartForegroundArgs[1] = notification;
			invokeMethod(mStartForeground, mStartForegroundArgs);
			return;
		}

		// Fall back on the old API.
		mSetForegroundArgs[0] = Boolean.TRUE;
		invokeMethod(mSetForeground, mSetForegroundArgs);
		mNM.notify(id, notification);
	}

	/**
	 * This is a wrapper around the new stopForeground method, using the older
	 * APIs if it is not available.
	 */
	void stopForegroundCompat(int id)
	{
		// If we have the new stopForeground API, then use it.
		if (mStopForeground != null)
		{
			mStopForegroundArgs[0] = Boolean.TRUE;
			invokeMethod(mStopForeground, mStopForegroundArgs);
			return;
		}

		// Fall back on the old API. Note to cancel BEFORE changing the
		// foreground state, since we could be killed at that point.
		mNM.cancel(id);
		mSetForegroundArgs[0] = Boolean.FALSE;
		invokeMethod(mSetForeground, mSetForegroundArgs);
	}

	void invokeMethod(Method method, Object[] args)
	{
		try
		{
			method.invoke(this, args);
		}
		catch (InvocationTargetException e)
		{
			// Should not happen
			Log.w(TAG, "Unable to invoke method", e);
		}
		catch (IllegalAccessException e)
		{
			// Should not happen
			Log.w(TAG, "Unable to invoke method", e);
		}
	}

	private String[] getUserProxy()
	{
		Method method = null;
		try
		{
			/*
			 * ProxyProperties proxyProperties = ConnectivityManager.getProxy();
			 */
			method = ConnectivityManager.class.getMethod("getProxy");
		}
		catch (NoSuchMethodException e)
		{
			// This is normal situation for pre-ICS devices
			return null;
		}
		catch (Exception e)
		{
			// This should not happen
			Log.e(TAG, "getProxy failure", e);
			return null;
		}
		
		try
		{
			Object pp = method.invoke(mCM);
			if (pp == null)
				return null;
			
			return getUserProxy(pp);
		}
		catch (Exception e)
		{
			// This should not happen
			Log.e(TAG, "getProxy failure", e);
			return null;
		}       
	}
	
	private String[] getUserProxy(Object pp) throws Exception
	{
		String[] userProxy = new String[3];
		
		String className = "android.net.ProxyProperties";
		Class<?> c = Class.forName(className);
		Method method;
		
	    /*
	     * String proxyHost = pp.getHost()
	     */
		method = c.getMethod("getHost");
		userProxy[0] = (String) method.invoke(pp);

	    /*
	     * int proxyPort = pp.getPort();
	     */
		method = c.getMethod("getPort");
		userProxy[1] = String.valueOf((Integer) method.invoke(pp));

		/*
		 * String proxyEL = getExclusionList()
		 */
		method = c.getMethod("getExclusionList");
		userProxy[2] = (String) method.invoke(pp);

		if (userProxy[0] != null)
			return userProxy;
		else
			return null;
	}
	
	/**
	 * Tries to set proxy via native call.
	 * 
	 * @return true if device supports native proxy setting
	 */
	private boolean setConnectionProxy()
	{
		Method method = null;
		try
		{
			/*
			 * android.net.LinkProperties lp = ConnectivityManager.getActiveLinkProperties();
			 */
			method = ConnectivityManager.class.getMethod("getActiveLinkProperties");
		}
		catch (NoSuchMethodException e)
		{
			// This is normal situation for pre-ICS devices
			return false;
		}
		catch (Exception e)
		{
			// This should not happen
			Log.e(TAG, "setHttpProxy failure", e);
			return false;
		}
		try
		{
			Object lp = method.invoke(mCM);
			if (lp == null)
				return true;
			/*
			 * ProxyProperties pp = new ProxyProperties("127.0.0.1", port, "");
			 */
			String className = "android.net.ProxyProperties";
			Class<?> c = Class.forName(className);
			Class<?>[] parameter = new Class[] {String.class, int.class, String.class};
			Object args[] = new Object[3];
			args[0] = "127.0.0.1";
			args[1] = new Integer(port);
			args[2] = "";
			Constructor<?> cons = c.getConstructor(parameter);
			Object pp = cons.newInstance(args);
			/*
			 * lp.setHttpProxy(pp);
			 */
			method = lp.getClass().getMethod("setHttpProxy", pp.getClass());
			method.invoke(lp, pp);

			Intent intent = null;
			NetworkInfo ni = mCM.getActiveNetworkInfo();
			switch (ni.getType())
			{
				case ConnectivityManager.TYPE_WIFI:
					intent = new Intent("android.net.wifi.LINK_CONFIGURATION_CHANGED");
					break;
				case ConnectivityManager.TYPE_MOBILE:
					//TODO We leave it here for future, it does not work now
			        //intent = new Intent("android.intent.action.ANY_DATA_STATE");
			        break;
			}
			if (intent != null)
			{
		        if (lp != null)
		        {
		        	intent.putExtra("linkProperties", (Parcelable) lp);
		        }
				sendBroadcast(intent);
			}
			
			return true;
		}
		catch (Exception e)
		{
			// This should not happen
			Log.e(TAG, "setHttpProxy failure", e);
			return false;
		}       
	}
	
	private void clearConnectionProxy()
	{
		try
		{
			/*
			 * android.net.LinkProperties lp =
			 * ConnectivityManager.getActiveLinkProperties();
			 */
			Method method = ConnectivityManager.class.getMethod("getActiveLinkProperties");
			Object lp = method.invoke(mCM);
			
			String proxyHost = (String) proxy.props.getProperty("adblock.proxyHost");
			String proxyPort = (String) proxy.props.getProperty("adblock.proxyPort");
			String proxyExcl = (String) proxy.props.getProperty("adblock.proxyExcl");

			String className = "android.net.ProxyProperties";
			Class<?> c = Class.forName(className);
			method = lp.getClass().getMethod("setHttpProxy", c);
			if (proxyHost != null)
			{
				/*
				 * ProxyProperties pp = new ProxyProperties(proxyHost, proxyPort, proxyExcl);
				 */
				Class<?>[] parameter = new Class[] {String.class, int.class, String.class};
				Object args[] = new Object[3];
				args[0] = proxyHost;
				args[1] = new Integer(proxyPort);
				args[2] = proxyExcl;
				Constructor<?> cons = c.getConstructor(parameter);
				Object pp = cons.newInstance(args);
				/*
				 * lp.setHttpProxy(pp);
				 */
				method.invoke(lp, pp);
			}
			else
			{
				/*
				 * lp.setHttpProxy(null);
				 */
				method.invoke(lp, new Object[] {null});
			}

			Intent intent = new Intent("android.net.wifi.LINK_CONFIGURATION_CHANGED");
			intent.putExtra("linkProperties", (Parcelable) lp);
			sendBroadcast(intent);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	private void configureUserProxy(Properties config, String proxyHost, String proxyPort, String proxyExcl, String proxyUser, String proxyPass)
	{
		if (proxyHost != null && ! "".equals(proxyHost) && proxyPort != null && ! "".equals(proxyPort))
		{
			// Check for dirty proxy settings - this indicated previous crash:
			// proxy points to ourselves
			// proxy port is 0
			// proxy is 127.0.0.1:8080
			int p = Integer.valueOf(proxyPort);
			if (p == 0 || isLocalHost(proxyHost) && (p == port || p == 8080))
				return;
			
			config.put("adblock.proxyHost", proxyHost);
			config.put("adblock.proxyPort", proxyPort);
			//TODO Not implemented in our proxy but needed to restore settings
			config.put("adblock.proxyExcl", proxyExcl);

			if (! isTransparent)
			{
				config.put("https.proxyHost", proxyHost);
				config.put("https.proxyPort", proxyPort);
			}

			if (proxyUser != null && ! "".equals(proxyUser) && proxyPass != null && ! "".equals(proxyPass))
			{
				// Base64 encode user:password
				String proxyAuth = "Basic " + new String(Base64.encode(proxyUser + ":" + proxyPass));
				config.put("adblock.auth", proxyAuth);
				if (! isTransparent)
				{
					config.put("https.auth", proxyAuth);
				}
			}
		}
	}
	
	private static final boolean isLocalHost(String host)
	{
		if (host == null)
			return false;

		try
		{
			if (host != null)
			{
				if (host.equalsIgnoreCase("localhost"))
					return true;
				
				String className = "android.net.NetworkUtils";
				Class<?> c = Class.forName(className);
				/*
				 * InetAddress address = NetworkUtils.numericToInetAddress(host);
				 */
				Method method = c.getMethod("numericToInetAddress", String.class);
				InetAddress address = (InetAddress) method.invoke(null, host);
				
				if (address.isLoopbackAddress())
					return true;
			}
		}
		catch (Exception e)
		{
		}
		return false;
	}

	public String getIptables() throws IOException, RootToolsException, TimeoutException
	{
		if (! RootTools.isAccessGiven())
			return null;
		
		File ipt = getFileStreamPath("iptables");
		
		if (! ipt.exists())
			return null;
		
		String path = ipt.getAbsolutePath();
		
		RootTools.sendShell("chmod 700 " + path, DEFAULT_TIMEOUT);

		boolean compatible = false;
		boolean version = false;

		String command = path + " --version\n" + path + " -L -t nat -n\n";

		List<String> result = RootTools.sendShell(command, DEFAULT_TIMEOUT);
		for (String line : result)
		{
			if (line.contains("OUTPUT"))
				compatible = true;
			if (line.contains("v1.4."))
				version = true;
		}

		if (!compatible || !version)
			return null;
		
		return path;
	}

	private final IBinder binder = new LocalBinder();

	public final class LocalBinder extends Binder
	{
		public ProxyService getService()
		{
			return ProxyService.this;
		}
	}

	@Override
	public IBinder onBind(Intent intent)
	{
		return binder;
	}

	private BroadcastReceiver proxyReceiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(final Context context, Intent intent)
		{
			if (intent.getAction().equals(ProxyService.BROADCAST_PROXY_FAILED))
			{
				stopSelf();
			}
		}
	};
	
	private BroadcastReceiver connectionReceiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(Context ctx, Intent intent)
		{
			String action = intent.getAction();
			Log.i(TAG, "Action: " + action);
			if (ConnectivityManager.CONNECTIVITY_ACTION.equals(action))
			{
				NetworkInfo info = intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
				String typeName = info.getTypeName();
				String subtypeName = info.getSubtypeName();
				boolean available = info.isAvailable();
				Log.i(TAG, "Network Type: " + typeName + ", subtype: " + subtypeName + ", available: " + available);
				if (info.getType() == ConnectivityManager.TYPE_WIFI)
					setConnectionProxy();
			}
			else if ("android.net.wifi.LINK_CONFIGURATION_CHANGED".equals(action))
			{
				Object lp = intent.getParcelableExtra("linkProperties");
				Method method;
				try
				{
					/*
					 * lp.getHttpProxy();
					 */
					method = lp.getClass().getMethod("getHttpProxy");
					Object pp = method.invoke(lp);
					
					String[] userProxy = getUserProxy(pp);
					if (userProxy != null && Integer.valueOf(userProxy[1]) != port)
					{
						Log.i(TAG, "User has set new proxy: " + userProxy[0] + ":" + userProxy[1] + "(" + userProxy[2] + ")");
						configureUserProxy(proxy.props, userProxy[0], userProxy[1], userProxy[2], null, null);
						proxy.restart(proxy.props.getProperty("handler"));
					}
				}
				catch (Exception e)
				{
					// This should not happen
					e.printStackTrace();
				}

				
			}
		}
	};
	
	private final class ProxyServer extends Server
	{
	    @Override
		public void close()
		{
			try
			{
				listen.close();
				this.interrupt();
				this.join();
			}
			catch (Exception e)
			{
			}
			log(LOG_WARNING, null, "server stopped");
		}

	    @Override
		public void log(int level, Object obj, String message)
		{
			if (level <= logLevel)
			{
				Log.println(7 - level, obj != null ? obj.toString() : TAG, message);
			}
		}
	}
}
