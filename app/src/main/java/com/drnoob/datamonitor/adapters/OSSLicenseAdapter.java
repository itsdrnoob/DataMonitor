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

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.drnoob.datamonitor.R;
import com.drnoob.datamonitor.adapters.data.OSSLibrary;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class OSSLicenseAdapter extends RecyclerView.Adapter<OSSLicenseAdapter.OSSLicenseViewHolder> {
    private static final String TAG = OSSLicenseAdapter.class.getSimpleName();

    private List<OSSLibrary> libraries;
    private Context mContext;

    public OSSLicenseAdapter() {

    }

    public OSSLicenseAdapter(List<OSSLibrary> libraries, Context mContext) {
        this.libraries = libraries;
        this.mContext = mContext;
    }

    @NonNull
    @NotNull
    @Override
    public OSSLicenseViewHolder onCreateViewHolder(@NonNull @NotNull ViewGroup parent, int viewType) {
        return new OSSLicenseViewHolder(LayoutInflater.from(mContext).inflate(R.layout.oss_license_item,
                parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull @NotNull OSSLicenseViewHolder holder, int position) {
        OSSLibrary library = libraries.get(position);
        holder.ossLibraryName.setText(library.getLibraryName());
        holder.ossLibraryAuthorName.setText(library.getLibraryAuthorName());
        if (library.getLibraryDesc().length() > 1) {
            holder.ossLibraryDesc.setText(library.getLibraryDesc());
        }
        else {
            holder.ossLibraryDesc.setVisibility(View.GONE);
        }
        holder.ossLibraryVersion.setText(library.getLibraryVersion());
        holder.ossLibraryLicense.setText(library.getLibraryLicense());
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(mContext.getString(library.getLibraryLicenseURL())));
                mContext.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return libraries.size();
    }

    protected class OSSLicenseViewHolder extends RecyclerView.ViewHolder {
        private TextView ossLibraryName,
                ossLibraryAuthorName,
                ossLibraryDesc,
                ossLibraryVersion,
                ossLibraryLicense;

        public OSSLicenseViewHolder(@NonNull @NotNull View itemView) {
            super(itemView);
            ossLibraryName = itemView.findViewById(R.id.oss_library_name);
            ossLibraryAuthorName = itemView.findViewById(R.id.oss_library_author_name);
            ossLibraryDesc = itemView.findViewById(R.id.oss_library_desc);
            ossLibraryVersion = itemView.findViewById(R.id.oss_library_version);
            ossLibraryLicense = itemView.findViewById(R.id.oss_library_license);
        }
    }
}
