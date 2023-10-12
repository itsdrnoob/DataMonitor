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

package com.drnoob.datamonitor.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.preference.PreferenceManager
import com.drnoob.datamonitor.Common.postAlarmPermissionDeniedNotification
import com.drnoob.datamonitor.Common.postNotification
import com.drnoob.datamonitor.R
import com.drnoob.datamonitor.core.Values
import com.drnoob.datamonitor.core.Values.DATA_QUOTA
import com.drnoob.datamonitor.core.Values.DATA_QUOTA_WARNING_SHOWN
import com.drnoob.datamonitor.core.Values.DATA_USAGE_WARNING_CHANNEL_ID
import com.drnoob.datamonitor.core.Values.DATA_USAGE_WARNING_NOTIFICATION_ID
import com.drnoob.datamonitor.core.Values.SESSION_TODAY
import com.drnoob.datamonitor.utils.NetworkStatsHelper.getDeviceMobileDataUsage
import java.util.Calendar
import kotlin.math.round

class DailyQuotaAlertReceiver: BroadcastReceiver() {
    companion object {
        private val TAG = DailyQuotaAlertReceiver::class.simpleName
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        val dataQuota = PreferenceManager.getDefaultSharedPreferences(context!!)
            .getFloat(DATA_QUOTA, -1f)
        val preference = PreferenceManager.getDefaultSharedPreferences(context)
        val selfIntent = Intent(context, this::class.java)
        val pendingIntent =
            PendingIntent.getBroadcast(context, 1000, selfIntent, PendingIntent.FLAG_IMMUTABLE)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (preference.getBoolean(DATA_QUOTA_WARNING_SHOWN, false)) {
            this.abortBroadcast
            alarmManager.cancel(pendingIntent)
        }
        else {
            if (dataQuota > 0) {
                val dataUsage = round((getDeviceMobileDataUsage(context,
                    SESSION_TODAY, -1)[2] / 1024f / 1024f) * 100) / 100
                Log.d(TAG, "onReceive: usage: $dataUsage, quota: $dataQuota")
                if (dataUsage >= dataQuota) {
                    showDataQuotaReachedAlert(context)
                    preference.edit().putBoolean(DATA_QUOTA_WARNING_SHOWN, true).apply()
                    val triggerMillis = getTriggerMillis(context)

                    val resetIntent = Intent(context, ResetDataQuotaAlert::class.java)
                    val resetPendingIntent = PendingIntent.getBroadcast(context, 69, resetIntent, PendingIntent.FLAG_IMMUTABLE)
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, resetPendingIntent)
                    Log.d(TAG, "onReceive: reset alarm set for: $triggerMillis")
                }
                else {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        System.currentTimeMillis() + 60000,
                        pendingIntent
                    )
                }
            }
        }
    }

    private fun showDataQuotaReachedAlert(context: Context) {
        val builder = NotificationCompat.Builder(context, DATA_USAGE_WARNING_CHANNEL_ID)
        builder.setContentTitle(context.getString(R.string.daily_quota_alert_title))
            .setContentText(context.getString(R.string.daily_quota_alert_body))
            .setSmallIcon(R.drawable.ic_info)
            .setAutoCancel(true)
            .priority = NotificationCompat.PRIORITY_HIGH
        val manager = NotificationManagerCompat.from(context)
        if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean("daily_quota_alert", false)) {
            postNotification(context, manager, builder, DATA_USAGE_WARNING_NOTIFICATION_ID)
        }
    }

    private fun getTriggerMillis(context: Context): Long {
        val calendar = Calendar.getInstance()
        val planType = PreferenceManager.getDefaultSharedPreferences(context)
            .getString(Values.DATA_RESET, "null")
        if (planType == Values.DATA_RESET_DAILY) {
            val resetHour = PreferenceManager.getDefaultSharedPreferences(context)
                .getInt(Values.DATA_RESET_HOUR, 0)
            val resetMin = PreferenceManager.getDefaultSharedPreferences(context)
                .getInt(Values.DATA_RESET_MIN, 0)
            calendar[Calendar.HOUR_OF_DAY] = resetHour
            calendar[Calendar.MINUTE] = resetMin
            calendar[Calendar.SECOND] = 0
        }
        else {
            calendar[Calendar.HOUR_OF_DAY] = 0
            calendar[Calendar.MINUTE] = 0
            calendar[Calendar.SECOND] = 0
        }
        if (calendar.timeInMillis < System.currentTimeMillis()) {
            calendar.add(Calendar.DATE, 1)
        }
        return calendar.timeInMillis
    }

    open class ResetDataQuotaAlert: BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d(TAG, "onReceive: Restarting quota alert")
            PreferenceManager.getDefaultSharedPreferences(context!!).edit()
                .putBoolean(DATA_QUOTA_WARNING_SHOWN, false).apply()
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val alertIntent = Intent(context, DailyQuotaAlertReceiver::class.java)
            val pendingIntent =PendingIntent.getBroadcast(context, 99, alertIntent, PendingIntent.FLAG_IMMUTABLE)
            if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean("daily_quota_alert", false)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,
                            System.currentTimeMillis() + 60000, pendingIntent)
                    }
                    else {
                        postAlarmPermissionDeniedNotification(context)
                    }
                }
                else {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,
                        System.currentTimeMillis() + 60000, pendingIntent)
                }
            }
        }

    }
}