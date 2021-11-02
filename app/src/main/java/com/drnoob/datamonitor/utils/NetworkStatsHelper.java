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

import android.app.usage.NetworkStats;
import android.app.usage.NetworkStatsManager;
import android.content.Context;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.RemoteException;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.preference.PreferenceManager;

import com.drnoob.datamonitor.R;
import com.drnoob.datamonitor.adapters.data.OverviewModel;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static android.app.usage.NetworkStats.Bucket.UID_REMOVED;
import static android.app.usage.NetworkStats.Bucket.UID_TETHERING;
import static com.drnoob.datamonitor.core.Values.SESSION_ALL_TIME;
import static com.drnoob.datamonitor.core.Values.SESSION_LAST_MONTH;
import static com.drnoob.datamonitor.core.Values.SESSION_THIS_MONTH;
import static com.drnoob.datamonitor.core.Values.SESSION_THIS_YEAR;
import static com.drnoob.datamonitor.core.Values.SESSION_TODAY;
import static com.drnoob.datamonitor.core.Values.SESSION_YESTERDAY;

/* Created by Dr.NooB on 23/09/2021 */

public class NetworkStatsHelper {
    private static final String TAG = NetworkStatsHelper.class.getSimpleName();

    public static String getSubscriberId(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            return telephonyManager.getSubscriberId();
        } else {
            return null;
        }
    }

    public static Long[] getDeviceWifiDataUsage(Context context, int session) throws ParseException, RemoteException {
        Long[] data;
        Long resetTimeMillis = getTimePeriod(context, session)[0];
        Long endTimeMillis = getTimePeriod(context, session)[1];
        Long sent, received, total;

        NetworkStatsManager networkStatsManager = (NetworkStatsManager) context.getSystemService(Context.NETWORK_STATS_SERVICE);
        NetworkStats.Bucket bucket = new NetworkStats.Bucket();
        bucket = networkStatsManager.querySummaryForDevice(ConnectivityManager.TYPE_WIFI,
                getSubscriberId(context),
                resetTimeMillis,
                endTimeMillis);

        received = bucket.getRxBytes();
        sent = bucket.getTxBytes();
        total = sent + received;
        data = new Long[]{sent, received, total};
        return data;
    }

    public static Long[] getDeviceMobileDataUsage(Context context, int session) throws ParseException, RemoteException {
        NetworkStatsManager networkStatsManager = (NetworkStatsManager) context.getSystemService(Context.NETWORK_STATS_SERVICE);
        NetworkStats networkStats = null;
        NetworkStats.Bucket bucket = new NetworkStats.Bucket();

        Long resetTimeMillis = getTimePeriod(context, session)[0];
        Long endTimeMillis = getTimePeriod(context, session)[1];
        Long sent = 0L,
                received = 0L,
                total = 0L;

        bucket = networkStatsManager.querySummaryForDevice(ConnectivityManager.TYPE_MOBILE,
                getSubscriberId(context),
                resetTimeMillis,
                endTimeMillis);

        Long rxBytes = bucket.getRxBytes();
        Long txBytes = bucket.getTxBytes();

        sent = txBytes;
        received = rxBytes;
        total = sent + received;
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

        Long resetTimeMillis = getTimePeriod(context, session)[0];
        Long endTimeMillis = getTimePeriod(context, session)[1];

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

        Long resetTimeMillis = getTimePeriod(context, session)[0];
        Long endTimeMillis = getTimePeriod(context, session)[1];

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
        Log.e(TAG, "updateOverview: " + list.size());
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

        Long resetTimeMillis = getTimePeriod(context, session)[0];
        Long endTimeMillis = getTimePeriod(context, session)[1];

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

        Long resetTimeMillis = getTimePeriod(context, session)[0];
        Long endTimeMillis = getTimePeriod(context, session)[1];

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

        Long resetTimeMillis = getTimePeriod(context, session)[0];
        Long endTimeMillis = getTimePeriod(context, session)[1];

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

    private static Long[] getTimePeriod(Context context, int session) throws ParseException {
        int year, month, day;
        long resetTimeMillis = 0l,
                endTimeMillis = 0l;

        int resetHour = PreferenceManager.getDefaultSharedPreferences(context)
                .getInt("reset_hour", 0);
        int resetMin = PreferenceManager.getDefaultSharedPreferences(context)
                .getInt("reset_min", 0);

        Date date = new Date();
        SimpleDateFormat yearFormat = new SimpleDateFormat("yyyy");
        SimpleDateFormat monthFormat = new SimpleDateFormat("MM");
        SimpleDateFormat dayFormat = new SimpleDateFormat("dd");
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

        String startTime, endTime;
        Date resetDate, endDate;

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
                Calendar calendar = Calendar.getInstance();
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
                day = 1;
                startTime = context.getResources().getString(R.string.reset_time, year, month, day, resetHour, resetMin);
                resetDate = dateFormat.parse(startTime);
                resetTimeMillis = resetDate.getTime();
                day = Integer.parseInt(dayFormat.format(date)) + 1;
                endTime = context.getResources().getString(R.string.reset_time, year, month, day, resetHour, resetMin);
                endDate = dateFormat.parse(endTime);
                endTimeMillis = endDate.getTime();
                break;

            case SESSION_LAST_MONTH:
                year = Integer.parseInt(yearFormat.format(date));
                month = Integer.parseInt(monthFormat.format(date)) - 1;
                day = 1;
                startTime = context.getResources().getString(R.string.reset_time, year, month, day, resetHour, resetMin);
                resetDate = dateFormat.parse(startTime);
                resetTimeMillis = resetDate.getTime();

                month = Integer.parseInt(monthFormat.format(date));
                endTime = context.getResources().getString(R.string.reset_time, year, month, day, resetHour, resetMin);
                endDate = dateFormat.parse(endTime);
                endTimeMillis = endDate.getTime();
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
        } else {
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
