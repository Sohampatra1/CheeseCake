package com.example.cheesecake

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "water_intake_records")
data class WaterIntakeRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis()
)
