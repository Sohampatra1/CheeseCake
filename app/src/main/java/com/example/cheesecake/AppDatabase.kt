package com.example.cheesecake

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [WaterIntakeRecord::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun waterIntakeDao(): WaterIntakeDao
}
