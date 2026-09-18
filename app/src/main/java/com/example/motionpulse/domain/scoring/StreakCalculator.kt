package com.example.motionpulse.domain.scoring

import com.example.motionpulse.data.local.entity.CompletionStatus
import com.example.motionpulse.data.local.entity.HabitCompletionEntity
import com.example.motionpulse.domain.models.FrequencyConfig
import java.time.LocalDate

object StreakCalculator {

    fun calculateCurrentStreak(
        completions: List<HabitCompletionEntity>,
        frequency: FrequencyConfig,
        today: LocalDate = LocalDate.now()
    ): Int {
        if (completions.isEmpty()) return 0

        val completionMap = completions.associateBy { it.date }
        var streak = 0
        var currentDate = today

        // If today is not completed and not scheduled, check yesterday
        if (completionMap[today]?.status != CompletionStatus.COMPLETED && !frequency.isScheduled(today)) {
            currentDate = today.minusDays(1)
        }

        while (true) {
            val completion = completionMap[currentDate]
            val isScheduled = frequency.isScheduled(currentDate)

            if (isScheduled) {
                if (completion?.status == CompletionStatus.COMPLETED) {
                    streak++
                } else {
                    // Streak broken if a scheduled day is not completed
                    // Unless it's today and we haven't finished the day yet? 
                    // For simplicity, we assume if it's not COMPLETED, the streak stops.
                    break
                }
            }
            // If not scheduled, we just skip the day and continue looking back
            currentDate = currentDate.minusDays(1)
            
            // Safety break for very long searches (e.g. 2 years)
            if (streak > 3650) break 
            
            // If we've gone back before the first completion date and haven't found a break, we might stop.
            // But we check until we find a break.
            if (currentDate.isBefore(today.minusYears(10))) break
        }

        return streak
    }

    fun calculateLongestStreak(
        completions: List<HabitCompletionEntity>,
        frequency: FrequencyConfig
    ): Int {
        if (completions.isEmpty()) return 0

        val sortedDates = completions
            .filter { it.status == CompletionStatus.COMPLETED }
            .map { it.date }
            .sorted()

        if (sortedDates.isEmpty()) return 0

        var maxStreak = 0
        var currentStreak = 0
        
        // This is more complex because we need to check every day in the range
        val minDate = sortedDates.first()
        val maxDate = sortedDates.last()
        var date = minDate
        
        while (!date.isAfter(maxDate)) {
            if (frequency.isScheduled(date)) {
                val wasCompleted = completions.any { it.date == date && it.status == CompletionStatus.COMPLETED }
                if (wasCompleted) {
                    currentStreak++
                    if (currentStreak > maxStreak) maxStreak = currentStreak
                } else {
                    currentStreak = 0
                }
            }
            date = date.plusDays(1)
        }

        return maxStreak
    }
}
