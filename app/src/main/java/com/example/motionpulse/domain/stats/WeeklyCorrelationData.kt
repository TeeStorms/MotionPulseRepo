package com.example.motionpulse.domain.stats

import com.example.motionpulse.data.local.entity.MoodLevel
import java.time.LocalDate

data class DayData(
    val date: LocalDate,
    val moodLevel: MoodLevel?,
    val habitCompletions: List<Boolean> // true = completed, false = scheduled but incomplete
)

data class WeeklyCorrelationData(
    val days: List<DayData> = emptyList()
)
