package com.example.motionpulse.domain.models

import com.example.motionpulse.data.local.entity.HabitEntity
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

object ReminderScheduler {

    fun nextOccurrence(
        habit: HabitEntity,
        now: ZonedDateTime = ZonedDateTime.now(),
        zone: ZoneId = now.zone
    ): ZonedDateTime? {
        val reminderTimeStr = habit.reminderTime ?: return null
        val reminderTime = try {
            LocalTime.parse(reminderTimeStr)
        } catch (e: Exception) {
            return null
        }

        val config = FrequencyConfig.decodeSafe(habit.frequencyConfig)
        var targetDate = now.toLocalDate()
        val nowTime = now.toLocalTime()

        // If today is scheduled but time already passed, start searching from tomorrow
        if (config.isScheduled(targetDate) && reminderTime.isBefore(nowTime)) {
            targetDate = targetDate.plusDays(1)
        }

        // Search up to 31 days ahead for next scheduled occurrence
        for (dayIdx in 0..31) {
            if (config.isScheduled(targetDate)) {
                return ZonedDateTime.of(targetDate, reminderTime, zone)
            }
            targetDate = targetDate.plusDays(1)
        }

        return null
    }
}
