package org.adblockplus.android;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Properties;

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
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

public class ProxyService extends Service
{
	private static final String TAG = "ProxyService";

	public static final String BASE = "/data/data/org.adblockplus.android/";

	public final static String DEFAULT_SHELL = "/system/bin/sh";

	public final static String DEFAULT_ROOT = "/system/bin/su";
	public final static String ALTERNATIVE_ROOT = "/system/xbin/su";

	public final static String DEFAULT_IPTABLES = "/data/data/org.adblockplus.android/iptables";
	public final static String ALTERNATIVE_IPTABLES = "/system/bin/iptables";

	public final static String SCRIPT_FILE = "/data/data/org.adblockplus.android/script";
	public final static String DEFOUT_FILE = "/data/data/org.adblockplus.android/defout";

	public final static int DEFAULT_TIMEOUT = 3000;
	public final static int EXECUTION_ERROR = -99;

	public final static String BROADCAST_PROXY_FAILED = "org.adblockplus.android.proxy.failure";

	private final static String CMD_IPTABLES_RETURN = "iptables -t nat -m owner --uid-owner {{UID}} -A OUTPUT -p tcp -j RETURN\n";
	private final static String CMD_IPTABLES_REDIRECT_ADD_HTTP = "iptables -t nat -A OUTPUT -p tcp --dport 80 -j REDIRECT --to {{PORT}}\n";
	private final static String CMD_IPTABLES_DNAT_ADD_HTTP = "iptables -t nat -A OUTPUT -p tcp --dport 80 -j DNAT --to-destination 127.0.0.1:{{PORT}}\n";

	private static final Class<?>[] mSetForegroundSignature = new Class[] { boolean.class };
	private static final Class<?>[] mStartForegroundSignature = new Class[] { int.class, Notification.class };
	private static final Class<?>[] mStopForegroundSignature = new Class[] { boolean.class };

	private NotificationManager mNM;
	private Method mSetForeground;
	private Method mStartForeground;
	private Method mStopForeground;
	private Object[] mSetForegroundArgs = new Object[1];
	private Object[] mStartForegroundArgs = new Object[2];
	private Object[] mStopForegroundArgs = new Object[1];

//	private ProxyThread proxy = null;
	private ProxyServer proxy = null;
	private int port;

	private int hasRedirectSupport = -1;
	private int isRoot = -1;
	
	private boolean isTransparent = false;

	private String shell = null;
	private String root_shell = null;
	private String iptables = null;

