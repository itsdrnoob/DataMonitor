package com.drnoob.datamonitor.core.base;

import android.app.Application;

import com.google.android.material.color.DynamicColors;

public class DataMonitor extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        DynamicColors.applyToActivitiesIfAvailable(this);
    }
}
