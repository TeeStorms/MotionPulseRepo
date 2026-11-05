package com.example.motionpulse

import com.example.motionpulse.util.NameValidationResult
import com.example.motionpulse.util.ValidationUtils
import org.junit.Assert.assertTrue
import org.junit.Test

class NameValidationTest {

    @Test
    fun validNames_passValidation() {
        val validNames = listOf("Zotwene", "John Doe", "Mary-Jane", "O'Connor", "René")
        validNames.forEach { name ->
            val result = ValidationUtils.validateProfileName(name, "Old Name")
            assertTrue("Expected valid for $name", result is NameValidationResult.Valid)
        }
    }

    @Test
    fun invalidNames_failValidation() {
        val invalidNames = listOf("a", "  ", "12345", "John  Doe", "   A   ", "ThisIsAVeryLongNameThatExceedsThirtyCharactersLimit")
        invalidNames.forEach { name ->
            val result = ValidationUtils.validateProfileName(name, "Old Name")
            assertTrue("Expected invalid for '$name'", result is NameValidationResult.Invalid)
        }
    }

    @Test
    fun sameAsCurrentName_failsValidation() {
        val result = ValidationUtils.validateProfileName("Zotwene", "Zotwene")
        assertTrue(result is NameValidationResult.Invalid)
    }
}
