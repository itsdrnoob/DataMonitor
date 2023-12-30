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

import static com.drnoob.datamonitor.Common.isAppInstalled;
import static com.drnoob.datamonitor.Common.isReadPhoneStateGranted;
import static com.drnoob.datamonitor.Common.isUsageAccessGranted;
import static com.drnoob.datamonitor.Common.refreshService;
import static com.drnoob.datamonitor.Common.setLanguage;
import static com.drnoob.datamonitor.core.Values.ALARM_PERMISSION_DENIED;
import static com.drnoob.datamonitor.core.Values.APP_COUNTRY_CODE;
import static com.drnoob.datamonitor.core.Values.APP_DATA_USAGE_WARNING_CHANNEL_ID;
import static com.drnoob.datamonitor.core.Values.APP_DATA_USAGE_WARNING_CHANNEL_NAME;
import static com.drnoob.datamonitor.core.Values.APP_LANGUAGE_CODE;
import static com.drnoob.datamonitor.core.Values.APP_THEME;
import static com.drnoob.datamonitor.core.Values.BOTTOM_NAVBAR_ITEM_SETTINGS;
import static com.drnoob.datamonitor.core.Values.DATA_RESET_DATE;
import static com.drnoob.datamonitor.core.Values.DATA_USAGE_NOTIFICATION_CHANNEL_ID;
import static com.drnoob.datamonitor.core.Values.DATA_USAGE_NOTIFICATION_CHANNEL_NAME;
import static com.drnoob.datamonitor.core.Values.DATA_USAGE_SYSTEM;
import static com.drnoob.datamonitor.core.Values.DATA_USAGE_VALUE;
import static com.drnoob.datamonitor.core.Values.DATA_USAGE_WARNING_CHANNEL_ID;
import static com.drnoob.datamonitor.core.Values.DATA_USAGE_WARNING_CHANNEL_NAME;
import static com.drnoob.datamonitor.core.Values.DISABLE_BATTERY_OPTIMISATION_FRAGMENT;
import static com.drnoob.datamonitor.core.Values.GENERAL_FRAGMENT_ID;
import static com.drnoob.datamonitor.core.Values.NETWORK_SIGNAL_CHANNEL_ID;
import static com.drnoob.datamonitor.core.Values.NETWORK_SIGNAL_CHANNEL_NAME;
import static com.drnoob.datamonitor.core.Values.OTHER_NOTIFICATION_CHANNEL_ID;
import static com.drnoob.datamonitor.core.Values.OTHER_NOTIFICATION_CHANNEL_NAME;
import static com.drnoob.datamonitor.core.Values.READ_PHONE_STATE_DISABLED;
import static com.drnoob.datamonitor.core.Values.REQUEST_POST_NOTIFICATIONS;
import static com.drnoob.datamonitor.core.Values.SESSION_TODAY;
import static com.drnoob.datamonitor.core.Values.SETUP_COMPLETED;
import static com.drnoob.datamonitor.core.Values.SETUP_VALUE;
import static com.drnoob.datamonitor.core.Values.SHOULD_SHOW_BATTERY_OPTIMISATION_ERROR;
import static com.drnoob.datamonitor.core.Values.TYPE_MOBILE_DATA;
import static com.drnoob.datamonitor.core.Values.UPDATE_NOTIFICATION_CHANNEL;
import static com.drnoob.datamonitor.core.Values.UPDATE_VERSION;
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

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.RemoteException;
import android.provider.Settings;
import android.text.Spannable;
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
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import androidx.preference.PreferenceManager;

