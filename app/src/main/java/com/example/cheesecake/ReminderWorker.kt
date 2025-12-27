package com.example.cheesecake

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class ReminderWorker(
    appContext: Context, 
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val currentHour = java.time.LocalTime.now().hour
        // Active between 10 AM (10) and 10 PM (22)
        if (currentHour in 10 until 22) {
            triggerNotification()
        }
        return Result.success()
    }

    private fun triggerNotification() {
        val channelId = "water_intake_channel"
        val notificationId = 1001

        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create Channel (Safe to call repeatedly)
        val channel = NotificationChannel(
            channelId,
            "Water Intake Reminders",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Reminds you to drink water"
        }
        notificationManager.createNotificationChannel(channel)

        // Intent to open App
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("EXTRA_REMINDER_TRIGGER", true) // Signal to beep
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, 
            0, 
            intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.mipmap.ic_launcher) // Fallback, will use app icon ideally
            .setContentTitle("Hydration Time!")
            .setContentText("It's time to drink water. Verify now!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        notificationManager.notify(notificationId, builder.build())
    }
}
