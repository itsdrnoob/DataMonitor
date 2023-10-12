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
import static com.drnoob.datamonitor.core.Values.DATA_USAGE_SESSION;
import static com.drnoob.datamonitor.core.Values.DATA_USAGE_TYPE;
import static com.drnoob.datamonitor.core.Values.SESSION_TODAY;
import static com.drnoob.datamonitor.core.Values.TYPE_MOBILE_DATA;
import static com.drnoob.datamonitor.utils.NetworkStatsHelper.getAppMobileDataUsage;
import static com.drnoob.datamonitor.utils.NetworkStatsHelper.getAppWifiDataUsage;
import static com.drnoob.datamonitor.utils.NetworkStatsHelper.getDeviceMobileDataUsage;
import static com.drnoob.datamonitor.utils.NetworkStatsHelper.getDeviceWifiDataUsage;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.drnoob.datamonitor.R;
import com.drnoob.datamonitor.adapters.AppDataUsageAdapter;
import com.drnoob.datamonitor.adapters.data.AppDataUsageModel;
import com.drnoob.datamonitor.core.task.DatabaseHandler;

import java.text.ParseException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class SystemDataUsageFragment extends Fragment {
    private static final String TAG = SystemDataUsageFragment.class.getSimpleName();

    public static RecyclerView mAppsView;
    private static LinearLayout mLoading;
    private static Context mContext;
    private static Activity mActivity;
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
        mActivity = getActivity();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_system_data_usage, container, false);

        mAppsView = view.findViewById(R.id.app_data_usage_recycler);
        mLoading = view.findViewById(R.id.layout_list_loading);
        mDataRefresh = view.findViewById(R.id.refresh_data_usage);
//        mSession = view.findViewById(R.id.data_usage_session);
//        mType = view.findViewById(R.id.data_usage_type);
        mTopBar = view.findViewById(R.id.top_bar);
        mEmptyList = view.findViewById(R.id.empty_list);
        mList = AppDataUsageFragment.mSystemList;
        mAdapter = new AppDataUsageAdapter(mList, mContext);
        mAdapter.setActivity(getActivity());
        mAppsView = view.findViewById(R.id.app_data_usage_recycler);


        int session = getActivity().getIntent().getIntExtra(DATA_USAGE_SESSION, SESSION_TODAY);
        int type = getActivity().getIntent().getIntExtra(DATA_USAGE_TYPE, TYPE_MOBILE_DATA);

        if (getActivity().getIntent() != null) {
            Boolean fromHome = getActivity().getIntent().getBooleanExtra(DAILY_DATA_HOME_ACTION, false);
            if (fromHome) {
                mTopBar.setVisibility(View.GONE);
                mAppsView.setPadding(0, 20, 0, 0);
            }
        }

//        setSession(session);
//        setType(type);


        if (mList.size() > 0) {
            mLoading.setAlpha(0.0f);
            onDataLoaded();
        } else {
            LoadData loadData = new LoadData(mContext, session, type);
            loadData.execute();
        }

        mDataRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                LoadData loadData = new LoadData(mContext, session, type);
                loadData.execute();
            }
        });

        return view;
    }

    private static void onDataLoaded() {
        Log.e(TAG, "onDataLoaded: " + mList.size() + " system");
        mAdapter = new AppDataUsageAdapter(mList, mContext);
        mAdapter.setActivity(mActivity);
        mAppsView.setAdapter(mAdapter);
        mAppsView.setLayoutManager(new LinearLayoutManager(mContext));
        mLoading.animate().alpha(0.0f);
        mAppsView.animate().alpha(1.0f);
        mDataRefresh.setRefreshing(false);
        if (mList.size() <= 0) {
            mEmptyList.animate().alpha(1.0f);
        }
//        else {
//            setSession(mList.get(0).getSession());
//            setType(mList.get(0).getType());
//        }
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
                                int progress;
                                if (p != null) {
                                    progress = p.intValue();
                                }
                                else {
                                    progress = 0;
                                }

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
                                int progress;
                                if (p != null) {
                                    progress = p.intValue();
                                }
                                else {
                                    progress = 0;
                                }

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
}