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
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.TrafficStats;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.airbnb.lottie.LottieAnimationView;
import com.drnoob.datamonitor.R;
import com.drnoob.datamonitor.ui.activities.ContainerActivity;
import com.google.android.material.snackbar.Snackbar;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;

import javax.net.ssl.HttpsURLConnection;

import fr.bmartel.speedtest.SpeedTestReport;
import fr.bmartel.speedtest.SpeedTestSocket;
import fr.bmartel.speedtest.inter.ISpeedTestListener;
import fr.bmartel.speedtest.model.SpeedTestError;
import fr.bmartel.speedtest.utils.SpeedTestUtils;
import io.ipinfo.api.IPinfo;
import io.ipinfo.api.model.IPResponse;

import static com.drnoob.datamonitor.core.Values.AVG_DOWNLOAD_SPEED;
import static com.drnoob.datamonitor.core.Values.AVG_LATENCY;
import static com.drnoob.datamonitor.core.Values.AVG_UPLOAD_SPEED;
import static com.drnoob.datamonitor.core.Values.DIAGNOSTICS_DOWNLOAD_URL;
import static com.drnoob.datamonitor.core.Values.DIAGNOSTICS_UPLOAD_URL;
import static com.drnoob.datamonitor.core.Values.GENERAL_FRAGMENT_ID;
import static com.drnoob.datamonitor.core.Values.ISP;
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
            currentTest;
    public static TextView currentConnectionType;
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
    private Boolean isNetworkConnected;
    private NetworkChangeMonitor mNetworkChangeMonitor;
    private ConstraintLayout mMeterView;
    private ImageView mNeedle;
    private ImageView mMeter;
    private TextView mCurrentSpeed;

    public native String getApiKey();

    private SpeedTest speedTest;
    private SpeedTestSocket downloadSpeedTestSocket,
            uploadSpeedTestSocket;

    private ConnectivityManager.NetworkCallback mNetworkCallback;

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

    @SuppressLint("ShowToast")
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

        mMeterView = view.findViewById(R.id.meter_view);
        mNeedle = view.findViewById(R.id.needle);
        mCurrentSpeed = view.findViewById(R.id.speed);
        mMeter = view.findViewById(R.id.meter);

        mDownloadSpeeds = new ArrayList<>();
        mUploadSpeeds = new ArrayList<>();
        mLatencies = new ArrayList<>();

        mNetworkChangeMonitor = new NetworkChangeMonitor(Objects.requireNonNull(getActivity()));

        mNetworkChangeMonitor.startMonitor();
        setConnectionStatus();

        runDiagnostics.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isNetworkConnected) {
                    runDiagnostics.setClickable(false);
                    runDiagnostics.setEnabled(false);
                    runDiagnostics.setBackground(getResources().getDrawable(R.drawable.button_run_diagnostics_running_background, null));
                    diagnosticsRunning.setVisibility(View.VISIBLE);
                    runDiagnostics.setText(R.string.connecting);
                    rippleView.pauseAnimation();
                    rippleView.setVisibility(View.INVISIBLE);
//                currentConnectionType.setVisibility(View.GONE);

                if (speedTest.getStatus() == AsyncTask.Status.FINISHED) {
                    speedTest = new SpeedTest(getActivity());
                }
//                new SpeedTest(requireActivity()).execute();
//                speedTest = new SpeedTest(getActivity());
//                    Log.e(TAG, "onClick: " + speedTest.getStatus() );
                    speedTest.execute();
                }
                else {
                    Snackbar.make(view, getString(R.string.no_network_connection),
                            Snackbar.LENGTH_SHORT).setAnchorView(Objects.requireNonNull(getActivity())
                            .findViewById(R.id.bottomNavigationView)).show();
                }

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
        try {
            if (downloadSpeedTestSocket != null) {
                downloadSpeedTestSocket.forceStopTask();
                downloadSpeedTestSocket.closeSocket();
            }
            if (uploadSpeedTestSocket != null) {
                uploadSpeedTestSocket.forceStopTask();
                uploadSpeedTestSocket.closeSocket();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        mNetworkChangeMonitor.stopMonitor();
        super.onPause();
    }

    @Keep
    private class SpeedTest extends AsyncTask<Object, String, Object> {
        private Activity activity;

        long sentBefore;
        long sentAfter;
        String downloadUrl, uploadUrl;

        public SpeedTest(Activity activity) {
            this.activity = activity;
        }

        @Override
        protected void onPreExecute() {
            Log.d(TAG, "onPreExecute: ");
            super.onPreExecute();
        }

        @Override
        protected Object doInBackground(Object[] objects) {
            final IPResponse[] ipResponse = new IPResponse[1];

            if (!isCancelled()) {
                try {
                    URL url = new URL(getString(R.string.api_url));
                    HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
//                    Scanner scanner = new Scanner(new URL(getString(R.string.api_url)).openStream(), "UTF-8").useDelimiter("\\A");
//                    String ip = scanner.next();

                    Scanner scanner = new Scanner(urlConnection.getInputStream());
                    String ip = scanner.next();

                    IPinfo ipInfo = new IPinfo.Builder()
                            .setToken(new String(Base64.decode(getApiKey(), Base64.DEFAULT)))
                            .build();

                    try {
                        Log.d(TAG, "doInBackground: " + ipInfo.lookupIP(ip));
                        ipResponse[0] = ipInfo.lookupIP(ip);
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                    mIpResponse = ipResponse[0];
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if (mIpResponse != null) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            onProgressUpdate("download");
                        }
                    });

                    downloadUrl = PreferenceManager.getDefaultSharedPreferences(activity)
                            .getString(DIAGNOSTICS_DOWNLOAD_URL, activity.getString(R.string.download_server_1_url));
                    downloadSpeedTestSocket = new SpeedTestSocket();
                    downloadSpeedTestSocket.startFixedDownload(downloadUrl, 15000, 1000);

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

                            uploadUrl = PreferenceManager.getDefaultSharedPreferences(activity)
                                    .getString(DIAGNOSTICS_UPLOAD_URL, activity.getString(R.string.upload_server_1_url));
                            uploadSpeedTestSocket = new SpeedTestSocket();
                            String fileName = SpeedTestUtils.generateFileName() + ".txt";
                            sentBefore = TrafficStats.getTotalTxBytes();
                            uploadSpeedTestSocket.startFixedUpload(uploadUrl + fileName, 10000000, 10000, 1000);

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
                                            String host = "1.1.1.1";
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

                                        }
                                    catch (IOException e) {
                                            e.printStackTrace();
                                        }

                                    }

                                    onPostExecute("upload");
                                }

                                @Override
                                public void onError(SpeedTestError speedTestError, String errorMessage) {
                                    // called when a download/upload error occur
                                    Log.e(TAG, "onError: " + errorMessage);
                                    onPostExecute("error");
                                }

                                @Override
                                public void onProgress(float percent, SpeedTestReport report) {
                                    // Called to notify upload progress
                                    Log.v("speedtest", "[PROGRESS] progress : " + percent + "%");
                                    Log.v("speedtest", "[PROGRESS] rate in octet/s : " + report.getTransferRateOctet());
//                                mUploadSpeeds.add(((report.getTransferRateOctet().floatValue() / 1024) / 1024) * 8);

                                    sentAfter = TrafficStats.getTotalTxBytes();
                                    Long progressBytes = sentAfter - sentBefore;
                                    sentBefore = sentAfter;
                                    Float currentSpeed = (progressBytes.floatValue() / (1024 * 1024)) * 8;

                                    activity.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            onProgressUpdate("upload_speed",
                                                    getString(R.string.network_speed_mbps, String.format("%.2f", currentSpeed)),
                                                    currentSpeed.toString());
                                        }
                                    });
                                }
                            });

                            onPostExecute("download");
                        }

                        @Override
                        public void onError(SpeedTestError speedTestError, String errorMessage) {
                            // Called when a download error occur
                            Log.e(TAG, "onError: " + errorMessage);
                            onPostExecute("error");
                        }

                        @Override
                        public void onProgress(float percent, SpeedTestReport report) {
                            // Called to notify download progress
                            Log.v("speedtest", "[PROGRESS] progress : " + percent + "%");
                            Log.v("speedtest", "[PROGRESS] rate in octet/s : " + report.getTransferRateOctet());
//                        mDownloadSpeeds.add(((report.getTransferRateOctet().floatValue() / 1024) / 1024) * 8);
                            Float currentSpeed = ((report.getTransferRateOctet()
                                    .floatValue() / 1024) / 1024) * 8;

                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    onProgressUpdate("download_speed",
                                            activity.getString(R.string.network_speed_mbps, String.format("%.1f", currentSpeed)),
                                            currentSpeed.toString());
                                }
                            });
                        }
                    });

                }
                else {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            onProgressUpdate("connection_error");
                        }
                    });
                }
            }

            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            float speed, rotation;
            int meterDrawable;
            if (getContext() != null) {
                if (values != null) {
                    String state = values[0];
                    switch (state) {
                        case "download":
                            DisplayMetrics metrics = new DisplayMetrics();
                            activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);

                            if (mIpResponse != null) {
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
                                        .alpha(0f)
                                        .setListener(new AnimatorListenerAdapter() {
                                            @Override
                                            public void onAnimationEnd(Animator animation) {
                                                super.onAnimationEnd(animation);
                                                runDiagnostics.setText(R.string.running_diagnostics);
                                                currentTest.setText(activity.getString(R.string.testing_download));
                                                currentTest.setVisibility(View.VISIBLE);
                                                currentTest.setAlpha(1f);
//                                                currentTestAnim.setVisibility(View.VISIBLE);


                                                diagnosticsView.setVisibility(View.GONE);
                                                diagnosticsView.setAlpha(1f);
                                            }

                                            @Override
                                            public void onAnimationStart(Animator animation) {
                                                super.onAnimationStart(animation);
                                                mMeterView.setAlpha(0f);
                                                mMeterView.setVisibility(View.VISIBLE);
                                                mMeterView.animate()
                                                        .alpha(1f)
                                                        .setListener(new AnimatorListenerAdapter() {
                                                            @Override
                                                            public void onAnimationEnd(Animator animation) {
                                                                super.onAnimationEnd(animation);
                                                            }
                                                        })
                                                        .start();
                                                mNeedle.animate()
                                                        .rotation(0f)
                                                        .setDuration(1000)
                                                        .start();
                                                mCurrentSpeed.setText(activity.getString(R.string.network_speed_mbps, "0"));
                                            }
                                        })
                                        .start();
                            }
                            break;
                        case "upload":
                            mMeter.setImageResource(R.drawable.ic_meter);
                            mNeedle.animate()
                                    .rotation(0f)
                                    .setDuration(450)
                                    .start();
                            currentTest.setText(activity.getString(R.string.testing_upload));
                            mCurrentSpeed.setText(activity.getString(R.string.network_speed_mbps, "0"));
                            break;

                        case "latency":
                            mMeterView.animate()
                                    .scaleY(0.8f)
                                    .scaleX(0.8f)
                                    .alpha(0f)
                                    .setListener(new AnimatorListenerAdapter() {
                                        @Override
                                        public void onAnimationEnd(Animator animation) {
                                            super.onAnimationEnd(animation);
                                            currentTestAnim.setVisibility(View.VISIBLE);
                                            currentTestAnim.playAnimation();
                                            currentTestAnim.animate()
                                                    .scaleX(1)
                                                    .scaleY(1)
                                                    .alpha(1)
                                                    .start();

                                            mMeterView.setVisibility(View.GONE);
                                            mMeterView.setAlpha(1);
                                            mMeterView.setScaleX(1);
                                            mMeterView.setScaleY(1);
                                        }

                                        @Override
                                        public void onAnimationStart(Animator animation) {
                                            super.onAnimationStart(animation);
                                            currentTestAnim.setScaleX(0.8f);
                                            currentTestAnim.setScaleY(0.8f);
                                            currentTestAnim.setAlpha(0f);
                                        }
                                    })
                                    .start();
                            mCurrentSpeed.setText(activity.getString(R.string.network_speed_mbps, "0.00"));
                            currentTest.setText(activity.getString(R.string.testing_latency));
                            break;

                        case "connection_error":
                            Snackbar.make(Objects.requireNonNull(getView()),
                                    activity.getString(R.string.connection_error), Snackbar.LENGTH_SHORT)
                                    .setAnchorView(activity.findViewById(R.id.bottomNavigationView))
                                    .show();

                            runDiagnostics.setClickable(true);
                            runDiagnostics.setEnabled(true);
                            diagnosticsInfo.setVisibility(View.VISIBLE);
                            diagnosticsInfo.animate()
                                    .translationY(0)
                                    .start();

                            diagnosticsView.setAlpha(0f);
                            diagnosticsView.setVisibility(View.VISIBLE);

                            diagnosticsView.animate()
                                    .translationY(0)
                                    .alpha(1f)
                                    .setListener(new AnimatorListenerAdapter() {
                                        @Override
                                        public void onAnimationStart(Animator animation) {
                                            super.onAnimationStart(animation);
                                            runDiagnostics.setBackground(getResources().getDrawable(R.drawable.button_run_diagnostics_background, null));
                                            runDiagnostics.setText(R.string.run_diagnostics);
                                            setConnectionStatus();
                                            currentTest.setVisibility(View.GONE);
                                            currentTestAnim.setVisibility(View.GONE);
                                            currentTestAnim.pauseAnimation();
                                            diagnosticsRunning.setVisibility(View.GONE);
                                            mMeterView.setVisibility(View.GONE);
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

                            break;

                        case "download_speed":
                            speed = Float.valueOf(values[2]);
                            mCurrentSpeed.setText(values[1]);
                            rotation = getPositionByRate(speed);
                            meterDrawable = getMeterDrawableId(speed);
                            mMeter.setImageResource(meterDrawable);
                            mNeedle.animate()
                                    .rotation(rotation)
                                    .setDuration(450)
                                    .start();
                            break;

                        case "upload_speed":
                            mCurrentSpeed.setText(values[1]);
                            speed = Float.valueOf(values[2]);
                            rotation = getPositionByRate(speed);
                            meterDrawable = getMeterDrawableId(speed);
                            mMeter.setImageResource(meterDrawable);
                            mNeedle.animate()
                                    .rotation(rotation)
                                    .setDuration(450)
                                    .start();
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
                                            try {
                                                bundle.putString(ISP, mIpResponse.getOrg().
                                                        replace(mIpResponse.getOrg().split(" ")[0], ""));
                                            }
                                            catch (NullPointerException e) {
                                                e.printStackTrace();
                                            }
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

                                    diagnosticsView.setAlpha(0f);
                                    diagnosticsView.setVisibility(View.VISIBLE);

                                    currentTestAnim.animate()
                                            .alpha(0f)
                                            .setListener(new AnimatorListenerAdapter() {
                                                @Override
                                                public void onAnimationStart(Animator animation) {
                                                    super.onAnimationStart(animation);
                                                    diagnosticsView.animate()
                                                            .translationY(0)
                                                            .alpha(1f)
                                                            .setListener(new AnimatorListenerAdapter() {
                                                                @Override
                                                                public void onAnimationStart(Animator animation) {
                                                                    super.onAnimationStart(animation);
                                                                    runDiagnostics.setBackground(getResources().getDrawable(R.drawable.button_run_diagnostics_background, null));
                                                                    runDiagnostics.setText(R.string.run_diagnostics);
                                                                    setConnectionStatus();
                                                                    currentTest.setVisibility(View.GONE);
                                                                    currentTestAnim.setVisibility(View.GONE);
                                                                    currentTestAnim.pauseAnimation();
                                                                    diagnosticsRunning.setVisibility(View.GONE);
                                                                }

                                                                @Override
                                                                public void onAnimationEnd(Animator animation) {
                                                                    super.onAnimationEnd(animation);
                                                                    rippleView.setVisibility(View.VISIBLE);
                                                                    rippleView.playAnimation();
//                                            currentConnectionType.setVisibility(View.VISIBLE);

                                                                    mMeterView.setVisibility(View.GONE);
                                                                }
                                                            })
                                                            .start();
                                                }
                                            })
                                            .start();
                                }
                            });
                        }
                    }
                    else if (o.toString().equalsIgnoreCase("error")) {
                        if (getContext() != null) {
                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    runDiagnostics.setClickable(true);
                                    runDiagnostics.setEnabled(true);
                                    diagnosticsInfo.setVisibility(View.VISIBLE);
                                    diagnosticsInfo.animate()
                                            .translationY(0)
                                            .start();

                                    currentTestAnim.animate()
                                            .alpha(0f)
                                            .setListener(new AnimatorListenerAdapter() {
                                                @Override
                                                public void onAnimationStart(Animator animation) {
                                                    super.onAnimationStart(animation);
                                                    diagnosticsView.setAlpha(0f);
                                                    diagnosticsView.setVisibility(View.VISIBLE);
                                                    diagnosticsView.animate()
                                                            .translationY(0)
                                                            .alpha(1f)
                                                            .setListener(new AnimatorListenerAdapter() {
                                                                @Override
                                                                public void onAnimationStart(Animator animation) {
                                                                    super.onAnimationStart(animation);
                                                                    runDiagnostics.setBackground(activity.getResources()
                                                                            .getDrawable(R.drawable.button_run_diagnostics_background, null));
                                                                    runDiagnostics.setText(R.string.run_diagnostics);
                                                                    setConnectionStatus();
                                                                    currentTest.setVisibility(View.GONE);
                                                                    currentTestAnim.setVisibility(View.GONE);
                                                                    currentTestAnim.pauseAnimation();
                                                                    diagnosticsRunning.setVisibility(View.GONE);
                                                                }

                                                                @Override
                                                                public void onAnimationEnd(Animator animation) {
                                                                    super.onAnimationEnd(animation);
                                                                    rippleView.setVisibility(View.VISIBLE);
                                                                    rippleView.playAnimation();
//                                            currentConnectionType.setVisibility(View.VISIBLE);

                                                                    mMeterView.setVisibility(View.GONE);
                                                                }
                                                            })
                                                            .start();
                                                }
                                            })
                                            .start();
                                    Snackbar.make(Objects.requireNonNull(getView()),
                                            activity.getString(R.string.connection_error), Snackbar.LENGTH_SHORT)
                                            .setAnchorView(activity.findViewById(R.id.bottomNavigationView))
                                            .show();
                                }
                            });
                        }
                    }
                }
            }
        }
    }

    private int getMeterDrawableId(float speed) {
        if (speed >= 0 && speed < 5) {
            return R.drawable.ic_meter_0;
        }
        else if (speed >= 5 && speed < 10) {
            return R.drawable.ic_meter_5;
        }
        else if (speed >= 10 && speed < 15) {
            return R.drawable.ic_meter_10;
        }
        else if (speed >= 15 && speed < 20) {
            return R.drawable.ic_meter_15;
        }
        else if (speed >= 20 && speed < 25) {
            return R.drawable.ic_meter_20;
        }
        else if (speed >= 25 && speed < 30) {
            return R.drawable.ic_meter_25;
        }
        else if (speed >= 30 && speed < 50) {
            return R.drawable.ic_meter_30;
        }
        else if (speed >= 50 && speed < 75) {
            return R.drawable.ic_meter_50;
        }
        else if (speed >= 75 && speed < 100) {
            return R.drawable.ic_meter_75;
        }
        else if (speed > 100) {
            return R.drawable.ic_meter_100;
        }
        return R.drawable.ic_meter;
    }

    public float getPositionByRate(float rate) {
        if (rate >= 0 && rate <= 30) {
            return rate * 6;
        }
        else if (rate >= 30 && rate <= 50) {
            return (float) ((rate - 30) * 1.5) + 180;
        }
        else if (rate > 50 && rate <= 100) {
            return (float) ((rate - 50) * 1.2) + 210;
        }
        else if (rate > 100) {
            return 270;
        }
        return 0;
    }

    private int getConnectivityStatus(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();
        if (info != null) {
            isNetworkConnected = true;
            return info.getType();
        }
        isNetworkConnected = false;
        return -1;
    }

    private void setConnectionStatus() {
        switch (getConnectivityStatus(Objects.requireNonNull(getContext()))) {
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

            case -1:
                mCurrentConnectionType = "Disconnected";
                break;

            default:
                mCurrentConnectionType = "Unknown";
                break;
        }
        currentConnectionType.setText(getContext().getString(R.string.current_connection, mCurrentConnectionType));
    }

    private class NetworkChangeMonitor extends ConnectivityManager.NetworkCallback {
        final NetworkRequest networkRequest;

        private ConnectivityManager connectivityManager;
        private Activity activity;

        public NetworkChangeMonitor(Activity activity) {
            this.activity = activity;
            connectivityManager = (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
            networkRequest = new NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .addTransportType(NetworkCapabilities.TRANSPORT_BLUETOOTH)
                    .addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET)
                    .build();
        }

        public void startMonitor() {
            if (connectivityManager != null) {
                connectivityManager.registerNetworkCallback(networkRequest, this);
            }
            else {
                connectivityManager = (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
                startMonitor();
            }
        }

        public void stopMonitor() {
            if (connectivityManager != null) {
                try {
                    connectivityManager.unregisterNetworkCallback(this);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
            else {
                connectivityManager = (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
                stopMonitor();
            }
        }

        @Override
        public void onAvailable(@NonNull Network network) {
            super.onAvailable(network);
            Log.d(TAG, "onAvailable: ");
            isNetworkConnected = true;
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    setConnectionStatus();
                }
            });
        }

        @Override
        public void onLost(@NonNull Network network) {
            super.onLost(network);
            Log.d(TAG, "onLost: ");
            isNetworkConnected = false;
            speedTest.onPostExecute("error");
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    setConnectionStatus();
                }
            });
        }
    }
}
