package com.example.motionpulse.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

enum class FriendStatus {
    PENDING, ACCEPTED
}

@Entity(tableName = "friends")
data class FriendEntity(
    @PrimaryKey val id: String = "",
    val ownerUid: String = "",
    val friendUid: String = "",
    val status: FriendStatus = FriendStatus.PENDING,
    val addedAt: Instant = Instant.now()
)
