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

import static com.drnoob.datamonitor.core.Values.AVG_DOWNLOAD_SPEED;
import static com.drnoob.datamonitor.core.Values.AVG_LATENCY;
import static com.drnoob.datamonitor.core.Values.AVG_UPLOAD_SPEED;
import static com.drnoob.datamonitor.core.Values.GENERAL_FRAGMENT_ID;
import static com.drnoob.datamonitor.core.Values.ISP;
import static com.drnoob.datamonitor.core.Values.MIN_LATENCY;
import static com.drnoob.datamonitor.core.Values.NETWORK_IP;
import static com.drnoob.datamonitor.core.Values.NETWORK_STATS_FRAGMENT;
import static com.drnoob.datamonitor.core.Values.REGION;
import static com.drnoob.datamonitor.core.Values.SERVER;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
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
import com.drnoob.datamonitor.adapters.data.DiagnosticsHistoryModel;
import com.drnoob.datamonitor.adapters.data.LiveData;
import com.drnoob.datamonitor.ui.activities.ContainerActivity;

import java.util.ArrayList;
import java.util.List;

public class DiagnosticsHistoryAdapter extends RecyclerView.Adapter<DiagnosticsHistoryAdapter.DiagnosticsHistoryViewHolder> {
    private static final String TAG = DiagnosticsHistoryAdapter.class.getSimpleName();
    private List<DiagnosticsHistoryModel> list;
    private Context context;
    private LiveData mLiveData;
    private boolean isSelectionView = false;
    private List<DiagnosticsHistoryModel> selectedResults = new ArrayList<>();

    public DiagnosticsHistoryAdapter() {
    }

    public DiagnosticsHistoryAdapter(List<DiagnosticsHistoryModel> list, Context context) {
        this.list = list;
        this.context = context;
        if (mLiveData == null) {
            mLiveData = new ViewModelProvider((ViewModelStoreOwner) context).get(LiveData.class);
        }
        this.setHasStableIds(true);
        if (mLiveData.getIsResultSelectionView().getValue() != null) {
            this.isSelectionView = mLiveData.getIsResultSelectionView().getValue();
        }
    }

    @NonNull
    @Override
    public DiagnosticsHistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new DiagnosticsHistoryViewHolder(LayoutInflater.from(context)
                .inflate(R.layout.diagnostics_history_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull DiagnosticsHistoryViewHolder holder, int position) {
        DiagnosticsHistoryModel model = list.get(position);
        holder.title.setText(model.getDate());
        holder.summary.setText(model.getSummary());

        if (model.isSelected()) {
            holder.selectionCheck.setVisibility(View.VISIBLE);
            holder.itemView.getBackground().setTint(context.getColor(R.color.search_bg));
        }
        else {
            holder.selectionCheck.setVisibility(View.GONE);
            holder.itemView.getBackground().setTint(context.getColor(R.color.surface));
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isSelectionView) {
                    model.setSelected(!model.isSelected());
                    if (model.isSelected()) {
                        holder.selectionCheck.setVisibility(View.VISIBLE);
                        holder.itemView.getBackground().setTint(context.getColor(R.color.search_bg));
                        selectedResults.add(model);
                    }
                    else {
                        holder.selectionCheck.setVisibility(View.GONE);
                        holder.itemView.getBackground().setTint(context.getColor(R.color.surface));
                        selectedResults.remove(model);
                    }
                    mLiveData.setSelectedResults(selectedResults);
                }
                else {
                    Bundle bundle = new Bundle();
                    bundle.putString(AVG_DOWNLOAD_SPEED, model.getDownloadSpeed());
                    bundle.putString(AVG_UPLOAD_SPEED, model.getUploadSpeed());
                    bundle.putString(MIN_LATENCY, model.getMinLatency());
                    bundle.putString(AVG_LATENCY, model.getAvgLatency());
                    bundle.putString(NETWORK_IP, model.getIp());
                    bundle.putString(ISP, model.getIsp());
                    bundle.putString(SERVER, model.getServer());
                    bundle.putString(REGION, model.getRegion());

                    Intent intent = new Intent(context, ContainerActivity.class);
                    intent.putExtra(GENERAL_FRAGMENT_ID, NETWORK_STATS_FRAGMENT);
                    intent.putExtras(bundle);
                    context.startActivity(intent);
                }
            }
        });

        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if (!isSelectionView) {
                    isSelectionView = true;
                }
                if (mLiveData == null || mLiveData.getIsResultSelectionView().getValue() == null
                        || !mLiveData.getIsResultSelectionView().getValue()) {
                    mLiveData.setIsResultSelectionView(true);
                }
                model.setSelected(!model.isSelected());
                if (model.isSelected()) {
                    holder.selectionCheck.setVisibility(View.VISIBLE);
                    holder.itemView.getBackground().setTint(context.getColor(R.color.search_bg));
                    selectedResults.add(model);
                }
                else {
                    holder.selectionCheck.setVisibility(View.GONE);
                    holder.itemView.getBackground().setTint(context.getColor(R.color.surface));
                    selectedResults.remove(model);
                }
                mLiveData.setSelectedResults(selectedResults);
                return true;
            }
        });

        mLiveData.getSelectedResults().observe((LifecycleOwner) context, new Observer<List<DiagnosticsHistoryModel>>() {
            @Override
            public void onChanged(List<DiagnosticsHistoryModel> diagnosticsHistoryModels) {
                if (diagnosticsHistoryModels != null && diagnosticsHistoryModels.size() <= 0) {
                    isSelectionView = false;
                    if (mLiveData == null || mLiveData.getIsResultSelectionView().getValue() == null
                            || mLiveData.getIsResultSelectionView().getValue()) {
                        mLiveData.setIsResultSelectionView(false);
                    }
                    selectedResults.clear();
                    model.setSelected(false);
                    holder.selectionCheck.setVisibility(View.GONE);
                    holder.itemView.getBackground().setTint(context.getColor(R.color.surface));
                }
            }
        });
    }

    public void selectAll() {
        selectedResults.clear();
        for (int i = 0; i < list.size(); i++) {
            list.get(i).setSelected(true);
        }
        selectedResults.addAll(list);
        mLiveData.setSelectedResults(selectedResults);
        notifyDataSetChanged();
    }

    public void deselectAll() {
        for (int i = 0; i < list.size(); i++) {
            list.get(i).setSelected(false);
        }
        selectedResults.clear();
        mLiveData.setSelectedResults(selectedResults);
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public class DiagnosticsHistoryViewHolder extends RecyclerView.ViewHolder {
        private TextView title, summary;
        private ImageView selectionCheck;

        public DiagnosticsHistoryViewHolder(@NonNull View itemView) {
            super(itemView);

            title = itemView.findViewById(R.id.history_date);
            summary = itemView.findViewById(R.id.history_up_down_summary);
            selectionCheck = itemView.findViewById(R.id.selection_check);
        }
    }
}
