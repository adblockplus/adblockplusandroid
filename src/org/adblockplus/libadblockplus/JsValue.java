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

import java.util.List;

public class JsValue implements Disposable
{
  private final Disposer disposer;
  protected final long ptr;

  static
  {
    System.loadLibrary("adblockplus-jni");
    registerNatives();
  }

  protected JsValue(final long ptr)
  {
    this.ptr = ptr;
    this.disposer = new Disposer(this, new DisposeWrapper(ptr));
  }

  @Override
  public void dispose()
  {
    this.disposer.dispose();
  }

  public boolean isUndefined()
  {
    return isUndefined(this.ptr);
  }

  public boolean isNull()
  {
    return isNull(this.ptr);
  }

  public boolean isString()
  {
    return isString(this.ptr);
  }

  public boolean isNumber()
  {
    return isNumber(this.ptr);
  }

  public boolean isBoolean()
  {
    return isBoolean(this.ptr);
  }

  public boolean isObject()
  {
    return isObject(this.ptr);
  }

  public boolean isArray()
  {
    return isArray(this.ptr);
  }

  public boolean isFunction()
  {
    return isFunction(this.ptr);
  }

  public String asString()
  {
    return asString(this.ptr);
  }

  public long asLong()
  {
    return asLong(this.ptr);
  }

  public boolean asBoolean()
  {
    return asBoolean(this.ptr);
  }

  public JsValue getProperty(final String name)
  {
    return getProperty(this.ptr, name);
  }

  public List<JsValue> asList()
  {
    return asList(this.ptr);
  }

  @Override
  public String toString()
  {
    return asString(this.ptr);
  }

  private final static class DisposeWrapper implements Disposable
  {
    private final long ptr;

    public DisposeWrapper(final long ptr)
    {
      this.ptr = ptr;
    }

    @Override
    public void dispose()
    {
      dtor(this.ptr);
    }
  }

  private final static native void registerNatives();

  private final static native boolean isUndefined(long ptr);

  private final static native boolean isNull(long ptr);

  private final static native boolean isString(long ptr);

  private final static native boolean isNumber(long ptr);

  private final static native boolean isBoolean(long ptr);

  private final static native boolean isObject(long ptr);

  private final static native boolean isArray(long ptr);

  private final static native boolean isFunction(long ptr);

  private final static native String asString(long ptr);

  private final static native long asLong(long ptr);

  private final static native boolean asBoolean(long ptr);

  private final static native JsValue getProperty(long ptr, String name);

  private final static native List<JsValue> asList(long ptr);

  private final static native void dtor(long ptr);
}
