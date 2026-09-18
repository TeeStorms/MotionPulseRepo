package com.example.motionpulse.domain.scoring

import com.example.motionpulse.data.local.entity.CompletionStatus
import com.example.motionpulse.data.local.entity.HabitCompletionEntity
import com.example.motionpulse.domain.models.FrequencyConfig
import java.time.LocalDate
import kotlin.math.max
import kotlin.math.min

object HabitScoreCalculator {

    private const val COMPLETION_GAIN_FACTOR = 0.05f // Learning rate
    private const val MISS_DECAY_FACTOR = 0.10f      // Forgiving decay rate

    fun calculateScore(
        previousScore: Float,
        completions: List<HabitCompletionEntity>,
        frequency: FrequencyConfig,
        numericTarget: Float? = null
    ): Float {
        if (completions.isEmpty()) return previousScore

        val sortedCompletions = completions.sortedBy { it.date }
        var currentScore = previousScore

        // We process the completions provided. 
        // In a real app, this might be a single day's update or a range update.
        for (completion in sortedCompletions) {
            val isScheduled = frequency.isScheduled(completion.date)

            when (completion.status) {
                CompletionStatus.COMPLETED -> {
                    // Diminishing returns: score increases more when low, less when high
                    currentScore += (100f - currentScore) * COMPLETION_GAIN_FACTOR
                }
                CompletionStatus.MISSED -> {
                    // Only penalize if it was a scheduled day
                    if (isScheduled) {
                        // Proportional decay: higher scores drop more points, but don't hit zero instantly
                        currentScore *= (1 - MISS_DECAY_FACTOR)
                    }
                }
                CompletionStatus.SKIPPED -> {
                    // No change for skipped days
                }
            }
        }

        return min(100f, max(0f, currentScore))
    }
}
