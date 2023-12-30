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

import static com.drnoob.datamonitor.Common.cancelDataPlanNotification;
import static com.drnoob.datamonitor.Common.dismissOnClick;
import static com.drnoob.datamonitor.Common.formatOrdinalNumber;
import static com.drnoob.datamonitor.Common.postNotification;
import static com.drnoob.datamonitor.Common.setDataPlanNotification;
import static com.drnoob.datamonitor.Common.setRefreshAlarm;
import static com.drnoob.datamonitor.Common.showAlarmPermissionDeniedDialog;
import static com.drnoob.datamonitor.core.Values.APP_DATA_LIMIT_FRAGMENT;
import static com.drnoob.datamonitor.core.Values.DATA_LIMIT;
import static com.drnoob.datamonitor.core.Values.DATA_PLAN_FRAGMENT;
import static com.drnoob.datamonitor.core.Values.DATA_RESET;
import static com.drnoob.datamonitor.core.Values.DATA_RESET_CUSTOM;
import static com.drnoob.datamonitor.core.Values.DATA_RESET_DAILY;
import static com.drnoob.datamonitor.core.Values.DATA_RESET_DATE;
import static com.drnoob.datamonitor.core.Values.DATA_RESET_HOUR;
import static com.drnoob.datamonitor.core.Values.DATA_RESET_MIN;
import static com.drnoob.datamonitor.core.Values.DATA_RESET_MONTHLY;
import static com.drnoob.datamonitor.core.Values.DATA_TYPE;
import static com.drnoob.datamonitor.core.Values.DATA_USAGE_ALERT;
import static com.drnoob.datamonitor.core.Values.DATA_USAGE_WARNING_SHOWN;
import static com.drnoob.datamonitor.core.Values.DATA_WARNING_TRIGGER_LEVEL;
import static com.drnoob.datamonitor.core.Values.EXCLUDE_APPS_FRAGMENT;
import static com.drnoob.datamonitor.core.Values.GENERAL_FRAGMENT_ID;
import static com.drnoob.datamonitor.core.Values.ICON_DATA_USAGE;
import static com.drnoob.datamonitor.core.Values.ICON_NETWORK_SPEED;
import static com.drnoob.datamonitor.core.Values.NETWORK_SIGNAL_CHANNEL_ID;
import static com.drnoob.datamonitor.core.Values.NETWORK_SIGNAL_NOTIFICATION_GROUP;
import static com.drnoob.datamonitor.core.Values.NETWORK_SIGNAL_NOTIFICATION_ID;
import static com.drnoob.datamonitor.core.Values.NOTIFICATION_REFRESH_INTERVAL;
import static com.drnoob.datamonitor.core.Values.NOTIFICATION_REFRESH_INTERVAL_SUMMARY;
import static com.drnoob.datamonitor.core.Values.NOTIFICATION_WIFI;
import static com.drnoob.datamonitor.core.Values.SESSION_TODAY;
import static com.drnoob.datamonitor.core.Values.WIDGET_REFRESH_INTERVAL;
import static com.drnoob.datamonitor.core.Values.WIDGET_REFRESH_INTERVAL_SUMMARY;
import static com.drnoob.datamonitor.utils.NetworkStatsHelper.formatData;
import static com.drnoob.datamonitor.utils.NetworkStatsHelper.getDeviceMobileDataUsage;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.Fragment;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.TimePicker;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.drnoob.datamonitor.Common;
import com.drnoob.datamonitor.R;
import com.drnoob.datamonitor.Widget.DataUsageWidget;
import com.drnoob.datamonitor.core.Values;
import com.drnoob.datamonitor.core.base.DatePicker;
import com.drnoob.datamonitor.core.base.Preference;
import com.drnoob.datamonitor.core.base.SwitchPreferenceCompat;
import com.drnoob.datamonitor.ui.activities.ContainerActivity;
import com.drnoob.datamonitor.ui.activities.MainActivity;
import com.drnoob.datamonitor.utils.CompoundNotification;
import com.drnoob.datamonitor.utils.DailyQuotaAlertReceiver;
import com.drnoob.datamonitor.utils.DataUsageMonitor;
import com.drnoob.datamonitor.utils.LiveNetworkMonitor;
import com.drnoob.datamonitor.utils.NotificationService;
import com.drnoob.datamonitor.utils.NotificationService.NotificationUpdater;
import com.drnoob.datamonitor.utils.SmartDataAllocationService;
import com.drnoob.datamonitor.utils.VibrationUtils;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.slider.LabelFormatter;
import com.google.android.material.slider.Slider;
import com.google.android.material.snackbar.Snackbar;

import org.jetbrains.annotations.NotNull;

import java.text.ParseException;
import java.util.Calendar;

public class SetupFragment extends Fragment {
    private static final String TAG = SetupFragment.class.getSimpleName();
    private static Context mContext;

    public SetupFragment() {
        // Required empty public constructor
    }

    public static class SetupPreference extends PreferenceFragmentCompat {
        private static final String TAG = SetupPreference.class.getSimpleName();
        private final int CALENDAR_YEAR = 2000;

        private Preference mSetupWidget, mWidgetRefreshInterval, mNotificationRefreshInterval,
                mAddDataPlan, mUsageResetTime, mWidgetRefresh, mDataWarningTrigger, mAppDataLimit,
                mCombinedNotificationIcon, mExcludeApps;
        private SwitchPreferenceCompat mSetupNotification, mRemainingDataInfo, mShowWifiWidget,
                mShowMobileData, mShowWifi, mShowDataWarning, mNetworkSignalNotification,
                mAutoHideNetworkSpeed, mCombineNotifications, mLockscreenNotifications, mAlwaysShowTotal,
                mAutoUpdateDataPlan, mSmartDataAllocation, mDailyQuotaAlert;
        private Snackbar snackbar;
        private Long planStartDateMillis, planEndDateMillis;
        private Intent liveNetworkMonitorIntent;
        private ActivityResultLauncher<Intent> dataPlanLauncher;

        @Override
        public void onAttach(@NonNull Context context) {
            super.onAttach(context);
            mContext = context;
        }

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
        }

