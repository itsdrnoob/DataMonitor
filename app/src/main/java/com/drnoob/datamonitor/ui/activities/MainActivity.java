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

import android.Manifest;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import androidx.preference.PreferenceManager;

import com.drnoob.datamonitor.R;
import com.drnoob.datamonitor.Widget.DataUsageWidget;
import com.drnoob.datamonitor.adapters.data.AppDataUsageModel;
import com.drnoob.datamonitor.core.base.Preference;
import com.drnoob.datamonitor.core.task.DatabaseHandler;
import com.drnoob.datamonitor.databinding.ActivityMainBinding;
import com.drnoob.datamonitor.ui.fragments.SettingsFragment;
import com.drnoob.datamonitor.utils.SharedPreferences;
import com.google.android.material.navigation.NavigationBarView;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.zip.Inflater;

import static com.drnoob.datamonitor.Common.isAppInstalled;
import static com.drnoob.datamonitor.Common.isReadPhoneStateGranted;
import static com.drnoob.datamonitor.Common.isUsageAccessGranted;
import static com.drnoob.datamonitor.Common.refreshService;
import static com.drnoob.datamonitor.Common.setLanguage;
import static com.drnoob.datamonitor.core.Values.APP_COUNTRY_CODE;
import static com.drnoob.datamonitor.core.Values.APP_DATA_USAGE_WARNING_CHANNEL_ID;
import static com.drnoob.datamonitor.core.Values.APP_DATA_USAGE_WARNING_CHANNEL_NAME;
import static com.drnoob.datamonitor.core.Values.APP_LANGUAGE_CODE;
import static com.drnoob.datamonitor.core.Values.BOTTOM_NAVBAR_ITEM_SETTINGS;
import static com.drnoob.datamonitor.core.Values.DATA_RESET_DATE;
import static com.drnoob.datamonitor.core.Values.DATA_USAGE_NOTIFICATION_CHANNEL_ID;
import static com.drnoob.datamonitor.core.Values.DATA_USAGE_NOTIFICATION_CHANNEL_NAME;
import static com.drnoob.datamonitor.core.Values.DATA_USAGE_SYSTEM;
import static com.drnoob.datamonitor.core.Values.DATA_USAGE_VALUE;
import static com.drnoob.datamonitor.core.Values.DATA_USAGE_WARNING_CHANNEL_ID;
import static com.drnoob.datamonitor.core.Values.DATA_USAGE_WARNING_CHANNEL_NAME;
import static com.drnoob.datamonitor.core.Values.GENERAL_FRAGMENT_ID;
import static com.drnoob.datamonitor.core.Values.NETWORK_SIGNAL_CHANNEL_ID;
import static com.drnoob.datamonitor.core.Values.NETWORK_SIGNAL_CHANNEL_NAME;
import static com.drnoob.datamonitor.core.Values.READ_PHONE_STATE_DISABLED;
import static com.drnoob.datamonitor.core.Values.SESSION_TODAY;
import static com.drnoob.datamonitor.core.Values.SETUP_COMPLETED;
import static com.drnoob.datamonitor.core.Values.SETUP_VALUE;
import static com.drnoob.datamonitor.core.Values.TYPE_MOBILE_DATA;
import static com.drnoob.datamonitor.core.Values.USAGE_ACCESS_DISABLED;
import static com.drnoob.datamonitor.ui.fragments.AppDataUsageFragment.getAppContext;
import static com.drnoob.datamonitor.ui.fragments.AppDataUsageFragment.onDataLoaded;
import static com.drnoob.datamonitor.utils.NetworkStatsHelper.getAppMobileDataUsage;
import static com.drnoob.datamonitor.utils.NetworkStatsHelper.getAppWifiDataUsage;
import static com.drnoob.datamonitor.utils.NetworkStatsHelper.getDeletedAppsMobileDataUsage;
import static com.drnoob.datamonitor.utils.NetworkStatsHelper.getDeletedAppsWifiDataUsage;
import static com.drnoob.datamonitor.utils.NetworkStatsHelper.getDeviceMobileDataUsage;
import static com.drnoob.datamonitor.utils.NetworkStatsHelper.getDeviceWifiDataUsage;
import static com.drnoob.datamonitor.utils.NetworkStatsHelper.getTetheringDataUsage;

public class MainActivity extends AppCompatActivity {

    ActivityMainBinding binding;

    private static final String TAG = MainActivity.class.getSimpleName();

