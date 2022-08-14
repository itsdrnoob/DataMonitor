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

package com.drnoob.datamonitor;

import android.Manifest;
import android.app.Activity;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.Log;
import android.view.View;

import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import com.airbnb.lottie.L;
import com.drnoob.datamonitor.utils.LiveNetworkMonitor;
import com.drnoob.datamonitor.utils.NotificationService;
import com.google.android.material.snackbar.Snackbar;

import java.util.Locale;

import static android.content.Context.APP_OPS_SERVICE;

public class Common {
    public static void dismissOnClick(Snackbar snackbar) {
        snackbar.getView().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                snackbar.dismiss();
            }
        });
    }

    public static Boolean isUsageAccessGranted(Context context) throws PackageManager.NameNotFoundException {
        ApplicationInfo applicationInfo = context.getPackageManager().getApplicationInfo(context.getPackageName(), 0);
        AppOpsManager appOpsManager = (AppOpsManager) context.getSystemService(APP_OPS_SERVICE);
        int mode = appOpsManager.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, applicationInfo.uid, applicationInfo.packageName);
        return (mode == AppOpsManager.MODE_ALLOWED);
    }

    public static Boolean isReadPhoneStateGranted(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE)
                == PackageManager.PERMISSION_GRANTED;
    }

    public static Boolean isAppInstalled(Context context, String packageName) {
        PackageManager pm = context.getPackageManager();
        try {
            pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void setLanguage(Activity activity, String languageCode, String countryCode) {
        Resources res = activity.getResources();
        Configuration conf = res.getConfiguration();
        Locale locale;
        if (countryCode.equals("rTW")) {
            locale = Locale.TAIWAN;
        }
        else if (countryCode.equals("rCN")) {
            locale = Locale.CHINESE;
        }
        else {
            locale = new Locale(languageCode, countryCode);
        }
        conf.locale = locale;
        conf.setLayoutDirection(locale);
        res.updateConfiguration(conf, res.getDisplayMetrics());

    }

    public static void refreshService(Context context) {
        if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean("network_signal_notification", false)) {
            context.startService(new Intent(context, LiveNetworkMonitor.class));
        }
        if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean("setup_notification", false)) {
            context.startService(new Intent(context, NotificationService.class));
        }
    }
}
