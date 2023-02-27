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

package com.drnoob.datamonitor.adapters;

import static com.drnoob.datamonitor.Common.isAppInstalled;
import static com.drnoob.datamonitor.core.Values.DAILY_DATA_HOME_ACTION;
import static com.drnoob.datamonitor.core.Values.DATA_USAGE_SESSION;
import static com.drnoob.datamonitor.core.Values.DATA_USAGE_SYSTEM;
import static com.drnoob.datamonitor.core.Values.DATA_USAGE_TYPE;
import static com.drnoob.datamonitor.core.Values.GENERAL_FRAGMENT_ID;
import static com.drnoob.datamonitor.utils.NetworkStatsHelper.formatData;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.Settings;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.drnoob.datamonitor.R;
import com.drnoob.datamonitor.adapters.data.AppDataUsageModel;
import com.drnoob.datamonitor.ui.activities.ContainerActivity;
import com.drnoob.datamonitor.utils.NetworkStatsHelper;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.skydoves.progressview.ProgressView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class AppDataUsageAdapter extends RecyclerView.Adapter<AppDataUsageAdapter.AppDataUsageViewHolder> {

    private static final String TAG = "AppDataUsageAdapter";
    private final List<AppDataUsageModel> mList;
    private final Context mContext;
    private Boolean animate;
    private Boolean fromHome;
    private Activity mActivity;

    public AppDataUsageAdapter(List<AppDataUsageModel> mList, Context mContext) {
        this.mList = mList;
        this.mContext = mContext;
    }

    public Activity getActivity() {
        return mActivity;
    }

    public void setActivity(Activity mActivity) {
        this.mActivity = mActivity;
    }

    public Boolean getFromHome() {
        return fromHome;
    }

    public void setFromHome(Boolean fromHome) {
        this.fromHome = fromHome;
    }

    @NonNull
    @Override
    public AppDataUsageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.app_data_usage_item, parent, false);
        return new AppDataUsageViewHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull AppDataUsageAdapter.AppDataUsageViewHolder holder, int position) {

        AppDataUsageModel model = mList.get(position);
        try {
            if (model.getPackageName().equals("com.android.tethering")) {
                holder.mAppIcon.setImageResource(R.drawable.hotspot);
            } else if (model.getPackageName().equals("com.android.deleted")) {
                holder.mAppIcon.setImageResource(R.drawable.deleted_apps);
            } else {
                if (isAppInstalled(mContext, model.getPackageName())) {
                    holder.mAppIcon.setImageDrawable(mContext.getPackageManager().getApplicationIcon(model.getPackageName()));
                } else {
                    holder.mAppIcon.setImageResource(R.drawable.deleted_apps);
                }
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        String totalDataUsage = formatData(model.getSentMobile(),
                model.getReceivedMobile())[2];

        if (model.getProgress() > 0) {
            holder.mProgress.setProgress(model.getProgress());
        } else {
            holder.mProgress.setProgress(1);
        }

        holder.mAppName.setText(model.getAppName());
        holder.mDataUsage.setText(totalDataUsage);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (model.getPackageName().equals(mContext.getString(R.string.package_system))) {
                    Intent intent = new Intent(mContext, ContainerActivity.class);
//                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.putExtra(GENERAL_FRAGMENT_ID, DATA_USAGE_SYSTEM);
                    intent.putExtra(DATA_USAGE_SESSION, model.getSession());
                    intent.putExtra(DATA_USAGE_TYPE, model.getType());
                    intent.putExtra(DAILY_DATA_HOME_ACTION, getFromHome());
                    mContext.startActivity(intent);
                }
//                else {
//                    LoadAppDetails loadAppDetails = new LoadAppDetails(model);
//                    if (!MainActivity.isDataLoading()) {
//                        loadAppDetails.execute();
//                    }
//                }


                else {
                    BottomSheetDialog dialog = new BottomSheetDialog(mContext, R.style.BottomSheet);
                    View dialogView = LayoutInflater.from(mContext).inflate(R.layout.app_detail_view, null);
                    dialog.setContentView(dialogView);

                    ImageView appIcon = dialogView.findViewById(R.id.icon);
                    TextView appName = dialogView.findViewById(R.id.name);
                    TextView dataSent = dialogView.findViewById(R.id.data_sent);
                    TextView dataReceived = dialogView.findViewById(R.id.data_received);
                    TextView appPackage = dialogView.findViewById(R.id.app_package);
                    TextView appUid = dialogView.findViewById(R.id.app_uid);
                    TextView appScreenTime = dialogView.findViewById(R.id.app_screen_time);
                    TextView appBackgroundTime = dialogView.findViewById(R.id.app_background_time);
                    TextView appCombinedTotal = dialogView.findViewById(R.id.app_combined_total);
                    MaterialButton appSettings = dialogView.findViewById(R.id.app_open_settings);

                    appName.setText(model.getAppName());
                    String packageName = mContext.getResources().getString(R.string.app_label_package_name,
                            model.getPackageName());
                    String uid = mContext.getResources().getString(R.string.app_label_uid,
                            model.getUid());

                    appPackage.setText(setBoldSpan(packageName, model.getPackageName()));
                    appUid.setText(setBoldSpan(uid, String.valueOf(model.getUid())));

                    if (model.getPackageName() != mContext.getString(R.string.package_tethering)) {
                        String screenTime = mContext.getString(R.string.app_label_screen_time,
                                mContext.getString(R.string.label_loading));
                        String backgroundTime = mContext.getString(R.string.app_label_background_time,
                                mContext.getString(R.string.label_loading));
                        appScreenTime.setText(setBoldSpan(screenTime,
                                mContext.getString(R.string.label_loading)));
                        appBackgroundTime.setText(setBoldSpan(backgroundTime,
                                mContext.getString(R.string.label_loading)));
                        LoadScreenTime loadScreenTime = new LoadScreenTime(model, getActivity(), appScreenTime, appBackgroundTime);
                        loadScreenTime.execute();
                    }
                    else {
                        appScreenTime.setVisibility(View.GONE);
                        appBackgroundTime.setVisibility(View.GONE);
                    }

                    long total = model.getSentMobile() + model.getReceivedMobile() + model.getSentWifi() + model.getReceivedWifi();
                    String combinedTotal = formatData(0l, total)[2];

                    dataSent.setText(formatData(model.getSentMobile(), model.getReceivedMobile())[0]);
                    dataReceived.setText(formatData(model.getSentMobile(), model.getReceivedMobile())[1]);
                    appCombinedTotal.setText(setBoldSpan(mContext.getString(R.string.app_label_combined_total, combinedTotal),
                            combinedTotal));

                    appSettings.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            Uri uri = Uri.fromParts("package", model.getPackageName(), null);
                            intent.setData(uri);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            mContext.startActivity(intent);
                        }
                    });

                    try {
                        if (model.getPackageName().equals(mContext.getString(R.string.package_tethering))) {
                            appIcon.setImageResource(R.drawable.hotspot);
                            appPackage.setVisibility(View.GONE);
                            appUid.setVisibility(View.GONE);
                            appSettings.setVisibility(View.GONE);
                            appCombinedTotal.setVisibility(View.GONE);
                        } else if (model.getPackageName().equals(mContext.getString(R.string.package_removed))) {
                            appIcon.setImageResource(R.drawable.deleted_apps);
                            appPackage.setVisibility(View.GONE);
                            appUid.setVisibility(View.GONE);
                            appSettings.setVisibility(View.GONE);
                        } else {
                            if (isAppInstalled(mContext, model.getPackageName())) {
                                appIcon.setImageDrawable(mContext.getPackageManager().getApplicationIcon(model.getPackageName()));
                            } else {
                                appIcon.setImageResource(R.drawable.deleted_apps);
                            }
                        }
                    } catch (PackageManager.NameNotFoundException e) {
                        e.printStackTrace();
                    }

                    dialog.setOnShowListener(new DialogInterface.OnShowListener() {
                        @Override
                        public void onShow(DialogInterface dialogInterface) {
                            BottomSheetDialog bottomSheetDialog = (BottomSheetDialog) dialogInterface;
                            FrameLayout bottomSheet = bottomSheetDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
                            BottomSheetBehavior behavior = BottomSheetBehavior.from(bottomSheet);
                            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                            behavior.setSkipCollapsed(true);
                        }
                    });

                    dialog.show();
                }
            }
        });

    }

    private SpannableString setBoldSpan(String text, String spanText) {
        SpannableString boldSpan = new SpannableString(text);
        int start = text.indexOf(spanText);
        if (start < 0) {
            start = 0;
        }
        int end = start + spanText.length();
        boldSpan.setSpan(new StyleSpan(Typeface.BOLD), start, end, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        return boldSpan;
    }

    private class LoadScreenTime extends AsyncTask {
        private AppDataUsageModel model;
        private Activity activity;

        TextView appScreenTime;
        TextView appBackgroundTime;

        public LoadScreenTime(AppDataUsageModel model, Activity activity, TextView appScreenTime, TextView appBackgroundTime) {
            this.model = model;
            this.activity = activity;
            this.appScreenTime = appScreenTime;
            this.appBackgroundTime = appBackgroundTime;
        }

        @Override
        protected Object doInBackground(Object[] objects) {
            assert getActivity() != null;
            if (model.getPackageName() != mContext.getString(R.string.package_tethering)) {
                int[] usageTime = getUsageTime(mContext, model.getPackageName(), model.getSession());
                if (usageTime[1] == -1) {
                    // If value is -1, build version is below Q
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            String screenTime = mContext.getString(R.string.app_label_screen_time,
                                    formatTime(usageTime[0] / 60f));
                            appScreenTime.setText(setBoldSpan(screenTime, formatTime(usageTime[0] / 60f)));
                            appBackgroundTime.setVisibility(View.GONE);
                        }
                    });
                }
                else {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            String screenTime = mContext.getString(R.string.app_label_screen_time,
                                    formatTime(usageTime[0] / 60f));
                            String backgroundTime = mContext.getString(R.string.app_label_background_time,
                                    formatTime(usageTime[1] / 60f));
                            appScreenTime.setText(setBoldSpan(screenTime, formatTime(usageTime[0] / 60f)));
                            appBackgroundTime.setText(setBoldSpan(backgroundTime, formatTime(usageTime[1] / 60f)));
                        }
                    });
                }
            }
            else {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        appScreenTime.setVisibility(View.GONE);
                        appBackgroundTime.setVisibility(View.GONE);
                    }
                });
            }
            return null;
        }

    }

    private String formatTime(Float minutes) {
        if (minutes < 1 && minutes > 0) {
            return "Less than a minute";
        }
        if (minutes >= 60) {
            Float f = minutes / 3.6f;
            int hours = (int) (minutes / 60);
            int mins = (int) (minutes % 60);
            String hourLabel, minuteLabel;
            if (hours > 1) {
                hourLabel = "hours";
            }
            else {
                hourLabel = "hour";
            }
            if (mins == 1) {
                minuteLabel = "minute";
            }
            else {
                minuteLabel = "minutes";
            }
            return mContext.getString(R.string.usage_time_label, hours, hourLabel, mins, minuteLabel);
        }
        if (minutes == 1) {
            return String.valueOf(Math.round(minutes)) + " minute";
        }
        return String.valueOf(Math.round(minutes)) + " minutes";
    }

    @Override
    public int getItemCount() {
        return mList.size();
    }

    private int[] getUsageTime(Context context, String packageName, int session) {

        /**
         * Returns app usage time as an array like [screenTime, backgroundTime]
         * ScreenTime source credit: https://stackoverflow.com/questions/61677505/how-to-count-app-usage-time-while-app-is-on-foreground
         */

        UsageEvents.Event currentEvent;
        List<UsageEvents.Event> allEvents = new ArrayList<>();
        HashMap<String, Integer> appScreenTime = new HashMap<>();
        HashMap<String, Integer> appBackgroundTime = new HashMap<>();
        UsageStatsManager usageStatsManager = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
        UsageEvents usageEvents = null;
        List<UsageStats> usageStats = null;
        assert usageStatsManager != null;
        try {
            usageEvents = usageStatsManager.queryEvents(NetworkStatsHelper.getTimePeriod(context, session, 1)[0],
                    NetworkStatsHelper.getTimePeriod(context, session, 1)[1]);

            usageStats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, NetworkStatsHelper.getTimePeriod(context, session, 1)[0],
                    NetworkStatsHelper.getTimePeriod(context, session, 1)[1]);

        } catch (Exception e) {
            e.printStackTrace();
        }

        if (usageEvents != null) {
            if (usageEvents.hasNextEvent()) {
                while (usageEvents.hasNextEvent()) {
                    currentEvent = new UsageEvents.Event();
                    usageEvents.getNextEvent(currentEvent);
                    if(currentEvent.getPackageName().equals(packageName)) {
                        if (currentEvent.getEventType() == UsageEvents.Event.ACTIVITY_RESUMED
                                || currentEvent.getEventType() == UsageEvents.Event.ACTIVITY_PAUSED
                                || currentEvent.getEventType() == UsageEvents.Event.FOREGROUND_SERVICE_START
                                || currentEvent.getEventType() == UsageEvents.Event.FOREGROUND_SERVICE_STOP) {
                            allEvents.add(currentEvent);
                            String key = currentEvent.getPackageName();
                            if (appScreenTime.get(key) == null)
                                appScreenTime.put(key, 0);
                        }
                    }
                }

                if (allEvents.size() > 0) {
                    for (int i = 0; i < allEvents.size() - 1; i++) {
                        UsageEvents.Event E0 = allEvents.get(i);
                        UsageEvents.Event E1 = allEvents.get(i + 1);
                        if (E0.getEventType() == UsageEvents.Event.ACTIVITY_RESUMED
                                && E1.getEventType() == UsageEvents.Event.ACTIVITY_PAUSED
                                && E0.getClassName().equals(E1.getClassName())) {
                            int diff = (int)(E1.getTimeStamp() - E0.getTimeStamp());
                            diff /= 1000;
                            Integer prev = appScreenTime.get(E0.getPackageName());
                            if(prev == null) prev = 0;
                            appScreenTime.put(E0.getPackageName(), prev + diff);
                        }
                    }
                    UsageEvents.Event lastEvent = allEvents.get(allEvents.size() - 1);
                    if(lastEvent.getEventType() == UsageEvents.Event.ACTIVITY_RESUMED) {
                        int diff = (int)System.currentTimeMillis() - (int)lastEvent.getTimeStamp();
                        diff /= 1000;
                        Integer prev = appScreenTime.get(lastEvent.getPackageName());
                        if(prev == null) prev = 0;
                        appScreenTime.put(lastEvent.getPackageName(), prev + diff);
                    }
                }
                else {
                    appScreenTime.put(packageName, 0);
                }
            }
        }

        // Check background app usage time
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (usageStats.size() > 0) {
                for (int i = 0; i < usageStats.size(); i++) {
                    if (usageStats.get(i).getPackageName().equals(packageName)) {
                        int backgroundTime = (int) usageStats.get(i).getTotalTimeForegroundServiceUsed() / 1000;
                        appBackgroundTime.put(packageName, backgroundTime);
                        break;
                    }
                }
            }
            else {
                appBackgroundTime.put(packageName, 0);
            }
        }
        else {
            appBackgroundTime.put(packageName, -1);
        }
        if (appBackgroundTime.get(packageName) == null) {
            appBackgroundTime.put(packageName, 0);
        }


//        return appScreenTime.get(packageName);
        return new int[] {appScreenTime.get(packageName), appBackgroundTime.get(packageName)};
    }

    protected class AppDataUsageViewHolder extends RecyclerView.ViewHolder {

        private final ImageView mAppIcon;
        private final TextView mAppName;
        private final TextView mDataUsage;
        private final ProgressView mProgress;

        public AppDataUsageViewHolder(@NonNull View itemView) {
            super(itemView);
            mAppIcon = itemView.findViewById(R.id.app_icon);
            mAppName = itemView.findViewById(R.id.app_name);
            mDataUsage = itemView.findViewById(R.id.data_usage);
            mProgress = itemView.findViewById(R.id.progress);
        }
    }
}
