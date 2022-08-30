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

import android.app.TimePickerDialog;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.os.RemoteException;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.TimePicker;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.drnoob.datamonitor.R;
import com.drnoob.datamonitor.Widget.DataUsageWidget;
import com.drnoob.datamonitor.core.Values;
import com.drnoob.datamonitor.core.base.DatePicker;
import com.drnoob.datamonitor.core.base.Preference;
import com.drnoob.datamonitor.core.base.SwitchPreferenceCompat;
import com.drnoob.datamonitor.ui.activities.ContainerActivity;
import com.drnoob.datamonitor.utils.DataUsageMonitor;
import com.drnoob.datamonitor.utils.LiveNetworkMonitor;
import com.drnoob.datamonitor.utils.NotificationService;
import com.drnoob.datamonitor.utils.NotificationService.NotificationUpdater;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.slider.LabelFormatter;
import com.google.android.material.slider.Slider;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;

import org.jetbrains.annotations.NotNull;

import java.sql.Time;
import java.text.ParseException;

import static com.drnoob.datamonitor.Common.dismissOnClick;
import static com.drnoob.datamonitor.core.Values.APP_DATA_LIMIT_FRAGMENT;
import static com.drnoob.datamonitor.core.Values.DATA_LIMIT;
import static com.drnoob.datamonitor.core.Values.DATA_RESET;
import static com.drnoob.datamonitor.core.Values.DATA_RESET_DAILY;
import static com.drnoob.datamonitor.core.Values.DATA_RESET_DATE;
import static com.drnoob.datamonitor.core.Values.DATA_RESET_HOUR;
import static com.drnoob.datamonitor.core.Values.DATA_RESET_MIN;
import static com.drnoob.datamonitor.core.Values.DATA_RESET_MONTHLY;
import static com.drnoob.datamonitor.core.Values.DATA_TYPE;
import static com.drnoob.datamonitor.core.Values.DATA_USAGE_ALERT;
import static com.drnoob.datamonitor.core.Values.DATA_USAGE_WARNING_SHOWN;
import static com.drnoob.datamonitor.core.Values.DATA_WARNING_TRIGGER_LEVEL;
import static com.drnoob.datamonitor.core.Values.GENERAL_FRAGMENT_ID;
import static com.drnoob.datamonitor.core.Values.LIMIT;
import static com.drnoob.datamonitor.core.Values.NOTIFICATION_REFRESH_INTERVAL;
import static com.drnoob.datamonitor.core.Values.NOTIFICATION_REFRESH_INTERVAL_SUMMARY;
import static com.drnoob.datamonitor.core.Values.NOTIFICATION_WIFI;
import static com.drnoob.datamonitor.core.Values.SESSION_MONTHLY;
import static com.drnoob.datamonitor.core.Values.SESSION_TODAY;
import static com.drnoob.datamonitor.core.Values.WIDGET_REFRESH_INTERVAL;
import static com.drnoob.datamonitor.core.Values.WIDGET_REFRESH_INTERVAL_SUMMARY;
import static com.drnoob.datamonitor.utils.NetworkStatsHelper.formatData;
import static com.drnoob.datamonitor.utils.NetworkStatsHelper.getDeviceMobileDataUsage;
import static com.drnoob.datamonitor.utils.NetworkStatsHelper.getTimePeriod;
import static com.drnoob.datamonitor.utils.VibrationUtils.hapticMajor;
import static com.drnoob.datamonitor.utils.VibrationUtils.hapticMinor;

public class SetupFragment extends Fragment {

    private static final String TAG = SetupFragment.class.getSimpleName();
    private static Context mContext;