import com.drnoob.datamonitor.BuildConfig;
import com.drnoob.datamonitor.R;
import com.drnoob.datamonitor.Widget.DataUsageWidget;
import com.drnoob.datamonitor.adapters.data.AppDataUsageModel;
import com.drnoob.datamonitor.core.task.DatabaseHandler;
import com.drnoob.datamonitor.databinding.ActivityMainBinding;
import com.drnoob.datamonitor.utils.CrashReporter;
import com.drnoob.datamonitor.utils.SharedPreferences;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.elevation.SurfaceColors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    ActivityMainBinding binding;

    public static List<AppDataUsageModel> mAppsList = new ArrayList<>();
    public static List<AppDataUsageModel> mUserAppsList = new ArrayList<>();
    public static List<AppDataUsageModel> mSystemAppsList = new ArrayList<>();
    public static int value;
    public static String themeSwitch;
    private static Boolean isDataLoading = false;
    private static Boolean refreshAppDataUsage = false;

    public static Boolean getRefreshAppDataUsage() {
        return refreshAppDataUsage;
    }

    public static void setRefreshAppDataUsage(Boolean refreshAppDataUsage) {
        MainActivity.refreshAppDataUsage = refreshAppDataUsage;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        MainActivity.setTheme(MainActivity.this);
        Thread.setDefaultUncaughtExceptionHandler(new CrashReporter(MainActivity.this));
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
        } else {
            setLanguage(this, languageCode, countryCode);
        }

        try {
            refreshService(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            if (isUsageAccessGranted(MainActivity.this)) {

                binding = ActivityMainBinding.inflate(getLayoutInflater());
                setTheme(R.style.Theme_DataMonitor);
                setContentView(binding.getRoot());
                setSupportActionBar(binding.mainToolbar);
                binding.mainToolbar.setBackgroundColor(SurfaceColors.SURFACE_2.getColor(this));
                getWindow().setStatusBarColor(SurfaceColors.SURFACE_2.getColor(this));
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                    getWindow().setNavigationBarColor(SurfaceColors.SURFACE_2.getColor(this));
                }

                SharedPreferences.getUserPrefs(this).edit().putBoolean(SETUP_COMPLETED, true).apply();

                if (binding.bottomNavigationView.getSelectedItemId() == R.id.bottom_menu_home) {
                    getSupportActionBar().setTitle(getString(R.string.app_name));
                }

                NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.main_nav_host_fragment);
                NavController controller = navHostFragment.getNavController();
                controller.addOnDestinationChangedListener(new NavController.OnDestinationChangedListener() {
                    @Override
                    public void onDestinationChanged(@NotNull NavController navController, @NotNull NavDestination navDestination, @Nullable Bundle bundle) {
                        changeBanner(navDestination);
                    }
                });   // working

                NavigationUI.setupWithNavController(binding.bottomNavigationView, controller);

                DatabaseHandler databaseHandler = new DatabaseHandler(MainActivity.this);
                if (databaseHandler.getUsageList() != null && databaseHandler.getUsageList().size() > 0) {

                } else {
                    MainActivity.FetchApps fetchApps = new MainActivity.FetchApps(this);
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
                        MainActivity.LoadData loadData = new MainActivity.LoadData(MainActivity.this, SESSION_TODAY, TYPE_MOBILE_DATA);
                        loadData.execute();
                    }
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    NotificationManager notificationManager = getSystemService(NotificationManager.class);
                    if (!notificationManager.areNotificationsEnabled()) {
                        requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_POST_NOTIFICATIONS);
                    }
                }
            } else {
                onResume();
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_POST_NOTIFICATIONS) {
            if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                new MaterialAlertDialogBuilder(MainActivity.this)
                        .setTitle(R.string.label_permission_denied)
                        .setMessage(R.string.notification_permission_denied_body)
                        .setPositiveButton(R.string.action_grant, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                Intent intent = new Intent();
                                intent.setAction("android.settings.APP_NOTIFICATION_SETTINGS");
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                intent.putExtra("app_package", getPackageName());
                                intent.putExtra("app_uid", getApplicationInfo().uid);
                                intent.putExtra("android.provider.extra.APP_PACKAGE", getPackageName());

                                startActivity(intent);
                            }
                        })
                        .setNegativeButton(R.string.action_cancel, null)
                        .show();
            }
        }
    }

    private void checkBatteryOptimisationState() {
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        if (powerManager.isIgnoringBatteryOptimizations(getPackageName())) {
            // Battery optimisation is disabled
            Log.d(TAG, "checkBatteryOptimisationState: Disabled");
        } else {
            // Battery optimisation is enabled
            Log.d(TAG, "checkBatteryOptimisationState: Enabled");
            if (SharedPreferences.getUserPrefs(this).getBoolean(SHOULD_SHOW_BATTERY_OPTIMISATION_ERROR, true)) {
                new MaterialAlertDialogBuilder(this)
                        .setTitle(getString(R.string.label_battery_optimisation))
                        .setMessage(getString(R.string.battery_optimisation_enabled_info))
                        .setPositiveButton(getString(R.string.disable_battery_optimisation), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                startActivity(new Intent(MainActivity.this, ContainerActivity.class)
                                        .putExtra(GENERAL_FRAGMENT_ID, DISABLE_BATTERY_OPTIMISATION_FRAGMENT));
                            }
                        })
                        .setNegativeButton(getString(R.string.label_do_not_show_again), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                SharedPreferences.getUserPrefs(MainActivity.this).edit()
                                        .putBoolean(SHOULD_SHOW_BATTERY_OPTIMISATION_ERROR, false)
                                        .apply();
                                dialog.dismiss();
                            }
                        })
                        .setNeutralButton(getString(R.string.action_cancel), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .show();
            }
        }
    }

    private void initializebottomNavigationViewBar() {
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.main_nav_host_fragment);
        NavController controller = navHostFragment.getNavController();
        controller.addOnDestinationChangedListener(new NavController.OnDestinationChangedListener() {
            @Override
            public void onDestinationChanged(@NotNull NavController navController, @NotNull NavDestination navDestination, @Nullable Bundle bundle) {
                changeBanner(navDestination);
            }
        });   // working

