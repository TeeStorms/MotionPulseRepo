package com.example.motionpulse

import com.example.motionpulse.domain.models.UserSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsRepositoryTest {

    @Test
    fun defaultUserSettings_hasCorrectDefaultValues() {
        val defaults = UserSettings()
        assertNull(defaults.language)
        assertTrue(defaults.remindersEnabled)
        assertTrue(defaults.shareMilestones)
        assertFalse(defaults.showHabitNames)
    }

    @Test
    fun lastWriteWins_newerTimestampOverridesOlder() {
        val older = UserSettings(language = "en", remindersEnabled = true, updatedAt = 1000L)
        val newer = UserSettings(language = "af", remindersEnabled = false, updatedAt = 2000L)

        val merged = if (newer.updatedAt >= older.updatedAt) newer else older
        assertEquals("af", merged.language)
        assertFalse(merged.remindersEnabled)
    }

    @Test
    fun customSettings_preservesSupportedLanguageTags() {
        val afrikaansSettings = UserSettings(language = "af")
        val setswanaSettings = UserSettings(language = "tn")
        val isizuluSettings = UserSettings(language = "zu")
        val shitsongaSettings = UserSettings(language = "ts")

        assertEquals("af", afrikaansSettings.language)
        assertEquals("tn", setswanaSettings.language)
        assertEquals("zu", isizuluSettings.language)
        assertEquals("ts", shitsongaSettings.language)
    }
}
