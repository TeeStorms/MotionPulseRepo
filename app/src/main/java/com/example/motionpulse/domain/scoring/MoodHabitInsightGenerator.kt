package com.example.motionpulse.domain.scoring

import com.example.motionpulse.data.local.entity.HabitCompletionEntity
import com.example.motionpulse.data.local.entity.HabitEntity
import com.example.motionpulse.data.local.entity.MoodEntity
import com.example.motionpulse.data.local.entity.MoodLevel
import java.time.LocalDate

object MoodHabitInsightGenerator {

    fun generateInsight(
        moods: List<MoodEntity>,
        habits: List<HabitEntity>,
        completions: List<HabitCompletionEntity>
    ): String {
        if (moods.size < 5) {
            return "Log a few more days of mood to see how your habits shape your rhythm."
        }

        // 1. Identify high mood days
        val highMoodDates = moods.filter { 
            it.moodLevel == MoodLevel.GOOD || it.moodLevel == MoodLevel.ENERGIZED 
        }.map { it.date }.toSet()

        if (highMoodDates.isEmpty()) {
            return "Keep tracking! We'll show you which habits boost your mood as soon as we detect your rhythm peaks."
        }

        // 2. Find the habit most frequently completed on high mood days
        val habitCorrelation = habits.map { habit ->
            val habitCompletions = completions.filter { it.habitId == habit.id }
            val countOnHighMood = habitCompletions.count { it.date in highMoodDates }
            val totalCompletions = habitCompletions.size
            
            val score = if (totalCompletions > 0) countOnHighMood.toFloat() / totalCompletions else 0f
            habit.title to score
        }.filter { it.second > 0 }.maxByOrNull { it.second }

        // 3. Find lowest mood weekday
        val weekdayAverages = moods.groupBy { it.date.dayOfWeek }
            .mapValues { (_, entries) -> 
                entries.map { moodToScore(it.moodLevel) }.average()
            }
        
        val lowestDay = weekdayAverages.minByOrNull { it.value }?.key

        val habitPart = if (habitCorrelation != null) {
            "Your mood peaks on days you **${habitCorrelation.first}**."
        } else ""

        val dayPart = if (lowestDay != null) {
            val dayName = lowestDay.name.lowercase().replaceFirstChar { it.uppercase() } + "s"
            "$dayName average your lowest rhythm — maybe ease into the week instead of starting cold."
        } else ""

        return "$habitPart $dayPart".trim()
    }

    private fun moodToScore(level: MoodLevel): Int = when (level) {
        MoodLevel.DRAINED -> 1
        MoodLevel.LOW -> 2
        MoodLevel.STEADY -> 3
        MoodLevel.GOOD -> 4
        MoodLevel.ENERGIZED -> 5
    }
}
