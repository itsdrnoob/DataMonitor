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

import static android.content.Context.APP_OPS_SERVICE;
import static com.drnoob.datamonitor.core.Values.ACTION_SHOW_DATA_PLAN_NOTIFICATION;
import static com.drnoob.datamonitor.core.Values.ALARM_PERMISSION_DENIED;
import static com.drnoob.datamonitor.core.Values.DATA_RESET_CUSTOM_DATE_END;
import static com.drnoob.datamonitor.core.Values.DATA_RESET_DATE;
import static com.drnoob.datamonitor.core.Values.INTENT_ACTION;
import static com.drnoob.datamonitor.core.Values.LANGUAGE_SYSTEM_DEFAULT;
import static com.drnoob.datamonitor.core.Values.OTHER_NOTIFICATION_CHANNEL_ID;
import static com.drnoob.datamonitor.core.Values.OTHER_NOTIFICATION_ID;
import static com.drnoob.datamonitor.core.Values.SESSION_CUSTOM;
import static com.drnoob.datamonitor.core.Values.SESSION_MONTHLY;
import static com.drnoob.datamonitor.utils.NetworkStatsHelper.getTimePeriod;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.AppOpsManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.icu.text.MessageFormat;
import android.os.Build;
import android.provider.Settings;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.core.os.ConfigurationCompat;
import androidx.preference.PreferenceManager;

