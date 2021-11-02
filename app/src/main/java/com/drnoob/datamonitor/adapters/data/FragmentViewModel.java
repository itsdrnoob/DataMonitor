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

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class FragmentViewModel extends ViewModel {
    private MutableLiveData<Integer> currentSession = new MutableLiveData<>();
    private MutableLiveData<Integer> currentType = new MutableLiveData<>();

    public MutableLiveData<Integer> getCurrentSession() {
        return currentSession;
    }

    public void setCurrentSession(Integer currentSession) {
        this.currentSession.setValue(currentSession);
    }

    public MutableLiveData<Integer> getCurrentType() {
        return currentType;
    }

    public void setCurrentType(Integer currentType) {
        this.currentType.setValue(currentType);
    }
}
