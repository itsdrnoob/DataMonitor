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

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import androidx.preference.PreferenceManager;

import com.drnoob.datamonitor.R;
import com.drnoob.datamonitor.ui.activities.MainActivity;

import java.text.ParseException;
import java.util.Calendar;

import static com.drnoob.datamonitor.core.Values.DATA_LIMIT;
import static com.drnoob.datamonitor.core.Values.DATA_RESET;
import static com.drnoob.datamonitor.core.Values.DATA_RESET_DAILY;
import static com.drnoob.datamonitor.core.Values.DATA_RESET_MONTHLY;
import static com.drnoob.datamonitor.core.Values.SESSION_THIS_MONTH;
import static com.drnoob.datamonitor.core.Values.SESSION_TODAY;
import static com.drnoob.datamonitor.utils.NetworkStatsHelper.formatData;
import static com.drnoob.datamonitor.utils.NetworkStatsHelper.getDeviceMobileDataUsage;
import static com.drnoob.datamonitor.utils.NetworkStatsHelper.getDeviceWifiDataUsage;

/**
 * Implementation of App Widget functionality.
 */

public class DataUsageWidget extends AppWidgetProvider {

    private static final String TAG = DataUsageWidget.class.getSimpleName();
    private static boolean isReceiverRunning = false;

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {
        Long[] mobile = null;
        Long[] wifi = null;
        String mobileData = null;
        String wifiData = null;

        try {
            mobile = getDeviceMobileDataUsage(context, SESSION_TODAY);

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

        Log.d(TAG, "updateAppWidget: " + mobileData + "  " + wifiData);

        Boolean showRemaining = PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean("remaining_data_info", true);

        if (showRemaining) {
            Float dataLimit = PreferenceManager.getDefaultSharedPreferences(context).getFloat(DATA_LIMIT, -1);
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
                        Long total = getDeviceMobileDataUsage(context, SESSION_THIS_MONTH)[2];
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

            }
            views.setViewVisibility(R.id.widget_data_usage_remaining, View.VISIBLE);
        } else {
            views.setTextViewText(R.id.widget_data_usage_remaining, "");
            views.setViewVisibility(R.id.widget_data_usage_remaining, View.GONE);
        }
        views.setTextViewText(R.id.widget_wifi_usage_remaining, "");
        Intent intent = new Intent(context, DataUsageWidget.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        int[] ids = AppWidgetManager.getInstance(context)
                .getAppWidgetIds(new ComponentName(context, DataUsageWidget.class));
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, appWidgetId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        views.setOnClickPendingIntent(R.id.widget_update, pendingIntent);

        Intent appIntent = new Intent(context, MainActivity.class);
        appIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent appPI = PendingIntent.getActivity(context, 0, appIntent, 0);
        views.setOnClickPendingIntent(R.id.widget_root, appPI);

        // Instruct the widget manager to update the widget

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
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
                PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        int elapsedTime = PreferenceManager.getDefaultSharedPreferences(context).getInt("widget_refresh_interval", 60000);
        alarmManager.setExact(AlarmManager.RTC, System.currentTimeMillis() + elapsedTime, pendingIntent);
    }

    private void startReceiver(Context context) {
        DataUsageWidget dataUsageWidget = new DataUsageWidget();
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        intentFilter.setPriority(100);
        context.getApplicationContext().registerReceiver(dataUsageWidget, intentFilter);

    }
}