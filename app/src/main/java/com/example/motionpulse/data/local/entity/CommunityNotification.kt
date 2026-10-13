package com.example.motionpulse.data.local.entity

import com.google.firebase.firestore.PropertyName

data class CommunityNotification(
    val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val type: String = "NUDGE", // NUDGE, DUEL_INVITE, DUEL_RESULT, FRIEND_REQUEST, FRIEND_ACCEPTED
    val timestamp: Long = System.currentTimeMillis(),
    @get:PropertyName("isRead") @set:PropertyName("isRead") var isRead: Boolean = false,
    val habitType: String? = null,
    val duelId: String? = null,
    val message: String? = null
)
