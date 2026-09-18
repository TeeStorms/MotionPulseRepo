package com.example.motionpulse.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

enum class BadgeType {
    STREAK_7_DAY, STREAK_30_DAY, FIRST_HABIT_CREATED
}

@Entity(tableName = "badges")
data class BadgeEntity(
    @PrimaryKey val id: String = "",
    val userId: String = "",
    val badgeType: BadgeType = BadgeType.STREAK_7_DAY,
    val unlockedAt: Instant = Instant.now(),
    val triggeredByCompletionId: String? = null
)
