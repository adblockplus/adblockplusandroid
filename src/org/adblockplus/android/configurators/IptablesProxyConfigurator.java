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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeoutException;

import org.adblockplus.android.Utils;

import android.content.Context;
import android.util.Log;

import com.stericson.RootTools.RootTools;
import com.stericson.RootTools.exceptions.RootDeniedException;
import com.stericson.RootTools.execution.Command;
import com.stericson.RootTools.execution.Shell;

/**
 * Proxy registration using RootTools and iptables.
 */
public class IptablesProxyConfigurator implements ProxyConfigurator
{
  private static final String TAG = Utils.getTag(IptablesProxyConfigurator.class);
  private static final int DEFAULT_TIMEOUT = 3000;
  private static final String IPTABLES_RETURN = " -t nat -m owner --uid-owner {{UID}} -A OUTPUT -p tcp -j RETURN\n";
  private static final String IPTABLES_ADD_HTTP = " -t nat -A OUTPUT -p tcp --dport 80 -j REDIRECT --to {{PORT}}\n";

  private final Context context;
  private String iptables;
  private boolean isRegistered = false;

  public IptablesProxyConfigurator(final Context context)
  {
    this.context = context;
  }

  private static List<String> runRootCommand(final String command, final int timeout) throws IOException, TimeoutException,
      RootDeniedException
  {
    final CapturingOutputCommand cmd = new CapturingOutputCommand(0, DEFAULT_TIMEOUT, command);

    Shell.runRootCommand(cmd);

    cmd.waitForCompletion();

    return cmd.output;
  }

  @Override
  public boolean initialize()
  {
    try
    {
      // If we don't set `handlerEnabled` to `false`, RootTools uses Handlers
      // which get executed on the UI thread which in fact renders it useless
      // for our purpose (as it either finished too late or blocks forever).
      RootTools.handlerEnabled = false;

      if (!RootTools.isAccessGiven())
      {
        throw new IllegalStateException("No root access");
      }

      final String path = getIptablesExecutablePath();

      runRootCommand("chmod 700 " + path, DEFAULT_TIMEOUT);

      boolean compatible = false;
      boolean version = false;

      final String command = path + " --version\n" + path + " -L -t nat -n\n";

      final List<String> result = runRootCommand(command, DEFAULT_TIMEOUT);

      for (final String line : result)
      {
        if (line.contains("OUTPUT"))
        {
          compatible = true;
        }
        if (line.contains("v1.4."))
        {
          version = true;
        }
      }

      if (!(compatible && version))
      {
        throw new IllegalStateException("Incompatible iptables excutable");
      }

      this.iptables = path;

      return true;
    }
    catch (final Exception e)
    {
      return false;
    }
  }

  private String getIptablesExecutablePath() throws FileNotFoundException
  {
    File iptablesExecutable = new File("/system/bin/iptables");
    if (!iptablesExecutable.exists())
    {
      Log.i(TAG, "iptables not found on the system, using embedded binary");
      iptablesExecutable = context.getFileStreamPath("iptables");
    }
    if (!iptablesExecutable.exists())
      throw new FileNotFoundException("No iptables executable");
    return iptablesExecutable.getAbsolutePath();
  }

  @Override
  public boolean registerProxy(final InetAddress address, final int port)
  {
    try
    {
      final int uid = this.context.getPackageManager().getPackageInfo(this.context.getPackageName(), 0).applicationInfo.uid;

      final StringBuilder cmd = new StringBuilder();
      cmd.append(this.iptables);
      cmd.append(IPTABLES_RETURN.replace("{{UID}}", String.valueOf(uid)));
      cmd.append('\n');
      cmd.append(this.iptables);
      cmd.append(IPTABLES_ADD_HTTP.replace("{{PORT}}", String.valueOf(port)));

      runRootCommand(cmd.toString(), DEFAULT_TIMEOUT);

      this.isRegistered = true;

      return true;
    }
    catch (final Exception e)
    {
      // I leave this logging message for now, passing 'init' and failing 'register' definitely is a failure
      Log.e(TAG, "Couldn't register proxy using iptables.", e);
      return false;
    }
  }

  @Override
  public void unregisterProxy()
  {
    try
    {
      runRootCommand(this.iptables + " -t nat -F OUTPUT", DEFAULT_TIMEOUT);
    }
    catch (final Exception e)
    {
      Log.w(TAG, "Failed to unregister proxy using iptables.", e);
    }
    finally
    {
      this.isRegistered = false;
    }
  }

  @Override
  public void shutdown()
  {
    // Nothing to do here
  }

  public static List<String> getIptablesOutput(final Context context)
  {
    try
    {
      if (!RootTools.isAccessGiven())
      {
        throw new IllegalStateException("No root access");
      }

      final File ipt = context.getFileStreamPath("iptables");

      if (!ipt.exists())
      {
        throw new FileNotFoundException("No iptables executable");
      }

      final String path = ipt.getAbsolutePath();

      runRootCommand("chmod 700 " + path, DEFAULT_TIMEOUT);

      boolean compatible = false;
      boolean version = false;

      String command = path + " --version\n" + path + " -L -t nat -n\n";

      final List<String> result = runRootCommand(command, DEFAULT_TIMEOUT);

      for (final String line : result)
      {
        if (line.contains("OUTPUT"))
        {
          compatible = true;
        }
        if (line.contains("v1.4."))
        {
          version = true;
        }
      }

      if (!(compatible && version))
      {
        throw new IllegalStateException("Incompatible iptables excutable");
      }

      command = path + " -L -t nat -n\n";

      return runRootCommand(command, DEFAULT_TIMEOUT);
    }
    catch (final Throwable t)
    {
      return null;
    }
  }

  @Override
  public ProxyRegistrationType getType()
  {
    return ProxyRegistrationType.IPTABLES;
  }

  @Override
  public boolean isRegistered()
  {
    return this.isRegistered;
  }

  @Override
  public boolean isSticky()
  {
    return false;
  }

  @Override
  public String toString()
  {
    return "[ProxyConfigurator: " + this.getType() + "]";
  }

  private final static class CapturingOutputCommand extends Command
  {
    private final Semaphore running = new Semaphore(1);

    public List<String> output = new ArrayList<String>();

    public CapturingOutputCommand(final int id, final int timeout, final String command)
    {
      super(id, timeout, command);

      this.running.acquireUninterruptibly();
    }

    @Override
    public void commandOutput(int id, String line)
    {
      this.output.add(line);
    }

    @Override
    public void commandCompleted(int id, int exitCode)
    {
      this.running.release();
    }

    @Override
    public void commandTerminated(int id, String reason)
    {
      this.running.release();
    }

    public void waitForCompletion()
    {
      this.running.acquireUninterruptibly();
    }
  }
}
