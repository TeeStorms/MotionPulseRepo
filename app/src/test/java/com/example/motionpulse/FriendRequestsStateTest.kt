package com.example.motionpulse

import com.example.motionpulse.data.local.entity.FriendEntity
import com.example.motionpulse.data.local.entity.FriendStatus
import com.example.motionpulse.domain.models.FriendRequestState
import org.junit.Assert.assertEquals
import org.junit.Test

class FriendRequestsStateTest {

    @Test
    fun splitFriendRequests_categorizesCorrectly() {
        val requests = listOf(
            FriendEntity(id = "1", requesterUid = "bob", recipientUid = "alice", status = FriendStatus.PENDING),
            FriendEntity(id = "2", requesterUid = "alice", recipientUid = "charlie", status = FriendStatus.PENDING),
            FriendEntity(id = "3", requesterUid = "dave", recipientUid = "alice", status = FriendStatus.ACCEPTED),
            FriendEntity(id = "4", requesterUid = "eve", recipientUid = "alice", status = FriendStatus.DECLINED)
        )

        val categorized = FriendRequestState.splitFriendRequests(requests, "alice")

        assertEquals(1, categorized.incoming.size)
        assertEquals("bob", categorized.incoming[0].requesterUid)

        assertEquals(1, categorized.sent.size)
        assertEquals("charlie", categorized.sent[0].recipientUid)

        assertEquals(1, categorized.friends.size)
        assertEquals("dave", categorized.friends[0].requesterUid)
    }
}
