package com.example.motionpulse

import com.example.motionpulse.domain.models.FriendEligibility
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FriendEligibilityTest {

    @Test
    fun normalizeFriendCode_formatsCorrectly() {
        assertEquals("MP-1234-5678", FriendEligibility.normalizeFriendCode("mp 1234 5678"))
        assertEquals("MP-1234-5678", FriendEligibility.normalizeFriendCode("1234-5678"))
        assertEquals("MP-1234-5678", FriendEligibility.normalizeFriendCode("MP12345678"))
    }

    @Test
    fun generateFriendRequestDocId_sortsUidsConsistently() {
        val id1 = FriendEligibility.generateFriendRequestDocId("bob", "alice")
        val id2 = FriendEligibility.generateFriendRequestDocId("alice", "bob")
        assertEquals("alice_bob", id1)
        assertEquals("alice_bob", id2)
    }

    @Test
    fun checkEligibility_selfAddition_denied() {
        val ctx = FriendEligibility.EligibilityContext(
            myUid = "alice", targetUid = "alice", isSelf = true,
            isAlreadyFriend = false, hasPendingOutgoing = false, hasPendingIncoming = false,
            isBlocked = false, declinedTimestamp = null, pendingSentCount = 0, friendCount = 0
        )
        val result = FriendEligibility.checkEligibility(ctx)
        assertTrue(result is FriendEligibility.EligibilityResult.Ineligible)
    }

    @Test
    fun checkEligibility_incomingRequest_reciprocalAccept() {
        val ctx = FriendEligibility.EligibilityContext(
            myUid = "alice", targetUid = "bob", isSelf = false,
            isAlreadyFriend = false, hasPendingOutgoing = false, hasPendingIncoming = true,
            isBlocked = false, declinedTimestamp = null, pendingSentCount = 0, friendCount = 0
        )
        val result = FriendEligibility.checkEligibility(ctx)
        assertTrue(result is FriendEligibility.EligibilityResult.ReciprocalAccept)
    }
}
