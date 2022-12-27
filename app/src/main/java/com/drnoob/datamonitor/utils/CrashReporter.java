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

import static com.drnoob.datamonitor.core.Values.CRASH_REPORT_KEY;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;

import com.drnoob.datamonitor.BuildConfig;
import com.drnoob.datamonitor.ui.activities.CrashReportActivity;

import org.jetbrains.annotations.NotNull;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;

public class CrashReporter implements Thread.UncaughtExceptionHandler {
    private static final String TAG = CrashReporter.class.getSimpleName();

    private Context mContext;
    private StringBuilder errorLogsBuilder;

    public CrashReporter(Context mContext) {
        this.mContext = mContext;
    }

    @Override
    public void uncaughtException(@NonNull @NotNull Thread thread, @NonNull @NotNull Throwable throwable) {
        errorLogsBuilder = new StringBuilder();
        StringWriter logs = new StringWriter();
        throwable.printStackTrace(new PrintWriter(logs));
        errorLogsBuilder.append("----------Start of crash----------" + "\n");
        errorLogsBuilder.append("Package: " + BuildConfig.APPLICATION_ID + "\n");
        errorLogsBuilder.append("Build type: " + BuildConfig.BUILD_TYPE + "\n");
        errorLogsBuilder.append("Version code: " + BuildConfig.VERSION_CODE + "\n");
        errorLogsBuilder.append("Version: " + BuildConfig.VERSION_NAME + "\n");
        errorLogsBuilder.append("Thread name: " + thread.getName() + "\n");
        errorLogsBuilder.append("Thread stacktrace: " + Arrays.toString(thread.getStackTrace())
                .replace(",", ",\n") + "\n");
        errorLogsBuilder.append("Crash message: " + throwable.getMessage() + "\n");
        errorLogsBuilder.append("\n");
        errorLogsBuilder.append("----------Crash logs----------" + "\n");
        errorLogsBuilder.append(logs.toString().replace(",", ",\n"));

        Intent intent = new Intent(mContext, CrashReportActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|
                        Intent.FLAG_ACTIVITY_CLEAR_TOP|
                        Intent.FLAG_ACTIVITY_CLEAR_TASK)
                .putExtra(CRASH_REPORT_KEY, errorLogsBuilder.toString());

        mContext.startActivity(intent);
        System.exit(1);
    }
}