	@Override
	public void onCreate()
	{
		super.onCreate();

		initForegroundCompat();

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

		if (isRoot())
		{
			runRootCommand("chmod 700 " + DEFAULT_IPTABLES + "\n", DEFAULT_TIMEOUT);
			testRedirectSupport();

			try
			{
				StringBuffer cmd = new StringBuffer();
				int uid = getPackageManager().getPackageInfo(getPackageName(), 0).applicationInfo.uid;
				cmd.append(CMD_IPTABLES_RETURN.replace("{{UID}}", String.valueOf(uid)));
				cmd.append(hasRedirectSupport > 0 ? CMD_IPTABLES_REDIRECT_ADD_HTTP.replace("{{PORT}}", String.valueOf(port)) : CMD_IPTABLES_DNAT_ADD_HTTP.replace("{{PORT}}", String.valueOf(port)));
				String rules = cmd.toString().replace("iptables", getIptables());
				runRootCommand(rules, DEFAULT_TIMEOUT);
				isTransparent = true;
			}
			catch (NameNotFoundException e)
			{
				e.printStackTrace();
			}
		}

		// Start engine
		AdblockPlus.getApplication().startEngine();

		registerReceiver(receiver, new IntentFilter(ProxyService.BROADCAST_PROXY_FAILED));

		// Start proxy
		if (proxy == null)
		{
			ServerSocket listen = null;
			try
			{
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
			config.put("adblock.handler", "proxy");
			config.put("proxy.class", "sunlabs.brazil.proxy.ProxyHandler");
			config.put("proxy.proxylog", "yes");

			String proxyHost = prefs.getString(getString(R.string.pref_proxyhost), "");
			String proxyPort = prefs.getString(getString(R.string.pref_proxyport), "");
			String proxyUser = prefs.getString(getString(R.string.pref_proxyuser), "");
			String proxyPass = prefs.getString(getString(R.string.pref_proxypass), "");

			if (! proxyHost.equals("") && ! proxyPort.equals(""))
			{
				config.put("proxy.proxyHost", proxyHost);
				config.put("proxy.proxyPort", proxyPort);

				if (! isTransparent)
				{
					config.put("https.proxyHost", proxyHost);
					config.put("https.proxyPort", proxyPort);
				}

				if (! proxyUser.equals("") && ! proxyPass.equals(""))
				{
					// Base64 encode user:password
					String proxyAuth = "Basic " + new String(Base64.encode(proxyUser + ":" + proxyPass));
					config.put("proxy.auth", proxyAuth);
					if (! isTransparent)
					{
						config.put("https.auth", proxyAuth);
					}
				}
			}

			proxy = new ProxyServer();
			proxy.logLevel = Server.LOG_DIAGNOSTIC;
			proxy.setup(listen, config.getProperty("handler"), config);
			proxy.start();
		}
		
		// Lock service
		String msg = getString(isTransparent ? R.string.notif_transparent : R.string.notif_proxy);
		if (! isTransparent)
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

		unregisterReceiver(receiver);

		// Stop IP redirecting
		if (isTransparent)
		{
			new Thread() {
				@Override
				public void run()
				{
					runRootCommand(getIptables() + " -t nat -F OUTPUT\n", DEFAULT_TIMEOUT);
				}
			}.start();
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

	private void testRedirectSupport()
	{
		StringBuilder sb = new StringBuilder();
		String command = getIptables() + " -t nat -A OUTPUT -p udp --dport 54 -j REDIRECT --to 8154";

		int exitcode = runScript(command, sb, DEFAULT_TIMEOUT, true);

		String lines = sb.toString();

		hasRedirectSupport = 1;

		// Flush the check command
		runRootCommand(command.replace("-A", "-D"), DEFAULT_TIMEOUT);

		if (exitcode == EXECUTION_ERROR)
			return;

		if (lines.contains("No chain/target/match"))
		{
			hasRedirectSupport = 0;
		}
	}

	public boolean runRootCommand(String command, int timeout)
	{
		StringBuilder res = new StringBuilder();
		runScript(command, res, timeout, true);
		return true;
	}

	private synchronized int runScript(String script, StringBuilder res, long timeout, boolean asroot)
	{
		Log.d(TAG, script);
		final File file = new File(SCRIPT_FILE);
		final ScriptRunner runner = new ScriptRunner(file, script, res, asroot);
		runner.start();
		try
		{
			if (timeout > 0)
			{
				runner.join(timeout);
			}
			else
			{
				runner.join();
			}
			if (runner.isAlive())
			{
				// Timed-out
				runner.destroy();
				runner.join(1000);
				return EXECUTION_ERROR;
			}
		}
		catch (InterruptedException ex)
		{
			return EXECUTION_ERROR;
		}
		return runner.exitcode;
	}

	public boolean isRoot()
	{
		if (isRoot != -1)
			return isRoot == 1;

		// Switch between binaries
		if (new File(DEFAULT_ROOT).exists())
		{
			root_shell = DEFAULT_ROOT;
		}
		else if (new File(ALTERNATIVE_ROOT).exists())
		{
			root_shell = ALTERNATIVE_ROOT;
		}
		else
		{
			root_shell = "su";
		}

		String lines = null;

		StringBuilder sb = new StringBuilder();
		String command = "ls /\n" + "exit\n";

		int exitcode = runScript(command, sb, 10 * 1000, true);

		if (exitcode == EXECUTION_ERROR)
		{
			return false;
		}

		lines = sb.toString();

		if (lines.contains("system"))
		{
			isRoot = 1;
		}

		return isRoot == 1 ? true : false;
	}

	private String getShell()
	{
		if (shell == null)
		{
			shell = DEFAULT_SHELL;
			if (!new File(shell).exists())
				shell = "sh";
		}
		return shell;
	}

	public String getIptables()
	{
		if (iptables == null)
		{
			iptables = DEFAULT_IPTABLES;

			if (!isRoot())
				return iptables;

			String lines = null;

			boolean compatible = false;
			boolean version = false;

			StringBuilder sb = new StringBuilder();
			String command = iptables + " --version\n" + iptables + " -L -t nat -n\n" + "exit\n";

			int exitcode = runScript(command, sb, DEFAULT_TIMEOUT, true);

			if (exitcode == EXECUTION_ERROR)
				return iptables;

			lines = sb.toString();

			if (lines.contains("OUTPUT"))
			{
				compatible = true;
			}
			if (lines.contains("v1.4."))
			{
				version = true;
			}

			if (!compatible || !version)
			{
				iptables = ALTERNATIVE_IPTABLES;
				if (!new File(iptables).exists())
					iptables = "iptables";
			}
		}
		return iptables;
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

	private BroadcastReceiver receiver = new BroadcastReceiver()
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

	private final class ScriptRunner extends Thread
	{
		private final File file;
		private final String script;
		private final StringBuilder res;
		private final boolean asroot;
		public int exitcode = -1;
		// private Process exec;
		private int mProcId;
		private FileDescriptor mTermFd;

		private int createSubprocess(int[] processId, String cmd)
		{
			ArrayList<String> argList = parse(cmd);
			String arg0 = argList.get(0);
			String[] args = argList.toArray(new String[1]);

			mTermFd = Exec.createSubprocess(arg0, args, null, processId);
			return processId[0];
		}

		private ArrayList<String> parse(String cmd)
		{
			final int PLAIN = 0;
			final int WHITESPACE = 1;
			final int INQUOTE = 2;
			int state = WHITESPACE;
			ArrayList<String> result = new ArrayList<String>();
			int cmdLen = cmd.length();
			StringBuilder builder = new StringBuilder();
			for (int i = 0; i < cmdLen; i++)
			{
				char c = cmd.charAt(i);
				if (state == PLAIN)
				{
					if (Character.isWhitespace(c))
					{
						result.add(builder.toString());
						builder.delete(0, builder.length());
						state = WHITESPACE;
					}
					else if (c == '"')
					{
						state = INQUOTE;
					}
					else
					{
						builder.append(c);
					}
				}
				else if (state == WHITESPACE)
				{
					if (Character.isWhitespace(c))
					{
						// do nothing
					}
					else if (c == '"')
					{
						state = INQUOTE;
					}
					else
					{
						state = PLAIN;
						builder.append(c);
					}
				}
				else if (state == INQUOTE)
				{
					if (c == '\\')
					{
						if (i + 1 < cmdLen)
						{
							i += 1;
							builder.append(cmd.charAt(i));
						}
					}
					else if (c == '"')
					{
						state = PLAIN;
					}
					else
					{
						builder.append(c);
					}
				}
			}
			if (builder.length() > 0)
			{
				result.add(builder.toString());
			}
			return result;
		}

		/**
		 * Creates a new script runner.
		 * 
		 * @param file
		 *            temporary script file
		 * @param script
		 *            script to run
		 * @param res
		 *            response output
		 * @param asroot
		 *            if true, executes the script as root
		 */
		public ScriptRunner(File file, String script, StringBuilder res, boolean asroot)
		{
			this.file = file;
			this.script = script;
			this.res = res;
			this.asroot = asroot;
		}

		/**
		 * Destroy this script runner
		 */
		@Override
		public synchronized void destroy()
		{
			try
			{
				Exec.hangupProcessGroup(mProcId);
				Exec.close(mTermFd);
			}
			catch (NoClassDefFoundError ignore)
			{
				// Nothing
			}
		}

		@Override
		public void run()
		{
			try
			{
				new File(DEFOUT_FILE).createNewFile();
				file.createNewFile();
				final String abspath = file.getAbsolutePath();

				// TODO: Rewrite this line
				// make sure we have execution permission on the script file
				// Runtime.getRuntime().exec("chmod 755 " + abspath).waitFor();

				// Write the script to be executed
				final OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(file));
				out.write("#!/system/bin/sh\n");
				out.write(script);
				if (!script.endsWith("\n"))
					out.write("\n");
				out.write("exit\n");
				out.flush();
				out.close();

				if (this.asroot)
				{
					// Create the "su" request to run the script
					// exec = Runtime.getRuntime().exec(
					// root_shell + " -c " + abspath);

					int pid[] = new int[1];
					mProcId = createSubprocess(pid, root_shell + " -c " + abspath);
				}
				else
				{
					// Create the "sh" request to run the script
					// exec = Runtime.getRuntime().exec(getShell() + " " +
					// abspath);

					int pid[] = new int[1];
					mProcId = createSubprocess(pid, getShell() + " " + abspath);
				}

				final InputStream stdout = new FileInputStream(DEFOUT_FILE);
				final byte buf[] = new byte[8192];
				int read = 0;

				exitcode = Exec.waitFor(mProcId);

				// Read stdout
				while (stdout.available() > 0)
				{
					read = stdout.read(buf);
					if (res != null)
						res.append(new String(buf, 0, read));
				}

			}
			catch (Exception ex)
			{
				if (res != null)
					res.append("\n" + ex);
			}
			finally
			{
				destroy();
			}
		}
	}
}
