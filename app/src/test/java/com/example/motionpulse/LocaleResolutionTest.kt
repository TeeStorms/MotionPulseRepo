package com.example.motionpulse

import com.example.motionpulse.util.LocaleHelper
import org.junit.Assert.assertEquals
import org.junit.Test

class LocaleResolutionTest {

    @Test
    fun resolveSupportedLocaleTag_savedChoice_returnsSavedChoice() {
        val resolved = LocaleHelper.resolveSupportedLocaleTag(savedTag = "zu", deviceTag = "en")
        assertEquals("zu", resolved)
    }

    @Test
    fun resolveSupportedLocaleTag_supportedDeviceLocale_returnsDeviceLocale() {
        val resolved = LocaleHelper.resolveSupportedLocaleTag(savedTag = null, deviceTag = "af")
        assertEquals("af", resolved)
    }

    @Test
    fun resolveSupportedLocaleTag_unsupportedDeviceLocale_returnsEnglishFallback() {
        val resolved = LocaleHelper.resolveSupportedLocaleTag(savedTag = null, deviceTag = "fr")
        assertEquals("en", resolved)
    }

    @Test
    fun resolveSupportedLocaleTag_allFiveSupportedLocales_resolveCorrectly() {
        listOf("en", "af", "tn", "zu", "ts").forEach { tag ->
            val resolved = LocaleHelper.resolveSupportedLocaleTag(savedTag = tag, deviceTag = "fr")
            assertEquals(tag, resolved)
        }
    }
}
