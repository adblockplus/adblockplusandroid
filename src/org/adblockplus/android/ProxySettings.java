/*
 * This file is part of Adblock Plus <http://adblockplus.org/>,
 * Copyright (C) 2006-2013 Eyeo GmbH
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

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Parcelable;
import android.util.Log;

public class ProxySettings
{
  private static final String TAG = "ProxySettings";

  public static Object getActiveLinkProxy(ConnectivityManager connectivityManager) throws Exception
  {
    /*
     * LinkProperties lp = connectivityManager.getActiveLinkProperties()
     */
    Method method = connectivityManager.getClass().getMethod("getActiveLinkProperties");
    Object lp = method.invoke(connectivityManager);

    Object pp = ProxySettings.getLinkProxy(lp);
    return pp;
  }

  /**
   * Reads proxy settings from link properties on Android 3.1+ using Java
   * reflection.
   * 
   * @param linkProperties
   *          android.net.LinkProperties
   * @return ProxyProperties
   * @throws Exception
   */
  public static Object getLinkProxy(Object linkProperties) throws Exception
  {
    /*
     * linkProperties.getHttpProxy();
     */
    Method method = linkProperties.getClass().getMethod("getHttpProxy");
    Object pp = method.invoke(linkProperties);
    return pp;
  }

  /**
   * Reads system proxy settings on Android 3.1+ using Java reflection.
   * 
   * @return string array of host, port and exclusion list
   */
  public static String[] getUserProxy(Context context)
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
      ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
      Object pp = method.invoke(connectivityManager);
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

  /**
   * Reads system proxy settings on Android 3.1+ using Java reflection.
   * 
   * @param pp
   *          ProxyProperties object
   * @return string array of host, port and exclusion list
   * @throws Exception
   */
  protected static String[] getUserProxy(Object pp) throws Exception
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
     * String proxyEL = pp.getExclusionList()
     */
    method = c.getMethod("getExclusionList");
    userProxy[2] = (String) method.invoke(pp);

    if (userProxy[0] != null)
      return userProxy;
    else
      return null;
  }

  /**
   * Tries to set local proxy in system settings via native call on Android 3.1+
   * devices using Java reflection.
   * 
   * @return true if device supports native proxy setting
   */
  public static boolean setConnectionProxy(Context context, String host, int port, String excl)
  {
    Method method = null;
    try
    {
      /*
       * android.net.LinkProperties lp =
       * ConnectivityManager.getActiveLinkProperties();
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
      ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
      Object lp = method.invoke(connectivityManager);
      // There is no active link now, but device has native proxy support
      if (lp == null)
        return true;

      String className = "android.net.ProxyProperties";
      Class<?> c = Class.forName(className);
      method = lp.getClass().getMethod("setHttpProxy", c);
      if (host != null)
      {
        /*
         * ProxyProperties pp = new ProxyProperties(host, port, excl);
         */
        Class<?>[] parameter = new Class[] {String.class, int.class, String.class};
        Object args[] = new Object[3];
        args[0] = host;
        args[1] = Integer.valueOf(port);
        args[2] = excl;
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

      Intent intent = null;
      NetworkInfo ni = connectivityManager.getActiveNetworkInfo();
      switch (ni.getType())
      {
        case ConnectivityManager.TYPE_WIFI:
          intent = new Intent("android.net.wifi.LINK_CONFIGURATION_CHANGED");
          break;
        case ConnectivityManager.TYPE_MOBILE:
          // TODO We leave it here for future, it does not work now
          // intent = new Intent("android.intent.action.ANY_DATA_STATE");
          break;
      }
      if (intent != null)
      {
        if (lp != null)
        {
          intent.putExtra("linkProperties", (Parcelable) lp);
        }
        context.sendBroadcast(intent);
      }

      return true;
    }
    catch (SecurityException e)
    {
      // This is ok for 4.1.2+, 4.2.2+ and later
      return false;
    }
    catch (Exception e)
    {
      // This should not happen
      Log.e(TAG, "setHttpProxy failure", e);
      return false;
    }
  }
}
