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

import org.adblockplus.android.R;
import org.adblockplus.android.logging.LogEntry.LogEntryViewHolder;

import com.actionbarsherlock.app.SherlockListActivity;

import android.os.Bundle;
import android.text.ClipboardManager;
import android.view.View;
import android.widget.AdapterView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;

/**
 * Logging UI.
 */
public class LogViewer extends SherlockListActivity implements OnItemClickListener, OnItemLongClickListener
{
  private ListView listView;
  private LogEntryAdapter logEntryAdapter;

  private int topItemPosition = -1;

  @Override
  public void onCreate(final Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);

    logEntryAdapter = new LogEntryAdapter(this);
    listView = getListView();
    listView.setTranscriptMode(ListView.TRANSCRIPT_MODE_NORMAL);
    listView.setAdapter(logEntryAdapter);
    listView.setOnItemClickListener(this);
    listView.setOnItemLongClickListener(this);

    if (savedInstanceState != null)
      topItemPosition = savedInstanceState.getInt("topItemPosition");
  }

  @Override
  protected void onSaveInstanceState(final Bundle outState)
  {
    outState.putInt("topItemPosition", listView.getFirstVisiblePosition());
    super.onSaveInstanceState(outState);
  }

  @Override
  public void onResume()
  {
    super.onResume();
    logEntryAdapter.initialize();

    // Restore scroll position
    // This has to be posted to the end of listView's message queue to ensure that
    // the adapter update above is complete before setSelection(). (Android Issue: 6741)
    listView.post(new Runnable()
    {
      @Override
      public void run()
      {
        listView.setSelection(topItemPosition >= 0 ? topItemPosition : logEntryAdapter.getCount() - 1);
      }
    });
  }

  @Override
  public void onPause()
  {
    super.onPause();
    logEntryAdapter.close();
  }

  @Override
  public void onItemClick(final AdapterView<?> parent, final View view,
      final int position, final long id)
  {
    final LogEntryViewHolder viewHolder = (LogEntryViewHolder) view.getTag();
    final TextView requestView = viewHolder.getRequestView();

    final boolean expand = requestView.getLineCount() == 1;
    requestView.setSingleLine(!expand);
    logEntryAdapter.getItem(position).setExpanded(expand);
  }

  @Override
  public boolean onItemLongClick(final AdapterView<?> parent, final View view,
      final int position, final long id)
  {
    final ClipboardManager manager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
    final LogEntryViewHolder viewHolder = (LogEntryViewHolder) view.getTag();

    manager.setText(viewHolder.getRequestView().getText());
    Toast.makeText(this, R.string.msg_clipboard, Toast.LENGTH_SHORT).show();
    return true;
  }
}