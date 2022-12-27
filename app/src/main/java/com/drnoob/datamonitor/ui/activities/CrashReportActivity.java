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

package com.drnoob.datamonitor.ui.activities;

import static com.drnoob.datamonitor.Common.dismissOnClick;
import static com.drnoob.datamonitor.Common.setLanguage;
import static com.drnoob.datamonitor.core.Values.APP_COUNTRY_CODE;
import static com.drnoob.datamonitor.core.Values.APP_LANGUAGE_CODE;
import static com.drnoob.datamonitor.core.Values.CRASH_REPORT_KEY;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.preference.PreferenceFragmentCompat;

import com.drnoob.datamonitor.R;
import com.drnoob.datamonitor.core.base.Preference;
import com.drnoob.datamonitor.databinding.ActivityCrashReportBinding;
import com.drnoob.datamonitor.utils.SharedPreferences;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.elevation.SurfaceColors;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class CrashReportActivity extends AppCompatActivity {
    private static final String TAG = CrashReportActivity.class.getSimpleName();

    private static String mErrorLogs;
    private static boolean includeDeviceInfo;

    ActivityCrashReportBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        MainActivity.setTheme(CrashReportActivity.this);
        String languageCode = SharedPreferences.getUserPrefs(this).getString(APP_LANGUAGE_CODE, "null");
        String countryCode = SharedPreferences.getUserPrefs(this).getString(APP_COUNTRY_CODE, "");
        if (languageCode.equals("null")) {
            setLanguage(this, "en", countryCode);
        } else {
            setLanguage(this, languageCode, countryCode);
        }
        super.onCreate(savedInstanceState);
        binding = ActivityCrashReportBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        binding.toolbar.setBackgroundColor(SurfaceColors.SURFACE_2.getColor(this));
        getWindow().setStatusBarColor(SurfaceColors.SURFACE_2.getColor(this));
        getSupportActionBar().setTitle(R.string.crash_report_title);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
//        getSupportActionBar().setHomeAsUpIndicator(getDrawable(R.drawable.ic_arrow));

        /*
        In versions lower than O_MR1, windowLightNavigationBar cannot be applied, which results in the
        navigation bar icons being a light color (white). This limits visibility in light theme.
         */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
//            getWindow().setNavigationBarColor(SurfaceColors.SURFACE_2.getColor(this));
            getWindow().setNavigationBarColor(getResources().getColor(R.color.background, null));
        }

        mErrorLogs = getIntent().getStringExtra(CRASH_REPORT_KEY);
        includeDeviceInfo = binding.deviceInfoLogs.isChecked();

        binding.deviceInfoLogsContent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new MaterialAlertDialogBuilder(CrashReportActivity.this)
                        .setTitle(R.string.title_device_info_contents)
                        .setMessage(R.string.device_info_body)
                        .setPositiveButton(R.string.action_ok, null)
                        .show();
            }
        });

        binding.deviceInfoLogs.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                includeDeviceInfo = b;
            }
        });

        binding.copyLogs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar snackbar;
                if (mErrorLogs == null) {
                    snackbar = Snackbar.make(binding.getRoot(), getString(R.string.error_no_crash_logs),
                            Snackbar.LENGTH_LONG);
                } else {
                    if (mErrorLogs != null) {
                        StringBuilder stringBuilder = new StringBuilder(mErrorLogs);
                        if (includeDeviceInfo) {
                            stringBuilder.append("\n")
                                    .append("\n")
                                    .append("----------Device info----------" + "\n")
                                    .append("Device Manufacturer: " + Build.MANUFACTURER + "\n")
                                    .append("Device Brand: " + Build.BRAND + "\n")
                                    .append("Device Model: " + Build.MODEL + "\n")
                                    .append("Device Codename: " + Build.PRODUCT + "\n")
                                    .append("Android version: " + Build.VERSION.RELEASE + ", " + Build.VERSION.SDK_INT + "\n");
                        }
                        ClipboardManager clipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                        ClipData clipData = ClipData.newPlainText("datamonitor-crash-logs", stringBuilder.toString());
                        clipboardManager.setPrimaryClip(clipData);
                        snackbar = Snackbar.make(binding.getRoot(), getString(R.string.label_crash_logs_copied),
                                Snackbar.LENGTH_LONG);
                    } else {
                        snackbar = Snackbar.make(binding.getRoot(), getString(R.string.error_no_crash_logs),
                                Snackbar.LENGTH_LONG);
                    }
                }
                dismissOnClick(snackbar);
                snackbar.show();
            }
        });

    }

    public static class SendReportFragment extends PreferenceFragmentCompat {
        Preference github, telegram, mail;
        List<String> telegramClients = new ArrayList<>();

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.crash_report_action, rootKey);

            telegram = (Preference) findPreference("report_telegram");
            github = (Preference) findPreference("report_github");
            mail = (Preference) findPreference("report_mail");

            telegramClients.add("org.telegram.messenger");
            telegramClients.add("org.telegram.plus");
            telegramClients.add("tw.nekomimi.nekogram");
            telegramClients.add("org.thunderdog.challegram");
            telegramClients.add("org.telegram.mdgram");
            telegramClients.add("org.telegram.mdgramyou");
            telegramClients.add("one.gram.onegram");
            telegramClients.add("nekox.messenger");
            telegramClients.add("ir.ilmili.telegraph");
            telegramClients.add("com.xplus.messenger");
            telegramClients.add("org.telegram.BifToGram");
            telegramClients.add("org.vidogram.messenger");

            telegram.setOnPreferenceClickListener(new androidx.preference.Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(androidx.preference.Preference preference) {
                    String logFile = "";
                    try {
                        logFile = extractLogsToFile(mErrorLogs, includeDeviceInfo, getContext());
                        if (logFile == null) {
                            Snackbar snackbar = Snackbar.make(getView(), getString(R.string.error_no_crash_logs),
                                    Snackbar.LENGTH_LONG);
                            dismissOnClick(snackbar);
                            snackbar.show();
                            return false;
                        }
                        try {
                            List<Intent> telegramIntents = new ArrayList<Intent>();
                            Intent telegramIntent = new Intent(Intent.ACTION_SEND);
                            telegramIntent.setType("application/pdf");
                            List<ResolveInfo> resInfo = getContext().getApplicationContext()
                                    .getPackageManager().queryIntentActivities(telegramIntent, 0);
                            if (!resInfo.isEmpty()) {
                                for (ResolveInfo resolveInfo : resInfo) {
                                    String packageName = resolveInfo.activityInfo.packageName;
                                    Intent targetedIntent = new Intent(Intent.ACTION_SEND);
                                    targetedIntent.setType("application/pdf");
                                    targetedIntent.setPackage(packageName);
                                    targetedIntent.putExtra(Intent.EXTRA_STREAM,
                                            FileProvider.getUriForFile(getContext(),
                                                    getContext().getApplicationContext().getPackageName() + ".provider",
                                                    new File(logFile)));
                                    targetedIntent.putExtra(Intent.EXTRA_TEXT, getContext().getString(R.string.crash_logs_extra_text));
                                    if (telegramClients.contains(packageName)) {
                                        telegramIntents.add(targetedIntent);
                                    }
                                }
                                Intent chooserIntent = Intent.createChooser(telegramIntents.remove(0),
                                        getString(R.string.label_select_app));
                                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS,
                                        telegramIntents.toArray(new Parcelable[telegramIntents.size()]));
                                startActivity(chooserIntent);
                                Toast.makeText(getContext(), getString(R.string.label_crash_logs_share_chat),
                                        Toast.LENGTH_LONG).show();
                            }


                        }
                        catch (Exception e) {
                            Snackbar snackbar = Snackbar.make(getView(), getString(R.string.error_unknown_telegram_client),
                                    Snackbar.LENGTH_LONG);
                            dismissOnClick(snackbar);
                            snackbar.show();
                            e.printStackTrace();
                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return false;
                }
            });

            github.setOnPreferenceClickListener(new androidx.preference.Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(androidx.preference.Preference preference) {
                    if (mErrorLogs == null) {
                        Snackbar snackbar = Snackbar.make(getView(), getString(R.string.error_no_crash_logs),
                                Snackbar.LENGTH_LONG);
                        dismissOnClick(snackbar);
                        snackbar.show();
                        return false;
                    }
                    Intent githubIntent = new Intent(Intent.ACTION_VIEW);
                    githubIntent.setData(Uri.parse(getString(R.string.github_new_issue)));
                    startActivity(githubIntent);
                    copyLogs(mErrorLogs, includeDeviceInfo, getContext());
                    return false;
                }
            });

            mail.setOnPreferenceClickListener(new androidx.preference.Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(androidx.preference.Preference preference) {
                    if (mErrorLogs == null) {
                        Snackbar snackbar = Snackbar.make(getView(), getString(R.string.error_no_crash_logs),
                                Snackbar.LENGTH_LONG);
                        dismissOnClick(snackbar);
                        snackbar.show();
                        return false;
                    }
                    String logs = getString(R.string.crash_logs_warning) + "\n\n" +
                            getLogs(mErrorLogs, includeDeviceInfo, getContext());
                    Intent mailIntent = new Intent(Intent.ACTION_SENDTO);
                    String mailData = getString(R.string.mail_uri,
                            getString(R.string.email),
                            getString(R.string.crash_logs_extra_text), Uri.encode(logs));
                    mailIntent.setData(Uri.parse(mailData));
                    startActivity(Intent.createChooser(mailIntent, getString(R.string.label_select_app)));
                    return false;
                }
            });
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }

    private static String extractLogsToFile(String logs, boolean includeDeviceInfo, Context context)
            throws IOException {
        if (logs == null) {
            return null;
        }
        String fileName;
        String filePath = context.getExternalFilesDir(null) + File.separator + "logs";
        File dir = new File(filePath);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmm");
        fileName = dateFormat.format(new Date()) + ".log";
        File logFile = new File(filePath + File.separator + fileName);

        StringBuilder stringBuilder = new StringBuilder(logs);
        if (includeDeviceInfo) {
            stringBuilder.append("\n")
                    .append("\n")
                    .append("----------Device info----------" + "\n")
                    .append("Device Manufacturer: " + Build.MANUFACTURER + "\n")
                    .append("Device Brand: " + Build.BRAND + "\n")
                    .append("Device Model: " + Build.MODEL + "\n")
                    .append("Device Codename: " + Build.PRODUCT + "\n")
                    .append("Android version: " + Build.VERSION.RELEASE + ", " + Build.VERSION.SDK_INT + "\n");
        }
        OutputStream os = new FileOutputStream(logFile);
        os.write(stringBuilder.toString().getBytes());
        os.close();
        return logFile.getAbsolutePath();
    }

    private static void copyLogs(String logs, boolean includeDeviceInfo, Context context) {
        StringBuilder stringBuilder = new StringBuilder(logs);
        if (includeDeviceInfo) {
            stringBuilder.append("\n")
                    .append("\n")
                    .append("----------Device info----------" + "\n")
                    .append("Device Manufacturer: " + Build.MANUFACTURER + "\n")
                    .append("Device Brand: " + Build.BRAND + "\n")
                    .append("Device Model: " + Build.MODEL + "\n")
                    .append("Device Codename: " + Build.PRODUCT + "\n")
                    .append("Android version: " + Build.VERSION.RELEASE + ", " + Build.VERSION.SDK_INT + "\n");
        }
        ClipboardManager clipboardManager = (ClipboardManager) context.getSystemService(CLIPBOARD_SERVICE);
        ClipData clipData = ClipData.newPlainText("datamonitor-crash-logs", stringBuilder.toString());
        clipboardManager.setPrimaryClip(clipData);
        Toast.makeText(context, context.getString(R.string.label_crash_logs_copied), Toast.LENGTH_LONG).show();
    }

    private static String getLogs(String logs, boolean includeDeviceInfo, Context context) {
        if (logs == null) {
            return null;
        }
        StringBuilder stringBuilder = new StringBuilder(logs);
        if (includeDeviceInfo) {
            stringBuilder.append("\n")
                    .append("\n")
                    .append("----------Device info----------" + "\n")
                    .append("Device Manufacturer: " + Build.MANUFACTURER + "\n")
                    .append("Device Brand: " + Build.BRAND + "\n")
                    .append("Device Model: " + Build.MODEL + "\n")
                    .append("Device Codename: " + Build.PRODUCT + "\n")
                    .append("Android version: " + Build.VERSION.RELEASE + ", " + Build.VERSION.SDK_INT + "\n");
        }
        return stringBuilder.toString();
    }
}