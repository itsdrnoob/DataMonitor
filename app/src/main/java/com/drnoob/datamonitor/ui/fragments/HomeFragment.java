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

import static com.drnoob.datamonitor.Common.dismissOnClick;
import static com.drnoob.datamonitor.Common.formatOrdinalNumber;
import static com.drnoob.datamonitor.Common.getCurrentLocale;
import static com.drnoob.datamonitor.Common.setDataPlanNotification;
import static com.drnoob.datamonitor.Common.setRefreshAlarm;
import static com.drnoob.datamonitor.core.Values.DAILY_DATA_HOME_ACTION;
import static com.drnoob.datamonitor.core.Values.DATA_LIMIT;
import static com.drnoob.datamonitor.core.Values.DATA_PLAN_FRAGMENT;
import static com.drnoob.datamonitor.core.Values.DATA_QUOTA;
import static com.drnoob.datamonitor.core.Values.DATA_QUOTA_PERFORMED_RESET;
import static com.drnoob.datamonitor.core.Values.DATA_QUOTA_SCHEDULED_RESET;
import static com.drnoob.datamonitor.core.Values.DATA_RESET;
import static com.drnoob.datamonitor.core.Values.DATA_RESET_CUSTOM;
import static com.drnoob.datamonitor.core.Values.DATA_RESET_CUSTOM_DATE_END;
import static com.drnoob.datamonitor.core.Values.DATA_RESET_CUSTOM_DATE_END_HOUR;
import static com.drnoob.datamonitor.core.Values.DATA_RESET_CUSTOM_DATE_END_MIN;
import static com.drnoob.datamonitor.core.Values.DATA_RESET_DAILY;
import static com.drnoob.datamonitor.core.Values.DATA_RESET_DATE;
import static com.drnoob.datamonitor.core.Values.DATA_RESET_MONTHLY;
import static com.drnoob.datamonitor.core.Values.DATA_USAGE_SESSION;
import static com.drnoob.datamonitor.core.Values.DATA_USAGE_TODAY;
import static com.drnoob.datamonitor.core.Values.DATA_USAGE_TYPE;
import static com.drnoob.datamonitor.core.Values.GENERAL_FRAGMENT_ID;
import static com.drnoob.datamonitor.core.Values.SESSION_CUSTOM;
import static com.drnoob.datamonitor.core.Values.SESSION_MONTHLY;
import static com.drnoob.datamonitor.core.Values.SESSION_TODAY;
import static com.drnoob.datamonitor.core.Values.SHOW_ADD_PLAN_BANNER;
import static com.drnoob.datamonitor.core.Values.TYPE_MOBILE_DATA;
import static com.drnoob.datamonitor.core.Values.TYPE_WIFI;
import static com.drnoob.datamonitor.ui.activities.MainActivity.setRefreshAppDataUsage;
import static com.drnoob.datamonitor.utils.NetworkStatsHelper.formatData;
import static com.drnoob.datamonitor.utils.NetworkStatsHelper.getDeviceMobileDataUsage;
import static com.drnoob.datamonitor.utils.NetworkStatsHelper.getDeviceWifiDataUsage;
import static com.drnoob.datamonitor.utils.NetworkStatsHelper.updateOverview;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.drnoob.datamonitor.R;
import com.drnoob.datamonitor.Widget.DataUsageWidget;
import com.drnoob.datamonitor.adapters.data.OverviewModel;
import com.drnoob.datamonitor.ui.activities.ContainerActivity;
import com.drnoob.datamonitor.utils.NotificationService;
import com.drnoob.datamonitor.utils.SmartDataAllocationService;
import com.drnoob.datamonitor.utils.VibrationUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.skydoves.progressview.ProgressView;

import org.jetbrains.annotations.NotNull;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;


public class HomeFragment extends Fragment implements View.OnLongClickListener {
    private static final String TAG = HomeFragment.class.getSimpleName();
    private static final int MODE_LOAD_OVERVIEW = 0;
    private static final int MODE_REFRESH_OVERVIEW = 1;

    private LinearLayout mSetupDataPlan;
    private ConstraintLayout mGraphView;
    private MaterialButton mDismissPlanBanner, mAddDataPlan;
    private TextView mMobileDataUsage,
            mMobileDataSent,
            mMobileDataReceived,
            mWifiDataUsage,
            mWifiDataSent,
            mWifiFataReceived;
    private Long[] mobile, wifi;
    private Snackbar snackbar;
    private LinearLayout mMobileDataUsageToday, mWifiUsageToday;
    private static ProgressView mMobileMon, mMobileTue, mMobileWed, mMobileThurs, mMobileFri, mMobileSat, mMobileSun,
            mWifiMon, mWifiTue, mWifiWed, mWifiThurs, mWifiFri, mWifiSat, mWifiSun;
    private LinearLayout mMonView, mTueView, mWedView, mThursView, mFriView, mSatView, mSunView;
    private static ConstraintLayout mOverview;
    private static ConstraintLayout mOverviewLoading;
    private static ImageView mRefreshOverview;
    private static Context mContext;
    private static List<OverviewModel> mList = new ArrayList<>();
    private boolean openQuickView = false;
    private TextView mDataRemaining;
    private Long planStartDateMillis, planEndDateMillis;
    private ActivityResultLauncher<Intent> dataPlanLauncher;
    private ConstraintLayout mPlanDetailsView;
    private TextView mPlanDetailsTitle, mPlanUsage, mPlanValidity, mDailyQuota;

