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

package com.drnoob.datamonitor.utils;

import static com.drnoob.datamonitor.Common.postNotification;
import static com.drnoob.datamonitor.core.Values.DATA_LIMIT;
import static com.drnoob.datamonitor.core.Values.DATA_RESET;
import static com.drnoob.datamonitor.core.Values.DATA_RESET_CUSTOM;
import static com.drnoob.datamonitor.core.Values.DATA_RESET_CUSTOM_DATE_END;
import static com.drnoob.datamonitor.core.Values.DATA_RESET_DAILY;
import static com.drnoob.datamonitor.core.Values.DATA_RESET_DATE;
import static com.drnoob.datamonitor.core.Values.DATA_RESET_HOUR;
import static com.drnoob.datamonitor.core.Values.DATA_RESET_MIN;
import static com.drnoob.datamonitor.core.Values.DATA_RESET_MONTHLY;
import static com.drnoob.datamonitor.core.Values.DATA_USAGE_WARNING_CHANNEL_ID;
import static com.drnoob.datamonitor.core.Values.DATA_USAGE_WARNING_NOTIFICATION_ID;
import static com.drnoob.datamonitor.core.Values.DATA_USAGE_WARNING_SHOWN;
import static com.drnoob.datamonitor.core.Values.DATA_WARNING_TRIGGER_LEVEL;
import static com.drnoob.datamonitor.core.Values.EXTRA_DATA_ALARM_RESET;
import static com.drnoob.datamonitor.core.Values.SESSION_TODAY;
import static com.drnoob.datamonitor.utils.NetworkStatsHelper.formatData;
import static com.drnoob.datamonitor.utils.NetworkStatsHelper.getDeviceMobileDataUsage;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.preference.PreferenceManager;