    public SetupFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(@NonNull @NotNull Context context) {
        super.onAttach(context);
        mContext = context;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_setup, container, false);
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    public static class SetupPreference extends PreferenceFragmentCompat {
        private static final String TAG = SetupPreference.class.getSimpleName();
        private Preference mSetupWidget, mWidgetRefreshInterval, mNotificationRefreshInterval,
                mAddDataPlan, mUsageResetTime, mWidgetRefresh, mDataWarningTrigger, mAppDataLimit;
        private SwitchPreferenceCompat mSetupNotification, mRemainingDataInfo, mShowMobileData, mShowWifi,
                mShowDataWarning, mNetworkSignalNotification;
        private Snackbar snackbar;

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

            mSetupNotification = (SwitchPreferenceCompat) findPreference("setup_notification");
            mNetworkSignalNotification = (SwitchPreferenceCompat) findPreference("network_signal_notification");
            mRemainingDataInfo = (SwitchPreferenceCompat) findPreference("remaining_data_info");
            mShowMobileData = (SwitchPreferenceCompat) findPreference("show_mobile_data_notification");
            mShowWifi = (SwitchPreferenceCompat) findPreference("show_wifi_notification");
            mShowDataWarning = (SwitchPreferenceCompat) findPreference("data_usage_alert");


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

            mSetupNotification.setOnPreferenceClickListener(new androidx.preference.Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(androidx.preference.Preference preference) {
                    boolean isChecked = PreferenceManager.getDefaultSharedPreferences(getContext()).getBoolean("setup_notification", false);
                    if (isChecked) {
                        getContext().startService(new Intent(getContext(), NotificationService.class));
                        Log.d(TAG, "onPreferenceClick: Notification started");
                        snackbar = Snackbar.make(getActivity().findViewById(R.id.main_root),
                                getString(R.string.label_notification_enabled), Snackbar.LENGTH_SHORT)
                                .setAnchorView(getActivity().findViewById(R.id.bottomNavigationView));
                    } else {
                        getContext().stopService(new Intent(getContext(), NotificationService.class));
                        Log.d(TAG, "onPreferenceClick: Notification stopped");
                        snackbar = Snackbar.make(getActivity().findViewById(R.id.main_root),
                                getString(R.string.notification_disabled), Snackbar.LENGTH_SHORT)
                                .setAnchorView(getActivity().findViewById(R.id.bottomNavigationView));
                    }
                    dismissOnClick(snackbar);
                    snackbar.show();
                    return false;
                }
            });

            mNetworkSignalNotification.setOnPreferenceClickListener(new androidx.preference.Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(androidx.preference.Preference preference) {
                    boolean isChecked = PreferenceManager.getDefaultSharedPreferences(getContext()).getBoolean("network_signal_notification", false);
                    if (isChecked) {
                        getContext().startService(new Intent(getContext(), LiveNetworkMonitor.class));
                        Log.d(TAG, "onPreferenceClick: Notification started");
                        snackbar = Snackbar.make(getActivity().findViewById(R.id.main_root),
                                getString(R.string.label_network_signal_notification_enabled), Snackbar.LENGTH_SHORT)
                                .setAnchorView(getActivity().findViewById(R.id.bottomNavigationView));
                    } else {
                        getContext().stopService(new Intent(getContext(), LiveNetworkMonitor.class));
                        Log.d(TAG, "onPreferenceClick: Notification stopped");
                        snackbar = Snackbar.make(getActivity().findViewById(R.id.main_root),
                                getString(R.string.label_network_signal_notification_disabled), Snackbar.LENGTH_SHORT)
                                .setAnchorView(getActivity().findViewById(R.id.bottomNavigationView));
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
                    dialog.show();
                    return false;
                }
            });

