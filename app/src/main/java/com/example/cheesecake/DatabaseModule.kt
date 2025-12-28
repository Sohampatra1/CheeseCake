package com.example.cheesecake

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "water_reminder.db"
        )
        .fallbackToDestructiveMigration()
        .build()
    }

    @Provides
    fun provideWaterIntakeDao(appDatabase: AppDatabase): WaterIntakeDao {
        return appDatabase.waterIntakeDao()
    }

    @Provides
    fun providePeriodDao(appDatabase: AppDatabase): PeriodDao {
        return appDatabase.periodDao()
    }
}
