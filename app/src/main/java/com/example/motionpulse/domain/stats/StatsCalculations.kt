package com.example.motionpulse.domain.stats

import com.example.motionpulse.data.local.entity.CompletionStatus
import com.example.motionpulse.data.local.entity.HabitCompletionEntity
import com.example.motionpulse.data.local.entity.HabitEntity
import com.example.motionpulse.domain.models.FrequencyConfig
import java.time.LocalDate

object StatsCalculations {

    fun isScheduled(habit: HabitEntity, date: LocalDate): Boolean {
        if (habit.isArchived) return false
        val createdDate = habit.createdAt.atZone(java.time.ZoneId.systemDefault()).toLocalDate()
        if (date.isBefore(createdDate)) return false
        val config = FrequencyConfig.decodeSafe(habit.frequencyConfig)
        return config.isScheduled(date)
    }

    fun formatDoneToday(completed: Int, scheduled: Int): String {
        return if (scheduled > 0) "$completed/$scheduled" else "--"
    }

    fun calculateWeeklyPercentage(
        habits: List<HabitEntity>,
        completions: List<HabitCompletionEntity>,
        startOfWeek: LocalDate,
        endOfRange: LocalDate
    ): Int {
        val habitCompletionsMap = completions.groupBy { it.habitId }
        var totalScheduled = 0
        var totalCompleted = 0

        val dateRange = generateSequence(startOfWeek) { it.plusDays(1) }
            .takeWhile { !it.isAfter(endOfRange) }
            .toList()

        for (habit in habits) {
            val habitComps = habitCompletionsMap[habit.id] ?: emptyList()
            for (date in dateRange) {
                if (isScheduled(habit, date)) {
                    totalScheduled++
                    if (habitComps.any { it.date == date && it.status == CompletionStatus.COMPLETED }) {
                        totalCompleted++
                    }
                }
            }
        }

        return if (totalScheduled > 0) {
            (totalCompleted.toFloat() / totalScheduled * 100).toInt().coerceIn(0, 100)
        } else {
            0
        }
    }

    fun calculateWeeklyTrend(currentPct: Int, prevScheduled: Int, prevCompleted: Int): String {
        if (prevScheduled <= 0) return "--"
        val prevPct = (prevCompleted.toFloat() / prevScheduled * 100).toInt()
        val diff = currentPct - prevPct
        return if (diff >= 0) "+$diff%" else "$diff%"
    }
}