    private SharedPreferences preferences;

    public HomeFragment() {
        // Required empty public constructor
    }

    public boolean isOpenQuickView() {
        return openQuickView;
    }

    public void setOpenQuickView(boolean openQuickView) {
        this.openQuickView = openQuickView;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dataPlanLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == Activity.RESULT_OK) {
                            snackbar = Snackbar.make(getActivity().findViewById(R.id.main_root),
                                            getString(R.string.label_data_plan_saved), Snackbar.LENGTH_SHORT)
                                    .setAnchorView(getActivity().findViewById(R.id.bottomNavigationView));
                            if (PreferenceManager.getDefaultSharedPreferences(requireContext())
                                    .getString(DATA_RESET, "null").equals(DATA_RESET_CUSTOM)) {
                                if (PreferenceManager.getDefaultSharedPreferences(requireContext())
                                        .getBoolean("auto_update_data_plan", false)) {
                                    setRefreshAlarm(requireContext());
                                } else {
                                    setDataPlanNotification(requireContext());
                                }
                            }
                            mSetupDataPlan.setVisibility(View.GONE);
                            updateDataBalance();
                            dismissOnClick(snackbar);
                            snackbar.show();
                            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(getContext());
                            int[] ids = appWidgetManager.getAppWidgetIds(new ComponentName(getContext(), DataUsageWidget.class));
                            Intent intent = new Intent(getContext(), DataUsageWidget.class);
                            intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
                            boolean updateNotification = PreferenceManager.getDefaultSharedPreferences(getContext()).getBoolean("setup_notification", false);
                            if (updateNotification) {
                                Intent notificationIntent = new Intent(getContext(), NotificationService.NotificationUpdater.class);
                                getContext().sendBroadcast(notificationIntent);
                            }

                            getContext().sendBroadcast(intent);
                        }
                    }
                }
        );

        preferences = PreferenceManager.getDefaultSharedPreferences(requireContext());
    }

    @Override
    public void onAttach(@NonNull @NotNull Context context) {
        super.onAttach(context);
        mContext = context;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        mSetupDataPlan = view.findViewById(R.id.setup_data_plan);
        mGraphView = view.findViewById(R.id.graph_view);
        mDismissPlanBanner = view.findViewById(R.id.dismiss_add_plan_banner);
        mAddDataPlan = view.findViewById(R.id.add_data_plan);
        mMobileDataUsage = view.findViewById(R.id.mobile_data_usage);
        mMobileDataSent = view.findViewById(R.id.mobile_data_sent);
        mMobileDataReceived = view.findViewById(R.id.mobile_data_received);
        mWifiDataUsage = view.findViewById(R.id.wifi_data_usage);
        mWifiDataSent = view.findViewById(R.id.wifi_data_sent);
        mWifiFataReceived = view.findViewById(R.id.wifi_data_received);
        mMobileDataUsageToday = view.findViewById(R.id.data_usage_mobile_today);
        mWifiUsageToday = view.findViewById(R.id.data_usage_wifi_today);
        mDataRemaining = view.findViewById(R.id.home_data_remaining);
        mPlanDetailsView = view.findViewById(R.id.plan_details_view);
        mPlanDetailsTitle = view.findViewById(R.id.plan_details_title);
        mPlanUsage = view.findViewById(R.id.plan_usage_details);
        mPlanValidity = view.findViewById(R.id.plan_validity_details);
        mDailyQuota = view.findViewById(R.id.daily_quota);

        mOverview = view.findViewById(R.id.overview);
        mOverviewLoading = view.findViewById(R.id.overview_loading);

        mMobileMon = mOverview.findViewById(R.id.progress_mobile_mon);
        mMobileTue = mOverview.findViewById(R.id.progress_mobile_tue);
        mMobileWed = mOverview.findViewById(R.id.progress_mobile_wed);
        mMobileThurs = mOverview.findViewById(R.id.progress_mobile_thurs);
        mMobileFri = mOverview.findViewById(R.id.progress_mobile_fri);
        mMobileSat = mOverview.findViewById(R.id.progress_mobile_sat);
        mMobileSun = mOverview.findViewById(R.id.progress_mobile_sun);

        mWifiMon = mOverview.findViewById(R.id.progress_wifi_mon);
        mWifiTue = mOverview.findViewById(R.id.progress_wifi_tue);
        mWifiWed = mOverview.findViewById(R.id.progress_wifi_wed);
        mWifiThurs = mOverview.findViewById(R.id.progress_wifi_thurs);
        mWifiFri = mOverview.findViewById(R.id.progress_wifi_fri);
        mWifiSat = mOverview.findViewById(R.id.progress_wifi_sat);
        mWifiSun = mOverview.findViewById(R.id.progress_wifi_sun);

        mMonView = view.findViewById(R.id.view_mon);
        mTueView = view.findViewById(R.id.view_tue);
        mWedView = view.findViewById(R.id.view_wed);
        mThursView = view.findViewById(R.id.view_thurs);
        mFriView = view.findViewById(R.id.view_fri);
        mSatView = view.findViewById(R.id.view_sat);
        mSunView = view.findViewById(R.id.view_sun);

        mMonView.setOnLongClickListener(this::onLongClick);
        mTueView.setOnLongClickListener(this::onLongClick);
        mWedView.setOnLongClickListener(this::onLongClick);
        mThursView.setOnLongClickListener(this::onLongClick);
        mFriView.setOnLongClickListener(this::onLongClick);
        mSatView.setOnLongClickListener(this::onLongClick);
        mSunView.setOnLongClickListener(this::onLongClick);

        mRefreshOverview = view.findViewById(R.id.overview_refresh);

        updateData();
        updateDataBalance();
        refreshOverview();
        checkDataQuota();

        mMobileDataUsage.setSelected(true);
        mWifiDataUsage.setSelected(true);

        boolean showPlanBanner = preferences.getBoolean(SHOW_ADD_PLAN_BANNER, true);

        if (preferences.getFloat(DATA_LIMIT, -1) > 0 || !showPlanBanner) {
            mSetupDataPlan.setVisibility(View.GONE);
        } else {
            mSetupDataPlan.setVisibility(View.VISIBLE);
        }

        mRefreshOverview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mRefreshOverview.animate().rotation(720).setDuration(1000)
                        .setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                super.onAnimationEnd(animation);
                                mRefreshOverview.setRotation(0);
                            }
                        });
