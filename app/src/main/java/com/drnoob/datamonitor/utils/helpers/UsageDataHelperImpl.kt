package com.drnoob.datamonitor.utils.helpers

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.RemoteException
import androidx.preference.PreferenceManager
import com.drnoob.datamonitor.Common
import com.drnoob.datamonitor.R
import com.drnoob.datamonitor.adapters.data.AppDataUsageModel
import com.drnoob.datamonitor.core.Values
import com.drnoob.datamonitor.core.task.DatabaseHandler
import com.drnoob.datamonitor.utils.NetworkStatsHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.ParseException

class UsageDataHelperImpl(val context: Context) : UsageDataHelper {

    override suspend fun fetchApps() = withContext(Dispatchers.IO) {
        val packageManager = context.packageManager
        val allApps =
            packageManager.getInstalledApplications(PackageManager.GET_META_DATA).removeDuplicates()

        val databaseHandler = DatabaseHandler(context)
        if (allApps.size == databaseHandler.usageListSize) return@withContext

        for (applicationInfo in allApps) {
            val model = if (applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM == 1) {
                AppDataUsageModel(
                    packageManager.getApplicationLabel(applicationInfo).toString(),
                    applicationInfo.packageName,
                    applicationInfo.uid,
                    true
                )
            } else {
                // User app
                AppDataUsageModel(
                    packageManager.getApplicationLabel(applicationInfo).toString(),
                    applicationInfo.packageName,
                    applicationInfo.uid,
                    false
                )
            }
            databaseHandler.addData(
                AppDataUsageModel().apply {
                    this.appName = model.appName
                    this.packageName = model.packageName
                    this.uid = model.uid
                    this.setIsSystemApp(model.isSystemApp)
                })
        }
    }

    override suspend fun loadUserAppsData(
        session: Int,
        type: Int
    ): MutableList<AppDataUsageModel?> = withContext(Dispatchers.IO) {
        val dataList = mutableListOf<AppDataUsageModel?>()
        var totalSystemSent = 0L
        var totalSystemReceived = 0L
        val date = PreferenceManager.getDefaultSharedPreferences(context)
            .getInt(Values.DATA_RESET_DATE, 1)

        fetchApps()

        val handler = DatabaseHandler(context)
        val list = handler.usageList

        for (currentData in list) {
            if (!Common.isAppInstalled(
                    context,
                    currentData.packageName
                ) || currentData.isSystemApp
            ) continue

            val model: AppDataUsageModel?
            val (sent, received) = when (type) {
                Values.TYPE_MOBILE_DATA -> {
                    try {
                        val mobileDataUsage = NetworkStatsHelper.getAppMobileDataUsage(
                            context,
                            currentData.uid,
                            session
                        )
                        mobileDataUsage[0] to mobileDataUsage[1]
                    } catch (e: ParseException) {
                        e.printStackTrace()
                        continue
                    } catch (e: RemoteException) {
                        e.printStackTrace()
                        continue
                    }
                }

                else -> {
                    try {
                        val wifiDataUsage = NetworkStatsHelper.getAppWifiDataUsage(
                            context,
                            currentData.uid,
                            session
                        )
                        wifiDataUsage[0] to wifiDataUsage[1]
                    } catch (e: ParseException) {
                        e.printStackTrace()
                        continue
                    } catch (e: RemoteException) {
                        e.printStackTrace()
                        continue
                    }
                }
            }

            if (sent <= 0 && received <= 0) continue

            model = AppDataUsageModel().apply {
                appName = currentData.appName
                packageName = currentData.packageName
                uid = currentData.uid
                sentMobile = sent
                receivedMobile = received
                this.session = session
                this.type = type

                val total = sent + received
                val deviceTotal = when (type) {
                    Values.TYPE_MOBILE_DATA -> NetworkStatsHelper.getDeviceMobileDataUsage(
                        context,
                        session,
                        date
                    )[2]

                    else -> NetworkStatsHelper.getDeviceWifiDataUsage(context, session)[2]
                }

                progress = ((total.toDouble() / deviceTotal.toDouble()) * 100 * 2).toInt()

                totalSystemSent += sent
                totalSystemReceived += received
            }
            dataList.add(model)
        }

        return@withContext modifyDataList(
            dataList,
            session, type,
            totalSystemSent,
            totalSystemReceived
        )
    }