            mNotificationRefreshInterval.setOnPreferenceClickListener(new androidx.preference.Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(androidx.preference.Preference preference) {
                    BottomSheetDialog dialog = new BottomSheetDialog(getContext(), R.style.BottomSheet);
                    View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.layout_refresh_interval, null);
                    RadioGroup intervalGroup = dialogView.findViewById(R.id.refresh_interval_group);
                    ConstraintLayout footer = dialogView.findViewById(R.id.footer);
                    TextView cancel = footer.findViewById(R.id.cancel);
                    TextView ok = footer.findViewById(R.id.ok);

                    int elapsedTIme = PreferenceManager.getDefaultSharedPreferences(getContext())
                            .getInt(NOTIFICATION_REFRESH_INTERVAL, 60000);
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
                            getContext().sendBroadcast(i);
                        }
                    });

                    dialog.setContentView(dialogView);
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

            mAddDataPlan.setOnPreferenceClickListener(new androidx.preference.Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(androidx.preference.Preference preference) {
                    BottomSheetDialog dialog = new BottomSheetDialog(getContext(), R.style.BottomSheet);
                    View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.layout_add_data_plan, null);

                    RadioGroup dataReset = dialogView.findViewById(R.id.data_reset);
                    TextInputEditText dataLimitInput = dialogView.findViewById(R.id.data_limit);
                    TabLayout dataTypeSwitcher = dialogView.findViewById(R.id.app_type_switcher);
                    ConstraintLayout footer = dialogView.findViewById(R.id.footer);
                    TextView cancel = footer.findViewById(R.id.cancel);
                    TextView ok = footer.findViewById(R.id.ok);

                    dataTypeSwitcher.selectTab(dataTypeSwitcher.getTabAt(PreferenceManager.getDefaultSharedPreferences(getContext())
                            .getInt(DATA_TYPE, 0)));
                    Float dataLimit = PreferenceManager.getDefaultSharedPreferences(getContext())
                            .getFloat(DATA_LIMIT, -1);
                    if (dataLimit > 0) {
                        if (dataLimit >= 1024) {
                            String data = String.format("%.2f", dataLimit / 1024) + "";
                            dataLimitInput.setText(data);
                        } else {
                            dataLimitInput.setText(PreferenceManager.getDefaultSharedPreferences(getContext())
                                    .getString(LIMIT, null));
                        }

                    }
                    if (PreferenceManager.getDefaultSharedPreferences(getContext())
                            .getString(DATA_RESET, "").equals(DATA_RESET_MONTHLY)) {
                        dataReset.check(R.id.monthly);
                    } else {
                        dataReset.check(R.id.daily);
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
                            if (dataLimitInput.getText().toString().length() <= 0) {
                                dataLimitInput.setBackground(getResources().getDrawable(R.drawable.text_input_error_background, null));
                            } else {
                                Float dataLimit = Float.parseFloat(dataLimitInput.getText().toString());
                                int dataType;
                                if (dataTypeSwitcher.getTabAt(0).isSelected()) {
                                    if (dataLimit >= 1024) {
                                        dataType = 1;
                                    } else {
                                        dataLimit = dataLimit;
                                        dataType = dataTypeSwitcher.getSelectedTabPosition();
                                    }
                                } else {
                                    dataLimit = dataLimit * 1024f;
                                    dataType = dataTypeSwitcher.getSelectedTabPosition();
                                }
                                if (dataReset.getCheckedRadioButtonId() == R.id.daily) {
                                    PreferenceManager.getDefaultSharedPreferences(getContext()).edit().putString(DATA_RESET, DATA_RESET_DAILY).apply();
                                } else {
                                    PreferenceManager.getDefaultSharedPreferences(getContext()).edit().putString(DATA_RESET, DATA_RESET_MONTHLY).apply();
                                }
                                PreferenceManager.getDefaultSharedPreferences(getContext()).edit().putFloat(DATA_LIMIT, dataLimit).apply();
                                PreferenceManager.getDefaultSharedPreferences(getContext()).edit().putString(LIMIT, dataLimitInput.getText().toString()).apply();
                                PreferenceManager.getDefaultSharedPreferences(getContext()).edit().putInt(DATA_TYPE, dataType).apply();
                                snackbar = Snackbar.make(getActivity().findViewById(R.id.main_root),
                                        getString(R.string.label_data_plan_saved), Snackbar.LENGTH_SHORT)
                                        .setAnchorView(getActivity().findViewById(R.id.bottomNavigationView));
                                updateResetData();
                                dialog.dismiss();
                                dismissOnClick(snackbar);
                                snackbar.show();
                                AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(getContext());
                                int[] ids = appWidgetManager.getAppWidgetIds(new ComponentName(getContext(), DataUsageWidget.class));
                                Intent intent = new Intent(getContext(), DataUsageWidget.class);
                                intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
                                getContext().sendBroadcast(intent);
                            }
                        }
                    });

                    dataLimitInput.addTextChangedListener(new TextWatcher() {
                        @Override
                        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                        }

                        @Override
                        public void onTextChanged(CharSequence s, int start, int before, int count) {
                            if (dataLimitInput.getText().toString().length() <= 0) {
                                dataLimitInput.setBackground(getResources().getDrawable(R.drawable.text_input_error_background, null));
                            } else {
                                dataLimitInput.setBackground(getResources().getDrawable(R.drawable.text_input_background, null));
                            }
                        }

                        @Override
                        public void afterTextChanged(Editable s) {

                        }
                    });

                    dialog.setContentView(dialogView);
                    dialog.setOnShowListener(new DialogInterface.OnShowListener() {
                        @Override
                        public void onShow(DialogInterface dialog) {
                            BottomSheetDialog bottomSheetDialog = (BottomSheetDialog) dialog;
                            FrameLayout bottomSheet = bottomSheetDialog.findViewById(R.id.design_bottom_sheet);
                            BottomSheetBehavior bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
                            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                        }
                    });
                    dialog.show();
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
                        ConstraintLayout footer = dialogView.findViewById(R.id.footer);
                        TextView cancel = footer.findViewById(R.id.cancel);
                        TextView ok = footer.findViewById(R.id.ok);

                        int resetDate = PreferenceManager.getDefaultSharedPreferences(getContext())
                                .getInt(DATA_RESET_DATE, -1);

                        if (resetDate > 0) {
                            datePicker.updateDate(datePicker.getYear(), datePicker.getMonth(), resetDate);
                        }

                        datePicker.setOnDateChangedListener(new android.widget.DatePicker.OnDateChangedListener() {
                            @Override
                            public void onDateChanged(android.widget.DatePicker datePicker, int i, int i1, int i2) {
                                hapticMinor(getContext());
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

                                String date = String.valueOf(datePicker.getDayOfMonth());
                                String suffix;

                                if (date.endsWith("1")) {
                                    suffix = "st";
                                }
                                else if (date.endsWith("2")) {
                                    suffix = "nd";
                                }
                                else if (date.endsWith("3")) {
                                    suffix = "rd";
                                }
                                else {
                                    suffix = "th";
                                }

                                mUsageResetTime.setSummary(getContext().getString(R.string.label_reset_every_month,
                                        date, suffix));

                                dialog.dismiss();


                                snackbar = Snackbar.make(getActivity().findViewById(R.id.main_root),
                                        getString(R.string.label_data_usage_reset_date_change, datePicker.getDayOfMonth(), suffix),
                                        Snackbar.LENGTH_SHORT)
                                        .setAnchorView(getActivity().findViewById(R.id.bottomNavigationView));
                                dismissOnClick(snackbar);
                                snackbar.show();

                            }
                        });

                        dialog.setContentView(dialogView);
                        dialog.show();
                    }
                    else {
                        BottomSheetDialog dialog = new BottomSheetDialog(getContext(), R.style.BottomSheet);
                        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.layout_time_picker, null);

                        TimePicker timePicker = (TimePicker) dialogView.findViewById(R.id.reset_time_picker);
                        ConstraintLayout footer = dialogView.findViewById(R.id.footer);
                        TextView cancel = footer.findViewById(R.id.cancel);
                        TextView ok = footer.findViewById(R.id.ok);

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
                                hapticMinor(getContext());
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

                                snackbar = Snackbar.make(getActivity().findViewById(R.id.main_root),
                                        getString(R.string.label_data_usage_reset_time_change, interval, time), Snackbar.LENGTH_SHORT)
                                        .setAnchorView(getActivity().findViewById(R.id.bottomNavigationView));
                                dismissOnClick(snackbar);
                                snackbar.show();

                            }
                        });


                        TimePickerDialog timePickerDialog = new TimePickerDialog(getContext(), new TimePickerDialog.OnTimeSetListener() {
                            @Override
                            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                                PreferenceManager.getDefaultSharedPreferences(getContext()).edit()
                                        .putInt(DATA_RESET_HOUR, hourOfDay).apply();
                                PreferenceManager.getDefaultSharedPreferences(getContext()).edit()
                                        .putInt(DATA_RESET_MIN, minute).apply();

                                Intent i = new Intent(getContext(), NotificationUpdater.class);
                                getContext().sendBroadcast(i);
                                AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(getContext());
                                int[] ids = appWidgetManager.getAppWidgetIds(new ComponentName(getContext(), DataUsageWidget.class));
                                Intent intent = new Intent(getContext(), DataUsageWidget.class);
                                intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
                                getContext().sendBroadcast(intent);

                                int h, m;
                                m = minute;
                                String time, interval;

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

                                snackbar = Snackbar.make(getActivity().findViewById(R.id.main_root),
                                        getString(R.string.label_data_usage_reset_time_change, interval, time), Snackbar.LENGTH_SHORT)
                                        .setAnchorView(getActivity().findViewById(R.id.bottomNavigationView));
                                dismissOnClick(snackbar);
                                snackbar.show();
                            }
                        }, hour, minute, false);
