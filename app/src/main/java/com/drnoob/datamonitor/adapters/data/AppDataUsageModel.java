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

package com.drnoob.datamonitor.adapters.data;

import android.graphics.drawable.Drawable;

import java.io.Serializable;
import java.util.List;

public class AppDataUsageModel implements Serializable {

    private String mAppName, mPackageName, mTotalDataUsage;
    private Drawable mAppIcon;
    private long mSentMobile, mSentWifi, mReceivedMobile, mReceivedWifi;
    private Float mMobileTotal, mWifiTotal;
    private int uid, session, type, progress;
    private Boolean isSystemApp;
    private Boolean isAppsList;
    private String dataLimit, dataType;

    private List<AppDataUsageModel> list;

    public AppDataUsageModel() {
    }

    public AppDataUsageModel(String mAppName, Drawable mAppIcon, String mTotalDataUsage) {
        this.mAppName = mAppName;
        this.mAppIcon = mAppIcon;
        this.mTotalDataUsage = mTotalDataUsage;
    }

    public AppDataUsageModel(String mAppName, String mTotalDataUsage, Drawable mAppIcon, Float mMobileTotal) {
        this.mAppName = mAppName;
        this.mTotalDataUsage = mTotalDataUsage;
        this.mAppIcon = mAppIcon;
        this.mMobileTotal = mMobileTotal;
    }

    public AppDataUsageModel(String mAppName, String mPackageName, int uid, Boolean isSystemApp) {
        this.mAppName = mAppName;
        this.mPackageName = mPackageName;
        this.uid = uid;
        this.isSystemApp = isSystemApp;
    }

    public AppDataUsageModel(String mAppName, String mPackageName, int uid, Boolean isSystemApp, Boolean isAppsList) {
        this.mAppName = mAppName;
        this.mPackageName = mPackageName;
        this.uid = uid;
        this.isSystemApp = isSystemApp;
        this.isAppsList = isAppsList;
    }


    public String getAppName() {
        return mAppName;
    }

    public void setAppName(String mAppName) {
        this.mAppName = mAppName;
    }

    public String getPackageName() {
        return mPackageName;
    }

    public void setPackageName(String mPackageName) {
        this.mPackageName = mPackageName;
    }

    public Drawable getAppIcon() {
        return mAppIcon;
    }

    public void setAppIcon(Drawable mAppIcon) {
        this.mAppIcon = mAppIcon;
    }

    public String getTotalDataUsage() {
        return mTotalDataUsage;
    }

    public void setTotalDataUsage(String mDataUsage) {
        this.mTotalDataUsage = mDataUsage;
    }

    public long getSentMobile() {
        return mSentMobile;
    }

    public void setSentMobile(long mSent) {
        this.mSentMobile = mSent;
    }

    public long getReceivedMobile() {
        return mReceivedMobile;
    }

    public void setReceivedMobile(long mReceived) {
        this.mReceivedMobile = mReceived;
    }

    public long getSentWifi() {
        return mSentWifi;
    }

    public void setSentWifi(long mSentWifi) {
        this.mSentWifi = mSentWifi;
    }

    public long getReceivedWifi() {
        return mReceivedWifi;
    }

    public void setReceivedWifi(long mReceivedWifi) {
        this.mReceivedWifi = mReceivedWifi;
    }

    public Float getMobileTotal() {
        return mMobileTotal;
    }

    public void setMobileTotal(Float mTotal) {
        this.mMobileTotal = mTotal;
    }

    public Float getWifiTotal() {
        return mWifiTotal;
    }

    public void setWifiTotal(Float mWifiTotal) {
        this.mWifiTotal = mWifiTotal;
    }

    public int getUid() {
        return uid;
    }

    public void setUid(int uid) {
        this.uid = uid;
    }

    public int getSession() {
        return session;
    }

    public void setSession(int session) {
        this.session = session;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public Boolean isSystemApp() {
        return isSystemApp;
    }

    public void setIsSystemApp(Boolean systemApp) {
        isSystemApp = systemApp;
    }

    public List<AppDataUsageModel> getList() {
        return list;
    }

    public void setList(List<AppDataUsageModel> list) {
        this.list = list;
    }

    public Boolean isAppsList() {
        return isAppsList;
    }

    public void setIsAppsList(Boolean appsList) {
        isAppsList = appsList;
    }

    public String getDataLimit() {
        return dataLimit;
    }

    public void setDataLimit(String dataLimit) {
        this.dataLimit = dataLimit;
    }

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }
}
