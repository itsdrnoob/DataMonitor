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

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.RemoteException;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.drnoob.datamonitor.R;
import com.drnoob.datamonitor.Widget.DataUsageWidget;
import com.drnoob.datamonitor.adapters.data.OverviewModel;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.skydoves.progressview.ProgressView;

import org.jetbrains.annotations.NotNull;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import static com.drnoob.datamonitor.core.Values.DATA_LIMIT;
import static com.drnoob.datamonitor.core.Values.DATA_RESET;
import static com.drnoob.datamonitor.core.Values.DATA_RESET_DAILY;
import static com.drnoob.datamonitor.core.Values.DATA_RESET_MONTHLY;
import static com.drnoob.datamonitor.core.Values.DATA_TYPE;
import static com.drnoob.datamonitor.core.Values.LIMIT;
import static com.drnoob.datamonitor.core.Values.SESSION_TODAY;
import static com.drnoob.datamonitor.utils.NetworkStatsHelper.formatData;
import static com.drnoob.datamonitor.utils.NetworkStatsHelper.getDeviceMobileDataUsage;
import static com.drnoob.datamonitor.utils.NetworkStatsHelper.getDeviceWifiDataUsage;
import static com.drnoob.datamonitor.utils.NetworkStatsHelper.updateOverview;


public class HomeFragment extends Fragment {

    private static final String TAG = HomeFragment.class.getSimpleName();
    private RelativeLayout mSetupDataPlan;
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
    private static ConstraintLayout mOverview;
    private static ConstraintLayout mOverviewLoading;
    private static ImageView mRefreshOverview;
    private static Context mContext;
    private static List<OverviewModel> mList = new ArrayList<>();

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        mMobileDataUsage = view.findViewById(R.id.mobile_data_usage);
        mMobileDataSent = view.findViewById(R.id.mobile_data_sent);
        mMobileDataReceived = view.findViewById(R.id.mobile_data_received);
        mWifiDataUsage = view.findViewById(R.id.wifi_data_usage);
        mWifiDataSent = view.findViewById(R.id.wifi_data_sent);
        mWifiFataReceived = view.findViewById(R.id.wifi_data_received);
        mMobileDataUsageToday = view.findViewById(R.id.data_usage_mobile_today);
        mWifiUsageToday = view.findViewById(R.id.data_usage_wifi_today);

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

        mRefreshOverview = view.findViewById(R.id.overview_refresh);

        updateData();
        refreshOverview();

        if (PreferenceManager.getDefaultSharedPreferences(getContext())
                .getFloat(DATA_LIMIT, -1) > 0) {
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
                refreshOverview();
            }
        });

        mSetupDataPlan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
                    dataLimitInput.setText(PreferenceManager.getDefaultSharedPreferences(getContext())
                            .getString(LIMIT, null));
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
                            if (dataTypeSwitcher.getTabAt(0).isSelected()) {
                                dataLimit = dataLimit;
                            } else {
                                dataLimit = dataLimit * 1024f;
                            }
                            int dataType = dataTypeSwitcher.getSelectedTabPosition();
                            if (dataReset.getCheckedRadioButtonId() == R.id.daily) {
                                PreferenceManager.getDefaultSharedPreferences(getContext()).edit().putString(DATA_RESET, DATA_RESET_DAILY).apply();
                            } else {
                                PreferenceManager.getDefaultSharedPreferences(getContext()).edit().putString(DATA_RESET, DATA_RESET_MONTHLY).apply();
                            }
                            PreferenceManager.getDefaultSharedPreferences(getContext()).edit().putFloat(DATA_LIMIT, dataLimit).apply();
                            PreferenceManager.getDefaultSharedPreferences(getContext()).edit().putString(LIMIT, dataLimitInput.getText().toString()).apply();
                            PreferenceManager.getDefaultSharedPreferences(getContext()).edit().putInt(DATA_TYPE, dataType).apply();
                            snackbar = Snackbar.make(getActivity().findViewById(R.id.main_root),
                                    getString(R.string.data_plan_saved), Snackbar.LENGTH_SHORT)
                                    .setAnchorView(getActivity().findViewById(R.id.bottomNavigationView));
                            mSetupDataPlan.setVisibility(View.GONE);
                            dialog.dismiss();
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
            }
        });

        mMobileDataUsageToday.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        mWifiUsageToday.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });


        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateData();
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
            mobile = getDeviceMobileDataUsage(getContext(), SESSION_TODAY);
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


        } catch (ParseException e) {
            e.printStackTrace();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private static void refreshOverview() {
        if (isOverviewAvailable()) {
            mOverviewLoading.setAlpha(0.0f);
            mOverview.setAlpha(1.0f);

            OverviewModel model = null;

            for (int i = 0; i < mList.size(); i++) {
                model = mList.get(i);
                switch (i) {
                    case 0:
                        mMobileMon.setProgress((model.getTotalMobile() / 25) + 2);  // 500 MB is 20 in the progressBar, so divided by 25. Added 2 to fix margin issue
                        mWifiMon.setProgress((model.getTotalWifi() / 25) + 2);
                        break;

                    case 1:
                        mMobileTue.setProgress((model.getTotalMobile() / 25) + 2);
                        mWifiTue.setProgress((model.getTotalWifi() / 25) + 2);
                        break;

                    case 2:
                        mMobileWed.setProgress((model.getTotalMobile() / 25) + 2);
                        mWifiWed.setProgress((model.getTotalWifi() / 25) + 2);
                        break;

                    case 3:
                        mMobileThurs.setProgress((model.getTotalMobile() / 25) + 2);
                        mWifiThurs.setProgress((model.getTotalWifi() / 25) + 2);
                        break;

                    case 4:
                        mMobileFri.setProgress((model.getTotalMobile() / 25) + 2);
                        mWifiFri.setProgress((model.getTotalWifi() / 25) + 2);
                        break;

                    case 5:
                        mMobileSat.setProgress((model.getTotalMobile() / 25) + 2);
                        mWifiSat.setProgress((model.getTotalWifi() / 25) + 2);
                        break;

                    case 6:
                        mMobileSun.setProgress((model.getTotalMobile() / 25) + 2);
                        mWifiSun.setProgress((model.getTotalWifi() / 25) + 2);
                        break;
                }
            }
        } else {
            UpdateOverview updateOverview = new UpdateOverview();
            updateOverview.execute();
        }
    }

    private static boolean isOverviewAvailable() {
        return mList.size() > 0;
    }

    private static class UpdateOverview extends AsyncTask<Object, Object, List<OverviewModel>> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Log.e(TAG, "onPreExecute: update overview");
            mOverview.animate().alpha(0.0f);
            mOverviewLoading.setAlpha(1.0f);
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
}