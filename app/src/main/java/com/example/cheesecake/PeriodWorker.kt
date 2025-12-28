package com.example.cheesecake

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit

class PeriodWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface PeriodWorkerEntryPoint {
        fun periodDao(): PeriodDao
    }

    override suspend fun doWork(): Result {
        val appContext = applicationContext
        
        // Hilt Injection for Worker
        val entryPoint = EntryPointAccessors.fromApplication(
            appContext,
            PeriodWorkerEntryPoint::class.java
        )
        val periodDao = entryPoint.periodDao()

        try {
            val lastPeriod = periodDao.getLastPeriod()
            if (lastPeriod != null) {
                // Prediction: 28 days
                val cycleLengthMillis = TimeUnit.DAYS.toMillis(28)
                val expectedNextPeriod = lastPeriod.timestamp + cycleLengthMillis

                // If today is ON or AFTER the expected date
                if (System.currentTimeMillis() >= expectedNextPeriod) {
                    NotificationManager.sendPeriodNotification(appContext)
                }
            }
        } catch (e: Exception) {
            return Result.failure()
        }

        return Result.success()
    }
}
