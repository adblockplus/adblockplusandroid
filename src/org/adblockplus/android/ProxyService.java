/*
 * This file is part of Adblock Plus <https://adblockplus.org/>,
 * Copyright (C) 2006-2015 Eyeo GmbH
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

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.util.Properties;

import org.adblockplus.android.configurators.ProxyConfigurator;
import org.adblockplus.android.configurators.ProxyConfigurators;
import org.adblockplus.android.configurators.ProxyRegistrationType;
import org.adblockplus.libadblockplus.Notification.Type;
import org.apache.commons.lang.StringUtils;

import sunlabs.brazil.server.Server;
import sunlabs.brazil.util.Base64;
import android.annotation.SuppressLint;
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
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class ProxyService extends Service implements OnSharedPreferenceChangeListener
{
  private static final String TAG = Utils.getTag(ProxyService.class);

  private static final String LOCALHOST = "127.0.0.1";

  private static final int[] PORT_VARIANTS = new int[] {-1, 2020, 3030, 4040, 5050, 6060, 7070, 9090, 1234, 12345, 4321, 0};

  private static final boolean LOG_REQUESTS = false;

  private static final long POSITION_RIGHT = Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD ? Long.MIN_VALUE : Long.MAX_VALUE;

  /**
   * This flag indicates that this mobile device runs an Android version that allows the user to configure a http(s) proxy.
   */
  public static final boolean GLOBAL_PROXY_USER_CONFIGURABLE = Build.VERSION.SDK_INT >= 12; // Honeycomb 3.1

  /**
   * Broadcasted when service starts or stops.
   */
  public static final String BROADCAST_STATE_CHANGED = "org.adblockplus.android.SERVICE_STATE_CHANGED";

  /**
   * Broadcasted if proxy fails to start.
   */
  public static final String BROADCAST_PROXY_FAILED = "org.adblockplus.android.PROXY_FAILURE";

  /**
   * Proxy state changed
   */
  public static final String PROXY_STATE_CHANGED_ACTION = "org.adblockplus.android.PROXY_STATE_CHANGED";

  boolean hideIcon;

  protected ProxyServer proxy = null;

  protected int port;

  private final Properties proxyConfiguration = new Properties();

  private ProxyConfigurator proxyConfigurator = null;

  private NotificationWatcher notificationWatcher = null;

  @SuppressLint("NewApi")
  @Override
  public void onCreate()
  {
    super.onCreate();

    StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
        .permitAll()
        .penaltyLog()
        .build());

    final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

    // Try to read user proxy settings
    String proxyHost = System.getProperty("http.proxyHost");;
    String proxyPort = System.getProperty("http.proxyPort");
    final String proxyExcl = System.getProperty("http.nonProxyHosts");
    String proxyUser = null;
    String proxyPass = null;

    if (proxyHost == null || proxyPort == null)
    {
      // Read application settings
      proxyHost = prefs.getString(getString(R.string.pref_proxyhost), null);
      proxyPort = prefs.getString(getString(R.string.pref_proxyport), null);
      proxyUser = prefs.getString(getString(R.string.pref_proxyuser), null);
      proxyPass = prefs.getString(getString(R.string.pref_proxypass), null);
    }

    registerReceiver(this.proxyReceiver, new IntentFilter(ProxyService.BROADCAST_PROXY_FAILED));
    registerReceiver(this.proxyStateChangedReceiver, new IntentFilter(ProxyService.PROXY_STATE_CHANGED_ACTION));

    final InetAddress inetAddress;
    try
    {
      inetAddress = InetAddress.getByName(LOCALHOST);
    }
    catch (final UnknownHostException e)
    {
      sendBroadcast(new Intent(BROADCAST_PROXY_FAILED)
          .putExtra("msg", "Could not resolve 'localhost'"));
      return;
    }

    // Start proxy
    if (proxy == null)
    {
      // Select available port and bind to it, use previously selected port by default
      ServerSocket listen = null;
      String msg = null;
      for (final int p : PORT_VARIANTS)
      {
        final int toCheck = (p == -1) ? prefs.getInt(getString(R.string.pref_lastport), -1) : p;

        if (toCheck >= 0)
        {
          try
          {
            // Fix for #232, bind proxy socket to loopback only
            listen = new ServerSocket(toCheck, 1024, inetAddress);
            this.port = listen.getLocalPort();
            break;
          }
          catch (final IOException e)
          {
            Log.e(TAG, null, e);
            msg = e.getMessage();
          }
        }
      }

      if (listen == null)
      {
        sendBroadcast(new Intent(BROADCAST_PROXY_FAILED).putExtra("msg", msg));
        return;
      }

      this.proxyConfigurator = ProxyConfigurators.registerProxy(this, inetAddress, this.port);

      if (this.proxyConfigurator == null)
      {
        sendBroadcast(new Intent(BROADCAST_PROXY_FAILED)
            .putExtra("msg", "Failed to register proxy"));
        return;
      }

      // Save selected port
      final SharedPreferences.Editor editor = prefs.edit();
      editor.putBoolean(getString(R.string.pref_proxyautoconfigured), this.getProxyRegistrationType().isAutoConfigured());
      editor.putInt(getString(R.string.pref_lastport), port);
      editor.commit();

      // Initialize proxy
      proxyConfiguration.put("handler", "main");
      proxyConfiguration.put("main.prefix", "");
      proxyConfiguration.put("main.class", "sunlabs.brazil.server.ChainHandler");
      switch (this.getProxyRegistrationType().getProxyType())
      {
      case HTTP:
        proxyConfiguration.put("main.handlers", "urlmodifier adblock");
        proxyConfiguration.put("urlmodifier.class", "org.adblockplus.brazil.TransparentProxyHandler");
        break;
      case HTTPS:
        proxyConfiguration.put("main.handlers", "https adblock");
        proxyConfiguration.put("https.class", "org.adblockplus.brazil.SSLConnectionHandler");
        break;
      default:
        sendBroadcast(new Intent(BROADCAST_PROXY_FAILED)
            .putExtra("msg", "Unsupported proxy server type: " + this.getProxyRegistrationType().getProxyType()));
        return;
      }
      proxyConfiguration.put("adblock.class", "org.adblockplus.brazil.RequestHandler");
      if (LOG_REQUESTS)
        proxyConfiguration.put("adblock.proxylog", "yes");

      configureUserProxy(proxyConfiguration, proxyHost, proxyPort, proxyExcl, proxyUser, proxyPass);

      proxy = new ProxyServer();
      proxy.logLevel = Server.LOG_LOG;
      proxy.setup(listen, proxyConfiguration.getProperty("handler"), proxyConfiguration);
      proxy.start();
    }

    prefs.registerOnSharedPreferenceChangeListener(this);

    // Lock service
    hideIcon = prefs.getBoolean(getString(R.string.pref_hideicon), getResources().getBoolean(R.bool.def_hideicon));
    startForeground(AdblockPlus.ONGOING_NOTIFICATION_ID, getNotification());

    sendStateChangedBroadcast();

    this.startNotificationWatcher();

    Log.i(TAG, "Service started");
  }

  @Override
  public int onStartCommand(final Intent intent, final int flags, final int startId)
  {
    return START_STICKY;
  }

  @Override
  public void onDestroy()
  {
    super.onDestroy();

    this.stopNotificationWatcher();

    unregisterReceiver(this.proxyReceiver);
    unregisterReceiver(this.proxyStateChangedReceiver);

    if (this.proxyConfigurator != null)
    {
      this.proxyConfigurator.unregisterProxy();
      this.proxyConfigurator.shutdown();
    }

    sendBroadcast(new Intent(BROADCAST_STATE_CHANGED).putExtra("enabled", false));

    // Stop proxy server
    if (proxy != null)
    {
      proxy.close();
      proxy = null;
    }

    // Release service lock
    stopForeground(true);

    Log.i(TAG, "Service stopped");
  }

  /**
   * Sets user proxy settings in proxy service properties.
   */
  private void configureUserProxy(final Properties config, final String proxyHost, final String proxyPort, final String proxyExcl,
      final String proxyUser, final String proxyPass)
  {
    // Clean previous settings
    config.remove("adblock.proxyHost");
    config.remove("adblock.proxyPort");
    config.remove("adblock.auth");
    config.remove("adblock.proxyExcl");
    config.remove("https.proxyHost");
    config.remove("https.proxyPort");
    config.remove("https.auth");

    final ProxyRegistrationType regType = this.getProxyRegistrationType();
    if (regType == ProxyRegistrationType.NATIVE)
    {
      passProxySettings(proxyHost, proxyPort, proxyExcl);
    }

    // Check if there are any settings
    if (StringUtils.isEmpty(proxyHost))
    {
      return;
    }

    // Check for dirty proxy settings - this indicated previous crash:
    // proxy points to ourselves
    // proxy port is null, 0 or not a number
    // proxy is 127.0.0.1:8080
    if (proxyPort == null)
    {
      return;
    }

    final int p;
    try
    {
      p = Integer.valueOf(proxyPort);
    }
    catch (final NumberFormatException e)
    {
      return;
    }

    if (p == 0 || isLocalhost(proxyHost) && (p == port || p == 8080))
    {
      if (regType == ProxyRegistrationType.NATIVE)
      {
        passProxySettings(null, null, null);
      }
      return;
    }

    config.put("adblock.proxyHost", proxyHost);
    config.put("adblock.proxyPort", proxyPort);
    if (regType.getProxyType() == ProxyServerType.HTTPS)
    {
      config.put("https.proxyHost", proxyHost);
      config.put("https.proxyPort", proxyPort);
    }

    // TODO Not implemented in our proxy but needed to restore settings
    if (proxyExcl != null)
    {
      config.put("adblock.proxyExcl", proxyExcl);
    }

    if (StringUtils.isNotEmpty(proxyUser) && StringUtils.isNotEmpty(proxyPass))
    {
      // Base64 encode user:password
      final String proxyAuth = "Basic " + new String(Base64.encode(proxyUser + ":" + proxyPass));
      config.put("adblock.auth", proxyAuth);
      if (regType.getProxyType() == ProxyServerType.HTTPS)
      {
        config.put("https.auth", proxyAuth);
      }
    }
  }

  /**
   * @return {@code true} if the given host string resolves to {@code localhost}
   */
  public static boolean isLocalhost(final String host)
  {
    try
    {
      if (StringUtils.isEmpty(host))
      {
        return false;
      }

      if (host.equals("127.0.0.1") || host.equalsIgnoreCase("localhost"))
      {
        return true;
      }

      return InetAddress.getByName(host).isLoopbackAddress();
    }
    catch (final Exception e)
    {
      return false;
    }
  }

  private void passProxySettings(final String proxyHost, final String proxyPort, final String proxyExcl)
  {
    try
    {
      final CrashHandler handler = (CrashHandler) Thread.getDefaultUncaughtExceptionHandler();
      handler.saveProxySettings(proxyHost, proxyPort, proxyExcl);
    }
    catch (final ClassCastException e)
    {
      // ignore - default handler in use
    }
  }

  @Override
  public void onSharedPreferenceChanged(final SharedPreferences sharedPreferences, final String key)
  {
    if (this.getProxyRegistrationType() != ProxyRegistrationType.NATIVE)
    {
      final String keyHost = getString(R.string.pref_proxyhost);
      final String keyPort = getString(R.string.pref_proxyport);
      final String keyUser = getString(R.string.pref_proxyuser);
      final String keyPass = getString(R.string.pref_proxypass);
      if (key.equals(keyHost) || key.equals(keyPort) || key.equals(keyUser) || key.equals(keyPass))
      {
        final String proxyHost = sharedPreferences.getString(keyHost, null);
        final String proxyPort = sharedPreferences.getString(keyPort, null);
        final String proxyUser = sharedPreferences.getString(keyUser, null);
        final String proxyPass = sharedPreferences.getString(keyPass, null);
        if (proxy != null)
        {
          configureUserProxy(proxyConfiguration, proxyHost, proxyPort, null, proxyUser, proxyPass);
          proxy.restart(proxyConfiguration.getProperty("handler"));
        }
      }
    }
  }

  /**
   * @return the active proxy configuration's type or {@link ProxyRegistrationType#UNKNOWN} if none is configured
   */
  private ProxyRegistrationType getProxyRegistrationType()
  {
    return this.proxyConfigurator != null ? this.proxyConfigurator.getType() : ProxyRegistrationType.UNKNOWN;
  }

  /**
   * @return {@code true} if there is a valid proxy configurator and registration succeeded
   */
  public boolean isRegistered()
  {
    return this.proxyConfigurator != null && this.proxyConfigurator.isRegistered();
  }

  /**
   * @return {@code true} if registration type is {@link ProxyRegistrationType#NATIVE} and registration succeeded
   */
  public boolean isNativeProxyAutoConfigured()
  {
    return this.getProxyRegistrationType() == ProxyRegistrationType.NATIVE && this.isRegistered();
  }

  /**
   * @return {@code true} if registration type is {@link ProxyRegistrationType#MANUAL}
   */
  public boolean isManual()
  {
    return this.getProxyRegistrationType() == ProxyRegistrationType.MANUAL;
  }

  /**
   * @return {@code true} if registration type is {@link ProxyRegistrationType#IPTABLES}
   */
  public boolean isIptables()
  {
    return this.getProxyRegistrationType() == ProxyRegistrationType.IPTABLES;
  }

  @SuppressLint("NewApi")
  private Notification getNotification()
  {
    final boolean filtering = AdblockPlus.getApplication().isFilteringEnabled();

    final int msgId;
    switch(this.getProxyRegistrationType())
    {
    case MANUAL:
      if (this.isRegistered())
      {
        msgId = filtering ? R.string.notif_wifi : R.string.notif_wifi_nofiltering;
      }
      else
      {
        msgId = filtering ? R.string.notif_waiting : R.string.notif_wifi_nofiltering;
      }
      break;
    case NATIVE:
      msgId = filtering ? R.string.notif_wifi : R.string.notif_wifi_nofiltering;
      break;
    case IPTABLES:
    case CYANOGENMOD:
      msgId = R.string.notif_all;
      break;
    default:
      msgId = R.string.notif_waiting;
      break;
    }

    final NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
    if (hideIcon && msgId != R.string.notif_waiting)
    {
      builder.setWhen(POSITION_RIGHT);
      builder.setSmallIcon(R.drawable.transparent);
    }
    else
    {
      builder.setWhen(0);
      builder.setSmallIcon(R.drawable.ic_stat_blocking);
    }
    final PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
        new Intent(this, Preferences.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK), 0);

    builder.setContentIntent(contentIntent);
    builder.setContentTitle(getText(R.string.app_name));
    builder.setContentText(getString(msgId, port));
    builder.setOngoing(true);

    return builder.getNotification();
  }

  public void setEmptyIcon(final boolean hide)
  {
    hideIcon = hide;
    final NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    notificationManager.notify(AdblockPlus.ONGOING_NOTIFICATION_ID, getNotification());
  }

  public void sendStateChangedBroadcast()
  {
    final boolean manual = isManual();
    final Intent stateIntent = new Intent(BROADCAST_STATE_CHANGED)
        .putExtra("enabled", true)
        .putExtra("port", port)
        .putExtra("manual", manual);

    if (manual)
    {
      stateIntent.putExtra("configured", this.isRegistered());
    }

    sendBroadcast(stateIntent);
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
  public IBinder onBind(final Intent intent)
  {
    return binder;
  }

  /**
   * Stops service if proxy fails.
   */
  private final BroadcastReceiver proxyReceiver = new BroadcastReceiver()
  {
    @Override
    public void onReceive(final Context context, final Intent intent)
    {
      if (intent.getAction().equals(ProxyService.BROADCAST_PROXY_FAILED))
      {
        stopSelf();
      }
    }
  };

  /**
   * <p>
   * Proxy state change receiver.
   * </p>
   */
  private final BroadcastReceiver proxyStateChangedReceiver = new BroadcastReceiver()
  {
    @Override
    public void onReceive(final Context context, final Intent intent)
    {
      if (intent != null && PROXY_STATE_CHANGED_ACTION.equals(intent.getAction()))
      {
        final NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(AdblockPlus.ONGOING_NOTIFICATION_ID, getNotification());
        ProxyService.this.sendStateChangedBroadcast();
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
      catch (final Exception e)
      {
        // ignore - it always happens
      }
      log(LOG_WARNING, null, "server stopped");
    }

    @Override
    public void log(final int level, final Object obj, final String message)
    {
      if (level <= logLevel)
      {
        Log.println(7 - level, obj != null ? obj.toString() : TAG, message);
      }
    }
  }

  private void startNotificationWatcher()
  {
    this.stopNotificationWatcher();

    this.notificationWatcher = new NotificationWatcher(this);

    final Thread thread = new Thread(this.notificationWatcher);
    thread.setDaemon(true);
    thread.start();
  }

  private void stopNotificationWatcher()
  {
    if (this.notificationWatcher != null)
    {
      this.notificationWatcher.stop();
      this.notificationWatcher = null;
    }
  }

  private static class NotificationWatcher implements Runnable
  {
    public static final int DEFAULT_POLL_DELAY = 3 * 60; /* seconds */
    public static final int DEFAULT_POLL_INTERVAL = 0; /* seconds */

    private final ProxyService proxyService;
    private final int pollDelay;
    private final int pollInterval;

    volatile boolean running = true;

    public NotificationWatcher(final ProxyService proxyService, final int pollDelay,
        final int pollInterval)
    {
      this.proxyService = proxyService;
      this.pollDelay = Math.max(0, pollDelay);
      this.pollInterval = Math.max(0, pollInterval);
    }

    public NotificationWatcher(final ProxyService proxyService)
    {
      this(proxyService, DEFAULT_POLL_DELAY, DEFAULT_POLL_INTERVAL);
    }

    protected void stop()
    {
      this.running = false;
    }

    @Override
    public void run()
    {
      if (this.pollDelay == 0)
      {
        this.running = false;
        return;
      }

      long nextPoll = System.currentTimeMillis() + 1000L * this.pollDelay;

      while (this.running)
      {
        try
        {
          Thread.sleep(250);
        }
        catch (InterruptedException ex)
        {
          break;
        }

        if (System.currentTimeMillis() >= nextPoll && this.running)
        {
          try
          {
            Log.d(TAG, "Polling for notifications");
            org.adblockplus.libadblockplus.Notification notification =
                AdblockPlus.getApplication().getNextNotificationToShow();

            while (notification != null
                && (notification.getType() == Type.INVALID || notification.getType() == Type.QUESTION))
            {
              notification = AdblockPlus.getApplication().getNextNotificationToShow();
            }

            if (notification != null)
            {
              final NotificationManager notificationManager = (NotificationManager) this.proxyService
                  .getSystemService(NOTIFICATION_SERVICE);

              notificationManager.notify(AdblockPlus.SERVER_NOTIFICATION_ID,
                  new NotificationCompat.Builder(this.proxyService.getApplicationContext())
                      .setSmallIcon(R.drawable.ic_stat_blocking)
                      .setContentTitle(notification.getTitle())
                      .setContentText(notification.getMessageString())
                      .getNotification());
            }
          }
          catch (Exception ex)
          {
            Log.e(TAG, "Polling for notifications failed: " + ex.getMessage(), ex);
          }

          if (this.pollInterval == 0)
          {
            this.running = false;
          }
          else
          {
            nextPoll = System.currentTimeMillis() + 1000L * this.pollInterval;
          }
        }
      }
    }
  }
}
