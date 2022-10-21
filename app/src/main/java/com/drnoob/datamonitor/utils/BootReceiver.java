package com.drnoob.datamonitor.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.drnoob.datamonitor.Common;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = BootReceiver.class.getSimpleName();
    
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive: Boot complete");
        Common.refreshService(context);
    }
}