//        NavigationUI.setupWithNavController(binding.bottomNavigationViewigationView, controller);  // working
    }


    private void changeBanner(NavDestination navDestination) {
        String destination = navDestination.getLabel().toString();
        Spannable banner;

        if (destination.equalsIgnoreCase(getString(R.string.home))) {
            // Home Fragment
            getSupportActionBar().setTitle(getString(R.string.app_name));
        } else if (destination.equalsIgnoreCase(getString(R.string.setup))) {
            // Setup Fragment
            getSupportActionBar().setTitle(getString(R.string.setup));
        } else if (destination.equalsIgnoreCase(getString(R.string.app_data_usage))) {
            // App data usage Fragment
            getSupportActionBar().setTitle(getString(R.string.app_data_usage));
        } else if (destination.equalsIgnoreCase(getString(R.string.network_diagnostics))) {
            // Network diagnostics Fragment
            getSupportActionBar().setTitle(getString(R.string.network_diagnostics));
        } else {
            // Unknown Fragment
        }

    }


    @Override
    protected void onStart() {
        super.onStart();
        verifyAppVersion();
//        initializebottomNavigationViewBar();

        if (!PreferenceManager.getDefaultSharedPreferences(MainActivity.this)
                .getBoolean(ALARM_PERMISSION_DENIED, false)) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (!alarmManager.canScheduleExactAlarms()) {
                    new MaterialAlertDialogBuilder(this)
                            .setTitle(getString(R.string.error_alarm_permission_denied))
                            .setMessage(getString(R.string.error_alarm_permission_denied_dialog_summary))
                            .setCancelable(false)
                            .setPositiveButton(getString(R.string.action_grant), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Intent intent = new Intent();
                                    intent.setAction(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    dialog.dismiss();
                                    startActivity(intent);
                                }
                            })
                            .setNegativeButton(getString(R.string.action_cancel), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            })
                            .setOnDismissListener(new DialogInterface.OnDismissListener() {
                                @Override
                                public void onDismiss(DialogInterface dialog) {
                                    PreferenceManager.getDefaultSharedPreferences(MainActivity.this).edit()
                                            .putBoolean(ALARM_PERMISSION_DENIED, true)
                                            .apply();
                                }
                            })
                            .show();
                }
            }
        }
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
        try {
            checkBatteryOptimisationState();
        } catch (Exception e) {
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
//                startActivity(new Intent(MainActivity.this, MainActivity.class));
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
                NotificationManager.IMPORTANCE_HIGH);
        NotificationChannel otherChannel = new NotificationChannel(OTHER_NOTIFICATION_CHANNEL_ID, OTHER_NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH);
        warningChannel.enableVibration(true);
        warningChannel.enableLights(true);
        appWarningChannel.enableVibration(true);
        appWarningChannel.enableLights(true);
        Uri sound = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + getPackageName() + "/" + R.raw.silent);
        AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build();
