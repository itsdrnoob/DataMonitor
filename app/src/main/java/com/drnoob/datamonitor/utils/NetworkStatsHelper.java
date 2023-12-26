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

import static android.app.usage.NetworkStats.Bucket.UID_REMOVED;
import static android.app.usage.NetworkStats.Bucket.UID_TETHERING;
import static com.drnoob.datamonitor.core.Values.DATA_RESET;
import static com.drnoob.datamonitor.core.Values.DATA_RESET_CUSTOM_DATE_END;
import static com.drnoob.datamonitor.core.Values.DATA_RESET_CUSTOM_DATE_END_HOUR;
import static com.drnoob.datamonitor.core.Values.DATA_RESET_CUSTOM_DATE_END_MIN;
import static com.drnoob.datamonitor.core.Values.DATA_RESET_CUSTOM_DATE_START;
import static com.drnoob.datamonitor.core.Values.DATA_RESET_CUSTOM_DATE_START_HOUR;
import static com.drnoob.datamonitor.core.Values.DATA_RESET_CUSTOM_DATE_START_MIN;
import static com.drnoob.datamonitor.core.Values.DATA_RESET_DAILY;
import static com.drnoob.datamonitor.core.Values.DATA_RESET_DATE;
import static com.drnoob.datamonitor.core.Values.EXCLUDE_APPS_LIST;
import static com.drnoob.datamonitor.core.Values.SESSION_ALL_TIME;
import static com.drnoob.datamonitor.core.Values.SESSION_CUSTOM;
import static com.drnoob.datamonitor.core.Values.SESSION_CUSTOM_FILTER;
import static com.drnoob.datamonitor.core.Values.SESSION_LAST_MONTH;
import static com.drnoob.datamonitor.core.Values.SESSION_MONTHLY;
import static com.drnoob.datamonitor.core.Values.SESSION_THIS_MONTH;
import static com.drnoob.datamonitor.core.Values.SESSION_THIS_YEAR;
import static com.drnoob.datamonitor.core.Values.SESSION_TODAY;
import static com.drnoob.datamonitor.core.Values.SESSION_YESTERDAY;

import android.annotation.SuppressLint;
import android.app.usage.NetworkStats;
import android.app.usage.NetworkStatsManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.RemoteException;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.util.Pair;
import androidx.preference.PreferenceManager;

import com.drnoob.datamonitor.R;
import com.drnoob.datamonitor.adapters.data.AppModel;
import com.drnoob.datamonitor.adapters.data.OverviewModel;
import com.drnoob.datamonitor.ui.fragments.AppDataUsageFragment;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;

import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/* Created by Dr.NooB on 23/09/2021 */

public class NetworkStatsHelper {
    private static final String TAG = NetworkStatsHelper.class.getSimpleName();
    private static Gson gson = new Gson();
    private static Type type = new TypeToken<List<AppModel>>() {}.getType();

    public static String getSubscriberId(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            String subscriberId = "";
            try {
                subscriberId =  telephonyManager.getSubscriberId();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            return subscriberId;
        } else {
            return null;
        }
    }

