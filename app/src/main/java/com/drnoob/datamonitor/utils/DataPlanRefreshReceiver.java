package com.drnoob.datamonitor.utils;

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

import java.util.Calendar;
import java.util.Date;

import static com.drnoob.datamonitor.Common.setRefreshAlarm;
import static com.drnoob.datamonitor.core.Values.ACTION_SHOW_DATA_PLAN_NOTIFICATION;
import static com.drnoob.datamonitor.core.Values.DATA_RESET;
import static com.drnoob.datamonitor.core.Values.DATA_RESET_CUSTOM;
import static com.drnoob.datamonitor.core.Values.DATA_RESET_CUSTOM_DATE_END;
import static com.drnoob.datamonitor.core.Values.DATA_RESET_CUSTOM_DATE_RESTART;
import static com.drnoob.datamonitor.core.Values.DATA_RESET_CUSTOM_DATE_START;
import static com.drnoob.datamonitor.core.Values.DEFAULT_NOTIFICATION_GROUP;
import static com.drnoob.datamonitor.core.Values.INTENT_ACTION;
import static com.drnoob.datamonitor.core.Values.OTHER_NOTIFICATION_CHANNEL_ID;
import static com.drnoob.datamonitor.core.Values.OTHER_NOTIFICATION_ID;

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
            managerCompat.notify(OTHER_NOTIFICATION_ID, builder.build());
        }
        else {
            /*
            Enables data plan to update on its own by calculating the validity of the currently set plan.
            Half-baked and not yet enabled.
             */
            Log.d(TAG, "onReceive: updating data plan validity");
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
            long start = PreferenceManager.getDefaultSharedPreferences(context)
                    .getLong(DATA_RESET_CUSTOM_DATE_START, new Date().getTime());
            long end = PreferenceManager.getDefaultSharedPreferences(context)
                    .getLong(DATA_RESET_CUSTOM_DATE_END, calendar.getTimeInMillis());
            calendar.setTimeInMillis(end);
            calendar.add(Calendar.DATE, 1);
            long restart = PreferenceManager.getDefaultSharedPreferences(context)
                    .getLong(DATA_RESET_CUSTOM_DATE_RESTART, calendar.getTimeInMillis());

            long validity = end - start;    // i day => 86400000 millis
            calendar.setTimeInMillis(restart + validity);

            PreferenceManager.getDefaultSharedPreferences(context).edit()
                    .putString(DATA_RESET, DATA_RESET_CUSTOM)
                    .putLong(DATA_RESET_CUSTOM_DATE_START, restart)
                    .putLong(DATA_RESET_CUSTOM_DATE_END, calendar.getTimeInMillis())
                    .apply();

            calendar.add(Calendar.DATE, 1);
            PreferenceManager.getDefaultSharedPreferences(context).edit()
                    .putLong(DATA_RESET_CUSTOM_DATE_RESTART, calendar.getTimeInMillis())
                    .apply();

            setRefreshAlarm(context);
        }
    }
}
