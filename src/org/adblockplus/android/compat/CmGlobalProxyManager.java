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

package org.adblockplus.android.compat;

import android.app.Activity;
import android.content.Context;

/**
 * Wrapper for CyanogenMod GlobalProxyManager API.
 */
public class CmGlobalProxyManager
{
  private final Object proxyManager;

  public final static String GLOBAL_PROXY_STATE_CHANGE_ACTION = "cyanogenmod.intent.action.GLOBAL_PROXY_STATE_CHANGE";

  public CmGlobalProxyManager(final Context context) throws CompatibilityException
  {
    try
    {
      this.proxyManager = Class.forName("org.cyanogenmod.support.proxy.GlobalProxyManager")
          .getConstructor(Context.class)
          .newInstance(context);
    }
    catch (final Exception e)
    {
      throw new CompatibilityException("Could not create GlobalProxyManager instance", e);
    }
  }

  public boolean isPackageCurrentManager() throws CompatibilityException
  {
    try
    {
      return ((Boolean) this.proxyManager.getClass().getMethod("isPackageCurrentManager")
          .invoke(this.proxyManager)).booleanValue();
    }
    catch (final Exception e)
    {
      throw new CompatibilityException(e);
    }
  }

  public void requestGlobalProxy(final Activity activity) throws CompatibilityException
  {
    try
    {
      this.proxyManager.getClass().getMethod("requestGlobalProxy", Activity.class)
          .invoke(this.proxyManager, activity);
    }
    catch (final Exception e)
    {
      throw new CompatibilityException(e);
    }
  }

  /**
   * <p>
   * Set a network-independent global http proxy. This is not normally what you want for typical HTTP proxies - they are general network dependent.
   * However if you're doing something unusual like general internal filtering this may be useful. On a private network where the proxy is not
   * accessible, you may break HTTP using this.
   * </p>
   * <p>
   * This method requires the call to hold the permission {@code cyanogenmod.permission.GLOBAL_PROXY_MANAGEMENT}.
   * </p>
   *
   * @param proxyProperties
   *          The a {@link CmProxyProperites} object defining the new global HTTP proxy. A {@code null} value will clear the global HTTP proxy.
   */
  public void setGlobalProxy(final ProxyProperties p) throws CompatibilityException
  {
    try
    {
      this.proxyManager.getClass().getMethod("setGlobalProxy", Class.forName("org.cyanogenmod.support.proxy.CmProxyProperties"))
          .invoke(this.proxyManager, p.toCmProxyProperties());
    }
    catch (final CompatibilityException ce)
    {
      throw ce;
    }
    catch (final Exception e)
    {
      throw new CompatibilityException(e);
    }
  }

  /**
   * <p>
   * Retrieve any network-independent global HTTP proxy.
   * </p>
   * <p>
   * This method requires the call to hold the permission {@link cyanogenmod.permission.GLOBAL_PROXY_MANAGEMENT}.
   * </p>
   *
   * @return {@link ProxyProperties} for the current global HTTP proxy or {@code null} if no global HTTP proxy is set.
   * @throws CompatibilityException
   */
  public ProxyProperties getGlobalProxy() throws CompatibilityException
  {
    try
    {
      return ProxyProperties.fromObject(this.proxyManager.getClass().getMethod("getGlobalProxy")
          .invoke(this.proxyManager));
    }
    catch (final CompatibilityException ce)
    {
      throw ce;
    }
    catch (final Exception e)
    {
      throw new CompatibilityException(e);
    }
  }
}
