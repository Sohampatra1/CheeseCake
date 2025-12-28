package com.example.cheesecake

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import android.content.Intent
import java.util.concurrent.TimeUnit

object NotificationManager {
    fun scheduleReminder(context: Context) {
        val workRequest = PeriodicWorkRequestBuilder<ReminderWorker>(30, TimeUnit.MINUTES)
            .setInitialDelay(30, TimeUnit.MINUTES) // First reminder after 30 mins
            .addTag("water_reminder")
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "water_reminder_work",
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }

    fun cancelReminder(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork("water_reminder_work")
    }

    fun schedulePeriodCheck(context: Context) {
        val workRequest = PeriodicWorkRequestBuilder<PeriodWorker>(6, TimeUnit.HOURS)
            .addTag("period_check")
            .build()
        
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "period_check_work",
            ExistingPeriodicWorkPolicy.KEEP, // Keep existing if valid
            workRequest
        )
    }

    fun sendPeriodNotification(context: Context) {
        val channelId = "period_tracker_channel"
        val notificationId = 2001
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager

        val channel = android.app.NotificationChannel(
            channelId,
            "Cycle Tracking",
            android.app.NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Reminders for your cycle"
        }
        notificationManager.createNotificationChannel(channel)

        // Intent to open Period Page (Cycles tab)
        // We can pass an extra to switch tab
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("EXTRA_OPEN_TAB", 1) // 1 = Cycles
        }
        val pendingIntent = android.app.PendingIntent.getActivity(
            context,
            2001,
            intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val builder = androidx.core.app.NotificationCompat.Builder(context, channelId)
            // Using a system icon as fallback if R.drawable.ic_logo isn't ready/generic
            .setSmallIcon(R.mipmap.ic_launcher) 
            .setContentTitle("Cycle Reminder")
            .setContentText("Your period is expected soon or is late. Please log it if it has started.")
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        notificationManager.notify(notificationId, builder.build())
    }
}
