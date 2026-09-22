package com.example.motionpulse.ui.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.room.Room
import com.example.motionpulse.MainActivity
import com.example.motionpulse.R
import com.example.motionpulse.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class HabitReminderReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "habit_reminders"
        const val ACTION_COMPLETE = "com.example.motionpulse.ACTION_COMPLETE"
        const val ACTION_SKIP = "com.example.motionpulse.ACTION_SKIP"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            rescheduleAllAlarms(context)
            return
        }

        val habitId = intent.getStringExtra("HABIT_ID") ?: return
        val habitTitle = intent.getStringExtra("HABIT_TITLE") ?: "Habit Reminder"

        showNotification(context, habitId, habitTitle)
        
        // Reschedule next occurrence
        rescheduleHabit(context, habitId)
    }

    private fun showNotification(context: Context, habitId: String, habitTitle: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val channel = NotificationChannel(CHANNEL_ID, "Habit Reminders", NotificationManager.IMPORTANCE_DEFAULT)
        notificationManager.createNotificationChannel(channel)

        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val mainPendingIntent = PendingIntent.getActivity(context, habitId.hashCode(), mainIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

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
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(habitTitle)
            .setContentText("Time to complete your habit!")
            .setContentIntent(mainPendingIntent)
            .addAction(0, "Complete", completePendingIntent)
            .addAction(0, "Skip", skipPendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(habitId.hashCode(), notification)
    }

    private fun rescheduleHabit(context: Context, habitId: String) {
        val db = Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java, "motion_pulse_db"
        ).build()
        
        CoroutineScope(Dispatchers.IO).launch {
            val habit = db.habitDao().getHabitById(habitId)
            if (habit != null && !habit.isArchived) {
                ReminderManager(context).scheduleReminder(habit)
            }
        }
    }

    private fun rescheduleAllAlarms(context: Context) {
        val db = Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java, "motion_pulse_db"
        ).build()

        CoroutineScope(Dispatchers.IO).launch {
            val allActiveHabits = db.habitDao().getAllActiveHabits().first()
            ReminderManager(context).scheduleAllReminders(allActiveHabits)
        }
    }
}
