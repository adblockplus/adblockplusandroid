/*
 * This file is part of the Adblock Plus,
 * Copyright (C) 2006-2012 Eyeo GmbH
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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeoutException;

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
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.stericson.RootTools.RootTools;
import com.stericson.RootTools.RootToolsException;

public class ProxyService extends Service implements OnSharedPreferenceChangeListener
{
  private static final String LOCALHOST = "127.0.0.1";
  /**
   * Indicates that system supports native proxy configuration.
   */
  public static boolean hasNativeProxy = Build.VERSION.SDK_INT >= 12; // Honeycomb 3.1

  static
  {
    RootTools.debugMode = false;
  }

  private static final String TAG = "ProxyService";
  private static final boolean logRequests = false;

  private final static int DEFAULT_TIMEOUT = 3000;
  private final static int NO_TRAFFIC_TIMEOUT = 5 * 60 * 1000; // 5 minutes

  final static int ONGOING_NOTIFICATION_ID = R.string.app_name;
  private final static int NOTRAFFIC_NOTIFICATION_ID = R.string.app_name + 3;

  /**
   * Broadcasted when service starts or stops.
   */
  public final static String BROADCAST_STATE_CHANGED = "org.adblockplus.android.service.state";
  /**
   * Broadcasted if proxy fails to start.
   */
  public final static String BROADCAST_PROXY_FAILED = "org.adblockplus.android.proxy.failure";

  private final static String IPTABLES_RETURN = " -t nat -m owner --uid-owner {{UID}} -A OUTPUT -p tcp -j RETURN\n";
  private final static String IPTABLES_ADD_HTTP = " -t nat -A OUTPUT -p tcp --dport 80 -j REDIRECT --to {{PORT}}\n";

  private Notification ongoingNotification;
  private PendingIntent contentIntent;

  private Handler notrafficHandler;

  protected ProxyServer proxy = null;
  protected int port;

  /**
   * Indicates that service is working with root privileges.
   */
  private boolean transparent = false;
  /**
   * Indicates that service has autoconfigured Android proxy settings (version
   * 3.1+).
   */
  private boolean nativeProxy = false;

  private String iptables = null;

  @Override
  public void onCreate()
  {
    super.onCreate();

    // Get port for local proxy
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
    Resources resources = getResources();
    String p = prefs.getString(getString(R.string.pref_port), null);
    try
    {
      port = p != null ? Integer.valueOf(p) : resources.getInteger(R.integer.def_port);
    }
    catch (NumberFormatException e)
    {
      Toast.makeText(this, getString(R.string.msg_badport) + ": " + p, Toast.LENGTH_LONG).show();
      port = getResources().getInteger(R.integer.def_port);
    }

    // Try to read user proxy settings
    String proxyHost = null;
    String proxyPort = null;
    String proxyExcl = null;
    String proxyUser = null;
    String proxyPass = null;

    if (hasNativeProxy)
    {
      // Read system settings
      proxyHost = System.getProperty("http.proxyHost");
      proxyPort = System.getProperty("http.proxyPort");
      proxyExcl = System.getProperty("http.nonProxyHosts");

      Log.d(TAG, "PRX: " + proxyHost + ":" + proxyPort + "(" + proxyExcl + ")");
      String[] px = ProxySettings.getUserProxy(getApplicationContext()); // not used but left for future reference
      if (px != null)
        Log.d(TAG, "PRX: " + px[0] + ":" + px[1] + "(" + px[2] + ")");
    }
    else
    {
      // Read application settings
      proxyHost = prefs.getString(getString(R.string.pref_proxyhost), null);
      proxyPort = prefs.getString(getString(R.string.pref_proxyport), null);
      proxyUser = prefs.getString(getString(R.string.pref_proxyuser), null);
      proxyPass = prefs.getString(getString(R.string.pref_proxypass), null);
    }

    // Check for root privileges and try to install transparent proxy
    if (RootTools.isAccessGiven())
    {
      try
      {
        initIptables();

        StringBuffer cmd = new StringBuffer();
        int uid = getPackageManager().getPackageInfo(getPackageName(), 0).applicationInfo.uid;
        cmd.append(iptables);
        cmd.append(IPTABLES_RETURN.replace("{{UID}}", String.valueOf(uid)));
        cmd.append(iptables);
        cmd.append(IPTABLES_ADD_HTTP.replace("{{PORT}}", String.valueOf(port)));
        String rules = cmd.toString();
        RootTools.sendShell(rules, DEFAULT_TIMEOUT);
        transparent = true;
      }
      catch (FileNotFoundException e)
      {
        // ignore - this is "normal" case
      }
      catch (NameNotFoundException e)
      {
        Log.e(TAG, "Failed to initialize iptables", e);
      }
      catch (IOException e)
      {
        Log.e(TAG, "Failed to initialize iptables", e);
      }
      catch (RootToolsException e)
      {
        Log.e(TAG, "Failed to initialize iptables", e);
      }
      catch (TimeoutException e)
      {
        Log.e(TAG, "Failed to initialize iptables", e);
      }
    }

    if (!transparent)
    {
      // Try to set native proxy
      nativeProxy = ProxySettings.setConnectionProxy(getApplicationContext(), LOCALHOST, port, "");

      if (nativeProxy)
      {
        registerReceiver(connectionReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        registerReceiver(connectionReceiver, new IntentFilter("android.net.wifi.LINK_CONFIGURATION_CHANGED"));
      }
    }

    // Start engine
    AdblockPlus.getApplication().startEngine();

    registerReceiver(proxyReceiver, new IntentFilter(ProxyService.BROADCAST_PROXY_FAILED));
    registerReceiver(matchesReceiver, new IntentFilter(AdblockPlus.BROADCAST_FILTER_MATCHES));

    // Start proxy
    if (proxy == null)
    {
      ServerSocket listen = null;
      try
      {
        // TODO Add port travel
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
      if (transparent)
      {
        config.put("main.handlers", "urlmodifier adblock");
        config.put("urlmodifier.class", "org.adblockplus.brazil.TransparentProxyHandler");
      }
      else
      {
        config.put("main.handlers", "https adblock");
        config.put("https.class", "org.adblockplus.brazil.SSLConnectionHandler");
      }
      config.put("adblock.class", "org.adblockplus.brazil.RequestHandler");
      if (logRequests)
        config.put("adblock.proxylog", "yes");

      configureUserProxy(config, proxyHost, proxyPort, proxyExcl, proxyUser, proxyPass);

      proxy = new ProxyServer();
      proxy.logLevel = Server.LOG_DIAGNOSTIC;
      proxy.setup(listen, config.getProperty("handler"), config);
      proxy.start();
    }

    prefs.registerOnSharedPreferenceChangeListener(this);

    String msg = getString(transparent ? R.string.notif_all : nativeProxy ? R.string.notif_wifi : R.string.notif_waiting);
    if (!transparent && !nativeProxy)
    {
      // Initiate no traffic check
      notrafficHandler = new Handler();
      notrafficHandler.postDelayed(noTraffic, NO_TRAFFIC_TIMEOUT);
    }
    // Prepare notification
    ongoingNotification = new Notification();
    ongoingNotification.when = 0;
    contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, Preferences.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK), 0);
    ongoingNotification.icon = R.drawable.ic_stat_blocking;
    ongoingNotification.setLatestEventInfo(getApplicationContext(), getText(R.string.app_name), msg, contentIntent);

    // Lock service
    if (prefs.getBoolean(getString(R.string.pref_priority), resources.getBoolean(R.bool.def_priority)))
    {
      startForeground(ONGOING_NOTIFICATION_ID, ongoingNotification);
    }

    sendBroadcast(new Intent(BROADCAST_STATE_CHANGED).putExtra("enabled", true).putExtra("port", port).putExtra("manual", isManual()));
    Log.i(TAG, "Service started");
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId)
  {
    return START_STICKY;
  }

  @Override
  public void onDestroy()
  {
    super.onDestroy();

    stopNoTrafficCheck(false);

    unregisterReceiver(matchesReceiver);
    unregisterReceiver(proxyReceiver);

    // Stop IP redirecting
    if (transparent)
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
            Log.e(TAG, "Failed to clear iptables", e);
          }
        }
      }.start();
    }

    // Clear native proxy
    if (nativeProxy)
    {
      unregisterReceiver(connectionReceiver);
      clearConnectionProxy();
    }

    sendBroadcast(new Intent(BROADCAST_STATE_CHANGED).putExtra("enabled", false));

    // Stop proxy server
    if (proxy != null)
      proxy.close();

    // Stop engine if not in interactive mode
    AdblockPlus.getApplication().stopEngine(false);

    // Release service lock
    stopForeground(true);

    Log.i(TAG, "Service stopped");
  }

  /**
   * Restores system proxy settings via native call on Android 3.1+ devices using
   * Java reflection.
   */
  private void clearConnectionProxy()
  {
    String proxyHost = (String) proxy.props.getProperty("adblock.proxyHost");
    String proxyPort = (String) proxy.props.getProperty("adblock.proxyPort");
    String proxyExcl = (String) proxy.props.getProperty("adblock.proxyExcl");
    int port = 0;
    try
    {
      if (proxyHost != null)
        port = Integer.valueOf(proxyPort);
    }
    catch (NumberFormatException e)
    {
      Log.e(TAG, "Bad port setting", e);
    }
    ProxySettings.setConnectionProxy(getApplicationContext(), proxyHost, port, proxyExcl);
  }

  /**
   * Sets user proxy settings in proxy service properties.
   */
  private void configureUserProxy(Properties config, String proxyHost, String proxyPort, String proxyExcl, String proxyUser, String proxyPass)
  {
    // Clean previous settings
    config.remove("adblock.proxyHost");
    config.remove("adblock.proxyPort");
    config.remove("adblock.auth");
    config.remove("adblock.proxyExcl");
    if (!transparent)
    {
      config.remove("https.proxyHost");
      config.remove("https.proxyPort");
      config.remove("https.auth");
    }

    if (nativeProxy)
      passProxySettings(proxyHost, proxyPort, proxyExcl);

    // Check if there are any settings
    if (proxyHost == null || "".equals(proxyHost))
      return;

    // Check for dirty proxy settings - this indicated previous crash:
    // proxy points to ourselves
    // proxy port is null, 0 or not a number
    // proxy is 127.0.0.1:8080
    if (proxyPort == null)
      return;
    int p = 0;
    try
    {
      p = Integer.valueOf(proxyPort);
    }
    catch (NumberFormatException e)
    {
      return;
    }
    if (p == 0 || isLocalHost(proxyHost) && (p == port || p == 8080))
    {
      if (nativeProxy)
        passProxySettings(null, null, null);
      return;
    }

    config.put("adblock.proxyHost", proxyHost);
    config.put("adblock.proxyPort", proxyPort);
    if (!transparent)
    {
      config.put("https.proxyHost", proxyHost);
      config.put("https.proxyPort", proxyPort);
    }

    // TODO Not implemented in our proxy but needed to restore settings
    if (proxyExcl != null)
      config.put("adblock.proxyExcl", proxyExcl);

    if (proxyUser != null && !"".equals(proxyUser) && proxyPass != null && !"".equals(proxyPass))
    {
      // Base64 encode user:password
      String proxyAuth = "Basic " + new String(Base64.encode(proxyUser + ":" + proxyPass));
      config.put("adblock.auth", proxyAuth);
      if (!transparent)
        config.put("https.auth", proxyAuth);
    }
  }

  private void passProxySettings(String proxyHost, String proxyPort, String proxyExcl)
  {
    try
    {
      CrashHandler handler = (CrashHandler) Thread.getDefaultUncaughtExceptionHandler();
      handler.saveProxySettings(proxyHost, proxyPort, proxyExcl);
    }
    catch (ClassCastException e)
    {
      // ignore - default handler in use
    }
  }

  @Override
  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
  {
    if (getString(R.string.pref_priority).equals(key))
    {
      if (sharedPreferences.getBoolean(key, false))
      {
        startForeground(ONGOING_NOTIFICATION_ID, ongoingNotification);
      }
      else
      {
        stopForeground(true);
      }
      return;
    }
    if (hasNativeProxy)
    {
      String ketHost = getString(R.string.pref_proxyhost);
      String keyPort = getString(R.string.pref_proxyport);
      String keyUser = getString(R.string.pref_proxyuser);
      String keyPass = getString(R.string.pref_proxypass);
      if (key.equals(ketHost) || key.equals(keyPort) || key.equals(keyUser) || key.equals(keyPass))
      {
        String proxyHost = sharedPreferences.getString(ketHost, null);
        String proxyPort = sharedPreferences.getString(keyPort, null);
        String proxyUser = sharedPreferences.getString(keyUser, null);
        String proxyPass = sharedPreferences.getString(keyPass, null);
        if (proxy != null)
        {
          configureUserProxy(proxy.props, proxyHost, proxyPort, null, proxyUser, proxyPass);
          proxy.restart(proxy.props.getProperty("handler"));
        }
      }
    }
  }

  public boolean isTransparent()
  {
    return transparent;
  }

  public boolean isNativeProxy()
  {
    return nativeProxy;
  }

  /**
   * Checks if user has to set proxy settings manually
   */
  public boolean isManual()
  {
    return !transparent && !nativeProxy;
  }

  /**
   * Checks if specified host is local.
   */
  private static final boolean isLocalHost(String host)
  {
    if (host == null)
      return false;

    try
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
    catch (Exception e)
    {
      Log.w(TAG, null, e);
    }
    return false;
  }

  /**
   * Initializes iptables executable.
   *
   * @throws FileNotFoundException If iptables initialization failed due to provided reasons.
   */
  private void initIptables() throws IOException, RootToolsException, TimeoutException, FileNotFoundException
  {
    if (!RootTools.isAccessGiven())
      throw new FileNotFoundException("No root access");

    File ipt = getFileStreamPath("iptables");

    if (!ipt.exists())
    {
      Log.e(TAG, "No iptables excutable found");
      throw new FileNotFoundException("No iptables executable");
    }

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
    {
      Log.e(TAG, "Incompatible iptables excutable");
      throw new FileNotFoundException("Incompatible iptables excutable");
    }

    iptables = path;
  }

  public List<String> getIptablesOutput()
  {
    if (iptables == null)
      return null;

    String command = iptables + " -L -t nat -n\n";
    try
    {
      return RootTools.sendShell(command, DEFAULT_TIMEOUT);
    }
    catch (Exception e)
    {
      Log.e(TAG, "Failed to get iptables configuration", e);
      return null;
    }
  }

  /**
   * Stops no traffic check, optionally resetting notification message.
   *
   * @param changeStatus
   *          true if notification message should be set to normal operating
   *          mode
   */
  private void stopNoTrafficCheck(boolean changeStatus)
  {
    if (notrafficHandler != null)
    {
      notrafficHandler.removeCallbacks(noTraffic);
      if (changeStatus)
      {
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        ongoingNotification.setLatestEventInfo(ProxyService.this, getText(R.string.app_name), getText(R.string.notif_wifi), contentIntent);
        notificationManager.notify(ONGOING_NOTIFICATION_ID, ongoingNotification);
      }
    }
    notrafficHandler = null;
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

  /**
   * Executed if no traffic is detected after a period of time. Notifies user
   * about possible configuration problems.
   */
  private Runnable noTraffic = new Runnable() {
    public void run()
    {
      // Show warning notification
      Notification notification = new Notification();
      notification.icon = R.drawable.ic_stat_warning;
      notification.when = System.currentTimeMillis();
      notification.flags |= Notification.FLAG_AUTO_CANCEL;
      notification.defaults |= Notification.DEFAULT_SOUND;
      Intent intent = new Intent(ProxyService.this, ConfigurationActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      intent.putExtra("port", port);
      PendingIntent contentIntent = PendingIntent.getActivity(ProxyService.this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
      notification.setLatestEventInfo(ProxyService.this, getText(R.string.app_name), getString(R.string.notif_notraffic), contentIntent);
      NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
      notificationManager.notify(NOTRAFFIC_NOTIFICATION_ID, notification);
    }
  };

  /**
   * Stops no traffic check if traffic is detected by proxy service.
   */
  private BroadcastReceiver matchesReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(final Context context, Intent intent)
    {
      if (intent.getAction().equals(AdblockPlus.BROADCAST_FILTER_MATCHES))
        stopNoTrafficCheck(true);
    }
  };

  /**
   * Stops service if proxy fails.
   */
  private BroadcastReceiver proxyReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(final Context context, Intent intent)
    {
      if (intent.getAction().equals(ProxyService.BROADCAST_PROXY_FAILED))
      {
        stopSelf();
      }
    }
  };

  /**
   * Monitors system network connection settings changes and updates proxy
   * settings accordingly.
   */
  private BroadcastReceiver connectionReceiver = new BroadcastReceiver() {
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
          ProxySettings.setConnectionProxy(getApplicationContext(), LOCALHOST, port, "");
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

          String[] userProxy = ProxySettings.getUserProxy(pp);
          if (userProxy != null && Integer.valueOf(userProxy[1]) != port)
          {
            Log.i(TAG, "User has set new proxy: " + userProxy[0] + ":" + userProxy[1] + "(" + userProxy[2] + ")");
            if (proxy != null)
            {
              configureUserProxy(proxy.props, userProxy[0], userProxy[1], userProxy[2], null, null);
              proxy.restart(proxy.props.getProperty("handler"));
            }
          }
        }
        catch (Exception e)
        {
          // This should not happen
          Log.e(TAG, null, e);
        }

      }
    }
  };

  final class ProxyServer extends Server
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
        // ignore - it always happens
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
