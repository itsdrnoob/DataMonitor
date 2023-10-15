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
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.drnoob.datamonitor.core.Values.DATA_QUOTA
import com.drnoob.datamonitor.core.Values.DATA_QUOTA_PERFORMED_RESET
import com.drnoob.datamonitor.core.Values.DATA_RESET
import com.drnoob.datamonitor.core.Values.DATA_RESET_DAILY
import com.drnoob.datamonitor.core.Values.SESSION_YESTERDAY
import kotlin.math.round

/**
 * Worker class to calculate and update data quota everyday.
 */
class DataRolloverHelper(context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {
    companion object {
        private val TAG = DataRolloverHelper::class.simpleName
    }

    override fun doWork(): Result {
        /*
        Divide plan into two first: 1. Daily, 2. Monthly and Custom
        > if daily, just check if there is any data left at the end of the day,
        If there is, add it to the data plan of the next day, if nothing is left or excess is used, do nothing

        > If monthly or custom, plan should be divided into daily quotas,
        if there is balance left at the end of the day, add it to the next day's quota.
        If excess is used, decrease it from the next days quota.
         */

        Log.d(TAG, "Updating data quota.")
        val preference = PreferenceManager.getDefaultSharedPreferences(applicationContext)

        val planType = preference.getString(DATA_RESET, "null")
        val dailyQuota = preference.getFloat(DATA_QUOTA, -1f)

        if (dailyQuota > 0) {
            when (planType) {
                DATA_RESET_DAILY -> {
                    val dataUsage = round(
                        (NetworkStatsHelper.getDeviceMobileDataUsage(
                            applicationContext,
                            SESSION_YESTERDAY, -1
                        )[2] / 1024f / 1024f) * 100
                    ) / 100 // rounds the usage value to 2 decimals
                    if (dailyQuota > dataUsage) {
                        val remainingData = dailyQuota - dataUsage
                        val newDataQuota = dailyQuota + remainingData
                        Log.d(
                            TAG,
                            "doWork: used: $dataUsage, remaining: $remainingData, new: $newDataQuota"
                        )
                        preference.edit().putFloat(DATA_QUOTA, newDataQuota).apply()
                    }
                }

                else -> {
                    val dataUsage = round(
                        (NetworkStatsHelper.getDeviceMobileDataUsage(
                            applicationContext,
                            SESSION_YESTERDAY, -1
                        )[2] / 1024f / 1024f) * 100
                    ) / 100 // rounds the usage value to 2 decimals
                    var newDataQuota = if (dailyQuota > dataUsage) {
                        val remainingData = dailyQuota - dataUsage
                        dailyQuota + remainingData
                    } else {
                        val excessUsage = dataUsage - dailyQuota
                        dailyQuota - excessUsage
                    }

                    if (newDataQuota < 0f) {
                        newDataQuota = 0f
                    }
                    Log.d(TAG, "doWork: usage: $dataUsage, new quota: $newDataQuota")
                    preference.edit().putFloat(DATA_QUOTA, newDataQuota).apply()
                }
            }


//            val dataUsage = round((NetworkStatsHelper.getDeviceMobileDataUsage(applicationContext,
//                SESSION_YESTERDAY, -1)[2] / 1024f / 1024f) * 100) / 100 // rounds the usage value to 2 decimals
//            val newDataQuota = if (dailyQuota > dataUsage) {
//                val remainingData = dailyQuota - dataUsage
//                dailyQuota + remainingData
//            }
//            else {
//                val excessUsage = dataUsage - dailyQuota
//                dailyQuota - excessUsage
//            }
//            Log.d(TAG, "doWork: usage: $dataUsage, new quota: $newDataQuota")
//            preference.edit().putFloat(DATA_QUOTA, newDataQuota).apply()
        } else {
            return Result.failure()
        }

        Log.d(TAG, "Updated data quota.")
        return Result.success()
    }

    /**
     * Worker class that resets the data quota once the current data plan iss over.
     */
    class QuotaRefreshHelper(context: Context, workerParams: WorkerParameters) :
        Worker(context, workerParams) {
        override fun doWork(): Result {
            Log.d(TAG, "doWork: Resetting data quota")

            val preference = PreferenceManager.getDefaultSharedPreferences(applicationContext)

            if (preference.getBoolean("smart_data_allocation", false)) {
                val workManager = WorkManager.getInstance(applicationContext)

                workManager.cancelUniqueWork("smart_data_allocation")
                workManager.cancelUniqueWork("data_rollover")
                workManager.cancelUniqueWork("quota_reset")

                val smartDataAllocationWorkRequest = OneTimeWorkRequest
                    .Builder(SmartDataAllocationService::class.java)
                    .build()

                workManager.enqueueUniqueWork(
                    "smart_data_allocation",
                    ExistingWorkPolicy.KEEP,
                    smartDataAllocationWorkRequest
                )

                preference.edit().putLong(DATA_QUOTA_PERFORMED_RESET, System.currentTimeMillis()).apply()

                return Result.success()
            }
            return Result.failure()
        }
    }
}