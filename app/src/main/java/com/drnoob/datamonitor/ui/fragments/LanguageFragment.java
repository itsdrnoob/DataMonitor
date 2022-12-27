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

import static com.drnoob.datamonitor.Common.refreshAvailableLanguages;
import static com.drnoob.datamonitor.Common.setLanguage;
import static com.drnoob.datamonitor.core.Values.APP_COUNTRY_CODE;
import static com.drnoob.datamonitor.core.Values.APP_LANGUAGE;
import static com.drnoob.datamonitor.core.Values.APP_LANGUAGE_CODE;
import static com.drnoob.datamonitor.core.Values.APP_LANGUAGE_FRAGMENT;
import static com.drnoob.datamonitor.core.Values.GENERAL_FRAGMENT_ID;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceFragmentCompat;

import com.drnoob.datamonitor.R;
import com.drnoob.datamonitor.adapters.data.LanguageModel;
import com.drnoob.datamonitor.core.base.Preference;
import com.drnoob.datamonitor.core.base.PreferenceCategory;
import com.drnoob.datamonitor.ui.activities.MainActivity;
import com.drnoob.datamonitor.utils.SharedPreferences;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class LanguageFragment extends Fragment {
    private static final String TAG = LanguageFragment.class.getSimpleName();

    private LinearLayout mContribute;

    @Nullable
    @org.jetbrains.annotations.Nullable
    @Override
    public View onCreateView(@NonNull @NotNull LayoutInflater inflater, @Nullable @org.jetbrains.annotations.Nullable ViewGroup container, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_language, container, false);
    }

    @Override
    public void onViewCreated(@NonNull @NotNull View view, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mContribute = view.findViewById(R.id.contribute_language);

        mContribute.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Intent.ACTION_VIEW)
                        .setData(Uri.parse(getString(R.string.github_contribute_translations))));
            }
        });
    }

    public static class Language extends PreferenceFragmentCompat {
        private List<LanguageModel> availableLanguages = refreshAvailableLanguages();
        private String currentLanguage;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.app_language, rootKey);

            Preference currentLanguagePref = (Preference) findPreference("current_language");
            Preference placeHolder = (Preference) findPreference("available_languages_placeholder");
            PreferenceCategory availableLanguagesCategory = (PreferenceCategory) placeHolder.getParent();
            availableLanguagesCategory.removeAll();

            String currentLanguage = SharedPreferences.getUserPrefs(getContext())
                    .getString(APP_LANGUAGE, "English");
            SpannableString spannableString = new SpannableString(currentLanguage);
            spannableString.setSpan(new ForegroundColorSpan(
                            getContext().getResources().getColor(R.color.primary, null)), 0,
                    spannableString.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            currentLanguagePref.setTitle(spannableString);

            for (int i = 0; i < availableLanguages.size(); i++) {
                String language = availableLanguages.get(i).getLanguage();
                String languageCode = availableLanguages.get(i).getLanguageCode();
                String countryCode = availableLanguages.get(i).getCountryCode();
                Preference preference = new Preference(getContext());
                preference.setTitle(language);
                preference.setIconSpaceReserved(false);

                preference.setOnPreferenceClickListener(new androidx.preference.Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(androidx.preference.Preference preference) {
                        if (!language.equals(currentLanguage)) {
                            // change app language
                            SharedPreferences.getUserPrefs(getContext()).edit()
                                    .putString(APP_LANGUAGE, language)
                                    .putString(APP_LANGUAGE_CODE, languageCode)
                                    .putString(APP_COUNTRY_CODE, countryCode)
                                    .apply();

                            setLanguage(getActivity(), languageCode, countryCode);
                            startActivity(new Intent(getActivity(), MainActivity.class)
                                    .putExtra(GENERAL_FRAGMENT_ID, APP_LANGUAGE_FRAGMENT)
                                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP));

//                        getActivity().recreate();

                        }
                        return false;
                    }
                });

                availableLanguagesCategory.addPreference(preference);
            }

        }

        @Override
        public void onViewCreated(@NonNull @NotNull View view, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            setDivider(new ColorDrawable(Color.TRANSPARENT));
            setDividerHeight(0);
        }
    }
}
