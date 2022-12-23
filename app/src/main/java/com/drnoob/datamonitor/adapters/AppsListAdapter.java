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

import static com.drnoob.datamonitor.core.Values.EXTRA_APP_NAME;
import static com.drnoob.datamonitor.core.Values.EXTRA_APP_PACKAGE;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.recyclerview.widget.RecyclerView;

import com.drnoob.datamonitor.R;
import com.drnoob.datamonitor.adapters.data.AppModel;
import com.drnoob.datamonitor.adapters.data.LiveData;
import com.drnoob.datamonitor.ui.activities.AppPickerActivity;

import java.util.ArrayList;
import java.util.List;

public class AppsListAdapter extends RecyclerView.Adapter<AppsListAdapter.AppsListViewHolder> {
    private static final String TAG = AppsListAdapter.class.getSimpleName();

    private Activity mActivity;
    private List<AppModel> mAppsList;
    private List<AppModel> mSelectedApps = new ArrayList<>();
    private boolean isSelectionView = false;
    private LiveData mLiveData;

    public AppsListAdapter() {
        this.setHasStableIds(true);
        if (mLiveData == null) {
            mLiveData = new ViewModelProvider((ViewModelStoreOwner) mActivity).get(LiveData.class);
        }
    }

    public AppsListAdapter(Activity mActivity, List<AppModel> mAppsList) {
        this.mActivity = mActivity;
        this.mAppsList = mAppsList;
        if (mLiveData == null) {
            mLiveData = new ViewModelProvider((ViewModelStoreOwner) mActivity).get(LiveData.class);
        }
        this.setHasStableIds(true);
        if (mLiveData.getIsAppSelectionView().getValue() != null) {
            this.isSelectionView = mLiveData.getIsAppSelectionView().getValue();
        }
    }

    @NonNull
    @Override
    public AppsListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new AppsListViewHolder(LayoutInflater.from(mActivity).inflate(R.layout.app_list_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull AppsListViewHolder holder, int position) {
        AppModel app = mAppsList.get(position);
        holder.appName.setText(app.getAppName());
        holder.appPackageName.setText(app.getPackageName());
        Drawable appIcon = mActivity.getDrawable(R.mipmap.ic_launcher);
        try {
            appIcon = (Drawable) mActivity.getPackageManager().getApplicationIcon(app.getPackageName());
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        if (app.getIsSelected()) {
            holder.appIcon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            holder.appIcon.setImageDrawable(mActivity.getDrawable(R.drawable.ic_check_circle));
            holder.itemView.getBackground().setTint(mActivity.getColor(R.color.search_bg));
        }
        else {
            holder.appIcon.setScaleType(ImageView.ScaleType.CENTER_CROP);
            holder.appIcon.setImageDrawable(appIcon);
            holder.itemView.getBackground().setTint(mActivity.getColor(R.color.surface));
        }

        Drawable finalAppIcon = appIcon;
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mActivity instanceof AppPickerActivity) {
                    Intent data = new Intent();
                    data.putExtra(EXTRA_APP_NAME, app.getAppName());
                    data.putExtra(EXTRA_APP_PACKAGE, app.getPackageName());
                    AppPickerActivity.setData(data);
                    ((AppPickerActivity) mActivity).returnResult();
                }
                else {
                    if (isSelectionView) {
                        app.setIsSelected(!app.getIsSelected());
                        if (app.getIsSelected()) {
                            holder.appIcon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                            holder.appIcon.setImageDrawable(mActivity.getDrawable(R.drawable.ic_check_circle));
                            holder.itemView.getBackground().setTint(mActivity.getColor(R.color.search_bg));
                            mSelectedApps.add(app);
                        }
                        else {
                            holder.appIcon.setScaleType(ImageView.ScaleType.CENTER_CROP);
                            holder.appIcon.setImageDrawable(finalAppIcon);
                            holder.itemView.getBackground().setTint(mActivity.getColor(R.color.surface));
                            mSelectedApps.remove(app);
                        }
                        mLiveData.setSelectedAppsList(mSelectedApps);
                    }
                }
            }
        });

        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if (mActivity instanceof AppPickerActivity) {

                }
                else {
                    if (!isSelectionView) {
                        isSelectionView = true;

                    }
                    if (mLiveData == null || mLiveData.getIsAppSelectionView().getValue() == null
                            || !mLiveData.getIsAppSelectionView().getValue()) {
                        mLiveData.setIsAppSelectionView(true);
                    }
                    app.setIsSelected(!app.getIsSelected());
                    if (app.getIsSelected()) {
                        holder.appIcon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                        holder.appIcon.setImageDrawable(mActivity.getDrawable(R.drawable.ic_check_circle));
                        holder.itemView.getBackground().setTint(mActivity.getColor(R.color.search_bg));
                        mSelectedApps.add(app);
                    }
                    else {
                        holder.appIcon.setScaleType(ImageView.ScaleType.CENTER_CROP);
                        holder.appIcon.setImageDrawable(finalAppIcon);
                        holder.itemView.getBackground().setTint(mActivity.getColor(R.color.surface));
                        mSelectedApps.remove(app);
                    }
                    mLiveData.setSelectedAppsList(mSelectedApps);
                    return true;
                }
                return false;
            }
        });

        mLiveData.getSelectedAppsList().observe((LifecycleOwner) mActivity, new Observer<List<AppModel>>() {
            @Override
            public void onChanged(List<AppModel> appModels) {
                if (appModels != null && appModels.size() <= 0) {
                    isSelectionView = false;
                    if (mLiveData == null || mLiveData.getIsAppSelectionView().getValue() == null
                            || mLiveData.getIsAppSelectionView().getValue()) {
                        mLiveData.setIsAppSelectionView(false);
                    }
                    mSelectedApps.clear();
                    app.setIsSelected(false);
                    holder.appIcon.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    holder.appIcon.setImageDrawable(finalAppIcon);
                    holder.itemView.getBackground().setTint(mActivity.getColor(R.color.surface));
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mAppsList.size();
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    public class AppsListViewHolder extends RecyclerView.ViewHolder {
        private TextView appName, appPackageName;
        private ImageView appIcon;

        public AppsListViewHolder(@NonNull View itemView) {
            super(itemView);

            appName = itemView.findViewById(R.id.app_name);
            appPackageName = itemView.findViewById(R.id.app_package);
            appIcon = itemView.findViewById(R.id.app_icon);
        }
    }
}
