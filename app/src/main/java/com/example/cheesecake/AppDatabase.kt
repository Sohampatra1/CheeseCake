package com.example.cheesecake

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [WaterIntakeRecord::class, PeriodRecord::class], version = 2)
abstract class AppDatabase : RoomDatabase() {
    abstract fun waterIntakeDao(): WaterIntakeDao
    abstract fun periodDao(): PeriodDao
}
