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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.drnoob.datamonitor.R;
import com.drnoob.datamonitor.adapters.data.AppDataUsageModel;
import com.drnoob.datamonitor.ui.activities.MainActivity;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.skydoves.progressview.ProgressView;

import java.util.List;

import static com.drnoob.datamonitor.Common.isAppInstalled;
import static com.drnoob.datamonitor.core.Values.DATA_USAGE_SESSION;
import static com.drnoob.datamonitor.core.Values.DATA_USAGE_SYSTEM;
import static com.drnoob.datamonitor.core.Values.DATA_USAGE_TYPE;
import static com.drnoob.datamonitor.core.Values.DATA_USAGE_VALUE;
import static com.drnoob.datamonitor.utils.NetworkStatsHelper.formatData;

public class AppDataUsageAdapter extends RecyclerView.Adapter<AppDataUsageAdapter.AppDataUsageViewHolder> {

    private static final String TAG = "AppDataUsageAdapter";
    private final List<AppDataUsageModel> mList;
    private final Context mContext;
    private Boolean animate;

    public AppDataUsageAdapter(List<AppDataUsageModel> mList, Context mContext) {
        this.mList = mList;
        this.mContext = mContext;
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
                    Intent intent = new Intent(mContext, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.putExtra(DATA_USAGE_VALUE, DATA_USAGE_SYSTEM);
                    intent.putExtra(DATA_USAGE_SESSION, model.getSession());
                    intent.putExtra(DATA_USAGE_TYPE, model.getType());
                    mContext.startActivity(intent);
                } else {
                    BottomSheetDialog dialog = new BottomSheetDialog(mContext, R.style.BottomSheet);
                    View dialogView = LayoutInflater.from(mContext).inflate(R.layout.app_detail_view, null);
                    dialog.setContentView(dialogView);

                    ImageView appIcon = dialogView.findViewById(R.id.icon);
                    TextView appName = dialogView.findViewById(R.id.name);
                    TextView dataSent = dialogView.findViewById(R.id.data_sent);
                    TextView dataReceived = dialogView.findViewById(R.id.data_received);
                    TextView appPackage = dialogView.findViewById(R.id.app_package);
                    TextView appUid = dialogView.findViewById(R.id.app_uid);
                    TextView appSettings = dialogView.findViewById(R.id.app_open_settings);

                    appName.setText(model.getAppName());
                    String packageName = mContext.getResources().getString(R.string.app_label_package_name,
                            model.getPackageName());
                    String uid = mContext.getResources().getString(R.string.app_label_uid,
                            model.getUid());

                    appPackage.setText(packageName);
                    appUid.setText(uid);

                    dataSent.setText(formatData(model.getSentMobile(), model.getReceivedMobile())[0]);
                    dataReceived.setText(formatData(model.getSentMobile(), model.getReceivedMobile())[1]);

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

                    dialog.show();
                }
            }
        });

    }

    @Override
    public int getItemCount() {
        return mList.size();
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
