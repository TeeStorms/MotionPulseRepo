package com.example.motionpulse.domain.models

import com.example.motionpulse.data.local.entity.FriendEntity
import com.example.motionpulse.data.local.entity.FriendStatus
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

sealed class RelationshipState {
    object None : RelationshipState()
    object Self : RelationshipState()
    object Friends : RelationshipState()
    data class PendingSent(val requestId: String) : RelationshipState()
    data class PendingReceived(val requestId: String, val senderName: String) : RelationshipState()
    data class DeclinedCooldown(val untilDate: String) : RelationshipState()
    object BlockedByMe : RelationshipState()
}

object FriendRelationshipState {

    fun calculateRelationshipState(
        myUid: String,
        targetUid: String,
        friendRequests: List<FriendEntity>,
        myBlockedUids: Set<String>,
        nowMs: Long = System.currentTimeMillis()
    ): RelationshipState {
        if (myUid == targetUid) {
            return RelationshipState.Self
        }
        if (myBlockedUids.contains(targetUid)) {
            return RelationshipState.BlockedByMe
        }

        val docId = if (myUid < targetUid) "${myUid}_$targetUid" else "${targetUid}_$myUid"
        val existing = friendRequests.find { it.id == docId || (it.requesterUid == targetUid || it.recipientUid == targetUid) }

        if (existing == null) {
            return RelationshipState.None
        }

        return when (existing.status) {
            FriendStatus.ACCEPTED -> RelationshipState.Friends
            FriendStatus.PENDING -> {
                if (existing.requesterUid == myUid) {
                    RelationshipState.PendingSent(existing.id)
                } else {
                    RelationshipState.PendingReceived(existing.id, existing.requesterUid)
                }
            }
            FriendStatus.DECLINED -> {
                val respondedAtMs = existing.respondedAt?.toEpochMilli() ?: existing.createdAt.toEpochMilli()
                val sevenDaysMs = 7L * 24 * 60 * 60 * 1000
                if (nowMs - respondedAtMs < sevenDaysMs) {
                    val untilLocalDate = Instant.ofEpochMilli(respondedAtMs + sevenDaysMs)
                        .atZone(ZoneId.systemDefault()).toLocalDate()
                    RelationshipState.DeclinedCooldown(untilLocalDate.toString())
                } else {
                    RelationshipState.None
                }
            }
        }
    }
}
