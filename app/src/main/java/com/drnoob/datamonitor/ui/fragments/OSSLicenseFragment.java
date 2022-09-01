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

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.drnoob.datamonitor.R;
import com.drnoob.datamonitor.adapters.OSSLicenseAdapter;
import com.drnoob.datamonitor.adapters.data.OSSLibrary;
import com.drnoob.datamonitor.utils.OSSLicense;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class OSSLicenseFragment extends Fragment {
    private static final String TAG = OSSLicenseFragment.class.getSimpleName();

    private OSSLicenseAdapter mAdapter;
    private List<OSSLibrary> libraries;
    private RecyclerView mLicenseView;

    @Override
    public void onCreate(@Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @org.jetbrains.annotations.Nullable
    @Override
    public View onCreateView(@NonNull @NotNull LayoutInflater inflater, @Nullable @org.jetbrains.annotations.Nullable ViewGroup container, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_oss_licenses, container, false);
    }

    @Override
    public void onViewCreated(@NonNull @NotNull View view, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        libraries = OSSLicense.getOSSLibraries();
        mAdapter = new OSSLicenseAdapter(libraries, getContext());
        mLicenseView = view.findViewById(R.id.oss_license_view);

        mLicenseView.setAdapter(mAdapter);
        mLicenseView.setLayoutManager(new LinearLayoutManager(getContext()));

    }
}
