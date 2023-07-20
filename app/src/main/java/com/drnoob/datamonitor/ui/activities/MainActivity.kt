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

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.ContentResolver
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.RemoteViews
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavDestination
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI.setupWithNavController
import androidx.preference.PreferenceManager
import com.drnoob.datamonitor.BuildConfig
import com.drnoob.datamonitor.Common
import com.drnoob.datamonitor.R
import com.drnoob.datamonitor.Widget.DataUsageWidget
import com.drnoob.datamonitor.adapters.data.DataUsageViewModel
import com.drnoob.datamonitor.adapters.data.DataUsageViewModelFactory
import com.drnoob.datamonitor.core.Values
import com.drnoob.datamonitor.core.task.DatabaseHandler
import com.drnoob.datamonitor.databinding.ActivityMainBinding
import com.drnoob.datamonitor.utils.CrashReporter
import com.drnoob.datamonitor.utils.SharedPreferences
import com.drnoob.datamonitor.utils.helpers.UsageDataHelperImpl
import com.drnoob.datamonitor.utils.helpers.setTheme
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.elevation.SurfaceColors

class MainActivity : AppCompatActivity() {

    lateinit var binding: ActivityMainBinding
    private val viewModel: DataUsageViewModel by viewModels {
        DataUsageViewModelFactory(UsageDataHelperImpl(this))
    }

    private val onBackPressedCallback: OnBackPressedCallback =
        object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val navHostFragment = supportFragmentManager.findFragmentById(R.id.main_nav_host_fragment) as NavHostFragment
                val navController = navHostFragment.navController
                if (navController.currentDestination?.id == R.id.bottom_menu_home) {
                    finish()
                } else {
                    binding.bottomNavigationView.selectedItemId = R.id.bottom_menu_home
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(this)
        Thread.setDefaultUncaughtExceptionHandler(CrashReporter(this@MainActivity))
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (!Common.isReadPhoneStateGranted(this@MainActivity)) {
                startActivity(
                    Intent(this, SetupActivity::class.java)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        .putExtra(Values.SETUP_VALUE, Values.READ_PHONE_STATE_DISABLED)
                )
                finish()
            }
        }
        super.onCreate(savedInstanceState)
        val languageCode =
            SharedPreferences.getUserPrefs(this).getString(Values.APP_LANGUAGE_CODE, "null")
        val countryCode =
            SharedPreferences.getUserPrefs(this).getString(Values.APP_COUNTRY_CODE, "")
        if (languageCode == "null") {
            Common.setLanguage(this, "en", countryCode)
        } else {
            Common.setLanguage(this, languageCode, countryCode)
        }
        try {
            Common.refreshService(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            if (Common.isUsageAccessGranted(this@MainActivity)) {
                binding = ActivityMainBinding.inflate(
                    layoutInflater
                )
                setTheme(R.style.Theme_DataMonitor)
                setContentView(binding.root)
                setSupportActionBar(binding.mainToolbar)
                binding.mainToolbar.setBackgroundColor(SurfaceColors.SURFACE_2.getColor(this))
                window.statusBarColor = SurfaceColors.SURFACE_2.getColor(this)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                    window.navigationBarColor = SurfaceColors.SURFACE_2.getColor(this)
                }
                SharedPreferences.getUserPrefs(this).edit().putBoolean(Values.SETUP_COMPLETED, true)
                    .apply()
                if (binding.bottomNavigationView.selectedItemId == R.id.bottom_menu_home) {
                    supportActionBar!!.title = getString(R.string.app_name)
                }
                val navHostFragment =
                    supportFragmentManager.findFragmentById(R.id.main_nav_host_fragment) as NavHostFragment?
                val controller = navHostFragment!!.navController
                controller.addOnDestinationChangedListener { _, navDestination, _ ->
                    changeBanner(
                        navDestination
                    )
                } // working
                setupWithNavController(binding.bottomNavigationView, controller)
                val databaseHandler = DatabaseHandler(this@MainActivity)
                if (databaseHandler.usageList == null || databaseHandler.usageList.size <= 0) {
                    viewModel.fetchApps()
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    createNotificationChannel()
                }
                value = intent.getIntExtra(Values.DATA_USAGE_VALUE, 0)
                if (value == Values.DATA_USAGE_SYSTEM) {
                    binding.bottomNavigationView.visibility = View.GONE
                    binding.bottomNavigationView.selectedItemId = R.id.bottom_menu_app_data_usage
                    supportActionBar!!.setTitle(R.string.system_data_usage)
                    supportActionBar!!.setDisplayHomeAsUpEnabled(true)
                    supportActionBar!!.setHomeAsUpIndicator(R.drawable.ic_arrow)
                    controller.navigate(R.id.system_data_usage) // >> working
                }
//                else if (!viewModel.isDataLoading) {
//                    viewModel.isDataLoading = true
//                    viewModel.loadData(Values.SESSION_TODAY, Values.TYPE_MOBILE_DATA)
//                    viewModel.isDataLoading = false
//                    viewModel.fetchApps()
//                    if (AppDataUsageFragment.appContext != null) {
//                        viewModel.callOnDataLoaded.value = true
//                    }
//                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val notificationManager = getSystemService(
                        NotificationManager::class.java
                    )
                    if (!notificationManager.areNotificationsEnabled()) {
                        requestPermissions(
                            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                            Values.REQUEST_POST_NOTIFICATIONS
                        )
                    }
                }
            } else {
                onResume()
            }
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        this@MainActivity.onBackPressedDispatcher
            .addCallback(this, onBackPressedCallback)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == Values.REQUEST_POST_NOTIFICATIONS) {
            if (grantResults.isNotEmpty() && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                MaterialAlertDialogBuilder(this@MainActivity)
                    .setTitle(R.string.label_permission_denied)
                    .setMessage(R.string.notification_permission_denied_body)
                    .setPositiveButton(R.string.action_grant) { _, _ ->
                        val intent = Intent()
                        intent.action = "android.settings.APP_NOTIFICATION_SETTINGS"
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        intent.putExtra("app_package", packageName)
                        intent.putExtra("app_uid", applicationInfo.uid)
                        intent.putExtra("android.provider.extra.APP_PACKAGE", packageName)
                        startActivity(intent)
                    }
                    .setNegativeButton(R.string.action_cancel, null)
                    .show()
            }
        }
    }

    private fun checkBatteryOptimisationState() {
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        if (powerManager.isIgnoringBatteryOptimizations(packageName)) {
            // Battery optimisation is disabled
            Log.d(TAG, "checkBatteryOptimisationState: Disabled")
        } else {
            // Battery optimisation is enabled
            Log.d(TAG, "checkBatteryOptimisationState: Enabled")
            if (SharedPreferences.getUserPrefs(this)
                    .getBoolean(Values.SHOULD_SHOW_BATTERY_OPTIMISATION_ERROR, true)
            ) {
                MaterialAlertDialogBuilder(this)
                    .setTitle(getString(R.string.label_battery_optimisation))
                    .setMessage(getString(R.string.battery_optimisation_enabled_info))
                    .setPositiveButton(getString(R.string.disable_battery_optimisation)) { dialog, _ ->
                        dialog.dismiss()
                        startActivity(
                            Intent(this@MainActivity, ContainerActivity::class.java)
                                .putExtra(
                                    Values.GENERAL_FRAGMENT_ID,
                                    Values.DISABLE_BATTERY_OPTIMISATION_FRAGMENT
                                )
                        )
                    }
                    .setNegativeButton(getString(R.string.label_do_not_show_again)) { dialog, _ ->
                        SharedPreferences.getUserPrefs(this@MainActivity).edit()
                            .putBoolean(Values.SHOULD_SHOW_BATTERY_OPTIMISATION_ERROR, false)
                            .apply()
                        dialog.dismiss()
                    }
                    .setNeutralButton(getString(R.string.action_cancel)) { dialog, _ -> dialog.dismiss() }
                    .show()
            }
        }
    }

    private fun initializeBottomNavigationViewBar() {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.main_nav_host_fragment) as NavHostFragment?
        val controller = navHostFragment!!.navController
        controller.addOnDestinationChangedListener { _, navDestination, _ ->
            changeBanner(
                navDestination
            )
        } // working

