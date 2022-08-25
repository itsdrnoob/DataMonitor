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

import androidx.annotation.Keep;

import java.io.Serializable;

@Keep
public class OSSLibrary implements Serializable {
    private String libraryName;
    private String libraryAuthorName;
    private String libraryDesc;
    private String libraryVersion;
    private String libraryLicense;
    private int libraryLicenseURL;

    public OSSLibrary() {

    }

    public OSSLibrary(String libraryName, String libraryAuthorName, String libraryDesc, String libraryVersion, String libraryLicense, int libraryLicenseURL) {
        this.libraryName = libraryName;
        this.libraryAuthorName = libraryAuthorName;
        this.libraryDesc = libraryDesc;
        this.libraryVersion = libraryVersion;
        this.libraryLicense = libraryLicense;
        this.libraryLicenseURL = libraryLicenseURL;
    }

    public String getLibraryName() {
        return libraryName;
    }

    public void setLibraryName(String libraryName) {
        this.libraryName = libraryName;
    }

    public String getLibraryAuthorName() {
        return libraryAuthorName;
    }

    public void setLibraryAuthorName(String libraryAuthorName) {
        this.libraryAuthorName = libraryAuthorName;
    }

    public String getLibraryDesc() {
        return libraryDesc;
    }

    public void setLibraryDesc(String libraryDesc) {
        this.libraryDesc = libraryDesc;
    }

    public String getLibraryVersion() {
        return libraryVersion;
    }

    public void setLibraryVersion(String libraryVersion) {
        this.libraryVersion = libraryVersion;
    }

    public String getLibraryLicense() {
        return libraryLicense;
    }

    public void setLibraryLicense(String libraryLicense) {
        this.libraryLicense = libraryLicense;
    }

    public int getLibraryLicenseURL() {
        return libraryLicenseURL;
    }

    public void setLibraryLicenseURL(int libraryLicenseURL) {
        this.libraryLicenseURL = libraryLicenseURL;
    }
}
