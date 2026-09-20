package com.example.motionpulse.data.local.entity

import java.time.Instant

enum class FeedEventType {
    STREAK_MILESTONE,
    ALL_HABITS_COMPLETED,
    STREAK_AT_RISK
}

data class ActivityFeedEntry(
    val id: String = "",
    val actorId: String = "",
    val actorName: String = "",
    val actorAvatarUrl: String? = null,
    val eventType: FeedEventType = FeedEventType.ALL_HABITS_COMPLETED,
    val timestamp: Long = System.currentTimeMillis(),
    val habitTitle: String? = null,
    val streakCount: Int? = null,
    val reactions: Map<String, Boolean> = emptyMap() // userId -> hasReacted
)
