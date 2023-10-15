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

import static com.drnoob.datamonitor.Common.getPlanValidity;
import static com.drnoob.datamonitor.Common.postNotification;
import static com.drnoob.datamonitor.Common.setRefreshAlarm;
import static com.drnoob.datamonitor.core.Values.ACTION_SHOW_DATA_PLAN_NOTIFICATION;
import static com.drnoob.datamonitor.core.Values.DATA_RESET;
import static com.drnoob.datamonitor.core.Values.DATA_RESET_CUSTOM;
import static com.drnoob.datamonitor.core.Values.DATA_RESET_CUSTOM_DATE_END;
import static com.drnoob.datamonitor.core.Values.DATA_RESET_CUSTOM_DATE_END_HOUR;
import static com.drnoob.datamonitor.core.Values.DATA_RESET_CUSTOM_DATE_END_MIN;
import static com.drnoob.datamonitor.core.Values.DATA_RESET_CUSTOM_DATE_RESTART;
import static com.drnoob.datamonitor.core.Values.DATA_RESET_CUSTOM_DATE_START;
import static com.drnoob.datamonitor.core.Values.DATA_RESET_CUSTOM_DATE_START_HOUR;
import static com.drnoob.datamonitor.core.Values.DATA_RESET_CUSTOM_DATE_START_MIN;
import static com.drnoob.datamonitor.core.Values.DEFAULT_NOTIFICATION_GROUP;
import static com.drnoob.datamonitor.core.Values.INTENT_ACTION;
import static com.drnoob.datamonitor.core.Values.OTHER_NOTIFICATION_CHANNEL_ID;
import static com.drnoob.datamonitor.core.Values.OTHER_NOTIFICATION_ID;
import static com.drnoob.datamonitor.core.Values.SESSION_CUSTOM;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.preference.PreferenceManager;

import com.drnoob.datamonitor.R;
import com.drnoob.datamonitor.ui.activities.MainActivity;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class DataPlanRefreshReceiver extends BroadcastReceiver {
    private static final String TAG = DataPlanRefreshReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        /*
        Throw a notification when plan ends.
         */
        if (intent.getStringExtra(INTENT_ACTION) != null &&
                intent.getStringExtra(INTENT_ACTION).equals(ACTION_SHOW_DATA_PLAN_NOTIFICATION)) {
            Intent action = new Intent(context, MainActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 1003, action,
                    PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, OTHER_NOTIFICATION_CHANNEL_ID);
            builder.setContentTitle(context.getString(R.string.title_data_plan_expired))
                    .setContentText(context.getString(R.string.summary_data_plan_expired))
                    .setAutoCancel(true)
                    .setSmallIcon(R.drawable.ic_info)
                    .setGroup(DEFAULT_NOTIFICATION_GROUP)
                    .setContentIntent(pendingIntent);
            NotificationManagerCompat managerCompat = NotificationManagerCompat.from(context);
            postNotification(context, managerCompat, builder, OTHER_NOTIFICATION_ID);
        }
        else {
            try {
                refreshDataPlan(context);
            }
            catch (ParseException e) {
                e.printStackTrace();
            }
        }
    }

    @SuppressLint("SimpleDateFormat")
    private void refreshDataPlan(Context context) throws ParseException {
        /*
        Enables data plan to update on its own by calculating the validity of the currently set plan.
         */
        Log.d(TAG, "onReceive: updating data plan validity");
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
        Long[] currentPlan = NetworkStatsHelper.getTimePeriod(context, SESSION_CUSTOM, -1);
        long start = currentPlan[0];
        long end = currentPlan[1];

        long validity = end - start;    // 1 day => 86400000 millis
        Log.d(TAG, "refreshDataPlan: plan validity: " + validity);
        calendar.setTimeInMillis(end + validity);

        start = end; // Next plan's start time = current plan's end time
        end = calendar.getTimeInMillis();

        SimpleDateFormat hourFormat = new SimpleDateFormat("HH");
        SimpleDateFormat minFormat = new SimpleDateFormat("mm");

        int startHour = Integer.parseInt(hourFormat.format(start));
        int endHour = Integer.parseInt(hourFormat.format(end));
        int startMin = Integer.parseInt(minFormat.format(start));
        int endMin = Integer.parseInt(minFormat.format(end));

        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putString(DATA_RESET, DATA_RESET_CUSTOM)
                .putLong(DATA_RESET_CUSTOM_DATE_START, start)
                .putLong(DATA_RESET_CUSTOM_DATE_END, end)
                .putLong(DATA_RESET_CUSTOM_DATE_RESTART, end)
                .putInt(DATA_RESET_CUSTOM_DATE_START_HOUR, startHour)
                .putInt(DATA_RESET_CUSTOM_DATE_START_MIN, startMin)
                .putInt(DATA_RESET_CUSTOM_DATE_END_HOUR, endHour)
                .putInt(DATA_RESET_CUSTOM_DATE_END_MIN, endMin)
                .apply();

        Log.d(TAG, "refreshDataPlan: plan refreshed! next refresh: " + end);
        setRefreshAlarm(context);
        postDataRefreshNotification(context);
    }

    private void postDataRefreshNotification(Context context) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, OTHER_NOTIFICATION_CHANNEL_ID);
        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 111, intent,
                PendingIntent.FLAG_ONE_SHOT|PendingIntent.FLAG_IMMUTABLE);
        String endDate = getPlanValidity(SESSION_CUSTOM, context);
        builder.setContentTitle(context.getString(R.string.label_data_plan_updated))
                .setContentText(context.getString(R.string.data_plan_updated_summary, endDate))
                .setAutoCancel(true)
                .setSmallIcon(R.drawable.ic_info)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setContentIntent(pendingIntent);
        NotificationManagerCompat managerCompat = NotificationManagerCompat.from(context);
        postNotification(context, managerCompat, builder, OTHER_NOTIFICATION_ID);
    }
}
