package com.example.cheesecake

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val waterIntakeDao: WaterIntakeDao
) : ViewModel() {

    val waterIntakeRecords: Flow<List<WaterIntakeRecord>> = waterIntakeDao.getAll()
    
    fun saveWaterIntakeRecord() {
        viewModelScope.launch {
            val record = WaterIntakeRecord(timestamp = System.currentTimeMillis())
            waterIntakeDao.insert(record)
        }
    }
}
