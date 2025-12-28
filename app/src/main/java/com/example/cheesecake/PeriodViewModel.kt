package com.example.cheesecake

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class PeriodViewModel @Inject constructor(
    private val periodDao: PeriodDao
) : ViewModel() {

    // Get all records in descending order
    val periodRecords: StateFlow<List<PeriodRecord>> = periodDao.getAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Derived state for the last period
    val lastPeriod = periodRecords.map { it.firstOrNull() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    // Calculate next predicted date (Last + 28 days)
    val nextPeriodDateLines = lastPeriod.map { record ->
        if (record == null) {
            "No Data"
        } else {
            val lastDate = Instant.ofEpochMilli(record.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
            val nextDate = lastDate.plusDays(28)
            val formatter = DateTimeFormatter.ofPattern("MMM dd")
            nextDate.format(formatter)
        }
    }

    // Days Late / Days Until
    fun getCycleStatus(lastTimestamp: Long?): String {
        if (lastTimestamp == null) return "Track your first period"
        
        val lastDate = Instant.ofEpochMilli(lastTimestamp).atZone(ZoneId.systemDefault()).toLocalDate()
        val nextDate = lastDate.plusDays(28)
        val today = java.time.LocalDate.now()
        
        val daysUntil = java.time.temporal.ChronoUnit.DAYS.between(today, nextDate)
        
        return when {
            daysUntil > 0 -> "$daysUntil days until next period"
            daysUntil == 0L -> "Period expected today"
            else -> "${kotlin.math.abs(daysUntil)} days late"
        }
    }

    fun logPeriodStart() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            periodDao.insert(PeriodRecord(timestamp = now))
        }
    }
}
