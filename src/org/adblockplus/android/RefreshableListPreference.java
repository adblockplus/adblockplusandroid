package org.adblockplus.android;

import android.content.Context;
import android.preference.ListPreference;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

public class RefreshableListPreference extends ListPreference
{
  private OnClickListener refreshClickListener;

  public RefreshableListPreference(Context context, AttributeSet attrs)
  {
    super(context, attrs);
  }

  @Override
  protected void onBindView(View view)
  {
    super.onBindView(view);
    final ImageView refreshImage = new ImageView(getContext());
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
    widgetFrameView.addView(refreshImage, 0);
    refreshImage.setImageResource(R.drawable.ic_menu_refresh);
    refreshImage.setPadding(refreshImage.getPaddingLeft(), refreshImage.getPaddingTop(), (int) (mDensity * rightPaddingDip), refreshImage.getPaddingBottom());
    refreshImage.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v)
      {
        if (refreshClickListener != null)
          refreshClickListener.onClick(refreshImage);
      }
    });
  }

  public void setOnRefreshClickListener(OnClickListener l)
  {
    refreshClickListener = l;
  }
}
