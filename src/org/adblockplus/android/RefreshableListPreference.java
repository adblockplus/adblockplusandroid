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

import android.content.Context;
import android.preference.ListPreference;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

/**
 * ListPreference UI with refresh button.
 */
public class RefreshableListPreference extends ListPreference
{
  private OnClickListener refreshClickListener;

  public RefreshableListPreference(final Context context, final AttributeSet attrs)
  {
    super(context, attrs);
  }

  @Override
  protected void onBindView(final View view)
  {
    super.onBindView(view);
    final ImageView refreshImage = new ImageView(getContext());
    final ViewGroup widgetFrameView = ((ViewGroup) view.findViewById(android.R.id.widget_frame));
    if (widgetFrameView == null)
      return;
    widgetFrameView.setVisibility(View.VISIBLE);
    final int rightPaddingDip = android.os.Build.VERSION.SDK_INT < 14 ? 8 : 5;
    final float density = getContext().getResources().getDisplayMetrics().density;
    if (widgetFrameView instanceof LinearLayout)
    {
      ((LinearLayout) widgetFrameView).setOrientation(LinearLayout.HORIZONTAL);
    }
    widgetFrameView.addView(refreshImage, 0);
    refreshImage.setImageResource(R.drawable.ic_menu_refresh);
    refreshImage.setPadding(refreshImage.getPaddingLeft(), refreshImage.getPaddingTop(), (int) (density * rightPaddingDip), refreshImage.getPaddingBottom());
    refreshImage.setOnClickListener(new OnClickListener()
    {
      @Override
      public void onClick(final View v)
      {
        if (refreshClickListener != null)
          refreshClickListener.onClick(refreshImage);
      }
    });
  }

  public void setOnRefreshClickListener(final OnClickListener l)
  {
    refreshClickListener = l;
  }
}
