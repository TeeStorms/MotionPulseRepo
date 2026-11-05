package com.example.motionpulse

import com.example.motionpulse.data.local.entity.CompletionStatus
import com.example.motionpulse.data.local.entity.FrequencyType
import com.example.motionpulse.data.local.entity.HabitCompletionEntity
import com.example.motionpulse.domain.models.FrequencyConfig
import com.example.motionpulse.domain.scoring.HabitScoreCalculator
import com.example.motionpulse.util.HabitStrengthFormatter
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class HabitStrengthFormatterTest {

    @Test
    fun toPercent_specificValues_formattedAndClampedCorrectly() {
        assertEquals(0, HabitStrengthFormatter.toPercent(0f))
        assertEquals(15, HabitStrengthFormatter.toPercent(14.5f))
        assertEquals(50, HabitStrengthFormatter.toPercent(50f))
        assertEquals(100, HabitStrengthFormatter.toPercent(99.6f))
        assertEquals(100, HabitStrengthFormatter.toPercent(100f))
        assertEquals(100, HabitStrengthFormatter.toPercent(150f))
        assertEquals(0, HabitStrengthFormatter.toPercent(-10f))
        assertEquals(0, HabitStrengthFormatter.toPercent(Float.NaN))
        assertEquals(0, HabitStrengthFormatter.toPercent(Float.POSITIVE_INFINITY))
        assertEquals(0, HabitStrengthFormatter.toPercent(null))
    }

    @Test
    fun toPercent_realCalculatorOutput_formatsCorrectly() {
        val dailyFreq = FrequencyConfig(FrequencyType.EVERY_DAY)

        // No completions -> 0%
        val scoreEmpty = HabitScoreCalculator.calculateScore(0f, emptyList(), dailyFreq, null)
        assertEquals(0, HabitStrengthFormatter.toPercent(scoreEmpty))

        // Single completion -> 5%
        val singleComp = listOf(HabitCompletionEntity("1", "h1", LocalDate.now(), CompletionStatus.COMPLETED))
        val scoreSingle = HabitScoreCalculator.calculateScore(0f, singleComp, dailyFreq, null)
        assertEquals(5, HabitStrengthFormatter.toPercent(scoreSingle))

        // Long streak of 20 completions
        val streakCompletions = (1..20).map { i ->
            HabitCompletionEntity(i.toString(), "h1", LocalDate.now().minusDays(i.toLong()), CompletionStatus.COMPLETED)
        }
        val scoreStreak = HabitScoreCalculator.calculateScore(0f, streakCompletions, dailyFreq, null)
        assertEquals(64, HabitStrengthFormatter.toPercent(scoreStreak))
    }
}
