package com.example.motionpulse.domain.models

import com.example.motionpulse.data.local.entity.FriendEntity
import com.example.motionpulse.data.local.entity.FriendStatus

data class CategorizedFriendRequests(
    val incoming: List<FriendEntity> = emptyList(),
    val sent: List<FriendEntity> = emptyList(),
    val friends: List<FriendEntity> = emptyList()
)

object FriendRequestState {

    fun splitFriendRequests(requests: List<FriendEntity>, myUid: String): CategorizedFriendRequests {
        val incoming = mutableListOf<FriendEntity>()
        val sent = mutableListOf<FriendEntity>()
        val friends = mutableListOf<FriendEntity>()

        for (req in requests) {
            when (req.status) {
                FriendStatus.PENDING -> {
                    if (req.recipientUid == myUid) {
                        incoming.add(req)
                    } else if (req.requesterUid == myUid) {
                        sent.add(req)
                    }
                }
                FriendStatus.ACCEPTED -> {
                    friends.add(req)
                }
                FriendStatus.DECLINED -> {
                    // Ignored in active lists
                }
            }
        }

        return CategorizedFriendRequests(incoming = incoming, sent = sent, friends = friends)
    }
}
