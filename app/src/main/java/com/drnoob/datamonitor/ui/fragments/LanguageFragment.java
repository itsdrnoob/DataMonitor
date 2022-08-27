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
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceFragmentCompat;

import com.drnoob.datamonitor.R;
import com.drnoob.datamonitor.adapters.data.LanguageModel;
import com.drnoob.datamonitor.core.base.Preference;
import com.drnoob.datamonitor.core.base.PreferenceCategory;
import com.drnoob.datamonitor.ui.activities.ContainerActivity;
import com.drnoob.datamonitor.ui.activities.MainActivity;
import com.drnoob.datamonitor.utils.SharedPreferences;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import static com.drnoob.datamonitor.Common.setLanguage;
import static com.drnoob.datamonitor.core.Values.APP_COUNTRY_CODE;
import static com.drnoob.datamonitor.core.Values.APP_LANGUAGE;
import static com.drnoob.datamonitor.core.Values.APP_LANGUAGE_CODE;
import static com.drnoob.datamonitor.core.Values.APP_LANGUAGE_FRAGMENT;
import static com.drnoob.datamonitor.core.Values.GENERAL_FRAGMENT_ID;
import static com.drnoob.datamonitor.ui.fragments.AppDataUsageFragmentTest.context;

public class LanguageFragment extends Fragment {
    private static final String TAG = LanguageFragment.class.getSimpleName();

    private TextView mContribute;

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
                startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse(getString(R.string.github_contribute_translations))));
            }
        });
    }

    public static class Language extends PreferenceFragmentCompat {
        private List<LanguageModel> availableLanguages = new ArrayList<>();
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

            availableLanguages.add(new LanguageModel("Romanian", "ro", ""));
            availableLanguages.add(new LanguageModel("English", "en", ""));
            availableLanguages.add(new LanguageModel("Simplified Chinese", "zh", "rCN"));
            availableLanguages.add(new LanguageModel("Traditional Chinese", "zh", "rTW"));
            availableLanguages.add(new LanguageModel("French", "fr", ""));
            availableLanguages.add(new LanguageModel("Arabic", "ar", ""));


            Collections.sort(availableLanguages, new Comparator<LanguageModel>() {
                @Override
                public int compare(LanguageModel languageModel, LanguageModel t1) {
                    return languageModel.getLanguage().compareTo(t1.getLanguage());
                }
            });

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
//                        LocaleListCompat appLocale = LocaleListCompat.forLanguageTags("ro");
//                        AppCompatDelegate.setApplicationLocales(appLocale);


//                        Locale locale = new Locale(languageCode);
//                        Locale.setDefault(locale);
//                        Resources resources = getActivity().getResources();
//                        Configuration config = resources.getConfiguration();
//                        config.setLocale(locale);
//                        resources.updateConfiguration(config, resources.getDisplayMetrics());

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
