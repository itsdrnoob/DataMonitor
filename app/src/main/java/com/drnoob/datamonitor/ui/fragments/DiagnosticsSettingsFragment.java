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

import static com.drnoob.datamonitor.Common.dismissOnClick;
import static com.drnoob.datamonitor.core.Values.DIAGNOSTICS_DOWNLOAD_URL_INDEX;
import static com.drnoob.datamonitor.core.Values.DIAGNOSTICS_DOWNLOAD_URL_SUMMARY;
import static com.drnoob.datamonitor.core.Values.DIAGNOSTICS_UPLOAD_URL_INDEX;
import static com.drnoob.datamonitor.core.Values.DIAGNOSTICS_UPLOAD_URL_SUMMARY;

import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.URLUtil;
import android.widget.FrameLayout;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.drnoob.datamonitor.R;
import com.drnoob.datamonitor.core.Values;
import com.drnoob.datamonitor.core.base.Preference;
import com.drnoob.datamonitor.core.base.ProcessDialog;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import fr.bmartel.speedtest.SpeedTestReport;
import fr.bmartel.speedtest.SpeedTestSocket;
import fr.bmartel.speedtest.inter.ISpeedTestListener;
import fr.bmartel.speedtest.model.SpeedTestError;
import fr.bmartel.speedtest.utils.SpeedTestUtils;

public class DiagnosticsSettingsFragment extends PreferenceFragmentCompat {
    public static final int TYPE_DOWNLOAD_URL = 1;
    public static final int TYPE_UPLOAD_URL = 2;

    private static final String TAG = DiagnosticsSettingsFragment.class.getSimpleName();

