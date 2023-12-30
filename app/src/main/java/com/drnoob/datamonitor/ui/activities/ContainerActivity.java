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

package com.drnoob.datamonitor.ui.activities;

import static com.drnoob.datamonitor.Common.setLanguage;
import static com.drnoob.datamonitor.core.Values.ABOUT_FRAGMENT;
import static com.drnoob.datamonitor.core.Values.ADD_CUSTOM_SESSION_FRAGMENT;
import static com.drnoob.datamonitor.core.Values.APP_CONTRIBUTORS_FRAGMENT;
import static com.drnoob.datamonitor.core.Values.APP_COUNTRY_CODE;
import static com.drnoob.datamonitor.core.Values.APP_DATA_LIMIT_FRAGMENT;
import static com.drnoob.datamonitor.core.Values.APP_LANGUAGE_CODE;
import static com.drnoob.datamonitor.core.Values.APP_LANGUAGE_FRAGMENT;
import static com.drnoob.datamonitor.core.Values.APP_LICENSE_FRAGMENT;
import static com.drnoob.datamonitor.core.Values.BOTTOM_NAVBAR_ITEM_SETTINGS;
import static com.drnoob.datamonitor.core.Values.CONTRIBUTORS_FRAGMENT;
import static com.drnoob.datamonitor.core.Values.DATA_PLAN_FRAGMENT;
import static com.drnoob.datamonitor.core.Values.DATA_USAGE_SYSTEM;
import static com.drnoob.datamonitor.core.Values.DATA_USAGE_TODAY;
import static com.drnoob.datamonitor.core.Values.DATA_USAGE_WEEKDAY;
import static com.drnoob.datamonitor.core.Values.DIAGNOSTICS_HISTORY_FRAGMENT;
import static com.drnoob.datamonitor.core.Values.DIAGNOSTICS_SETTINGS_FRAGMENT;
import static com.drnoob.datamonitor.core.Values.DISABLE_BATTERY_OPTIMISATION_FRAGMENT;
import static com.drnoob.datamonitor.core.Values.DONATE_FRAGMENT;
import static com.drnoob.datamonitor.core.Values.EXCLUDE_APPS_FRAGMENT;
import static com.drnoob.datamonitor.core.Values.GENERAL_FRAGMENT_ID;
import static com.drnoob.datamonitor.core.Values.LICENSE_FRAGMENT;
import static com.drnoob.datamonitor.core.Values.NETWORK_STATS_FRAGMENT;
import static com.drnoob.datamonitor.core.Values.OSS_LICENSE_FRAGMENT;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat;

import com.drnoob.datamonitor.R;
import com.drnoob.datamonitor.adapters.data.AppModel;
import com.drnoob.datamonitor.adapters.data.DiagnosticsHistoryModel;
import com.drnoob.datamonitor.adapters.data.LiveData;
import com.drnoob.datamonitor.databinding.ActivityContainerBinding;
import com.drnoob.datamonitor.ui.fragments.AboutFragment;
import com.drnoob.datamonitor.ui.fragments.AppContributorsFragment;
import com.drnoob.datamonitor.ui.fragments.AppDataLimitFragment;
import com.drnoob.datamonitor.ui.fragments.AppDataUsageFragment;
import com.drnoob.datamonitor.ui.fragments.ContributorsFragment;
import com.drnoob.datamonitor.ui.fragments.CustomSessionFragment;
import com.drnoob.datamonitor.ui.fragments.DataPlanFragment;
import com.drnoob.datamonitor.ui.fragments.DiagnosticsHistoryFragment;
import com.drnoob.datamonitor.ui.fragments.DiagnosticsSettingsFragment;
import com.drnoob.datamonitor.ui.fragments.DonateFragment;
import com.drnoob.datamonitor.ui.fragments.ExcludeAppsFragment;
import com.drnoob.datamonitor.ui.fragments.LanguageFragment;
import com.drnoob.datamonitor.ui.fragments.LicenseFragment;
import com.drnoob.datamonitor.ui.fragments.NetworkStatsFragment;
import com.drnoob.datamonitor.ui.fragments.OSSLicenseFragment;
import com.drnoob.datamonitor.ui.fragments.SettingsFragment;
import com.drnoob.datamonitor.ui.fragments.SystemDataUsageFragment;
import com.drnoob.datamonitor.utils.CrashReporter;
import com.drnoob.datamonitor.utils.SharedPreferences;
import com.google.android.material.elevation.SurfaceColors;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ContainerActivity extends AppCompatActivity {

    ActivityContainerBinding binding;
    private int fragmentID;
    private LiveData mLiveData;
    private boolean isAppSelectionView = false;
    private boolean isResultsSelectionView = false;
    private boolean isListSelected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        MainActivity.setTheme(ContainerActivity.this);
        Thread.setDefaultUncaughtExceptionHandler(new CrashReporter(ContainerActivity.this));
        String languageCode = SharedPreferences.getUserPrefs(this).getString(APP_LANGUAGE_CODE, "null");
        String countryCode = SharedPreferences.getUserPrefs(this).getString(APP_COUNTRY_CODE, "");
        if (languageCode.equals("null")) {
            setLanguage(this, "en", countryCode);
        }
        else {
            setLanguage(this, languageCode, countryCode);
        }
        super.onCreate(savedInstanceState);
        fragmentID = getIntent().getIntExtra(GENERAL_FRAGMENT_ID, 0);
        binding = ActivityContainerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.containerToolbar);
        getSupportActionBar().setTitle(null);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
