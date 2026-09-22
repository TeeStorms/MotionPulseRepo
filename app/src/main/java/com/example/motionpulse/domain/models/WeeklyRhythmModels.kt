package com.example.motionpulse.domain.models

import com.example.motionpulse.data.local.entity.MoodLevel

data class DayMoodHabitData(
    val dayLabel: String,
    val moodScore: Int, // 1 to 5
    val habitsCompleted: Int,
    val totalHabits: Int = 3
)

data class WeeklyRhythmUiState(
    val days: List<DayMoodHabitData> = emptyList()
)
