package org.adblockplus.android;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;

public class SummarizedPreferences extends PreferenceActivity implements OnSharedPreferenceChangeListener
{
	@Override
	public void onResume()
	{
		super.onResume();
		// initialize list summaries
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
