/*
 * Copyright (C) 2021 Dr.NooB
 *
 * This file is a part of Data Monitor <https://github.com/itsdrnoob/DataMonitor>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.drnoob.datamonitor.ui.fragments;

import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.drnoob.datamonitor.R;
import com.drnoob.datamonitor.core.base.Preference;
import com.drnoob.datamonitor.core.base.SwitchPreferenceCompat;
import com.drnoob.datamonitor.ui.activities.ContainerActivity;
import com.google.android.material.snackbar.Snackbar;

import static com.drnoob.datamonitor.Common.dismissOnClick;
import static com.drnoob.datamonitor.core.Values.ABOUT_FRAGMENT;
import static com.drnoob.datamonitor.core.Values.APP_LANGUAGE_FRAGMENT;
import static com.drnoob.datamonitor.core.Values.CONTRIBUTORS_FRAGMENT;
import static com.drnoob.datamonitor.core.Values.DARK_MODE_TOGGLE;
import static com.drnoob.datamonitor.core.Values.DONATE_FRAGMENT;
import static com.drnoob.datamonitor.core.Values.GENERAL_FRAGMENT_ID;
import static com.drnoob.datamonitor.core.Values.LICENSE_FRAGMENT;

public class SettingsFragment extends PreferenceFragmentCompat {
    private static final String TAG = SettingsFragment.class.getSimpleName();
    private SwitchPreferenceCompat mDarkModeToggle;
    private Preference mLanguagePicker, mAbout, mLicense, mContributors, mDonate;
    private Snackbar snackbar;

    public SettingsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.app_settings, rootKey);

        mDarkModeToggle = (SwitchPreferenceCompat) findPreference("dark_mode_toggle");
        mLanguagePicker = (Preference) findPreference("language_picker");
        mAbout = (Preference) findPreference("about");
        mLicense = (Preference) findPreference("license");
        mContributors = (Preference) findPreference("contributors");
        mDonate = (Preference) findPreference("donate");

        int nightModeFlags = getContext().getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        switch (nightModeFlags) {
            case Configuration.UI_MODE_NIGHT_YES:
                mDarkModeToggle.setChecked(true);
                break;

            case Configuration.UI_MODE_NIGHT_NO:
                mDarkModeToggle.setChecked(false);
                break;

            case Configuration.UI_MODE_NIGHT_UNDEFINED:
                break;
        }

        mDarkModeToggle.setOnPreferenceClickListener(new androidx.preference.Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(androidx.preference.Preference preference) {
                Boolean isChecked = PreferenceManager.getDefaultSharedPreferences(getContext()).getBoolean(DARK_MODE_TOGGLE, false);
                if (isChecked) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                } else {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                }
                return false;
            }
        });

        mLanguagePicker.setOnPreferenceClickListener(new androidx.preference.Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(androidx.preference.Preference preference) {
                startActivity(new Intent(getContext(), ContainerActivity.class)
                        .putExtra(GENERAL_FRAGMENT_ID, APP_LANGUAGE_FRAGMENT));
                return false;
            }
        });

        mAbout.setOnPreferenceClickListener(new androidx.preference.Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(androidx.preference.Preference preference) {
                startActivity(new Intent(getContext(), ContainerActivity.class)
                        .putExtra(GENERAL_FRAGMENT_ID, ABOUT_FRAGMENT));
                return false;
            }
        });

        mLicense.setOnPreferenceClickListener(new androidx.preference.Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(androidx.preference.Preference preference) {
                startActivity(new Intent(getContext(), ContainerActivity.class)
                        .putExtra(GENERAL_FRAGMENT_ID, LICENSE_FRAGMENT));
                return false;
            }
        });

        mContributors.setOnPreferenceClickListener(new androidx.preference.Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(androidx.preference.Preference preference) {
                startActivity(new Intent(getContext(), ContainerActivity.class)
                        .putExtra(GENERAL_FRAGMENT_ID, CONTRIBUTORS_FRAGMENT));
                return false;
            }
        });

        mDonate.setOnPreferenceClickListener(new androidx.preference.Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(androidx.preference.Preference preference) {
                startActivity(new Intent(getContext(), ContainerActivity.class)
                        .putExtra(GENERAL_FRAGMENT_ID, DONATE_FRAGMENT));
                return false;
            }
        });


    }


    @Override
    public void onPause() {
        super.onPause();
        if (snackbar != null) {
            snackbar.dismiss();
        }
    }
}