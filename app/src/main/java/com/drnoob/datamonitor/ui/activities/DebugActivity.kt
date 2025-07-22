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

package com.drnoob.datamonitor.ui.activities

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.drnoob.datamonitor.Common
import com.drnoob.datamonitor.R
import com.drnoob.datamonitor.core.Values
import com.drnoob.datamonitor.databinding.ActivityDebugBinding
import com.drnoob.datamonitor.utils.CrashReporter
import com.drnoob.datamonitor.utils.LoggingService
import com.drnoob.datamonitor.utils.SharedPreferences
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.elevation.SurfaceColors
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

class DebugActivity: AppCompatActivity() {
    companion object {
        private val TAG = DebugActivity::class.simpleName
    }

    lateinit var binding: ActivityDebugBinding

    private var logCaptureToggleState = 0 // 0 -> Stopped state; 1 -> Running state

    private var loggingService: LoggingService? = null
    private var isLoggingServiceBound = MutableLiveData(false)

    private var loggingActionReceiver = LoggingActionReceiver()

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as LoggingService.LocalBinder
            loggingService = binder.getService()
            isLoggingServiceBound.value = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isLoggingServiceBound.value = false
        }
    }

    private var logFilePath: String? = null

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onCreate(savedInstanceState: Bundle?) {
        MainActivity.setTheme(this)
        Thread.setDefaultUncaughtExceptionHandler(CrashReporter(this))
        val languageCode = SharedPreferences.getUserPrefs(this).getString(Values.APP_LANGUAGE_CODE, "null")
        val countryCode = SharedPreferences.getUserPrefs(this).getString(Values.APP_COUNTRY_CODE, "")
        if (languageCode == "null") {
            Common.setLanguage(this, "en", countryCode)
        } else {
            Common.setLanguage(this, languageCode, countryCode)
        }
        super.onCreate(savedInstanceState)

        Log.d(TAG, "onCreate: ${LoggingService.isServiceRunning}")

        binding = ActivityDebugBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = getString(R.string.debug)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        binding.toolbar.setBackgroundColor(SurfaceColors.SURFACE_2.getColor(this))
        window.statusBarColor = SurfaceColors.SURFACE_2.getColor(this)

        /*
        In versions lower than O_MR1, windowLightNavigationBar cannot be applied, which results in the
        navigation bar icons being a light color (white). This limits visibility in light theme.
         */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            window.navigationBarColor = resources.getColor(R.color.background, null)
        }


        val intentFilter = IntentFilter().apply {
            addAction(LoggingService.INTENT_ACTION_START_LOGGING)
            addAction(LoggingService.INTENT_ACTION_STOP_LOGGING)
            addAction(LoggingService.INTENT_ACTION_SHARE_LOGGING_DATA)
        }

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

        binding.previousLogs.layoutManager = LinearLayoutManager(this)

        isLoggingServiceBound.observe(this) { isBound ->
            if (isBound) {
                showLoggingUi()
                loggingService?.getElapsedLoggingTime()?.observe(this) { elapsedTime ->
                    val minutes = elapsedTime / 60
                    val seconds = elapsedTime % 60
                    val loggingSince = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
                    binding.logsCaptureRuntime.text = loggingSince
                }
            }
            else {
                hideLoggingUi()
            }
        }

        binding.toggleCaptureLogs.setOnClickListener {
            if (logCaptureToggleState == 0) {
                startService(Intent(this, LoggingService::class.java))
                logCaptureToggleState = 1
            }
            else {
                sendBroadcast(Intent(LoggingService.INTENT_ACTION_STOP_LOGGING))
                logCaptureToggleState = 0
            }
        }

        binding.clearLogs.setOnClickListener {
            val logs = getExternalFilesDir("logs")?.absolutePath?.let { path -> File(path) }
            logs?.let { dir ->
                lifecycleScope.launch(Dispatchers.IO) {
                    val success = deleteContents(dir)

                    withContext(Dispatchers.Main) {
                        PreviousLogsStorage(this@DebugActivity).clear()
                        binding.previousLogsContainer.isVisible = false
                        binding.previousLogs.adapter = null

                        Toast.makeText(
                            this@DebugActivity,
                            if (success) getString(R.string.clear_logs_success) else getString(R.string.clear_logs_failure),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val logsStorage = PreviousLogsStorage(this)
        val logItems = logsStorage.getItems()
        if (logItems.isEmpty()) {
            binding.previousLogsContainer.isVisible = false
        }
        else {
            binding.previousLogsContainer.isVisible = true
        }
        binding.previousLogs.swapAdapter(PreviousLogsAdapter(logItems), true)

        if (LoggingService.isServiceRunning) {
            showLoggingUi()
            logCaptureToggleState = 1
        }
        else {
            hideLoggingUi()
            logCaptureToggleState = 0
        }

        if (LoggingService.isServiceRunning) {
            Intent(this, LoggingService::class.java).also {
                bindService(it, serviceConnection, BIND_AUTO_CREATE)
            }
        }
    }

    private fun showLoggingUi() {
        binding.logsCaptureRuntime.isVisible = true
        binding.toggleCaptureLogs.text = getString(R.string.stop_logging)
    }

    private fun hideLoggingUi() {
        binding.logsCaptureRuntime.isVisible = false
        binding.toggleCaptureLogs.text = getString(R.string.start_logging)
    }

    private fun showLoggingCompletedUi() {
        val dialog = BottomSheetDialog(this, R.style.BottomSheet)
        val view = layoutInflater.inflate(R.layout.layout_logging_complete, null)
        dialog.setContentView(view)

        dialog.show()
    }

    override fun onStop() {
        super.onStop()
        if (isLoggingServiceBound.value == true) {
            unbindService(serviceConnection)
            isLoggingServiceBound.value = false
        }
    }

    override fun onDestroy() {
        try {
            applicationContext.unregisterReceiver(loggingActionReceiver)
        }
        catch (e: Exception) {
            e.printStackTrace()
        }
        super.onDestroy()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun deleteContents(dir: File): Boolean {
        var success = true
        dir.listFiles()?.forEach { file ->
            success = success && if (file.isDirectory) {
                deleteContents(file) && file.delete()
            }
            else {
                file.delete()
            }
        }
        return success
    }

    inner class LoggingActionReceiver: BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.action?.let {
                when (it) {
                    LoggingService.INTENT_ACTION_START_LOGGING -> {
                        if (isLoggingServiceBound.value == false) {
                            Intent(this@DebugActivity, LoggingService::class.java).also { intent ->
                                bindService(intent, serviceConnection, BIND_AUTO_CREATE)
                            }
                        }
                    }
                }
                if (it.equals(LoggingService.INTENT_ACTION_STOP_LOGGING)) {
                    if (isLoggingServiceBound.value == true) {
                        unbindService(serviceConnection)
                        isLoggingServiceBound.value = false
                    }
                }

                if (it.equals(LoggingService.INTENT_ACTION_SHARE_LOGGING_DATA)) {
                    logFilePath = intent.extras?.getString("log_file")
                    val preSaved = intent.extras?.getBoolean("pre_saved", false)
//                    showLoggingCompletedUi()
//                    val intent = Intent(this@DebugActivity, LoggingCompleteActivity::class.java)
//                    intent.putExtra("log_file", logFilePath)
                    startActivity(Intent(
                        this@DebugActivity, LoggingCompleteActivity::class.java)
                        .putExtra("log_file", logFilePath)
                        .putExtra("pre_saved", preSaved)
                    )

                }
            }
        }

    }

    data class PreviousLogsItem(val fileName: String, val filePath: String)

    class PreviousLogsAdapter(private val items: List<PreviousLogsItem>):
        RecyclerView.Adapter<PreviousLogsAdapter.PreviousLogsViewHolder>() {
        class PreviousLogsViewHolder(view: View): RecyclerView.ViewHolder(view) {
            val fileName: TextView = view.findViewById(R.id.log_file_name)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PreviousLogsViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.previous_logs_item, parent, false)
            return PreviousLogsViewHolder(view)
        }

        override fun getItemCount(): Int {
            return items.size
        }

        override fun onBindViewHolder(holder: PreviousLogsViewHolder, position: Int) {
            val item = items[position]
            holder.fileName.text = item.fileName

            holder.itemView.setOnClickListener {
                val intent = Intent(LoggingService.INTENT_ACTION_SHARE_LOGGING_DATA)
                intent.putExtra("log_file", item.filePath)
                intent.putExtra("pre_saved", true)
                it.context.sendBroadcast(intent)
            }
        }
    }

    class PreviousLogsStorage(private val context: Context?) {
        private val prefs = SharedPreferences.getPreviousLogsPrefs(context)
        private val gson = Gson()
        private val key = "prev_logs"
        private val maxSize = 3

        fun addItem(item: PreviousLogsItem) {
            val items = getItems().toMutableList()
            items.add(0, item)

            if (items.size > maxSize) {
                items.removeAt(items.lastIndex)
            }

            saveItems(items)
        }

        fun updateItem(oldItem: PreviousLogsItem, newItem: PreviousLogsItem) {
            val items = getItems().toMutableList()
            val index = items.indexOf(oldItem)
            if (index == -1) return
            items.removeAt(index)
            items.add(index, newItem)

            saveItems(items)
        }

        fun getItems(): List<PreviousLogsItem> {
            val json = prefs.getString(key, null) ?: emptyList<PreviousLogsItem>().toString()
            val type = object : TypeToken<List<PreviousLogsItem>>() {}.type
            return gson.fromJson(json, type)
        }

        fun clear() {
            prefs.edit().remove(key).apply()
        }

        private fun saveItems(items: List<PreviousLogsItem>) {
            val json = gson.toJson(items)
            prefs.edit().putString(key, json).apply()
        }
    }
}