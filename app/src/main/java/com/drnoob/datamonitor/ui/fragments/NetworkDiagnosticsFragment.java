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
import static com.drnoob.datamonitor.core.Values.DIAGNOSTICS_DOWNLOAD_URL;
import static com.drnoob.datamonitor.core.Values.DIAGNOSTICS_HISTORY_FRAGMENT;
import static com.drnoob.datamonitor.core.Values.DIAGNOSTICS_HISTORY_LIST;
import static com.drnoob.datamonitor.core.Values.DIAGNOSTICS_UPLOAD_URL;
import static com.drnoob.datamonitor.core.Values.GENERAL_FRAGMENT_ID;
import static com.drnoob.datamonitor.core.Values.ISP;
import static com.drnoob.datamonitor.core.Values.MIN_LATENCY;
import static com.drnoob.datamonitor.core.Values.NETWORK_IP;
import static com.drnoob.datamonitor.core.Values.NETWORK_STATS_FRAGMENT;
import static com.drnoob.datamonitor.core.Values.REGION;
import static com.drnoob.datamonitor.core.Values.SERVER;

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
import android.os.SystemClock;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.airbnb.lottie.LottieAnimationView;
import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.drnoob.datamonitor.R;
import com.drnoob.datamonitor.adapters.data.DiagnosticsHistoryModel;
import com.drnoob.datamonitor.ui.activities.ContainerActivity;
import com.drnoob.datamonitor.utils.SharedPreferences;
import com.google.android.material.snackbar.Snackbar;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;

import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

import fr.bmartel.speedtest.SpeedTestReport;
import fr.bmartel.speedtest.SpeedTestSocket;
import fr.bmartel.speedtest.inter.ISpeedTestListener;
import fr.bmartel.speedtest.model.SpeedTestError;
import fr.bmartel.speedtest.utils.SpeedTestUtils;
import io.ipinfo.api.model.IPResponse;

@Keep
public class NetworkDiagnosticsFragment extends Fragment {
    private static final String TAG = NetworkDiagnosticsFragment.class.getSimpleName();

    private TextView runDiagnostics,
            currentTest;
    private LinearLayout diagnosticsInfo, history;
    public static TextView currentConnectionType;
    private LottieAnimationView rippleView,
            currentTestAnim;
    private ConstraintLayout diagnosticsView;
    private ProgressBar diagnosticsRunning;

    private Float mDownloadSpeed,
            mUploadSpeed;
    private Long mLatency;
    private IPResponse mIpResponse;
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
    private Context context;
    private boolean isTestPaused = false;
    private boolean shouldShowResultOnResume = false;
    private boolean shouldShowErrorOnResume = false;
    private Bundle resultBundle;
    private Intent resultIntent;
    private Snackbar errorSnackbar;

    public native String getApiKey();
    public native String getToken();

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

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        this.context = null;
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
        history = view.findViewById(R.id.history);

        mMeterView = view.findViewById(R.id.meter_view);
        mNeedle = view.findViewById(R.id.needle);
        mCurrentSpeed = view.findViewById(R.id.speed);
        mMeter = view.findViewById(R.id.meter);

        mDownloadSpeeds = new ArrayList<>();
        mUploadSpeeds = new ArrayList<>();
        mLatencies = new ArrayList<>();

        mNetworkChangeMonitor = new NetworkChangeMonitor(requireActivity());

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

