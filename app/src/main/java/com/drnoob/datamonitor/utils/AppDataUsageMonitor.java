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

import static com.drnoob.datamonitor.core.Values.APP_DATA_USAGE_WARNING_CHANNEL_ID;
import static com.drnoob.datamonitor.core.Values.APP_DATA_USAGE_WARNING_NOTIFICATION_ID;
import static com.drnoob.datamonitor.core.Values.SESSION_TODAY;
import static com.drnoob.datamonitor.utils.NetworkStatsHelper.formatData;
import static com.drnoob.datamonitor.utils.NetworkStatsHelper.getAppMobileDataUsage;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.drnoob.datamonitor.R;
import com.drnoob.datamonitor.adapters.data.AppDataUsageModel;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

public class AppDataUsageMonitor extends Service {
    private static final String TAG = AppDataUsageMonitor.class.getSimpleName();
    private static final AppDataMonitor appDataMonitor = new AppDataMonitor();

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
        Intent intent = new Intent(this, AppDataMonitor.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 1, intent, PendingIntent.FLAG_ONE_SHOT|PendingIntent.FLAG_IMMUTABLE);
        manager.setExact(AlarmManager.RTC, System.currentTimeMillis(), pendingIntent);
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopMonitor(this);
    }

    public static void startMonitor(Context context) {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.setPriority(100);
        intentFilter.addAction(Intent.ACTION_BOOT_COMPLETED);
        intentFilter.addAction(Intent.ACTION_SCREEN_ON);
        context.registerReceiver(appDataMonitor, intentFilter);
    }

    public static void stopMonitor(Context context) {
        try {
            context.unregisterReceiver(appDataMonitor);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static class AppDataMonitor extends BroadcastReceiver {
        List<AppDataUsageModel> mList = new ArrayList<>();

        @Override
        public void onReceive(Context context, Intent intent) {
            Type type = new TypeToken<List<AppDataUsageModel>>() {}.getType();
            AppDataUsageModel model = null;
            Gson gson = new Gson();
            String s = SharedPreferences.getAppDataLimitPrefs(context).getString("monitor_apps_list", null);
            List<AppDataUsageModel> list = gson.fromJson(s, type);
            if (list != null) {
                if (list.size() < 1) {
                    stopMonitor(context);
                }
                else {
                    for (int i = 0; i < list.size(); i++) {
                        String jsonText = SharedPreferences.getAppDataLimitPrefs(context).getString(list.get(i).getPackageName(), null);
                        if (jsonText != null) {
                            String[] data = gson.fromJson(jsonText, String[].class);
                            model = list.get(i);
                            model.setDataLimit(data[1]);
                            model.setDataType(data[2]);
                            model.setIsAppsList(false);
                            String isNotificationShown;
                            if (data.length > 3) {
                                isNotificationShown = data[3];
                            }
                            else {
                                isNotificationShown = "no";
                            }

                            try {
                                String totalData = formatData(getAppMobileDataUsage(context, context.getPackageManager().getApplicationInfo(list.get(i).getPackageName(), 0).uid, SESSION_TODAY)[0],
                                        getAppMobileDataUsage(context, context.getPackageManager().getApplicationInfo(list.get(i).getPackageName(), 0).uid, SESSION_TODAY)[1])[2];
                                String totalDataType = totalData.split("\\s+")[1];
                                Float totalDataUsed = Float.parseFloat(totalData.split("\\s+")[0]);
                                int dataLimit = Integer.parseInt(model.getDataLimit());

                                if (totalDataType.equals(context.getString(R.string.data_type_gb))) {
                                    totalDataUsed = totalDataUsed * 1024;
                                }
                                if (model.getDataType().equals("1")) {
                                    dataLimit = dataLimit * 1024;
                                }

                                if (totalDataUsed.intValue() >= dataLimit || totalDataUsed.intValue() == dataLimit) {
                                    if (isNotificationShown.equals("no")) {
                                        showNotification(context, model.getAppName());
                                        data = new String[] {data[0], data[1], data[2], "yes"};
                                        jsonText = gson.toJson(data, String[].class);
                                        SharedPreferences.getAppDataLimitPrefs(context).edit().putString(model.getPackageName(), jsonText).apply();
                                    }
                                    else {

                                    }

                                }



                                Log.e(TAG, "onReceive: " + totalDataUsed + " " + model.getDataLimit() + "  " + model.getDataType() );

                            } catch (RemoteException e) {
                                e.printStackTrace();
                            } catch (ParseException e) {
                                e.printStackTrace();
                            } catch (PackageManager.NameNotFoundException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    setRepeating(context);
                }
            }

        }

        private void showNotification(Context context, String appName) {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, APP_DATA_USAGE_WARNING_CHANNEL_ID);
            builder.setContentTitle(context.getString(R.string.title_data_warning_notification))
                    .setContentText(context.getString(R.string.body_app_data_warning_notification, appName))
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setSmallIcon(R.drawable.ic_info)
                    .setVibrate(new long[]{0, 100, 1000, 300});
            NotificationManagerCompat managerCompat = NotificationManagerCompat.from(context);
            managerCompat.notify(APP_DATA_USAGE_WARNING_NOTIFICATION_ID, builder.build());
        }

        private static void setRepeating(Context context) {
            Intent intent = new Intent(context, AppDataUsageMonitor.AppDataMonitor.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_ONE_SHOT|PendingIntent.FLAG_IMMUTABLE);
            AlarmManager manager = (AlarmManager) context.getSystemService(ALARM_SERVICE);
            manager.setExact(AlarmManager.RTC, System.currentTimeMillis() + 60000, pendingIntent);

        }

        private void restartMonitor(Context context) {
            Intent intent = new Intent(context, AppDataUsageMonitor.AppDataMonitor.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_ONE_SHOT|PendingIntent.FLAG_IMMUTABLE);
            AlarmManager manager = (AlarmManager) context.getSystemService(ALARM_SERVICE);
            manager.setExact(AlarmManager.RTC, System.currentTimeMillis(), pendingIntent);
        }
    }
}
