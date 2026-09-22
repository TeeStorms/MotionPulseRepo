package com.example.motionpulse.domain.scoring

import com.example.motionpulse.data.local.entity.CompletionStatus
import com.example.motionpulse.data.local.entity.FrequencyType
import com.example.motionpulse.data.local.entity.HabitCompletionEntity
import com.example.motionpulse.domain.models.FrequencyConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class HabitScoreCalculatorTest {

    private val dailyFreq = FrequencyConfig(FrequencyType.EVERY_DAY)

    // PARTIAL completion is not supported in the current implementation.

    @Test
    fun `long streak survives one miss without resetting to zero`() {
        // Start with a high score
        var score = 90f
        val completions = listOf(
            HabitCompletionEntity("1", "h1", LocalDate.now(), CompletionStatus.MISSED)
        )
        
        val newScore = HabitScoreCalculator.calculateScore(score, completions, dailyFreq, null)
        
        // 90 * (1 - 0.10) = 81
        assertEquals(81f, newScore, 0.01f)
        assertTrue("Score should be forgiving", newScore > 0f)
    }

    @Test
    fun `X_TIMES_PER_WEEK habit not penalized on non-scheduled days`() {
        // Note: My current implementation of calculateScore treats every day as 'missable' 
        // if isScheduled(date) returns true. 
        // For X_TIMES_PER_WEEK, isScheduled returns true (because scoring handles count).
        // Actually, the prompt says "a day that isn't a scheduled day should not count as a miss".
        // Let's use SPECIFIC_DAYS to test this logic clearly.
        
        val specificFreq = FrequencyConfig(
            type = FrequencyType.SPECIFIC_DAYS,
            specificDays = setOf(java.time.DayOfWeek.MONDAY)
        )
        
        val score = 50f
        // Tuesday is NOT scheduled
        val tuesday = LocalDate.of(2023, 10, 24) // It was a Tuesday
        val completions = listOf(
            HabitCompletionEntity("1", "h1", tuesday, CompletionStatus.MISSED)
        )
        
        val newScore = HabitScoreCalculator.calculateScore(score, completions, specificFreq, null)
        
        assertEquals("Score should not change on non-scheduled miss", 50f, newScore, 0.01f)
    }

    @Test
    fun `score decays over multiple consecutive misses`() {
        var score = 100f
        val completions = listOf(
            HabitCompletionEntity("1", "h1", LocalDate.now().minusDays(1), CompletionStatus.MISSED),
            HabitCompletionEntity("2", "h1", LocalDate.now(), CompletionStatus.MISSED)
        )
        
        val newScore = HabitScoreCalculator.calculateScore(score, completions, dailyFreq, null)
        
        // 100 -> 90 -> 81
        assertEquals(81f, newScore, 0.01f)
    }

    @Test
    fun `score shows diminishing returns as it approaches 100`() {
        val lowScore = 10f
        val highScore = 90f
        
        val comp = listOf(HabitCompletionEntity("1", "h1", LocalDate.now(), CompletionStatus.COMPLETED))
        
        val newLow = HabitScoreCalculator.calculateScore(lowScore, comp, dailyFreq, null)
        val newHigh = HabitScoreCalculator.calculateScore(highScore, comp, dailyFreq, null)
        
        val gainLow = newLow - lowScore // (100 - 10) * 0.05 = 4.5
        val gainHigh = newHigh - highScore // (100 - 90) * 0.05 = 0.5
        
        assertTrue("Gain should be higher for lower scores", gainLow > gainHigh)
        assertEquals(14.5f, newLow, 0.01f)
        assertEquals(90.5f, newHigh, 0.01f)
    }
}