    Preference downloadServer, uploadServer;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.diagnostics_settings, rootKey);

        downloadServer = (Preference) findPreference("download_server");
        uploadServer = (Preference) findPreference("upload_server");

        refreshSummary();

        downloadServer.setOnPreferenceClickListener(new androidx.preference.Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(androidx.preference.Preference preference) {
                BottomSheetDialog dialog = new BottomSheetDialog(getContext(), R.style.BottomSheet);
                View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.layout_download_server, null);
                RadioGroup downloadServerGroup = dialogView.findViewById(R.id.download_server_group);
                TextInputEditText customServer = dialogView.findViewById(R.id.custom_server);
                ConstraintLayout footer = dialogView.findViewById(R.id.footer);
                TextView cancel = footer.findViewById(R.id.cancel);
                TextView ok = footer.findViewById(R.id.ok);

                int index = PreferenceManager
                        .getDefaultSharedPreferences(getContext())
                        .getInt(DIAGNOSTICS_DOWNLOAD_URL_INDEX, 0);
                if (index < 0) {
                    downloadServerGroup.clearCheck();
                    customServer.setText(PreferenceManager.getDefaultSharedPreferences(getContext())
                            .getString(DIAGNOSTICS_DOWNLOAD_URL_SUMMARY, getString(R.string.download_url_1_summary)));
                }
                else {
                    downloadServerGroup.check(downloadServerGroup.getChildAt(index).getId());
                }

                cancel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();
                    }
                });

                ok.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        String url = "";
                        String summary = "";
                        int index = 0;
                        if (customServer.getText().toString() != null && customServer.getText().toString().length() > 0) {
                            url = customServer.getText().toString();
                            ValidateURL validateURL = new ValidateURL(getContext(),
                                    TYPE_DOWNLOAD_URL,
                                    url,
                                    dialogView,
                                    dialog);

                            validateURL.execute();
                        }
                        else {
                            if (downloadServerGroup.getCheckedRadioButtonId() == R.id.server_1) {
                                url = getString(R.string.download_server_1_url);
                                summary = getString(R.string.download_url_1_summary);
                                index = 0;
                            }
                            else if (downloadServerGroup.getCheckedRadioButtonId() == R.id.server_2) {
                                url = getString(R.string.download_server_2_url);
                                summary = getString(R.string.download_url_2_summary);
                                index = 1;
                            }
                            else if (downloadServerGroup.getCheckedRadioButtonId() == R.id.server_3) {
                                url = getString(R.string.download_server_3_url);
                                summary = getString(R.string.download_url_3_summary);
                                index = 2;
                            }
                            else {
                                url = getString(R.string.download_server_1_url);
                                summary = getString(R.string.download_url_1_summary);
                                index = 0;
                            }
                            PreferenceManager.getDefaultSharedPreferences(getContext()).edit()
                                    .putString(Values.DIAGNOSTICS_DOWNLOAD_URL, url)
                                    .putString(DIAGNOSTICS_DOWNLOAD_URL_SUMMARY, summary)
                                    .putInt(DIAGNOSTICS_DOWNLOAD_URL_INDEX, index)
                                    .apply();

                            refreshSummary();
                            dialog.dismiss();
                        }
                    }
                });

                downloadServerGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(RadioGroup radioGroup, int i) {
                        if (radioGroup.getCheckedRadioButtonId() != -1) {
                            customServer.setText("");
                            customServer.clearFocus();
                        }
                    }
                });

                customServer.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                    @Override
                    public void onFocusChange(View view, boolean b) {
                        if (b) {
                            downloadServerGroup.clearCheck();
                            view.requestFocus();
                        }
                    }
                });

                dialog.setOnShowListener(new DialogInterface.OnShowListener() {
                    @Override
                    public void onShow(DialogInterface dialog) {
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                BottomSheetDialog d = (BottomSheetDialog) dialog;
                                FrameLayout bottomSheet = d.findViewById(R.id.design_bottom_sheet);
                                BottomSheetBehavior bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
                                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                            }
                        },0);
                    }
                });
                dialog.setContentView(dialogView);
                dialog.show();
                return false;
            }
        });

        uploadServer.setOnPreferenceClickListener(new androidx.preference.Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(androidx.preference.Preference preference) {
                BottomSheetDialog dialog = new BottomSheetDialog(getContext(), R.style.BottomSheet);
                View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.layout_upload_server, null);
                RadioGroup uploadServerGroup = dialogView.findViewById(R.id.upload_server_group);
                TextInputEditText customServer = dialogView.findViewById(R.id.custom_server);
                ConstraintLayout footer = dialogView.findViewById(R.id.footer);
                TextView cancel = footer.findViewById(R.id.cancel);
                TextView ok = footer.findViewById(R.id.ok);

                int index = PreferenceManager
                        .getDefaultSharedPreferences(getContext())
                        .getInt(DIAGNOSTICS_UPLOAD_URL_INDEX, 0);
                if (index < 0) {
                    uploadServerGroup.clearCheck();
                    customServer.setText(PreferenceManager.getDefaultSharedPreferences(getContext())
                            .getString(DIAGNOSTICS_UPLOAD_URL_SUMMARY, getString(R.string.upload_url_1_summary)));
                }
                else {
                    uploadServerGroup.check(uploadServerGroup.getChildAt(index).getId());
                }

                cancel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();
                    }
                });

                ok.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        String url = "";
                        String summary = "";
                        int index = 0;
                        if (customServer.getText().toString() != null && customServer.getText().toString().length() > 10) {
                            url = customServer.getText().toString();
                            ValidateURL validateURL = new ValidateURL(getContext(),
                                    TYPE_UPLOAD_URL,
                                    url,
                                    dialogView,
                                    dialog);

                            validateURL.execute();
                        }
                        else {
                            if (uploadServerGroup.getCheckedRadioButtonId() == R.id.server_1) {
                                url = getString(R.string.upload_server_1_url);
                                summary = getString(R.string.upload_url_1_summary);
                                index = 0;
                            }
                            else if (uploadServerGroup.getCheckedRadioButtonId() == R.id.server_2) {
                                url = getString(R.string.upload_server_2_url);
                                summary = getString(R.string.upload_url_2_summary);
                                index = 1;
                            }
                            else if (uploadServerGroup.getCheckedRadioButtonId() == R.id.server_3) {
                                url = getString(R.string.upload_server_3_url);
                                summary = getString(R.string.upload_url_3_summary);
                                index = 2;
                            }
                            else {
                                url = getString(R.string.upload_server_1_url);
                                summary = getString(R.string.upload_url_1_summary);
                                index = 0;
                            }
                            PreferenceManager.getDefaultSharedPreferences(getContext()).edit()
                                    .putString(Values.DIAGNOSTICS_UPLOAD_URL, url)
                                    .putString(DIAGNOSTICS_UPLOAD_URL_SUMMARY, summary)
                                    .putInt(DIAGNOSTICS_UPLOAD_URL_INDEX, index)
                                    .apply();

                            refreshSummary();
                            dialog.dismiss();
                        }
                    }
                });

                uploadServerGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(RadioGroup radioGroup, int i) {
                        if (radioGroup.getCheckedRadioButtonId() != -1) {
                            customServer.setText("");
                            customServer.clearFocus();
                        }
                    }
                });

                customServer.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                    @Override
                    public void onFocusChange(View view, boolean b) {
                        if (b) {
                            uploadServerGroup.clearCheck();
                            view.requestFocus();
                        }
                    }
                });

                dialog.setOnShowListener(new DialogInterface.OnShowListener() {
                    @Override
                    public void onShow(DialogInterface dialog) {
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                BottomSheetDialog d = (BottomSheetDialog) dialog;
                                FrameLayout bottomSheet = d.findViewById(R.id.design_bottom_sheet);
                                BottomSheetBehavior bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
                                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                            }
                        },0);
                    }
                });
                dialog.setContentView(dialogView);
                dialog.show();
                return false;
            }
        });
    }

    private void refreshSummary() {
        String downloadSummary = PreferenceManager.getDefaultSharedPreferences(getContext())
                .getString(DIAGNOSTICS_DOWNLOAD_URL_SUMMARY, getString(R.string.download_url_1_summary));
        String uploadSummary = PreferenceManager.getDefaultSharedPreferences(getContext())
                .getString(DIAGNOSTICS_UPLOAD_URL_SUMMARY, getString(R.string.upload_url_1_summary));

        downloadServer.setSummary(downloadSummary);
        uploadServer.setSummary(uploadSummary);
    }

    private class ValidateURL extends AsyncTask<Object, String, String> {
        private Context context;
        private int type;
        private String url;
        private View dialogView;
        private ProcessDialog processDialog;
        private BottomSheetDialog dialog;
        private TextInputLayout customURLInput;
        private long startTime, endTime;

        public View getDialogView() {
            return dialogView;
        }

        public ValidateURL(Context context, int type, String url, View dialogView, BottomSheetDialog dialog) {
            this.context = context;
            this.type = type;
            this.url = url;
            this.dialogView = dialogView;
            this.dialog = dialog;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if (processDialog == null) {
                processDialog = new ProcessDialog(context, true, new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialogInterface) {
                        ValidateURL.this.cancel(true);
                    }
                });
            }
            processDialog.show();
            startTime = System.currentTimeMillis();
        }

        @Override
        protected String doInBackground(Object[] objects) {
            SpeedTestSocket speedTestSocket = new SpeedTestSocket();
            if (URLUtil.isValidUrl(url)) {
                if (type == TYPE_DOWNLOAD_URL) {
                    speedTestSocket.startFixedDownload(url, 1500);
                }
                else if (type == TYPE_UPLOAD_URL) {
                    String fileName = SpeedTestUtils.generateFileName() + ".txt";
                    speedTestSocket.startFixedUpload(url + fileName, 10000000, 1500, 200);
                }
                speedTestSocket.addSpeedTestListener(new ISpeedTestListener() {
                    @Override
                    public void onCompletion(SpeedTestReport report) {
                        onPostExecute("valid");
                        return;
                    }

                    @Override
                    public void onProgress(float percent, SpeedTestReport report) {

                    }

                    @Override
                    public void onError(SpeedTestError speedTestError, String errorMessage) {
                        onPostExecute("invalid");
                        return;
                    }
                });
            }
            else {
                return "invalid";
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            if (s != null) {
                Snackbar snackbar;
                if (s.equalsIgnoreCase("valid")) {
                    if (type == TYPE_DOWNLOAD_URL) {
                        PreferenceManager.getDefaultSharedPreferences(getContext()).edit()
                                .putString(Values.DIAGNOSTICS_DOWNLOAD_URL, url)
                                .putString(DIAGNOSTICS_DOWNLOAD_URL_SUMMARY, url)
                                .putInt(DIAGNOSTICS_DOWNLOAD_URL_INDEX, -1)
                                .apply();
                    }
                    else if (type == TYPE_UPLOAD_URL) {
                        PreferenceManager.getDefaultSharedPreferences(getContext()).edit()
                                .putString(Values.DIAGNOSTICS_UPLOAD_URL, url)
                                .putString(DIAGNOSTICS_UPLOAD_URL_SUMMARY, url)
                                .putInt(DIAGNOSTICS_UPLOAD_URL_INDEX, -1)
                                .apply();
                    }
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            refreshSummary();
                        }
                    });
                    dialog.dismiss();
                    snackbar = Snackbar.make(getView(),
                            context.getString(R.string.label_custom_server_save_success),
                            Snackbar.LENGTH_SHORT);
                }
                else if (s.equalsIgnoreCase("invalid")) {
                    snackbar = Snackbar.make(getDialogView().getRootView(),
                            context.getString(R.string.error_invaliid_url),
                            Snackbar.LENGTH_SHORT);
                }
                else {
                    snackbar = Snackbar.make(getDialogView().getRootView(),
                            context.getString(R.string.error_url_verification_failed),
                            Snackbar.LENGTH_SHORT);
                }
                processDialog.dismiss();
                processDialog.cancel();
                dismissOnClick(snackbar);
                snackbar.show();
            }
            else {

            }
        }
    }
}
