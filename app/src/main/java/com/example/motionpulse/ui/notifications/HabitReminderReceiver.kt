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
import com.example.motionpulse.data.local.entity.CompletionStatus
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
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
                    intent.action == Intent.ACTION_MY_PACKAGE_REPLACED ||
                    intent.action == Intent.ACTION_TIME_CHANGED ||
                    intent.action == Intent.ACTION_TIMEZONE_CHANGED
                ) {
                    rescheduleAllAlarmsSync(context)
                    return@launch
                }

                val habitId = intent.getStringExtra("HABIT_ID") ?: return@launch
                val habitTitle = intent.getStringExtra("HABIT_TITLE") ?: "Habit Reminder"

                val db = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java, "motion_pulse_db"
                ).fallbackToDestructiveMigration().build()

                val habit = db.habitDao().getHabitById(habitId)
                if (habit == null || habit.isArchived) return@launch

                val today = java.time.LocalDate.now()
                val todayCompletion = db.habitCompletionDao().getCompletionByDate(habitId, today)
                if (todayCompletion?.status == CompletionStatus.COMPLETED) {
                    // Already completed today, skip notification and schedule next occurrence
                    ReminderManager(context).scheduleReminder(habit)
                    return@launch
                }

                showNotification(context, habitId, habitTitle)
                ReminderManager(context).scheduleReminder(habit)
            } catch (e: Exception) {
                android.util.Log.e("HabitReminderReceiver", "Error in HabitReminderReceiver: ${e.message}", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun showNotification(context: Context, habitId: String, habitTitle: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(CHANNEL_ID, "Habit reminders", NotificationManager.IMPORTANCE_DEFAULT)
        notificationManager.createNotificationChannel(channel)

        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to_habit_id", habitId)
        }
        val mainPendingIntent = PendingIntent.getActivity(
            context,
            habitId.hashCode(),
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val completeIntent = Intent(context, HabitActionReceiver::class.java).apply {
            action = ACTION_COMPLETE
            putExtra("HABIT_ID", habitId)
        }
        val completePendingIntent = PendingIntent.getBroadcast(
            context,
            habitId.hashCode() + 1,
            completeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val skipIntent = Intent(context, HabitActionReceiver::class.java).apply {
            action = ACTION_SKIP
            putExtra("HABIT_ID", habitId)
        }
        val skipPendingIntent = PendingIntent.getBroadcast(
            context,
            habitId.hashCode() + 2,
            skipIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Public Lock Screen Version (Privacy protected)
        val publicNotification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_logo)
            .setContentTitle("Motion.Pulse reminder")
            .setContentText("Time for your habit")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        // Private Full Version
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_logo)
            .setContentTitle(habitTitle)
            .setContentText("Time for your habit")
            .setContentIntent(mainPendingIntent)
            .addAction(0, "Done", completePendingIntent)
            .addAction(0, "Skip", skipPendingIntent)
            .setPublicVersion(publicNotification)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(habitId.hashCode(), notification)
    }

    private suspend fun rescheduleAllAlarmsSync(context: Context) {
        val db = Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java, "motion_pulse_db"
        ).fallbackToDestructiveMigration().build()

        val allActiveHabits = db.habitDao().getAllActiveHabits().first()
        ReminderManager(context).scheduleAllReminders(allActiveHabits)
    }
}
