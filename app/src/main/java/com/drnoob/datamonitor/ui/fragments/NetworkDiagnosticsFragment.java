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

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

import com.airbnb.lottie.LottieAnimationView;
import com.drnoob.datamonitor.R;
import com.drnoob.datamonitor.ui.activities.ContainerActivity;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import fr.bmartel.speedtest.SpeedTestReport;
import fr.bmartel.speedtest.SpeedTestSocket;
import fr.bmartel.speedtest.inter.ISpeedTestListener;
import fr.bmartel.speedtest.model.SpeedTestError;
import io.ipinfo.api.IPinfo;
import io.ipinfo.api.errors.RateLimitedException;
import io.ipinfo.api.model.IPResponse;

import static com.drnoob.datamonitor.core.Values.AVG_DOWNLOAD_SPEED;
import static com.drnoob.datamonitor.core.Values.AVG_LATENCY;
import static com.drnoob.datamonitor.core.Values.AVG_UPLOAD_SPEED;
import static com.drnoob.datamonitor.core.Values.GENERAL_FRAGMENT_ID;
import static com.drnoob.datamonitor.core.Values.ISP;
import static com.drnoob.datamonitor.core.Values.MAX_DOWNLOAD_SPEED;
import static com.drnoob.datamonitor.core.Values.MAX_UPLOAD_SPEED;
import static com.drnoob.datamonitor.core.Values.MIN_LATENCY;
import static com.drnoob.datamonitor.core.Values.NETWORK_IP;
import static com.drnoob.datamonitor.core.Values.NETWORK_STATS_FRAGMENT;
import static com.drnoob.datamonitor.core.Values.REGION;
import static com.drnoob.datamonitor.core.Values.SERVER;

@Keep
public class NetworkDiagnosticsFragment extends Fragment {
    private static final String TAG = NetworkDiagnosticsFragment.class.getSimpleName();

    private TextView runDiagnostics,
            diagnosticsInfo,
            currentTest,
            currentConnectionType;
    private LottieAnimationView rippleView,
            currentTestAnim;
    private ConstraintLayout diagnosticsView;
    private ProgressBar diagnosticsRunning;

    private Float mDownloadSpeed,
            mUploadSpeed;
    private Long mLatency;
    private IPResponse mIpResponse;
    private Boolean isDiagnosisRunning = false;
    private String mCurrentConnectionType;
    private List<Float> mDownloadSpeeds,
            mUploadSpeeds;
    private List<Long> mLatencies;
    private Float mMaxDownloadSpeed = 0F,
            mMaxUploadSpeed = 0F;
    private Long mMinLatency = 0L;

    public native String getApiKey();

    private SpeedTest speedTest;
    private SpeedTestSocket downloadSpeedTestSocket,
            uploadSpeedTestSocket;

    static {
        System.loadLibrary("keys");
    }

    @Override
    public void onCreate(@Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @org.jetbrains.annotations.Nullable
    @Override
    public View onCreateView(@NonNull @NotNull LayoutInflater inflater, @Nullable @org.jetbrains.annotations.Nullable ViewGroup container, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_network_diagnostics, container, false);
    }

