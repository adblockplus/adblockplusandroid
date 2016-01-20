/*
 * This file is part of Adblock Plus <https://adblockplus.org/>,
 * Copyright (C) 2006-2016 Eyeo GmbH
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

package org.adblockplus.libadblockplus;

import java.util.concurrent.LinkedBlockingQueue;

import org.adblockplus.android.Utils;

import android.util.Log;

public final class JniExceptionHandler
{
  private final static String TAG = Utils.getTag(JniExceptionHandler.class);

  private static LogWorker logWorker = null;

  static
  {
    logWorker = new LogWorker();
    final Thread t = new Thread(logWorker);
    t.setDaemon(true);
    t.start();
  }

  public static void logException(final Throwable t)
  {
    logWorker.logException(t);
  }

  private final static class LogWorker implements Runnable
  {
    LinkedBlockingQueue<Throwable> exceptionQueue = new LinkedBlockingQueue<Throwable>();

    private void logException(final Throwable t)
    {
      this.exceptionQueue.offer(t);
    }

    @Override
    public void run()
    {
      for (;;)
      {
        try
        {
          final Throwable t = this.exceptionQueue.take();
          Log.e(TAG, "Exception from JNI", t);
        }
        catch (final InterruptedException ie)
        {
          break;
        }
        catch (final Throwable ex)
        {
          // TODO: Swallow or log?
        }
      }
    }
  }
}