//                refreshOverview();
                UpdateOverview updateOverview = new UpdateOverview(MODE_REFRESH_OVERVIEW);
                updateOverview.execute();
            }
        });


//        if (mobile != null && mobile.length > 0) {
//
//        }
//        else {
//            Log.d(TAG, "onCreateView: refreshing data");
//            updateData();
//        }

        mAddDataPlan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), ContainerActivity.class);
                intent.putExtra(GENERAL_FRAGMENT_ID, DATA_PLAN_FRAGMENT);
                dataPlanLauncher.launch(intent);
            }
        });

        mDismissPlanBanner.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mSetupDataPlan.animate()
                        .alpha(0f)
                        .setDuration(200)
                        .scaleY(0.8f)
                        .scaleX(0.8f)
                        .setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationStart(Animator animation) {
                                super.onAnimationStart(animation);
                                float total = mSetupDataPlan.getHeight() + 28; // just to adjust a bit of distance issue
                                mGraphView.animate()
                                        .translationY((total * -1))
                                        .setDuration(300)
                                        .setStartDelay(80)
                                        .setListener(new AnimatorListenerAdapter() {
                                            @Override
                                            public void onAnimationEnd(Animator animation) {
                                                super.onAnimationEnd(animation);

                                                mGraphView.animate()
                                                        .translationY(0)
                                                        .setDuration(0)
                                                        .setListener(new AnimatorListenerAdapter() {
                                                            @Override
                                                            public void onAnimationStart(Animator animation) {
                                                                super.onAnimationStart(animation);
                                                                mSetupDataPlan.setVisibility(View.GONE);
                                                            }
                                                        })
                                                        .start();
                                            }
                                        })
                                        .start();
                            }
                        })
                        .start();

                preferences.edit()
                        .putBoolean(SHOW_ADD_PLAN_BANNER, false)
                        .apply();
            }
        });

        mMobileDataUsageToday.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mContext, ContainerActivity.class);
                intent.putExtra(GENERAL_FRAGMENT_ID, DATA_USAGE_TODAY);
                intent.putExtra(DATA_USAGE_SESSION, SESSION_TODAY);
                intent.putExtra(DATA_USAGE_TYPE, TYPE_MOBILE_DATA);
                intent.putExtra(DAILY_DATA_HOME_ACTION, true);
                setRefreshAppDataUsage(true);
                startActivity(intent);
            }
        });

        mWifiUsageToday.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mContext, ContainerActivity.class);
                intent.putExtra(GENERAL_FRAGMENT_ID, DATA_USAGE_TODAY);
                intent.putExtra(DATA_USAGE_SESSION, SESSION_TODAY);
                intent.putExtra(DATA_USAGE_TYPE, TYPE_WIFI);
                intent.putExtra(DAILY_DATA_HOME_ACTION, true);
                setRefreshAppDataUsage(true);
                startActivity(intent);
            }
        });


        return view;
    }

    private void updateDataBalance() {
        Long[] mobileData = null;
        int date = preferences.getInt(DATA_RESET_DATE, 1);
        String planDetailsTitle = requireContext().getString(R.string.label_plan_details_title_unknown);
        boolean isSmartAllocationEnabled = preferences.getBoolean("smart_data_allocation", false);

        try {
            if (preferences.getString(DATA_RESET, "null")
                    .equals(DATA_RESET_MONTHLY)) {
                mobileData = getDeviceMobileDataUsage(getContext(), SESSION_MONTHLY, date);
                planDetailsTitle = requireContext().getString(R.string.label_plan_details_title_monthly);
            } else if (preferences.getString(DATA_RESET, "null")
                    .equals(DATA_RESET_DAILY)) {
                mobileData = getDeviceMobileDataUsage(getContext(), SESSION_TODAY, 1);
                planDetailsTitle = requireContext().getString(R.string.label_plan_details_title_daily);
            } else {
                mobileData = getDeviceMobileDataUsage(getContext(), SESSION_CUSTOM, -1);
                planDetailsTitle = requireContext().getString(R.string.label_plan_details_title_custom);
            }

        } catch (ParseException | RemoteException e) {
            e.printStackTrace();
        }

        Float dataLimit = preferences.getFloat(DATA_LIMIT, -1);
        if (dataLimit > 0) {
            if (preferences.getString(DATA_RESET, null)
                    .equals(DATA_RESET_DAILY)) {
//                Long total = (mobileData[2]);
//                Long limit = dataLimit.longValue() * 1048576;
//                Long remaining;
//                String remainingData;
//                if (limit > total) {
//                    remaining= limit - total;
//                    remainingData = formatData(remaining / 2, remaining / 2)[2];
//                    mDataRemaining.setText(getContext().getString(R.string.label_data_remaining, remainingData));
//                }
//                else {
//                    remaining= total - limit;
//                    remainingData = formatData(remaining / 2, remaining / 2)[2];
//                    mDataRemaining.setText(getContext().getString(R.string.label_data_remaining_used_excess, remainingData));
//                }

                Long total = (mobileData[2]);
                Long limit = dataLimit.longValue() * 1048576;
                Long remaining;
                String remainingData;
                mPlanValidity.setVisibility(View.GONE);

                if (isSmartAllocationEnabled) {
                    Float quota = preferences.getFloat(DATA_QUOTA, 0F);
                    String dailyQuota = formatData(0L, (quota.longValue() * 1024 * 1024))[2];
                    mDailyQuota.setText(getString(R.string.label_daily_quota, dailyQuota));
                    mDailyQuota.setVisibility(View.VISIBLE);
                } else {
                    mDailyQuota.setVisibility(View.GONE);
                }

                if (limit > total) {
                    remaining = limit - total;
                    remainingData = formatData(remaining / 2, remaining / 2)[2];
                    mPlanUsage.setText(getContext().getString(R.string.label_data_remaining, remainingData));
                } else {
                    remaining = total - limit;
                    remainingData = formatData(remaining / 2, remaining / 2)[2];
                    mPlanUsage.setText(getContext().getString(R.string.label_data_remaining_used_excess, remainingData));
                }


            } else if (preferences.getString(DATA_RESET, null)
                    .equals(DATA_RESET_MONTHLY)) {
                String validity = getPlanValidity(SESSION_MONTHLY);
                mPlanValidity.setText(validity);
                if (isSmartAllocationEnabled) {
                    Float quota = preferences.getFloat(DATA_QUOTA, 0F);
                    String dailyQuota = formatData(0L, (quota.longValue() * 1024 * 1024))[2];
                    mDailyQuota.setText(getString(R.string.label_daily_quota, dailyQuota));
                    mDailyQuota.setVisibility(View.VISIBLE);
                } else {
                    mDailyQuota.setVisibility(View.GONE);
                }
//                Long total = getDeviceMobileDataUsage(getContext(), SESSION_MONTHLY, date)[2];
                Long total = mobileData[2];
                Long limit = dataLimit.longValue() * 1048576;
                Long remaining;
                String remainingData;
                String used = formatData(0l, total)[2];
                if (limit > total) {
                    remaining = limit - total;
                    remainingData = requireContext().getString(R.string.label_data_remaining,
                            formatData(remaining / 2, remaining / 2)[2]);
//                    mDataRemaining.setText(getContext().getString(R.string.label_data_remaining, remainingData));
                } else {
                    remaining = total - limit;
                    remainingData = requireContext().getString(R.string.label_data_remaining_used_excess,
                            formatData(remaining / 2, remaining / 2)[2]);
//                    mDataRemaining.setText(getContext().getString(R.string.label_data_remaining_used_excess, remainingData));
                }

                String usageDetails = requireContext().getString(R.string.home_plan_usage_details, used, remainingData);
                mPlanUsage.setText(usageDetails);
            } else {
                String validity = getPlanValidity(SESSION_CUSTOM);
                mPlanValidity.setText(validity);
                if (isSmartAllocationEnabled) {
                    Float quota = preferences.getFloat(DATA_QUOTA, 0F);
                    String dailyQuota = formatData(0L, (quota.longValue() * 1024 * 1024))[2];
                    mDailyQuota.setText(getString(R.string.label_daily_quota, dailyQuota));
                    mDailyQuota.setVisibility(View.VISIBLE);
                } else {
                    mDailyQuota.setVisibility(View.GONE);
                }
                Long total = (mobileData[2]);
                Long limit = dataLimit.longValue() * 1048576;
                Long remaining;
                String remainingData;
                String used = formatData(0l, total)[2];
                if (limit > total) {
                    remaining = limit - total;
                    remainingData = requireContext().getString(R.string.label_data_remaining,
                            formatData(remaining / 2, remaining / 2)[2]);
//                    mDataRemaining.setText(getContext().getString(R.string.label_data_remaining, remainingData));
                } else {
                    remaining = total - limit;
                    remainingData = requireContext().getString(R.string.label_data_remaining_used_excess,
                            formatData(remaining / 2, remaining / 2)[2]);
//                    mDataRemaining.setText(getContext().getString(R.string.label_data_remaining_used_excess, remainingData));
                }
                String usageDetails = requireContext().getString(R.string.home_plan_usage_details, used, remainingData);
                mPlanUsage.setText(usageDetails);
            }

            mDataRemaining.setVisibility(View.GONE);
            mPlanDetailsView.setVisibility(View.VISIBLE);
            mPlanDetailsTitle.setText(planDetailsTitle);
        } else {
            // No data plan is set. Hide mDataRemaining view.
            mDataRemaining.setVisibility(View.GONE);
            mPlanDetailsView.setVisibility(View.GONE);
        }
    }

    /**
     * Calculates the plan validity for Monthly/Custom data plans.
     *
     * @param session The plan session. One of SESSION_MONTHLY or SESSION_CUSTOM.
     *
     * @return Plan reset date and the number of days remaining as a formatted string.
     */
    @SuppressLint("StringFormatMatches")
    private String getPlanValidity(int session) {
        String validity;
        Calendar calendar = Calendar.getInstance();
        String month, ordinal, end;
        int endDate;
        int daysRemaining;
        if (session == SESSION_MONTHLY) {
            int planReset = preferences.getInt(DATA_RESET_DATE, 1);
            int daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
            int currentReset = Math.min(planReset, daysInMonth);
            int today = calendar.get(Calendar.DAY_OF_MONTH);
            if (today >= currentReset) {
                calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) + 1);
                daysRemaining = daysInMonth - today + planReset;
            }
            else {
                daysRemaining = planReset - today;
            }
            month = new SimpleDateFormat("MMMM", getCurrentLocale(requireContext())).format(calendar.getTime());
            endDate = planReset;
        }
        else {
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);

            long planEndDateMillis;
            try {
                planEndDateMillis = preferences.getLong(DATA_RESET_CUSTOM_DATE_END, -1);
            }
            catch (ClassCastException e) {
                int planEndIntValue = preferences.getInt(DATA_RESET_CUSTOM_DATE_END, -1);
                planEndDateMillis = ((Number) planEndIntValue).longValue();
            }

            int planEndHour = preferences.getInt(DATA_RESET_CUSTOM_DATE_END_HOUR, 0);
            int planEndMin = preferences.getInt(DATA_RESET_CUSTOM_DATE_END_MIN, 0);

            calendar.setTimeInMillis(planEndDateMillis);
            calendar.set(Calendar.HOUR, planEndHour);
            calendar.set(Calendar.MINUTE, planEndMin);

            month = new SimpleDateFormat("MMMM", getCurrentLocale(requireContext())).format(calendar.getTime());
            endDate = calendar.get(Calendar.DAY_OF_MONTH);

            long currentTimeMillis = System.currentTimeMillis();
            long endTimeMillis = calendar.getTimeInMillis();

            long remainingMillis = endTimeMillis - currentTimeMillis;
            daysRemaining = (int) Math.round((remainingMillis / (24 * 60 * 60 * 1000.0)));
        }
        ordinal = formatOrdinalNumber(endDate, requireContext());
        end = ordinal + " " + month;
        if (daysRemaining < 0) {
            daysRemaining = 0;
        }
        String remaining = requireContext().getString(R.string.label_days_remaining, Integer.toString(daysRemaining));
        validity = requireContext().getString(R.string.label_plan_validity, end, remaining);
        return validity;
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateData();
        updateDataBalance();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (snackbar != null) {
            snackbar.dismiss();
        }
    }

    private void updateData() {
        try {
            mobile = getDeviceMobileDataUsage(getContext(), SESSION_TODAY, 1);
            wifi = getDeviceWifiDataUsage(getContext(), SESSION_TODAY);

            String[] mobileData = formatData(mobile[0], mobile[1]);
            String[] wifiData = formatData(wifi[0], wifi[1]);
            mMobileDataUsage.setText(mobileData[2]);
            mWifiDataUsage.setText(wifiData[2]);

            String mobileDataSent = getResources().getString(R.string.home_mobile_data_sent,
                    mobileData[0]);
            String mobileDataReceived = getResources().getString(R.string.home_mobile_data_received,
                    mobileData[1]);
            String wifiDataSent = getResources().getString(R.string.home_wifi_data_sent,
                    wifiData[0]);
            String wifiDataReceived = getResources().getString(R.string.home_wifi_data_received,
                    wifiData[1]);

            mMobileDataSent.setText(mobileDataSent);
            mMobileDataReceived.setText(mobileDataReceived);
            mWifiDataSent.setText(wifiDataSent);
            mWifiFataReceived.setText(wifiDataReceived);


        } catch (ParseException | RemoteException e) {
            e.printStackTrace();
        }
    }

    private static void resetOverview() {
        mOverviewLoading.setAlpha(0.0f);
        mOverview.setAlpha(1.0f);

        OverviewModel model = null;

        for (int i = 0; i < mList.size(); i++) {
            model = mList.get(i);
            long mobileSent = (model.getTotalMobile() / 2l) * 1048576;
            long mobileReceived = mobileSent;
            long wifiSent = (model.getTotalWifi() / 2l) * 1048576;
            long wifiReceived = wifiSent;
            String data = formatData(mobileSent, mobileReceived)[2];
            String wifi = formatData(wifiSent, wifiReceived)[2];
            switch (i) {
                case 0:
                    mMobileMon.setProgress((model.getTotalMobile() / 25) + 2);  // 500 MB is 20 in the progressBar, so divided by 25. Added 2 to fix margin issue
                    mWifiMon.setProgress((model.getTotalWifi() / 25) + 2);
                    mMobileMon.setLabelText(data);
                    mWifiMon.setLabelText(wifi);
                    break;

                case 1:
                    mMobileTue.setProgress((model.getTotalMobile() / 25) + 2);
                    mWifiTue.setProgress((model.getTotalWifi() / 25) + 2);
                    mMobileTue.setLabelText(data);
                    mWifiTue.setLabelText(wifi);
                    break;

                case 2:
                    mMobileWed.setProgress((model.getTotalMobile() / 25) + 2);
                    mWifiWed.setProgress((model.getTotalWifi() / 25) + 2);
                    mMobileWed.setLabelText(data);
                    mWifiWed.setLabelText(wifi);
                    break;

                case 3:
                    mMobileThurs.setProgress((model.getTotalMobile() / 25) + 2);
                    mWifiThurs.setProgress((model.getTotalWifi() / 25) + 2);
                    mMobileThurs.setLabelText(data);
                    mWifiThurs.setLabelText(wifi);
                    break;

                case 4:
                    mMobileFri.setProgress((model.getTotalMobile() / 25) + 2);
                    mWifiFri.setProgress((model.getTotalWifi() / 25) + 2);
                    mMobileFri.setLabelText(data);
                    mWifiFri.setLabelText(wifi);
                    break;

                case 5:
                    mMobileSat.setProgress((model.getTotalMobile() / 25) + 2);
                    mWifiSat.setProgress((model.getTotalWifi() / 25) + 2);
                    mMobileSat.setLabelText(data);
                    mWifiSat.setLabelText(wifi);
                    break;

                case 6:
                    mMobileSun.setProgress((model.getTotalMobile() / 25) + 2);
                    mWifiSun.setProgress((model.getTotalWifi() / 25) + 2);
                    mMobileSun.setLabelText(data);
                    mWifiSun.setLabelText(wifi);
                    break;
            }
        }
    }

    private static void refreshOverview() {
        if (isOverviewAvailable()) {
            mOverviewLoading.setAlpha(0.0f);
            mOverview.setAlpha(1.0f);

            OverviewModel model = null;

            for (int i = 0; i < mList.size(); i++) {
                model = mList.get(i);
                long mobileSent = (model.getTotalMobile() / 2l) * 1048576;
                long mobileReceived = mobileSent;
                long wifiSent = (model.getTotalWifi() / 2l) * 1048576;
                long wifiReceived = wifiSent;
                String data = formatData(mobileSent, mobileReceived)[2];
                String wifi = formatData(wifiSent, wifiReceived)[2];
                switch (i) {
                    case 0:
                        mMobileMon.setProgress((model.getTotalMobile() / 25) + 2);  // 500 MB is 20 in the progressBar, so divided by 25. Added 2 to fix margin issue
                        mWifiMon.setProgress((model.getTotalWifi() / 25) + 2);
                        mMobileMon.setLabelText(data);
                        mWifiMon.setLabelText(wifi);
                        break;

                    case 1:
                        mMobileTue.setProgress((model.getTotalMobile() / 25) + 2);
                        mWifiTue.setProgress((model.getTotalWifi() / 25) + 2);
                        mMobileTue.setLabelText(data);
                        mWifiTue.setLabelText(wifi);
                        break;

                    case 2:
                        mMobileWed.setProgress((model.getTotalMobile() / 25) + 2);
                        mWifiWed.setProgress((model.getTotalWifi() / 25) + 2);
                        mMobileWed.setLabelText(data);
                        mWifiWed.setLabelText(wifi);
                        break;

                    case 3:
                        mMobileThurs.setProgress((model.getTotalMobile() / 25) + 2);
                        mWifiThurs.setProgress((model.getTotalWifi() / 25) + 2);
                        mMobileThurs.setLabelText(data);
                        mWifiThurs.setLabelText(wifi);
                        break;

                    case 4:
                        mMobileFri.setProgress((model.getTotalMobile() / 25) + 2);
                        mWifiFri.setProgress((model.getTotalWifi() / 25) + 2);
                        mMobileFri.setLabelText(data);
                        mWifiFri.setLabelText(wifi);
                        break;

                    case 5:
                        mMobileSat.setProgress((model.getTotalMobile() / 25) + 2);
                        mWifiSat.setProgress((model.getTotalWifi() / 25) + 2);
                        mMobileSat.setLabelText(data);
                        mWifiSat.setLabelText(wifi);
                        break;

                    case 6:
                        mMobileSun.setProgress((model.getTotalMobile() / 25) + 2);
                        mWifiSun.setProgress((model.getTotalWifi() / 25) + 2);
                        mMobileSun.setLabelText(data);
                        mWifiSun.setLabelText(wifi);
                        break;
                }
            }
        } else {
            UpdateOverview updateOverview = new UpdateOverview(MODE_LOAD_OVERVIEW);
            updateOverview.execute();
        }
    }

    private static boolean isOverviewAvailable() {
        return mList.size() > 0;
    }

    @Override
    public boolean onLongClick(View view) {
        setOpenQuickView(true);
        float translation = getTranslation(view);
        float finalTranslation = translation;

        View popupView = LayoutInflater.from(getContext()).inflate(R.layout.layout_overview_quick_view, null);
        TextView overviewMobile = popupView.findViewById(R.id.overview_mobile_data);
        TextView overviewWifi = popupView.findViewById(R.id.overview_wifi);

        PopupWindow popupWindow = new PopupWindow(popupView,
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT,
                true);
        popupWindow.setAnimationStyle(R.style.PopupAnimation);

        if (!preferences.getBoolean("disable_haptics", false)) {
            VibrationUtils.hapticMajor(getContext());
        }
        if (isOverviewAvailable()) {
            try {
                String[] dataUsage = getDataUsage(view);
                overviewMobile.setText(dataUsage[0]);
                overviewWifi.setText(dataUsage[1]);
                popupWindow.setElevation(100);
                view.setElevation(100);
                popupWindow.showAtLocation(getView(), Gravity.CENTER, 0, 0);
            } catch (NullPointerException e) {
                e.printStackTrace();
            }
        }
        view.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (isOpenQuickView()) {
                    if (motionEvent.getAction() == MotionEvent.ACTION_UP ||
                            motionEvent.getAction() == MotionEvent.ACTION_CANCEL) {
                        if (!preferences.getBoolean("disable_haptics", false)) {
                            VibrationUtils.hapticMinor(getContext());
                        }
                        popupWindow.dismiss();
                        setOpenQuickView(false);
                    }
                }
                return false;
            }
        });
        return false;
    }

    private String[] getDataUsage(View view) {
        String mobile, wifi;
        switch (view.getId()) {
            case R.id.view_mon:
                mobile = mMobileMon.getLabelText().toString();
                wifi = mWifiMon.getLabelText().toString();
                break;

            case R.id.view_tue:
                mobile = mMobileTue.getLabelText().toString();
                wifi = mWifiTue.getLabelText().toString();
                break;

            case R.id.view_wed:
                mobile = mMobileWed.getLabelText().toString();
                wifi = mWifiWed.getLabelText().toString();
                break;

            case R.id.view_thurs:
                mobile = mMobileThurs.getLabelText().toString();
                wifi = mWifiThurs.getLabelText().toString();
                break;

            case R.id.view_fri:
                mobile = mMobileFri.getLabelText().toString();
                wifi = mWifiFri.getLabelText().toString();
                break;

            case R.id.view_sat:
                mobile = mMobileSat.getLabelText().toString();
                wifi = mWifiSat.getLabelText().toString();
                break;

            case R.id.view_sun:
                mobile = mMobileSun.getLabelText().toString();
                wifi = mWifiSun.getLabelText().toString();
                break;

            default:
                mobile = getString(R.string.app_data_usage_placeholder);
                wifi = getString(R.string.app_data_usage_placeholder);
        }
        return new String[]{mobile, wifi};
    }

    private Float getTranslation(View view) {
        float translation;
        switch (view.getId()) {
            case R.id.view_mon:
                if (mMobileMon.getProgress() > 90 || mWifiMon.getProgress() > 90) {
                    translation = 230;
                } else if (mMobileMon.getProgress() < 20 || mWifiMon.getProgress() > 20) {
                    translation = 230;
                } else {
                    translation = 100;
                }
                break;

            case R.id.view_tue:
                if (mMobileTue.getProgress() > 90 || mWifiTue.getProgress() > 90) {
                    translation = 230;
                } else {
                    translation = 100;
                }
                break;

            case R.id.view_wed:
                if (mMobileWed.getProgress() > 90 || mWifiWed.getProgress() > 90) {
                    translation = -230;
                } else {
                    translation = 100;
                }
                break;

            case R.id.view_thurs:
                if (mMobileThurs.getProgress() > 90 || mWifiThurs.getProgress() > 90) {
                    translation = 230;
                } else {
                    translation = 100;
                }
                break;

            case R.id.view_fri:
                if (mMobileFri.getProgress() > 90 || mWifiFri.getProgress() > 90) {
                    translation = 230;
                } else {
                    translation = 100;
                }
                break;

            case R.id.view_sat:
                if (mMobileSat.getProgress() > 90 || mWifiSat.getProgress() > 90) {
                    translation = 230;
                } else if (mMobileSat.getProgress() < 20 || mWifiSat.getProgress() > 20) {
                    translation = -230;
                } else {
                    translation = 100;
                }
                break;

            case R.id.view_sun:
                if (mMobileSun.getProgress() > 90 || mWifiSun.getProgress() > 90) {
                    translation = 230;
                } else {
                    translation = 100;
                }
                break;

            default:
                translation = 100;
        }
        return translation;
    }

    private static class UpdateOverview extends AsyncTask<Object, Object, List<OverviewModel>> {
        private int mode;

        public UpdateOverview(int mode) {
            this.mode = mode;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Log.d(TAG, "onPreExecute: update overview");
            if (mode == MODE_LOAD_OVERVIEW) {
                mOverview.animate().alpha(0.0f);
                mOverviewLoading.setAlpha(1.0f);
            } else if (mode == MODE_REFRESH_OVERVIEW) {
                mOverviewLoading.setAlpha(0.0f);
                mOverview.setAlpha(1.0f);
            }
        }

        @Override
        protected List<OverviewModel> doInBackground(Object[] objects) {
            Calendar calendar = Calendar.getInstance();
            int day = calendar.get(Calendar.DAY_OF_WEEK);
            int[] days;
            List<OverviewModel> list = null;
            switch (day) {
                case Calendar.MONDAY:
                    days = new int[]{Calendar.MONDAY};
                    break;

                case Calendar.TUESDAY:
                    days = new int[]{Calendar.MONDAY, Calendar.TUESDAY};
                    break;

                case Calendar.WEDNESDAY:
                    days = new int[]{Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY};
                    break;

                case Calendar.THURSDAY:
                    days = new int[]{Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY};
                    break;

                case Calendar.FRIDAY:
                    days = new int[]{Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
                            Calendar.THURSDAY, Calendar.FRIDAY};
                    break;

                case Calendar.SATURDAY:
                    days = new int[]{Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
                            Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY};
                    break;

                case Calendar.SUNDAY:
                    days = new int[]{Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
                            Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY};
                    break;

                default:
                    days = new int[]{0};
                    break;
            }

            try {
                list = updateOverview(mContext, days);
                mList = list;

                Log.d(TAG, "doInBackground: " + mList.size());

            } catch (ParseException e) {
                e.printStackTrace();
            } catch (RemoteException e) {
                e.printStackTrace();
            }

            return list;
        }

        @Override
        protected void onPostExecute(List<OverviewModel> list) {
            super.onPostExecute(list);
            mOverview.animate().alpha(1.0f);
            mOverviewLoading.animate().alpha(0.0f);
            refreshOverview();
        }
    }

    private void checkDataQuota() {
        if (preferences.getBoolean("smart_data_allocation", false)) {
            long scheduledTime = preferences.getLong(DATA_QUOTA_SCHEDULED_RESET, 0);
            long performedTime = preferences.getLong(DATA_QUOTA_PERFORMED_RESET, 0);

            if (scheduledTime > 0 &&
                    scheduledTime < System.currentTimeMillis() &&
                    performedTime < scheduledTime) {
                Log.d(TAG, "checkDataQuota: Performing a quota reset");

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