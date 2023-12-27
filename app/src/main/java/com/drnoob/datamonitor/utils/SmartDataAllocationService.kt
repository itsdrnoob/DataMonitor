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

import android.content.Context
import android.util.Log
import androidx.preference.PreferenceManager
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.drnoob.datamonitor.core.Values.DATA_LIMIT
import com.drnoob.datamonitor.core.Values.DATA_QUOTA
import com.drnoob.datamonitor.core.Values.DATA_QUOTA_SCHEDULED_RESET
import com.drnoob.datamonitor.core.Values.DATA_RESET
import com.drnoob.datamonitor.core.Values.DATA_RESET_CUSTOM
import com.drnoob.datamonitor.core.Values.DATA_RESET_CUSTOM_DATE_END
import com.drnoob.datamonitor.core.Values.DATA_RESET_CUSTOM_DATE_END_HOUR
import com.drnoob.datamonitor.core.Values.DATA_RESET_CUSTOM_DATE_END_MIN
import com.drnoob.datamonitor.core.Values.DATA_RESET_DAILY
import com.drnoob.datamonitor.core.Values.DATA_RESET_DATE
import com.drnoob.datamonitor.core.Values.DATA_RESET_HOUR
import com.drnoob.datamonitor.core.Values.DATA_RESET_MIN
import com.drnoob.datamonitor.core.Values.DATA_RESET_MONTHLY
import com.drnoob.datamonitor.core.Values.SESSION_CUSTOM
import com.drnoob.datamonitor.core.Values.SESSION_MONTHLY
import com.drnoob.datamonitor.core.Values.SESSION_TODAY
import com.drnoob.datamonitor.utils.NetworkStatsHelper.getDeviceMobileDataUsage
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.round
import kotlin.math.roundToInt

/**
 * Worker class to calculate and divide data plan into daily quotas.
 */
