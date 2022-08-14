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

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.ArrayList;
import java.util.List;

import static com.drnoob.datamonitor.core.Values.*;
import static com.drnoob.datamonitor.ui.activities.MainActivity.mSystemAppsList;
import static com.drnoob.datamonitor.ui.activities.MainActivity.mUserAppsList;

public class AppDataUsageFragment extends Fragment {
    private static final String TAG = AppDataUsageFragment.class.getSimpleName();
    public static RecyclerView mAppsView;
    public static AppDataUsageAdapter mAdapter;
    public static List<AppDataUsageModel> mList = new ArrayList<>();
    public static List<AppDataUsageModel> mSystemList = new ArrayList<>();
    private static LinearLayout mLoading;
    private static Context mContext;
    private static SwipeRefreshLayout mDataRefresh;
    private static TextView mSession, mType, mEmptyList;
    public static LinearLayout mTopBar;
    private FragmentViewModel viewModel;

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
        mTopBar = view.findViewById(R.id.top_bar);
        mEmptyList = view.findViewById(R.id.empty_list);

        mAdapter = new AppDataUsageAdapter(mList, mContext);

        int session = getActivity().getIntent().getIntExtra(DATA_USAGE_SESSION, SESSION_TODAY);
        int type = getActivity().getIntent().getIntExtra(DATA_USAGE_TYPE, TYPE_MOBILE_DATA);

        if (getArguments() != null) {
            boolean fromHome = getArguments().getBoolean(DAILY_DATA_HOME_ACTION, false);
            if (fromHome) {
                type = getArguments().getInt(DATA_USAGE_TYPE, TYPE_MOBILE_DATA);
                setType(type);
                refreshData();
            }
        }

        setSession(session);
        setType(type);

        mList = mUserAppsList;
        mSystemList = mSystemAppsList;

        if (!MainActivity.isDataLoading()) {
            mLoading.setAlpha(0.0f);
            mAppsView.setAlpha(1.0f);
            onDataLoaded();
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
                BottomSheetDialog dialog = new BottomSheetDialog(getContext(), R.style.BottomSheet);
                View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.data_usage_session, null);

                RadioGroup sessions = dialogView.findViewById(R.id.session_group);
                ConstraintLayout footer = dialogView.findViewById(R.id.footer);
                TextView cancel = footer.findViewById(R.id.cancel);
                TextView ok = footer.findViewById(R.id.ok);

                switch (getSession()) {
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
                dialog.show();
            }
        });

        mType.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BottomSheetDialog dialog = new BottomSheetDialog(getContext(), R.style.BottomSheet);
                View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.data_usage_type, null);

                RadioGroup types = dialogView.findViewById(R.id.type_group);
                ConstraintLayout footer = dialogView.findViewById(R.id.footer);
                TextView cancel = footer.findViewById(R.id.cancel);
                TextView ok = footer.findViewById(R.id.ok);

                switch (getType()) {
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
        super.onPause();
        viewModel.setCurrentSession(getSession());
        viewModel.setCurrentType(getType());
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

        MainActivity.LoadData loadData = new MainActivity.LoadData(mContext, getSession(), getType());
        loadData.execute();

    }

    public static void onDataLoaded() {
        Log.d(TAG, "onDataLoaded: " + mSystemList.size() + " system");
        Log.d(TAG, "onDataLoaded: " + mList.size() + " user");
        mAdapter = new AppDataUsageAdapter(mList, mContext);
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
    }

    public static int getSession() {
        int session = 0;
        String selectedSession = mSession.getText().toString();
        switch (selectedSession) {
            case USAGE_SESSION_TODAY:
                session = SESSION_TODAY;
                break;

            case USAGE_SESSION_YESTERDAY:
                session = SESSION_YESTERDAY;
                break;

            case USAGE_SESSION_THIS_MONTH:
                session = SESSION_THIS_MONTH;
                break;

            case USAGE_SESSION_LAST_MONTH:
                session = SESSION_LAST_MONTH;
                break;

            case USAGE_SESSION_THIS_YEAR:
                session = SESSION_THIS_YEAR;
                break;

            case USAGE_SESSION_ALL_TIME:
                session = SESSION_ALL_TIME;
                break;
        }
        return session;
    }

    public static int getType() {
        int type = 0;
        String selectedType = mType.getText().toString();
        switch (selectedType) {
            case USAGE_TYPE_MOBILE_DATA:
                type = TYPE_MOBILE_DATA;
                break;

            case USAGE_TYPE_WIFI:
                type = TYPE_WIFI;
                break;
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
