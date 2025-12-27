package com.example.cheesecake

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WaterIntakeDao {
    @Insert
    suspend fun insert(record: WaterIntakeRecord)

    @Query("SELECT * FROM water_intake_records ORDER BY timestamp DESC")
    fun getAll(): Flow<List<WaterIntakeRecord>>
}
