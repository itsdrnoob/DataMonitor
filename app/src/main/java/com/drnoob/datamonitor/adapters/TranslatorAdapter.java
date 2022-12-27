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
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.drnoob.datamonitor.R;
import com.drnoob.datamonitor.adapters.data.TranslatorModel;

import java.util.List;

public class TranslatorAdapter extends RecyclerView.Adapter<TranslatorAdapter.TranslatorViewHolder> {
    private List<TranslatorModel> translators;
    private Activity activity;

    public TranslatorAdapter() {
    }

    public TranslatorAdapter(List<TranslatorModel> translators, Activity activity) {
        this.translators = translators;
        this.activity = activity;
    }

    @NonNull
    @Override
    public TranslatorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new TranslatorViewHolder(LayoutInflater.from(activity)
                .inflate(R.layout.translators_item, parent, false));
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    @Override
    public void onBindViewHolder(@NonNull TranslatorViewHolder holder, int position) {
        TranslatorModel translator = translators.get(position);
        holder.title.setText(translator.getTitle());
        holder.summary.setText(translator.getSummary());
        holder.icon.setImageDrawable(activity.getDrawable(translator.getIconID()));
        if (!translator.hasLinkedGithub()) {
            holder.githubWidget.setVisibility(View.GONE);
        }
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (translator.hasLinkedGithub()) {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(translator.getGithubLink()));
                    activity.startActivity(intent);
                }
            }
        });
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return translators.size();
    }

    public class TranslatorViewHolder extends RecyclerView.ViewHolder {
        private TextView title, summary;
        private ImageView icon, githubWidget;

        public TranslatorViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.title);
            summary = itemView.findViewById(R.id.summary);
            icon = itemView.findViewById(R.id.icon);
            githubWidget = itemView.findViewById(R.id.widget_github);
        }
    }
}