                    String ipLookupUrl = requireContext().getString(R.string.api_ip_lookup);
                    Volley.newRequestQueue(requireContext()).add(
                            new StringRequest(
                                    Request.Method.GET,
                                    ipLookupUrl,
                                    new Response.Listener<String>() {
                                        @Override
                                        public void onResponse(String response) {
                                            if (getContext() != null) {
                                                String ip = "",
                                                        city = "",
                                                        region = "",
                                                        org = "";
                                                try {
                                                    JSONObject result = new JSONObject(response);
                                                    ip = result.getString("ip");
                                                    city = result.getString("city");
                                                    region = result.getString("region");
                                                    org = result.getString("org");

                                                }
                                                catch (Exception e) {
                                                    e.printStackTrace();
                                                }
                                                mIpResponse = new IPResponse(
                                                        ip, requireContext().getString(R.string.label_unknown),
                                                        false,
                                                        city,
                                                        region,
                                                        requireContext().getString(R.string.label_unknown),
                                                        requireContext().getString(R.string.label_unknown),
                                                        org,
                                                        requireContext().getString(R.string.label_unknown),
                                                        requireContext().getString(R.string.label_unknown),
                                                        null, null, null, null, null, null
                                                );

                                                if (getActivity() != null) {
                                                    if (speedTest.getStatus() == AsyncTask.Status.FINISHED) {
                                                        speedTest = new SpeedTest(requireActivity());
                                                    }
                                                    speedTest.execute();
                                                }
                                            }
                                        }
                                    },
                                    new Response.ErrorListener() {
                                        @Override
                                        public void onErrorResponse(VolleyError error) {
                                            String errorMessage = requireContext().getString(R.string.error_unknown);
                                            try {
                                                errorMessage = new String(error.networkResponse.data, StandardCharsets.UTF_8);
                                                runDiagnostics.setClickable(true);
                                                runDiagnostics.setEnabled(true);
                                                diagnosticsInfo.setVisibility(View.VISIBLE);
                                                diagnosticsInfo.animate()
                                                        .translationY(0)
                                                        .start();

                                                currentConnectionType.animate()
                                                        .alpha(0f)
                                                        .setListener(new AnimatorListenerAdapter() {
                                                            @Override
                                                            public void onAnimationEnd(Animator animation) {
                                                                history.setVisibility(View.VISIBLE);
                                                                super.onAnimationEnd(animation);
                                                                currentConnectionType.animate()
                                                                        .alpha(1f)
                                                                        .start();
                                                            }
                                                        })
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
                                                            }
                                                        })
                                                        .start();
                                            }
                                            catch (Exception e) {
                                                e.printStackTrace();
                                            }
                                            errorSnackbar = Snackbar.make(view, errorMessage, Snackbar.LENGTH_SHORT)
                                                    .setAnchorView(requireActivity().findViewById(R.id.bottomNavigationView));
                                            if (!isTestPaused) {
                                                errorSnackbar.show();
                                            }
                                            else {
                                                shouldShowErrorOnResume = true;
                                            }

                                        }
                                    }
                            ){
                                @Override
                                public Map<String, String> getHeaders() throws AuthFailureError {
                                    HashMap<String, String> headers = new HashMap<>();
                                    headers.put("token", new String(Base64.decode(getToken(), Base64.DEFAULT)));
                                    return headers;
                                }
                            }
                            .setRetryPolicy(new DefaultRetryPolicy(
                                    0, 0,
                                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
                            ))
                    );
                }
                else {
                    Snackbar.make(view, getString(R.string.no_network_connection),
                            Snackbar.LENGTH_SHORT).setAnchorView(requireActivity()
                            .findViewById(R.id.bottomNavigationView)).show();
                }

            }
        });

        history.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getActivity(), ContainerActivity.class)
                        .putExtra(GENERAL_FRAGMENT_ID, DIAGNOSTICS_HISTORY_FRAGMENT));
            }
        });

    }

    @Override
    public void onStart() {
        speedTest = new SpeedTest(getActivity());
        super.onStart();
    }

    @Override
    public void onResume() {
        isTestPaused = false;
        if (shouldShowResultOnResume) {
            if (resultBundle != null && resultIntent != null) {
                shouldShowResultOnResume = false;
                startActivity(resultIntent);
            }
        }
        if (shouldShowErrorOnResume) {
            if (errorSnackbar != null) {
                shouldShowErrorOnResume = false;
                errorSnackbar.show();
            }
        }
        super.onResume();
    }

    @Override
    public void onPause() {
        isTestPaused = true;
        super.onPause();
    }

    @Override
    public void onDestroy() {
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
        super.onDestroy();
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
            if (!isCancelled()) {
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
                                            URL connectionUrl = new URL("https://1.1.1.1");
                                            for (int i = 0; i < 20; i++) {
                                                long beforeTime = SystemClock.elapsedRealtime();
                                                HttpsURLConnection connection = (HttpsURLConnection) connectionUrl.openConnection();
                                                connection.setRequestMethod("HEAD");
                                                connection.setConnectTimeout(10000);
                                                connection.connect();
                                                long afterTime = SystemClock.elapsedRealtime();
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
                                            onPostExecute("error");
                                        }

                                    }
                                }

                                @Override
                                public void onError(SpeedTestError speedTestError, String errorMessage) {
                                    // called when a download/upload error occurs
                                    Log.e(TAG, "onError: " + errorMessage);
                                    onPostExecute("error");
                                }

                                @Override
                                public void onProgress(float percent, SpeedTestReport report) {
                                    // Called to notify upload progress
                                    Log.v("speedtest", "[PROGRESS] progress : " + percent + "%");
                                    Log.v("speedtest", "[PROGRESS] rate in octet/s : " + report.getTransferRateOctet());
                                    sentAfter = TrafficStats.getTotalTxBytes();
                                    Long progressBytes = sentAfter - sentBefore;
                                    sentBefore = sentAfter;
                                    Float currentSpeed = (progressBytes.floatValue() / (1024 * 1024)) * 8;

                                    activity.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            onProgressUpdate("upload_speed",
                                                    activity.getString(R.string.network_speed_mbps,
                                                            String.format("%.2f", currentSpeed)),
                                                    currentSpeed.toString());
                                        }
                                    });
                                }
                            });
                        }

                        @Override
                        public void onError(SpeedTestError speedTestError, String errorMessage) {
                            // Called when a download error occurs
                            Log.e(TAG, "onError: " + errorMessage);
                            onPostExecute("error");
                        }

                        @Override
                        public void onProgress(float percent, SpeedTestReport report) {
                            // Called to notify download progress
                            Log.v("speedtest", "[PROGRESS] progress : " + percent + "%");
                            Log.v("speedtest", "[PROGRESS] rate in octet/s : " + report.getTransferRateOctet());
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

                                currentConnectionType.animate()
                                        .alpha(0f)
                                        .setListener(new AnimatorListenerAdapter() {
                                            @Override
                                            public void onAnimationEnd(Animator animation) {
                                                history.setVisibility(View.GONE);
                                                super.onAnimationEnd(animation);
                                                currentConnectionType.animate()
                                                        .alpha(1f)
                                                        .start();
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
                                            currentTestAnim.playAnimation();
                                            currentTestAnim.animate()
                                                    .scaleX(1)
                                                    .scaleY(1)
                                                    .alpha(1)
                                                    .setListener(new AnimatorListenerAdapter() {
                                                        @Override
                                                        public void onAnimationStart(Animator animation) {
                                                            super.onAnimationStart(animation);
                                                            currentTestAnim.setAlpha(0f);
                                                            currentTestAnim.setVisibility(View.VISIBLE);
                                                            currentTestAnim.setScaleX(0.8f);
                                                            currentTestAnim.setScaleY(0.8f);
                                                        }
                                                    })
                                                    .start();

                                            mMeterView.setVisibility(View.GONE);
                                            mMeterView.setAlpha(1);
                                            mMeterView.setScaleX(1);
                                            mMeterView.setScaleY(1);
                                        }

                                        @Override
                                        public void onAnimationStart(Animator animation) {
                                            super.onAnimationStart(animation);

                                        }
                                    })
                                    .start();
                            mCurrentSpeed.setText(activity.getString(R.string.network_speed_mbps, "0.00"));
                            currentTest.setText(activity.getString(R.string.testing_latency));

                            break;

                        case "connection_error":
                            Snackbar.make(requireView(),
                                    activity.getString(R.string.connection_error), Snackbar.LENGTH_SHORT)
                                    .setAnchorView(activity.findViewById(R.id.bottomNavigationView))
                                    .show();

                            runDiagnostics.setClickable(true);
                            runDiagnostics.setEnabled(true);
                            diagnosticsInfo.setVisibility(View.VISIBLE);
                            diagnosticsInfo.animate()
                                    .translationY(0)
                                    .start();

                            currentConnectionType.animate()
                                    .alpha(0f)
                                    .setListener(new AnimatorListenerAdapter() {
                                        @Override
                                        public void onAnimationEnd(Animator animation) {
                                            history.setVisibility(View.VISIBLE);
                                            super.onAnimationEnd(animation);
                                            currentConnectionType.animate()
                                                    .alpha(1f)
                                                    .start();
                                        }
                                    })
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
                                            Long minLatency = mLatencies.get(0);
                                            for (int k = 0; k < mLatencies.size(); k++) {
                                                if (mLatencies.get(k) < minLatency) {
                                                    minLatency = mLatencies.get(k);
                                                } else {
                                                    minLatency = minLatency;
                                                }
                                            }
                                            mMinLatency = minLatency;

                                            resultBundle = new Bundle();
                                            resultBundle.putString(AVG_DOWNLOAD_SPEED, String.format("%.2f", mDownloadSpeed));
                                            resultBundle.putString(AVG_UPLOAD_SPEED, String.format("%.2f", mUploadSpeed));
                                            resultBundle.putString(MIN_LATENCY, String.valueOf(mMinLatency));
                                            resultBundle.putString(AVG_LATENCY, String.valueOf(mLatency));
                                            resultBundle.putString(NETWORK_IP, mIpResponse.getIp());
                                            try {
                                                resultBundle.putString(ISP, mIpResponse.getOrg().
                                                        replace(mIpResponse.getOrg().split(" ")[0], ""));
                                            }
                                            catch (NullPointerException e) {
                                                e.printStackTrace();
                                            }
                                            resultBundle.putString(SERVER, mIpResponse.getCity());
                                            resultBundle.putString(REGION, mIpResponse.getRegion());

                                            resultIntent = new Intent(activity, ContainerActivity.class);
                                            resultIntent.putExtra(GENERAL_FRAGMENT_ID, NETWORK_STATS_FRAGMENT);
                                            resultIntent.putExtras(resultBundle);
                                            if (getContext() != null) {
                                                if (!isTestPaused) {
                                                    startActivity(resultIntent);
                                                }
                                                else {
                                                    shouldShowResultOnResume = true;
                                                }
                                            }

                                            boolean saveResults = PreferenceManager.getDefaultSharedPreferences(requireContext())
                                                    .getBoolean("save_results", true);
                                            if (saveResults) {
                                                Date date = new Date();
                                                String day = new SimpleDateFormat("EE", Locale.getDefault()).format(date.getTime());
                                                String dayOfMonth = new SimpleDateFormat("dd", Locale.getDefault()).format(date.getTime());
                                                String month = new SimpleDateFormat("MMM", Locale.getDefault()).format(date.getTime());
                                                String year = new SimpleDateFormat("yyyy", Locale.getDefault()).format(date.getTime());
                                                String time = DateFormat.getTimeInstance(DateFormat.SHORT).format(date.getTime());
                                                String isp = mIpResponse.getOrg().replace(mIpResponse.getOrg().split(" ")[0], "") == null ?
                                                        requireContext().getString(R.string.label_unknown) :
                                                        mIpResponse.getOrg().replace(mIpResponse.getOrg().split(" ")[0], "");
                                                String resultDate = day + " " + dayOfMonth + " " + month + " " + year + " " + time;
                                                DiagnosticsHistoryModel result = new DiagnosticsHistoryModel(
                                                        System.currentTimeMillis(),
                                                        resultDate,
                                                        requireContext().getString(R.string.network_speed_mbps, String.format("%.2f", mDownloadSpeed))
                                                                + "  |  " +
                                                                requireContext().getString(R.string.network_speed_mbps, String.format("%.2f", mUploadSpeed)),
                                                        String.format("%.2f", mDownloadSpeed),
                                                        String.format("%.2f", mUploadSpeed),
                                                        String.valueOf(mMinLatency),
                                                        String.valueOf(mLatency),
                                                        mIpResponse.getIp(),
                                                        isp,
                                                        mIpResponse.getCity(),
                                                        mIpResponse.getRegion()
                                                );

                                                Gson gson = new Gson();
                                                Type type = new TypeToken<List<DiagnosticsHistoryModel>>() {}.getType();
                                                List<DiagnosticsHistoryModel> list = new ArrayList<>();
                                                String jsonData = SharedPreferences.getDiagnosticsHistoryPrefs(requireContext())
                                                                .getString(DIAGNOSTICS_HISTORY_LIST, null);
                                                if (jsonData != null) {
                                                    list.addAll(gson.fromJson(jsonData, type));
                                                }
                                                list.add(result);

                                                jsonData = gson.toJson(list, type);
                                                SharedPreferences.getDiagnosticsHistoryPrefs(requireContext()).edit()
                                                        .putString(DIAGNOSTICS_HISTORY_LIST, jsonData)
                                                        .apply();
                                            }

                                        }
                                    }).start();

                                    runDiagnostics.setClickable(true);
                                    runDiagnostics.setEnabled(true);
                                    diagnosticsInfo.setVisibility(View.VISIBLE);
                                    diagnosticsInfo.animate()
                                            .translationY(0)
                                            .start();

                                    currentConnectionType.animate()
                                            .alpha(0f)
                                            .setListener(new AnimatorListenerAdapter() {
                                                @Override
                                                public void onAnimationEnd(Animator animation) {
                                                    history.setVisibility(View.VISIBLE);
                                                    super.onAnimationEnd(animation);
                                                    currentConnectionType.animate()
                                                            .alpha(1f)
                                                            .start();
                                                }
                                            })
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

                                    currentConnectionType.animate()
                                            .alpha(0f)
                                            .setListener(new AnimatorListenerAdapter() {
                                                @Override
                                                public void onAnimationEnd(Animator animation) {
                                                    history.setVisibility(View.VISIBLE);
                                                    super.onAnimationEnd(animation);
                                                    currentConnectionType.animate()
                                                            .alpha(1f)
                                                            .start();
                                                }
                                            })
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
                                                                @SuppressLint("UseCompatLoadingForDrawables")
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
                                                                    mMeterView.setVisibility(View.GONE);
                                                                }
                                                            })
                                                            .start();
                                                }
                                            })
                                            .start();
                                    Snackbar.make(requireView(),
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
            return (float) (rate * 5.6);
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
        switch (getConnectivityStatus(requireContext())) {
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
                    try {
                        setConnectionStatus();
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }
}
