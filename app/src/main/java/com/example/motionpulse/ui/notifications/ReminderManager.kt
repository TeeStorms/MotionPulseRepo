package com.example.motionpulse.ui.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.motionpulse.data.local.entity.HabitEntity
import java.time.LocalTime
import java.time.ZonedDateTime

class ReminderManager(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleReminder(habit: HabitEntity) {
        val reminderTimeStr = habit.reminderTime ?: return
        val reminderTime = LocalTime.parse(reminderTimeStr)
        
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

        val nextRun = ZonedDateTime.now().with(reminderTime)
        val finalRun = if (nextRun.isBefore(ZonedDateTime.now())) nextRun.plusDays(1) else nextRun

        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                finalRun.toInstant().toEpochMilli(),
                pendingIntent
            )
        } catch (e: SecurityException) {
            // Handle lack of exact alarm permission
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
}
