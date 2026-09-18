package com.example.motionpulse.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

enum class GoalType {
    YES_NO, NUMERIC
}

enum class FrequencyType {
    EVERY_DAY, X_TIMES_PER_WEEK, SPECIFIC_DAYS, EVERY_OTHER_DAY
}

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey val id: String = "",
    val ownerId: String = "",
    val title: String = "",
    val category: String = "",
    val colorTag: String = "",
    val goalType: GoalType = GoalType.YES_NO,
    val numericTarget: Float? = null,
    val numericUnit: String? = null,
    val frequencyType: FrequencyType = FrequencyType.EVERY_DAY,
    val frequencyConfig: String = "", // JSON serialized
    val reminderTime: String? = null, // Stored as "HH:mm"
    val habitStrengthScore: Float = 0f,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val manualOrder: Int = 0,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
    val isArchived: Boolean = false
)
