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
import android.annotation.SuppressLint;
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
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.preference.PreferenceManager;

import com.drnoob.datamonitor.R;
import com.drnoob.datamonitor.Widget.DataUsageWidget;
import com.drnoob.datamonitor.adapters.data.FragmentViewModel;
import com.drnoob.datamonitor.adapters.data.OverviewModel;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.skydoves.progressview.ProgressView;
import com.skydoves.progressview.ProgressViewOrientation;

import org.jetbrains.annotations.NotNull;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import static com.drnoob.datamonitor.core.Values.DAILY_DATA_HOME_ACTION;
import static com.drnoob.datamonitor.core.Values.DATA_LIMIT;
import static com.drnoob.datamonitor.core.Values.DATA_RESET;
import static com.drnoob.datamonitor.core.Values.DATA_RESET_DAILY;
import static com.drnoob.datamonitor.core.Values.DATA_RESET_MONTHLY;
import static com.drnoob.datamonitor.core.Values.DATA_TYPE;
import static com.drnoob.datamonitor.core.Values.DATA_USAGE_SESSION;
import static com.drnoob.datamonitor.core.Values.DATA_USAGE_TYPE;
import static com.drnoob.datamonitor.core.Values.LIMIT;
import static com.drnoob.datamonitor.core.Values.SESSION_TODAY;
import static com.drnoob.datamonitor.core.Values.TYPE_MOBILE_DATA;
import static com.drnoob.datamonitor.core.Values.TYPE_WIFI;
import static com.drnoob.datamonitor.utils.NetworkStatsHelper.formatData;
import static com.drnoob.datamonitor.utils.NetworkStatsHelper.getDeviceMobileDataUsage;
import static com.drnoob.datamonitor.utils.NetworkStatsHelper.getDeviceWifiDataUsage;
import static com.drnoob.datamonitor.utils.NetworkStatsHelper.updateOverview;
import static com.drnoob.datamonitor.utils.VibrationUtils.hapticMajor;
import static com.drnoob.datamonitor.utils.VibrationUtils.hapticMinor;


public class HomeFragment extends Fragment implements View.OnLongClickListener {

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
    private LinearLayout mMonView, mTueView, mWedView, mThursView, mFriView, mSatView, mSunView;
    private LinearLayout mQuickView;
    private TextView mQuickViewMobile, mQuickViewWifi;
    private static ConstraintLayout mOverview;
    private static ConstraintLayout mOverviewLoading;
    private static ImageView mRefreshOverview;
    private static Context mContext;
    private static List<OverviewModel> mList = new ArrayList<>();
    private boolean openQuickView = false;

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

        mQuickView = view.findViewById(R.id.overview_quick_view);
        mQuickViewMobile = view.findViewById(R.id.mobile_data_quick_view);
        mQuickViewWifi = view.findViewById(R.id.wifi_quick_view);

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

        NavController controller = Navigation.findNavController(getActivity(), R.id.main_nav_host_fragment);
        FragmentViewModel viewModel = new ViewModelProvider(getActivity()).get(FragmentViewModel.class);

        mMobileDataUsageToday.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (controller != null) {
                    Bundle bundle = new Bundle();
                    bundle.putInt(DATA_USAGE_TYPE, TYPE_MOBILE_DATA);
                    bundle.putBoolean(DAILY_DATA_HOME_ACTION, true);
                    viewModel.setCurrentType(TYPE_MOBILE_DATA);
                    controller.navigate(R.id.bottom_menu_app_data_usage, bundle);
                }
            }
        });

        mWifiUsageToday.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (controller != null) {
                    Bundle bundle = new Bundle();
                    bundle.putInt(DATA_USAGE_TYPE, TYPE_WIFI);
                    bundle.putBoolean(DAILY_DATA_HOME_ACTION, true);
                    viewModel.setCurrentType(TYPE_WIFI);
                    controller.navigate(R.id.bottom_menu_app_data_usage, bundle);
                }
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
        else {
            UpdateOverview updateOverview = new UpdateOverview();
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
        hapticMajor(getContext());
        if (isOverviewAvailable()) {
            try {
                String[] dataUsage = getDataUsage(view);
                mQuickViewMobile.setText(dataUsage[0]);
                mQuickViewWifi.setText(dataUsage[1]);
                mQuickView.setVisibility(View.VISIBLE);
            }
            catch (NullPointerException e) {
                e.printStackTrace();
            }
        }
        view.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (isOpenQuickView()) {
                    if (motionEvent.getAction() == MotionEvent.ACTION_UP ||
                            motionEvent.getAction() == MotionEvent.ACTION_CANCEL) {
                        hapticMinor(getContext());
                        mQuickView.setVisibility(View.INVISIBLE);
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
                }
                else if (mMobileMon.getProgress() < 20 || mWifiMon.getProgress() > 20) {
                    translation = 230;
                }
                else {
                    translation = 100;
                }
                break;

            case R.id.view_tue:
                if (mMobileTue.getProgress() > 90 || mWifiTue.getProgress() > 90) {
                    translation = 230;
                }
                else {
                    translation = 100;
                }
                break;

            case R.id.view_wed:
                if (mMobileWed.getProgress() > 90 || mWifiWed.getProgress() > 90) {
                    translation = -230;
                }
                else {
                    translation = 100;
                }
                break;

            case R.id.view_thurs:
                if (mMobileThurs.getProgress() > 90 || mWifiThurs.getProgress() > 90) {
                    translation = 230;
                }
                else {
                    translation = 100;
                }
                break;

            case R.id.view_fri:
                if (mMobileFri.getProgress() > 90 || mWifiFri.getProgress() > 90) {
                    translation = 230;
                }
                else {
                    translation = 100;
                }
                break;

            case R.id.view_sat:
                if (mMobileSat.getProgress() > 90 || mWifiSat.getProgress() > 90) {
                    translation = 230;
                }
                else if (mMobileSat.getProgress() < 20 || mWifiSat.getProgress() > 20) {
                    translation = -230;
                }
                else {
                    translation = 100;
                }
                break;

            case R.id.view_sun:
                if (mMobileSun.getProgress() > 90 || mWifiSun.getProgress() > 90) {
                    translation = 230;
                }
                else {
                    translation = 100;
                }
                break;

            default:
                translation = 100;
        }
        return translation;
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