import com.drnoob.datamonitor.Common;
import com.drnoob.datamonitor.R;
import com.google.android.material.datepicker.MaterialDatePicker;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class DataUsageMonitor extends Service {
    private static final String TAG = DataUsageMonitor.class.getSimpleName();
    private static final DataMonitor dataMonitor = new DataMonitor();
    private static DataUsageMonitor mDataUsageMonitor;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mDataUsageMonitor = this;
        boolean isChecked = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("data_usage_alert", false);
        if (isChecked) {
//            startForeground(0, null);
            startMonitor(this);
            AlarmManager manager = (AlarmManager) getSystemService(ALARM_SERVICE);
            Intent intent = new Intent(this, DataMonitor.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_ONE_SHOT|PendingIntent.FLAG_IMMUTABLE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (manager.canScheduleExactAlarms()) {
                    manager.setExact(AlarmManager.RTC, System.currentTimeMillis(), pendingIntent);
                }
                else  {
                    Log.e(TAG, "setRefreshAlarm: permission SCHEDULE_EXACT_ALARM not granted" );
                    Common.postAlarmPermissionDeniedNotification(this);
                }
            }
            else {
                manager.setExact(AlarmManager.RTC, System.currentTimeMillis(), pendingIntent);
            }
        }
        else {
            onDestroy();
        }

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        boolean isRestart;
        if (intent != null) {
            isRestart = intent.getBooleanExtra(EXTRA_DATA_ALARM_RESET, false);
        }
        else {
            isRestart = false;
        }
        if (isRestart) {
            Log.d(TAG, "onStartCommand: Restarting alarm");
            PreferenceManager.getDefaultSharedPreferences(this).edit()
                    .putBoolean("data_usage_warning_shown", false)
                    .apply();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: ");
        stopMonitor(this);
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        Log.d(TAG, "onTaskRemoved: ");
    }

    public static void startMonitor(Context context) {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.setPriority(100);
        intentFilter.addAction(Intent.ACTION_BOOT_COMPLETED);
        intentFilter.addAction(Intent.ACTION_SCREEN_ON);
        context.registerReceiver(dataMonitor, intentFilter);
    }

    public static void stopMonitor(Context context) {
        try {
            context.unregisterReceiver(dataMonitor);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void stopService(Context context) {
        Log.d(TAG, "stopService: ");
        if (mDataUsageMonitor != null) {
            mDataUsageMonitor.stopSelf();
            context.stopService(new Intent(context, mDataUsageMonitor.getClass()));
        }
        else {
            Log.d(TAG, "stopService: mDataUsageMonitor returned null. Attempting to stop");
            context.stopService(new Intent(context, DataUsageMonitor.class));
        }
    }

    public static class DataMonitor extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            boolean isWaningEnabled = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("data_usage_alert", false);
            if (isWaningEnabled) {
                int trigger = PreferenceManager.getDefaultSharedPreferences(context).getInt(DATA_WARNING_TRIGGER_LEVEL, 85);
                Float dataLimit = PreferenceManager.getDefaultSharedPreferences(context).getFloat(DATA_LIMIT, -1);

                Double triggerLevel = 0d;
                if (dataLimit > 0) {
                    triggerLevel = dataLimit.doubleValue() * trigger / 100;
                }
                try {
                    String totalRaw = formatData(getDeviceMobileDataUsage(context, SESSION_TODAY, 1)[0],
                            getDeviceMobileDataUsage(context, SESSION_TODAY, 1)[1])[2];
                    Double totalData = 0d;
                    if (totalRaw.contains(",")) {
                        totalRaw = totalRaw.replace(",", ".");
                    }
                    if (totalRaw.contains("٫")) {
                        totalRaw = totalRaw.replace("٫", ".");
                    }
                    try {
                        if (totalRaw.contains(" MB")) {
                            totalRaw = totalRaw.replace(" MB", "");
                            totalData = Double.parseDouble(totalRaw);
                        } else {
                            totalRaw = totalRaw.replace(" GB", "");
                            totalData = Double.parseDouble(totalRaw) * 1024;
                        }
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }

                    Log.d(TAG, "onReceive: " + totalData + " " + triggerLevel.intValue());
                    if (totalData.intValue() > triggerLevel.intValue() || totalData.intValue() == triggerLevel.intValue()) {
                        Log.d(TAG, "onReceive: Notification shown: " + PreferenceManager.getDefaultSharedPreferences(context).getBoolean("data_usage_warning_shown", false));
                        if (!PreferenceManager.getDefaultSharedPreferences(context).getBoolean("data_usage_warning_shown", false)) {
                            try {
                                showNotification(context);
                            }
                            catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        else {
                            stopService(context);
                        }
                    }
                    if (totalData.intValue() >= dataLimit.intValue()) {

                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                setRepeating(context);
            }
        }

        private void showNotification(Context context) throws ParseException {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context,
                    DATA_USAGE_WARNING_CHANNEL_ID);
            int triggerLevel = PreferenceManager.getDefaultSharedPreferences(context).getInt(DATA_WARNING_TRIGGER_LEVEL, 85);
            Uri uri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            builder.setContentTitle(context.getString(R.string.title_data_warning_notification))
                    .setContentText(context.getString(R.string.body_data_warning_notification, triggerLevel))
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setSmallIcon(R.drawable.ic_info)
                    .setDefaults(NotificationCompat.DEFAULT_ALL)
                    .setShowWhen(true)
                    .setAutoCancel(true)
                    .setSound(uri)
                    .setVibrate(new long[]{0, 100, 1000, 300});
            NotificationManagerCompat managerCompat = NotificationManagerCompat.from(context);
            postNotification(context, managerCompat, builder, DATA_USAGE_WARNING_NOTIFICATION_ID);
            PreferenceManager.getDefaultSharedPreferences(context).edit()
                    .putBoolean(DATA_USAGE_WARNING_SHOWN, true)
                    .apply();
            stopService(context);
            restartMonitor(context);
        }

        private static void setRepeating(Context context) {
            if (!PreferenceManager.getDefaultSharedPreferences(context).getBoolean("data_usage_warning_shown", false)) {
                Intent intent = new Intent(context, DataUsageMonitor.DataMonitor.class);
                PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_ONE_SHOT|PendingIntent.FLAG_IMMUTABLE);
                AlarmManager alarmManager = (AlarmManager) context.getSystemService(ALARM_SERVICE);
//            int elapsedTime = PreferenceManager.getDefaultSharedPreferences(context)
//                    .getInt(NOTIFICATION_REFRESH_INTERVAL, 6000);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExact(AlarmManager.RTC, System.currentTimeMillis() + 30000, pendingIntent);
                    }
                    else  {
                        Log.e(TAG, "setRefreshAlarm: permission SCHEDULE_EXACT_ALARM not granted" );
                        Common.postAlarmPermissionDeniedNotification(context);
                    }
                }
                else {
                    alarmManager.setExact(AlarmManager.RTC, System.currentTimeMillis() + 30000, pendingIntent);
                }
            }
            else {
                Log.d(TAG, "setRepeating: Stopping monitor");
                stopMonitor(context);
            }
        }

        private static void restartMonitor(Context context) throws ParseException {
            AlarmManager manager = (AlarmManager) context.getSystemService(ALARM_SERVICE);

            int year, month, day;
            String resetTime, endTime;
            Date resetDate, endDate;
            Long resetTimeMillis = 0L,
                    endTimeMillis = 0L;
            SimpleDateFormat resetFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

            int resetHour = PreferenceManager.getDefaultSharedPreferences(context)
                    .getInt(DATA_RESET_HOUR, 0);
            int resetMin = PreferenceManager.getDefaultSharedPreferences(context)
                    .getInt(DATA_RESET_MIN, 0);
            SimpleDateFormat yearFormat = new SimpleDateFormat("yyyy");
            SimpleDateFormat monthFormat = new SimpleDateFormat("MM");
            SimpleDateFormat dayFormat = new SimpleDateFormat("dd");
            Date date = new Date();

            String planType = PreferenceManager.getDefaultSharedPreferences(context)
                    .getString(DATA_RESET, DATA_RESET_DAILY);

            switch (planType) {
                case DATA_RESET_DAILY:
                    year = Integer.parseInt(yearFormat.format(date));
                    month = Integer.parseInt(monthFormat.format(date));
                    day = Integer.parseInt(dayFormat.format(date));
                    resetTime = context.getResources().getString(R.string.reset_time, year, month, day, resetHour, resetMin);
                    resetDate = resetFormat.parse(resetTime);
                    resetTimeMillis = resetDate.getTime();
                    day = Integer.parseInt(dayFormat.format(date)) + 1;
                    endTime = context.getResources().getString(R.string.reset_time, year, month, day, resetHour, resetMin);
                    endDate = resetFormat.parse(endTime);
                    endTimeMillis = endDate.getTime();
                    break;

                case DATA_RESET_MONTHLY:
                    int planEnd = PreferenceManager.getDefaultSharedPreferences(context)
                            .getInt(DATA_RESET_DATE, 1);
                    Calendar calendar = Calendar.getInstance();
                    int daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
                    int today = calendar.get(Calendar.DAY_OF_MONTH) + 1;
                    if (planEnd > daysInMonth) {
                        planEnd = daysInMonth;
                    }
                    calendar.set(Calendar.DAY_OF_MONTH, planEnd);
                    if (today >= planEnd) {
                        calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) + 1);
                    }
                    calendar.set(Calendar.HOUR_OF_DAY, 0);
                    calendar.set(Calendar.MINUTE, 0);
                    calendar.set(Calendar.SECOND, 0);
                    endTimeMillis = calendar.getTimeInMillis();
                    break;

                case DATA_RESET_CUSTOM:
                    try {
                        endTimeMillis = PreferenceManager.getDefaultSharedPreferences(context)
                                .getLong(DATA_RESET_CUSTOM_DATE_END, MaterialDatePicker.todayInUtcMilliseconds());
                    }
                    catch (ClassCastException e) {
                        int planEndIntValue = PreferenceManager.getDefaultSharedPreferences(context)
                                .getInt(DATA_RESET_CUSTOM_DATE_END, -1);
                        endTimeMillis = ((Number) planEndIntValue).longValue();
                    }
                    break;
            }

            if (resetTimeMillis > System.currentTimeMillis()) {
                year = Integer.parseInt(yearFormat.format(date));
                month = Integer.parseInt(monthFormat.format(date));
                day = Integer.parseInt(dayFormat.format(date));
                day = day - 1;
                resetTime = context.getResources().getString(R.string.reset_time, year, month, day, resetHour, resetMin);
                resetDate = resetFormat.parse(resetTime);
                resetTimeMillis = resetDate.getTime();

                resetTime = context.getResources().getString(R.string.reset_time, year, month, day, resetHour, resetMin);
                resetDate = resetFormat.parse(resetTime);
                resetTimeMillis = resetDate.getTime();
                day = Integer.parseInt(dayFormat.format(date));
                endTime = context.getResources().getString(R.string.reset_time, year, month, day, resetHour, resetMin);
                endDate = resetFormat.parse(endTime);
                endTimeMillis = endDate.getTime();
            }
            else {
                if (planType.equals(DATA_RESET_DAILY)) {
                    year = Integer.parseInt(yearFormat.format(date));
                    month = Integer.parseInt(monthFormat.format(date));
                    day = Integer.parseInt(dayFormat.format(date));
                    resetTime = context.getResources().getString(R.string.reset_time, year, month, day, resetHour, resetMin);
                    resetDate = resetFormat.parse(resetTime);
                    resetTimeMillis = resetDate.getTime();

                    day = Integer.parseInt(dayFormat.format(date)) + 1;
                    endTime = context.getResources().getString(R.string.reset_time, year, month, day, resetHour, resetMin);
                    endDate = resetFormat.parse(endTime);
                    endTimeMillis = endDate.getTime();
                }

            }

            Intent restartIntent = new Intent(context, DataUsageMonitor.class);
            restartIntent.putExtra(EXTRA_DATA_ALARM_RESET, true);
            PendingIntent pendingIntent = PendingIntent.getService(context, 0,
                    restartIntent, PendingIntent.FLAG_ONE_SHOT|PendingIntent.FLAG_IMMUTABLE);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (manager.canScheduleExactAlarms()) {
                    manager.setExact(AlarmManager.RTC, endTimeMillis, pendingIntent);
                }
                else  {
                    Log.e(TAG, "setRefreshAlarm: permission SCHEDULE_EXACT_ALARM not granted" );
                    Common.postAlarmPermissionDeniedNotification(context);
                }
            }
            else {
                manager.setExact(AlarmManager.RTC, endTimeMillis, pendingIntent);
            }
            Log.d(TAG, "restartMonitor: Restart at: " + endTimeMillis);
        }
    }

    public static void updateServiceRestart(Context context) {
        if (PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(DATA_USAGE_WARNING_SHOWN, false)) {
            AlarmManager manager = (AlarmManager) context.getSystemService(ALARM_SERVICE);
            Intent intent = new Intent(context, DataUsageMonitor.class);
            PendingIntent pendingIntent = PendingIntent.getService(context, 0,
                    intent, PendingIntent.FLAG_ONE_SHOT|PendingIntent.FLAG_IMMUTABLE);
            manager.cancel(pendingIntent);
            Log.d(TAG, "updateServiceRestart: Monitor cancelled, creating new one");
            try {
                DataMonitor.restartMonitor(context);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
