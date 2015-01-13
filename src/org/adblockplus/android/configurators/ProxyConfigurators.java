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

package org.adblockplus.android.configurators;

import java.lang.reflect.Constructor;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;

import org.adblockplus.android.Utils;

import android.content.Context;
import android.util.Log;

/**
 * Common proxy configurator methods.
 */
public final class ProxyConfigurators
{
  private static final String TAG = Utils.getTag(ProxyConfigurators.class);
  private static final ArrayList<Class<?>> CONFIGURATORS = new ArrayList<Class<?>>();
  private static final HashMap<ProxyRegistrationType, Class<?>> CONFIGURATOR_MAP = new HashMap<ProxyRegistrationType, Class<?>>();
  private static final HashMap<Class<?>, ProxyRegistrationType> CONFIGURATOR_INV_MAP = new HashMap<Class<?>, ProxyRegistrationType>();

  static
  {
    /*
     * Add new proxy-configurators here.
     *
     * Note: order of adding is important (as later added configurators act as fallbacks for earlier ones).
     */
    add(CyanogenProxyConfigurator.class);
    add(IptablesProxyConfigurator.class);
    add(NativeProxyConfigurator.class);
    add(ManualProxyConfigurator.class);
  }

  private ProxyConfigurators()
  {
    // no instantiation
  }

  private static void add(final Class<?> clazz)
  {
    try
    {
      final Constructor<?> ctor = clazz.getConstructor(Context.class);
      final ProxyConfigurator pc = (ProxyConfigurator) ctor.newInstance((Context) null);
      final ProxyRegistrationType type = pc.getType();

      if (!CONFIGURATOR_MAP.containsKey(type))
      {
        CONFIGURATORS.add(clazz);
        CONFIGURATOR_MAP.put(type, clazz);
        CONFIGURATOR_INV_MAP.put(clazz, type);
      }
      else
      {
        // We fail hard here.
        throw new IllegalArgumentException("Duplicate proxy configurator (" + clazz + ") for type: " + type);
      }
    }
    catch (final Exception e)
    {
      // We fail hard here.
      throw new IllegalStateException("Failed to register proxy configurator (" + clazz + ")", e);
    }
  }

  private static ProxyConfigurator initialize(final Context context, final ProxyConfigurator from)
  {
    int i = 0;

    if (from != null)
    {
      // Scan through configurators to find last used one
      while (i < CONFIGURATORS.size() && !CONFIGURATORS.get(i).equals(from.getClass()))
      {
        i++;
      }
      // One after last-used
      i++;
    }

    // Loop through configurators until one succeeds to initialize
    while (i < CONFIGURATORS.size())
    {
      try
      {
        final Class<?> clazz = CONFIGURATORS.get(i);
        final Constructor<?> ctor = clazz.getConstructor(Context.class);
        final ProxyConfigurator pc = (ProxyConfigurator) ctor.newInstance(context);
        if (pc.initialize())
        {
          return pc;
        }
      }
      catch (final Exception e)
      {
        Log.d(TAG, "Configurator exception", e);
        // We don't need to handle exceptions here, only success matters
      }

      i++;
    }

    return null;
  }

  public static ProxyConfigurator registerProxy(final Context context, final InetAddress address, final int port)
  {
    ProxyConfigurator pc = null;

    do
    {
      pc = initialize(context, pc);

      if (pc != null)
      {
        if (pc.registerProxy(address, port))
        {
          Log.d(TAG, "Using '" + pc.toString() + "'");
          return pc;
        }

        pc.shutdown();
      }
    }
    while (pc != null);

    return null;
  }
}
