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

package com.drnoob.datamonitor.Widget;

import static com.drnoob.datamonitor.core.Values.DATA_LIMIT;
import static com.drnoob.datamonitor.core.Values.DATA_RESET;
import static com.drnoob.datamonitor.core.Values.DATA_RESET_CUSTOM;
import static com.drnoob.datamonitor.core.Values.DATA_RESET_DAILY;
import static com.drnoob.datamonitor.core.Values.DATA_RESET_DATE;
import static com.drnoob.datamonitor.core.Values.DATA_RESET_MONTHLY;
import static com.drnoob.datamonitor.core.Values.SESSION_CUSTOM;
import static com.drnoob.datamonitor.core.Values.SESSION_MONTHLY;
import static com.drnoob.datamonitor.core.Values.SESSION_TODAY;
import static com.drnoob.datamonitor.utils.NetworkStatsHelper.formatData;
import static com.drnoob.datamonitor.utils.NetworkStatsHelper.getDeviceMobileDataUsage;
import static com.drnoob.datamonitor.utils.NetworkStatsHelper.getDeviceWifiDataUsage;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import androidx.preference.PreferenceManager;

import com.drnoob.datamonitor.Common;
import com.drnoob.datamonitor.R;
import com.drnoob.datamonitor.ui.activities.MainActivity;

import java.text.ParseException;
import java.util.Calendar;

/**
 * Implementation of App Widget functionality.
 */

public class DataUsageWidget extends AppWidgetProvider {
    private static final String TAG = DataUsageWidget.class.getSimpleName();
    public static final String TYPE_MANUAL_REFRESH = "widget_manual_refresh";

    private static boolean isReceiverRunning = false;
    private boolean manualRefresh = false;

    public boolean isManualRefresh() {
        return manualRefresh;
    }

    public void setManualRefresh(boolean manualRefresh) {
        this.manualRefresh = manualRefresh;
    }