//        getSupportActionBar().setHomeAsUpIndicator(getDrawable(R.drawable.ic_arrow));

//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
//            binding.containerToolbar.setBackgroundColor(SurfaceColors.SURFACE_2.getColor(this));
//        }
//        else {
//            binding.containerToolbar.setBackgroundColor(getResources().getColor(R.color.surface, null));
//        }
        binding.containerToolbar.setBackgroundColor(SurfaceColors.SURFACE_2.getColor(this));
        getWindow().setStatusBarColor(SurfaceColors.SURFACE_2.getColor(this));

        /*
        In versions lower than O_MR1, windowLightNavigationBar cannot be applied, which results in the
        navigation bar icons being a light color (white). This limits visibility in light theme.
         */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
//            getWindow().setNavigationBarColor(SurfaceColors.SURFACE_2.getColor(this));
            getWindow().setNavigationBarColor(getResources().getColor(R.color.background, null));
        }

        mLiveData = new ViewModelProvider(this).get(LiveData.class);

        Fragment fragment = null;
        String title = null;
        switch (fragmentID) {
            case ABOUT_FRAGMENT:
                fragment = new AboutFragment();
                title = getString(R.string.about);
                break;

            case LICENSE_FRAGMENT:
                fragment = new LicenseFragment();
                title = getString(R.string.license);
                break;

            case CONTRIBUTORS_FRAGMENT:
                fragment = new ContributorsFragment();
                title = getString(R.string.contributors);
                break;

            case DONATE_FRAGMENT:
                fragment = new DonateFragment();
                title = getString(R.string.donate);
                break;


            case APP_LICENSE_FRAGMENT:
                fragment = new LicenseFragment();
                title = getString(R.string.app_license_header);
                break;

            case OSS_LICENSE_FRAGMENT:
                fragment = new OSSLicenseFragment();
                title = getString(R.string.oss_licenses);
                break;

            case APP_DATA_LIMIT_FRAGMENT:
                fragment = new AppDataLimitFragment();
                title = getString(R.string.title_app_data_limit);
                break;

            case NETWORK_STATS_FRAGMENT:
                fragment = new NetworkStatsFragment();
                title = getString(R.string.network_stats);
                break;

            case APP_LANGUAGE_FRAGMENT:
                fragment = new LanguageFragment();
                title = getString(R.string.settings_language);
                break;

            case BOTTOM_NAVBAR_ITEM_SETTINGS:
                fragment = new SettingsFragment();
                title = getString(R.string.settings);
                break;

            case DATA_USAGE_SYSTEM:
                fragment = new SystemDataUsageFragment();
                title = getString(R.string.system_data_usage);
                break;

            case DATA_USAGE_TODAY:
                fragment = new AppDataUsageFragment();
                title = getString(R.string.heading_data_usage_today);
                break;

            case DISABLE_BATTERY_OPTIMISATION_FRAGMENT:
                fragment = new SetupActivity.DisableBatteryOptimisationFragment();
                title = getString(R.string.label_battery_optimisation);
                break;

            case DIAGNOSTICS_SETTINGS_FRAGMENT:
                fragment = new DiagnosticsSettingsFragment();
                title = getString(R.string.settings_network_diagnostics);
                break;

            case EXCLUDE_APPS_FRAGMENT:
                fragment = new ExcludeAppsFragment();
                title = getString(R.string.exclude_apps);
                break;

            case DIAGNOSTICS_HISTORY_FRAGMENT:
                fragment = new DiagnosticsHistoryFragment();
                title = getString(R.string.diagnostics_history);
                break;

            case DATA_PLAN_FRAGMENT:
                fragment = new DataPlanFragment();
                title = getString(R.string.title_add_data_plan);
//                binding.toolbarSave.setVisibility(View.VISIBLE);
                getSupportActionBar().hide();
                break;

            case DATA_USAGE_WEEKDAY:
                fragment = new AppDataUsageFragment();
                title = getString(R.string.app_data_usage);
                break;

            case APP_CONTRIBUTORS_FRAGMENT:
                fragment = new AppContributorsFragment();
                title = getString(R.string.app_contributors);
                break;

            case ADD_CUSTOM_SESSION_FRAGMENT:
                fragment = new CustomSessionFragment();
                title = getString(R.string.add_custom_session);
                getSupportActionBar().hide();
                break;
        }

        getSupportFragmentManager().beginTransaction().replace(R.id.container_host_fragment, fragment).commit();
        getSupportActionBar().setTitle(title);

        mLiveData.getIsAppSelectionView().observe(this, new Observer<Boolean>() {
            @SuppressLint("RestrictedApi")
            @Override
            public void onChanged(Boolean aBoolean) {
                invalidateOptionsMenu();
                if (aBoolean) {
                    AnimatedVectorDrawableCompat arrowToCross = AnimatedVectorDrawableCompat
                            .create(ContainerActivity.this, R.drawable.anim_back_arrow_to_cross);
                    arrowToCross.setTint(getResources().getColor(R.color.arrow_back, null));
                    getSupportActionBar().setHomeAsUpIndicator(arrowToCross);
                    arrowToCross.start();
                }
                else {
                    AnimatedVectorDrawableCompat crossToArrow = AnimatedVectorDrawableCompat
                            .create(ContainerActivity.this, R.drawable.anim_cross_to_back_arrow);
                    crossToArrow.setTint(getResources().getColor(R.color.arrow_back, null));
                    getSupportActionBar().setHomeAsUpIndicator(crossToArrow);
                    crossToArrow.start();
                }
                isAppSelectionView = aBoolean;

                mLiveData.getSelectedAppsList().observe(ContainerActivity.this, new Observer<List<AppModel>>() {
                    @Override
                    public void onChanged(List<AppModel> appModels) {
                        if (appModels != null) {
                            if (appModels.isEmpty()) {
                                getSupportActionBar().setTitle(getString(R.string.exclude_apps));
                            }
                            else {
                                getSupportActionBar().setTitle(String.valueOf(appModels.size()));
                            }
                        }
                    }
                });
            }
        });

        mLiveData.getIsResultSelectionView().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean aBoolean) {
                invalidateOptionsMenu();
                if (aBoolean) {
                    AnimatedVectorDrawableCompat arrowToCross = AnimatedVectorDrawableCompat
                            .create(ContainerActivity.this, R.drawable.anim_back_arrow_to_cross);
                    arrowToCross.setTint(getResources().getColor(R.color.text_primary, null));
                    getSupportActionBar().setHomeAsUpIndicator(arrowToCross);
                    arrowToCross.start();
                }
                else {
                    AnimatedVectorDrawableCompat crossToArrow = AnimatedVectorDrawableCompat
                            .create(ContainerActivity.this, R.drawable.anim_cross_to_back_arrow);
                    crossToArrow.setTint(getResources().getColor(R.color.text_primary, null));
                    getSupportActionBar().setHomeAsUpIndicator(crossToArrow);
                    crossToArrow.start();
                }
                isResultsSelectionView = aBoolean;

                mLiveData.getSelectedResults().observe(ContainerActivity.this, new Observer<List<DiagnosticsHistoryModel>>() {
                    @Override
                    public void onChanged(List<DiagnosticsHistoryModel> diagnosticsHistoryModels) {
                        if (diagnosticsHistoryModels != null) {
                            if (diagnosticsHistoryModels.isEmpty()) {
                                getSupportActionBar().setTitle(getString(R.string.diagnostics_history));
                            }
                            else {
                                getSupportActionBar().setTitle(String.valueOf(diagnosticsHistoryModels.size()));
                            }
                        }
                    }
                });
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (fragmentID == EXCLUDE_APPS_FRAGMENT || fragmentID == DIAGNOSTICS_HISTORY_FRAGMENT) {
            getMenuInflater().inflate(R.menu.selection_options_menu, menu);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (fragmentID == EXCLUDE_APPS_FRAGMENT) {
            menu.findItem(R.id.menu_delete).setVisible(isAppSelectionView);
            menu.findItem(R.id.menu_select_all).setVisible(false);
        }
        if (fragmentID == DIAGNOSTICS_HISTORY_FRAGMENT) {
            menu.findItem(R.id.menu_delete).setVisible(isResultsSelectionView);
            menu.findItem(R.id.menu_select_all).setVisible(isResultsSelectionView);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull @NotNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (isAppSelectionView) {
            List<AppModel> appModelList = new ArrayList<>();
            mLiveData.setIsAppSelectionView(false);
            mLiveData.setSelectedAppsList(appModelList);
        }
        else if (isResultsSelectionView) {
            List<DiagnosticsHistoryModel> modelList = new ArrayList<>();
            mLiveData.setIsResultSelectionView(false);
            mLiveData.setSelectedResults(modelList);
        }
        else {
            super.onBackPressed();
        }
    }
}