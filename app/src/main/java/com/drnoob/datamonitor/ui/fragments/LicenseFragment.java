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

import static com.drnoob.datamonitor.core.Values.APP_LICENSE_FRAGMENT;
import static com.drnoob.datamonitor.core.Values.GENERAL_FRAGMENT_ID;
import static com.drnoob.datamonitor.core.Values.LICENSE_FRAGMENT;
import static com.drnoob.datamonitor.core.Values.OSS_LICENSE_FRAGMENT;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceFragmentCompat;

import com.drnoob.datamonitor.R;
import com.drnoob.datamonitor.core.base.Preference;
import com.drnoob.datamonitor.ui.activities.ContainerActivity;

import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class LicenseFragment extends Fragment {
    private static final String TAG = LicenseFragment.class.getSimpleName();
    private TextView license;

    public LicenseFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull @NotNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        int fragmentId = getActivity().getIntent().getIntExtra(GENERAL_FRAGMENT_ID, 0);
        View view = null;

        if (fragmentId == LICENSE_FRAGMENT) {
            view = inflater.inflate(R.layout.fragment_license, container, false);
        } else {
            view = inflater.inflate(R.layout.app_license, container, false);
//            ((ContainerActivity) getActivity()).getSupportActionBar().hide();
            license = view.findViewById(R.id.app_license_text);
            StringBuilder builder = new StringBuilder();
            BufferedReader reader;

            Log.d(TAG, "onCreateView: " + System.currentTimeMillis());
            try {
                reader = new BufferedReader(new InputStreamReader(getResources().openRawResource(R.raw.license)));
                String str;
                while ((str = reader.readLine()) != null) {
                    builder.append(str);
                    builder.append("<br>");
                }
                reader.close();
                license.setText(Html.fromHtml(Html.fromHtml(builder.toString()).toString()));
                Log.d(TAG, "onCreateView: " + System.currentTimeMillis());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return view;
    }

    public static class LicenseCategory extends PreferenceFragmentCompat {
        Preference mAppLicense, mOssLicense;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.license_preferences, rootKey);

            mAppLicense = (Preference) findPreference("app_license");
            mOssLicense = (Preference) findPreference("oss_license");

            mAppLicense.setOnPreferenceClickListener(new androidx.preference.Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(androidx.preference.Preference preference) {
                    startActivity(new Intent(getContext(), ContainerActivity.class).putExtra(GENERAL_FRAGMENT_ID, APP_LICENSE_FRAGMENT));
                    return false;
                }
            });

            mOssLicense.setOnPreferenceClickListener(new androidx.preference.Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(androidx.preference.Preference preference) {
                    startActivity(new Intent(getActivity(), ContainerActivity.class)
                            .putExtra(GENERAL_FRAGMENT_ID, OSS_LICENSE_FRAGMENT));
                    return false;
                }
            });
        }
    }
}
