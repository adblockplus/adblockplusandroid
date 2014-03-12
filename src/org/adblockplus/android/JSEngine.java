/*
 * This file is part of Adblock Plus <http://adblockplus.org/>,
 * Copyright (C) 2006-2014 Eyeo GmbH
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
import java.io.Reader;

/**
 * Wrapper for JS executor native code.
 */
public class JSEngine
{
  private long context;

  public JSEngine(Object callback)
  {
    context = nativeInitialize(callback);
  }

  public void release()
  {
    nativeRelease(context);
  }

  public Object evaluate(String script)
  {
    if (script == null)
      throw new IllegalArgumentException("empty script");
    return nativeExecute(script, context);
  }

  public Object evaluate(Reader reader) throws IOException
  {
    return evaluate(readAll(reader));
  }

  private String readAll(Reader reader) throws IOException
  {
    StringBuilder sb = new StringBuilder();

    char[] buffer = new char[8192];
    int read;

    while ((read = reader.read(buffer, 0, buffer.length)) > 0)
    {
      sb.append(buffer, 0, read);
    }

    return sb.toString();
  }

  public Object get(String name)
  {
    return nativeGet(name, context);
  }

  public void put(String name, Object value)
  {
    nativePut(name, value, context);
  }

  public long runCallbacks()
  {
    return nativeRunCallbacks(context);
  }

  public void callback(long callback, Object[] params)
  {
    nativeCallback(callback, params, context);
  }

  private native long nativeInitialize(Object callback);

  private native void nativeRelease(long context);

  private native Object nativeExecute(String script, long context);

  private native Object nativeGet(String name, long context);

  private native Object nativePut(String name, Object value, long context);

  private native long nativeRunCallbacks(long context);

  private native void nativeCallback(long callback, Object[] params, long context);

  static
  {
    System.loadLibrary("jsEngine");
  }
}
