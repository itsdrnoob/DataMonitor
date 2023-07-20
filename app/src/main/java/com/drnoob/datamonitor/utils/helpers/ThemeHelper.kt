package com.drnoob.datamonitor.utils.helpers

import android.app.Activity
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import com.drnoob.datamonitor.core.Values

fun setTheme(activity: Activity?) {
    val theme = PreferenceManager.getDefaultSharedPreferences(
        activity!!
    ).getString(Values.APP_THEME, "system")
    when (theme) {
        "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        "system" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
    }
}