    override suspend fun loadSystemAppsData(
        session: Int,
        type: Int
    ): MutableList<AppDataUsageModel?> = withContext(Dispatchers.IO) {
        val dataList = mutableListOf<AppDataUsageModel?>()
        var totalSystemSent = 0L
        var totalSystemReceived = 0L
        val date = PreferenceManager.getDefaultSharedPreferences(context)
            .getInt(Values.DATA_RESET_DATE, 1)
        val handler = DatabaseHandler(context)
        val list = handler.usageList

        for (currentData in list) {
            val model: AppDataUsageModel?
            val (sent, received) = when (type) {
                Values.TYPE_MOBILE_DATA -> {
                    try {
                        val mobileDataUsage = NetworkStatsHelper.getAppMobileDataUsage(
                            context,
                            currentData.uid,
                            session
                        )
                        mobileDataUsage[0] to mobileDataUsage[1]
                    } catch (e: ParseException) {
                        e.printStackTrace()
                        continue
                    } catch (e: RemoteException) {
                        e.printStackTrace()
                        continue
                    }
                }

                else -> {
                    try {
                        val wifiDataUsage = NetworkStatsHelper.getAppWifiDataUsage(
                            context,
                            currentData.uid,
                            session
                        )
                        wifiDataUsage[0] to wifiDataUsage[1]
                    } catch (e: ParseException) {
                        e.printStackTrace()
                        continue
                    } catch (e: RemoteException) {
                        e.printStackTrace()
                        continue
                    }
                }
            }

            if (sent <= 0 && received <= 0) continue

            model = AppDataUsageModel().apply {
                appName = currentData.appName
                packageName = currentData.packageName
                uid = currentData.uid
                sentMobile = sent
                receivedMobile = received
                this.session = session
                this.type = type

                val total = sent + received
                val deviceTotal = when (type) {
                    Values.TYPE_MOBILE_DATA -> NetworkStatsHelper.getDeviceMobileDataUsage(
                        context,
                        session,
                        date
                    )[2]

                    else -> NetworkStatsHelper.getDeviceWifiDataUsage(context, session)[2]
                }

                progress = ((total.toDouble() / deviceTotal.toDouble()) * 100 * 2).toInt()

                totalSystemSent += sent
                totalSystemReceived += received
            }
            dataList.add(model)
        }

        return@withContext modifyDataList(
            dataList,
            session, type,
            totalSystemSent,
            totalSystemReceived
        )
    }

