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

import java.io.Serializable;

public class AppModel implements Serializable {
    private String appName, packageName;
    private Boolean isSystemApp;
    private Boolean isSelected = false;

    public AppModel() {
        // Empty constructor
    }

    public AppModel(String appName, String packageName) {
        this.appName = appName;
        this.packageName = packageName;
    }

    public AppModel(String appName, String packageName, Boolean isSystemApp) {
        this.appName = appName;
        this.packageName = packageName;
        this.isSystemApp = isSystemApp;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String mAppName) {
        this.appName = mAppName;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String mPackageName) {
        this.packageName = mPackageName;
    }

    public Boolean getIsSystemApp() {
        return isSystemApp;
    }

    public void setIsSystemApp(Boolean systemApp) {
        isSystemApp = systemApp;
    }

    public Boolean getIsSelected() {
        return isSelected;
    }

    public void setIsSelected(Boolean selected) {
        isSelected = selected;
    }
}
