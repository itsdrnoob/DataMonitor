package com.drnoob.datamonitor.adapters.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.drnoob.datamonitor.utils.helpers.UsageDataHelper
import kotlinx.coroutines.launch

class DataUsageViewModel(private val usageDataHelper: UsageDataHelper) : ViewModel() {

    private val _userAppsList: MutableLiveData<List<AppDataUsageModel?>> = MutableLiveData()
    val userAppsList: LiveData<List<AppDataUsageModel?>>
        get() = _userAppsList

    private val _systemAppsList: MutableLiveData<List<AppDataUsageModel?>> = MutableLiveData()
    val systemAppsList: LiveData<List<AppDataUsageModel?>>
        get() = _userAppsList

    fun fetchApps() = viewModelScope.launch {
        usageDataHelper.fetchApps()
    }

    fun loadUserAppsData(session: Int, type: Int) = viewModelScope.launch {
        _userAppsList.postValue(usageDataHelper.loadUserAppsData(session, type))
    }

    fun loadSystemAppsData(session: Int, type: Int) = viewModelScope.launch {
        _systemAppsList.postValue(usageDataHelper.loadSystemAppsData(session, type))
    }

}

class DataUsageViewModelFactory(private val usageDataHelper: UsageDataHelper) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DataUsageViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DataUsageViewModel(usageDataHelper) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}