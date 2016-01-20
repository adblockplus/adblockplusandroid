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

public final class FilterEngine implements Disposable
{
  private final Disposer disposer;
  protected final long ptr;

  static
  {
    System.loadLibrary("adblockplus-jni");
    registerNatives();
  }

  public static enum ContentType
  {
    OTHER, SCRIPT, IMAGE, STYLESHEET, OBJECT, SUBDOCUMENT, DOCUMENT, XMLHTTPREQUEST,
    OBJECT_SUBREQUEST, FONT, MEDIA
  }

  public FilterEngine(final JsEngine jsEngine)
  {
    this.ptr = ctor(jsEngine.ptr);
    this.disposer = new Disposer(this, new DisposeWrapper(this.ptr));
  }

  public boolean isFirstRun()
  {
    return isFirstRun(this.ptr);
  }

  public Filter getFilter(final String text)
  {
    return getFilter(this.ptr, text);
  }

  public List<Filter> getListedFilters()
  {
    return getListedFilters(this.ptr);
  }

  public Subscription getSubscription(final String url)
  {
    return getSubscription(this.ptr, url);
  }

  public List<Subscription> getListedSubscriptions()
  {
    return getListedSubscriptions(this.ptr);
  }

  public List<Subscription> fetchAvailableSubscriptions()
  {
    return fetchAvailableSubscriptions(this.ptr);
  }

  public void removeUpdateAvailableCallback()
  {
    removeUpdateAvailableCallback(this.ptr);
  }

  public void setUpdateAvailableCallback(final UpdateAvailableCallback callback)
  {
    setUpdateAvailableCallback(this.ptr, callback.ptr);
  }

  public void removeFilterChangeCallback()
  {
    removeFilterChangeCallback(this.ptr);
  }

  public void setFilterChangeCallback(final FilterChangeCallback callback)
  {
    setFilterChangeCallback(this.ptr, callback.ptr);
  }

  public void forceUpdateCheck()
  {
    forceUpdateCheck(this.ptr, 0);
  }

  public void forceUpdateCheck(final UpdateCheckDoneCallback callback)
  {
    forceUpdateCheck(this.ptr, callback != null ? callback.ptr : 0);
  }

  public List<String> getElementHidingSelectors(final String domain)
  {
    return getElementHidingSelectors(this.ptr, domain);
  }

  public void showNextNotification(final String url)
  {
    showNextNotification(this.ptr, url);
  }

  public void showNextNotification()
  {
    showNextNotification(this.ptr, null);
  }

  public void setShowNotificationCallback(final ShowNotificationCallback callback)
  {
    setShowNotificationCallback(this.ptr, callback.ptr);
  }

  public void removeShowNotificationCallback()
  {
    removeShowNotificationCallback(this.ptr);
  }

  public Filter matches(final String url, final ContentType contentType, final String documentUrl)
  {
    return matches(this.ptr, url, contentType, documentUrl);
  }

  public Filter matches(final String url, final ContentType contentType, final String[] documentUrls)
  {
    return matches(this.ptr, url, contentType, documentUrls);
  }

  public boolean isDocumentWhitelisted(String url, String[] documentUrls)
  {
    return isDocumentWhitelisted(this.ptr, url, documentUrls);
  }

  public boolean isElemhideWhitelisted(String url, String[] documentUrls)
  {
    return isElemhideWhitelisted(this.ptr, url, documentUrls);
  }

  public JsValue getPref(final String pref)
  {
    return getPref(this.ptr, pref);
  }

  public void setPref(final String pref, final JsValue value)
  {
    setPref(this.ptr, pref, value.ptr);
  }

  @Override
  public void dispose()
  {
    this.disposer.dispose();
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

  private final static native long ctor(long jsEnginePtr);

  private final static native boolean isFirstRun(long ptr);

  private final static native Filter getFilter(long ptr, String text);

  private final static native List<Filter> getListedFilters(long ptr);

  private final static native Subscription getSubscription(long ptr, String url);

  private final static native List<Subscription> getListedSubscriptions(long ptr);

  private final static native List<Subscription> fetchAvailableSubscriptions(long ptr);

  private final static native void removeUpdateAvailableCallback(long ptr);

  private final static native void setUpdateAvailableCallback(long ptr, long filterPtr);

  private final static native void removeFilterChangeCallback(long ptr);

  private final static native void setFilterChangeCallback(long ptr, long filterPtr);

  private final static native void forceUpdateCheck(long ptr, long updatePtr);

  private final static native List<String> getElementHidingSelectors(long ptr, String domain);

  private final static native void showNextNotification(long ptr, String url);

  private final static native void setShowNotificationCallback(long ptr, long callbackPtr);

  private final static native void removeShowNotificationCallback(long ptr);

  private final static native JsValue getPref(long ptr, String pref);

  private final static native Filter matches(long ptr, String url, ContentType contentType, String documentUrl);

  private final static native Filter matches(long ptr, String url, ContentType contentType, String[] documentUrls);

  private final static native boolean isDocumentWhitelisted(long ptr, String url, String[] documentUrls);

  private final static native boolean isElemhideWhitelisted(long ptr, String url, String[] documentUrls);

  private final static native void setPref(long ptr, String pref, long valuePtr);

  private final static native void dtor(long ptr);
}
