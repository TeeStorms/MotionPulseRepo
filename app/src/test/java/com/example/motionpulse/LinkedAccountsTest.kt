package com.example.motionpulse

import com.example.motionpulse.util.ValidationUtils
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LinkedAccountsTest {

    @Test
    fun canUnlink_singleProvider_returnsFalse() {
        val providers = listOf("password")
        assertFalse(ValidationUtils.canUnlink(providers, "password"))
    }

    @Test
    fun canUnlink_multipleProviders_returnsTrue() {
        val providers = listOf("password", "google.com")
        assertTrue(ValidationUtils.canUnlink(providers, "password"))
        assertTrue(ValidationUtils.canUnlink(providers, "google.com"))
    }

    @Test
    fun canUnlink_targetNotLinked_returnsFalse() {
        val providers = listOf("password")
        assertFalse(ValidationUtils.canUnlink(providers, "google.com"))
    }
}
