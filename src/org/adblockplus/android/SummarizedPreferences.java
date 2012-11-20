/*
 * This file is part of the Adblock Plus,
 * Copyright (C) 2006-2012 Eyeo GmbH
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

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;

/**
 * PreferencesActivity which automatically sets preference summaries according
 * to its current values.
 */
public class SummarizedPreferences extends PreferenceActivity implements OnSharedPreferenceChangeListener
{
  @Override
  public void onResume()
  {
    super.onResume();
    initSummaries(getPreferenceScreen());
    getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
  }

  @Override
  public void onPause()
  {
    super.onPause();
    getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
  }

  @Override
  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
  {
    Preference pref = findPreference(key);
    setPrefSummary(pref);
  }

  protected void setPrefSummary(Preference pref)
  {
    if (pref instanceof ListPreference)
    {
      CharSequence summary = ((ListPreference) pref).getEntry();
      if (summary != null)
      {
        pref.setSummary(summary);
      }
    }
    if (pref instanceof EditTextPreference)
    {
      CharSequence summary = ((EditTextPreference) pref).getText();
      if (summary != null)
      {
        pref.setSummary(summary);
      }
    }
  }

  protected void initSummaries(PreferenceGroup preference)
  {
    for (int i = preference.getPreferenceCount() - 1; i >= 0; i--)
    {
      Preference pref = preference.getPreference(i);

      if (pref instanceof PreferenceGroup || pref instanceof PreferenceScreen)
      {
        initSummaries((PreferenceGroup) pref);
      }
      else
      {
        setPrefSummary(pref);
      }
    }
  }

}
