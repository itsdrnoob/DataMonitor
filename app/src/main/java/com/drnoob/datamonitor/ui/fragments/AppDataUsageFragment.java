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

import static android.app.Activity.RESULT_OK;
import static com.drnoob.datamonitor.core.Values.ADD_CUSTOM_SESSION_FRAGMENT;
import static com.drnoob.datamonitor.core.Values.DAILY_DATA_HOME_ACTION;
import static com.drnoob.datamonitor.core.Values.DATA_RESET;
import static com.drnoob.datamonitor.core.Values.DATA_RESET_CUSTOM;
import static com.drnoob.datamonitor.core.Values.DATA_RESET_DATE;
import static com.drnoob.datamonitor.core.Values.DATA_USAGE_SESSION;
import static com.drnoob.datamonitor.core.Values.DATA_USAGE_TYPE;
import static com.drnoob.datamonitor.core.Values.EXTRA_IS_WEEK_DAY_VIEW;
import static com.drnoob.datamonitor.core.Values.EXTRA_WEEK_DAY;
import static com.drnoob.datamonitor.core.Values.GENERAL_FRAGMENT_ID;
import static com.drnoob.datamonitor.core.Values.SESSION_ALL_TIME;
import static com.drnoob.datamonitor.core.Values.SESSION_CUSTOM;
import static com.drnoob.datamonitor.core.Values.SESSION_CUSTOM_FILTER;
import static com.drnoob.datamonitor.core.Values.SESSION_LAST_MONTH;
import static com.drnoob.datamonitor.core.Values.SESSION_THIS_MONTH;
import static com.drnoob.datamonitor.core.Values.SESSION_THIS_YEAR;
import static com.drnoob.datamonitor.core.Values.SESSION_TODAY;
import static com.drnoob.datamonitor.core.Values.SESSION_YESTERDAY;
import static com.drnoob.datamonitor.core.Values.TYPE_MOBILE_DATA;
import static com.drnoob.datamonitor.core.Values.TYPE_WIFI;
import static com.drnoob.datamonitor.ui.activities.MainActivity.getRefreshAppDataUsage;
import static com.drnoob.datamonitor.ui.activities.MainActivity.isDataLoading;
import static com.drnoob.datamonitor.ui.activities.MainActivity.mSystemAppsList;
import static com.drnoob.datamonitor.ui.activities.MainActivity.mUserAppsList;
import static com.drnoob.datamonitor.ui.activities.MainActivity.setRefreshAppDataUsage;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.util.Pair;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.drnoob.datamonitor.R;
import com.drnoob.datamonitor.adapters.AppDataUsageAdapter;
import com.drnoob.datamonitor.adapters.data.AppDataUsageModel;
import com.drnoob.datamonitor.adapters.data.FragmentViewModel;
import com.drnoob.datamonitor.ui.activities.ContainerActivity;
import com.drnoob.datamonitor.ui.activities.MainActivity;
import com.drnoob.datamonitor.utils.NetworkStatsHelper;
import com.drnoob.datamonitor.utils.VibrationUtils;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AppDataUsageFragment extends Fragment {
    private static final String TAG = AppDataUsageFragment.class.getSimpleName();
    public static RecyclerView mAppsView;
    public static AppDataUsageAdapter mAdapter;
    public static List<AppDataUsageModel> mList = new ArrayList<>();
    public static List<AppDataUsageModel> mSystemList = new ArrayList<>();
    private static LinearLayout mLoading;
    private static Context mContext;
    private static Activity mActivity;
    private static SwipeRefreshLayout mDataRefresh;
    private static TextView mEmptyList;
    private FragmentViewModel viewModel;
    private ExtendedFloatingActionButton mFilter;
    private static TextView mTotalUsage;
    private static boolean fromHome;
    private static boolean isWeekDayView;
    private static String totalDataUsage;
    private static int selectedSession, selectedType;
    private ActivityResultLauncher<Intent> customSessionLauncher;

    public static MutableLiveData<Pair<Long, Long>> customFilter = new MutableLiveData<>();
    public static MutableLiveData<String> customFilterDate = new MutableLiveData<>();
    public static MutableLiveData<Pair<Long, Long>> customFilterDateMillis = new MutableLiveData<>();
    public static MutableLiveData<Map<String, Integer>> customFilterTime = new MutableLiveData<>();
    public static Boolean shouldShowTime = false;

    public AppDataUsageFragment() {

    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        customSessionLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Long startMillis = result.getData().getLongExtra("start", 0);
                        Long endMillis = result.getData().getLongExtra("end", 0);
                        String date = result.getData().getStringExtra("date");
                        customFilter.postValue(new Pair<>(startMillis, endMillis));
                        customFilterDate.postValue(date);
                    }
                }
        );
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mContext = context;
        mActivity = getActivity();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_app_data_usage, container, false);

        viewModel = new ViewModelProvider(getActivity()).get(FragmentViewModel.class);

        mAppsView = view.findViewById(R.id.app_data_usage_recycler);
        mLoading = view.findViewById(R.id.layout_list_loading);
        mDataRefresh = view.findViewById(R.id.refresh_data_usage);
        mEmptyList = view.findViewById(R.id.empty_list);
        mTotalUsage = view.findViewById(R.id.current_session_total);
        mFilter = view.findViewById(R.id.filter_app_usage);

        mAdapter = new AppDataUsageAdapter(mList, mContext);
        mAdapter.setActivity(getActivity());

        int session = getActivity().getIntent().getIntExtra(DATA_USAGE_SESSION, SESSION_TODAY);
        int type = getActivity().getIntent().getIntExtra(DATA_USAGE_TYPE, TYPE_MOBILE_DATA);
        fromHome = getActivity().getIntent().getBooleanExtra(DAILY_DATA_HOME_ACTION, false);
        isWeekDayView = getActivity().getIntent().getBooleanExtra(EXTRA_IS_WEEK_DAY_VIEW, false);

        if (getActivity().getIntent() != null) {
            if (fromHome) {
                type = getActivity().getIntent().getIntExtra(DATA_USAGE_TYPE, TYPE_MOBILE_DATA);
                setType(type);
                refreshData();
//                mTopBar.setVisibility(View.GONE);
                mFilter.setVisibility(View.GONE);
                mAppsView.setPadding(0, 130, 0, 0);
            }
            else if (isWeekDayView) {
                String weekDay = getActivity().getIntent().getStringExtra(EXTRA_WEEK_DAY);
            }
        }

        setSession(session);
        setType(type);
        mTotalUsage.setText("...");

        Log.d(TAG, "onCreateView: " + getRefreshAppDataUsage() );
        if (getRefreshAppDataUsage()) {
            refreshData();
        }

        mList = mUserAppsList;
        mSystemList = mSystemAppsList;

        if (!MainActivity.isDataLoading()) {
            mLoading.setAlpha(0.0f);
            mAppsView.setAlpha(1.0f);
            onDataLoaded(getContext());
        }
        else {
            mDataRefresh.setRefreshing(true);
        }

        mFilter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isDataLoading()) {
                    return;
                }
                BottomSheetDialog dialog = new BottomSheetDialog(getContext(), R.style.BottomSheet);
                View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.layout_app_usage_filter, null);

                ChipGroup sessionGroup = dialogView.findViewById(R.id.session_group);
                ChipGroup typeGroup = dialogView.findViewById(R.id.type_group);

                ConstraintLayout footer = dialogView.findViewById(R.id.footer);
                TextView cancel = footer.findViewById(R.id.cancel);
                TextView ok = footer.findViewById(R.id.ok);

                Chip sessionCurrentPlan = sessionGroup.findViewById(R.id.session_current_plan);
                Chip sessionCustom = sessionGroup.findViewById(R.id.session_custom);

                if (PreferenceManager.getDefaultSharedPreferences(getContext())
                        .getString(DATA_RESET, "null")
                        .equals(DATA_RESET_CUSTOM)) {
                    sessionCurrentPlan.setVisibility(View.VISIBLE);
                }
                else {
                    sessionCurrentPlan.setVisibility(View.GONE);
                }

                String sessionText = customFilterDate.getValue() == null ? getString(R.string.add_custom_session)
                        : customFilterDate.getValue();
                sessionCustom.setText(sessionText);

                customFilterDate.observe(getActivity(), new Observer<String>() {
                    @Override
                    public void onChanged(String s) {
                        String sessionText = s == null ? getString(R.string.add_custom_session) : s;
                        sessionCustom.setText(sessionText);
                    }
                });

                sessionCustom.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        sessionCustom.setChecked(true);
                        customSessionLauncher.launch(new Intent(requireActivity(), ContainerActivity.class)
                                .putExtra(GENERAL_FRAGMENT_ID, ADD_CUSTOM_SESSION_FRAGMENT));
                    }
                });

                sessionGroup.setOnCheckedStateChangeListener(new ChipGroup.OnCheckedStateChangeListener() {
                    @Override
                    public void onCheckedChanged(@NonNull ChipGroup group, @NonNull List<Integer> checkedIds) {
                        if (!PreferenceManager.getDefaultSharedPreferences(getContext())
                                .getBoolean("disable_haptics", false)) {
                            VibrationUtils.hapticMinor(getContext());
                        }
                    }
                });

                typeGroup.setOnCheckedStateChangeListener(new ChipGroup.OnCheckedStateChangeListener() {
                    @Override
                    public void onCheckedChanged(@NonNull ChipGroup group, @NonNull List<Integer> checkedIds) {
                        if (!PreferenceManager.getDefaultSharedPreferences(getContext())
                                .getBoolean("disable_haptics", false)) {
                            VibrationUtils.hapticMinor(getContext());
                        }
                    }
                });

                switch (getSession()) {
                    case SESSION_TODAY:
                        sessionGroup.check(R.id.session_today);
                        break;

                    case SESSION_YESTERDAY:
                        sessionGroup.check(R.id.session_yesterday);
                        break;

                    case SESSION_THIS_MONTH:
                        sessionGroup.check(R.id.session_this_month);
                        break;

                    case SESSION_LAST_MONTH:
                        sessionGroup.check(R.id.session_last_month);
                        break;

                    case SESSION_THIS_YEAR:
                        sessionGroup.check(R.id.session_this_year);
                        break;

                    case SESSION_ALL_TIME:
                        sessionGroup.check(R.id.session_all_time);
                        break;

                    case SESSION_CUSTOM:
                        sessionGroup.check(R.id.session_current_plan);
                        break;

                    case SESSION_CUSTOM_FILTER:
                        sessionGroup.check(R.id.session_custom);

                }

                switch (getType()) {
                    case TYPE_MOBILE_DATA:
                        typeGroup.check(R.id.type_mobile);
                        break;

                    case TYPE_WIFI:
                        typeGroup.check(R.id.type_wifi);
                        break;
                }

                cancel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();
                    }
                });

                ok.setOnClickListener(new View.OnClickListener() {
                    @SuppressLint("NonConstantResourceId")
                    @Override
                    public void onClick(View v) {
                        switch (sessionGroup.getCheckedChipId()) {
                            case R.id.session_yesterday:
                                selectedSession = SESSION_YESTERDAY;
                                break;

                            case R.id.session_this_month:
                                selectedSession = SESSION_THIS_MONTH;
                                break;

                            case R.id.session_last_month:
                                selectedSession = SESSION_LAST_MONTH;
                                break;

                            case R.id.session_this_year:
                                selectedSession = SESSION_THIS_YEAR;
                                break;

                            case R.id.session_all_time:
                                selectedSession = SESSION_ALL_TIME;
                                break;

                            case R.id.session_current_plan:
                                selectedSession = SESSION_CUSTOM;
                                break;

                            case R.id.session_custom:
                                selectedSession = SESSION_CUSTOM_FILTER;
                                break;

                            case R.id.session_today:

                            default:
                                selectedSession = SESSION_TODAY;
                                break;
                        }

                        switch (typeGroup.getCheckedChipId()) {
                            case R.id.type_wifi:
                                selectedType = TYPE_WIFI;
                                break;

                            case R.id.type_mobile:

                            default:
                                selectedType = TYPE_MOBILE_DATA;
                                break;
                        }

                        if (!MainActivity.isDataLoading()) {
                            refreshData();
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

            }
        });

        mDataRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refreshData();
            }
        });

         /*
        Shrink or expand the FAB according to user scroll
         */
        mAppsView.setOnScrollChangeListener(new View.OnScrollChangeListener() {
            @Override
            public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                if (oldScrollY < -15 && mFilter.isExtended()) {
                    mFilter.shrink();
                }
                else if (oldScrollY > 15 && !mFilter.isExtended()) {
                    mFilter.extend();
                }
                else if (mAppsView.computeVerticalScrollOffset() == 0 && !mFilter.isExtended()) {
                    mFilter.extend();
                }
            }
        });


        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mList.size() > 0) {
            setSession(mList.get(0).getSession());
            setType(mList.get(0).getType());
        }
        else {
            if (viewModel.getCurrentSession().getValue() != null &&
                    viewModel.getCurrentType().getValue() != null) {
                setSession(viewModel.getCurrentSession().getValue());
                setType(viewModel.getCurrentType().getValue());
            }

        }
        if (!PreferenceManager.getDefaultSharedPreferences(requireContext())
                .getString(DATA_RESET, "null")
                .equals(DATA_RESET_CUSTOM)) {
            if (getSession() == SESSION_CUSTOM) {
                setSession(SESSION_TODAY);
                refreshData();
            }
        }
    }

    @Override
    public void onPause() {
        viewModel.setCurrentSession(getSession());
        viewModel.setCurrentType(getType());
        super.onPause();
    }

    public static Context getAppContext() {
        return mContext;
    }

    private static void refreshData() {
        mLoading.animate().alpha(1.0f);
        mAppsView.animate().alpha(0.0f);
        mEmptyList.animate().alpha(0.0f);
        mDataRefresh.setRefreshing(true);
        mAppsView.removeAllViews();
        mList.clear();
        mSystemList.clear();
        totalDataUsage = "";
        mTotalUsage.setText("...");


        MainActivity.LoadData loadData = new MainActivity.LoadData(mContext, getSession(),
                getType());
        if (!isDataLoading()) {
            loadData.execute();
        }

    }

    public static void onDataLoaded(Context context) {
        try {
            totalDataUsage = getTotalDataUsage(context);
            mTotalUsage.setText(context.getString(R.string.total_usage, totalDataUsage));

        }
        catch (ParseException | RemoteException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "onDataLoaded: " + mSystemList.size() + " system");
        Log.d(TAG, "onDataLoaded: " + mList.size() + " user");
        mAdapter = new AppDataUsageAdapter(mList, mContext);
        mAdapter.setActivity(mActivity);
        mAdapter.setFromHome(fromHome);
        mAppsView.setAdapter(mAdapter);
        mAppsView.setLayoutManager(new LinearLayoutManager(mContext));
        mLoading.animate().alpha(0.0f);
        mAppsView.animate().alpha(1.0f);
        mDataRefresh.setRefreshing(false);
        if (mList.size() <= 0) {
            mEmptyList.animate().alpha(1.0f);
        }
        else {
            setSession(mList.get(0).getSession());
            setType(mList.get(0).getType());
        }
        if (!fromHome) {
            setRefreshAppDataUsage(false);
        }
    }

    private static String getTotalDataUsage(Context context) throws ParseException, RemoteException {
        int date = PreferenceManager.getDefaultSharedPreferences(context).getInt(DATA_RESET_DATE, -1);
        String totalUsage;
        int type = getType();
        if (type == TYPE_MOBILE_DATA) {
            totalUsage = NetworkStatsHelper.formatData(
                    NetworkStatsHelper.getTotalAppMobileDataUsage(context, getSession(), date)[0],
                    NetworkStatsHelper.getTotalAppMobileDataUsage(context, getSession(), date)[1]
            )[2];
        }
        else if (type == TYPE_WIFI) {
            totalUsage = NetworkStatsHelper.formatData(
                    NetworkStatsHelper.getTotalAppWifiDataUsage(context, getSession())[0],
                    NetworkStatsHelper.getTotalAppWifiDataUsage(context, getSession())[1]
            )[2];
        }
        else {
            totalUsage = context.getString(R.string.label_unknown);
        }
        return totalUsage;
    }

    public static int getSession() {
        if (selectedSession == 0) {
            selectedSession = SESSION_TODAY;
        }

        return selectedSession;
    }

    public static int getType() {
        if (selectedType == 0) {
            selectedType = TYPE_MOBILE_DATA;
        }
        return selectedType;
    }

    private static void setSession(int session) {
        selectedSession = session;
    }

    private static void setType(int type) {
        selectedType = type;
    }

}
