package com.example.motionpulse.data.local.entity

data class CommunityNotification(
    val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val type: String = "NUDGE", // NUDGE, DUEL_INVITE, DUEL_RESULT
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val habitType: String? = null,
    val duelId: String? = null,
    val message: String? = null
)
