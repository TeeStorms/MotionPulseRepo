package com.example.motionpulse

import com.example.motionpulse.data.local.entity.FrequencyType
import com.example.motionpulse.data.local.entity.HabitEntity
import com.example.motionpulse.domain.models.FrequencyConfig
import com.example.motionpulse.domain.stats.StatsCalculations
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class StatsCalculationsTest {

    @Test
    fun formatDoneToday_zeroScheduled_returnsDashDash() {
        assertEquals("--", StatsCalculations.formatDoneToday(0, 0))
        assertEquals("3/5", StatsCalculations.formatDoneToday(3, 5))
    }

    @Test
    fun isScheduled_respectsArchivedAndCreationDate() {
        val habit = HabitEntity(
            isArchived = true,
            createdAt = Instant.now(),
            frequencyConfig = Json.encodeToString(FrequencyConfig(FrequencyType.EVERY_DAY))
        )
        assertFalse(StatsCalculations.isScheduled(habit, LocalDate.now()))
    }

    @Test
    fun calculateWeeklyTrend_zeroPrevScheduled_returnsDashDash() {
        assertEquals("--", StatsCalculations.calculateWeeklyTrend(80, 0, 0))
        assertEquals("+10%", StatsCalculations.calculateWeeklyTrend(90, 10, 8))
        assertEquals("-5%", StatsCalculations.calculateWeeklyTrend(75, 10, 8))
    }
}
