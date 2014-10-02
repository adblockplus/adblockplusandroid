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

package org.adblockplus.android.logging;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import android.widget.TextView;

/**
 * Data for a log entry.
 */
public final class LogEntry
{
  private final long id;
  private final long timestamp;
  private final boolean blocked;
  private final String request;
  private final String filter;
  private final List<String> subscriptions;

  private static final AtomicLong counter = new AtomicLong();

  private boolean expanded;

  public LogEntry(final boolean blocked, final String request, final String filter, final List<String> subscriptions)
  {
    this.id = counter.incrementAndGet();
    this.timestamp = System.currentTimeMillis();

    this.blocked = blocked;
    this.request = request;
    this.filter = filter;
    this.subscriptions = subscriptions;
  }

  public long getId()
  {
    return id;
  }

  public long getTimestamp()
  {
    return timestamp;
  }

  public boolean isBlocked()
  {
    return blocked;
  }

  public String getRequest()
  {
    return request;
  }

  public String getFilter()
  {
    return filter;
  }

  public List<String> getSubscriptions()
  {
    return subscriptions;
  }

  public boolean isExpanded()
  {
    return expanded;
  }

  public void setExpanded(final boolean expanded)
  {
    this.expanded = expanded;
  }

  /**
   * View holder for a log entry.
   */
  public static final class LogEntryViewHolder
  {
    private final TextView timestampView;
    private final TextView requestView;

    public LogEntryViewHolder(final TextView timestampView, final TextView requestView)
    {
      this.timestampView = timestampView;
      this.requestView = requestView;
    }

    public TextView getTimestampView()
    {
      return timestampView;
    }

    public TextView getRequestView()
    {
      return requestView;
    }
  }
}