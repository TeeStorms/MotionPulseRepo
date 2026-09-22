package com.example.motionpulse.domain.scoring

import com.example.motionpulse.data.local.entity.CompletionStatus
import com.example.motionpulse.data.local.entity.FrequencyType
import com.example.motionpulse.data.local.entity.HabitCompletionEntity
import com.example.motionpulse.domain.models.FrequencyConfig
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class StreakCalculatorTest {

    @Test
    fun `consecutive daily completions increase current streak`() {
        val freq = FrequencyConfig(FrequencyType.EVERY_DAY)
        val today = LocalDate.of(2023, 10, 25)
        val completions = listOf(
            HabitCompletionEntity("1", "h1", today, CompletionStatus.COMPLETED),
            HabitCompletionEntity("2", "h1", today.minusDays(1), CompletionStatus.COMPLETED),
            HabitCompletionEntity("3", "h1", today.minusDays(2), CompletionStatus.COMPLETED)
        )
        
        val streak = StreakCalculator.calculateCurrentStreak(completions, freq, today)
        assertEquals(3, streak)
    }

    @Test
    fun `streak continues with frequency gaps`() {
        // Mon, Wed scheduled
        val freq = FrequencyConfig(
            type = FrequencyType.SPECIFIC_DAYS,
            specificDays = setOf(java.time.DayOfWeek.MONDAY, java.time.DayOfWeek.WEDNESDAY)
        )
        
        val mon = LocalDate.of(2023, 10, 23)
        val tue = LocalDate.of(2023, 10, 24)
        val wed = LocalDate.of(2023, 10, 25)
        
        val completions = listOf(
            HabitCompletionEntity("1", "h1", mon, CompletionStatus.COMPLETED),
            // Tue skipped (not scheduled)
            HabitCompletionEntity("2", "h1", wed, CompletionStatus.COMPLETED)
        )
        
        // As of Wednesday
        val streak = StreakCalculator.calculateCurrentStreak(completions, freq, wed)
        assertEquals(2, streak)
    }

    @Test
    fun `miss on scheduled day breaks streak`() {
        val freq = FrequencyConfig(FrequencyType.EVERY_DAY)
        val today = LocalDate.of(2023, 10, 25)
        val completions = listOf(
            HabitCompletionEntity("1", "h1", today, CompletionStatus.COMPLETED),
            HabitCompletionEntity("2", "h1", today.minusDays(1), CompletionStatus.MISSED),
            HabitCompletionEntity("3", "h1", today.minusDays(2), CompletionStatus.COMPLETED)
        )
        
        val streak = StreakCalculator.calculateCurrentStreak(completions, freq, today)
        assertEquals(1, streak)
    }

    @Test
    fun `partial on scheduled day breaks streak`() {
        val freq = FrequencyConfig(FrequencyType.EVERY_DAY)
        val today = LocalDate.of(2023, 10, 25)
        val completions = listOf(
            HabitCompletionEntity("1", "h1", today, CompletionStatus.COMPLETED),
            HabitCompletionEntity("2", "h1", today.minusDays(1), CompletionStatus.MISSED, numericValueLogged = 10f),
            HabitCompletionEntity("3", "h1", today.minusDays(2), CompletionStatus.COMPLETED)
        )
        
        val streak = StreakCalculator.calculateCurrentStreak(completions, freq, today)
        assertEquals(1, streak)
    }

    @Test
    fun `longest streak calculation`() {
        val freq = FrequencyConfig(FrequencyType.EVERY_DAY)
        val start = LocalDate.of(2023, 10, 20)
        val completions = listOf(
            HabitCompletionEntity("1", "h1", start, CompletionStatus.COMPLETED),
            HabitCompletionEntity("2", "h1", start.plusDays(1), CompletionStatus.COMPLETED),
            HabitCompletionEntity("3", "h1", start.plusDays(2), CompletionStatus.MISSED),
            HabitCompletionEntity("4", "h1", start.plusDays(3), CompletionStatus.COMPLETED),
            HabitCompletionEntity("5", "h1", start.plusDays(4), CompletionStatus.COMPLETED),
            HabitCompletionEntity("6", "h1", start.plusDays(5), CompletionStatus.COMPLETED)
        )
        
        val longest = StreakCalculator.calculateLongestStreak(completions, freq)
        assertEquals(3, longest)
    }
}
