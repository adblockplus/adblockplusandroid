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

import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.adblockplus.android.AdblockPlus;
import org.adblockplus.android.R;
import org.adblockplus.android.logging.LogEntry.LogEntryViewHolder;
import org.apache.commons.lang.StringUtils;

import android.content.Context;
import android.os.Handler;
import android.text.Html;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

/**
 * A list adapter for loading log entries.
 */
public final class LogEntryAdapter extends BaseAdapter
{
  private List<LogEntry> logEntries = Collections.emptyList();

  private final Handler handler = new Handler();

  private final AdblockPlus application;

  private final LayoutInflater inflater;

  private final java.text.DateFormat systemDateFormat;

  private final String filterLabel;
  private final String subscriptionsLabel;

  public LogEntryAdapter(Context context)
  {
    inflater = LayoutInflater.from(context);
    systemDateFormat = DateFormat.getTimeFormat(context);
    filterLabel = context.getString(R.string.filter);
    subscriptionsLabel = context.getString(R.string.subscriptions);

    application = AdblockPlus.getApplication();
  }

  @Override
  public int getCount()
  {
    return logEntries.size();
  }

  @Override
  public LogEntry getItem(final int position)
  {
    return logEntries.get(position);
  }

  @Override
  public long getItemId(final int position)
  {
    return logEntries.get(position).getId();
  }

  @Override
  public boolean hasStableIds()
  {
    return true;
  }

  @Override
  public View getView(final int position, View convertView, final ViewGroup parent)
  {
    TextView timestampView, requestView;

    // Use view holder pattern for efficiency
    if (convertView == null)
    {
      convertView = inflater.inflate(R.layout.log_entry, parent, false);
      timestampView = (TextView) convertView.findViewById(R.id.log_timestamp);
      requestView = (TextView) convertView.findViewById(R.id.log_request);
      convertView.setTag(new LogEntryViewHolder(timestampView, requestView));
    }
    else
    {
      final LogEntryViewHolder viewHolder = (LogEntryViewHolder) convertView.getTag();
      timestampView = viewHolder.getTimestampView();
      requestView = viewHolder.getRequestView();
    }

    final LogEntry logEntry = logEntries.get(position);

    timestampView.setText(systemDateFormat.format(new Date(logEntry.getTimestamp())));

    final StringBuilder requestData = new StringBuilder();
    requestData.append("<font color='").append(logEntry.isBlocked() ? "red'>" : "green'>").
        append(logEntry.getRequest()).append("</font>");

    final String filter = logEntry.getFilter();
    if (StringUtils.isNotBlank(filter))
    {
      requestData.append("<br/>").append("<br/>").append(filterLabel).append("<br/>").append(filter);
      requestData.append("<br/>").append("<br/>").append(subscriptionsLabel);
      for (final String subscription : logEntry.getSubscriptions())
        requestData.append("<br/>").append(subscription);
    }

    requestView.setText(Html.fromHtml(requestData.toString()), TextView.BufferType.SPANNABLE);
    requestView.setSingleLine(!logEntry.isExpanded());

    return convertView;
  }

  public void initialize()
  {
    logEntries = application.getLogs(this);
    notifyDataSetChanged();
  }

  public void pushLog(final LogEntry logEntry)
  {
    // Update on the UI thread
    handler.post(new Runnable()
    {
      @Override
      public void run()
      {
        logEntries.add(logEntry);
        // Keep at the most 1000 records in memory
        if (logEntries.size() > 1000)
          logEntries.subList(0, 100).clear();

        notifyDataSetChanged();
      }
    });
  }

  public void close()
  {
    application.clearLogAdapter();
  }
}