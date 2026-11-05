package com.example.motionpulse.ui.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.motionpulse.data.local.entity.HabitEntity
import com.example.motionpulse.data.repository.PreferenceRepository
import com.example.motionpulse.domain.models.ReminderScheduler

class ReminderManager(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val prefs = PreferenceRepository(context)

    fun scheduleReminder(habit: HabitEntity) {
        if (!prefs.isRemindersEnabled()) return

        val nextRun = ReminderScheduler.nextOccurrence(habit) ?: return

        val intent = Intent(context, HabitReminderReceiver::class.java).apply {
            putExtra("HABIT_ID", habit.id)
            putExtra("HABIT_TITLE", habit.title)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            habit.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        nextRun.toInstant().toEpochMilli(),
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        nextRun.toInstant().toEpochMilli(),
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    nextRun.toInstant().toEpochMilli(),
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                nextRun.toInstant().toEpochMilli(),
                pendingIntent
            )
        }
    }

    fun scheduleAllReminders(habits: List<HabitEntity>) {
        if (!prefs.isRemindersEnabled()) return
        habits.filter { !it.isArchived && it.reminderTime != null }.forEach {
            scheduleReminder(it)
        }
    }

    fun cancelReminder(habitId: String) {
        val intent = Intent(context, HabitReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            habitId.hashCode(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
        }
    }

    fun cancelAllReminders(habitIds: List<String>) {
        habitIds.forEach { cancelReminder(it) }
    }
}
