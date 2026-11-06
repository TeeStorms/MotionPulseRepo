package com.example.motionpulse.data.local.entity

import com.google.firebase.firestore.PropertyName

data class PublicProfileEntity(
    @get:PropertyName("uid") @set:PropertyName("uid") var uid: String = "",
    @get:PropertyName("displayName") @set:PropertyName("displayName") var displayName: String = "",
    @get:PropertyName("avatarUrl") @set:PropertyName("avatarUrl") var avatarUrl: String? = null,
    @get:PropertyName("totalAuraXp") @set:PropertyName("totalAuraXp") var totalAuraXp: Long = 0,
    @get:PropertyName("currentLevel") @set:PropertyName("currentLevel") var currentLevel: Int = 1,
    @get:PropertyName("currentStreak") @set:PropertyName("currentStreak") var currentStreak: Int = 0,
    @get:PropertyName("recentXp") @set:PropertyName("recentXp") var recentXp: Map<String, Long> = emptyMap()
)