        @Override
        public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            dataPlanLauncher = registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
                        @Override
                        public void onActivityResult(ActivityResult result) {
                            if (result.getResultCode() == Activity.RESULT_OK) {
                                snackbar = Snackbar.make(getActivity().findViewById(R.id.main_root),
                                                getString(R.string.label_data_plan_saved), Snackbar.LENGTH_SHORT)
                                        .setAnchorView(getActivity().findViewById(R.id.bottomNavigationView));
                                updateResetData();
                                refreshDataPlanSettingsVisibility();
                                refreshQuotaAlertVisibility();
                                updateDailyQuota();
                                if (PreferenceManager.getDefaultSharedPreferences(requireContext())
                                        .getString(DATA_RESET, "null").equals(DATA_RESET_CUSTOM)) {
                                    if (PreferenceManager.getDefaultSharedPreferences(requireContext())
                                            .getBoolean("auto_update_data_plan", false)) {
                                        setRefreshAlarm(requireContext());
                                    }
                                    else {
                                        setDataPlanNotification(requireContext());
                                    }
                                }
                                dismissOnClick(snackbar);
                                snackbar.show();
                                AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(getContext());
                                int[] ids = appWidgetManager.getAppWidgetIds(new ComponentName(getContext(), DataUsageWidget.class));
                                Intent intent = new Intent(getContext(), DataUsageWidget.class);
                                intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);

                                boolean updateNotification = PreferenceManager.getDefaultSharedPreferences(getContext()).getBoolean("setup_notification", false);
                                if (updateNotification) {
                                    Intent notificationIntent = new Intent(getContext(), NotificationUpdater.class);
                                    getContext().sendBroadcast(notificationIntent);
                                }

                                getContext().sendBroadcast(intent);
                            }
                        }
                    }
            );
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.setup_preference, rootKey);

            mSetupWidget = (Preference) findPreference("setup_widget");
            mWidgetRefreshInterval = (Preference) findPreference("widget_refresh_interval");
            mNotificationRefreshInterval = (Preference) findPreference("notification_refresh_interval");
            mAddDataPlan = (Preference) findPreference("add_data_plan");
            mUsageResetTime = (Preference) findPreference("data_usage_reset_time");
            mWidgetRefresh = (Preference) findPreference("refresh_widget");
            mDataWarningTrigger = (Preference) findPreference("data_warning_trigger_level");
            mAppDataLimit = (Preference) findPreference("app_data_limit");
            mCombinedNotificationIcon = (Preference) findPreference("combined_notification_icon");
            mExcludeApps = (Preference) findPreference("exclude_apps");

            mSetupNotification = (SwitchPreferenceCompat) findPreference("setup_notification");
            mNetworkSignalNotification = (SwitchPreferenceCompat) findPreference("network_signal_notification");
            mRemainingDataInfo = (SwitchPreferenceCompat) findPreference("remaining_data_info");
            mShowMobileData = (SwitchPreferenceCompat) findPreference("show_mobile_data_notification");
            mShowWifi = (SwitchPreferenceCompat) findPreference("show_wifi_notification");
            mShowDataWarning = (SwitchPreferenceCompat) findPreference("data_usage_alert");
            mAutoHideNetworkSpeed = (SwitchPreferenceCompat) findPreference("auto_hide_network_speed");
            mShowWifiWidget = (SwitchPreferenceCompat) findPreference("widget_show_wifi_usage");
            mCombineNotifications = (SwitchPreferenceCompat) findPreference("combine_notifications");
            mLockscreenNotifications = (SwitchPreferenceCompat) findPreference("lockscreen_notification");
            mAlwaysShowTotal = (SwitchPreferenceCompat) findPreference("always_show_total");
            mAutoUpdateDataPlan = (SwitchPreferenceCompat) findPreference("auto_update_data_plan");
            mSmartDataAllocation = (SwitchPreferenceCompat) findPreference("smart_data_allocation");
            mDailyQuotaAlert = (SwitchPreferenceCompat) findPreference("daily_quota_alert");

            liveNetworkMonitorIntent = new Intent(getContext(), LiveNetworkMonitor.class);


            int widgetRefreshInterval = PreferenceManager.getDefaultSharedPreferences(getContext())
                    .getInt(WIDGET_REFRESH_INTERVAL, 60000);
            int notificationRefreshInterval = PreferenceManager.getDefaultSharedPreferences(getContext())
                    .getInt(NOTIFICATION_REFRESH_INTERVAL, 60000);
            String widgetRefreshSummary = getString(R.string.option_1_min);
            String notificationRefreshSummary = getString(R.string.option_1_min);

            switch (widgetRefreshInterval) {
                case 60000:
                    widgetRefreshSummary = getString(R.string.option_1_min);
                    break;

                case 120000:
                    widgetRefreshSummary = getString(R.string.option_2_min);
                    break;

                case 300000:
                    widgetRefreshSummary = getString(R.string.option_5_min);
                    break;

                case 600000:
                    widgetRefreshSummary = getString(R.string.option_10_min);
                    break;

                case 900000:
                    widgetRefreshSummary = getString(R.string.option_15_min);
                    break;
            }

            switch (notificationRefreshInterval) {
                case 60000:
                    notificationRefreshSummary = getString(R.string.option_1_min);
                    break;

                case 120000:
                    notificationRefreshSummary = getString(R.string.option_2_min);
                    break;

                case 300000:
                    notificationRefreshSummary = getString(R.string.option_5_min);
                    break;

                case 600000:
                    notificationRefreshSummary = getString(R.string.option_10_min);
                    break;

                case 900000:
                    notificationRefreshSummary = getString(R.string.option_15_min);
                    break;
            }

            mWidgetRefreshInterval.setSummary(widgetRefreshSummary);
            mNotificationRefreshInterval.setSummary(notificationRefreshSummary);
            mDataWarningTrigger.setSummary(getContext().getString(R.string.label_data_trigger_level,
                    String.valueOf(PreferenceManager.getDefaultSharedPreferences(getContext()).
                            getInt("data_warning_trigger_level", 85))));

            updateResetData();
            refreshDataPlanSettingsVisibility();
            refreshQuotaAlertVisibility();

            mCombinedNotificationIcon.setVisible(PreferenceManager.getDefaultSharedPreferences(getContext())
                    .getBoolean("combine_notifications", false));

            mSetupNotification.setOnPreferenceClickListener(new androidx.preference.Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(androidx.preference.Preference preference) {
                    AlarmManager alarmManager = (AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                        showAlarmPermissionDeniedDialog(requireContext());
                        mSetupNotification.setChecked(false);
                    }
                    else {
                        boolean isCombinedNotifiationEnabled = PreferenceManager.getDefaultSharedPreferences(getContext())
                                .getBoolean("combine_notifications", false);
                        if (isCombinedNotifiationEnabled) {
                            snackbar = Snackbar.make(getActivity().findViewById(R.id.main_root),
                                            getString(R.string.error_combine_notifications_enabled),
                                            Snackbar.LENGTH_SHORT)
                                    .setAnchorView(getActivity().findViewById(R.id.bottomNavigationView));
                            mSetupNotification.setChecked(true);
                        }
                        else {
                            boolean isChecked = PreferenceManager.getDefaultSharedPreferences(getContext()).getBoolean("setup_notification", false);
                            if (isChecked) {
                                getContext().startService(new Intent(getContext(), NotificationService.class));
                                Log.d(TAG, "onPreferenceClick: Notification started");
                                snackbar = Snackbar.make(getActivity().findViewById(R.id.main_root),
                                                getString(R.string.label_notification_enabled), Snackbar.LENGTH_SHORT)
                                        .setAnchorView(getActivity().findViewById(R.id.bottomNavigationView));
                            }
                            else {
                                getContext().stopService(new Intent(getContext(), NotificationService.class));
                                Log.d(TAG, "onPreferenceClick: Notification stopped");
                                snackbar = Snackbar.make(getActivity().findViewById(R.id.main_root),
                                                getString(R.string.notification_disabled), Snackbar.LENGTH_SHORT)
                                        .setAnchorView(getActivity().findViewById(R.id.bottomNavigationView));
                            }
                        }
                        dismissOnClick(snackbar);
                        snackbar.show();
                    }
                    return false;
                }
            });

            mNetworkSignalNotification.setOnPreferenceClickListener(new androidx.preference.Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(androidx.preference.Preference preference) {
                    boolean isCombinedNotifiationEnabled = PreferenceManager.getDefaultSharedPreferences(getContext())
                            .getBoolean("combine_notifications", false);
                    if (isCombinedNotifiationEnabled) {
                        snackbar = Snackbar.make(getActivity().findViewById(R.id.main_root),
                                        getString(R.string.error_combine_notifications_enabled),
                                        Snackbar.LENGTH_SHORT)
                                .setAnchorView(getActivity().findViewById(R.id.bottomNavigationView));
                        mNetworkSignalNotification.setChecked(true);
                    }
                    else {
                        boolean isChecked = PreferenceManager.getDefaultSharedPreferences(getContext()).getBoolean("network_signal_notification", false);
                        if (isChecked) {
                            getContext().startService(new Intent(getContext(), LiveNetworkMonitor.class));
//                        getContext().startService(liveNetworkMonitorIntent);
                            Log.d(TAG, "onPreferenceClick: Notification started");
                            snackbar = Snackbar.make(getActivity().findViewById(R.id.main_root),
                                            getString(R.string.label_network_signal_notification_enabled), Snackbar.LENGTH_SHORT)
                                    .setAnchorView(getActivity().findViewById(R.id.bottomNavigationView));
                        } else {
                            getContext().stopService(new Intent(getContext(), LiveNetworkMonitor.class));
//                        getContext().stopService(liveNetworkMonitorIntent);
                            Log.d(TAG, "onPreferenceClick: Notification stopped");
                            snackbar = Snackbar.make(getActivity().findViewById(R.id.main_root),
                                            getString(R.string.label_network_signal_notification_disabled), Snackbar.LENGTH_SHORT)
                                    .setAnchorView(getActivity().findViewById(R.id.bottomNavigationView));
                        }
                    }
                    dismissOnClick(snackbar);
                    snackbar.show();
                    return false;
                }
            });

            mSetupWidget.setOnPreferenceClickListener(new androidx.preference.Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(androidx.preference.Preference preference) {
                    BottomSheetDialog dialog = new BottomSheetDialog(getContext(), R.style.BottomSheet);
                    View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.layout_widget_setup, null);
                    dialog.setContentView(dialogView);
                    dialog.setOnShowListener(new DialogInterface.OnShowListener() {
                        @Override
                        public void onShow(DialogInterface dialogInterface) {
                            BottomSheetDialog bottomSheetDialog = (BottomSheetDialog) dialogInterface;
                            FrameLayout bottomSheet = bottomSheetDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
                            BottomSheetBehavior.from(bottomSheet).setState(BottomSheetBehavior.STATE_EXPANDED);
                        }
                    });
                    dialog.show();
                    return false;
                }
            });

            mWidgetRefreshInterval.setOnPreferenceClickListener(new androidx.preference.Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(androidx.preference.Preference preference) {
                    BottomSheetDialog dialog = new BottomSheetDialog(getContext(), R.style.BottomSheet);
                    View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.layout_refresh_interval, null);
                    RadioGroup intervalGroup = dialogView.findViewById(R.id.refresh_interval_group);
                    ConstraintLayout footer = dialogView.findViewById(R.id.footer);
                    TextView cancel = footer.findViewById(R.id.cancel);
                    TextView ok = footer.findViewById(R.id.ok);

                    int elapsedTIme = PreferenceManager.getDefaultSharedPreferences(getContext())
                            .getInt(WIDGET_REFRESH_INTERVAL, 60000);
                    switch (elapsedTIme) {
                        case 60000:
                            intervalGroup.check(R.id.interval_1_min);
                            break;

                        case 120000:
                            intervalGroup.check(R.id.interval_2_min);
                            break;

                        case 300000:
                            intervalGroup.check(R.id.interval_5_min);
                            break;

                        case 600000:
                            intervalGroup.check(R.id.interval_10_min);
                            break;

                        case 900000:
                            intervalGroup.check(R.id.interval_15_min);
                            break;
                    }

                    cancel.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            dialog.dismiss();
                        }
                    });

                    ok.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            int elapsedTime = 60000;
                            String refreshGap = getString(R.string.option_1_min);
                            switch (intervalGroup.getCheckedRadioButtonId()) {
                                case R.id.interval_1_min:
                                    elapsedTime = 60000;
                                    refreshGap = getString(R.string.option_1_min);
                                    break;

                                case R.id.interval_2_min:
                                    elapsedTime = 120000;
                                    refreshGap = getString(R.string.option_2_min);
                                    break;

                                case R.id.interval_5_min:
                                    elapsedTime = 300000;
                                    refreshGap = getString(R.string.option_5_min);
                                    break;

                                case R.id.interval_10_min:
                                    elapsedTime = 600000;
                                    refreshGap = getString(R.string.option_10_min);
                                    break;

                                case R.id.interval_15_min:
                                    elapsedTime = 900000;
                                    refreshGap = getString(R.string.option_15_min);
                                    break;
                            }
                            PreferenceManager.getDefaultSharedPreferences(getContext()).edit().putInt(WIDGET_REFRESH_INTERVAL,
                                    elapsedTime).apply();
                            PreferenceManager.getDefaultSharedPreferences(getContext()).edit().putString(WIDGET_REFRESH_INTERVAL_SUMMARY,
                                    refreshGap).apply();
                            snackbar = Snackbar.make(getActivity().findViewById(R.id.main_root),
                                    getContext().getString(R.string.label_widget_refresh_interval_change, refreshGap), Snackbar.LENGTH_SHORT)
                                    .setAnchorView(getActivity().findViewById(R.id.bottomNavigationView));
                            dismissOnClick(snackbar);
                            dialog.dismiss();
                            mWidgetRefreshInterval.setSummary(refreshGap);
                            dismissOnClick(snackbar);
                            snackbar.show();
                            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(getContext());
                            int[] ids = appWidgetManager.getAppWidgetIds(new ComponentName(getContext(), DataUsageWidget.class));
                            Intent intent = new Intent(getContext(), DataUsageWidget.class);
                            intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
                            getContext().sendBroadcast(intent);
                        }
                    });

                    dialog.setContentView(dialogView);
                    dialog.setOnShowListener(new DialogInterface.OnShowListener() {
                        @Override
                        public void onShow(DialogInterface dialogInterface) {
                            BottomSheetDialog bottomSheetDialog = (BottomSheetDialog) dialogInterface;
                            FrameLayout bottomSheet = bottomSheetDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
                            BottomSheetBehavior.from(bottomSheet).setState(BottomSheetBehavior.STATE_EXPANDED);
                        }
                    });
                    dialog.show();
                    return false;
                }
            });

            mNotificationRefreshInterval.setOnPreferenceClickListener(new androidx.preference.Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(androidx.preference.Preference preference) {
                    boolean isCombinedNotificationEnabled = PreferenceManager.getDefaultSharedPreferences(getContext())
                            .getBoolean("combine_notifications", false);
//                    if (isCombinedNotificationEnabled) {
//                        snackbar = Snackbar.make(getActivity().findViewById(R.id.main_root),
//                                        getString(R.string.error_interval_combined_notifications_enabled), Snackbar.LENGTH_LONG)
//                                .setAnchorView(getActivity().findViewById(R.id.bottomNavigationView));
//                        dismissOnClick(snackbar);
//                        snackbar.show();
//                    }
//                    else {
//                        BottomSheetDialog dialog = new BottomSheetDialog(getContext(), R.style.BottomSheet);
//                        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.layout_refresh_interval, null);
//                        RadioGroup intervalGroup = dialogView.findViewById(R.id.refresh_interval_group);
//                        ConstraintLayout footer = dialogView.findViewById(R.id.footer);
//                        TextView cancel = footer.findViewById(R.id.cancel);
//                        TextView ok = footer.findViewById(R.id.ok);
//
//                        int elapsedTIme = PreferenceManager.getDefaultSharedPreferences(getContext())
//                                .getInt(NOTIFICATION_REFRESH_INTERVAL, 60000);
//                        switch (elapsedTIme) {
//                            case 60000:
//                                intervalGroup.check(R.id.interval_1_min);
//                                break;
//
//                            case 120000:
//                                intervalGroup.check(R.id.interval_2_min);
//                                break;
//
//                            case 300000:
//                                intervalGroup.check(R.id.interval_5_min);
//                                break;
//
//                            case 600000:
//                                intervalGroup.check(R.id.interval_10_min);
//                                break;
//
//                            case 900000:
//                                intervalGroup.check(R.id.interval_15_min);
//                                break;
//                        }
//
//                        cancel.setOnClickListener(new View.OnClickListener() {
//                            @Override
//                            public void onClick(View v) {
//                                dialog.dismiss();
//                            }
//                        });
//
//                        ok.setOnClickListener(new View.OnClickListener() {
//                            @Override
//                            public void onClick(View v) {
//                                int elapsedTime = 60000;
//                                String refreshGap = getString(R.string.option_1_min);
//                                switch (intervalGroup.getCheckedRadioButtonId()) {
//                                    case R.id.interval_1_min:
//                                        elapsedTime = 60000;
//                                        refreshGap = getString(R.string.option_1_min);
//                                        break;
//
//                                    case R.id.interval_2_min:
//                                        elapsedTime = 120000;
//                                        refreshGap = getString(R.string.option_2_min);
//                                        break;
//
//                                    case R.id.interval_5_min:
//                                        elapsedTime = 300000;
//                                        refreshGap = getString(R.string.option_5_min);
//                                        break;
//
//                                    case R.id.interval_10_min:
//                                        elapsedTime = 600000;
//                                        refreshGap = getString(R.string.option_10_min);
//                                        break;
//
//                                    case R.id.interval_15_min:
//                                        elapsedTime = 900000;
//                                        refreshGap = getString(R.string.option_15_min);
//                                        break;
//                                }
//                                PreferenceManager.getDefaultSharedPreferences(getContext()).edit().putInt(NOTIFICATION_REFRESH_INTERVAL,
//                                        elapsedTime).apply();
//                                PreferenceManager.getDefaultSharedPreferences(getContext()).edit().putString(NOTIFICATION_REFRESH_INTERVAL_SUMMARY,
//                                        refreshGap).apply();
//                                snackbar = Snackbar.make(getActivity().findViewById(R.id.main_root),
//                                                getString(R.string.label_notification_refresh_interval_change, refreshGap), Snackbar.LENGTH_SHORT)
//                                        .setAnchorView(getActivity().findViewById(R.id.bottomNavigationView));
//                                dialog.dismiss();
//                                mNotificationRefreshInterval.setSummary(refreshGap);
//                                dismissOnClick(snackbar);
//                                snackbar.show();
//                                Intent i = new Intent(getContext(), NotificationUpdater.class);
//                                getContext().sendBroadcast(i);
//                            }
//                        });
//
//                        dialog.setContentView(dialogView);
//                        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
//                            @Override
//                            public void onShow(DialogInterface dialogInterface) {
//                                BottomSheetDialog bottomSheetDialog = (BottomSheetDialog) dialogInterface;
//                                FrameLayout bottomSheet = bottomSheetDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
//                                BottomSheetBehavior.from(bottomSheet).setState(BottomSheetBehavior.STATE_EXPANDED);
//                            }
//                        });
//                        dialog.show();
//                    }

                    BottomSheetDialog dialog = new BottomSheetDialog(getContext(), R.style.BottomSheet);
                    View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.layout_notification_refresh_interval, null);
                    RadioGroup intervalGroup = dialogView.findViewById(R.id.refresh_interval_group);
                    ConstraintLayout footer = dialogView.findViewById(R.id.footer);
                    TextView cancel = footer.findViewById(R.id.cancel);
                    TextView ok = footer.findViewById(R.id.ok);

                    int elapsedTIme = PreferenceManager.getDefaultSharedPreferences(getContext())
                            .getInt(NOTIFICATION_REFRESH_INTERVAL, 60000);

                    if (!isCombinedNotificationEnabled) {
                        intervalGroup.findViewById(R.id.interval_1_sec).setVisibility(View.GONE);
                        intervalGroup.findViewById(R.id.interval_15_sec).setVisibility(View.GONE);
                        intervalGroup.findViewById(R.id.interval_30_sec).setVisibility(View.GONE);
                        if (elapsedTIme < 60000) {
                            elapsedTIme = 60000;
                        }
                    }

                    switch (elapsedTIme) {
                        case 1000:
                            intervalGroup.check(R.id.interval_1_sec);
                            break;

                        case 15000:
                            intervalGroup.check(R.id.interval_15_sec);
                            break;

                        case 30000:
                            intervalGroup.check(R.id.interval_30_sec);
                            break;

                        case 60000:
                            intervalGroup.check(R.id.interval_1_min);
                            break;

                        case 120000:
                            intervalGroup.check(R.id.interval_2_min);
                            break;

                        case 300000:
                            intervalGroup.check(R.id.interval_5_min);
                            break;

                        case 600000:
                            intervalGroup.check(R.id.interval_10_min);
                            break;

                        case 900000:
                            intervalGroup.check(R.id.interval_15_min);
                            break;
                    }

                    cancel.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            dialog.dismiss();
                        }
                    });

                    ok.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            int elapsedTime = 60000;
                            String refreshGap = getString(R.string.option_1_min);
                            switch (intervalGroup.getCheckedRadioButtonId()) {
                                case R.id.interval_1_sec:
                                    elapsedTime = 1000;
                                    refreshGap = getString(R.string.option_1_sec);
                                    break;

                                case R.id.interval_15_sec:
                                    elapsedTime = 15000;
                                    refreshGap = getString(R.string.option_15_sec);
                                    break;

                                case R.id.interval_30_sec:
                                    elapsedTime = 30000;
                                    refreshGap = getString(R.string.option_30_sec);
                                    break;

                                case R.id.interval_1_min:
                                    elapsedTime = 60000;
                                    refreshGap = getString(R.string.option_1_min);
                                    break;

                                case R.id.interval_2_min:
                                    elapsedTime = 120000;
                                    refreshGap = getString(R.string.option_2_min);
                                    break;

                                case R.id.interval_5_min:
                                    elapsedTime = 300000;
                                    refreshGap = getString(R.string.option_5_min);
                                    break;

                                case R.id.interval_10_min:
                                    elapsedTime = 600000;
                                    refreshGap = getString(R.string.option_10_min);
                                    break;

                                case R.id.interval_15_min:
                                    elapsedTime = 900000;
                                    refreshGap = getString(R.string.option_15_min);
                                    break;
                            }
                            PreferenceManager.getDefaultSharedPreferences(getContext()).edit().putInt(NOTIFICATION_REFRESH_INTERVAL,
                                    elapsedTime).apply();
                            PreferenceManager.getDefaultSharedPreferences(getContext()).edit().putString(NOTIFICATION_REFRESH_INTERVAL_SUMMARY,
                                    refreshGap).apply();
                            snackbar = Snackbar.make(getActivity().findViewById(R.id.main_root),
                                            getString(R.string.label_notification_refresh_interval_change, refreshGap), Snackbar.LENGTH_SHORT)
                                    .setAnchorView(getActivity().findViewById(R.id.bottomNavigationView));
                            dialog.dismiss();
                            mNotificationRefreshInterval.setSummary(refreshGap);
                            dismissOnClick(snackbar);
                            snackbar.show();
                            Intent i = new Intent(getContext(), NotificationUpdater.class);
                            if (isCombinedNotificationEnabled) {
//                                getContext().stopService(new Intent(getContext(), CompoundNotification.class));
//                                getContext().startService(new Intent(getContext(), CompoundNotification.class));
                            }
                            else {
                                getContext().sendBroadcast(i);
                            }
                        }
                    });

                    dialog.setContentView(dialogView);
                    dialog.setOnShowListener(new DialogInterface.OnShowListener() {
                        @Override
                        public void onShow(DialogInterface dialogInterface) {
                            BottomSheetDialog bottomSheetDialog = (BottomSheetDialog) dialogInterface;
                            FrameLayout bottomSheet = bottomSheetDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
                            BottomSheetBehavior.from(bottomSheet).setState(BottomSheetBehavior.STATE_EXPANDED);
                        }
                    });
                    dialog.show();
                    return false;
                }
            });

            mRemainingDataInfo.setOnPreferenceClickListener(new androidx.preference.Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(androidx.preference.Preference preference) {
                    Boolean isChecked = PreferenceManager.getDefaultSharedPreferences(getContext())
                            .getBoolean("remaining_data_info", true);
                    if (PreferenceManager.getDefaultSharedPreferences(getContext()).getFloat(DATA_LIMIT, -1) > 0) {
//                    Log.d(TAG, "onPreferenceClick: " + PreferenceManager.getDefaultSharedPreferences(getContext()).getInt("data_limit", -1) );
                        if (isChecked) {
                            snackbar = Snackbar.make(getActivity().findViewById(R.id.main_root),
                                    getString(R.string.remaining_data_info_enabled), Snackbar.LENGTH_SHORT)
                                    .setAnchorView(getActivity().findViewById(R.id.bottomNavigationView));
                        } else {
                            snackbar = Snackbar.make(getActivity().findViewById(R.id.main_root),
                                    getString(R.string.remaining_data_info_disabled), Snackbar.LENGTH_SHORT)
                                    .setAnchorView(getActivity().findViewById(R.id.bottomNavigationView));
                        }
                        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(getContext());
                        int[] ids = appWidgetManager.getAppWidgetIds(new ComponentName(getContext(), DataUsageWidget.class));
                        Intent intent = new Intent(getContext(), DataUsageWidget.class);
                        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
                        getContext().sendBroadcast(intent);
                    } else {
                        mRemainingDataInfo.setChecked(false);
                        snackbar = Snackbar.make(getActivity().findViewById(R.id.main_root),
                                getString(R.string.label_monitoring_no_plan), Snackbar.LENGTH_SHORT)
                                .setAnchorView(getActivity().findViewById(R.id.bottomNavigationView));
                    }
                    dismissOnClick(snackbar);
                    snackbar.show();
                    return false;
                }
            });

            mShowWifiWidget.setOnPreferenceClickListener(new androidx.preference.Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(androidx.preference.Preference preference) {
                    AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(getContext());
                    int[] ids = appWidgetManager.getAppWidgetIds(new ComponentName(getContext(), DataUsageWidget.class));
                    Intent intent = new Intent(getContext(), DataUsageWidget.class);
                    intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                    intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
                    getContext().sendBroadcast(intent);
                    return false;
                }
            });

            mWidgetRefresh.setOnPreferenceClickListener(new androidx.preference.Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(androidx.preference.Preference preference) {
                    snackbar = Snackbar.make(getActivity().findViewById(R.id.main_root),
                            getString(R.string.label_widget_refreshed), Snackbar.LENGTH_SHORT)
                            .setAnchorView(getActivity().findViewById(R.id.bottomNavigationView));
                    AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(getContext());
                    int[] ids = appWidgetManager.getAppWidgetIds(new ComponentName(getContext(), DataUsageWidget.class));
                    Intent intent = new Intent(getContext(), DataUsageWidget.class);
                    intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                    intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
                    getContext().sendBroadcast(intent);
                    snackbar.show();
                    return false;
                }
            });

            mShowMobileData.setOnPreferenceClickListener(new androidx.preference.Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(androidx.preference.Preference preference) {
                    if (PreferenceManager.getDefaultSharedPreferences(getContext())
                            .getBoolean("setup_notification", false)) {
                        Boolean isChecked = PreferenceManager.getDefaultSharedPreferences(getContext())
                                .getBoolean("show_mobile_data_notification", false);
                        if (isChecked) {
                            PreferenceManager.getDefaultSharedPreferences(getContext()).edit()
                                    .putBoolean(Values.NOTIFICATION_MOBILE_DATA, true).apply();
                        } else {
                            if (!PreferenceManager.getDefaultSharedPreferences(getContext())
                                    .getBoolean("show_wifi_notification", true)) {
                                mShowMobileData.setChecked(true);
                                snackbar = Snackbar.make(getActivity().findViewById(R.id.main_root),
                                        getString(R.string.label_notification_cannot_disable_both), Snackbar.LENGTH_SHORT)
                                        .setAnchorView(getActivity().findViewById(R.id.bottomNavigationView));
                                dismissOnClick(snackbar);
                                snackbar.show();
                            } else {
                                PreferenceManager.getDefaultSharedPreferences(getContext()).edit()
                                        .putBoolean(Values.NOTIFICATION_MOBILE_DATA, false).apply();
                            }
                        }
                        Intent i = new Intent(getContext(), NotificationUpdater.class);
                        getContext().sendBroadcast(i);
                    } else {
                        mShowMobileData.setChecked(true);
                        snackbar = Snackbar.make(getActivity().findViewById(R.id.main_root),
                                getString(R.string.label_setup_notification_first), Snackbar.LENGTH_SHORT)
                                .setAnchorView(getActivity().findViewById(R.id.bottomNavigationView));
                        dismissOnClick(snackbar);
                        snackbar.show();
                    }
                    return false;
                }
            });

            mShowWifi.setOnPreferenceClickListener(new androidx.preference.Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(androidx.preference.Preference preference) {
                    if (PreferenceManager.getDefaultSharedPreferences(getContext())
                            .getBoolean("setup_notification", false)) {
                        Boolean isChecked = PreferenceManager.getDefaultSharedPreferences(getContext())
                                .getBoolean("show_wifi_notification", false);
                        if (isChecked) {
                            PreferenceManager.getDefaultSharedPreferences(getContext()).edit()
                                    .putBoolean(NOTIFICATION_WIFI, true).apply();
                        } else {
                            if (!PreferenceManager.getDefaultSharedPreferences(getContext())
                                    .getBoolean("show_mobile_data_notification", true)) {
                                mShowWifi.setChecked(true);
                                snackbar = Snackbar.make(getActivity().findViewById(R.id.main_root),
                                        getString(R.string.label_notification_cannot_disable_both), Snackbar.LENGTH_SHORT)
                                        .setAnchorView(getActivity().findViewById(R.id.bottomNavigationView));
                                dismissOnClick(snackbar);
                                snackbar.show();
                            } else {
                                PreferenceManager.getDefaultSharedPreferences(getContext()).edit()
                                        .putBoolean(NOTIFICATION_WIFI, false).apply();
                            }
                        }
                        Intent i = new Intent(getContext(), NotificationUpdater.class);
                        getContext().sendBroadcast(i);
                    } else {
                        mShowWifi.setChecked(true);
                        snackbar = Snackbar.make(getActivity().findViewById(R.id.main_root),
                                getString(R.string.label_setup_notification_first), Snackbar.LENGTH_SHORT)
                                .setAnchorView(getActivity().findViewById(R.id.bottomNavigationView));
                        dismissOnClick(snackbar);
                        snackbar.show();
                    }
                    return false;
                }
            });

            mAlwaysShowTotal.setOnPreferenceClickListener(new androidx.preference.Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(@NonNull androidx.preference.Preference preference) {
                    Intent i = new Intent(getContext(), NotificationUpdater.class);
                    getContext().sendBroadcast(i);
                    return false;
                }
            });

            mAutoUpdateDataPlan.setOnPreferenceClickListener(new androidx.preference.Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(@NonNull androidx.preference.Preference preference) {
                    boolean isChecked = PreferenceManager.getDefaultSharedPreferences(requireContext())
                            .getBoolean("auto_update_data_plan", false);
                    cancelDataPlanNotification(requireContext());
                    if (isChecked) {
                        setRefreshAlarm(requireContext());
                    }
                    else {
                        setDataPlanNotification(requireContext());
                    }
                    return false;
                }
            });

            mAutoHideNetworkSpeed.setOnPreferenceClickListener(new androidx.preference.Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(androidx.preference.Preference preference) {
                    boolean isCombinedNotificationEnabled = PreferenceManager.getDefaultSharedPreferences(getContext())
                            .getBoolean("combine_notifications", false);
                    if (!isCombinedNotificationEnabled) {
                        boolean isChecked = PreferenceManager.getDefaultSharedPreferences(getContext())
                                .getBoolean("auto_hide_network_speed", false);
                        NotificationManagerCompat managerCompat = NotificationManagerCompat.from(getContext());
                        if (!isChecked) {
                            boolean isNetworkSpeedEnabled = PreferenceManager.getDefaultSharedPreferences(getContext())
                                    .getBoolean("network_signal_notification", false);
                            if (isNetworkSpeedEnabled) {
                                Intent activityIntent = new Intent(Intent.ACTION_MAIN);
                                activityIntent.addCategory(Intent.CATEGORY_LAUNCHER);
                                activityIntent.setComponent(new ComponentName(getContext().getPackageName(),
                                        MainActivity.class.getName()));
                                PendingIntent activityPendingIntent = PendingIntent.getActivity(
                                        getContext(), 0, activityIntent, PendingIntent.FLAG_IMMUTABLE);
                                NotificationCompat.Builder builder = new NotificationCompat.Builder(getContext(),
                                        NETWORK_SIGNAL_CHANNEL_ID);
                                builder.setSmallIcon(R.drawable.ic_signal_kb_0);
                                builder.setOngoing(true);
                                builder.setPriority(NotificationCompat.PRIORITY_DEFAULT);
                                builder.setContentTitle(getString(R.string.network_speed_title, "0 KB/s"));
                                builder.setStyle(new NotificationCompat.InboxStyle()
                                        .addLine(getString(R.string.network_speed_download, "0 KB/s"))
                                        .addLine(getString(R.string.network_speed_upload, "0 KB/s")));
                                builder.setShowWhen(false);
                                builder.setVisibility(NotificationCompat.VISIBILITY_SECRET);
                                builder.setContentIntent(activityPendingIntent);
                                builder.setAutoCancel(false);
                                builder.setGroup(NETWORK_SIGNAL_NOTIFICATION_GROUP);
//                                managerCompat.notify(NETWORK_SIGNAL_NOTIFICATION_ID, builder.build());
                                postNotification(getContext(), managerCompat, builder, NETWORK_SIGNAL_NOTIFICATION_ID);
                            }
                        }
                        else {
                            managerCompat.cancel(NETWORK_SIGNAL_NOTIFICATION_ID);
                        }
                    }
                    return false;
                }
            });

            mCombineNotifications.setOnPreferenceClickListener(new androidx.preference.Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(@NonNull androidx.preference.Preference preference) {
                    boolean isDataMonitorNotificationEnabled = PreferenceManager.getDefaultSharedPreferences(getContext())
                            .getBoolean("setup_notification", false);
                    boolean isNetworkSpeedNotificationEnabled = PreferenceManager.getDefaultSharedPreferences(getContext())
                            .getBoolean("network_signal_notification", false);
                    if (isDataMonitorNotificationEnabled && isNetworkSpeedNotificationEnabled) {
                        boolean isChecked = PreferenceManager.getDefaultSharedPreferences(getContext())
                                .getBoolean("combine_notifications", false);
                        if (isChecked) {
                            mCombinedNotificationIcon.setVisible(true);
                            // Stop both services and start combined one
                            getContext().stopService(new Intent(getContext(), LiveNetworkMonitor.class));
                            getContext().stopService(new Intent(getContext(), NotificationService.class));

                            getContext().startService(new Intent(getContext(), CompoundNotification.class));
                        }
                        else {
                            mCombinedNotificationIcon.setVisible(false);
                            // Stop compound service and start individual ones
                            getContext().stopService(new Intent(getContext(), CompoundNotification.class));

                            getContext().startService(new Intent(getContext(), LiveNetworkMonitor.class));
                            getContext().startService(new Intent(getContext(), NotificationService.class));

                            // Set notification refresh interval back to 1 min if changed
                            int elapsedTime = PreferenceManager.getDefaultSharedPreferences(getContext())
                                    .getInt(NOTIFICATION_REFRESH_INTERVAL, 60000);
                            String refreshGap = PreferenceManager.getDefaultSharedPreferences(getContext())
                                    .getString(NOTIFICATION_REFRESH_INTERVAL_SUMMARY, getContext().getString(R.string.option_1_min));
                            if (elapsedTime < 60000) {
                                // less than 1 min
                                elapsedTime = 60000;
                                refreshGap = getContext().getString(R.string.option_1_min);
                            }
                            mNotificationRefreshInterval.setSummary(refreshGap);
                            PreferenceManager.getDefaultSharedPreferences(getContext()).edit().putInt(NOTIFICATION_REFRESH_INTERVAL,
                                    elapsedTime).apply();
                            PreferenceManager.getDefaultSharedPreferences(getContext()).edit().putString(NOTIFICATION_REFRESH_INTERVAL_SUMMARY,
                                    refreshGap).apply();

                        }
                    }
                    else {
                        mCombineNotifications.setChecked(false);
                        snackbar = Snackbar.make(getActivity().findViewById(R.id.main_root),
                                        getString(R.string.label_setup_notification_first), Snackbar.LENGTH_SHORT)
                                .setAnchorView(getActivity().findViewById(R.id.bottomNavigationView));
                        dismissOnClick(snackbar);
                        snackbar.show();
                    }
                    return false;
                }
            });

            mCombinedNotificationIcon.setOnPreferenceClickListener(new androidx.preference.Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(@NonNull androidx.preference.Preference preference) {
                    BottomSheetDialog dialog = new BottomSheetDialog(getContext(), R.style.BottomSheet);
                    View dialogView = LayoutInflater.from(getContext())
                            .inflate(R.layout.layout_combined_notification_icon, null);
                    RadioGroup iconGroup = dialogView.findViewById(R.id.combined_notification_icon_group);
                    ConstraintLayout footer = dialogView.findViewById(R.id.footer);
                    TextView cancel = footer.findViewById(R.id.cancel);
                    TextView ok = footer.findViewById(R.id.ok);
                    dialog.setContentView(dialogView);

                    String checkIcon = PreferenceManager.getDefaultSharedPreferences(getContext())
                                    .getString("combined_notification_icon", ICON_NETWORK_SPEED);
                    if (checkIcon.equals(ICON_NETWORK_SPEED)) {
                        iconGroup.check(R.id.icon_network_speed);
                    }
                    else {
                        iconGroup.check(R.id.icon_data_usage_percent);
                    }

                    ok.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            int selectedIcon = iconGroup.getCheckedRadioButtonId();
                            String icon = "";
                            switch (selectedIcon) {
                                case R.id.icon_data_usage_percent:
                                    icon = ICON_DATA_USAGE;
                                    break;

                                case R.id.icon_network_speed:
                                    icon = ICON_NETWORK_SPEED;
                                    break;
                            }

                            PreferenceManager.getDefaultSharedPreferences(getContext()).edit()
                                    .putString("combined_notification_icon", icon)
                                    .apply();
                            dialog.dismiss();
                        }
                    });

                    cancel.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            dialog.dismiss();
                        }
                    });

                    dialog.show();
                    return false;
                }
            });

            mAddDataPlan.setOnPreferenceClickListener(new androidx.preference.Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(androidx.preference.Preference preference) {
                    Intent intent = new Intent(getActivity(), ContainerActivity.class);
                    intent.putExtra(GENERAL_FRAGMENT_ID, DATA_PLAN_FRAGMENT);
                    dataPlanLauncher.launch(intent);

                    return false;
                }
            });

            mUsageResetTime.setOnPreferenceClickListener(new androidx.preference.Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(androidx.preference.Preference preference) {
                    int hour, minute, year, month, dayOfMonth;
                    hour = PreferenceManager.getDefaultSharedPreferences(getContext())
                            .getInt(DATA_RESET_HOUR, 0);
                    minute = PreferenceManager.getDefaultSharedPreferences(getContext())
                            .getInt(DATA_RESET_MIN, 0);

                    if (PreferenceManager.getDefaultSharedPreferences(getContext()).getString(DATA_RESET, "null")
                            .equals(DATA_RESET_MONTHLY)) {
                        BottomSheetDialog dialog = new BottomSheetDialog(getContext(), R.style.BottomSheet);
                        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.layout_date_picker, null);

                        DatePicker datePicker = (DatePicker) dialogView.findViewById(R.id.reset_date_picker);
                        Calendar calendar = Calendar.getInstance();
                        datePicker.init(CALENDAR_YEAR, 0, 1, null);
                        ConstraintLayout footer = dialogView.findViewById(R.id.footer);
                        TextView cancel = footer.findViewById(R.id.cancel);
                        TextView ok = footer.findViewById(R.id.ok);

                        (((LinearLayout) ((LinearLayout) datePicker.getChildAt(0)).getChildAt(0)).getChildAt(0)).setVerticalScrollBarEnabled(false);
                        (((LinearLayout) ((LinearLayout) datePicker.getChildAt(0)).getChildAt(0)).getChildAt(2)).setVerticalScrollBarEnabled(false);

                        int resetDate = PreferenceManager.getDefaultSharedPreferences(getContext())
                                .getInt(DATA_RESET_DATE, -1);

                        if (resetDate > 0) {
                            datePicker.updateDate(datePicker.getYear(), datePicker.getMonth(), resetDate);
                        }

                        datePicker.setOnDateChangedListener(new android.widget.DatePicker.OnDateChangedListener() {
                            @Override
                            public void onDateChanged(android.widget.DatePicker datePicker, int i, int i1, int i2) {
                                if (!PreferenceManager.getDefaultSharedPreferences(getContext())
                                        .getBoolean("disable_haptics", false)) {
                                    VibrationUtils.hapticMinor(getContext());
                                }
                                datePicker.updateDate(CALENDAR_YEAR, 0, datePicker.getDayOfMonth());
                            }
                        });

                        int yearField = getContext().getResources().getIdentifier("android:id/year", null, null);
                        int monthField = getContext().getResources().getIdentifier("android:id/month", null, null);
                        if(yearField != 0){
                            View yearPicker = datePicker.findViewById(yearField);
                            if(yearPicker != null){
                                yearPicker.setVisibility(View.GONE);
                            }
                        }
                        if(monthField != 0){
                            View monthPicker = datePicker.findViewById(monthField);
                            if(monthPicker != null){
                                monthPicker.setVisibility(View.GONE);
                            }
                        }

                        cancel.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                dialog.dismiss();
                            }
                        });

                        ok.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                PreferenceManager.getDefaultSharedPreferences(getContext()).edit()
                                        .putInt(DATA_RESET_DATE, datePicker.getDayOfMonth()).apply();

                                Intent i = new Intent(getContext(), NotificationUpdater.class);
                                getContext().sendBroadcast(i);
                                AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(getContext());
                                int[] ids = appWidgetManager.getAppWidgetIds(new ComponentName(getContext(), DataUsageWidget.class));
                                Intent intent = new Intent(getContext(), DataUsageWidget.class);
                                intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
                                getContext().sendBroadcast(intent);

                                int date = datePicker.getDayOfMonth();
                                String ordinal = formatOrdinalNumber(date, requireContext());

                                mUsageResetTime.setSummary(getContext().getString(R.string.label_reset_every_month,
                                        ordinal));

                                dialog.dismiss();

                                if (PreferenceManager.getDefaultSharedPreferences(requireContext())
                                        .getBoolean("data_usage_alert", false)) {
                                    DataUsageMonitor.updateServiceRestart(requireContext());
                                }

                                snackbar = Snackbar.make(getActivity().findViewById(R.id.main_root),
                                        getString(R.string.label_data_usage_reset_date_change, ordinal),
                                        Snackbar.LENGTH_SHORT)
                                        .setAnchorView(getActivity().findViewById(R.id.bottomNavigationView));
                                dismissOnClick(snackbar);
                                snackbar.show();
                                updateDailyQuota();
                            }
                        });

                        dialog.setContentView(dialogView);
                        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
                            @Override
                            public void onShow(DialogInterface dialogInterface) {
                                BottomSheetDialog bottomSheetDialog = (BottomSheetDialog) dialogInterface;
                                FrameLayout bottomSheet = bottomSheetDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
                                BottomSheetBehavior.from(bottomSheet).setState(BottomSheetBehavior.STATE_EXPANDED);
                            }
                        });
                        dialog.show();
                    }
                    else if (PreferenceManager.getDefaultSharedPreferences(getContext()).getString(DATA_RESET, "null")
                            .equals(DATA_RESET_DAILY)) {
                        BottomSheetDialog dialog = new BottomSheetDialog(getContext(), R.style.BottomSheet);
                        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.layout_time_picker, null);

                        TimePicker timePicker = (TimePicker) dialogView.findViewById(R.id.reset_time_picker);
                        ConstraintLayout footer = dialogView.findViewById(R.id.footer);
                        TextView cancel = footer.findViewById(R.id.cancel);
                        TextView ok = footer.findViewById(R.id.ok);

                        (((LinearLayout) ((LinearLayout) timePicker.getChildAt(0)).getChildAt(0)).getChildAt(0)).setVerticalScrollBarEnabled(false);
                        (((LinearLayout) ((LinearLayout) timePicker.getChildAt(0)).getChildAt(0)).getChildAt(2)).setVerticalScrollBarEnabled(false);

                        int resetHour = PreferenceManager.getDefaultSharedPreferences(getContext())
                                .getInt(DATA_RESET_HOUR, -1);
                        int resetMinute = PreferenceManager.getDefaultSharedPreferences(getContext())
                                .getInt(DATA_RESET_MIN, -1);

                        if (resetHour >= 0 && resetMinute >= 0) {
                            timePicker.setHour(resetHour);
                            timePicker.setMinute(resetMinute);
                        }

                        timePicker.setOnTimeChangedListener(new TimePicker.OnTimeChangedListener() {
                            @Override
                            public void onTimeChanged(TimePicker timePicker, int i, int i1) {
                                if (!PreferenceManager.getDefaultSharedPreferences(getContext())
                                        .getBoolean("disable_haptics", false)) {
                                    VibrationUtils.hapticMinor(getContext());
                                }
                            }
                        });

                        cancel.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                dialog.dismiss();
                            }
                        });

                        ok.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                PreferenceManager.getDefaultSharedPreferences(getContext()).edit()
                                        .putInt(DATA_RESET_HOUR, timePicker.getHour()).apply();
                                PreferenceManager.getDefaultSharedPreferences(getContext()).edit()
                                        .putInt(DATA_RESET_MIN, timePicker.getMinute()).apply();

                                Intent i = new Intent(getContext(), NotificationUpdater.class);
                                getContext().sendBroadcast(i);
                                AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(getContext());
                                int[] ids = appWidgetManager.getAppWidgetIds(new ComponentName(getContext(), DataUsageWidget.class));
                                Intent intent = new Intent(getContext(), DataUsageWidget.class);
                                intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
                                getContext().sendBroadcast(intent);

                                int h, m;
                                m = timePicker.getMinute();
                                String time, interval;

                                int hourOfDay = timePicker.getHour();

                                // Again :)

                                if (hourOfDay >= 12) {
                                    if (hourOfDay == 12) {
                                        h = 12;
                                    } else {
                                        h = (hourOfDay - 12);
                                    }
                                    if (m < 10) {
                                        time = h + ":0" + m + " pm";
                                    } else {
                                        time = h + ":" + m + " pm";
                                    }
                                } else {
                                    if (hourOfDay == 0) {
                                        h = 12;
                                    } else if (hourOfDay < 10) {
                                        h = hourOfDay;
                                        if (m < 10) {
                                            time = "0" + h + ":0" + m + " pm";
                                        } else {
                                            time = "0" + h + ":" + m + " pm";
                                        }
                                    } else {
                                        h = hourOfDay;
                                    }
                                    if (h < 10) {
                                        time = "0" + h + ":" + m + " am";
                                    } else {
                                        time = h + ":" + m + " am";
                                    }
                                    if (m < 10) {
                                        time = h + ":0" + m + " am";
                                    } else {
                                        time = h + ":" + m + " am";
                                    }
                                }
                                if (PreferenceManager.getDefaultSharedPreferences(getContext())
                                        .getString(DATA_RESET, "").equals(DATA_RESET_MONTHLY)) {
                                    interval = getString(R.string.month);
                                } else {
                                    interval = getString(R.string.day);
                                }

                                mUsageResetTime.setSummary(time);

                                dialog.dismiss();

                                if (PreferenceManager.getDefaultSharedPreferences(requireContext())
                                        .getBoolean("data_usage_alert", false)) {
                                    DataUsageMonitor.updateServiceRestart(requireContext());
                                }

                                snackbar = Snackbar.make(getActivity().findViewById(R.id.main_root),
                                        getString(R.string.label_data_usage_reset_time_change, interval, time), Snackbar.LENGTH_SHORT)
                                        .setAnchorView(getActivity().findViewById(R.id.bottomNavigationView));
                                dismissOnClick(snackbar);
                                snackbar.show();
                                updateDailyQuota();

                            }
                        });

                        dialog.setContentView(dialogView);
                        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
                            @Override
                            public void onShow(DialogInterface dialogInterface) {
                                BottomSheetDialog bottomSheetDialog = (BottomSheetDialog) dialogInterface;
                                FrameLayout bottomSheet = bottomSheetDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
                                BottomSheetBehavior.from(bottomSheet).setState(BottomSheetBehavior.STATE_EXPANDED);
                            }
                        });
                        dialog.show();
                    }
                    else if (PreferenceManager.getDefaultSharedPreferences(getContext()).getString(DATA_RESET, "null")
                            .equals(DATA_RESET_CUSTOM)) {
                        snackbar = Snackbar.make(getActivity().findViewById(R.id.main_root),
                                getString(R.string.setup_usage_reset_date_custom_selected), Snackbar.LENGTH_SHORT)
                                .setAnchorView(getActivity().findViewById(R.id.bottomNavigationView));
                        dismissOnClick(snackbar);
                        snackbar.show();
                    }
                    else {
                        snackbar = Snackbar.make(getActivity().findViewById(R.id.main_root),
                                getString(R.string.label_add_data_plan_first), Snackbar.LENGTH_SHORT)
                                .setAnchorView(getActivity().findViewById(R.id.bottomNavigationView));
                        dismissOnClick(snackbar);
                        snackbar.show();
                    }
                    return false;
                }
            });


            mShowDataWarning.setOnPreferenceClickListener(new androidx.preference.Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(androidx.preference.Preference preference) {
                    AlarmManager alarmManager = (AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                        showAlarmPermissionDeniedDialog(requireContext());
                        mShowDataWarning.setChecked(false);
                    }
                    else {
                        Boolean isChecked = PreferenceManager.getDefaultSharedPreferences(getContext())
                                .getBoolean("data_usage_alert", false);
                        Float dataLimit = PreferenceManager.getDefaultSharedPreferences(getContext())
                                .getFloat(DATA_LIMIT, -1);
                        if (dataLimit < 0) {
                            mShowDataWarning.setChecked(false);
                            snackbar = Snackbar.make(getActivity().findViewById(R.id.main_root),
                                            getString(R.string.label_add_data_plan_first), Snackbar.LENGTH_SHORT)
                                    .setAnchorView(getActivity().findViewById(R.id.bottomNavigationView));
                            snackbar.show();
                        }
                        else {
                            if (isChecked) {
                                getContext().startService(new Intent(getContext(), DataUsageMonitor.class));
                            } else {
                                getContext().stopService(new Intent(getContext(), DataUsageMonitor.class));
                            }
                        }
                    }
                    return false;
                }
            });

            mDataWarningTrigger.setOnPreferenceClickListener(new androidx.preference.Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(androidx.preference.Preference preference) {
                    BottomSheetDialog dialog = new BottomSheetDialog(getContext(), R.style.BottomSheet);
                    View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.layout_data_warning_trigger, null);

                    Slider slider = dialogView.findViewById(R.id.slider);
                    TextView currentLevel = dialogView.findViewById(R.id.current_trigger_level);
                    ConstraintLayout footer = dialogView.findViewById(R.id.footer);
                    TextView cancel = footer.findViewById(R.id.cancel);
                    TextView ok = footer.findViewById(R.id.ok);

                    int value = PreferenceManager.getDefaultSharedPreferences(getContext()).getInt(DATA_WARNING_TRIGGER_LEVEL, 85);
                    String level = String.valueOf(value);

                    final int dataType = PreferenceManager.getDefaultSharedPreferences(getContext())
                            .getInt(DATA_TYPE, 0);
                    final Float dataLimit = PreferenceManager.getDefaultSharedPreferences(getContext())
                            .getFloat(DATA_LIMIT, -1);

                    String triggerDataLevelValue = null;
                    Double triggerDataLevel = dataLimit.doubleValue() * value / 100;
                    if (dataType == 1) {
                        triggerDataLevel = triggerDataLevel / 1024;
                        triggerDataLevelValue = String.format("%.2f", triggerDataLevel.floatValue()) + " GB";
                    } else {
                        triggerDataLevelValue = String.format("%.2f", triggerDataLevel.floatValue()) + " MB";
                    }

                    if (PreferenceManager.getDefaultSharedPreferences(getContext())
                            .getFloat(DATA_LIMIT, -1) > 0) {
                        currentLevel.setText(getContext().getString(R.string.label_current_data_warning_trigger_level_limit_set,
                                level, triggerDataLevelValue));
                    }
                    else {
                        currentLevel.setText(getContext().getString(R.string.label_current_data_warning_trigger_level_limit_not_set,
                                level));
                    }


//                    Log.d(TAG, "onPreferenceClick: " + triggerDataLevel );

                    slider.setValueFrom(0);
                    slider.setValueTo(50);
                    slider.setValue(value - 50);
                    slider.setStepSize(1);
                    slider.setLabelFormatter(new LabelFormatter() {
                        @NonNull
                        @NotNull
                        @Override
                        public String getFormattedValue(float value) {
                            Float newValue = value + 50; // To ensure value is correct as slider slides
                            String rawValue = newValue.toString();
                            String output;
                            if (rawValue.contains(",")) {
                                output = rawValue.replace(",0", "");
                            }
                            else if (rawValue.contains("٫")) {
                                output = rawValue.replace("٫0", "");
                            }
                            else {
                                output = rawValue.replace(".0", "");
                            }
                            int sliderValue = Integer.parseInt(output);
                            String triggerDataLevelValue = null;
                            Double triggerDataLevel = dataLimit.doubleValue() * sliderValue / 100;
                            if (dataType == 1) {
                                triggerDataLevel = triggerDataLevel / 1024;
                                triggerDataLevelValue = String.format("%.2f", triggerDataLevel.floatValue()) + " GB";
                            } else {
                                triggerDataLevelValue = String.format("%.2f", triggerDataLevel.floatValue()) + " MB";
                            }

                            if (PreferenceManager.getDefaultSharedPreferences(getContext())
                                    .getFloat(DATA_LIMIT, -1) > 0) {
                                currentLevel.setText(getContext().getString(R.string.label_current_data_warning_trigger_level_limit_set,
                                        output, triggerDataLevelValue));
                            }
                            else {
                                currentLevel.setText(getContext().getString(R.string.label_current_data_warning_trigger_level_limit_not_set,
                                        output));
                            }
                            return output;
                        }
                    });

                    slider.addOnChangeListener(new Slider.OnChangeListener() {
                        @Override
                        public void onValueChange(@NonNull @NotNull Slider slider, float value, boolean fromUser) {
                            if (fromUser && !PreferenceManager.getDefaultSharedPreferences(getContext())
                                    .getBoolean("disable_haptics", false)) {
                                VibrationUtils.hapticMinor(getContext());
                            }
                        }
                    });

                    cancel.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            dialog.dismiss();
                        }
                    });

                    ok.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Float value = slider.getValue() + 50;
                            String rawValue = value.toString();
                            String output;
                            if (rawValue.contains(",")) {
                                output = rawValue.replace(",0", "");
                            }
                            else if (rawValue.contains("٫")) {
                                output = rawValue.replace("٫0", "");
                            }
                            else {
                                output = rawValue.replace(".0", "");
                            }
                            PreferenceManager.getDefaultSharedPreferences(getContext()).edit().putInt(DATA_WARNING_TRIGGER_LEVEL,
                                    Integer.parseInt(output)).apply();
                            mDataWarningTrigger.setSummary(getContext().getString(R.string.label_data_trigger_level, output));
                            PreferenceManager.getDefaultSharedPreferences(getContext()).edit().putBoolean(DATA_USAGE_WARNING_SHOWN, false).apply();
                            if (PreferenceManager.getDefaultSharedPreferences(getContext()).getBoolean(DATA_USAGE_ALERT, false)) {
                                try {
                                    int trigger = PreferenceManager.getDefaultSharedPreferences(getContext()).getInt(DATA_WARNING_TRIGGER_LEVEL, 85);

                                    String totalUsage = formatData(getDeviceMobileDataUsage(getContext(), SESSION_TODAY, 1)[0],
                                            getDeviceMobileDataUsage(getContext(), SESSION_TODAY, 1)[1])[2]
                                            .replace(" MB", "").replace(" GB", "");
                                    if (totalUsage.contains(",")) {
                                        totalUsage = totalUsage.replace(",", ".");
                                    }
                                    else if (totalUsage.contains("٫")) {
                                        totalUsage = totalUsage.replace("٫", ".");
                                    }

                                    totalUsage = Common.parseNumber(totalUsage);

                                    if (PreferenceManager.getDefaultSharedPreferences(getContext()).getFloat(DATA_LIMIT, -1) * trigger / 100 >
                                            Double.parseDouble(totalUsage)) {
                                        resumeMonitor();
                                    }
                                }
                                catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                            dialog.dismiss();
                        }
                    });

                    dialog.setContentView(dialogView);
                    dialog.setOnShowListener(new DialogInterface.OnShowListener() {
                        @Override
                        public void onShow(DialogInterface dialogInterface) {
                            BottomSheetDialog bottomSheetDialog = (BottomSheetDialog) dialogInterface;
                            FrameLayout bottomSheet = bottomSheetDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
                            BottomSheetBehavior.from(bottomSheet).setState(BottomSheetBehavior.STATE_EXPANDED);
                        }
                    });
                    dialog.show();
                    return false;
                }
            });

            mAppDataLimit.setOnPreferenceClickListener(new androidx.preference.Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(androidx.preference.Preference preference) {
                    startActivity(new Intent(getContext(), ContainerActivity.class)
                            .putExtra(GENERAL_FRAGMENT_ID, APP_DATA_LIMIT_FRAGMENT));
                    return false;
                }
            });

            mExcludeApps.setOnPreferenceClickListener(new androidx.preference.Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(@NonNull androidx.preference.Preference preference) {
                    startActivity(new Intent(getActivity(), ContainerActivity.class)
                            .putExtra(GENERAL_FRAGMENT_ID, EXCLUDE_APPS_FRAGMENT));
                    return false;
                }
            });

            mSmartDataAllocation.setOnPreferenceClickListener(new androidx.preference.Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(@NonNull androidx.preference.Preference preference) {
                    boolean isChecked = PreferenceManager.getDefaultSharedPreferences(getContext())
                            .getBoolean("smart_data_allocation", false);
                    Float dataLimit = PreferenceManager.getDefaultSharedPreferences(getContext())
                            .getFloat(DATA_LIMIT, -1);
                    WorkManager workManager = WorkManager.getInstance(requireContext());
                    if (dataLimit < 0) {
                        mSmartDataAllocation.setChecked(false);
                        snackbar = Snackbar.make(getActivity().findViewById(R.id.main_root),
                                        getString(R.string.label_add_data_plan_first), Snackbar.LENGTH_SHORT)
                                .setAnchorView(getActivity().findViewById(R.id.bottomNavigationView));
                        snackbar.show();
                    }
                    else {
                        if (isChecked) {
                            OneTimeWorkRequest smartDataAllocationWorkRequest = new OneTimeWorkRequest
                                    .Builder(SmartDataAllocationService.class)
                                    .build();

                            workManager.enqueueUniqueWork(
                                    "smart_data_allocation",
                                    ExistingWorkPolicy.KEEP,
                                    smartDataAllocationWorkRequest
                            );
                        }
                        else {
                            workManager.cancelUniqueWork("smart_data_allocation");
                            workManager.cancelUniqueWork("data_rollover");
                            workManager.cancelUniqueWork("quota_reset");
                            mDailyQuotaAlert.setChecked(false);

                        }
                        refreshQuotaAlertVisibility();
                    }
                    return false;
                }
            });

            mDailyQuotaAlert.setOnPreferenceClickListener(new androidx.preference.Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(@NonNull androidx.preference.Preference preference) {
                    boolean isChecked = PreferenceManager.getDefaultSharedPreferences(getContext())
                            .getBoolean("daily_quota_alert", false);
                    Float dataLimit = PreferenceManager.getDefaultSharedPreferences(getContext())
                            .getFloat(DATA_LIMIT, -1);
                    if (dataLimit < 0) {
                        mDailyQuotaAlert.setChecked(false);
                        snackbar = Snackbar.make(getActivity().findViewById(R.id.main_root),
                                        getString(R.string.label_add_data_plan_first), Snackbar.LENGTH_SHORT)
                                .setAnchorView(getActivity().findViewById(R.id.bottomNavigationView));
                        snackbar.show();
                    }
                    else {
                        Intent intent = new Intent(getContext(), DailyQuotaAlertReceiver.class);
                        PendingIntent pendingIntent = PendingIntent.getBroadcast(getContext(), 1000, intent, PendingIntent.FLAG_IMMUTABLE);
                        AlarmManager alarmManager = (AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);
                        if (isChecked) {
                            alarmManager.setExactAndAllowWhileIdle(
                                    AlarmManager.RTC_WAKEUP,
                                    System.currentTimeMillis(),
                                    pendingIntent
                            );
                        }
                        else {
                            alarmManager.cancel(pendingIntent);
                        }
                    }
                    return false;
                }
            });

        }

        @Override
        public void onDestroyView() {
            super.onDestroyView();
            if (dataPlanLauncher != null) {
                dataPlanLauncher.unregister();
                dataPlanLauncher = null;
            }
        }

        private void refreshDataPlanSettingsVisibility() {
            if (PreferenceManager.getDefaultSharedPreferences(requireContext())
                    .getString(DATA_RESET, "null").equals(DATA_RESET_CUSTOM)) {
                mAutoUpdateDataPlan.setVisible(true);
                mUsageResetTime.setVisible(false);
            }
            else {
                mAutoUpdateDataPlan.setVisible(false);
                mUsageResetTime.setVisible(true);
            }
        }

        private void refreshQuotaAlertVisibility() {
            if (PreferenceManager.getDefaultSharedPreferences(requireContext())
                    .getString(DATA_RESET, "null").equals(DATA_RESET_MONTHLY) ||
                    PreferenceManager.getDefaultSharedPreferences(requireContext())
                            .getString(DATA_RESET, "null").equals(DATA_RESET_CUSTOM)) {
                if (PreferenceManager.getDefaultSharedPreferences(getContext())
                        .getBoolean("smart_data_allocation", false)) {
                    mDailyQuotaAlert.setVisible(true);
                }
                else {
                    mDailyQuotaAlert.setVisible(false);
                }
            }
            else {
                mDailyQuotaAlert.setVisible(false);
            }
        }

        public static void pauseMonitor() {
            try {
                mContext.stopService(new Intent(mContext, DataUsageMonitor.class));
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

        public static void resumeMonitor() {
            mContext.startService(new Intent(mContext, DataUsageMonitor.class));
        }

        private void updateResetData() {
            String resetTitle, resetSummary;
            if (PreferenceManager.getDefaultSharedPreferences(getContext()).getString(DATA_RESET, "null")
                    .equals(DATA_RESET_MONTHLY)) {
                resetTitle = getContext().getString(R.string.setup_usage_reset_date);
                int date = PreferenceManager.getDefaultSharedPreferences(getContext())
                        .getInt(DATA_RESET_DATE, 1);
                String ordinal = formatOrdinalNumber(date, requireContext());

                resetSummary = (getContext().getString(R.string.label_reset_every_month, ordinal));
            }
            else if (PreferenceManager.getDefaultSharedPreferences(getContext()).getString(DATA_RESET, "null")
                    .equals(DATA_RESET_CUSTOM)) {
                resetTitle = getContext().getString(R.string.setup_usage_reset_date);
                resetSummary = getContext().getString(R.string.setup_usage_reset_date_custom_selected);
            }
            else {
                resetTitle = getContext().getString(R.string.setup_usage_reset_time);
                int hour, minute;
                hour = PreferenceManager.getDefaultSharedPreferences(getContext())
                        .getInt(DATA_RESET_HOUR, 0);
                minute = PreferenceManager.getDefaultSharedPreferences(getContext())
                        .getInt(DATA_RESET_MIN, 0);

                int h, m;
                m = minute;


                // Conversion to 12h clock xD :)

                if (hour >= 12) {
                    if (hour == 12) {
                        h = 12;
                    } else {
                        h = (hour - 12);
                    }
                    if (m < 10) {
                        resetSummary = h + ":0" + m + " pm";
                    } else {
                        resetSummary = h + ":" + m + " pm";
                    }
                } else {
                    if (hour == 0) {
                        h = 12;
                    } else if (hour < 10) {
                        h = hour;
                        if (m < 10) {
                            resetSummary = "0" + h + ":0" + m + " pm";
                        } else {
                            resetSummary = "0" + h + ":" + m + " pm";
                        }
                    } else {
                        h = hour;
                    }
                    if (h < 10) {
                        resetSummary = "0" + h + ":" + m + " am";
                    } else {
                        resetSummary = h + " : " + m + " am";
                    }
                    if (m < 10) {
                        resetSummary = h + ":0" + m + " am";
                    } else {
                        resetSummary = h + ":" + m + " am";
                    }
                }
            }

            mUsageResetTime.setTitle(resetTitle);
            mUsageResetTime.setSummary(resetSummary);
        }

        private void updateDailyQuota() {
            if (mSmartDataAllocation.isChecked()) {
                WorkManager workManager = WorkManager.getInstance(requireContext());
                workManager.cancelUniqueWork("smart_data_allocation");
                workManager.cancelUniqueWork("data_rollover");
                OneTimeWorkRequest smartDataAllocationWorkRequest = new OneTimeWorkRequest
                        .Builder(SmartDataAllocationService.class)
                        .build();

                workManager.enqueueUniqueWork(
                        "smart_data_allocation",
                        ExistingWorkPolicy.KEEP,
                        smartDataAllocationWorkRequest
                );
            }
        }
    }

}