class SmartDataAllocationService(context: Context, workerParams: WorkerParameters)
    : Worker(context, workerParams) {
    companion object {
        private val TAG = SmartDataAllocationService::class.simpleName
    }

    private val preferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)

    override fun doWork(): Result {
        Log.d(TAG, "Initialising data allocation")
        val dataLimit = preferences.getFloat(DATA_LIMIT, -1f)
        val planType = preferences.getString(DATA_RESET, "null")
        val calendar = Calendar.getInstance()
        var dailyQuota: Float
        val dataUsage: Float
        val daysRemaining: Int
        val dataRemaining: Float
        val millisPerDay = 1000 * 60 * 60 * 24.toFloat()

        if (getResetTimeDelay() == null) {
            preferences.edit().putFloat(DATA_QUOTA, 0f).apply()
            return Result.failure()
        }

        if (dataLimit > 0) {
            when (planType) {
                DATA_RESET_DAILY -> {
                    dailyQuota = dataLimit
                    preferences.edit().putFloat(DATA_QUOTA, dailyQuota).apply()
                }
                DATA_RESET_MONTHLY -> {
                    val today = Calendar.getInstance()
                    var resetDay = preferences.getInt(DATA_RESET_DATE, 1)
                    val reset = Calendar.getInstance()

                    today.set(Calendar.HOUR_OF_DAY, 0)
                    today.set(Calendar.MINUTE, 0)
                    today.set(Calendar.SECOND, 0)

                    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                    resetDay = if (resetDay > daysInMonth) {
                        daysInMonth
                    }
                    else {
                        resetDay
                    }

                    if (today.get(Calendar.DAY_OF_MONTH) > resetDay)
                        reset.set(Calendar.MONTH, reset.get(Calendar.MONTH) + 1)

                    reset.set(Calendar.DAY_OF_MONTH, resetDay)
                    reset.set(Calendar.HOUR_OF_DAY, 23)
                    reset.set(Calendar.MINUTE, 59)
                    reset.set(Calendar.SECOND, 59)

                    dataUsage = round((getDeviceMobileDataUsage(applicationContext,
                        SESSION_MONTHLY, reset.get(Calendar.DAY_OF_MONTH))[2] / 1024f / 1024f) * 100) / 100

                    val usedToday = round((getDeviceMobileDataUsage(applicationContext,
                        SESSION_TODAY, -1)[2] / 1024f / 1024f) * 100) / 100

                    daysRemaining = max(((reset.timeInMillis - today.timeInMillis) / millisPerDay).roundToInt(), 1)

                    /*
                    Subtract the current day's usage from the total monthly usage
                    then subtract it from the data limit. This way the data can be properly split into quotas.
                    Then the current day's usage will count against that day's quota.
                     */
                    dataRemaining = dataLimit - (dataUsage - usedToday)
                    dailyQuota = dataRemaining / daysRemaining
                    dailyQuota = round(dailyQuota * 100) / 100
                    dailyQuota = max(dailyQuota, 0f)

                    preferences.edit().putFloat(DATA_QUOTA, dailyQuota).apply()
                }
                DATA_RESET_CUSTOM -> {
                    val today = Calendar.getInstance()

                    today.set(Calendar.HOUR_OF_DAY, 0)
                    today.set(Calendar.MINUTE, 0)
                    today.set(Calendar.SECOND, 0)

                    dataUsage = round((getDeviceMobileDataUsage(applicationContext,
                        SESSION_CUSTOM, -1)[2] / 1024f / 1024f) * 100) / 100

                    val usedToday = round((getDeviceMobileDataUsage(applicationContext,
                        SESSION_TODAY, -1)[2] / 1024f / 1024f) * 100) / 100

                    val planEndDateMillis: Long = try {
                        preferences.getLong(DATA_RESET_CUSTOM_DATE_END, -1)
                    }
                    catch (e: ClassCastException) {
                        val planEndIntValue = preferences.getInt(DATA_RESET_CUSTOM_DATE_END, -1)
                        (planEndIntValue as Number).toLong()
                    }

                    val planEndHour = preferences.getInt(DATA_RESET_CUSTOM_DATE_END_HOUR, 23)
                    val planEndMin = preferences.getInt(DATA_RESET_CUSTOM_DATE_END_MIN, 59)

                    val reset = Calendar.getInstance()
                    reset.timeInMillis = planEndDateMillis
                    reset.set(Calendar.HOUR_OF_DAY, planEndHour)
                    reset.set(Calendar.MINUTE, planEndMin)
                    reset.set(Calendar.SECOND, 59)

                    daysRemaining = max(((reset.timeInMillis - today.timeInMillis) / millisPerDay).roundToInt(), 1)

                    /*
                    Subtract the current day's usage from the total usage for this session
                    then subtract it from the data limit. This way the data can be properly split into quotas.
                    Then the current day's usage will count against that day's quota.
                     */
                    dataRemaining = dataLimit - (dataUsage - usedToday)
                    dailyQuota = dataRemaining / daysRemaining.toFloat()
                    dailyQuota = round(dailyQuota * 100) / 100
                    dailyQuota = max(dailyQuota, 0f)

                    preferences.edit().putFloat(DATA_QUOTA, dailyQuota).apply()
                }
            }

            val workManager = WorkManager.getInstance(applicationContext)

            val dataRolloverWorkRequest = PeriodicWorkRequest
                .Builder(DataRolloverHelper::class.java, 1, TimeUnit.DAYS)
                .setInitialDelay(getInitialTimeDelay(), TimeUnit.MILLISECONDS)
                .addTag("data_rollover")
                .build()

            workManager.enqueueUniquePeriodicWork(
                "data_rollover",
                ExistingPeriodicWorkPolicy.KEEP,
                dataRolloverWorkRequest
            )

            if (planType != DATA_RESET_DAILY) {
                getResetTimeDelay()?.let { time ->
                    val quotaResetWorkRequest = OneTimeWorkRequest
                        .Builder(DataRolloverHelper.QuotaRefreshHelper::class.java)
                        .addTag("quota_reset")
                        .setInitialDelay(time, TimeUnit.MILLISECONDS)
                        .build()

                    workManager.enqueueUniqueWork(
                        "quota_reset",
                        ExistingWorkPolicy.REPLACE,
                        quotaResetWorkRequest
                    )
                }
            }
        }

        return Result.success()
    }

    /**
     * Calculates the initial delay time for the data quota refresh work based on user data plan.
     *
     * @return The initial delay time in milliseconds.
     */
    private fun getInitialTimeDelay(): Long {
        val delay: Long
        val calendar = Calendar.getInstance()
        val planType = preferences.getString(DATA_RESET, "null")
        if (planType == DATA_RESET_DAILY) {
            val resetHour = preferences.getInt(DATA_RESET_HOUR, 0)
            val resetMin = preferences.getInt(DATA_RESET_MIN, 0)
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
        delay = calendar.timeInMillis - System.currentTimeMillis()
        Log.d(TAG, "getInitialTimeDelay: $delay, time: ${calendar.timeInMillis}")
        return delay
    }

    /**
     * Calculates the time delay until the scheduled data plan reset.
     *
     * @return Time delay in milliseconds. Null if reset time is in the past.
     */
    private fun getResetTimeDelay(): Long? {
        val planType = preferences.getString(DATA_RESET, "null")
        if (planType == DATA_RESET_DAILY) {
            return 0
        }
        val startDate = preferences.getInt(DATA_RESET_DATE, -1)
        val session = when (planType) {
            DATA_RESET_MONTHLY -> {
                SESSION_MONTHLY
            }
            else -> {
                SESSION_CUSTOM
            }
        }
        val planEndTime = NetworkStatsHelper.getTimePeriod(applicationContext, session, startDate)[1]
        if (planEndTime > System.currentTimeMillis()) {
            preferences.edit().putLong(DATA_QUOTA_SCHEDULED_RESET, planEndTime).apply()
            return planEndTime - System.currentTimeMillis()
        }
        return null
    }
}