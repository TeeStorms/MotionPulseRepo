package com.example.motionpulse

import com.example.motionpulse.data.local.entity.FrequencyType
import com.example.motionpulse.data.local.entity.HabitEntity
import com.example.motionpulse.domain.models.FrequencyConfig
import com.example.motionpulse.domain.models.ReminderScheduler
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class NextOccurrenceTest {

    private val zone = ZoneId.of("Africa/Johannesburg")

    @Test
    fun habitNoReminderTime_returnsNull() {
        val habit = HabitEntity(reminderTime = null)
        val next = ReminderScheduler.nextOccurrence(habit)
        assertNull(next)
    }

    @Test
    fun everyDayHabit_beforeTime_returnsToday() {
        val habit = HabitEntity(
            reminderTime = "09:00",
            frequencyConfig = Json.encodeToString(FrequencyConfig(FrequencyType.EVERY_DAY))
        )
        val now = ZonedDateTime.of(LocalDate.of(2026, 3, 15), LocalTime.of(8, 0), zone)
        val next = ReminderScheduler.nextOccurrence(habit, now, zone)

        assertNotNull(next)
        assertEquals(LocalDate.of(2026, 3, 15), next!!.toLocalDate())
        assertEquals(LocalTime.of(9, 0), next.toLocalTime())
    }

    @Test
    fun everyDayHabit_afterTime_returnsTomorrow() {
        val habit = HabitEntity(
            reminderTime = "09:00",
            frequencyConfig = Json.encodeToString(FrequencyConfig(FrequencyType.EVERY_DAY))
        )
        val now = ZonedDateTime.of(LocalDate.of(2026, 3, 15), LocalTime.of(10, 0), zone)
        val next = ReminderScheduler.nextOccurrence(habit, now, zone)

        assertNotNull(next)
        assertEquals(LocalDate.of(2026, 3, 16), next!!.toLocalDate())
        assertEquals(LocalTime.of(9, 0), next.toLocalTime())
    }

    @Test
    fun monthEndTransition_calculatesCorrectNextMonthDate() {
        val habit = HabitEntity(
            reminderTime = "09:00",
            frequencyConfig = Json.encodeToString(FrequencyConfig(FrequencyType.EVERY_DAY))
        )
        val now = ZonedDateTime.of(LocalDate.of(2026, 3, 31), LocalTime.of(10, 0), zone)
        val next = ReminderScheduler.nextOccurrence(habit, now, zone)

        assertNotNull(next)
        assertEquals(LocalDate.of(2026, 4, 1), next!!.toLocalDate())
    }

    @Test
    fun timeZoneChange_preservesExactLocalTimeInTargetZone() {
        val targetZone = ZoneId.of("Europe/London")
        val habit = HabitEntity(
            reminderTime = "09:00",
            frequencyConfig = Json.encodeToString(FrequencyConfig(FrequencyType.EVERY_DAY))
        )
        val now = ZonedDateTime.of(LocalDate.of(2026, 3, 15), LocalTime.of(8, 0), targetZone)
        val next = ReminderScheduler.nextOccurrence(habit, now, targetZone)

        assertNotNull(next)
        assertEquals(targetZone, next!!.zone)
        assertEquals(LocalTime.of(9, 0), next.toLocalTime())
    }
}
