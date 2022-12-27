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

package com.drnoob.datamonitor.utils;

import static com.drnoob.datamonitor.core.Values.DIAGNOSTICS_HISTORY_PREFERENCES;
import static com.drnoob.datamonitor.core.Values.EXCLUDE_APPS_PREFERENCES;

import android.content.Context;

public class SharedPreferences {

    public static android.content.SharedPreferences getUserPrefs(Context context) {
        android.content.SharedPreferences preferences = null;
        if (context != null) {
            preferences = context.getSharedPreferences("user_prefs",
                    Context.MODE_PRIVATE);
        }

        return preferences;
    }

    public static android.content.SharedPreferences getAppPrefs(Context context) {
        android.content.SharedPreferences preferences = null;
        if (context != null) {
            preferences = context.getSharedPreferences("app_prefs",
                    Context.MODE_PRIVATE);
        }

        return preferences;
    }

    public static android.content.SharedPreferences getAppDataLimitPrefs(Context context) {
        android.content.SharedPreferences preferences = null;
        if (context != null) {
            preferences = context.getSharedPreferences("app_data_limit_prefs",
                    Context.MODE_PRIVATE);
        }

        return preferences;
    }

    public static android.content.SharedPreferences getExcludeAppsPrefs(Context context) {
        android.content.SharedPreferences preferences = null;
        if (context != null) {
            preferences = context.getSharedPreferences(EXCLUDE_APPS_PREFERENCES,
                    Context.MODE_PRIVATE);
        }

        return preferences;
    }

    public static android.content.SharedPreferences getDiagnosticsHistoryPrefs(Context context) {
        android.content.SharedPreferences preferences = null;
        if (context != null) {
            preferences = context.getSharedPreferences(DIAGNOSTICS_HISTORY_PREFERENCES,
                    Context.MODE_PRIVATE);
        }

        return preferences;
    }
}
