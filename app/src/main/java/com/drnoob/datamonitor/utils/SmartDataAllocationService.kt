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
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.drnoob.datamonitor.core.Values.*
import com.drnoob.datamonitor.utils.NetworkStatsHelper.getDeviceMobileDataUsage
import java.util.Calendar
import java.util.concurrent.TimeUnit
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

    override fun doWork(): Result {
        Log.d(TAG, "Initialising data allocation")
        val preference = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        val dataLimit = preference.getFloat(DATA_LIMIT, -1f)
        val planType = preference.getString(DATA_RESET, "null")
        val calendar = Calendar.getInstance()
        var dailyQuota: Float
        val dataUsage: Float
        val daysRemaining: Int
        val dataRemaining: Float
        val millisPerDay = 1000 * 60 * 60 * 24.toFloat()
        if (dataLimit > 0) {
            when (planType) {
                DATA_RESET_DAILY -> {
                    dailyQuota = dataLimit
                    preference.edit().putFloat(DATA_QUOTA, dailyQuota).apply()
                }
                DATA_RESET_MONTHLY -> {
                    val today = Calendar.getInstance()
                    var resetDay = preference.getInt(DATA_RESET_DATE, 1)
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

                    daysRemaining = ((reset.timeInMillis - today.timeInMillis) / millisPerDay).roundToInt()

                    dataRemaining = dataLimit - dataUsage
                    dailyQuota = dataRemaining / daysRemaining
                    dailyQuota = round(dailyQuota * 100) / 100

                    preference.edit().putFloat(DATA_QUOTA, dailyQuota).apply()
                }
                DATA_RESET_CUSTOM -> {
                    val today = Calendar.getInstance()

                    today.set(Calendar.HOUR_OF_DAY, 0)
                    today.set(Calendar.MINUTE, 0)
                    today.set(Calendar.SECOND, 0)

                    dataUsage = round((getDeviceMobileDataUsage(applicationContext,
                        SESSION_CUSTOM, -1)[2] / 1024f / 1024f) * 100) / 100
                    val planEndDateMillis: Long = try {
                        preference.getLong(DATA_RESET_CUSTOM_DATE_END, -1)
                    } catch (e: ClassCastException) {
                        val planEndIntValue = preference.getInt(DATA_RESET_CUSTOM_DATE_END, -1)
                        (planEndIntValue as Number).toLong()
                    }
//                    daysRemaining = TimeUnit.MILLISECONDS.toDays(planEndDateMillis - System.currentTimeMillis()).toInt()

                    daysRemaining = ((planEndDateMillis - today.timeInMillis) / millisPerDay).roundToInt()

                    dataRemaining = dataLimit - dataUsage
                    dailyQuota = dataRemaining / daysRemaining.toFloat()
                    dailyQuota = round(dailyQuota * 100) / 100
                    Log.d(TAG, "doWork: quota: $dailyQuota")

                    preference.edit().putFloat(DATA_QUOTA, dailyQuota).apply()
                }
            }

            val dataRolloverWorkRequest = PeriodicWorkRequest
                .Builder(DataRolloverHelper::class.java, 1, TimeUnit.DAYS)
                .setInitialDelay(getInitialTimeDelay(), TimeUnit.MILLISECONDS)
                .addTag("data_rollover")
                .build()
            WorkManager.getInstance(applicationContext)
                .enqueueUniquePeriodicWork(
                    "data_rollover",
                    ExistingPeriodicWorkPolicy.KEEP,
                    dataRolloverWorkRequest
                )
        }

        return Result.success()
    }

    private fun getInitialTimeDelay(): Long {
        val delay: Long
        val calendar = Calendar.getInstance()
        val planType = PreferenceManager.getDefaultSharedPreferences(applicationContext)
            .getString(DATA_RESET, "null")
        if (planType == DATA_RESET_DAILY) {
            val resetHour = PreferenceManager.getDefaultSharedPreferences(applicationContext)
                .getInt(DATA_RESET_HOUR, 0)
            val resetMin = PreferenceManager.getDefaultSharedPreferences(applicationContext)
                .getInt(DATA_RESET_MIN, 0)
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
}