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

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.drnoob.datamonitor.Common.postNotification
import com.drnoob.datamonitor.R
import com.drnoob.datamonitor.core.Values
import com.drnoob.datamonitor.ui.activities.DebugActivity
import com.drnoob.datamonitor.ui.activities.LoggingCompleteActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Timer
import java.util.TimerTask

class LoggingService: Service() {
    companion object {
        var isServiceRunning = false
        private const val DEBUG_ACTIVITY_REQUEST_CODE = 4001
        private const val STOP_LOGGING_REQUEST_CODE = 4002

        const val INTENT_ACTION_STOP_LOGGING = "com.drnoob.datamonitor.LoggingService.ACTION_STOP_LOGGING"
        const val INTENT_ACTION_START_LOGGING = "com.drnoob.datamonitor.LoggingService.ACTION_START_LOGGING"
        const val INTENT_ACTION_SHARE_LOGGING_DATA = "com.drnoob.datamonitor.LoggingService.ACTION_SHARE_DATA"
    }

    private val binder = LocalBinder()
    private val loggingActionReceiver = LoggingActionReceiver()

    private lateinit var logFile: File

    private lateinit var timer: Timer
    private lateinit var timerTask: TimerTask
    private val elapsedLoggingTime = MutableLiveData(0)

    private lateinit var logOutputStream: FileOutputStream
    private lateinit var readLogs: Job

    private lateinit var stopActionIntent: PendingIntent

    fun getElapsedLoggingTime(): LiveData<Int> = elapsedLoggingTime

    override fun onBind(p0: Intent?): IBinder? {
        return binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        return super.onUnbind(intent)
    }

    private val elapsedTimeObserver = Observer<Int> { elapsedTime ->
        val minutes = elapsedTime / 60
        val seconds = elapsedTime % 60
        val loggingSince = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)

