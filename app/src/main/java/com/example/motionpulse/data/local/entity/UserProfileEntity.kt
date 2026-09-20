package com.example.motionpulse.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "user_profiles")
data class UserProfileEntity(
    @PrimaryKey val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val avatarUrl: String? = null,
    val totalAuraXp: Long = 0,
    val currentLevel: Int = 1,
    val currentStreak: Int = 0,
    val language: String = "English",
    val remindersEnabled: Boolean = true,
    val fcmToken: String? = null,
    val recentXp: Map<String, Long> = emptyMap(), // date -> xp gained
    val createdAt: Instant = Instant.now()
)