    @Override
    public void onViewCreated(@NonNull @NotNull View view, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        runDiagnostics = view.findViewById(R.id.run_diagnostics);
        rippleView = view.findViewById(R.id.ripple_view);
        diagnosticsInfo = view.findViewById(R.id.info);
        currentTest = view.findViewById(R.id.current_test);
        currentTestAnim = view.findViewById(R.id.testing_anim);
        diagnosticsView = view.findViewById(R.id.test_view);
        diagnosticsRunning = view.findViewById(R.id.diagnostics_running);
        currentConnectionType = view.findViewById(R.id.current_connection);

        mDownloadSpeeds = new ArrayList<>();
        mUploadSpeeds = new ArrayList<>();
        mLatencies = new ArrayList<>();

        ConnectivityManager cm = (ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();
        int currentConnection = info.getType();
        switch (currentConnection) {
            case ConnectivityManager.TYPE_MOBILE:
                mCurrentConnectionType = "Mobile Data";
                break;

            case ConnectivityManager.TYPE_WIFI:
                mCurrentConnectionType = "Wifi";
                break;

            case ConnectivityManager.TYPE_BLUETOOTH:
                mCurrentConnectionType = "Bluetooth";
                break;

            case ConnectivityManager.TYPE_ETHERNET:
                mCurrentConnectionType = "Ethernet";
                break;

            default:
                mCurrentConnectionType = "Unknown";
                break;
        }
        currentConnectionType.setText(getString(R.string.current_connection, mCurrentConnectionType));

        runDiagnostics.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                runDiagnostics.setClickable(false);
                runDiagnostics.setEnabled(false);
                runDiagnostics.setBackground(getResources().getDrawable(R.drawable.button_run_diagnostics_running_background, null));
                diagnosticsRunning.setVisibility(View.VISIBLE);
                runDiagnostics.setText(R.string.connecting);
                rippleView.pauseAnimation();
                rippleView.setVisibility(View.INVISIBLE);
//                currentConnectionType.setVisibility(View.GONE);

//                if (speedTest.getStatus() == AsyncTask.Status.FINISHED) {
//                    speedTest = new SpeedTest(getActivity());
//                }
//                new SpeedTest(requireActivity()).execute();
//                speedTest = new SpeedTest(getActivity());
                speedTest.execute();

            }
        });

    }

    @Override
    public void onStart() {
        speedTest = new SpeedTest(getActivity());
        super.onStart();
    }

    @Override
    public void onPause() {
        speedTest.cancel(true);
        if (downloadSpeedTestSocket != null) {
            downloadSpeedTestSocket.forceStopTask();
            downloadSpeedTestSocket.closeSocket();
        }
        if (uploadSpeedTestSocket != null) {
            uploadSpeedTestSocket.forceStopTask();
            uploadSpeedTestSocket.closeSocket();
        }
        super.onPause();
    }

    @Keep
    private class SpeedTest extends AsyncTask<Object, String, Object> {
        private Activity activity;

        public SpeedTest(Activity activity) {
            this.activity = activity;
        }

        @Override
        protected void onPreExecute() {
            Log.e(TAG, "onPreExecute: ");
            super.onPreExecute();
        }

        @Override
        protected Object doInBackground(Object[] objects) {
            final IPResponse[] ipResponse = new IPResponse[1];
            if (!isCancelled()) {
                try {
                    Scanner scanner = new Scanner(new URL(getString(R.string.api_url)).openStream(), "UTF-8").useDelimiter("\\A");
                    String ip = scanner.next();

                    IPinfo ipInfo = new IPinfo.Builder()
                            .setToken(new String(Base64.decode(getApiKey(), Base64.DEFAULT)))
                            .build();

                    Log.d(TAG, "doInBackground: " + ipInfo.lookupIP(ip));
                    ipResponse[0] = ipInfo.lookupIP(ip);
                    mIpResponse = ipResponse[0];
                } catch (RateLimitedException | IOException e) {
                    e.printStackTrace();
                }

                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        onProgressUpdate("download");
                    }
                });

                downloadSpeedTestSocket = new SpeedTestSocket();
                downloadSpeedTestSocket.startFixedDownload(activity.getString(R.string.download_test_url), 15000, 1000);

                // add a listener to wait for speedtest completion and progress
                downloadSpeedTestSocket.addSpeedTestListener(new ISpeedTestListener() {

                    @Override
                    public void onCompletion(SpeedTestReport report) {
                        // Called when download is finished
                        Log.v("speedtest", "[COMPLETED] rate in octet/s : " + report.getTransferRateOctet());
                        mDownloadSpeed = ((report.getTransferRateOctet().floatValue() / 1024) / 1024) * 8;

                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                onProgressUpdate("upload");
                            }
                        });

                        uploadSpeedTestSocket = new SpeedTestSocket();
                        uploadSpeedTestSocket.startFixedUpload(activity.getString(R.string.upload_test_url), 10000000, 10000);

                        // add a listener to wait for speedtest completion and progress
                        uploadSpeedTestSocket.addSpeedTestListener(new ISpeedTestListener() {

                            @Override
                            public void onCompletion(SpeedTestReport report) {
                                // Called when upload is finished
                                Log.v("speedtest", "[COMPLETED] rate in octet/s : " + report.getTransferRateOctet());
                                mUploadSpeed = ((report.getTransferRateOctet().floatValue() / 1024) / 1024) * 8;
                                activity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        onProgressUpdate("latency");
                                    }
                                });

                                if (!isCancelled()) {
                                    try {
                                        String host = "www.google.com";
                                        int timeout = 10000;
                                        for (int i = 0; i < 20; i++) {
                                            long beforeTime = System.currentTimeMillis();
                                            boolean reachable = InetAddress.getByName(host).isReachable(timeout);
                                            long afterTime = System.currentTimeMillis();
                                            long latency = afterTime - beforeTime;
                                            Log.d(TAG, "latency: " + latency);
                                            mLatencies.add(latency);
                                        }
                                        Long sum = 0L;
                                        for (int j = 0; j < mLatencies.size(); j++) {
                                            sum += mLatencies.get(j);
                                        }
                                        mLatency = sum / mLatencies.size();
                                        onPostExecute("complete");
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }

                                onPostExecute("upload");
                            }

                            @Override
                            public void onError(SpeedTestError speedTestError, String errorMessage) {
                                // called when a download/upload error occur
                                Log.e(TAG, "onError: " + errorMessage);
                            }

                            @Override
                            public void onProgress(float percent, SpeedTestReport report) {
                                // Called to notify upload progress
                                Log.v("speedtest", "[PROGRESS] progress : " + percent + "%");
                                Log.v("speedtest", "[PROGRESS] rate in octet/s : " + report.getTransferRateOctet());
//                                mUploadSpeeds.add(((report.getTransferRateOctet().floatValue() / 1024) / 1024) * 8);
                            }
                        });

                        onPostExecute("download");
                    }

                    @Override
                    public void onError(SpeedTestError speedTestError, String errorMessage) {
                        // Called when a download error occur
                        Log.e(TAG, "onError: " + errorMessage);
                    }

                    @Override
                    public void onProgress(float percent, SpeedTestReport report) {
                        // Called to notify download progress
//                    onProgressUpdate(report);
                        Log.v("speedtest", "[PROGRESS] progress : " + percent + "%");
                        Log.v("speedtest", "[PROGRESS] rate in octet/s : " + report.getTransferRateOctet());
//                        mDownloadSpeeds.add(((report.getTransferRateOctet().floatValue() / 1024) / 1024) * 8);
                    }
                });
            }

            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            if (getContext() != null) {
                if (values != null) {
                    String state = values[0];
                    switch (state) {
                        case "download":
                            DisplayMetrics metrics = new DisplayMetrics();
                            activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);

                            currentConnectionType.setText(activity.getString(R.string.connected_server,
                                    mIpResponse.getCity()));

                            diagnosticsInfo.animate()
                                    .translationYBy((getView().getPivotY() + 100) * -1)
                                    .alpha(0f)
                                    .setListener(new AnimatorListenerAdapter() {
                                        @Override
                                        public void onAnimationEnd(Animator animation) {
                                            super.onAnimationEnd(animation);
//                                        diagnosticsInfo.setVisibility(View.GONE);
                                            diagnosticsInfo.setAlpha(1f);
                                        }
                                    })
                                    .start();

                            diagnosticsView.animate()
                                    .translationY(((getView().getPivotY()) - 300) * -1)
                                    .setListener(new AnimatorListenerAdapter() {
                                        @Override
                                        public void onAnimationEnd(Animator animation) {
                                            super.onAnimationEnd(animation);
                                            runDiagnostics.setText(R.string.running_diagnostics);
                                            currentTest.setText(getString(R.string.testing_download));
                                            currentTest.setVisibility(View.VISIBLE);
                                            currentTestAnim.setVisibility(View.VISIBLE);
                                        }
                                    })
                                    .start();
                            break;
                        case "upload":
                            currentTest.setText(activity.getString(R.string.testing_upload));
                            break;

                        case "latency":
                            currentTest.setText(activity.getString(R.string.testing_latency));
                            break;

                        default:
                            currentTest.setText(activity.getString(R.string.testing_download));
                            break;
                    }
                }
            }

            super.onProgressUpdate(values);
        }

        @Override
        protected void onPostExecute(Object o) {
            super.onPostExecute(o);
            if (getContext() != null) {
                if (o != null) {
                    if (o.toString().equals("complete")) {
                        if (getContext() != null) {
                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    new Thread(new Runnable() {
                                        @SuppressLint("DefaultLocale")
                                        @Override
                                        public void run() {
//                                            Float maxDownload = mDownloadSpeeds.get(0);
//                                            Float maxUpload = mUploadSpeeds.get(0);
                                            Long minLatency = mLatencies.get(0);
//                                            for (int i = 0; i < mDownloadSpeeds.size(); i++) {
//                                                if (mDownloadSpeeds.get(i) > maxDownload) {
//                                                    maxDownload = mDownloadSpeeds.get(i);
//                                                } else {
//                                                    maxDownload = maxDownload;
//                                                }
//                                            }
//                                            for (int j = 0; j < mUploadSpeeds.size(); j++) {
//                                                if (mUploadSpeeds.get(j) > maxUpload) {
//                                                    maxUpload = mUploadSpeeds.get(j);
//                                                } else {
//                                                    maxUpload = maxUpload;
//                                                }
//                                            }
                                            for (int k = 0; k < mLatencies.size(); k++) {
                                                if (mLatencies.get(k) < minLatency) {
                                                    minLatency = mLatencies.get(k);
                                                } else {
                                                    minLatency = minLatency;
                                                }
                                            }
//                                            mMaxDownloadSpeed = maxDownload;
//                                            mMaxUploadSpeed = maxUpload;
                                            mMinLatency = minLatency;

                                            Bundle bundle = new Bundle();
//                                            bundle.putString(MAX_DOWNLOAD_SPEED, String.format("%.2f", mMaxDownloadSpeed));
                                            bundle.putString(AVG_DOWNLOAD_SPEED, String.format("%.2f", mDownloadSpeed));
//                                            bundle.putString(MAX_UPLOAD_SPEED, String.format("%.2f", mMaxUploadSpeed));
                                            bundle.putString(AVG_UPLOAD_SPEED, String.format("%.2f", mUploadSpeed));
                                            bundle.putString(MIN_LATENCY, String.valueOf(mMinLatency));
                                            bundle.putString(AVG_LATENCY, String.valueOf(mLatency));
                                            bundle.putString(NETWORK_IP, mIpResponse.getIp());
                                            bundle.putString(ISP, mIpResponse.getOrg().
                                                    replace(mIpResponse.getOrg().split(" ")[0], ""));
                                            bundle.putString(SERVER, mIpResponse.getCity());
                                            bundle.putString(REGION, mIpResponse.getRegion());

                                            Intent intent = new Intent(activity, ContainerActivity.class);
                                            intent.putExtra(GENERAL_FRAGMENT_ID, NETWORK_STATS_FRAGMENT);
                                            intent.putExtras(bundle);
                                            if (getContext() != null) {
                                                startActivity(intent);
                                            }

                                        }
                                    }).start();

                                    runDiagnostics.setClickable(true);
                                    runDiagnostics.setEnabled(true);
                                    diagnosticsInfo.setVisibility(View.VISIBLE);
                                    diagnosticsInfo.animate()
                                            .translationY(0)
                                            .start();

                                    diagnosticsView.animate()
                                            .translationY(0)
                                            .setListener(new AnimatorListenerAdapter() {
                                                @Override
                                                public void onAnimationStart(Animator animation) {
                                                    super.onAnimationStart(animation);
                                                    runDiagnostics.setBackground(getResources().getDrawable(R.drawable.button_run_diagnostics_background, null));
                                                    runDiagnostics.setText(R.string.run_diagnostics);
                                                    currentConnectionType.setText(activity.getString(R.string.current_connection, mCurrentConnectionType));
                                                    currentTest.setVisibility(View.GONE);
                                                    currentTestAnim.setVisibility(View.GONE);
                                                    diagnosticsRunning.setVisibility(View.GONE);
                                                }

                                                @Override
                                                public void onAnimationEnd(Animator animation) {
                                                    super.onAnimationEnd(animation);
                                                    rippleView.setVisibility(View.VISIBLE);
                                                    rippleView.playAnimation();
//                                            currentConnectionType.setVisibility(View.VISIBLE);
                                                }
                                            })
                                            .start();
                                }
                            });
                        }
                    }
                }
            }
        }
    }
}
