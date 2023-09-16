package com.drnoob.datamonitor.utils.helpers

import com.drnoob.datamonitor.adapters.data.AppDataUsageModel

interface UsageDataHelper {
    suspend fun fetchApps()
    suspend fun loadUserAppsData(session: Int, type: Int): MutableList<AppDataUsageModel?>
    suspend fun loadSystemAppsData(session: Int, type: Int): MutableList<AppDataUsageModel?>
}