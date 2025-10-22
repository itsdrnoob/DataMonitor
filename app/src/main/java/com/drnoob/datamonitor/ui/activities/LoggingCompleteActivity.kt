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

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Parcelable
import android.util.Log
import android.view.KeyEvent
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedDispatcher
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.FileProvider
import androidx.core.view.OnReceiveContentListener
import androidx.core.view.isVisible
import androidx.preference.PreferenceFragmentCompat
import com.drnoob.datamonitor.Common
import com.drnoob.datamonitor.R
import com.drnoob.datamonitor.core.Values
import com.drnoob.datamonitor.core.Values.CAPTURED_LOGS_NOTIFICATION_ID
import com.drnoob.datamonitor.core.base.Preference
import com.drnoob.datamonitor.databinding.ActivityLoggingCompleteBinding
import com.drnoob.datamonitor.utils.CrashReporter
import com.drnoob.datamonitor.utils.LoggingService
import com.drnoob.datamonitor.utils.SharedPreferences
import com.google.android.material.elevation.SurfaceColors
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

class LoggingCompleteActivity: AppCompatActivity() {
    companion object {
        private val TAG = LoggingCompleteActivity::class.simpleName
        var logFile: File? = null
        var logs: String? = null
    }

    lateinit var binding: ActivityLoggingCompleteBinding

    private var isLogViewExpanded = false
    private var shouldLoadLogs = true
    private var shouldDeleteLogFile = true

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

        binding = ActivityLoggingCompleteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = getString(R.string.logging_complete)
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

        val notificationManager = NotificationManagerCompat.from(this@LoggingCompleteActivity)
        notificationManager.cancel(CAPTURED_LOGS_NOTIFICATION_ID)

        var logFilePath = intent.getStringExtra("log_file")
        Log.e(TAG, "onCreate:, $logFilePath")
        logFile = logFilePath?.let { File(it) }

        val preSaved = intent.getBooleanExtra("pre_saved", false)
        if (preSaved) {
            shouldDeleteLogFile = false
            binding.saveLogFile.isVisible = false
        }


        logFile?.let {
            binding.logFileNameInput.hint = it.name.split(".")[0] // show the first half without .log extension
        }

        val logsStorage = DebugActivity.PreviousLogsStorage(this)

