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

import static com.drnoob.datamonitor.core.Values.DATA_LIMIT;
import static com.drnoob.datamonitor.core.Values.ICON_DATA_USAGE;
import static com.drnoob.datamonitor.core.Values.ICON_NETWORK_SPEED;
import static com.drnoob.datamonitor.core.Values.NETWORK_SIGNAL_CHANNEL_ID;
import static com.drnoob.datamonitor.core.Values.NETWORK_SIGNAL_NOTIFICATION_GROUP;
import static com.drnoob.datamonitor.core.Values.NETWORK_SIGNAL_NOTIFICATION_ID;
import static com.drnoob.datamonitor.core.Values.NOTIFICATION_MOBILE_DATA;
import static com.drnoob.datamonitor.core.Values.NOTIFICATION_REFRESH_INTERVAL;
import static com.drnoob.datamonitor.core.Values.NOTIFICATION_WIFI;
import static com.drnoob.datamonitor.core.Values.SESSION_TODAY;
import static com.drnoob.datamonitor.utils.NetworkStatsHelper.formatData;
import static com.drnoob.datamonitor.utils.NetworkStatsHelper.getDeviceMobileDataUsage;
import static com.drnoob.datamonitor.utils.NetworkStatsHelper.getDeviceWifiDataUsage;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.TrafficStats;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.graphics.drawable.IconCompat;
import androidx.preference.PreferenceManager;

import com.drnoob.datamonitor.R;
import com.drnoob.datamonitor.ui.activities.MainActivity;

import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

public class CompoundNotification extends Service {
    private static final String TAG = CompoundNotification.class.getSimpleName();

    private Timer mTimer;
    private TimerTask mTimerTask;
    public Long previousTotalBytes, previousUpBytes, previousDownBytes;
    private Intent mActivityIntent;
    private PendingIntent mActivityPendingIntent;
    private NotificationCompat.Builder mBuilder;
    private CompoundNotificationReceiver compoundNotificationReceiver = new CompoundNotificationReceiver();
    private NetworkChangeMonitor mNetworkChangeMonitor;
    private boolean isNetworkConnected;
    private boolean isTimerCancelled = true;
    private boolean isTaskPaused = false;
    private boolean isNotificationReceiverRegistered = false;
    private boolean isServiceRunning;
    private String mobileDataUsage,
            wifiDataUsage,
            totalDataUsage;
    private IconCompat dataUsageIcon, networkSpeedIcon;
    
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public CompoundNotification() {
        // Empty constructor
    }

