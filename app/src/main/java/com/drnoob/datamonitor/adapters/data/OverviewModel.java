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

public class OverviewModel implements Serializable {
    private Long totalMobile, totalWifi;

    public OverviewModel() {
    }

    public OverviewModel(Long totalMobile, Long totalWifi) {
        this.totalMobile = totalMobile;
        this.totalWifi = totalWifi;
    }

    public Long getTotalMobile() {
        return totalMobile;
    }

    public void setTotalMobile(Long totalMobile) {
        this.totalMobile = totalMobile;
    }

    public Long getTotalWifi() {
        return totalWifi;
    }

    public void setTotalWifi(Long totalWifi) {
        this.totalWifi = totalWifi;
    }
}
