package com.example.motionpulse.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

enum class FriendStatus {
    PENDING, ACCEPTED, DECLINED
}

@Entity(tableName = "friends")
data class FriendEntity(
    @PrimaryKey val id: String = "",
    val requesterUid: String = "",
    val recipientUid: String = "",
    val status: FriendStatus = FriendStatus.PENDING,
    val createdAt: Instant = Instant.now(),
    val respondedAt: Instant? = null
)