        binding.logFileName.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_NEXT ||
                (event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(v.windowToken, 0)
                binding.logFileNameInput.clearFocus()

                val newLogName = getLogFileName()
                val newFile = File(getExternalFilesDir("logs"), newLogName)

                logFile?.renameTo(newFile)

                if (preSaved) {
                    logsStorage.updateItem(
                        DebugActivity.PreviousLogsItem(logFile!!.name, logFilePath!!),
                        DebugActivity.PreviousLogsItem(newLogName, newFile.absolutePath)
                    )
                }

                logFile = newFile
                logFilePath = newFile.absolutePath

                binding.logFileNameInput.hint = newLogName.split(".")[0]

                true
            }
            else {
                false
            }
        }


        val targetHeight = resources.getDimensionPixelSize(R.dimen.logs_dropdown_fixed_height) // e.g. 200dp
        binding.logViewHeader.setOnClickListener {
            if (binding.logsContent.visibility == View.GONE) {
                expand(binding.logsContent, targetHeight) {
                    loadLogs(binding.logsContentText, logFile)
                }
            } else {
                collapse(binding.logsContent)
            }
        }

        binding.copyLogs.setOnClickListener {
            copyLogs()
        }

        binding.saveLogFile.setOnClickListener {
            shouldDeleteLogFile = false

            logsStorage.addItem(DebugActivity.PreviousLogsItem(
                logFile?.name ?: getString(R.string.label_unknown),
                logFilePath ?: getString(R.string.label_unknown)
            ))

            onBackPressedDispatcher.onBackPressed()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (shouldDeleteLogFile) logFile?.delete()
    }

    fun expand(view: View, targetHeight: Int, onExpanded: () -> Unit) {
        view.measure(View.MeasureSpec.makeMeasureSpec(view.width, View.MeasureSpec.EXACTLY), View.MeasureSpec.UNSPECIFIED)
        view.layoutParams.height = 0
        view.visibility = View.VISIBLE

        val animator = ValueAnimator.ofInt(0, targetHeight)
        animator.addUpdateListener {
            view.layoutParams.height = it.animatedValue as Int
            view.requestLayout()
        }
        animator.duration = 250
        animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                onExpanded()
                isLogViewExpanded = true
                Handler(Looper.getMainLooper()).postDelayed( {
                    binding.logsContentText.isVisible = true
                }, 30)
            }
        })
        animator.start()

        binding.logsDropdownArrow.animate()
            .rotation(180f)
            .setDuration(250)
            .start()
    }

    fun collapse(view: View) {
        isLogViewExpanded = false
        val initialHeight = view.measuredHeight

        val animator = ValueAnimator.ofInt(initialHeight, 0)
        animator.addUpdateListener {
            view.layoutParams.height = it.animatedValue as Int
            view.requestLayout()
        }
        animator.duration = 250
        animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                view.visibility = View.GONE
                binding.logsContentText.isVisible = false
            }
        })
        animator.start()

        binding.logsDropdownArrow.animate()
            .rotation(0f)
            .setDuration(250)
            .start()
    }

    private fun loadLogs(textView: TextView, file: File?) {
        if (!shouldLoadLogs) return

        binding.logsContentText.text = ""
        file?.let {
            CoroutineScope(Dispatchers.IO).launch {
                it.bufferedReader().useLines { lines ->
                    for (line in lines) {
                        delay(10)
//                        if (isLogViewExpanded) {
//                            withContext(Dispatchers.Main) {
//                                textView.append(line + "\n")
//                            }
//                        }
                        withContext(Dispatchers.Main) {
                            textView.append(line + "\n")
                        }
                        shouldLoadLogs = false
                    }
                }
            }
        }
    }

    private fun copyLogs() {
        logFile?.let {
            val logs = it.bufferedReader().use { file -> file.readText() }
            val clipboardManager = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            val clipData = ClipData.newPlainText("datamonitor-debug-logs", logs)
            clipboardManager.setPrimaryClip(clipData)
            Toast.makeText(
                this,
                getString(R.string.label_crash_logs_copied),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun getLogFileName(): String {
        var newLogName = binding.logFileName.text.toString().trim()
        if (newLogName.isBlank()) {
            newLogName = logFile?.name ?: "logs.log"
        }
        else {
            newLogName += ".log"
        }
        return newLogName.replace(" ", "-")
    }

    class SendReportFragment : PreferenceFragmentCompat() {
        var github: Preference? = null
        var telegram: Preference? = null
        var mail: Preference? = null
        var telegramClients: MutableList<String> = ArrayList()

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.crash_report_action, rootKey)

            telegram = findPreference("report_telegram") as Preference?
            github = findPreference("report_github") as Preference?
            mail = findPreference("report_mail") as Preference?

            mail?.isVisible = false // data parcel size limit

            telegramClients.add("org.telegram.messenger")
            telegramClients.add("org.telegram.plus")
            telegramClients.add("tw.nekomimi.nekogram")
            telegramClients.add("org.thunderdog.challegram")
            telegramClients.add("org.telegram.mdgram")
            telegramClients.add("org.telegram.mdgramyou")
            telegramClients.add("one.gram.onegram")
            telegramClients.add("nekox.messenger")
            telegramClients.add("ir.ilmili.telegraph")
            telegramClients.add("com.xplus.messenger")
            telegramClients.add("org.telegram.BifToGram")
            telegramClients.add("org.vidogram.messenger")

            telegram!!.onPreferenceClickListener = androidx.preference.Preference.OnPreferenceClickListener {
                try {
                    if (logFile == null) {
                        val snackbar = Snackbar.make(
                            requireView(), getString(R.string.error_no_crash_logs),
                            Snackbar.LENGTH_LONG
                        )
                        Common.dismissOnClick(snackbar)
                        snackbar.show()
                        return@OnPreferenceClickListener false
                    }
                    try {
                        val telegramIntents: MutableList<Intent> = ArrayList()
                        val telegramIntent = Intent(Intent.ACTION_SEND)
                        telegramIntent.setType("application/pdf")
                        val resInfo = requireContext().applicationContext
                            .packageManager.queryIntentActivities(telegramIntent, 0)
                        if (resInfo.isNotEmpty()) {
                            for (resolveInfo in resInfo) {
                                val packageName = resolveInfo.activityInfo.packageName
                                val targetedIntent = Intent(Intent.ACTION_SEND)
                                targetedIntent.setType("application/pdf")
                                targetedIntent.setPackage(packageName)
                                targetedIntent.putExtra(
                                    Intent.EXTRA_STREAM,
                                    FileProvider.getUriForFile(
                                        requireContext(),
                                        requireContext().applicationContext.packageName + ".provider",
                                        logFile!!
                                    )
                                )
                                targetedIntent.putExtra(
                                    Intent.EXTRA_TEXT,
                                    requireContext().getString(R.string.crash_logs_extra_text)
                                )
                                if (telegramClients.contains(packageName)) {
                                    telegramIntents.add(targetedIntent)
                                }
                            }
                            val chooserIntent = Intent.createChooser(
                                telegramIntents.removeAt(0),
                                getString(R.string.label_select_app)
                            )
                            chooserIntent.putExtra(
                                Intent.EXTRA_INITIAL_INTENTS,
                                telegramIntents.toTypedArray<Parcelable>()
                            )
                            startActivity(chooserIntent)
                            Toast.makeText(
                                context, getString(R.string.label_crash_logs_share_chat),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    } catch (e: Exception) {
                        val snackbar = Snackbar.make(
                            requireView(), getString(R.string.error_unknown_telegram_client),
                            Snackbar.LENGTH_LONG
                        )
                        Common.dismissOnClick(snackbar)
                        snackbar.show()
                        e.printStackTrace()
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                }
                false
            }

            github!!.onPreferenceClickListener = androidx.preference.Preference.OnPreferenceClickListener {
                if (logFile == null) {
                    val snackbar = Snackbar.make(
                        requireView(), getString(R.string.error_no_crash_logs),
                        Snackbar.LENGTH_LONG
                    )
                    Common.dismissOnClick(snackbar)
                    snackbar.show()
                    return@OnPreferenceClickListener false
                }
                val githubIntent = Intent(Intent.ACTION_VIEW)
                githubIntent.setData(Uri.parse(getString(R.string.github_new_issue)))
                startActivity(githubIntent)
                copyLogs()
                false
            }
        }

        private fun copyLogs() {
            logFile?.let {
                val logs = it.bufferedReader().use { file -> file.readText() }
                val clipboardManager = context?.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                val clipData = ClipData.newPlainText("datamonitor-debug-logs", logs)
                clipboardManager.setPrimaryClip(clipData)
                Toast.makeText(
                    context,
                    getString(R.string.label_crash_logs_copied),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

}