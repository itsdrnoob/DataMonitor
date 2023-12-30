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

import static com.drnoob.datamonitor.core.Values.GENERAL_FRAGMENT_ID;
import static com.drnoob.datamonitor.core.Values.LICENSE_FRAGMENT;
import static com.drnoob.datamonitor.core.Values.MD5_F_DROID;
import static com.drnoob.datamonitor.core.Values.UPDATE_VERSION;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceFragmentCompat;

import com.drnoob.datamonitor.BuildConfig;
import com.drnoob.datamonitor.R;
import com.drnoob.datamonitor.core.base.Preference;
import com.drnoob.datamonitor.ui.activities.ContainerActivity;
import com.drnoob.datamonitor.utils.KeyUtils;
import com.drnoob.datamonitor.utils.SharedPreferences;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;

import java.io.IOException;
import java.net.URL;
import java.util.Scanner;

import javax.net.ssl.HttpsURLConnection;

public class AboutFragment extends Fragment {
    private static final String TAG = AboutFragment.class.getSimpleName();
    private TextView mAppVersion;
    private TextView mCheckForUpdate;
    private boolean isUpdateAvailable = false;
    private AlertDialog updateCheckDialog;

    public AboutFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_about, container, false);

        mAppVersion = view.findViewById(R.id.app_version);
        mCheckForUpdate = view.findViewById(R.id.check_for_update);

        mAppVersion.setText(BuildConfig.VERSION_NAME);
        String currentVersionName = BuildConfig.VERSION_NAME;

        if (currentVersionName.contains("-")) {
            currentVersionName = currentVersionName.split("-")[0];
        }

        int currentVersion = Integer.parseInt(currentVersionName.split("v")[1]
                .replace(".", ""));
        int newVersion = Integer.parseInt(SharedPreferences.getAppPrefs(getContext()).getString(UPDATE_VERSION,
                        currentVersionName)
                .split("v")[1].replace(".", ""));

        if (newVersion > currentVersion) {
            isUpdateAvailable = true;
            mCheckForUpdate.setText(R.string.label_update_available);
        }

        String md5 = KeyUtils.get(getContext(), "MD5");
        if (md5.equalsIgnoreCase(MD5_F_DROID)) {
            // F-Droid build
            mCheckForUpdate.setVisibility(View.GONE);
        }
        else {
            mCheckForUpdate.setVisibility(View.VISIBLE);
        }

        mCheckForUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isUpdateAvailable) {
                    downloadUpdate();
                }
                else {
                    View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.layout_update_check_dialog, null);
                    LinearLayout updateAvailableView = dialogView.findViewById(R.id.view_update_available);

                    updateAvailableView.setVisibility(View.GONE);

                    updateCheckDialog = new MaterialAlertDialogBuilder(getContext())
                            .setView(dialogView)
                            .create();

                    updateCheckDialog.show();

                    CheckForUpdate checkForUpdate = new CheckForUpdate();
                    checkForUpdate.execute();

                    updateCheckDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialogInterface) {
                            try {
                                checkForUpdate.cancel(true);
                            }
                            catch (Exception e) {
                                e.printStackTrace();
                            }

                            if (isUpdateAvailable) {
                                mCheckForUpdate.setText(R.string.label_update_available);
                            }
                        }
                    });
                }
            }
        });

        return view;
    }

    private void downloadUpdate() {
        String md5 = KeyUtils.get(getContext(), "MD5");
        Intent updateIntent = new Intent(Intent.ACTION_VIEW);
        updateIntent.setData(Uri.parse(getString(R.string.github_release_latest)));
        startActivity(updateIntent);
    }

    private void updateDialog(AlertDialog dialog, Context context) {
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        manager.getDefaultDisplay().getMetrics(metrics);

        lp.copyFrom(dialog.getWindow().getAttributes());
        lp.width = (metrics.widthPixels * 85 / 100);
        lp.y = 50;
        dialog.getWindow().setAttributes(lp);
    }

    private class CheckForUpdate extends AsyncTask<Void, String, String> {

        public CheckForUpdate() {

        }

        @Override
        protected String doInBackground(Void... voids) {
            String newVersion = null;
            try {
                URL url = new URL(getString(R.string.update_check));
                HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
                Scanner scanner = new Scanner(urlConnection.getInputStream());

                newVersion = scanner.next();
                return newVersion;
            }
            catch (IOException e) {
                e.printStackTrace();
                return null;
            }

        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            if (s != null) {
                String currentVersion = BuildConfig.VERSION_NAME;
                if (currentVersion.contains("-")) {
                    currentVersion = currentVersion.split("-")[0];
                }
                String currentVersionNumber = currentVersion.split("v")[1].replace(".", "");
                String newVersionNumber = s.split("v")[1].replace(".", "");
                if (!isCancelled()) {
                    if (Float.parseFloat(newVersionNumber) > Float.parseFloat(currentVersionNumber)) {
                        isUpdateAvailable = true;
                        updateCheckDialog.dismiss();
                        updateAvailable(BuildConfig.VERSION_NAME, s);
                    }
                    else {
                        updateCheckDialog.dismiss();
                        Snackbar.make(requireView(), getString(R.string.no_update_available), Snackbar.LENGTH_SHORT)
                                .show();
                    }
                }
            }
            else {
                updateCheckDialog.dismiss();
                Snackbar.make(requireView(), getString(R.string.update_fetch_error), Snackbar.LENGTH_SHORT).show();
            }
        }
    }

    private void updateAvailable(String current, String update) {
        SharedPreferences.getAppPrefs(requireContext()).edit()
                .putString(UPDATE_VERSION, update)
                .apply();
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.layout_update_check_dialog, null);

        TextView title = dialogView.findViewById(R.id.alert_dialog_title);
        LinearProgressIndicator progressIndicator = dialogView.findViewById(R.id.checking_for_update_progress);

        TextView currentVersion = dialogView.findViewById(R.id.current_version);
        TextView newVersion = dialogView.findViewById(R.id.new_version);
        TextView changelogs = dialogView.findViewById(R.id.changelogs);

        title.setText(requireContext().getString(R.string.label_update_available));
        currentVersion.setText(getString(R.string.current_version, current));
        newVersion.setText(getString(R.string.new_version, update));

        progressIndicator.setVisibility(View.GONE);

        updateCheckDialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(dialogView)
                .setPositiveButton(requireContext().getString(R.string.label_download_update),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                downloadUpdate();
                                updateCheckDialog.dismiss();
                            }
                        })
                .setNegativeButton(requireContext().getString(R.string.action_cancel), null)
                .create();

        changelogs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String changelogURL = getString(R.string.changelog) + "#" + update.replace(".", "");
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(changelogURL));
                startActivity(intent);
                updateCheckDialog.dismiss();
            }
        });

        updateCheckDialog.show();
    }

    public static class SupportAndDevelopment extends PreferenceFragmentCompat {
        Preference mLicense;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.about_preference, rootKey);

            mLicense = (Preference) findPreference("license");

            mLicense.setOnPreferenceClickListener(new androidx.preference.Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(androidx.preference.Preference preference) {
                    startActivity(new Intent(getContext(), ContainerActivity.class)
                            .putExtra(GENERAL_FRAGMENT_ID, LICENSE_FRAGMENT));
                    return false;
                }
            });
        }
    }
}