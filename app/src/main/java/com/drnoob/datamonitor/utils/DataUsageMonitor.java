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

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.preference.PreferenceManager;

import com.drnoob.datamonitor.R;
import com.drnoob.datamonitor.ui.fragments.SetupFragment;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static com.drnoob.datamonitor.core.Values.DATA_LIMIT;
import static com.drnoob.datamonitor.core.Values.DATA_RESET_HOUR;
import static com.drnoob.datamonitor.core.Values.DATA_RESET_MIN;
import static com.drnoob.datamonitor.core.Values.DATA_USAGE_NOTIFICATION_CHANNEL_ID;
import static com.drnoob.datamonitor.core.Values.DATA_USAGE_WARNING_CHANNEL_ID;
import static com.drnoob.datamonitor.core.Values.DATA_USAGE_WARNING_NOTIFICATION_ID;
import static com.drnoob.datamonitor.core.Values.DATA_USAGE_WARNING_SHOWN;
import static com.drnoob.datamonitor.core.Values.DATA_WARNING_TRIGGER_LEVEL;
import static com.drnoob.datamonitor.core.Values.NOTIFICATION_REFRESH_INTERVAL;
import static com.drnoob.datamonitor.core.Values.SESSION_TODAY;
import static com.drnoob.datamonitor.utils.NetworkStatsHelper.formatData;
import static com.drnoob.datamonitor.utils.NetworkStatsHelper.getDeviceMobileDataUsage;

public class DataUsageMonitor extends Service {
    private static final String TAG = DataUsageMonitor.class.getSimpleName();
    private static final DataMonitor dataMonitor = new DataMonitor();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        startForeground(0, null);
        startMonitor(this);
        AlarmManager manager = (AlarmManager) getSystemService(ALARM_SERVICE);
        Intent intent = new Intent(this, DataMonitor.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_ONE_SHOT);
        manager.setExact(AlarmManager.RTC, System.currentTimeMillis(), pendingIntent);

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

    public static class DataMonitor extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            int trigger = PreferenceManager.getDefaultSharedPreferences(context).getInt(DATA_WARNING_TRIGGER_LEVEL, 85);
            Float dataLimit = PreferenceManager.getDefaultSharedPreferences(context).getFloat(DATA_LIMIT, -1);

            Double triggerLevel = 0d;
            if (dataLimit > 0) {
                triggerLevel = dataLimit.doubleValue() * trigger / 100;
            }
            try {
                String totalRaw = formatData(getDeviceMobileDataUsage(context, SESSION_TODAY)[0],
                        getDeviceMobileDataUsage(context, SESSION_TODAY)[1])[2];
                Double totalData = 0d;
                if (totalRaw.contains(" MB")) {
                    totalRaw = totalRaw.replace(" MB", "");
                    totalData = Double.parseDouble(totalRaw);
                } else {
                    totalRaw = totalRaw.replace(" GB", "");
                    totalData = Double.parseDouble(totalRaw) * 1024;
                }

                Log.d(TAG, "onReceive: " + totalData + " " + triggerLevel.intValue());
                if (totalData.intValue() > triggerLevel.intValue() || totalData.intValue() == triggerLevel.intValue()) {
                    Log.d(TAG, "onReceive: " + PreferenceManager.getDefaultSharedPreferences(context).getBoolean("data_usage_warning_shown", false));
                    if (!PreferenceManager.getDefaultSharedPreferences(context).getBoolean("data_usage_warning_shown", false)) {
                        showNotification(context);
                    } else {
                        SetupFragment.SetupPreference.pauseMonitor();
                        restartMonitor(context);
                    }
                }
                if (totalData.intValue() >= dataLimit.intValue()) {

                }
            } catch (ParseException e) {
                e.printStackTrace();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            setRepeating(context);
        }

        private void showNotification(Context context) throws ParseException {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context,
                    DATA_USAGE_WARNING_CHANNEL_ID);
            int triggerLevel = PreferenceManager.getDefaultSharedPreferences(context).getInt(DATA_WARNING_TRIGGER_LEVEL, 85);
            builder.setContentTitle(context.getString(R.string.title_data_warning_notification))
                    .setContentText(context.getString(R.string.body_data_warning_notification, triggerLevel))
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setSmallIcon(R.drawable.ic_info)
                    .setVibrate(new long[]{0, 100, 1000, 300});
            NotificationManagerCompat managerCompat = NotificationManagerCompat.from(context);
            managerCompat.notify(DATA_USAGE_WARNING_NOTIFICATION_ID, builder.build());
            PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(DATA_USAGE_WARNING_SHOWN, true).apply();
            SetupFragment.SetupPreference.pauseMonitor();
            restartMonitor(context);
        }

        private static void setRepeating(Context context) {
            Intent intent = new Intent(context, DataUsageMonitor.DataMonitor.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_ONE_SHOT);
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(ALARM_SERVICE);
//            int elapsedTime = PreferenceManager.getDefaultSharedPreferences(context)
//                    .getInt(NOTIFICATION_REFRESH_INTERVAL, 6000);
            alarmManager.setExact(AlarmManager.RTC, System.currentTimeMillis() + 60000, pendingIntent);
        }

        private void restartMonitor(Context context) throws ParseException {
            AlarmManager manager = (AlarmManager) context.getSystemService(ALARM_SERVICE);
            Intent restartIntent = new Intent(context, DataMonitor.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, restartIntent, PendingIntent.FLAG_ONE_SHOT);
            int year, month, day;
            String resetTime, endTime;
            Date resetDate, endDate;
            Long resetTimeMillis, endTimeMillis;
            SimpleDateFormat resetFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

            int resetHour = PreferenceManager.getDefaultSharedPreferences(context)
                    .getInt(DATA_RESET_HOUR, 0);
            int resetMin = PreferenceManager.getDefaultSharedPreferences(context)
                    .getInt(DATA_RESET_MIN, 0);
            SimpleDateFormat yearFormat = new SimpleDateFormat("yyyy");
            SimpleDateFormat monthFormat = new SimpleDateFormat("MM");
            SimpleDateFormat dayFormat = new SimpleDateFormat("dd");
            Date date = new Date();

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
            } else {
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

            manager.setExact(AlarmManager.RTC, endTimeMillis, pendingIntent);

            Log.d(TAG, "onReceive: " + endTimeMillis);
        }
    }
}
