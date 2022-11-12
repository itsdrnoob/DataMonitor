package com.drnoob.datamonitor.utils;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;

import com.drnoob.datamonitor.ui.activities.CrashReportActivity;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

import static com.drnoob.datamonitor.core.Values.CRASH_REPORT_KEY;

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
        errorLogsBuilder.append("----------Start of crash----------" + "\n");
        errorLogsBuilder.append("Thread name: " + thread.getName() + "\n");
        errorLogsBuilder.append("Thread stacktrace: " + Arrays.toString(thread.getStackTrace())
                .replace(",", ",\n") + "\n");
        errorLogsBuilder.append("crash message: " + throwable.getMessage() + "\n");
        errorLogsBuilder.append("\n");
        errorLogsBuilder.append("----------Crash logs----------" + "\n");
        errorLogsBuilder.append(Arrays.toString(throwable.getStackTrace()).replace(",", ",\n"));

        Intent intent = new Intent(mContext, CrashReportActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|
                        Intent.FLAG_ACTIVITY_CLEAR_TOP|
                        Intent.FLAG_ACTIVITY_CLEAR_TASK)
                .putExtra(CRASH_REPORT_KEY, errorLogsBuilder.toString());

        mContext.startActivity(intent);
        System.exit(1);
    }
}