//                        timePickerDialog.show();

                        dialog.setContentView(dialogView);
                        dialog.show();
                    }
                    return false;
                }
            });


            mShowDataWarning.setOnPreferenceClickListener(new androidx.preference.Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(androidx.preference.Preference preference) {
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
                            String output = rawValue.replace(".0", "");

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
                            String output = rawValue.replace(".0", "");
                            PreferenceManager.getDefaultSharedPreferences(getContext()).edit().putInt(DATA_WARNING_TRIGGER_LEVEL,
                                    Integer.parseInt(output)).apply();
                            mDataWarningTrigger.setSummary(getContext().getString(R.string.label_data_trigger_level, output));
                            PreferenceManager.getDefaultSharedPreferences(getContext()).edit().putBoolean(DATA_USAGE_WARNING_SHOWN, false).apply();
                            if (PreferenceManager.getDefaultSharedPreferences(getContext()).getBoolean(DATA_USAGE_ALERT, false)) {
                                try {
                                    int trigger = PreferenceManager.getDefaultSharedPreferences(getContext()).getInt(DATA_WARNING_TRIGGER_LEVEL, 85);

                                    if (PreferenceManager.getDefaultSharedPreferences(getContext()).getFloat(DATA_LIMIT, -1) * trigger / 100 >
                                            Double.parseDouble(formatData(getDeviceMobileDataUsage(getContext(), SESSION_TODAY, 1)[0],
                                                    getDeviceMobileDataUsage(getContext(), SESSION_TODAY, 1)[1])[2]
                                                    .replace(" MB", "").replace(" GB", ""))) {
                                        resumeMonitor();
                                    }
                                } catch (ParseException e) {
                                    e.printStackTrace();
                                } catch (RemoteException e) {
                                    e.printStackTrace();
                                }
                            }
                            dialog.dismiss();
                        }
                    });

                    dialog.setContentView(dialogView);
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

        }

        public static void pauseMonitor() {
            mContext.stopService(new Intent(mContext, DataUsageMonitor.class));
        }

        public static void resumeMonitor() {
            mContext.startService(new Intent(mContext, DataUsageMonitor.class));
        }

        private void updateResetData() {
            String resetTitle, resetSummary;
            if (PreferenceManager.getDefaultSharedPreferences(getContext()).getString(DATA_RESET, "null")
                    .equals(DATA_RESET_MONTHLY)) {
                resetTitle = getContext().getString(R.string.setup_usage_reset_date);
                String date = String.valueOf(PreferenceManager.getDefaultSharedPreferences(getContext())
                        .getInt(DATA_RESET_DATE, 1));
                String suffix;

                if (date.endsWith("1")) {
                    suffix = "st";
                }
                else if (date.endsWith("2")) {
                    suffix = "nd";
                }
                else if (date.endsWith("3")) {
                    suffix = "rd";
                }
                else {
                    suffix = "th";
                }

                resetSummary = (getContext().getString(R.string.label_reset_every_month,
                        date, suffix));
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
    }

}