    public static List<AppDataUsageModel> mAppsList = new ArrayList<>();
    public static List<AppDataUsageModel> mUserAppsList = new ArrayList<>();
    public static List<AppDataUsageModel> mSystemAppsList = new ArrayList<>();
    public static int value;
    public static String themeSwitch;
    private static Boolean isDataLoading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(MainActivity.this);
//        setTheme(R.style.Theme_DataMonitor_MaterialYou);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (!isReadPhoneStateGranted(MainActivity.this)) {
                startActivity(new Intent(this, SetupActivity.class)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        .putExtra(SETUP_VALUE, READ_PHONE_STATE_DISABLED));
                finish();
            }
        }

        super.onCreate(savedInstanceState);
        String languageCode = SharedPreferences.getUserPrefs(this).getString(APP_LANGUAGE_CODE, "null");
        String countryCode = SharedPreferences.getUserPrefs(this).getString(APP_COUNTRY_CODE, "");
        if (languageCode.equals("null")) {
            setLanguage(this, "en", countryCode);
        }
        else {
            setLanguage(this, languageCode, countryCode);
        }

        refreshService(this);

        try {
            if (isUsageAccessGranted(MainActivity.this)) {

                binding = ActivityMainBinding.inflate(getLayoutInflater());
                setTheme(R.style.Theme_DataMonitor);
                setContentView(binding.getRoot());
                setSupportActionBar(binding.mainToolbar);

                SharedPreferences.getUserPrefs(this).edit().putBoolean(SETUP_COMPLETED, true).apply();

                if (binding.bottomNavigationView.getSelectedItemId() == R.id.bottom_menu_home) {
                    getSupportActionBar().setTitle(getString(R.string.app_name));
                }

//                AppBarConfiguration configuration = new AppBarConfiguration.Builder(R.id.bottom_menu_home,
//                        R.id.bottom_menu_setup, R.id.bottom_menu_app_data_usage, R.id.bottom_menu_settings, R.id.system_data_usage).build();




//                NavController controller = Navigation.findNavController(this, R.id.main_nav_host_fragment);





                NavHostFragment navHostFragment = (NavHostFragment)  getSupportFragmentManager().findFragmentById(R.id.main_nav_host_fragment);
                NavController controller = navHostFragment.getNavController();
                controller.addOnDestinationChangedListener(new NavController.OnDestinationChangedListener() {
                    @Override
                    public void onDestinationChanged(@NotNull NavController navController, @NotNull NavDestination navDestination, @Nullable Bundle bundle) {
                        changeBanner(navDestination);
                    }
                });   // working

                NavigationUI.setupWithNavController(binding.bottomNavigationView, controller);




//        NavigationUI.setupActionBarWithNavController(this, controller, configuration);
//        NavigationUI.setupWithNavController(binding.bottomNavigationView, controller);

                DatabaseHandler databaseHandler = new DatabaseHandler(MainActivity.this);
                if (databaseHandler.getUsageList() != null && databaseHandler.getUsageList().size() > 0) {

                } else {
                    FetchApps fetchApps = new FetchApps(this);
                    fetchApps.execute();

                }


                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    createNotificationChannel();
                }

                value = getIntent().getIntExtra(DATA_USAGE_VALUE, 0);

                if (value == DATA_USAGE_SYSTEM) {
                    binding.bottomNavigationView.setVisibility(View.GONE);
                    binding.bottomNavigationView.setSelectedItemId(R.id.bottom_menu_app_data_usage);
                    getSupportActionBar().setTitle(R.string.system_data_usage);
                    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                    getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_arrow);
                    controller.navigate(R.id.system_data_usage);  // >> working
                }
                if (value != DATA_USAGE_SYSTEM) {
                    if (!isDataLoading()) {
                        LoadData loadData = new LoadData(MainActivity.this, SESSION_TODAY, TYPE_MOBILE_DATA);
                        loadData.execute();
                    }
                }

