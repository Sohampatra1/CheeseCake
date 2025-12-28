package com.example.cheesecake

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "period_records")
data class PeriodRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long
)
