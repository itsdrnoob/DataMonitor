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

import static com.drnoob.datamonitor.core.Values.AVG_DOWNLOAD_SPEED;
import static com.drnoob.datamonitor.core.Values.AVG_LATENCY;
import static com.drnoob.datamonitor.core.Values.AVG_UPLOAD_SPEED;
import static com.drnoob.datamonitor.core.Values.ISP;
import static com.drnoob.datamonitor.core.Values.MIN_LATENCY;
import static com.drnoob.datamonitor.core.Values.NETWORK_IP;
import static com.drnoob.datamonitor.core.Values.REGION;
import static com.drnoob.datamonitor.core.Values.SERVER;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.drnoob.datamonitor.R;

import org.jetbrains.annotations.NotNull;

public class NetworkStatsFragment extends Fragment {
    private static final String TAG = NetworkStatsFragment.class.getSimpleName();

    private TextView networkIP,
            isp,
            ispServer,
            ispRegion,
            maxDownloadSpeed,
            avgDownloadSpeed,
            maxUploadSpeed,
            avgUploadSpeed,
            minLatency,
            avgLatency;
    private Bundle mData;

    @Override
    public void onCreate(@Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getActivity().getIntent().getExtras() != null) {
            mData = getActivity().getIntent().getExtras();
        }
    }

    @Nullable
    @org.jetbrains.annotations.Nullable
    @Override
    public View onCreateView(@NonNull @NotNull LayoutInflater inflater, @Nullable @org.jetbrains.annotations.Nullable ViewGroup container, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_network_stats, container, false);
    }

    @Override
    public void onViewCreated(@NonNull @NotNull View view, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        networkIP = view.findViewById(R.id.network_ip);
        isp = view.findViewById(R.id.isp);
        ispServer = view.findViewById(R.id.isp_server);
        ispRegion = view.findViewById(R.id.isp_region);
//        maxDownloadSpeed = view.findViewById(R.id.max_download_speed);
        avgDownloadSpeed = view.findViewById(R.id.avg_download_speed);
//        maxUploadSpeed = view.findViewById(R.id.max_upload_speed);
        avgUploadSpeed = view.findViewById(R.id.avg_upload_speed);
        minLatency = view.findViewById(R.id.min_latency);
        avgLatency = view.findViewById(R.id.avg_latency);

        networkIP.setText(mData.getString(NETWORK_IP));
        isp.setText(mData.getString(ISP));
        ispServer.setText(mData.getString(SERVER));
        ispRegion.setText(mData.getString(REGION));
//        maxDownloadSpeed.setText(getString(R.string.network_speed_mbps, mData.getString(MAX_DOWNLOAD_SPEED)));
        avgDownloadSpeed.setText(getString(R.string.network_speed_mbps, mData.getString(AVG_DOWNLOAD_SPEED)));
//        maxUploadSpeed.setText(getString(R.string.network_speed_mbps, mData.getString(MAX_UPLOAD_SPEED)));
        avgUploadSpeed.setText(getString(R.string.network_speed_mbps, mData.getString(AVG_UPLOAD_SPEED)));
        minLatency.setText(getString(R.string.network_latency_ms, mData.getString(MIN_LATENCY)));
        avgLatency.setText(getString(R.string.network_latency_ms, mData.getString(AVG_LATENCY)));

    }
}
