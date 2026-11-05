package com.example.motionpulse.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.motionpulse.R
import java.time.Instant
import java.time.LocalDate

enum class MoodLevel {
    DRAINED, LOW, STEADY, GOOD, ENERGIZED;

    val labelRes: Int
        get() = when (this) {
            DRAINED -> R.string.mood_level_drained
            LOW -> R.string.mood_level_low
            STEADY -> R.string.mood_level_steady
            GOOD -> R.string.mood_level_good
            ENERGIZED -> R.string.mood_level_energized
        }
}

enum class MoodFactor {
    SLEEP, WORK, HEALTH, SOCIAL, WEATHER;

    val labelRes: Int
        get() = when (this) {
            SLEEP -> R.string.mood_factor_sleep
            WORK -> R.string.mood_factor_work
            HEALTH -> R.string.mood_factor_health
            SOCIAL -> R.string.mood_factor_social
            WEATHER -> R.string.mood_factor_weather
        }
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
