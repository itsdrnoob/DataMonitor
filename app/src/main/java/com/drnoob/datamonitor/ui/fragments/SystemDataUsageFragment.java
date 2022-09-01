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
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.drnoob.datamonitor.R;
import com.drnoob.datamonitor.adapters.AppDataUsageAdapter;
import com.drnoob.datamonitor.adapters.data.AppDataUsageModel;
import com.drnoob.datamonitor.core.task.DatabaseHandler;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.text.ParseException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

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
import static com.drnoob.datamonitor.utils.NetworkStatsHelper.getAppMobileDataUsage;
import static com.drnoob.datamonitor.utils.NetworkStatsHelper.getAppWifiDataUsage;
import static com.drnoob.datamonitor.utils.NetworkStatsHelper.getDeviceMobileDataUsage;
import static com.drnoob.datamonitor.utils.NetworkStatsHelper.getDeviceWifiDataUsage;

public class SystemDataUsageFragment extends Fragment {
    private static final String TAG = SystemDataUsageFragment.class.getSimpleName();

    public static RecyclerView mAppsView;
    private static LinearLayout mLoading;
    private static Context mContext;
    private static SwipeRefreshLayout mDataRefresh;
    private static TextView mSession, mType, mEmptyList;
    public static LinearLayout mTopBar;
    private static List<AppDataUsageModel> mList;
    private static AppDataUsageAdapter mAdapter;

    public SystemDataUsageFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mContext = context;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_system_data_usage, container, false);

        mAppsView = view.findViewById(R.id.app_data_usage_recycler);
        mLoading = view.findViewById(R.id.layout_list_loading);
        mDataRefresh = view.findViewById(R.id.refresh_data_usage);
        mSession = view.findViewById(R.id.data_usage_session);
        mType = view.findViewById(R.id.data_usage_type);
        mTopBar = view.findViewById(R.id.top_bar);
        mEmptyList = view.findViewById(R.id.empty_list);
        mList = AppDataUsageFragment.mSystemList;
        mAdapter = new AppDataUsageAdapter(mList, mContext);
        mAppsView = view.findViewById(R.id.app_data_usage_recycler);


        int session = getActivity().getIntent().getIntExtra(DATA_USAGE_SESSION, SESSION_TODAY);
        int type = getActivity().getIntent().getIntExtra(DATA_USAGE_TYPE, TYPE_MOBILE_DATA);


        setSession(session);
        setType(type);


        if (mList.size() > 0) {
            mLoading.setAlpha(0.0f);
            onDataLoaded();
        } else {
            LoadData loadData = new LoadData(mContext, getSession(mContext), getType(mContext));
            loadData.execute();
        }

        mDataRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                LoadData loadData = new LoadData(mContext, getSession(mContext), getType(mContext));
                loadData.execute();
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
                        LoadData loadData = new LoadData(mContext, getSession(mContext), getType(mContext));
                        loadData.execute();
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
                        LoadData loadData = new LoadData(mContext, getSession(mContext), getType(mContext));
                        loadData.execute();
                        dialog.dismiss();
                    }
                });

                dialog.setContentView(dialogView);
                dialog.show();
            }
        });

        return view;
    }

    private static void onDataLoaded() {
        Log.e(TAG, "onDataLoaded: " + mList.size() + " system");
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

    public static class LoadData extends AsyncTask {
        private final Context mContext;
        private final int session;
        private final int type;

        public LoadData(Context mContext, int session, int type) {
            this.mContext = mContext;
            this.session = session;
            this.type = type;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mLoading.animate().alpha(1.0f);
            mAppsView.animate().alpha(0.0f);
            mEmptyList.animate().alpha(0.0f);
            mDataRefresh.setRefreshing(true);
            mList.clear();
        }

        @Override
        protected Object doInBackground(Object[] objects) {
            Long sent = 0L,
                    received = 0L,
                    totalSystemSent = 0L,
                    totalSystemReceived = 0L;

            DatabaseHandler handler = new DatabaseHandler(mContext);
            List<AppDataUsageModel> list = handler.getUsageList();
            Log.e(TAG, "doInBackground: " + list.size());
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
                                Long deviceTotal = getDeviceMobileDataUsage(mContext, session, 1)[2];

                                Double p = ((total.doubleValue() / deviceTotal.doubleValue()) * 100) * 5;

                                int progress = p.intValue();

                                model.setProgress(progress);

                                mList.add(model);
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

                                Double p = ((total.doubleValue() / deviceTotal.doubleValue()) * 100) * 5;

                                int progress = p.intValue();

                                model.setProgress(progress);

                                mList.add(model);
                            }
                        } catch (ParseException e) {
                            e.printStackTrace();
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

            Collections.sort(mList, new Comparator<AppDataUsageModel>() {
                @Override
                public int compare(AppDataUsageModel o1, AppDataUsageModel o2) {
                    o1.setMobileTotal((o1.getSentMobile() + o1.getReceivedMobile()) / 1024f);
                    o2.setMobileTotal((o2.getSentMobile() + o2.getReceivedMobile()) / 1024f);
                    return o1.getMobileTotal().compareTo(o2.getMobileTotal());
                }
            });

            Collections.reverse(mList);

            return null;
        }

        @Override
        protected void onPostExecute(Object o) {
            super.onPostExecute(o);
            onDataLoaded();
        }
    }

    private int getSession(Context context) {
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

    private int getType(Context context) {
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