    public void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {
        Long[] mobile = null;
        Long[] wifi = null;
        String mobileData = null;
        String wifiData = null;
        int date = PreferenceManager.getDefaultSharedPreferences(context).getInt(DATA_RESET_DATE, 1);

        try {

            if (PreferenceManager.getDefaultSharedPreferences(context).getString(DATA_RESET, "null")
                    .equals(DATA_RESET_MONTHLY)) {
                mobile = getDeviceMobileDataUsage(context, SESSION_MONTHLY, date);
            }
            else if (PreferenceManager.getDefaultSharedPreferences(context).getString(DATA_RESET, "null")
                    .equals(DATA_RESET_DAILY)) {
                mobile = getDeviceMobileDataUsage(context, SESSION_TODAY, 1);
            }
            else if (PreferenceManager.getDefaultSharedPreferences(context).getString(DATA_RESET, "null")
                    .equals(DATA_RESET_CUSTOM)) {
                mobile = getDeviceMobileDataUsage(context, SESSION_CUSTOM, -1);
            }
            else {
                mobile = getDeviceMobileDataUsage(context, SESSION_TODAY, -1);
            }

            mobileData = formatData(mobile[0], mobile[1])[2];

            wifi = getDeviceWifiDataUsage(context, SESSION_TODAY);
            wifiData = formatData(wifi[0], wifi[1])[2];

        } catch (ParseException e) {
            e.printStackTrace();
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        // Construct the RemoteViews object
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.data_usage_widget);

        views.setTextViewText(R.id.widget_mobile_data_used, mobileData);
        views.setTextViewText(R.id.widget_wifi_used, wifiData);

        if (isManualRefresh()) {
            views.setViewVisibility(R.id.widget_update, View.INVISIBLE);
            views.setViewVisibility(R.id.widget_update_progress, View.VISIBLE);
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }

        Log.d(TAG, "updateAppWidget: " + mobileData + "  " + wifiData);

        Boolean showRemaining = PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean("remaining_data_info", true);
        Boolean showWifiUsage = PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean("widget_show_wifi_usage", true);
        Float dataLimit = PreferenceManager.getDefaultSharedPreferences(context).getFloat(DATA_LIMIT, -1);
        if (dataLimit < 0) {
            views.setTextViewText(R.id.widget_data_usage_remaining, "");
            views.setViewVisibility(R.id.widget_data_usage_remaining, View.GONE);
        }

        if (!showWifiUsage) {
            views.setViewVisibility(R.id.layout_wifi, View.GONE);
        }
        else {
            views.setViewVisibility(R.id.layout_wifi, View.VISIBLE);
        }

        if (showRemaining) {
            if (dataLimit > 0) {
                if (PreferenceManager.getDefaultSharedPreferences(context).getString(DATA_RESET, null)
                        .equals(DATA_RESET_DAILY)) {
                    Long total = (mobile[2]);
                    Long limit = dataLimit.longValue() * 1048576;
                    Long remaining;
                    String remainingData;
                    if (limit > total) {
                        remaining= limit - total;
                        remainingData = formatData(remaining / 2, remaining / 2)[2];
                        views.setTextViewText(R.id.widget_data_usage_remaining, context.getString(R.string.label_data_remaining, remainingData));
                    }
                    else {
                        remaining= total - limit;
                        remainingData = formatData(remaining / 2, remaining / 2)[2];
                        views.setTextViewText(R.id.widget_data_usage_remaining, context.getString(R.string.label_data_remaining_used_excess, remainingData));
                    }
                    Log.d(TAG, "updateAppWidget: " + remainingData);
                } else if (PreferenceManager.getDefaultSharedPreferences(context).getString(DATA_RESET, null)
                        .equals(DATA_RESET_MONTHLY)) {
                    try {
                        Long total = getDeviceMobileDataUsage(context, SESSION_MONTHLY, date)[2];
                        Long limit = dataLimit.longValue() * 1048576;
                        Long remaining;
                        String remainingData;
                        if (limit > total) {
                            remaining= limit - total;
                            remainingData = formatData(remaining / 2, remaining / 2)[2];
                            views.setTextViewText(R.id.widget_data_usage_remaining, context.getString(R.string.label_data_remaining, remainingData));
                        }
                        else {
                            remaining= total - limit;
                            remainingData = formatData(remaining / 2, remaining / 2)[2];
                            views.setTextViewText(R.id.widget_data_usage_remaining, context.getString(R.string.label_data_remaining_used_excess, remainingData));
                        }
                        Log.d(TAG, "updateAppWidget: " + remainingData);
                    } catch (ParseException e) {
                        e.printStackTrace();
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
                else {
                    Long total = (mobile[2]);
                    Long limit = dataLimit.longValue() * 1048576;
                    Long remaining;
                    String remainingData;
                    if (limit > total) {
                        remaining= limit - total;
                        remainingData = formatData(remaining / 2, remaining / 2)[2];
                        views.setTextViewText(R.id.widget_data_usage_remaining, context.getString(R.string.label_data_remaining, remainingData));
                    }
                    else {
                        remaining= total - limit;
                        remainingData = formatData(remaining / 2, remaining / 2)[2];
                        views.setTextViewText(R.id.widget_data_usage_remaining, context.getString(R.string.label_data_remaining_used_excess, remainingData));
                    }
                    Log.d(TAG, "updateAppWidget: " + remainingData);
                }
                views.setViewVisibility(R.id.widget_data_usage_remaining, View.VISIBLE);

            }

        }
        else {
            views.setTextViewText(R.id.widget_data_usage_remaining, "");
            views.setViewVisibility(R.id.widget_data_usage_remaining, View.GONE);
        }
//        views.setTextViewText(R.id.widget_wifi_usage_remaining, "");

        Intent intent = new Intent(context, DataUsageWidget.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        intent.setType(TYPE_MANUAL_REFRESH);
        int[] ids = AppWidgetManager.getInstance(context)
                .getAppWidgetIds(new ComponentName(context, DataUsageWidget.class));
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, appWidgetId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);


//        Intent intent = new Intent(context, WidgetUpdate.class);
//        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, appWidgetId, intent,
//                PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);

        views.setOnClickPendingIntent(R.id.widget_update, pendingIntent);

        Intent appIntent = new Intent(Intent.ACTION_MAIN);
        appIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        appIntent.setComponent(new ComponentName(context.getPackageName(), MainActivity.class.getName()));

        PendingIntent appPI = PendingIntent.getActivity(context, 0, appIntent, PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(android.R.id.background, appPI);

        // Instruct the widget manager to update the widget

        Log.e(TAG, "updateAppWidget: " + isManualRefresh() );
        if (isManualRefresh()) {
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    Log.e(TAG, "run: " );
                    views.setViewVisibility(R.id.widget_update, View.VISIBLE);
                    views.setViewVisibility(R.id.widget_update_progress, View.GONE);
                    appWidgetManager.updateAppWidget(appWidgetId, views);
                }
            }, 750);
        }
        else {
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }

        Log.e(TAG, "updateAppWidget: end" );

    }



    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getType() != null) {
            if (intent.getType().equals(TYPE_MANUAL_REFRESH)) {
                setManualRefresh(true);
            }
            else {
                setManualRefresh(false);
            }
        }
        else {
            setManualRefresh(false);
        }
        super.onReceive(context, intent);
        if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            int[] ids = appWidgetManager.getAppWidgetIds(new ComponentName(context, DataUsageWidget.class));
            if (ids.length > 0) {
                onUpdate(context, appWidgetManager, ids);
            }
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them

        for (int appWidgetId : appWidgetIds) {
            try {
                updateAppWidget(context, appWidgetManager, appWidgetId);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        Log.d(TAG, "onUpdate: " + isReceiverRunning);
        if (!isReceiverRunning) {
            startReceiver(context);
        }
        isReceiverRunning = true;
        int[] ids = appWidgetManager.getAppWidgetIds(new ComponentName(context, DataUsageWidget.class));
        if (ids.length > 0) {
            setRepeating(context);
        }
        Log.d(TAG, "onUpdate: Widget updated");
    }

    private void pushWidgetUpdate(Context context, RemoteViews views) {
        ComponentName componentName = new ComponentName(context, DataUsageWidget.class);
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        appWidgetManager.updateAppWidget(componentName, views);
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }

    private static void setRepeating(Context context) {
        Intent intent = new Intent(context, DataUsageWidget.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        int[] ids = AppWidgetManager.getInstance(context)
                .getAppWidgetIds(new ComponentName(context, DataUsageWidget.class));
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        int elapsedTime = PreferenceManager.getDefaultSharedPreferences(context).getInt("widget_refresh_interval", 60000);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExact(AlarmManager.RTC, System.currentTimeMillis() + elapsedTime, pendingIntent);
            }
            else  {
                Log.e(TAG, "setRefreshAlarm: permission SCHEDULE_EXACT_ALARM not granted" );
                Common.postAlarmPermissionDeniedNotification(context);
            }
        }
        else {
            alarmManager.setExact(AlarmManager.RTC, System.currentTimeMillis() + elapsedTime, pendingIntent);
        }
    }

    private void startReceiver(Context context) {
        DataUsageWidget dataUsageWidget = new DataUsageWidget();
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        intentFilter.setPriority(100);
        context.getApplicationContext().registerReceiver(dataUsageWidget, intentFilter);

    }
}