    private fun loadData(
        session: Int,
        type: Int,
        totalSystemSent: Long,
        totalSystemReceived: Long
    ): MutableList<AppDataUsageModel?> {
        val dataList = mutableListOf<AppDataUsageModel?>()
        val totalTetheringSent: Long
        val totalTetheringReceived: Long
        val totalDeletedAppsSent: Long
        val totalDeletedAppsReceived: Long
        val tetheringTotal: Long
        val deletedAppsTotal: Long
        var deviceTotal = 0L
        val date = PreferenceManager.getDefaultSharedPreferences(context)
            .getInt(Values.DATA_RESET_DATE, 1)

        var model = AppDataUsageModel().apply {
            this.appName = context.getString(R.string.label_system_apps)
            this.packageName = context.getString(R.string.package_system)
            this.sentMobile = totalSystemSent
            this.receivedMobile = totalSystemReceived
            this.session = session
            this.type = type
            val total = totalSystemSent + totalSystemReceived
            if (type == Values.TYPE_MOBILE_DATA) {
                try {
                    deviceTotal =
                        NetworkStatsHelper.getDeviceMobileDataUsage(context, session, date)[2]
                    this.progress = (total.toDouble() / deviceTotal.toDouble() * 100 * 2).toInt()
                } catch (e: ParseException) {
                    e.printStackTrace()
                } catch (e: RemoteException) {
                    e.printStackTrace()
                }
            } else {
                try {
                    deviceTotal = NetworkStatsHelper.getDeviceWifiDataUsage(context, session)[2]
                    this.progress = (total.toDouble() / deviceTotal.toDouble() * 100 * 2).toInt()
                } catch (e: ParseException) {
                    e.printStackTrace()
                } catch (e: RemoteException) {
                    e.printStackTrace()
                }
            }
        }
        if (deviceTotal > 0) dataList.add(model)
        try {
            if (type == Values.TYPE_MOBILE_DATA) {
                totalTetheringSent = NetworkStatsHelper.getTetheringDataUsage(context, session)[0]
                totalTetheringReceived =
                    NetworkStatsHelper.getTetheringDataUsage(context, session)[1]
                tetheringTotal = totalTetheringSent + totalTetheringReceived
                val tetheringProgress = tetheringTotal.toDouble() / deviceTotal.toDouble() * 100 * 2
                val tetheringProgressInt: Int = tetheringProgress.toInt()
                model = AppDataUsageModel().apply {
                    this.appName = context.getString(R.string.label_tethering)
                    this.packageName = context.getString(R.string.package_tethering)
                    this.sentMobile = totalTetheringSent
                    this.receivedMobile = totalTetheringReceived
                    this.session = session
                    this.type = type
                    this.progress = tetheringProgressInt
                }
                if (tetheringTotal > 0) dataList.add(model)
                totalDeletedAppsSent =
                    NetworkStatsHelper.getDeletedAppsMobileDataUsage(context, session)[0]
                totalDeletedAppsReceived = NetworkStatsHelper.getDeletedAppsMobileDataUsage(
                    context, session
                )[1]
            } else {
                totalDeletedAppsSent =
                    NetworkStatsHelper.getDeletedAppsWifiDataUsage(context, session)[0]
                totalDeletedAppsReceived = NetworkStatsHelper.getDeletedAppsWifiDataUsage(
                    context, session
                )[1]
            }
            deletedAppsTotal = totalDeletedAppsSent + totalDeletedAppsReceived
            val deletedProgress = deletedAppsTotal.toDouble() / deviceTotal.toDouble() * 100 * 2
            val deletedProgressInt: Int = deletedProgress.toInt()

            model = AppDataUsageModel().apply {
                this.packageName = context.getString(R.string.package_removed)
                this.appName = context.getString(R.string.label_removed)
                this.sentMobile = totalDeletedAppsSent
                this.receivedMobile = totalDeletedAppsReceived
                this.session = session
                this.type = type
                this.progress = deletedProgressInt
            }
            if (deletedAppsTotal > 0) dataList.add(model)
        } catch (e: ParseException) {
            e.printStackTrace()
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
        return dataList
    }

    private fun modifyDataList(
        dataList: MutableList<AppDataUsageModel?>,
        session: Int,
        type: Int,
        totalSystemSent: Long,
        totalSystemReceived: Long
    ): MutableList<AppDataUsageModel?> {
        dataList.addAll(loadData(session, type, totalSystemSent, totalSystemReceived))
        dataList.sortWith { o1, o2 ->
            o1!!.mobileTotal = (o1.sentMobile + o1.receivedMobile) / 1024f
            o2!!.mobileTotal = (o2.sentMobile + o2.receivedMobile) / 1024f
            o1.mobileTotal.compareTo(o2.mobileTotal)
        }
        dataList.reverse()
        dataList.sortWith { o1, o2 ->
            o1!!.mobileTotal = (o1.sentMobile + o1.receivedMobile) / 1024f
            o2!!.mobileTotal = (o2.sentMobile + o2.receivedMobile) / 1024f
            o1.mobileTotal.compareTo(o2.mobileTotal)
        }
        dataList.reverse()

        return dataList
    }

    /**
     * Removes the [ApplicationInfo] objects from list which have duplicate [ApplicationInfo.uid]
     * */
    private fun MutableList<ApplicationInfo>.removeDuplicates(): List<ApplicationInfo> {
        val uniqueUid = HashSet<Int>()
        val iterator = this.iterator()

        while (iterator.hasNext()) {
            val appInfo = iterator.next()
            if (!uniqueUid.add(appInfo.uid)) {
                iterator.remove()
            }
        }
        return this
    }
}