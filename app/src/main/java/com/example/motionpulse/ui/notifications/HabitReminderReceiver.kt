package com.example.motionpulse.ui.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.motionpulse.R

class HabitReminderReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "habit_reminders"
        const val ACTION_COMPLETE = "com.example.motionpulse.ACTION_COMPLETE"
        const val ACTION_SKIP = "com.example.motionpulse.ACTION_SKIP"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val habitId = intent.getStringExtra("HABIT_ID") ?: return
        val habitTitle = intent.getStringExtra("HABIT_TITLE") ?: "Habit Reminder"

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Create channel for API 26+
        val channel = NotificationChannel(CHANNEL_ID, "Habit Reminders", NotificationManager.IMPORTANCE_DEFAULT)
        notificationManager.createNotificationChannel(channel)

        val completeIntent = Intent(context, HabitActionReceiver::class.java).apply {
            action = ACTION_COMPLETE
            putExtra("HABIT_ID", habitId)
        }
        val completePendingIntent = PendingIntent.getBroadcast(context, habitId.hashCode() + 1, completeIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val skipIntent = Intent(context, HabitActionReceiver::class.java).apply {
            action = ACTION_SKIP
            putExtra("HABIT_ID", habitId)
        }
        val skipPendingIntent = PendingIntent.getBroadcast(context, habitId.hashCode() + 2, skipIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Use default for now
            .setContentTitle(habitTitle)
            .setContentText("Time to complete your habit!")
            .addAction(0, "Complete", completePendingIntent)
            .addAction(0, "Skip", skipPendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(habitId.hashCode(), notification)
    }
}