    @Override
    public void onCreate() {
        super.onCreate();

        previousDownBytes = TrafficStats.getTotalRxBytes();
        previousUpBytes = TrafficStats.getTotalTxBytes();
        previousTotalBytes = previousDownBytes + previousUpBytes;
        mActivityIntent = new Intent(Intent.ACTION_MAIN);
        mActivityIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        mActivityIntent.setComponent(new ComponentName(getPackageName(), MainActivity.class.getName()));
        mActivityPendingIntent = PendingIntent.getActivity(this, 0, mActivityIntent,
                PendingIntent.FLAG_IMMUTABLE);
        mBuilder = new NotificationCompat.Builder(this,
                NETWORK_SIGNAL_CHANNEL_ID);
        mNetworkChangeMonitor = new NetworkChangeMonitor(this);
        mNetworkChangeMonitor.startMonitor();

        totalDataUsage = getString(R.string.title_data_usage_notification,
                getString(R.string.body_data_usage_notification_loading));
        mobileDataUsage = getString(R.string.notification_mobile_data_usage,
                getString(R.string.body_data_usage_notification_loading));
        wifiDataUsage = getString(R.string.notification_wifi_data_usage,
                getString(R.string.body_data_usage_notification_loading));

        RemoteViews contentView = new RemoteViews(getPackageName(), R.layout.layout_data_usage_notification);
        RemoteViews bigContentView = new RemoteViews(getPackageName(), R.layout.layout_data_usage_notification_expanded);

        // Set placeholder values to the views.
        contentView.setTextViewText(R.id.data_usage_title,
                getString(R.string.title_data_usage_notification, getString(R.string.body_data_usage_notification_loading)));
        contentView.setTextViewText(R.id.network_speed_title, getString(R.string.network_speed_title, "0 KB/s"));

        bigContentView.setTextViewText(R.id.data_usage_title,
                getString(R.string.title_data_usage_notification, getString(R.string.body_data_usage_notification_loading)));
        bigContentView.setTextViewText(R.id.data_usage_mobile,
                getString(R.string.notification_mobile_data_usage, getString(R.string.body_data_usage_notification_loading)));
        bigContentView.setTextViewText(R.id.data_usage_wifi,
                getString(R.string.notification_wifi_data_usage, getString(R.string.body_data_usage_notification_loading)));

        bigContentView.setTextViewText(R.id.network_speed_title, getString(R.string.network_speed_title, "0 KB/s"));
        bigContentView.setTextViewText(R.id.network_speed_upload, getString(R.string.network_speed_upload, "0 KB/s"));
        bigContentView.setTextViewText(R.id.network_speed_download, getString(R.string.network_speed_download, "0 KB/s"));


        boolean showOnLockscreen = PreferenceManager.getDefaultSharedPreferences(CompoundNotification.this)
                .getBoolean("lockscreen_notification", false);

        mBuilder.setSmallIcon(R.drawable.ic_signal_kb_0); // change this
        mBuilder.setContentTitle(getString(R.string.app_name));
        mBuilder.setOngoing(true);
        mBuilder.setPriority(NotificationCompat.PRIORITY_DEFAULT);
        mBuilder.setStyle(new NotificationCompat.DecoratedCustomViewStyle());
        mBuilder.setShowWhen(false);
        if (showOnLockscreen) {
            mBuilder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        }
        else {
            mBuilder.setVisibility(NotificationCompat.VISIBILITY_SECRET);
        }
        mBuilder.setContentIntent(mActivityPendingIntent);
        mBuilder.setAutoCancel(false);
        mBuilder.setGroup(NETWORK_SIGNAL_NOTIFICATION_GROUP);
        mBuilder.setCustomContentView(contentView);
        mBuilder.setCustomBigContentView(bigContentView);

        if (isServiceRunning) {
            return;
        }
        startForeground(NETWORK_SIGNAL_NOTIFICATION_ID, mBuilder.build());
        isServiceRunning = true;

        if (isTimerCancelled) {
            mTimer = new Timer();
        }
        mTimerTask = new TimerTask() {
            int interval = 0;
            @Override
            public void run() {
                if (PreferenceManager.getDefaultSharedPreferences(CompoundNotification.this).
                        getBoolean("combine_notifications", false)) {
                    updateNotification(CompoundNotification.this, interval);
                    interval++;
                    int elapsedTime = PreferenceManager.getDefaultSharedPreferences(CompoundNotification.this)
                            .getInt(NOTIFICATION_REFRESH_INTERVAL, 60000);
                    elapsedTime /= 1000;
                    if (interval > elapsedTime) {
                        interval = 1;
                    }
                }
                else {
                    Log.d(TAG, "run: aborted");
                    try {
                        mTimerTask.cancel();
                        mTimer.cancel();
                        isTimerCancelled = true;
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                    CompoundNotification.this.stopForeground(true);
                    CompoundNotification.this.stopSelf();
                    stopService(new Intent(CompoundNotification.this, this.getClass()));
                }
            }
        };
        mTimer.scheduleAtFixedRate(mTimerTask, 0, 1000);
        if (!isNotificationReceiverRegistered && !isTaskPaused) {
            registerNetworkReceiver();
        }
    }

    @Override
    public void onDestroy() {
        // Service is stopped here
        Log.d(TAG, "onDestroy: stopped");
        mNetworkChangeMonitor.stopMonitor();
        unregisterNetworkReceiver();
        isServiceRunning = false;
        try {
            mTimerTask.cancel();
            mTimer.cancel();
            isTimerCancelled = true;
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        this.stopSelf();
        super.onDestroy();
    }

    private void updateNotification(Context context, int interval) {
        // Update notification text here
        String[] speeds;
        NotificationManagerCompat managerCompat = NotificationManagerCompat.from(context);
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        RemoteViews contentView = new RemoteViews(getPackageName(), R.layout.layout_data_usage_notification);
        RemoteViews bigContentView = new RemoteViews(getPackageName(), R.layout.layout_data_usage_notification_expanded);

        Long[] mobile, wifi;
        Boolean showPercent;

        Boolean showMobileData = PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(NOTIFICATION_MOBILE_DATA, true);
        Boolean showWifi = PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(NOTIFICATION_WIFI, true);
        Boolean autoHide = PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean("auto_hide_network_speed", false);

        String notificationIcon = PreferenceManager.getDefaultSharedPreferences(CompoundNotification.this)
                .getString("combined_notification_icon", ICON_NETWORK_SPEED);

        int elapsedTime = PreferenceManager.getDefaultSharedPreferences(CompoundNotification.this)
                .getInt(NOTIFICATION_REFRESH_INTERVAL, 60000);
        elapsedTime /= 1000;

        int percent = 0;

        if (interval == 0 || interval == elapsedTime) {
            Float dataLimit = PreferenceManager.getDefaultSharedPreferences(context).getFloat(DATA_LIMIT, -1);
            showPercent = dataLimit > 0;
            Float mobileMB;

            try {
                mobile = getDeviceMobileDataUsage(context, SESSION_TODAY, 1);
                String[] mobileData = formatData(mobile[0], mobile[1]);

                wifi = getDeviceWifiDataUsage(context, SESSION_TODAY);
                String[] wifiData = formatData(wifi[0], wifi[1]);

                long totalSent = mobile[0] + wifi[0];
                long totalReceived = mobile[1] + wifi[1];

                if (!showMobileData) {
                    totalSent = totalSent - mobile[0];
                    totalReceived = totalReceived - mobile[1];
                }
                if (!showWifi) {
                    totalSent = totalSent - wifi[0];
                    totalReceived = totalReceived - wifi[1];
                }

                String[] total = formatData(totalSent, totalReceived);
                totalDataUsage = context.getResources().getString(R.string.title_data_usage_notification, total[2]);
                mobileDataUsage = context.getResources().getString(R.string.notification_mobile_data_usage,
                        mobileData[2]);
                wifiDataUsage = context.getResources().getString(R.string.notification_wifi_data_usage,
                        wifiData[2]);

                if (showPercent) {
                    String mobileDataTotal = mobileData[2];
                    if (mobileDataTotal.contains(",")) {
                        mobileDataTotal = mobileDataTotal.replace(",", ".");
                    }
                    if (mobileDataTotal.contains("٫")) {
                        mobileDataTotal = mobileDataTotal.replace("٫", ".");
                    }
                    if (mobileDataTotal.split(" ")[1].equalsIgnoreCase("GB")) {
                        mobileMB = Float.parseFloat(mobileDataTotal.split(" ")[0]) * 1024;
                    }
                    else {
                        mobileMB = Float.parseFloat(mobileDataTotal.split(" ")[0]);
                    }
                    if (mobileMB > dataLimit) {
                        percent = 100;
                    }
                    else {
                        percent = (int) (mobileMB / dataLimit * 100);
                    }
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }

            if (showPercent) {
                String iconPrefix = "ic_data_usage_percent_";
                String iconSuffix = String.valueOf(percent);
                String iconName = iconPrefix + iconSuffix;
                int iconResID = context.getResources().getIdentifier(iconName , "drawable", context.getPackageName());
                dataUsageIcon = IconCompat.createWithResource(context, iconResID);
            }
            else {
                dataUsageIcon = IconCompat.createWithResource(context, R.drawable.ic_mobile_data);
            }

        }

        if (isNetworkConnected) {
            if (!isNotificationReceiverRegistered) {
                registerNetworkReceiver();
            }
            Long currentUpBytes = TrafficStats.getTotalTxBytes();
            Long currentDownBytes = TrafficStats.getTotalRxBytes();
            Long currentTotalBytes = currentDownBytes + currentUpBytes;

            Long upSpeed = currentUpBytes - previousUpBytes;
            Long downSpeed = currentDownBytes - previousDownBytes;
            Long totalSpeed = upSpeed + downSpeed;

            speeds = formatNetworkSpeed(upSpeed, downSpeed, totalSpeed);

            previousUpBytes = currentUpBytes;
            previousDownBytes = currentDownBytes;
            previousTotalBytes = currentTotalBytes;
        }
        else  {
            speeds = new String[]{"0 KB/s", "0 KB/s", "0 KB/s"};
        }

        String iconPrefix = "ic_signal_";
        String networkType;
        String totalSuffix = speeds[2].split(" ")[1];
        if (totalSuffix.equals("MB/s")) {
            networkType = "mb_";
        }
        else {
            networkType = "kb_";
        }
        String iconSuffix = speeds[2].split(" ")[0];
        if (iconSuffix.contains(".")) {
            iconSuffix = iconSuffix.replace(".", "_");
        }
        if (iconSuffix.contains(",")) {
            iconSuffix = iconSuffix.replace(",", "_");
        }
        if (iconSuffix.contains("٫")) {
            iconSuffix = iconSuffix.replace("٫", "_");
        }
        if (!iconSuffix.contains("_")) {
            if (networkType.equals("mb_") && Integer.parseInt(iconSuffix) > 200) {
                iconSuffix = "200_plus";
            }
        }
        String iconName = iconPrefix + networkType + iconSuffix;
        Log.d(TAG, "updateNotification: " + iconName + "  " + Arrays.toString(speeds));
        try {
            int iconResID = context.getResources().getIdentifier(iconName , "drawable", context.getPackageName());
            networkSpeedIcon = IconCompat.createWithResource(context, iconResID);
        }
        catch (Exception e) {
            networkSpeedIcon = IconCompat.createWithResource(context, R.drawable.ic_signal_kb_0);
        }
        if (mBuilder == null) {
            mBuilder = new NotificationCompat.Builder(this,
                    NETWORK_SIGNAL_CHANNEL_ID);
        }

        contentView.setTextViewText(R.id.network_speed_title, getString(R.string.network_speed_title, speeds[2]));
        contentView.setTextViewText(R.id.data_usage_title, totalDataUsage);

        bigContentView.setTextViewText(R.id.data_usage_title, totalDataUsage);
        bigContentView.setTextViewText(R.id.network_speed_title, getString(R.string.network_speed_title, speeds[2]));
        bigContentView.setTextViewText(R.id.network_speed_upload, getString(R.string.network_speed_upload, speeds[0]));
        bigContentView.setTextViewText(R.id.network_speed_download, getString(R.string.network_speed_download, speeds[1]));

        if (showMobileData && showWifi) {
            bigContentView.setTextViewText(R.id.data_usage_mobile, mobileDataUsage);
            bigContentView.setTextViewText(R.id.data_usage_wifi, wifiDataUsage);
            bigContentView.setViewVisibility(R.id.data_usage_mobile, View.VISIBLE);
            bigContentView.setViewVisibility(R.id.data_usage_wifi, View.VISIBLE);
        }
        if (!showMobileData && showWifi) {
            bigContentView.setTextViewText(R.id.data_usage_wifi, wifiDataUsage);
            bigContentView.setViewVisibility(R.id.data_usage_mobile, View.GONE);
            bigContentView.setViewVisibility(R.id.data_usage_wifi, View.VISIBLE);
        }
        if (showMobileData && !showWifi) {
            bigContentView.setTextViewText(R.id.data_usage_mobile, mobileDataUsage);
            bigContentView.setViewVisibility(R.id.data_usage_mobile, View.VISIBLE);
            bigContentView.setViewVisibility(R.id.data_usage_wifi, View.GONE);
        }

        if (!isNetworkConnected) {
            if (autoHide) {
                contentView.setViewVisibility(R.id.network_speed_title, View.GONE);
                bigContentView.setViewVisibility(R.id.network_speed_title, View.GONE);
                bigContentView.setViewVisibility(R.id.network_speed_upload, View.GONE);
                bigContentView.setViewVisibility(R.id.network_speed_download, View.GONE);
            }
        }

//        contentView.setTextViewText(R.id.data_usage_title, totalDataUsage);
//        bigContentView.setTextViewText(R.id.data_usage_title, totalDataUsage);

        boolean showOnLockscreen = PreferenceManager.getDefaultSharedPreferences(CompoundNotification.this)
                .getBoolean("lockscreen_notification", false);

        if (notificationIcon.equals(ICON_DATA_USAGE)) {
            mBuilder.setSmallIcon(dataUsageIcon);
        }
        else {
            if (!isNetworkConnected && autoHide) {
                mBuilder.setSmallIcon(dataUsageIcon);
            }
            else {
                mBuilder.setSmallIcon(networkSpeedIcon);
            }
        }
        mBuilder.setOngoing(true);
        mBuilder.setPriority(NotificationCompat.PRIORITY_DEFAULT);
        mBuilder.setContentTitle(context.getString(R.string.app_name));
        mBuilder.setStyle(new NotificationCompat.DecoratedCustomViewStyle());
        mBuilder.setContentIntent(mActivityPendingIntent);
        mBuilder.setAutoCancel(false);
        mBuilder.setShowWhen(false);
        mBuilder.setGroup(NETWORK_SIGNAL_NOTIFICATION_GROUP);
        if (showOnLockscreen) {
            mBuilder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        }
        else {
            mBuilder.setVisibility(NotificationCompat.VISIBILITY_SECRET);
        }
        mBuilder.setCustomContentView(contentView);
        mBuilder.setCustomBigContentView(bigContentView);
        managerCompat.notify(NETWORK_SIGNAL_NOTIFICATION_ID, mBuilder.build());

    }

    private void registerNetworkReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_BOOT_COMPLETED);
        intentFilter.addAction(Intent.ACTION_SCREEN_ON);
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
        intentFilter.setPriority(100);
        if (!isNotificationReceiverRegistered) {
            registerReceiver(compoundNotificationReceiver, intentFilter);
            isNotificationReceiverRegistered = true;
            Log.d(TAG, "registerNetworkReceiver: registered" );
        }
    }

    private void unregisterNetworkReceiver() {
        try {
            unregisterReceiver(compoundNotificationReceiver);
            isNotificationReceiverRegistered = false;
            Log.d(TAG, "unregisterNetworkReceive: stopped" );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public class CompoundNotificationReceiver extends BroadcastReceiver {

        public CompoundNotificationReceiver() {
            // Empty constructor
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                // Screen turned off. Cancel task
                try {
                    mTimerTask.cancel();
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
            else {
                restartService(context, true);
            }
        }
    }

    public void restartService(Context context, boolean startReceiver) {
        if (startReceiver && !isNotificationReceiverRegistered) {
            registerNetworkReceiver();
        }
        previousDownBytes = TrafficStats.getTotalRxBytes();
        previousUpBytes = TrafficStats.getTotalTxBytes();
        previousTotalBytes = previousDownBytes + previousUpBytes;
        mTimerTask = new TimerTask() {
            int interval = 0;
            @Override
            public void run() {
                if (PreferenceManager.getDefaultSharedPreferences(CompoundNotification.this).
                        getBoolean("combine_notifications", false)) {
                    updateNotification(CompoundNotification.this, interval);
                    interval++;
                    int elapsedTime = PreferenceManager.getDefaultSharedPreferences(CompoundNotification.this)
                            .getInt(NOTIFICATION_REFRESH_INTERVAL, 60000);
                    elapsedTime /= 1000;
                    if (interval > elapsedTime) {
                        interval = 1;
                    }
                }
                else {
                    try {
                        mTimerTask.cancel();
                        mTimer.cancel();
                        isTimerCancelled = true;
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                    context.stopService(new Intent(context, LiveNetworkMonitor.class));
                }
            }
        };
        if (isTimerCancelled) {
            mTimer = new Timer();
        }
        mTimer.scheduleAtFixedRate(mTimerTask, 0, 1000);
    }

    private static String[] formatNetworkSpeed(Long upSpeed, Long downSpeed, Long totalSpeed) {
        String up, down, total;
        int upSpeedKB = (int) (upSpeed / 1024);
        int downSpeedKB = (int) (downSpeed / 1024);
        int totalSpeedKB = (int) (totalSpeed / 1024);
        String upData, downData, totalData;
        Float upSpeedMB, downSpeedMB, totalSpeedMB;

        if (upSpeedKB >= 1000 && upSpeedKB < 1024) {
            upData = "1.0 MB/s";
        }
        else if (upSpeedKB >= 1024) {
            upSpeedMB = upSpeedKB / 1024f;
            if (upSpeedMB < 10) {
                upData = String.format("%.1f", upSpeedMB) + " MB/s";
            }
            else {
                upData = (int) (upSpeedKB / 1024) + " MB/s";
            }
        }
        else {
            upData = upSpeedKB + " KB/s";
        }

        if (downSpeedKB >= 1000 && downSpeedKB < 1024) {
            downData = "1.0 MB/s";
        }
        else if (downSpeedKB >= 1024) {
            downSpeedMB = downSpeedKB / 1024f;
            if (downSpeedMB < 10) {
                downData = String.format("%.1f", downSpeedMB) + " MB/s";
            }
            else {
                downData = (int) (downSpeedKB / 1024) + " MB/s";
            }
        }
        else {
            downData = downSpeedKB + " KB/s";
        }

        if (totalSpeedKB >= 1000 && totalSpeedKB < 1024) {
            totalData = "1.0 MB/s";
        }
        else if (totalSpeedKB >= 1024) {
            totalSpeedMB = totalSpeedKB / 1024f;
            if (totalSpeedMB < 10) {
                totalData = String.format("%.1f", totalSpeedMB) + " MB/s";
            }
            else {
                totalData = (int) (totalSpeedKB / 1024) + " MB/s";
            }
        }
        else {
            totalData = totalSpeedKB + " KB/s";
        }
        return new String[]{upData, downData, totalData};
    }

    private class NetworkChangeMonitor extends ConnectivityManager.NetworkCallback {
        final NetworkRequest networkRequest;

        private ConnectivityManager connectivityManager;
        private Context context;

        public NetworkChangeMonitor(Context context) {
            this.context = context;
            connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            networkRequest = new NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET)
                    .build();
        }

        public void startMonitor() {
            if (connectivityManager != null) {
                try {
                    connectivityManager.registerNetworkCallback(networkRequest, this);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
            else {
                connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                startMonitor();
            }
        }

        public void stopMonitor() {
            if (connectivityManager != null) {
                try {
                    connectivityManager.unregisterNetworkCallback(this);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
            else {
                connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                stopMonitor();
            }
        }

        @Override
        public void onAvailable(@NonNull Network network) {
            super.onAvailable(network);
            isNetworkConnected = true;
            if (isTaskPaused) {
                CompoundNotification.this.startForeground(NETWORK_SIGNAL_NOTIFICATION_ID, mBuilder.build());
                restartService(context, false);
                isTaskPaused = false;
            }
        }

        @Override
        public void onLost(@NonNull Network network) {
            super.onLost(network);
            isNetworkConnected = false;
        }
    }
}