import com.drnoob.datamonitor.adapters.data.LanguageModel;
import com.drnoob.datamonitor.utils.CompoundNotification;
import com.drnoob.datamonitor.utils.DataPlanRefreshReceiver;
import com.drnoob.datamonitor.utils.DataUsageMonitor;
import com.drnoob.datamonitor.utils.LiveNetworkMonitor;
import com.drnoob.datamonitor.utils.NotificationService;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class Common {
    private static final String TAG = Common.class.getSimpleName();

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
        List<LanguageModel> availableLanguages = refreshAvailableLanguages();

        if (languageCode.equalsIgnoreCase(LANGUAGE_SYSTEM_DEFAULT)) {
            // setting default system language if available
            String systemLanguageCode = ConfigurationCompat.getLocales(Resources.getSystem()
                    .getConfiguration()).get(0).getLanguage();
            String systemCountryCode = "r" + ConfigurationCompat.getLocales(Resources.getSystem()
                    .getConfiguration()).get(0).getCountry();

            List<LanguageModel> availableSystemLanguages = new ArrayList<>();
            for (LanguageModel languageModel : availableLanguages) {
                if (languageModel.getLanguageCode().equalsIgnoreCase(systemLanguageCode)) {
                    // Matching language code
                    languageCode = systemLanguageCode;
                    availableSystemLanguages.add(languageModel);
                }
            }
            for (LanguageModel languageModel : availableSystemLanguages) {
                if (languageModel.getCountryCode().equalsIgnoreCase(systemCountryCode)) {
                    // System country code available
                    countryCode = systemCountryCode;
                    break;
                } else {
                    // System country code not available
                    countryCode = "";
                }
            }
        }
        if (languageCode.equalsIgnoreCase(LANGUAGE_SYSTEM_DEFAULT)) {
            languageCode = "en";
            countryCode = "";
        }
        Resources res = activity.getResources();
        Configuration conf = res.getConfiguration();
        Locale locale;
        if (countryCode.equals("rTW")) {
            locale = Locale.TAIWAN;
        } else if (countryCode.equals("rCN")) {
            locale = Locale.CHINESE;
        } else {
            locale = new Locale(languageCode, countryCode);
        }
        conf.locale = locale;
        conf.setLayoutDirection(locale);
        res.updateConfiguration(conf, res.getDisplayMetrics());

    }

    public static List<LanguageModel> refreshAvailableLanguages() {
        List<LanguageModel> list = new ArrayList<>();
        list.add(new LanguageModel("Romanian", "ro", ""));
        list.add(new LanguageModel("English", "en", ""));
        list.add(new LanguageModel("Simplified Chinese", "zh", "rCN"));
        list.add(new LanguageModel("Traditional Chinese", "zh", "rTW"));
        list.add(new LanguageModel("French", "fr", ""));
        list.add(new LanguageModel("Arabic", "ar", ""));
        list.add(new LanguageModel("Malayalam", "ml", ""));
        list.add(new LanguageModel("Italian", "it", ""));
        list.add(new LanguageModel("Russian", "ru", ""));
        list.add(new LanguageModel("Turkish", "tr", ""));
        list.add(new LanguageModel("German", "de", ""));
        list.add(new LanguageModel("Norwegian Bokmål", "nb", "rNO"));
        list.add(new LanguageModel("Portuguese", "pt", "rBR"));
        list.add(new LanguageModel("Spanish", "es", ""));
        list.add(new LanguageModel("Ukrainian", "uk", ""));
        list.add(new LanguageModel("Hindi", "hi", ""));
        list.add(new LanguageModel("Indonesian", "in", ""));
        list.add(new LanguageModel("Korean", "ko", ""));
        list.add(new LanguageModel("Uzbek", "uz", ""));
        list.add(new LanguageModel("Marathi", "mr", ""));
        list.add(new LanguageModel("Dutch", "nl", ""));
        list.add(new LanguageModel("Polish", "pl", ""));
        list.add(new LanguageModel("Czech", "cs", ""));
        list.add(new LanguageModel("Vietnamese", "vi", ""));
        list.add(new LanguageModel("Japanese", "ja", ""));
        list.add(new LanguageModel("Hebrew", "iw", ""));
        list.add(new LanguageModel("Malay", "ms", ""));

        Collections.sort(list, new Comparator<LanguageModel>() {
            @Override
            public int compare(LanguageModel languageModel, LanguageModel t1) {
                return languageModel.getLanguage().compareTo(t1.getLanguage());
            }
        });

        list.add(0, new LanguageModel("System Default", LANGUAGE_SYSTEM_DEFAULT, ""));

        return list;
    }

    public static void refreshService(Context context) {
        if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean("combine_notifications", false)) {
            if (!isCombinedNotificationServiceRunning(context)) {
                context.startService(new Intent(context, CompoundNotification.class));
            }
        }
        else {
            if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean("network_signal_notification", false)) {
                if (!isLiveNetworkServiceRunning(context)) {
                    context.startService(new Intent(context, LiveNetworkMonitor.class));
                }
            }
            if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean("setup_notification", false)) {
                if (!isNotificationServiceRunning(context)) {
                    context.startService(new Intent(context, NotificationService.class));
                }
            }
        }
        if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean("data_usage_alert", false)) {
            if (!isDataUsageAlertServiceRunning(context)) {
                context.startService(new Intent(context, DataUsageMonitor.class));
            }
        }

    }

    public static void setRefreshAlarm(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        long wakeupMillis = 0l;
        try {
            wakeupMillis = getTimePeriod(context, SESSION_CUSTOM, -1)[1];
        } catch (ParseException e) {
            e.printStackTrace();
        }

        Intent intent = new Intent(context, DataPlanRefreshReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 1001,
                intent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        if (wakeupMillis > System.currentTimeMillis()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, wakeupMillis, pendingIntent);
                } else {
                    Log.e(TAG, "setRefreshAlarm: permission SCHEDULE_EXACT_ALARM not granted");
                    postAlarmPermissionDeniedNotification(context);
                }
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, wakeupMillis, pendingIntent);
            }
            Log.d(TAG, "setRefreshAlarm: set");
        } else {
            Log.e(TAG, "setRefreshAlarm: something is wrong here " + wakeupMillis);
        }
    }

    public static void setDataPlanNotification(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        long wakeupMillis = 0l;
        try {
            wakeupMillis = getTimePeriod(context, SESSION_CUSTOM, -1)[1];
        } catch (ParseException e) {
            e.printStackTrace();
        }

        Intent intent = new Intent(context, DataPlanRefreshReceiver.class)
                .putExtra(INTENT_ACTION, ACTION_SHOW_DATA_PLAN_NOTIFICATION);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 1001,
                intent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        if (wakeupMillis > System.currentTimeMillis()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, wakeupMillis, pendingIntent);
                } else {
                    Log.e(TAG, "setRefreshAlarm: permission SCHEDULE_EXACT_ALARM not granted");
                    postAlarmPermissionDeniedNotification(context);
                }
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, wakeupMillis, pendingIntent);
            }
            Log.d(TAG, "setDataPlanNotification: set");
        } else {
            Log.e(TAG, "setDataPlanNotification: something is wrong here " + wakeupMillis);
        }
    }

    public static void cancelDataPlanNotification(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, DataPlanRefreshReceiver.class)
                .putExtra(INTENT_ACTION, ACTION_SHOW_DATA_PLAN_NOTIFICATION);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 1001,
                intent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        alarmManager.cancel(pendingIntent);
    }

    public static void updateDialog(AlertDialog dialog, Context context) {
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        manager.getDefaultDisplay().getMetrics(metrics);

        lp.copyFrom(dialog.getWindow().getAttributes());
        lp.width = (metrics.widthPixels * 85 / 100);
        lp.y = 50;
        dialog.getWindow().setAttributes(lp);
    }

    public static SpannableString setBoldSpan(String text, String spanText) {
        SpannableString boldSpan = new SpannableString(text);
        int start = text.indexOf(spanText);
        if (start < 0) {
            start = 0;
        }
        int end = start + spanText.length();
        boldSpan.setSpan(new StyleSpan(Typeface.BOLD), start, end, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        return boldSpan;
    }

    public static Long UTCToLocal(Long UTCTime) {
        int offset = TimeZone.getDefault().getOffset(new Date().getTime());
        return UTCTime - (offset);
    }

    public static Long localToUTC(Long localTime) {
        int offset = TimeZone.getDefault().getOffset(new Date().getTime());
        return localTime + (offset);
    }

    public static void showAlarmPermissionDeniedDialog(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                new MaterialAlertDialogBuilder(context)
                        .setTitle(context.getString(R.string.error_alarm_permission_denied))
                        .setMessage(context.getString(R.string.error_alarm_permission_denied_feature_summary))
                        .setCancelable(false)
                        .setPositiveButton(context.getString(R.string.action_grant), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent intent = new Intent();
                                intent.setAction(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                dialog.dismiss();
                                context.startActivity(intent);
                            }
                        })
                        .setNegativeButton(context.getString(R.string.action_cancel), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .setOnDismissListener(new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface dialog) {
                                PreferenceManager.getDefaultSharedPreferences(context).edit()
                                        .putBoolean(ALARM_PERMISSION_DENIED, true)
                                        .apply();
                            }
                        })
                        .show();
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    public static void postAlarmPermissionDeniedNotification(Context context) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, OTHER_NOTIFICATION_CHANNEL_ID);
        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 1, intent, PendingIntent.FLAG_IMMUTABLE);
        builder.setContentTitle(context.getString(R.string.error_alarm_permission_denied))
                .setContentText(context.getString(R.string.error_alarm_permission_denied_summary))
                .setStyle(new NotificationCompat.BigTextStyle())
                .setSmallIcon(R.drawable.ic_info)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_MAX);
        NotificationManagerCompat managerCompat = NotificationManagerCompat.from(context);
        postNotification(context, managerCompat, builder, OTHER_NOTIFICATION_ID);
    }

    public static String getPlanValidity(int session, Context context) {
        String validity;
        Calendar calendar = Calendar.getInstance();
        String month, ordinal, end;
        int endDate;
        if (session == SESSION_MONTHLY) {
            int planEnd = PreferenceManager.getDefaultSharedPreferences(context)
                    .getInt(DATA_RESET_DATE, 1);
            int daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
            if (planEnd > daysInMonth) {
                planEnd = daysInMonth;
            }
            int today = calendar.get(Calendar.DAY_OF_MONTH) + 1;
            if (today >= planEnd) {
                calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) + 1);
            }
            month = new SimpleDateFormat("MMMM", getCurrentLocale(context)).format(calendar.getTime());
            endDate = planEnd;
        }
        else {
            long planEndDateMillis;
            try {
                planEndDateMillis = PreferenceManager.getDefaultSharedPreferences(context)
                        .getLong(DATA_RESET_CUSTOM_DATE_END, -1);
            }
            catch (ClassCastException e) {
                int planEndIntValue = PreferenceManager.getDefaultSharedPreferences(context)
                        .getInt(DATA_RESET_CUSTOM_DATE_END, -1);
                planEndDateMillis = ((Number) planEndIntValue).longValue();
            }
            calendar.setTimeInMillis(planEndDateMillis);
            month = new SimpleDateFormat("MMMM", getCurrentLocale(context)).format(calendar.getTime());
            endDate = calendar.get(Calendar.DAY_OF_MONTH);
        }
        ordinal = formatOrdinalNumber(endDate, context);
        end = ordinal + " " + month;
        validity = end;
        return validity;
    }

    public static String formatOrdinalNumber(int number, Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            final String format = "{0,ordinal}";
            final Locale locale = getCurrentLocale(context);
            final MessageFormat formatter = new MessageFormat(format, locale);

            return formatter.format(new Object[] {number});
        } else {
            String numberString = String.valueOf(number);
            String suffix;
            if (numberString.endsWith("1")) {
                suffix = "st";
            } else if (numberString.endsWith("2")) {
                suffix = "nd";
            } else if (numberString.endsWith("3")) {
                suffix = "rd";
            } else {
                suffix = "th";
            }
            return numberString + suffix;
        }
    }

    public static Locale getCurrentLocale(Context context) {
        final Configuration config = context.getResources().getConfiguration();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return config.getLocales().get(0);
        } else {
            return config.locale;
        }
    }

    private static boolean isLiveNetworkServiceRunning(Context context) {
        // Check if the service is already running
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (LiveNetworkMonitor.class.getName().equals(service.service.getClassName()) ||
                    LiveNetworkMonitor.isServiceRunning) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCombinedNotificationServiceRunning(Context context) {
        // Check if the service is already running
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (CompoundNotification.class.getName().equals(service.service.getClassName()) ||
                    CompoundNotification.isServiceRunning) {
                return true;
            }
        }
        return false;
    }

    private static boolean isNotificationServiceRunning(Context context) {
        // Check if the service is already running
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (NotificationService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isDataUsageAlertServiceRunning(Context context) {
        // Check if the service is already running
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (DataUsageMonitor.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public static void postNotification(Context context, NotificationManagerCompat notificationManager,
                                           NotificationCompat.Builder builder, int notificationId) {
        if (notificationManager != null && builder != null) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED) {
                notificationManager.notify(notificationId, builder.build());
            }
        }
    }

    public static String parseNumber(String number) {
        if (number.matches("[0-9.,]+")) {
            return number.replace(",", ".");
        }
        String output = "0.0";
        NumberFormat numberFormat = NumberFormat.getInstance(Locale.US);
        try {
            output = numberFormat.parse(number).toString();
        }
        catch (Exception ignored) {}

        return output;
    }
}
