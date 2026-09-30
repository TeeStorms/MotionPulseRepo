package com.example.motionpulse.data.local.entity

data class PublicProfileEntity(
    val uid: String = "",
    val displayName: String = "",
    val avatarUrl: String? = null,
    val totalAuraXp: Long = 0,
    val currentLevel: Int = 1,
    val currentStreak: Int = 0,
    val friendCode: String = "",
    val recentXp: Map<String, Long> = emptyMap()
)