    public static Long[] getDeviceWifiDataUsage(Context context, int session) throws ParseException, RemoteException {
        Long[] data;
        Long resetTimeMillis = getTimePeriod(context, session, 1)[0];
        Long endTimeMillis = getTimePeriod(context, session, 1)[1];
        Long sent, received, total;

        Long excludedSent = 0L,
                excludedReceived = 0L,
                excludedTotal = 0L;

        String jsonData = SharedPreferences.getExcludeAppsPrefs(context)
                .getString(EXCLUDE_APPS_LIST, null);
        List<AppModel> excludedAppsList = new ArrayList<>();
        if (jsonData != null) {
            excludedAppsList.addAll(gson.fromJson(jsonData, type));
        }
        for (AppModel app : excludedAppsList) {
            int uid = 0;
            try {
                uid = context.getPackageManager().getApplicationInfo(app.getPackageName(), 0).uid;
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
            Long[] wifi = getAppWifiDataUsage(context, uid, session);

            excludedSent += wifi[0];
            excludedReceived += wifi[1];
            excludedTotal += wifi[2];
        }

        NetworkStatsManager networkStatsManager = (NetworkStatsManager) context.getSystemService(Context.NETWORK_STATS_SERVICE);
        NetworkStats.Bucket bucket = new NetworkStats.Bucket();

        bucket = networkStatsManager.querySummaryForDevice(ConnectivityManager.TYPE_WIFI,
                getSubscriberId(context),
                resetTimeMillis,
                endTimeMillis);

        received = bucket.getRxBytes();
        sent = bucket.getTxBytes();
        total = sent + received;

        sent = sent - excludedSent;
        received = received - excludedReceived;
        total = total - excludedTotal;

        data = new Long[]{sent, received, total};
        return data;
    }

    public static Long[] getDeviceMobileDataUsage(Context context, int session, @Nullable int startDate) throws ParseException, RemoteException {
        NetworkStatsManager networkStatsManager = (NetworkStatsManager) context.getSystemService(Context.NETWORK_STATS_SERVICE);
        NetworkStats.Bucket bucket = new NetworkStats.Bucket();

        Long resetTimeMillis = getTimePeriod(context, session, startDate)[0];
        Long endTimeMillis = getTimePeriod(context, session, startDate)[1];

        Long sent = 0L,
                received = 0L,
                total = 0L;
        Long excludedSent = 0L,
                excludedReceived = 0L,
                excludedTotal = 0L;

        String jsonData = SharedPreferences.getExcludeAppsPrefs(context)
                .getString(EXCLUDE_APPS_LIST, null);
        List<AppModel> excludedAppsList = new ArrayList<>();
        if (jsonData != null) {
            excludedAppsList.addAll(gson.fromJson(jsonData, type));
        }
        for (AppModel app : excludedAppsList) {
            int uid = 0;
            try {
                uid = context.getPackageManager().getApplicationInfo(app.getPackageName(), 0).uid;
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
            Long[] mobile = getAppMobileDataUsage(context, uid, session);

            excludedSent += mobile[0];
            excludedReceived += mobile[1];
            excludedTotal += mobile[2];
        }

        bucket = networkStatsManager.querySummaryForDevice(ConnectivityManager.TYPE_MOBILE,
                getSubscriberId(context),
                resetTimeMillis,
                endTimeMillis);

//        NetworkStats networkStats = networkStatsManager.querySummary(ConnectivityManager.TYPE_MOBILE,
//                getSubscriberId(context),
//                resetTimeMillis,
//                endTimeMillis);

//        Long rxBytes = 0L;
//        Long txBytes = 0L;
//
//        do {
//            networkStats.getNextBucket(bucket);
//            rxBytes += bucket.getRxBytes();
//            txBytes += bucket.getTxBytes();
//        }
//        while (networkStats.hasNextBucket());

        Long rxBytes = bucket.getRxBytes();
        Long txBytes = bucket.getTxBytes();

        sent = txBytes;
        received = rxBytes;
        total = sent + received;

        sent = sent - excludedSent;
        received = received - excludedReceived;
        total = total - excludedTotal;

        Long[] data = new Long[]{sent, received, total};
        return data;
    }

    public static Long[] getTotalAppWifiDataUsage(Context context, int session) throws ParseException, RemoteException {
        Long[] data;
        Long resetTimeMillis = getTimePeriod(context, session, 1)[0];
        Long endTimeMillis = getTimePeriod(context, session, 1)[1];
        Long sent, received, total;

        Long excludedSent = 0L,
                excludedReceived = 0L,
                excludedTotal = 0L;

        String jsonData = SharedPreferences.getExcludeAppsPrefs(context)
                .getString(EXCLUDE_APPS_LIST, null);
        List<AppModel> excludedAppsList = new ArrayList<>();
        if (jsonData != null) {
            excludedAppsList.addAll(gson.fromJson(jsonData, type));
        }
        for (AppModel app : excludedAppsList) {
            int uid = 0;
            try {
                uid = context.getPackageManager().getApplicationInfo(app.getPackageName(), 0).uid;
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
            Long[] wifi = getAppWifiDataUsage(context, uid, session);

            excludedSent += wifi[0];
            excludedReceived += wifi[1];
            excludedTotal += wifi[2];
        }

        NetworkStatsManager networkStatsManager = (NetworkStatsManager) context.getSystemService(Context.NETWORK_STATS_SERVICE);
        NetworkStats.Bucket bucket = new NetworkStats.Bucket();

        NetworkStats networkStats = networkStatsManager.querySummary(ConnectivityManager.TYPE_WIFI,
                getSubscriberId(context),
                resetTimeMillis,
                endTimeMillis);

        received = 0l;
        sent = 0l;

        do {
            networkStats.getNextBucket(bucket);
            received += bucket.getRxBytes();
            sent += bucket.getTxBytes();
        }
        while (networkStats.hasNextBucket());

        total = sent + received;

        sent = sent - excludedSent;
        received = received - excludedReceived;
        total = total - excludedTotal;

        data = new Long[]{sent, received, total};
        return data;
    }

    public static Long[] getTotalAppMobileDataUsage(Context context, int session, @Nullable int startDate) throws ParseException, RemoteException {
        NetworkStatsManager networkStatsManager = (NetworkStatsManager) context.getSystemService(Context.NETWORK_STATS_SERVICE);
        NetworkStats.Bucket bucket = new NetworkStats.Bucket();

        Long resetTimeMillis = getTimePeriod(context, session, startDate)[0];
        Long endTimeMillis = getTimePeriod(context, session, startDate)[1];

        Long sent = 0L,
                received = 0L,
                total = 0L;
        Long excludedSent = 0L,
                excludedReceived = 0L,
                excludedTotal = 0L;

        String jsonData = SharedPreferences.getExcludeAppsPrefs(context)
                .getString(EXCLUDE_APPS_LIST, null);
        List<AppModel> excludedAppsList = new ArrayList<>();
        if (jsonData != null) {
            excludedAppsList.addAll(gson.fromJson(jsonData, type));
        }
        for (AppModel app : excludedAppsList) {
            int uid = 0;
            try {
                uid = context.getPackageManager().getApplicationInfo(app.getPackageName(), 0).uid;
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
            Long[] mobile = getAppMobileDataUsage(context, uid, session);

            excludedSent += mobile[0];
            excludedReceived += mobile[1];
            excludedTotal += mobile[2];
        }

        NetworkStats networkStats = networkStatsManager.querySummary(ConnectivityManager.TYPE_MOBILE,
                getSubscriberId(context),
                resetTimeMillis,
                endTimeMillis);

        Long rxBytes = 0L;
        Long txBytes = 0L;

        do {
            networkStats.getNextBucket(bucket);
            rxBytes += bucket.getRxBytes();
            txBytes += bucket.getTxBytes();
        }
        while (networkStats.hasNextBucket());

        sent = txBytes;
        received = rxBytes;
        total = sent + received;

        sent = sent - excludedSent;
        received = received - excludedReceived;
        total = total - excludedTotal;

        Long[] data = new Long[]{sent, received, total};
        return data;
    }

    public static Long[] getAppWifiDataUsage(Context context, int uid, int session)
            throws ParseException, RemoteException {
        NetworkStatsManager networkStatsManager = (NetworkStatsManager) context.getApplicationContext().
                getSystemService(Context.NETWORK_STATS_SERVICE);
        NetworkStats networkStats = null;

        Long sent = 0L;
        Long received = 0L;
        Long total = 0L;

        Long resetTimeMillis = getTimePeriod(context, session, 1)[0];
        Long endTimeMillis = getTimePeriod(context, session, 1)[1];

        networkStats = networkStatsManager
                .querySummary(ConnectivityManager.TYPE_WIFI,
                        getSubscriberId(context),
                        resetTimeMillis,
                        endTimeMillis);

        do {
            NetworkStats.Bucket bucket = new NetworkStats.Bucket();
            networkStats.getNextBucket(bucket);
            if (bucket.getUid() == uid) {
                sent = sent + (bucket.getTxBytes());
                received = received + (bucket.getRxBytes());
            }
        }
        while (networkStats.hasNextBucket());

        total = sent + received;
        networkStats.close();

        Long[] data = new Long[]{sent, received, total};
        return data;
    }

    public static Long[] getAppMobileDataUsage(Context context, int uid, int session) throws RemoteException, ParseException {
        NetworkStatsManager networkStatsManager = (NetworkStatsManager) context.getApplicationContext().
                getSystemService(Context.NETWORK_STATS_SERVICE);
        NetworkStats networkStats = null;

        Long total = 0L;

        Long sent = 0L;
        Long received = 0L;

        Long resetTimeMillis = getTimePeriod(context, session, 1)[0];
        Long endTimeMillis = getTimePeriod(context, session, 1)[1];

        networkStats = networkStatsManager.querySummary(ConnectivityManager.TYPE_MOBILE,
                getSubscriberId(context),
                resetTimeMillis,
                endTimeMillis);

        do {
            NetworkStats.Bucket bucket = new NetworkStats.Bucket();
            networkStats.getNextBucket(bucket);
            if (bucket.getUid() == uid) {
                sent = sent + (bucket.getTxBytes());
                received = received + (bucket.getRxBytes());

            }
        }
        while (networkStats.hasNextBucket());

        total = sent + received;
        networkStats.close();

        Long[] data = new Long[]{sent, received, total};

        return data;
    }

    public static String[] formatData(Long sent, Long received) {
        Long total = sent + received;
        String[] data;

        Float totalBytes = total / 1024f;
        Float sentBytes = sent / 1024f;
        Float receivedBytes = received / 1024f;
        Float totalMB = totalBytes / 1024f;
        Float totalGB, sentGB, sentMB, receivedGB, receivedMB;
        sentMB = sentBytes / 1024f;
        receivedMB = receivedBytes / 1024f;
        String sentData = "", receivedData = "", totalData;

        if (totalMB > 1024) {
            totalGB = totalMB / 1024f;
            totalData = String.format("%.2f", totalGB) + " GB";
        } else {
            totalData = String.format("%.2f", totalMB) + " MB";
        }
        if (sentMB > 1024) {
            sentGB = sentMB / 1024f;
            sentData = String.format("%.2f", sentGB) + " GB";
        } else {
            sentData = String.format("%.2f", sentMB) + " MB";
        }

        if (receivedMB > 1024) {
            receivedGB = receivedMB / 1024f;
            receivedData = String.format("%.2f", receivedGB) + " GB";
        } else {
            receivedData = String.format("%.2f", receivedMB) + " MB";
        }

        data = new String[]{sentData, receivedData, totalData};
        return data;
    }

    public static List<OverviewModel> updateOverview(Context context, int[] days) throws ParseException, RemoteException {
        NetworkStatsManager networkStatsManager = (NetworkStatsManager)
                context.getSystemService(Context.NETWORK_STATS_SERVICE);
        NetworkStats networkStats = null;
        NetworkStats.Bucket mobileBucket = new NetworkStats.Bucket();
        NetworkStats.Bucket wifiBucket = new NetworkStats.Bucket();

        int year, month, day;
        long resetTimeMillis = 0l,
                endTimeMillis = 0l;
        Long sentMobile = 0L,
                receivedMobile = 0L,
                totalMobile = 0L,
                sentWifi = 0L,
                receivedWifi = 0L,
                totalWifi = 0L;

        Date date = new Date();
        SimpleDateFormat yearFormat = new SimpleDateFormat("yyyy");
        SimpleDateFormat monthFormat = new SimpleDateFormat("MM");
        SimpleDateFormat dayFormat = new SimpleDateFormat("dd");
        SimpleDateFormat resetFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

        String resetTime, endTime;
        Date resetDate, endDate;

        year = Integer.parseInt(yearFormat.format(date));
        month = Integer.parseInt(monthFormat.format(date));
        day = Integer.parseInt(dayFormat.format(date));
        List<OverviewModel> list = new ArrayList<>();

        for (int i = 0; i < days.length; i++) {
            day = Integer.parseInt(dayFormat.format(date)) - i;
            resetTime = context.getResources().getString(R.string.reset_time, year, month, day, 00, 00);
            resetDate = resetFormat.parse(resetTime);
            resetTimeMillis = resetDate.getTime();

            day = day + 1;
            endTime = context.getResources().getString(R.string.reset_time, year, month, day, 00, 00);
            endDate = resetFormat.parse(endTime);
            endTimeMillis = endDate.getTime();


            mobileBucket = networkStatsManager.querySummaryForDevice(ConnectivityManager.TYPE_MOBILE,
                    getSubscriberId(context),
                    resetTimeMillis,
                    endTimeMillis);

            Long rxBytes = mobileBucket.getRxBytes();
            Long txBytes = mobileBucket.getTxBytes();
            sentMobile = txBytes;
            receivedMobile = rxBytes;
            totalMobile = ((sentMobile + receivedMobile) / 1024) / 1024;

            wifiBucket = networkStatsManager.querySummaryForDevice(ConnectivityManager.TYPE_WIFI,
                    getSubscriberId(context),
                    resetTimeMillis,
                    endTimeMillis);

            receivedWifi = wifiBucket.getRxBytes();
            sentWifi = wifiBucket.getTxBytes();
            totalWifi = ((sentWifi + receivedWifi) / 1024) / 1024;

            list.add(i, new OverviewModel(totalMobile, totalWifi));
        }
        Log.d(TAG, "updateOverview: " + list.size());
        Collections.reverse(list);

        return list;
    }

    public static Long[] getTetheringDataUsage(Context context, int session) throws ParseException, RemoteException {
        NetworkStatsManager networkStatsManager = (NetworkStatsManager) context.getApplicationContext().
                getSystemService(Context.NETWORK_STATS_SERVICE);
        NetworkStats networkStats = null;

        Long total = 0L;

        Long sent = 0L;
        Long received = 0L;

        Long resetTimeMillis = getTimePeriod(context, session, 1)[0];
        Long endTimeMillis = getTimePeriod(context, session, 1)[1];

        networkStats = networkStatsManager.querySummary(ConnectivityManager.TYPE_MOBILE,
                getSubscriberId(context),
                resetTimeMillis,
                endTimeMillis);

        do {
            NetworkStats.Bucket bucket = new NetworkStats.Bucket();
            networkStats.getNextBucket(bucket);
            if (bucket.getUid() == UID_TETHERING) {
                sent = sent + (bucket.getTxBytes());
                received = received + (bucket.getRxBytes());
            }
        }
        while (networkStats.hasNextBucket());

        total = sent + received;
        networkStats.close();

        Long[] data = new Long[]{sent, received, total};

        return data;
    }

    public static Long[] getDeletedAppsMobileDataUsage(Context context, int session) throws RemoteException, ParseException {
        NetworkStatsManager networkStatsManager = (NetworkStatsManager) context.getApplicationContext().
                getSystemService(Context.NETWORK_STATS_SERVICE);
        NetworkStats networkStats = null;

        Long total = 0L;

        Long sent = 0L;
        Long received = 0L;

        Long resetTimeMillis = getTimePeriod(context, session, 1)[0];
        Long endTimeMillis = getTimePeriod(context, session, 1)[1];

        networkStats = networkStatsManager.querySummary(ConnectivityManager.TYPE_MOBILE,
                getSubscriberId(context),
                resetTimeMillis,
                endTimeMillis);

        do {
            NetworkStats.Bucket bucket = new NetworkStats.Bucket();
            networkStats.getNextBucket(bucket);
            if (bucket.getUid() == UID_REMOVED) {
                sent = sent + (bucket.getTxBytes());
                received = received + (bucket.getRxBytes());
            }
        }
        while (networkStats.hasNextBucket());

        total = sent + received;
        networkStats.close();

        Long[] data = new Long[]{sent, received, total};

        return data;
    }

    public static Long[] getDeletedAppsWifiDataUsage(Context context, int session) throws RemoteException, ParseException {
        NetworkStatsManager networkStatsManager = (NetworkStatsManager) context.getApplicationContext().
                getSystemService(Context.NETWORK_STATS_SERVICE);
        NetworkStats networkStats = null;

        Long total = 0L;

        Long sent = 0L;
        Long received = 0L;

        Long resetTimeMillis = getTimePeriod(context, session, 1)[0];
        Long endTimeMillis = getTimePeriod(context, session, 1)[1];

        networkStats = networkStatsManager.querySummary(ConnectivityManager.TYPE_WIFI,
                getSubscriberId(context),
                resetTimeMillis,
                endTimeMillis);

        do {
            NetworkStats.Bucket bucket = new NetworkStats.Bucket();
            networkStats.getNextBucket(bucket);
            if (bucket.getUid() == UID_REMOVED) {
                sent = sent + (bucket.getTxBytes());
                received = received + (bucket.getRxBytes());
            }
        }
        while (networkStats.hasNextBucket());

        total = sent + received;
        networkStats.close();

        Long[] data = new Long[]{sent, received, total};

        return data;
    }

    @SuppressLint("SimpleDateFormat")
    public static Long[] getTimePeriod(Context context, int session, @Nullable int startDate) throws ParseException {
        int year, month, day;
        long resetTimeMillis = 0l,
                endTimeMillis = 0l;

        Long planStartDateMillis, planEndDateMillis;
        try {
            planStartDateMillis = PreferenceManager.getDefaultSharedPreferences(context)
                    .getLong(DATA_RESET_CUSTOM_DATE_START, MaterialDatePicker.todayInUtcMilliseconds());
            planEndDateMillis = PreferenceManager.getDefaultSharedPreferences(context)
                    .getLong(DATA_RESET_CUSTOM_DATE_END, MaterialDatePicker.todayInUtcMilliseconds());
        }
        catch (ClassCastException e) {
            int planStartIntValue = PreferenceManager.getDefaultSharedPreferences(context)
                    .getInt(DATA_RESET_CUSTOM_DATE_START, -1);
            int planEndIntValue = PreferenceManager.getDefaultSharedPreferences(context)
                    .getInt(DATA_RESET_CUSTOM_DATE_END, -1);
            planStartDateMillis = ((Number) planStartIntValue).longValue();
            planEndDateMillis = ((Number) planEndIntValue).longValue();
        }

        int resetHour = PreferenceManager.getDefaultSharedPreferences(context)
                .getInt("reset_hour", 0);
        int resetMin = PreferenceManager.getDefaultSharedPreferences(context)
                .getInt("reset_min", 0);

        if (!PreferenceManager.getDefaultSharedPreferences(context).getString(DATA_RESET, "null")
                .equals(DATA_RESET_DAILY)) {
            resetHour = 0;
            resetMin = 0;
        }

        int customStartHour = PreferenceManager.getDefaultSharedPreferences(context)
                .getInt(DATA_RESET_CUSTOM_DATE_START_HOUR,0);
        int customStartMin = PreferenceManager.getDefaultSharedPreferences(context)
                .getInt(DATA_RESET_CUSTOM_DATE_START_MIN,0);
        int customEndHour = PreferenceManager.getDefaultSharedPreferences(context)
                .getInt(DATA_RESET_CUSTOM_DATE_END_HOUR,11);
        int customEndMin = PreferenceManager.getDefaultSharedPreferences(context)
                .getInt(DATA_RESET_CUSTOM_DATE_END_MIN,59);

        Date date = new Date();
        SimpleDateFormat yearFormat = new SimpleDateFormat("yyyy");
        SimpleDateFormat monthFormat = new SimpleDateFormat("MM");
        SimpleDateFormat dayFormat = new SimpleDateFormat("dd");
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

        String startTime, endTime;
        Date resetDate, endDate;
        Calendar calendar = Calendar.getInstance();
        int monthlyResetDate = PreferenceManager.getDefaultSharedPreferences(context).getInt(DATA_RESET_DATE, 1);
        int today = calendar.get(Calendar.DAY_OF_MONTH) + 1;

        switch (session) {
            case SESSION_TODAY:
                year = Integer.parseInt(yearFormat.format(date));
                month = Integer.parseInt(monthFormat.format(date));
                day = Integer.parseInt(dayFormat.format(date));

                startTime = context.getResources().getString(R.string.reset_time, year, month, day, resetHour, resetMin);
                resetDate = dateFormat.parse(startTime);
                resetTimeMillis = resetDate.getTime();
                day = Integer.parseInt(dayFormat.format(date)) + 1;
                endTime = context.getResources().getString(R.string.reset_time, year, month, day, resetHour, resetMin);
                endDate = dateFormat.parse(endTime);
                endTimeMillis = endDate.getTime();
                calendar.add(Calendar.DATE, 1);
//                endTimeMillis = calendar.getTimeInMillis();
//                endTimeMillis = System.currentTimeMillis();
                break;

            case SESSION_YESTERDAY:
                year = Integer.parseInt(yearFormat.format(date));
                month = Integer.parseInt(monthFormat.format(date));
                day = Integer.parseInt(dayFormat.format(date)) - 1;
                startTime = context.getResources().getString(R.string.reset_time, year, month, day, resetHour, resetMin);
                resetDate = dateFormat.parse(startTime);
                resetTimeMillis = resetDate.getTime();

                day = Integer.parseInt(dayFormat.format(date));
                endTime = context.getResources().getString(R.string.reset_time, year, month, day, resetHour, resetMin);
                endDate = dateFormat.parse(endTime);
                endTimeMillis = endDate.getTime();

                break;

            case SESSION_THIS_MONTH:
                year = Integer.parseInt(yearFormat.format(date));
                month = Integer.parseInt(monthFormat.format(date));
                day = startDate;
                startTime = context.getResources().getString(R.string.reset_time, year, month, day, resetHour, resetMin);
                resetDate = dateFormat.parse(startTime);
                resetTimeMillis = resetDate.getTime();
                day = Integer.parseInt(dayFormat.format(date)) + 1;
                endTime = context.getResources().getString(R.string.reset_time, year, month, day, resetHour, resetMin);
                endDate = dateFormat.parse(endTime);
                endTimeMillis = endDate.getTime();
                break;

            case SESSION_LAST_MONTH:
//                year = Integer.parseInt(yearFormat.format(date));
//                month = Integer.parseInt(monthFormat.format(date)) - 1;
//                day = 1;
//                startTime = context.getResources().getString(R.string.reset_time, year, month, day, resetHour, resetMin);
//                resetDate = dateFormat.parse(startTime);
//                resetTimeMillis = resetDate.getTime();
//
//                month = Integer.parseInt(monthFormat.format(date));
//                endTime = context.getResources().getString(R.string.reset_time, year, month, day, resetHour, resetMin);
//                endDate = dateFormat.parse(endTime);
//                endTimeMillis = endDate.getTime();


                /*
                 * When data reset date is ahead of today's date, reducing 1 from the current month will
                 * only give the month when the current plan started.
                 * So to get the last month's period, 2 has to be subtracted to get the starting month
                 * and 1 to get the ending month
                 * For eg: Today is 4th of August and plan resets on 8th of August, subtracting 2 & 1
                 * respectively will wive the period of June 8th to July 8th, i.e period of last month.
                 */

                if (monthlyResetDate >= today) {
                    // Time period from reset date of previous month till today
                    year = Integer.parseInt(yearFormat.format(date));
                    month = Integer.parseInt(monthFormat.format(date)) - 2;
                    day = monthlyResetDate;
                    startTime = context.getResources().getString(R.string.reset_time, year, month, day, resetHour, resetMin);
                    resetDate = dateFormat.parse(startTime);
                    resetTimeMillis = resetDate.getTime();

                    month = Integer.parseInt(monthFormat.format(date)) - 1;
                    day = monthlyResetDate;
                    endTime = context.getResources().getString(R.string.reset_time, year, month, day, resetHour, resetMin);
                    endDate = dateFormat.parse(endTime);
                    endTimeMillis = endDate.getTime();
                }
                else {
                    // Reset date is in the current month.
                    year = Integer.parseInt(yearFormat.format(date));
                    month = Integer.parseInt(monthFormat.format(date)) - 1;
                    day = monthlyResetDate;
                    startTime = context.getResources().getString(R.string.reset_time, year, month, day, resetHour, resetMin);
                    resetDate = dateFormat.parse(startTime);
                    resetTimeMillis = resetDate.getTime();

//                    day = monthlyResetDate;
                    month += 1; // To restore back the current month.
                    endTime = context.getResources().getString(R.string.reset_time, year, month, day, resetHour, resetMin);
                    endDate = dateFormat.parse(endTime);
                    endTimeMillis = endDate.getTime();
                }
                break;

            case SESSION_THIS_YEAR:
                year = Integer.parseInt(yearFormat.format(date));
                month = 1;
                day = 1;
                startTime = context.getResources().getString(R.string.reset_time, year, month, day, resetHour, resetMin);
                resetDate = dateFormat.parse(startTime);
                resetTimeMillis = resetDate.getTime();
                month = Integer.parseInt(monthFormat.format(date));
                day = Integer.parseInt(dayFormat.format(date)) + 1;
                endTime = context.getResources().getString(R.string.reset_time, year, month, day, resetHour, resetMin);
                endDate = dateFormat.parse(endTime);
                endTimeMillis = endDate.getTime();
                break;

            case SESSION_ALL_TIME:
                resetTimeMillis = 0l;
                year = Integer.parseInt(yearFormat.format(date));
                month = Integer.parseInt(monthFormat.format(date));
                day = Integer.parseInt(dayFormat.format(date)) + 1;
                endTime = context.getResources().getString(R.string.reset_time, year, month, day, resetHour, resetMin);
                endDate = dateFormat.parse(endTime);
                endTimeMillis = endDate.getTime();
                break;

            case SESSION_MONTHLY:
                if (monthlyResetDate >= today) {
                    // Time period from reset date of previous month till today
                    year = Integer.parseInt(yearFormat.format(date));
                    month = Integer.parseInt(monthFormat.format(date)) - 1;
                    day = monthlyResetDate;
                    startTime = context.getResources().getString(R.string.reset_time, year, month, day, resetHour, resetMin);
                    resetDate = dateFormat.parse(startTime);
                    resetTimeMillis = resetDate.getTime();

                    month = Integer.parseInt(monthFormat.format(date));
                    day = today;
                    endTime = context.getResources().getString(R.string.reset_time, year, month, day, resetHour, resetMin);
                    endDate = dateFormat.parse(endTime);
                    endTimeMillis = endDate.getTime();
                }
                else {
                    // Reset date is in the current month.
                    year = Integer.parseInt(yearFormat.format(date));
                    month = Integer.parseInt(monthFormat.format(date));
                    day = monthlyResetDate;
                    startTime = context.getResources().getString(R.string.reset_time, year, month, day, resetHour, resetMin);
                    resetDate = dateFormat.parse(startTime);
                    resetTimeMillis = resetDate.getTime();
                    day = Integer.parseInt(dayFormat.format(date)) + 1;
                    endTime = context.getResources().getString(R.string.reset_time, year, month, day, resetHour, resetMin);
                    endDate = dateFormat.parse(endTime);
                    endTimeMillis = endDate.getTime();
                }
                break;

            case SESSION_CUSTOM:
                year = Integer.parseInt(yearFormat.format(planStartDateMillis));;
                month = Integer.parseInt(monthFormat.format(planStartDateMillis));
                day = Integer.parseInt(dayFormat.format(planStartDateMillis));
                startTime = context.getResources()
                        .getString(R.string.reset_time, year, month, day, customStartHour, customStartMin);
                resetDate = dateFormat.parse(startTime);
                resetTimeMillis = resetDate.getTime();

                year = Integer.parseInt(yearFormat.format(planEndDateMillis));
                month = Integer.parseInt(monthFormat.format(planEndDateMillis));
                day = Integer.parseInt(dayFormat.format(planEndDateMillis));
                endTime = context.getResources()
                        .getString(R.string.reset_time, year, month, day, customEndHour, customEndMin);
                endDate = dateFormat.parse(endTime);
                endTimeMillis = endDate.getTime();
                break;

            case SESSION_CUSTOM_FILTER:
                Pair<Long, Long> filter = AppDataUsageFragment.customFilter.getValue();
                resetTimeMillis = filter == null ? 0 : filter.first;

                endTimeMillis = filter == null ? 0 : filter.second;
                break;
        }

        if (resetTimeMillis > System.currentTimeMillis()) {
            year = Integer.parseInt(yearFormat.format(date));
            month = Integer.parseInt(monthFormat.format(date));
            day = Integer.parseInt(dayFormat.format(date));
            day = day - 1;
            startTime = context.getResources().getString(R.string.reset_time, year, month, day, resetHour, resetMin);
            resetDate = dateFormat.parse(startTime);
            resetTimeMillis = resetDate.getTime();

            startTime = context.getResources().getString(R.string.reset_time, year, month, day, resetHour, resetMin);
            resetDate = dateFormat.parse(startTime);
            resetTimeMillis = resetDate.getTime();
            day = Integer.parseInt(dayFormat.format(date));
            endTime = context.getResources().getString(R.string.reset_time, year, month, day, resetHour, resetMin);
            endDate = dateFormat.parse(endTime);
            endTimeMillis = endDate.getTime();
        }
        else {
            if (session == SESSION_TODAY) {
                year = Integer.parseInt(yearFormat.format(date));
                month = Integer.parseInt(monthFormat.format(date));
                day = Integer.parseInt(dayFormat.format(date));
                startTime = context.getResources().getString(R.string.reset_time, year, month, day, resetHour, resetMin);
                resetDate = dateFormat.parse(startTime);
                resetTimeMillis = resetDate.getTime();

                day = Integer.parseInt(dayFormat.format(date)) + 1;
                endTime = context.getResources().getString(R.string.reset_time, year, month, day, resetHour, resetMin);
                endDate = dateFormat.parse(endTime);
                endTimeMillis = endDate.getTime();
            }

        }
        return new Long[]{resetTimeMillis, endTimeMillis};
    }
}
