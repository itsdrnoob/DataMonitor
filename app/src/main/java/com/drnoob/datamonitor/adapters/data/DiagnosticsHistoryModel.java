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

public class DiagnosticsHistoryModel implements Serializable {
    private Long id;
    private String date;
    private String summary;
    private String downloadSpeed,
            uploadSpeed,
            minLatency,
            avgLatency,
            ip,
            isp,
            server,
            region;
    private Boolean isSelected = false;

    public DiagnosticsHistoryModel() {

    }

    public DiagnosticsHistoryModel(String date, String summary) {
        this.date = date;
        this.summary = summary;
    }

    public DiagnosticsHistoryModel(Long id, String date, String summary,
                                   String downloadSpeed, String uploadSpeed, String minLatency,
                                   String avgLatency, String ip, String isp, String server, String region) {
        this.id = id;
        this.date = date;
        this.summary = summary;
        this.downloadSpeed = downloadSpeed;
        this.uploadSpeed = uploadSpeed;
        this.minLatency = minLatency;
        this.avgLatency = avgLatency;
        this.ip = ip;
        this.isp = isp;
        this.server = server;
        this.region = region;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDownloadSpeed() {
        return downloadSpeed;
    }

    public void setDownloadSpeed(String downloadSpeed) {
        this.downloadSpeed = downloadSpeed;
    }

    public String getUploadSpeed() {
        return uploadSpeed;
    }

    public void setUploadSpeed(String uploadSpeed) {
        this.uploadSpeed = uploadSpeed;
    }

    public String getMinLatency() {
        return minLatency;
    }

    public void setMinLatency(String minLatency) {
        this.minLatency = minLatency;
    }

    public String getAvgLatency() {
        return avgLatency;
    }

    public void setAvgLatency(String avgLatency) {
        this.avgLatency = avgLatency;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getIsp() {
        return isp;
    }

    public void setIsp(String isp) {
        this.isp = isp;
    }

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public Boolean isSelected() {
        return isSelected;
    }

    public void setSelected(Boolean selected) {
        isSelected = selected;
    }
}
