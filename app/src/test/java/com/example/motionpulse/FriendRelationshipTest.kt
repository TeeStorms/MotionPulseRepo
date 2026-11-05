package com.example.motionpulse

import com.example.motionpulse.data.local.entity.FriendEntity
import com.example.motionpulse.data.local.entity.FriendStatus
import com.example.motionpulse.domain.models.FriendRelationshipState
import com.example.motionpulse.domain.models.RelationshipState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class FriendRelationshipTest {

    private val now = System.currentTimeMillis()

    @Test
    fun calculateRelationshipState_self_returnsSelf() {
        val res = FriendRelationshipState.calculateRelationshipState("alice", "alice", emptyList(), emptySet(), now)
        assertEquals(RelationshipState.Self, res)
    }

    @Test
    fun calculateRelationshipState_blocked_returnsBlockedByMe() {
        val res = FriendRelationshipState.calculateRelationshipState("alice", "bob", emptyList(), setOf("bob"), now)
        assertEquals(RelationshipState.BlockedByMe, res)
    }

    @Test
    fun calculateRelationshipState_accepted_returnsFriends() {
        val requests = listOf(
            FriendEntity(id = "alice_bob", requesterUid = "alice", recipientUid = "bob", status = FriendStatus.ACCEPTED)
        )
        val res = FriendRelationshipState.calculateRelationshipState("alice", "bob", requests, emptySet(), now)
        assertEquals(RelationshipState.Friends, res)
    }

    @Test
    fun calculateRelationshipState_pendingSent_returnsPendingSent() {
        val requests = listOf(
            FriendEntity(id = "alice_bob", requesterUid = "alice", recipientUid = "bob", status = FriendStatus.PENDING)
        )
        val res = FriendRelationshipState.calculateRelationshipState("alice", "bob", requests, emptySet(), now)
        assertTrue(res is RelationshipState.PendingSent)
    }

    @Test
    fun calculateRelationshipState_pendingReceived_returnsPendingReceived() {
        val requests = listOf(
            FriendEntity(id = "alice_bob", requesterUid = "bob", recipientUid = "alice", status = FriendStatus.PENDING)
        )
        val res = FriendRelationshipState.calculateRelationshipState("alice", "bob", requests, emptySet(), now)
        assertTrue(res is RelationshipState.PendingReceived)
    }

    @Test
    fun calculateRelationshipState_declinedRecent_returnsDeclinedCooldown() {
        val requests = listOf(
            FriendEntity(id = "alice_bob", requesterUid = "alice", recipientUid = "bob", status = FriendStatus.DECLINED, respondedAt = Instant.ofEpochMilli(now - 1000L))
        )
        val res = FriendRelationshipState.calculateRelationshipState("alice", "bob", requests, emptySet(), now)
        assertTrue(res is RelationshipState.DeclinedCooldown)
    }

    @Test
    fun calculateRelationshipState_noRequestOrPostUnfriend_returnsNone() {
        val res = FriendRelationshipState.calculateRelationshipState("alice", "bob", emptyList(), emptySet(), now)
        assertEquals(RelationshipState.None, res)
    }
}
