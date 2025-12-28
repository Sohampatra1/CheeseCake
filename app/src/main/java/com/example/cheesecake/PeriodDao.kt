package com.example.cheesecake

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PeriodDao {
    @Insert
    suspend fun insert(record: PeriodRecord)

    @Query("SELECT * FROM period_records ORDER BY timestamp DESC")
    fun getAll(): Flow<List<PeriodRecord>>

    @Query("SELECT * FROM period_records ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastPeriod(): PeriodRecord?
}
