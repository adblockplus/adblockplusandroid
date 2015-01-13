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

import java.net.InetAddress;

/**
 * Interface for different proxy registration types. Constructor must always succeed, do any work that may fail in
 * {@link ProxyConfigurator#initialize()}.
 */
public interface ProxyConfigurator
{
  /**
   * Initializes this {@code ProxyConfigurator}. This function will normally get called only once.
   *
   * @return {@code true} if initialization succeeded
   */
  public boolean initialize();

  /**
   * Registers the proxy. May get called multiple times (e.g. for enable/disable).
   *
   * @param address
   *          The address ...
   * @param port
   *          ... and port to bind to
   * @return {@code true} if registration succeeded
   */
  public boolean registerProxy(InetAddress address, int port);

  /**
   * Unregisters the proxy. May get called multiple times (e.g. for enable/disable).
   */
  public void unregisterProxy();

  /**
   * Shuts down this configurator, normally called on application exit to perform any clean up.
   *
   * @param context
   *          Service/Application context
   */
  public void shutdown();

  /**
   * @return {@code true} if we actually registered the proxy
   */
  public boolean isRegistered();

  /**
   * Returning {@code true} here will allow the configurator to succeed on {@link #initialize()} and fail on {@link #registerProxy(InetAddress, int)}.
   * This situation could arise for the CyanogenMod and iptables case.
   *
   * @return {@code true} to disable auto-advancing in configurator list
   */
  public boolean isSticky();

  /**
   * @return the registration type of this configurator.
   */
  public ProxyRegistrationType getType();
}
