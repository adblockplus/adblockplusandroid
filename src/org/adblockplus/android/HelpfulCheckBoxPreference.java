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
import android.content.Intent;
import android.net.Uri;
import android.preference.CheckBoxPreference;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import org.adblockplus.android.R;

public class HelpfulCheckBoxPreference extends CheckBoxPreference
{
  private OnClickListener helpClickListener;
  private String url;

  public HelpfulCheckBoxPreference(final Context context, final AttributeSet attrs)
  {
    super(context, attrs);
  }

  @Override
  protected void onBindView(final View view)
  {
    super.onBindView(view);

    final ImageView helpImage = new ImageView(getContext());
    final ViewGroup widgetFrameView = ((ViewGroup) view.findViewById(android.R.id.widget_frame));
    if (widgetFrameView == null)
      return;
    widgetFrameView.setVisibility(View.VISIBLE);
    final int rightPaddingDip = android.os.Build.VERSION.SDK_INT < 14 ? 8 : 5;
    final float mDensity = getContext().getResources().getDisplayMetrics().density;
    if (widgetFrameView instanceof LinearLayout)
    {
      ((LinearLayout) widgetFrameView).setOrientation(LinearLayout.HORIZONTAL);
    }
    widgetFrameView.addView(helpImage, 0);
    helpImage.setImageResource(R.drawable.ic_menu_help);
    helpImage.setPadding(helpImage.getPaddingLeft(), helpImage.getPaddingTop(), (int) (mDensity * rightPaddingDip), helpImage.getPaddingBottom());
    helpImage.setOnClickListener(new OnClickListener()
    {
      @Override
      public void onClick(final View v)
      {
        if (helpClickListener != null)
        {
          helpClickListener.onClick(helpImage);
        }
        else if (url != null)
        {
          final Uri uri = Uri.parse(url);
          final Intent intent = new Intent(Intent.ACTION_VIEW, uri);
          HelpfulCheckBoxPreference.this.getContext().startActivity(intent);
        }
      }
    });
  }

  public void setOnHelpClickListener(final OnClickListener l)
  {
    helpClickListener = l;
  }

  public void setHelpUrl(final String url)
  {
    this.url = url;
  }
}
