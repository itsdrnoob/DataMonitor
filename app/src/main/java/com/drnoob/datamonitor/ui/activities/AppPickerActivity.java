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
import static com.drnoob.datamonitor.core.Values.APP_COUNTRY_CODE;
import static com.drnoob.datamonitor.core.Values.APP_LANGUAGE_CODE;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.drnoob.datamonitor.R;
import com.drnoob.datamonitor.adapters.AppsListAdapter;
import com.drnoob.datamonitor.adapters.data.AppDataUsageModel;
import com.drnoob.datamonitor.adapters.data.AppModel;
import com.drnoob.datamonitor.core.task.DatabaseHandler;
import com.drnoob.datamonitor.databinding.ActivityAppPickerBinding;
import com.drnoob.datamonitor.utils.CrashReporter;
import com.drnoob.datamonitor.utils.SharedPreferences;
import com.google.android.material.elevation.SurfaceColors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class AppPickerActivity extends AppCompatActivity {
    private static final String TAG = AppPickerActivity.class.getSimpleName();

    ActivityAppPickerBinding binding;
    private List<AppModel> mAppsList;
    private List<AppModel> mAllAppsList;
    private LinearLayout mDataLoading;
    private AppsListAdapter mAdapter;
    private Boolean showSystem;

    private static Intent data;

    public static Intent getData() {
        return data;
    }

    public static void setData(Intent data) {
        AppPickerActivity.data = data;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        MainActivity.setTheme(AppPickerActivity.this);
        Thread.setDefaultUncaughtExceptionHandler(new CrashReporter(AppPickerActivity.this));
        String languageCode = SharedPreferences.getUserPrefs(this).getString(APP_LANGUAGE_CODE, "null");
        String countryCode = SharedPreferences.getUserPrefs(this).getString(APP_COUNTRY_CODE, "");
        if (languageCode.equals("null")) {
            setLanguage(this, "en", countryCode);
        }
        else {
            setLanguage(this, languageCode, countryCode);
        }
        super.onCreate(savedInstanceState);
        binding = ActivityAppPickerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setTitle(getString(R.string.title_app_picker));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        binding.toolbar.setBackgroundColor(SurfaceColors.SURFACE_2.getColor(this));
        binding.searchView.setBackgroundColor(SurfaceColors.SURFACE_2.getColor(this));
        getWindow().setStatusBarColor(SurfaceColors.SURFACE_2.getColor(this));

        /*
        In versions lower than O_MR1, windowLightNavigationBar cannot be applied, which results in the
        navigation bar icons being a light color (white). This limits visibility in light theme.
         */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
//            getWindow().setNavigationBarColor(SurfaceColors.SURFACE_2.getColor(this));
            getWindow().setNavigationBarColor(getResources().getColor(R.color.background, null));
        }


        mAppsList = new ArrayList<>();
        mAllAppsList = new ArrayList<>();
        mDataLoading = (LinearLayout) findViewById(R.id.layout_list_loading);
        showSystem = false;

        refreshAppsList();

        binding.searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
//                Toast.makeText(AppPickerActivity.this, "" + charSequence.toString()
//                        + "  " + i + "  " + i1 + "  " + i2, Toast.LENGTH_SHORT).show();
                if (binding.searchEditText.getText().toString().length() > 0) {
                    if (binding.clearSearch.getVisibility() != View.VISIBLE) {
                        showClearButton();
                    }
                }
                else {
                    if (binding.clearSearch.getVisibility() == View.VISIBLE) {
                        hideClearButton();
                    }
                }

                searchApp(charSequence.toString());
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        binding.clearSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                binding.searchEditText.setText("");
                hideClearButton();
            }
        });
    }

    public void returnResult() {
        setResult(RESULT_OK, getData());
        finish();
    }

    private void showClearButton() {
        binding.clearSearch.setAlpha(0f);
        binding.clearSearch.animate()
                .alpha(1f)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                        binding.clearSearch.setVisibility(View.VISIBLE);
                        super.onAnimationStart(animation);
                    }
                })
                .start();
    }

    private void hideClearButton() {
        binding.clearSearch.animate()
                .alpha(0f)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        binding.clearSearch.setVisibility(View.GONE);
                        super.onAnimationEnd(animation);
                    }
                })
                .start();
    }

    private void refreshAppsList() {
        DatabaseHandler handler = new DatabaseHandler(AppPickerActivity.this);
        try {
            if (handler.getUsageList() != null && !handler.getUsageList().isEmpty()) {
                for (AppDataUsageModel model : handler.getUsageList()) {
                    AppModel app = new AppModel(model.getAppName(), model.getPackageName(), model.isSystemApp());
                    if (!model.isSystemApp()) {
                        mAppsList.add(app);
                    }
                    mAllAppsList.add(app);
                }
                Collections.sort(mAppsList, new Comparator<AppModel>() {
                    @Override
                    public int compare(AppModel o1, AppModel o2) {
                        return o1.getAppName().compareTo(o2.getAppName());
                    }
                });

                Collections.sort(mAllAppsList, new Comparator<AppModel>() {
                    @Override
                    public int compare(AppModel o1, AppModel o2) {
                        return o1.getAppName().compareTo(o2.getAppName());
                    }
                });
                loadApps();
            }
            else {
                GetAppsList getAppsList = new GetAppsList();
                getAppsList.execute();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadApps() {
        mAdapter = new AppsListAdapter(AppPickerActivity.this, mAppsList);
        binding.appsView.setLayoutManager(new LinearLayoutManager(AppPickerActivity.this));
        binding.appsView.setAdapter(mAdapter);
        mAdapter.notifyDataSetChanged();
        binding.appsView.setHasFixedSize(false);
        if (mAppsList != null && !mAppsList.isEmpty()) {
            mDataLoading.animate()
                    .alpha(0f)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);
                            mDataLoading.setVisibility(View.GONE);
                            binding.appsView.animate()
                                    .alpha(1f)
                                    .start();
                        }
                    })
                    .start();
        }
    }

    private void showAllApps() {
        mAdapter = new AppsListAdapter(AppPickerActivity.this, mAllAppsList);
        binding.appsView.swapAdapter(mAdapter, true);
        mAdapter.notifyDataSetChanged();
    }

    private void showUserApps() {
        mAdapter = new AppsListAdapter(AppPickerActivity.this, mAppsList);
        binding.appsView.swapAdapter(mAdapter, true);
        mAdapter.notifyDataSetChanged();
    }

    private void searchApp(String searchText) {
        List<AppModel> searchList = new ArrayList<>();
        if (searchText.length() > 0) {
            for (AppModel app : mAllAppsList) {
                if (app.getAppName().toLowerCase(Locale.ROOT).contains(searchText.toLowerCase(Locale.ROOT))) {
                    searchList.add(app);
                }
            }
            mAdapter = new AppsListAdapter(AppPickerActivity.this, searchList);
        }
        else {
            if (showSystem) {
                mAdapter = new AppsListAdapter(AppPickerActivity.this, mAllAppsList);
            }
            else {
                mAdapter = new AppsListAdapter(AppPickerActivity.this, mAppsList);
            }
        }
        binding.appsView.swapAdapter(mAdapter, true);
        mAdapter.notifyDataSetChanged();
    }

    private class GetAppsList extends AsyncTask<Object, Integer, Object> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Object doInBackground(Object[] objects) {
            PackageManager manager = getPackageManager();
            List<ApplicationInfo> apps = manager.getInstalledApplications(PackageManager.GET_META_DATA);
            AppModel app = new AppModel();
            for (ApplicationInfo info : apps) {
                app.setAppName(manager.getApplicationLabel(info).toString());
                app.setPackageName(info.packageName);

                if ((info.flags & ApplicationInfo.FLAG_SYSTEM) == 1) {
                    // System Apps
                    app.setIsSystemApp(true);
                }
                else {
                    // User Apps
                    app.setIsSystemApp(false);
                }
                mAppsList.add(app);
            }
            Collections.sort(mAppsList, new Comparator<AppModel>() {
                @Override
                public int compare(AppModel o1, AppModel o2) {
                    return o1.getAppName().compareTo(o2.getAppName());
                }
            });
            return null;
        }

        @Override
        protected void onPostExecute(Object o) {
            super.onPostExecute(o);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.app_picker_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem item = menu.findItem(R.id.menu_show_system);
        if (showSystem) {
            // All apps are displayed. Hide system
            item.setTitle(getString(R.string.label_hide_system_apps));
        }
        else {
            // User apps are displayed. Show system
            item.setTitle(getString(R.string.label_show_system_apps));
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        }
        if (item.getItemId() == R.id.menu_show_system) {
            if (showSystem) {
                // All apps are displayed. Hide system
                showSystem = false;
                showUserApps();
            }
            else {
                // User apps are displayed. Show system
                showSystem = true;
                showAllApps();
            }
        }
        return super.onOptionsItemSelected(item);
    }
}