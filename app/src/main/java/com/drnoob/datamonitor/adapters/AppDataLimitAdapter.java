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
import static com.drnoob.datamonitor.core.Values.SESSION_TODAY;
import static com.drnoob.datamonitor.utils.NetworkStatsHelper.getAppMobileDataUsage;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.RemoteException;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.drnoob.datamonitor.R;
import com.drnoob.datamonitor.adapters.data.AppDataUsageModel;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.skydoves.progressview.ProgressView;

import org.jetbrains.annotations.NotNull;

import java.text.ParseException;
import java.util.List;

public class AppDataLimitAdapter extends RecyclerView.Adapter<AppDataLimitAdapter.AppDataLimitViewHolder> {
    private static final String TAG = AppDataLimitAdapter.class.getSimpleName();
    private final List<AppDataUsageModel> mList;
    private final Context mContext;
    private BottomSheetDialog dialog;
    private AppDataUsageModel selectedAppModel;

    public AppDataLimitAdapter(List<AppDataUsageModel> mList, Context mContext) {
        this.mList = mList;
        this.mContext = mContext;
    }

    public void setDialog(BottomSheetDialog dialog) {
        this.dialog = dialog;
    }

    public AppDataUsageModel getSelectedAppModel() {
        return selectedAppModel;
    }

    @NonNull
    @NotNull
    @Override
    public AppDataLimitViewHolder onCreateViewHolder(@NonNull @NotNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.app_data_usage_item, parent, false);
        return new AppDataLimitViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull @NotNull AppDataLimitViewHolder holder, int position) {
        AppDataUsageModel model = mList.get(position);
        if (model.isAppsList() != null && model.isAppsList()) {
            holder.mProgress.setVisibility(View.GONE);
            holder.mDataUsage.setVisibility(View.GONE);
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectedAppModel = new AppDataUsageModel();
                    selectedAppModel.setAppName(model.getAppName());
                    selectedAppModel.setPackageName(model.getPackageName());
                    selectedAppModel.setUid(model.getUid());
                    if (dialog != null) {
                        dialog.dismiss();
                    }
                }
            });
        }
        else {
            Float currentDataUsage = 0F;
            try {
                currentDataUsage = getAppMobileDataUsage(mContext, model.getUid(), SESSION_TODAY)[2] / 1024f / 1024f;
            } catch (RemoteException e) {
                e.printStackTrace();
            } catch (ParseException e) {
                e.printStackTrace();
            }
            String dataType = "";
            Float totalDataUsage = 0F;
            if (model.getDataType() != null && model.getDataType().equals("0")) {
                if (model.getDataLimit() != null) {
                    dataType = " MB";
                    totalDataUsage = Float.parseFloat(model.getDataLimit());
                }
            }
            else {
                if (model.getDataLimit() != null) {
                    dataType = " GB";
                    totalDataUsage = Float.parseFloat(model.getDataLimit()) * 1024;
                }
            }

            holder.mDataUsage.setText(model.getDataLimit() + dataType);
            Float progress = currentDataUsage / totalDataUsage * 100;
            Log.e(TAG, "onBindViewHolder: " + progress );
            holder.mProgress.setProgress(progress);

//            ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT |
//                    ItemTouchHelper.RIGHT | ItemTouchHelper.UP | ItemTouchHelper.DOWN) {
//                @Override
//                public boolean onMove(@NonNull @NotNull RecyclerView recyclerView, @NonNull @NotNull RecyclerView.ViewHolder viewHolder, @NonNull @NotNull RecyclerView.ViewHolder target) {
//                    Toast.makeText(mContext, "on Move", Toast.LENGTH_SHORT).show();
//                    return false;
//                }
//
//                @Override
//                public void onSwiped(@NonNull @NotNull RecyclerView.ViewHolder viewHolder, int direction) {
//                    Toast.makeText(mContext, "on Swiped", Toast.LENGTH_SHORT).show();
//                }
//            };

        }
        try {
            if (isAppInstalled(mContext, model.getPackageName())) {
                holder.mAppIcon.setImageDrawable(mContext.getPackageManager().getApplicationIcon(model.getPackageName()));
            } else {
                holder.mAppIcon.setImageResource(R.drawable.deleted_apps);
            }
        }
        catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        holder.mAppName.setText(model.getAppName());
    }

    @Override
    public int getItemCount() {
        return mList.size();
    }

    public class AppDataLimitViewHolder extends RecyclerView.ViewHolder {
        private final ImageView mAppIcon;
        private final TextView mAppName;
        private final TextView mDataUsage;
        private final ProgressView mProgress;

        public AppDataLimitViewHolder(@NonNull @NotNull View itemView) {
            super(itemView);
            mAppIcon = itemView.findViewById(R.id.app_icon);
            mAppName = itemView.findViewById(R.id.app_name);
            mDataUsage = itemView.findViewById(R.id.data_usage);
            mProgress = itemView.findViewById(R.id.progress);
        }
    }
}