        val builder = NotificationCompat.Builder(this@LoggingService, Values.LOGGING_NOTIFICATION_CHANNEL_ID).apply {
            setSmallIcon(R.drawable.ic_debug_menu)
            setOngoing(true)
            setAutoCancel(false)
            setContentTitle(getString(R.string.label_capturing_logs))
            setContentText(getString(R.string.capturing_logs_body, loggingSince))
            setOnlyAlertOnce(true)
            setPriority(NotificationCompat.PRIORITY_LOW)
            setSound(null)
            addAction(
                R.drawable.ic_debug_menu,
                getString(R.string.stop_logging),
                stopActionIntent
            )
            setContentIntent(PendingIntent.getActivity(
                this@LoggingService,
                DEBUG_ACTIVITY_REQUEST_CODE,
                Intent(this@LoggingService, DebugActivity::class.java),
                (PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            ))
        }

        val notificationManager = NotificationManagerCompat.from(this@LoggingService)
        postNotification(this@LoggingService, notificationManager, builder, Values.LOGGING_NOTIFICATION_ID)
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onCreate() {
        super.onCreate()

        val intentFilter = IntentFilter(INTENT_ACTION_STOP_LOGGING)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            applicationContext.registerReceiver(
                loggingActionReceiver,
                intentFilter,
                RECEIVER_EXPORTED
            )
        }
        else {
            applicationContext.registerReceiver(loggingActionReceiver, intentFilter)
        }

        val broadcast = Intent(INTENT_ACTION_STOP_LOGGING)
        stopActionIntent = PendingIntent.getBroadcast(this, STOP_LOGGING_REQUEST_CODE, broadcast, PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(this, Values.LOGGING_NOTIFICATION_CHANNEL_ID).apply {
            setSmallIcon(R.drawable.ic_debug_menu)
            setOngoing(true)
            setAutoCancel(false)
            setContentTitle(getString(R.string.label_capturing_logs))
            setContentText(getString(R.string.capturing_logs_body, "00:00"))
            setOnlyAlertOnce(true)
            setPriority(NotificationCompat.PRIORITY_LOW)
            setSound(null)
            addAction(
                R.drawable.ic_debug_menu,
                getString(R.string.stop_logging),
                stopActionIntent
            )
            setContentIntent(PendingIntent.getActivity(
                this@LoggingService,
                DEBUG_ACTIVITY_REQUEST_CODE,
                Intent(this@LoggingService, DebugActivity::class.java),
                (PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            ))
        }

        val notificationManager = NotificationManagerCompat.from(this)
        postNotification(this, notificationManager, builder, Values.LOGGING_NOTIFICATION_ID)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    Values.LOGGING_NOTIFICATION_ID,
                    builder.build(),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            }
            else {
                startForeground(
                    Values.LOGGING_NOTIFICATION_ID,
                    builder.build()
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        startTimer()
        startLogging()
        isServiceRunning = true

        elapsedLoggingTime.observeForever(elapsedTimeObserver)
    }

    private fun startTimer() {
        timer = Timer()
        timerTask = object : TimerTask() {
            override fun run() {
                elapsedLoggingTime.postValue(elapsedLoggingTime.value?.plus(1))
            }
        }

        timer.schedule(timerTask, 1000, 1000)
    }

    private fun stopTimer() {
        timer.cancel()
        elapsedLoggingTime.removeObserver(elapsedTimeObserver)
    }

    private fun startLogging() {
        val simpleDateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val logFileName = "${simpleDateFormat.format(Date())}.log"

        logFile = File(getExternalFilesDir("logs"), logFileName)
        logFile.createNewFile()

        logOutputStream = FileOutputStream(logFile, true)

        Runtime.getRuntime().exec("logcat -c").waitFor() // capture fresh logs

        readLogs = CoroutineScope(Dispatchers.IO).launch {
            Runtime.getRuntime().exec("logcat")
                .inputStream
                .bufferedReader()
                .useLines { lines ->
                    lines.forEach { line ->
                        logOutputStream.write(line.toByteArray())
                        logOutputStream.write("\n".toByteArray())
                    }

                }
        }

        readLogs.start()
        sendBroadcast(Intent(INTENT_ACTION_START_LOGGING))
    }

    private fun stopLogging() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                readLogs.cancelAndJoin()
                logOutputStream.close()
            }
            catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun notifyLoggingCompletion() {
        val builder = NotificationCompat.Builder(this@LoggingService, Values.CAPTURED_LOGS_NOTIFICATION_CHANNEL_ID).apply {
            setSmallIcon(R.drawable.ic_debug_menu)
            setAutoCancel(true)
            setContentTitle(getString(R.string.label_logs_captured))
            setContentText(getString(R.string.logging_complete_body))
            setPriority(NotificationCompat.PRIORITY_HIGH)
            setContentIntent(PendingIntent.getActivity(
                this@LoggingService,
                DEBUG_ACTIVITY_REQUEST_CODE,
                Intent(this@LoggingService, LoggingCompleteActivity::class.java).apply {
                    putExtra("log_file", logFile.absolutePath)
                },
                (PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            ))
        }

        val notificationManager = NotificationManagerCompat.from(this@LoggingService)
        postNotification(this@LoggingService, notificationManager, builder, Values.CAPTURED_LOGS_NOTIFICATION_ID)
    }

    override fun onDestroy() {
        try {
            applicationContext.unregisterReceiver(loggingActionReceiver)
        }
        catch (e: Exception) {
            e.printStackTrace()
        }

        stopTimer()
        stopLogging()
        notifyLoggingCompletion()

        val intent = Intent(INTENT_ACTION_SHARE_LOGGING_DATA)
        intent.putExtra("log_file", logFile.absolutePath)
        sendBroadcast(intent)

        isServiceRunning = false

        super.onDestroy()
    }

    inner class LocalBinder: Binder() {
        fun getService(): LoggingService = this@LoggingService
    }

    inner class LoggingActionReceiver: BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.action?.let {
                if (it.equals(INTENT_ACTION_STOP_LOGGING)) {
                    context?.stopService(Intent(context, LoggingService::class.java))
                }
            }
        }

    }

}