//        networkSignalChannel.setSound(sound, attributes);
        networkSignalChannel.setSound(Uri.EMPTY, null);
        networkSignalChannel.setShowBadge(false);
        networkSignalChannel.enableVibration(false);
        networkSignalChannel.enableLights(false);
        networkSignalChannel.setBypassDnd(true);
        otherChannel.enableVibration(true);
        otherChannel.enableLights(true);

        List<NotificationChannel> channels = new ArrayList<>();
        channels.add(usageChannel);
        channels.add(warningChannel);
        channels.add(appWarningChannel);
        channels.add(networkSignalChannel);
        channels.add(otherChannel);


        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (PreferenceManager.getDefaultSharedPreferences(MainActivity.this)
                .getBoolean(UPDATE_NOTIFICATION_CHANNEL, true)) {
            notificationManager.deleteNotificationChannel("NetworkSignal.Notification");
            PreferenceManager.getDefaultSharedPreferences(MainActivity.this).edit()
                    .putBoolean(UPDATE_NOTIFICATION_CHANNEL, false)
                    .apply();
        }
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
                                int progressInt;
                                if (progress != null) {
                                    progressInt = progress.intValue();
                                } else {
                                    progressInt = 0;
                                }
                                model.setProgress(progressInt);

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
                                int progressInt;
                                if (progress != null) {
                                    progressInt = progress.intValue();
                                } else {
                                    progressInt = 0;
                                }
                                model.setProgress(progressInt);

                                mSystemAppsList.add(model);
                            }
                        } catch (ParseException e) {
                            e.printStackTrace();
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }
                } else {
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
                                    int progressInt;
                                    if (progress != null) {
                                        progressInt = progress.intValue();
                                    } else {
                                        progressInt = 0;
                                    }
                                    model.setProgress(progressInt);

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
                                    int progressInt;
                                    if (progress != null) {
                                        progressInt = progress.intValue();
                                    } else {
                                        progressInt = 0;
                                    }
                                    model.setProgress(progressInt);

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
                    int progressInt;
                    if (progress != null) {
                        progressInt = progress.intValue();
                    } else {
                        progressInt = 0;
                    }
                    model.setProgress(progressInt);

                } catch (ParseException e) {
                    e.printStackTrace();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    deviceTotal = getDeviceWifiDataUsage(mContext, session)[2];
                    Double progress = ((total.doubleValue() / deviceTotal.doubleValue()) * 100) * 2;
                    int progressInt;
                    if (progress != null) {
                        progressInt = progress.intValue();
                    } else {
                        progressInt = 0;
                    }
                    model.setProgress(progressInt);

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
                    int tetheringProgressInt;
                    if (tetheringProgress != null) {
                        tetheringProgressInt = tetheringProgress.intValue();
                    } else {
                        tetheringProgressInt = 0;
                    }

                    model = new AppDataUsageModel();
                    model.setAppName(mContext.getString(R.string.label_tethering));
                    model.setPackageName(mContext.getString(R.string.package_tethering));
                    model.setSentMobile(totalTetheringSent);
                    model.setReceivedMobile(totalTetheringReceived);
                    model.setSession(session);
                    model.setType(type);
                    model.setProgress(tetheringProgressInt);

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
                int deletedProgressInt;
                if (deletedProgress != null) {
                    deletedProgressInt = deletedProgress.intValue();
                } else {
                    deletedProgressInt = 0;
                }

                model = new AppDataUsageModel();
                model.setAppName(mContext.getString(R.string.label_removed));
                model.setPackageName(mContext.getString(R.string.package_removed));
                model.setSentMobile(totalDeletedAppsSent);
                model.setReceivedMobile(totalDeletedAppsReceived);
                model.setSession(session);
                model.setType(type);
                model.setProgress(deletedProgressInt);

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
                onDataLoaded(getAppContext());
            } else {

            }
            MainActivity.FetchApps fetchApps = new MainActivity.FetchApps(mContext);
            fetchApps.execute();
        }

    }

    private void verifyAppVersion() {
        String updateVersion = SharedPreferences.getAppPrefs(MainActivity.this)
                .getString(UPDATE_VERSION, BuildConfig.VERSION_NAME);
        if (updateVersion.equalsIgnoreCase(BuildConfig.VERSION_NAME)) {
            SharedPreferences.getAppPrefs(MainActivity.this)
                    .edit().remove(UPDATE_VERSION).apply();
        }
    }

    public static void setTheme(Activity activity) {
        String theme = PreferenceManager.getDefaultSharedPreferences(activity).getString(APP_THEME, "system");
        switch (theme) {
            case "dark":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;

            case "light":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;

            case "system":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;

            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }

    }
}