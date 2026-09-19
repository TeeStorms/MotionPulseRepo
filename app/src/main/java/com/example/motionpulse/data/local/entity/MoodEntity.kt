package com.example.motionpulse.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate

enum class MoodLevel {
    DRAINED, LOW, STEADY, GOOD, ENERGIZED
}

enum class MoodFactor {
    SLEEP, WORK, HEALTH, SOCIAL, WEATHER
}

@Entity(tableName = "moods")
data class MoodEntity(
    @PrimaryKey val id: String = "",
    val userId: String = "",
    val date: LocalDate = LocalDate.now(),
    val moodLevel: MoodLevel = MoodLevel.STEADY,
    val factors: List<MoodFactor> = emptyList(),
    val note: String? = null,
    val loggedAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
)