//        NavigationUI.setupWithNavController(binding.bottomNavigationView, controller);  // working
    }

    private fun changeBanner(navDestination: NavDestination) {
        val destination = navDestination.label.toString()
        if (destination.equals(getString(R.string.home), ignoreCase = true)) {
            // Home Fragment
            supportActionBar!!.title = getString(R.string.app_name)
        } else if (destination.equals(getString(R.string.setup), ignoreCase = true)) {
            // Setup Fragment
            supportActionBar!!.title = getString(R.string.setup)
        } else if (destination.equals(getString(R.string.app_data_usage), ignoreCase = true)) {
            // App data usage Fragment
            supportActionBar!!.title = getString(R.string.app_data_usage)
        } else if (destination.equals(getString(R.string.network_diagnostics), ignoreCase = true)) {
            // Network diagnostics Fragment
            supportActionBar!!.title = getString(R.string.network_diagnostics)
        } else {
            // Unknown Fragment
        }
    }

    override fun onStart() {
        super.onStart()
        verifyAppVersion()
        //        initializeBottomNavigationViewBar();
        if (!PreferenceManager.getDefaultSharedPreferences(this@MainActivity)
                .getBoolean(Values.ALARM_PERMISSION_DENIED, false)
        ) {
            val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (!alarmManager.canScheduleExactAlarms()) {
                    MaterialAlertDialogBuilder(this)
                        .setTitle(getString(R.string.error_alarm_permission_denied))
                        .setMessage(getString(R.string.error_alarm_permission_denied_dialog_summary))
                        .setCancelable(false)
                        .setPositiveButton(getString(R.string.action_grant)) { dialog, _ ->
                            val intent = Intent()
                            intent.action = Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            dialog.dismiss()
                            startActivity(intent)
                        }
                        .setNegativeButton(getString(R.string.action_cancel)) { dialog, _ -> dialog.dismiss() }
                        .setOnDismissListener {
                            PreferenceManager.getDefaultSharedPreferences(this@MainActivity).edit()
                                .putBoolean(Values.ALARM_PERMISSION_DENIED, true)
                                .apply()
                        }
                        .show()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            if (!Common.isUsageAccessGranted(this@MainActivity)) {
                startActivity(
                    Intent(this, SetupActivity::class.java)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        .putExtra(Values.SETUP_VALUE, Values.USAGE_ACCESS_DISABLED)
                )
            }
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                if (!Common.isReadPhoneStateGranted(this@MainActivity)) {
                    startActivity(
                        Intent(this, SetupActivity::class.java)
                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                            .putExtra(Values.SETUP_VALUE, Values.READ_PHONE_STATE_DISABLED)
                    )
                }
            }
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        try {
            checkBatteryOptimisationState()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        val intent = Intent(this@MainActivity, DataUsageWidget::class.java)
        intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
        val ids = AppWidgetManager.getInstance(this@MainActivity)
            .getAppWidgetIds(ComponentName(this@MainActivity, DataUsageWidget::class.java))
        AppWidgetManager.getInstance(this)
            .updateAppWidget(ids, RemoteViews(packageName, R.layout.data_usage_widget))
        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> if (value == Values.DATA_USAGE_SYSTEM) {
                value = 0
                finish()
            }

            R.id.toolbar_settings -> startActivity(
                Intent(this@MainActivity, ContainerActivity::class.java)
                    .putExtra(Values.GENERAL_FRAGMENT_ID, Values.BOTTOM_NAVBAR_ITEM_SETTINGS)
            )
        }
        return super.onOptionsItemSelected(item)
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val usageChannel = NotificationChannel(
            Values.DATA_USAGE_NOTIFICATION_CHANNEL_ID,
            Values.DATA_USAGE_NOTIFICATION_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        )
        val warningChannel = NotificationChannel(
            Values.DATA_USAGE_WARNING_CHANNEL_ID, Values.DATA_USAGE_WARNING_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        )
        val appWarningChannel = NotificationChannel(
            Values.APP_DATA_USAGE_WARNING_CHANNEL_ID, Values.APP_DATA_USAGE_WARNING_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        )
        val networkSignalChannel = NotificationChannel(
            Values.NETWORK_SIGNAL_CHANNEL_ID, Values.NETWORK_SIGNAL_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        )
        val otherChannel = NotificationChannel(
            Values.OTHER_NOTIFICATION_CHANNEL_ID, Values.OTHER_NOTIFICATION_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        )
        warningChannel.enableVibration(true)
        warningChannel.enableLights(true)
        appWarningChannel.enableVibration(true)
        appWarningChannel.enableLights(true)
        val sound =
            Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + packageName + "/" + R.raw.silent)
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .build()
        //        networkSignalChannel.setSound(sound, attributes);
        networkSignalChannel.setSound(Uri.EMPTY, null)
        networkSignalChannel.setShowBadge(false)
        networkSignalChannel.enableVibration(false)
        networkSignalChannel.enableLights(false)
        networkSignalChannel.setBypassDnd(true)
        otherChannel.enableVibration(true)
        otherChannel.enableLights(true)
        val channels: MutableList<NotificationChannel> = ArrayList()
        channels.add(usageChannel)
        channels.add(warningChannel)
        channels.add(appWarningChannel)
        channels.add(networkSignalChannel)
        channels.add(otherChannel)
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (PreferenceManager.getDefaultSharedPreferences(this@MainActivity)
                .getBoolean(Values.UPDATE_NOTIFICATION_CHANNEL, true)
        ) {
            notificationManager.deleteNotificationChannel("NetworkSignal.Notification")
            PreferenceManager.getDefaultSharedPreferences(this@MainActivity).edit()
                .putBoolean(Values.UPDATE_NOTIFICATION_CHANNEL, false)
                .apply()
        }
        notificationManager.createNotificationChannels(channels)
    }

    private fun verifyAppVersion() {
        val updateVersion = SharedPreferences.getAppPrefs(this@MainActivity)
            .getString(Values.UPDATE_VERSION, BuildConfig.VERSION_NAME)
        if (updateVersion.equals(BuildConfig.VERSION_NAME, ignoreCase = true)) {
            SharedPreferences.getAppPrefs(this@MainActivity)
                .edit().remove(Values.UPDATE_VERSION).apply()
        }
    }


    companion object {
        private val TAG = MainActivity::class.java.simpleName
        var value = 0
        var refreshAppDataUsage = false
    }
}