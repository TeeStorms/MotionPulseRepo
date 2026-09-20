package com.example.motionpulse.data.local.entity

data class CommunityNotification(
    val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val type: String = "NUDGE",
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)
