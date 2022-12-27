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

import java.util.List;

public class LiveData extends ViewModel {
    private MutableLiveData<Boolean> isAppSelectionView = new MutableLiveData<>();
    private MutableLiveData<List<AppModel>> selectedAppsList = new MutableLiveData<>();
    private MutableLiveData<Integer> delete = new MutableLiveData<>();
    private MutableLiveData<Boolean> isResultSelectionView = new MutableLiveData<>();
    private MutableLiveData<List<DiagnosticsHistoryModel>> selectedResults = new MutableLiveData<>();

    public MutableLiveData<Boolean> getIsAppSelectionView() {
        return isAppSelectionView;
    }

    public void setIsAppSelectionView(Boolean isAppSelectionView) {
        this.isAppSelectionView.setValue(isAppSelectionView);
    }

    public MutableLiveData<List<AppModel>> getSelectedAppsList() {
        return selectedAppsList;
    }

    public void setSelectedAppsList(List<AppModel> selectedAppsList) {
        this.selectedAppsList.setValue(selectedAppsList);
    }

    public MutableLiveData<Boolean> getIsResultSelectionView() {
        return isResultSelectionView;
    }

    public void setIsResultSelectionView(Boolean isResultSelectionView) {
        this.isResultSelectionView.setValue(isResultSelectionView);
    }

    public MutableLiveData<List<DiagnosticsHistoryModel>> getSelectedResults() {
        return selectedResults;
    }

    public void setSelectedResults(List<DiagnosticsHistoryModel> selectedResults) {
        this.selectedResults.setValue(selectedResults);
    }
}
