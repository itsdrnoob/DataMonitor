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

import static com.drnoob.datamonitor.core.Values.DAILY_DATA_HOME_ACTION;
import static com.drnoob.datamonitor.core.Values.DATA_RESET_DATE;
import static com.drnoob.datamonitor.core.Values.DATA_USAGE_SESSION;
import static com.drnoob.datamonitor.core.Values.DATA_USAGE_TYPE;
import static com.drnoob.datamonitor.core.Values.SESSION_ALL_TIME;
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

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.drnoob.datamonitor.R;
import com.drnoob.datamonitor.adapters.AppDataUsageAdapter;
import com.drnoob.datamonitor.adapters.data.AppDataUsageModel;
import com.drnoob.datamonitor.adapters.data.FragmentViewModel;
import com.drnoob.datamonitor.ui.activities.MainActivity;
import com.drnoob.datamonitor.utils.NetworkStatsHelper;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

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
    private static TextView mSession, mType, mEmptyList;
    public static LinearLayout mTopBar;
    private FragmentViewModel viewModel;
    private static TextView mTotalUsage;
    private static boolean fromHome;

    public AppDataUsageFragment() {

    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        mSession = view.findViewById(R.id.data_usage_session);
        mType = view.findViewById(R.id.data_usage_type);
        mTopBar = view.findViewById(R.id.nested_top_bar);
        mEmptyList = view.findViewById(R.id.empty_list);
        mTotalUsage = view.findViewById(R.id.current_session_total);

        mAdapter = new AppDataUsageAdapter(mList, mContext);
        mAdapter.setActivity(getActivity());

        int session = getActivity().getIntent().getIntExtra(DATA_USAGE_SESSION, SESSION_TODAY);
        int type = getActivity().getIntent().getIntExtra(DATA_USAGE_TYPE, TYPE_MOBILE_DATA);
        fromHome = getActivity().getIntent().getBooleanExtra(DAILY_DATA_HOME_ACTION, false);

        if (getActivity().getIntent() != null) {
            if (fromHome) {
                type = getActivity().getIntent().getIntExtra(DATA_USAGE_TYPE, TYPE_MOBILE_DATA);
                setType(type);
                refreshData();
                mTopBar.setVisibility(View.GONE);
                mAppsView.setPadding(0, 130, 0, 0);
            }
        }
        Log.e(TAG, "onCreateView: " + getRefreshAppDataUsage() );
        if (getRefreshAppDataUsage()) {
            refreshData();
        }

        setSession(session);
        setType(type);
        mTotalUsage.setText("...");

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

        mDataRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refreshData();
            }
        });

        mSession.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isDataLoading()) {
                    return;
                }
                BottomSheetDialog dialog = new BottomSheetDialog(getContext(), R.style.BottomSheet);
                View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.data_usage_session, null);

                RadioGroup sessions = dialogView.findViewById(R.id.session_group);
                ConstraintLayout footer = dialogView.findViewById(R.id.footer);
                TextView cancel = footer.findViewById(R.id.cancel);
                TextView ok = footer.findViewById(R.id.ok);

                switch (getSession(getContext())) {
                    case SESSION_TODAY:
                        sessions.check(R.id.session_today);
                        break;

                    case SESSION_YESTERDAY:
                        sessions.check(R.id.session_yesterday);
                        break;

                    case SESSION_THIS_MONTH:
                        sessions.check(R.id.session_this_month);
                        break;

                    case SESSION_LAST_MONTH:
                        sessions.check(R.id.session_last_month);
                        break;

                    case SESSION_THIS_YEAR:
                        sessions.check(R.id.session_this_year);
                        break;

                    case SESSION_ALL_TIME:
                        sessions.check(R.id.session_all_time);
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
                        String session = null;
                        switch (sessions.getCheckedRadioButtonId()) {
                            case R.id.session_today:
                                session = getString(R.string.label_today);
                                break;

                            case R.id.session_yesterday:
                                session = getString(R.string.label_yesterday);
                                break;

                            case R.id.session_this_month:
                                session = getString(R.string.label_this_month);
                                break;

                            case R.id.session_last_month:
                                session = getString(R.string.label_last_month);
                                break;

                            case R.id.session_this_year:
                                session = getString(R.string.label_this_year);
                                break;

                            case R.id.session_all_time:
                                session = getString(R.string.label_all_time);
                                break;
                        }
                        mSession.setText(session);
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

        mType.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isDataLoading()) {
                    return;
                }
                BottomSheetDialog dialog = new BottomSheetDialog(getContext(), R.style.BottomSheet);
                View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.data_usage_type, null);

                RadioGroup types = dialogView.findViewById(R.id.type_group);
                ConstraintLayout footer = dialogView.findViewById(R.id.footer);
                TextView cancel = footer.findViewById(R.id.cancel);
                TextView ok = footer.findViewById(R.id.ok);

                switch (getType(getContext())) {
                    case TYPE_MOBILE_DATA:
                        types.check(R.id.type_mobile);
                        break;

                    case TYPE_WIFI:
                        types.check(R.id.type_wifi);
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
                        String type = null;
                        switch (types.getCheckedRadioButtonId()) {
                            case R.id.type_mobile:
                                type = getString(R.string.label_mobile_data);
                                break;

                            case R.id.type_wifi:
                                type = getString(R.string.label_wifi);
                                break;
                        }
                        mType.setText(type);

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
    }

    @Override
    public void onPause() {
        viewModel.setCurrentSession(getSession(requireContext()));
        viewModel.setCurrentType(getType(getContext()));
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
        mTotalUsage.setText("...");


        MainActivity.LoadData loadData = new MainActivity.LoadData(mContext, getSession(mContext),
                getType(mContext));
        if (!isDataLoading()) {
            loadData.execute();
        }

    }

    public static void onDataLoaded(Context context) {
        try {
            String totalUsage = getTotalDataUsage(context);
            mTotalUsage.setText(context.getString(R.string.total_usage, totalUsage));

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
        int date = PreferenceManager.getDefaultSharedPreferences(context).getInt(DATA_RESET_DATE, 1);
        String totalUsage;
        int type = getType(context);
        if (type == TYPE_MOBILE_DATA) {
            totalUsage = NetworkStatsHelper.formatData(
                    NetworkStatsHelper.getDeviceMobileDataUsage(context, getSession(context), date)[0],
                    NetworkStatsHelper.getDeviceMobileDataUsage(context, getSession(context), date)[1]
            )[2];
        }
        else if (type == TYPE_WIFI) {
            totalUsage = NetworkStatsHelper.formatData(
                    NetworkStatsHelper.getDeviceWifiDataUsage(context, getSession(context))[0],
                    NetworkStatsHelper.getDeviceWifiDataUsage(context, getSession(context))[1]
            )[2];
        }
        else {
            totalUsage = context.getString(R.string.label_unknown);
        }
        return totalUsage;
    }

    public static int getSession(Context context) {
        int session = 0;
        String selectedSession = mSession.getText().toString();
        String sessionToday = context.getString(R.string.label_today);
        String sessionYesterday = context.getString(R.string.label_yesterday);
        String sessionThisMonth = context.getString(R.string.label_this_month);
        String sessionLastMonth = context.getString(R.string.label_last_month);
        String sessionThisYear = context.getString(R.string.label_this_year);
        String sessionAllTime = context.getString(R.string.label_all_time);

        if (selectedSession.equalsIgnoreCase(sessionToday)) {
            session = SESSION_TODAY;
        }
        else if (selectedSession.equalsIgnoreCase(sessionYesterday)) {
            session = SESSION_YESTERDAY;
        }
        else if (selectedSession.equalsIgnoreCase(sessionThisMonth)) {
            session = SESSION_THIS_MONTH;
        }
        else if (selectedSession.equalsIgnoreCase(sessionLastMonth)) {
            session = SESSION_LAST_MONTH;
        }
        else if (selectedSession.equalsIgnoreCase(sessionThisYear)) {
            session = SESSION_THIS_YEAR;
        }
        else if (selectedSession.equalsIgnoreCase(sessionAllTime)) {
            session = SESSION_ALL_TIME;
        }
        else {
            session = SESSION_TODAY;
        }

        return session;
    }

    public static int getType(Context context) {
        int type = 0;
        String selectedType = mType.getText().toString();
        String mobileData = context.getString(R.string.label_mobile_data);
        String wifi = context.getString(R.string.label_wifi);

        if (selectedType.equalsIgnoreCase(mobileData)) {
            type = TYPE_MOBILE_DATA;
        }
        else if (selectedType.equalsIgnoreCase(wifi)) {
            type = TYPE_WIFI;
        }
        else {
            type = TYPE_MOBILE_DATA;
        }
        return type;
    }

    private static void setSession(int session) {
        switch (session) {
            case SESSION_TODAY:
                mSession.setText(mContext.getString(R.string.label_today));
                break;

            case SESSION_YESTERDAY:
                mSession.setText(mContext.getString(R.string.label_yesterday));
                break;

            case SESSION_THIS_MONTH:
                mSession.setText(mContext.getString(R.string.label_this_month));
                break;

            case SESSION_LAST_MONTH:
                mSession.setText(mContext.getString(R.string.label_last_month));
                break;
            case SESSION_THIS_YEAR:
                mSession.setText(mContext.getString(R.string.label_this_year));
                break;

            case SESSION_ALL_TIME:
                mSession.setText(mContext.getString(R.string.label_all_time));
                break;

        }
    }

    private static void setType(int type) {
        switch (type) {
            case TYPE_MOBILE_DATA:
                mType.setText(mContext.getString(R.string.label_mobile_data));
                break;

            case TYPE_WIFI:
                mType.setText(mContext.getString(R.string.label_wifi));
                break;
        }
    }

}
