package com.example.motionpulse

import com.example.motionpulse.domain.models.FriendLookupResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FriendCodeDataSourceTest {

    @Test
    fun lookupResultMapping_messagesAreCorrect() {
        val notFound = FriendLookupResult.NotFound
        assertTrue(notFound is FriendLookupResult.NotFound)

        val self = FriendLookupResult.Self("That's your own code.")
        assertEquals("That's your own code.", self.message)

        val invalid = FriendLookupResult.InvalidFormat("Invalid")
        assertEquals("Invalid", invalid.message)

        val rate = FriendLookupResult.RateLimited(30)
        assertEquals(30, rate.seconds)

        val offline = FriendLookupResult.Offline("Offline")
        assertEquals("Offline", offline.message)

        val perm = FriendLookupResult.PermissionDenied("Denied")
        assertEquals("Denied", perm.message)

        val err = FriendLookupResult.Error("Err")
        assertEquals("Err", err.message)
    }

    @Test
    fun lookupResultMapping_additionalErrorCodes() {
        val unauth = FriendLookupResult.Error("Please sign in again.")
        assertEquals("Please sign in again.", unauth.message)

        val quota = FriendLookupResult.RateLimited(60)
        assertEquals(60, quota.seconds)
    }
}
