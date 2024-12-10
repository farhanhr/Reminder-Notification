package com.example.myapplication

import android.content.BroadcastReceiver
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat


class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d("ReminderReceiver", "onReceive triggered")

        if (intent == null) {
            Log.d("ReminderReceiver", "Intent is null")
        } else {
            Log.d("ReminderReceiver", "Intent received: ${intent.action}")
        }

        val notificationManager =
            context?.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channelId = "daily_reminder_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Daily Reminder",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Channel for daily reminders"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle("Reminder")
            .setContentText("It's time for your reminder!")
            .setSmallIcon(R.drawable.ic_notifications)
            .build()

        notificationManager.notify(1, notification)
    }

}