//                binding.bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
//                    @Override
//                    public boolean onNavigationItemSelected(@NonNull @NotNull MenuItem item) {
//                        if (item.getItemId() == binding.bottomNavigationView.getSelectedItemId()) {
//                            return false;
//                        }
//                        if (item.getItemId() == R.id.bottom_menu_home) {
//                            getSupportActionBar().setTitle(getString(R.string.app_name));
//                        } else {
//                            getSupportActionBar().setTitle(item.getTitle());
//                        }
//                        NavigationUI.onNavDestinationSelected(item, controller);
//                        return true;
//                    }
//                });

            }
            else {
                onResume();
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

    }

    private void initializeBottomNavBar() {
        NavHostFragment navHostFragment = (NavHostFragment)  getSupportFragmentManager().findFragmentById(R.id.main_nav_host_fragment);
        NavController controller = navHostFragment.getNavController();
        controller.addOnDestinationChangedListener(new NavController.OnDestinationChangedListener() {
            @Override
            public void onDestinationChanged(@NotNull NavController navController, @NotNull NavDestination navDestination, @Nullable Bundle bundle) {
                changeBanner(navDestination);
            }
        });   // working

        NavigationUI.setupWithNavController(binding.bottomNavigationView, controller);  // working
    }


    private void changeBanner(NavDestination navDestination) {
        String destination = navDestination.getLabel().toString();
        Spannable banner;

        if (destination.equalsIgnoreCase(getString(R.string.home))) {
            // Home Fragment
            getSupportActionBar().setTitle(getString(R.string.app_name));
        }
        else if (destination.equalsIgnoreCase(getString(R.string.setup))) {
            // Setup Fragment
            getSupportActionBar().setTitle(getString(R.string.setup));
        }
        else if (destination.equalsIgnoreCase(getString(R.string.app_data_usage))) {
            // App data usage Fragment
            getSupportActionBar().setTitle(getString(R.string.app_data_usage));
        }
        else if (destination.equalsIgnoreCase(getString(R.string.network_diagnostics))) {
            // Network diagnostics Fragment
            getSupportActionBar().setTitle(getString(R.string.network_diagnostics));
        }
        else {
            // Unknown Fragment
        }

    }



    @Override
    protected void onStart() {
        super.onStart();
//        initializeBottomNavBar();
    }

    @Override
    public void onBackPressed() {
        if (value == DATA_USAGE_SYSTEM) {
            value = 0;
            finish();
        } else {
            super.onBackPressed();
            binding.bottomNavigationView.setSelectedItemId(R.id.bottom_menu_home);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            if (!isUsageAccessGranted(MainActivity.this)) {
                startActivity(new Intent(this, SetupActivity.class)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        .putExtra(SETUP_VALUE, USAGE_ACCESS_DISABLED));
            }
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                if (!isReadPhoneStateGranted(MainActivity.this)) {
                    startActivity(new Intent(this, SetupActivity.class)
                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK)
                            .putExtra(SETUP_VALUE, READ_PHONE_STATE_DISABLED));
                }
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        // Action bar title resets while changing theme in settings, setting current title
//        NavController controller = Navigation.findNavController(this, R.id.main_nav_host_fragment);
//        if (controller.getCurrentDestination().getId() == R.id.bottom_menu_settings) {
//            getSupportActionBar().setTitle(getString(R.string.settings));
//        }

    }

    @Override
    protected void onDestroy() {
        Intent intent = new Intent(MainActivity.this, DataUsageWidget.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        int[] ids = AppWidgetManager.getInstance(MainActivity.this)
                .getAppWidgetIds(new ComponentName(MainActivity.this, DataUsageWidget.class));
        AppWidgetManager.getInstance(this).updateAppWidget(ids, new RemoteViews(getPackageName(), R.layout.data_usage_widget));
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull @NotNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if (value == DATA_USAGE_SYSTEM) {
                    value = 0;
                    finish();
                }
                break;

            case R.id.toolbar_settings:
                startActivity(new Intent(MainActivity.this, ContainerActivity.class)
                        .putExtra(GENERAL_FRAGMENT_ID, BOTTOM_NAVBAR_ITEM_SETTINGS));
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void createNotificationChannel() {
        NotificationChannel usageChannel = new NotificationChannel(DATA_USAGE_NOTIFICATION_CHANNEL_ID,
                DATA_USAGE_NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW);
        NotificationChannel warningChannel = new NotificationChannel(DATA_USAGE_WARNING_CHANNEL_ID, DATA_USAGE_WARNING_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH);
        NotificationChannel appWarningChannel = new NotificationChannel(APP_DATA_USAGE_WARNING_CHANNEL_ID, APP_DATA_USAGE_WARNING_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH);
        NotificationChannel networkSignalChannel = new NotificationChannel(NETWORK_SIGNAL_CHANNEL_ID, NETWORK_SIGNAL_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW);
        warningChannel.enableVibration(true);
        warningChannel.enableLights(true);
        appWarningChannel.enableVibration(true);
        appWarningChannel.enableLights(true);
        List<NotificationChannel> channels = new ArrayList<>();
        channels.add(usageChannel);
        channels.add(warningChannel);
        channels.add(appWarningChannel);
        channels.add(networkSignalChannel);

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.createNotificationChannels(channels);
    }

    protected void disableSelectedItem(int selectedItemIndex) {
        for (int i = 0; i <= 3; i++) {
            if (i == selectedItemIndex) {
                binding.bottomNavigationView.getMenu().getItem(selectedItemIndex).setEnabled(false);
            } else {
                binding.bottomNavigationView.getMenu().getItem(i).setEnabled(true);
            }
        }
    }

    private static class FetchApps extends AsyncTask {
        private final Context mContext;

        public FetchApps(Context mContext) {
            this.mContext = mContext;
        }

        @Override
        protected Object doInBackground(Object[] objects) {
            Log.d(TAG, "doInBackground: checking applications");
            PackageManager packageManager = mContext.getPackageManager();
            List<ApplicationInfo> allApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA);
            List<AppDataUsageModel> modelList = new ArrayList<>();
            AppDataUsageModel model = null;
            DatabaseHandler databaseHandler = new DatabaseHandler(mContext);

            for (ApplicationInfo applicationInfo : allApps) {
                if ((applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 1) {
                    // System app
                    modelList.add(new AppDataUsageModel(packageManager.getApplicationLabel(applicationInfo).toString(),
                            applicationInfo.packageName,
                            applicationInfo.uid,
                            true));
                } else {
                    // User app
                    modelList.add(new AppDataUsageModel(packageManager.getApplicationLabel(applicationInfo).toString(),
                            applicationInfo.packageName,
                            applicationInfo.uid,
                            false));
                }
            }

            for (int i = 0; i < modelList.size(); i++) {
                model = new AppDataUsageModel();
                model.setAppName(modelList.get(i).getAppName());
                model.setPackageName(modelList.get(i).getPackageName());
                model.setUid(modelList.get(i).getUid());
                model.setIsSystemApp(modelList.get(i).isSystemApp());

                databaseHandler.addData(model);
            }

            return null;
        }
    }

    public static List<AppDataUsageModel> getAppsList() {
        return mAppsList;
    }

    public static Boolean isDataLoading() {
        return isDataLoading;
    }

    public static void setIsDataLoading(Boolean isDataLoading) {
        MainActivity.isDataLoading = isDataLoading;
    }

    public static class LoadData extends AsyncTask {
        private final Context mContext;
        private final int session;
        private final int type;
        private int date;

        public LoadData(Context mContext, int session, int type) {
            this.mContext = mContext;
            this.session = session;
            this.type = type;
        }



        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            isDataLoading = true;
            mUserAppsList.clear();
            mSystemAppsList.clear();
            Log.d(TAG, "onPreExecute: load data");
        }

        @Override
        protected Object doInBackground(Object[] objects) {
            Long sent = 0L,
                    systemSent = 0L,
                    received = 0L,
                    systemReceived = 0L,
                    totalSystemSent = 0L,
                    totalSystemReceived = 0L,
                    totalTetheringSent = 0L,
                    totalTetheringReceived = 0L,
                    totalDeletedAppsSent = 0L,
                    totalDeletedAppsReceived = 0L,
                    tetheringTotal = 0L,
                    deletedAppsTotal = 0L;

            date = PreferenceManager.getDefaultSharedPreferences(mContext).getInt(DATA_RESET_DATE, 1);

            DatabaseHandler handler = new DatabaseHandler(mContext);
            List<AppDataUsageModel> list = handler.getUsageList();
            AppDataUsageModel model = null;

            for (int i = 0; i < list.size(); i++) {
                AppDataUsageModel currentData = list.get(i);
                if (currentData.isSystemApp()) {
                    if (type == TYPE_MOBILE_DATA) {
                        try {
                            sent = getAppMobileDataUsage(mContext, currentData.getUid(), session)[0];
                            received = getAppMobileDataUsage(mContext, currentData.getUid(), session)[1];
                            totalSystemSent = totalSystemSent + sent;
                            totalSystemReceived = totalSystemReceived + received;

                            if (sent > 0 || received > 0) {
                                model = new AppDataUsageModel();
                                model.setAppName(currentData.getAppName());
                                model.setPackageName(currentData.getPackageName());
                                model.setUid(currentData.getUid());
                                model.setSentMobile(sent);
                                model.setReceivedMobile(received);
                                model.setSession(session);
                                model.setType(type);

                                Long total = sent + received;
                                Long deviceTotal = getDeviceMobileDataUsage(mContext, session, date)[2];

                                // multiplied by 2 just to increase progress a bit.
                                Double progress = ((total.doubleValue() / deviceTotal.doubleValue()) * 100) * 2;
                                model.setProgress(progress.intValue());

                                mSystemAppsList.add(model);
                            }

                        } catch (ParseException e) {
                            e.printStackTrace();
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    } else {
                        try {
                            sent = getAppWifiDataUsage(mContext, currentData.getUid(), session)[0];
                            received = getAppWifiDataUsage(mContext, currentData.getUid(), session)[1];
                            totalSystemSent = totalSystemSent + sent;
                            totalSystemReceived = totalSystemReceived + received;

                            if (sent > 0 || received > 0) {
                                model = new AppDataUsageModel();
                                model.setAppName(currentData.getAppName());
                                model.setPackageName(currentData.getPackageName());
                                model.setUid(currentData.getUid());
                                model.setSentMobile(sent);
                                model.setReceivedMobile(received);
                                model.setSession(session);
                                model.setType(type);

                                Long total = sent + received;
                                Long deviceTotal = getDeviceWifiDataUsage(mContext, session)[2];

                                Double progress = ((total.doubleValue() / deviceTotal.doubleValue()) * 100) * 2;
                                model.setProgress(progress.intValue());

                                mSystemAppsList.add(model);
                            }
                        } catch (ParseException e) {
                            e.printStackTrace();
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }
                }
                else {
                    if (isAppInstalled(mContext, currentData.getPackageName())) {
                        if (type == TYPE_MOBILE_DATA) {
                            try {
                                sent = getAppMobileDataUsage(mContext, currentData.getUid(), session)[0];
                                received = getAppMobileDataUsage(mContext, currentData.getUid(), session)[1];

                                if (sent > 0 || received > 0) {
                                    model = new AppDataUsageModel();
                                    model.setAppName(currentData.getAppName());
                                    model.setPackageName(currentData.getPackageName());
                                    model.setUid(currentData.getUid());
                                    model.setSentMobile(sent);
                                    model.setReceivedMobile(received);
                                    model.setSession(session);
                                    model.setType(type);

                                    Long total = sent + received;
                                    Long deviceTotal = getDeviceMobileDataUsage(mContext, session, date)[2];

                                    Double progress = ((total.doubleValue() / deviceTotal.doubleValue()) * 100) * 2;
                                    model.setProgress(progress.intValue());

                                    mUserAppsList.add(model);
                                }


                            } catch (ParseException e) {
                                e.printStackTrace();
                            } catch (RemoteException e) {
                                e.printStackTrace();
                            }
                        } else {
                            try {
                                sent = getAppWifiDataUsage(mContext, currentData.getUid(), session)[0];
                                received = getAppWifiDataUsage(mContext, currentData.getUid(), session)[1];

                                if (sent > 0 || received > 0) {
                                    model = new AppDataUsageModel();
                                    model.setAppName(currentData.getAppName());
                                    model.setPackageName(currentData.getPackageName());
                                    model.setUid(currentData.getUid());
                                    model.setSentMobile(sent);
                                    model.setReceivedMobile(received);
                                    model.setSession(session);
                                    model.setType(type);

                                    Long total = sent + received;
                                    Long deviceTotal = getDeviceWifiDataUsage(mContext, session)[2];

                                    Double progress = ((total.doubleValue() / deviceTotal.doubleValue()) * 100) * 2;
                                    model.setProgress(progress.intValue());

                                    mUserAppsList.add(model);
                                }

                            } catch (ParseException e) {
                                e.printStackTrace();
                            } catch (RemoteException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }

            model = new AppDataUsageModel();
            model.setAppName(mContext.getString(R.string.label_system_apps));
            model.setPackageName(mContext.getString(R.string.package_system));
            model.setSentMobile(totalSystemSent);
            model.setReceivedMobile(totalSystemReceived);
            model.setSession(session);
            model.setType(type);

            Long total = totalSystemSent + totalSystemReceived;

            Long deviceTotal = null;
            if (type == TYPE_MOBILE_DATA) {
                try {
                    deviceTotal = getDeviceMobileDataUsage(mContext, session, date)[2];
                    Double progress = ((total.doubleValue() / deviceTotal.doubleValue()) * 100) * 2;
                    model.setProgress(progress.intValue());

                } catch (ParseException e) {
                    e.printStackTrace();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    deviceTotal = getDeviceWifiDataUsage(mContext, session)[2];
                    Double progress = ((total.doubleValue() / deviceTotal.doubleValue()) * 100) * 2;
                    model.setProgress(progress.intValue());

                } catch (ParseException e) {
                    e.printStackTrace();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }

            if (deviceTotal > 0) {
                mUserAppsList.add(model);
            }

            try {
                if (type == TYPE_MOBILE_DATA) {
                    totalTetheringSent = getTetheringDataUsage(mContext, session)[0];
                    totalTetheringReceived = getTetheringDataUsage(mContext, session)[1];
                    tetheringTotal = totalTetheringSent + totalTetheringReceived;

                    Double tetheringProgress = ((tetheringTotal.doubleValue() / deviceTotal.doubleValue()) * 100) * 2;

                    model = new AppDataUsageModel();
                    model.setAppName(mContext.getString(R.string.label_tethering));
                    model.setPackageName(mContext.getString(R.string.package_tethering));
                    model.setSentMobile(totalTetheringSent);
                    model.setReceivedMobile(totalTetheringReceived);
                    model.setSession(session);
                    model.setType(type);
                    model.setProgress(tetheringProgress.intValue());

                    if (tetheringTotal > 0) {
                        mUserAppsList.add(model);
                    }


                    totalDeletedAppsSent = getDeletedAppsMobileDataUsage(mContext, session)[0];
                    totalDeletedAppsReceived = getDeletedAppsMobileDataUsage(mContext, session)[1];
                } else {
                    totalDeletedAppsSent = getDeletedAppsWifiDataUsage(mContext, session)[0];
                    totalDeletedAppsReceived = getDeletedAppsWifiDataUsage(mContext, session)[1];
                }
                deletedAppsTotal = totalDeletedAppsSent + totalDeletedAppsReceived;

                Double deletedProgress = ((deletedAppsTotal.doubleValue() / deviceTotal.doubleValue()) * 100) * 2;

                model = new AppDataUsageModel();
                model.setAppName(mContext.getString(R.string.label_removed));
                model.setPackageName(mContext.getString(R.string.package_removed));
                model.setSentMobile(totalDeletedAppsSent);
                model.setReceivedMobile(totalDeletedAppsReceived);
                model.setSession(session);
                model.setType(type);
                model.setProgress(deletedProgress.intValue());

                if (deletedAppsTotal > 0) {
                    mUserAppsList.add(model);
                }


                Collections.sort(mUserAppsList, new Comparator<AppDataUsageModel>() {
                    @Override
                    public int compare(AppDataUsageModel o1, AppDataUsageModel o2) {
                        o1.setMobileTotal((o1.getSentMobile() + o1.getReceivedMobile()) / 1024f);
                        o2.setMobileTotal((o2.getSentMobile() + o2.getReceivedMobile()) / 1024f);
                        return o1.getMobileTotal().compareTo(o2.getMobileTotal());
                    }
                });

                Collections.reverse(mUserAppsList);

                Collections.sort(mSystemAppsList, new Comparator<AppDataUsageModel>() {
                    @Override
                    public int compare(AppDataUsageModel o1, AppDataUsageModel o2) {
                        o1.setMobileTotal((o1.getSentMobile() + o1.getReceivedMobile()) / 1024f);
                        o2.setMobileTotal((o2.getSentMobile() + o2.getReceivedMobile()) / 1024f);
                        return o1.getMobileTotal().compareTo(o2.getMobileTotal());
                    }
                });

                Collections.reverse(mSystemAppsList);


            } catch (ParseException e) {
                e.printStackTrace();
            } catch (RemoteException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Object o) {
            super.onPostExecute(o);
            isDataLoading = false;
            if (getAppContext() != null) {
                onDataLoaded();
            } else {

            }
            FetchApps fetchApps = new FetchApps(mContext);
            fetchApps.execute();
        }

    }

    public static void setTheme(Activity activity) {
        Boolean nightMode = PreferenceManager.getDefaultSharedPreferences(activity.getApplicationContext())
                .getBoolean("dark_mode_toggle", false);
        if (nightMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else if (!nightMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        }

    }
}