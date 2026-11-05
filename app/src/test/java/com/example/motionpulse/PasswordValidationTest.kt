package com.example.motionpulse

import com.example.motionpulse.util.PasswordStrength
import com.example.motionpulse.util.PasswordValidationResult
import com.example.motionpulse.util.ValidationUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordValidationTest {

    @Test
    fun passwordStrength_calculatesCorrectly() {
        assertEquals(PasswordStrength.WEAK, ValidationUtils.calculatePasswordStrength("12345"))
        assertEquals(PasswordStrength.FAIR, ValidationUtils.calculatePasswordStrength("password123"))
        assertEquals(PasswordStrength.STRONG, ValidationUtils.calculatePasswordStrength("StrongP@ssw0rd!"))
    }

    @Test
    fun passwordValidation_validInputs_passes() {
        val result = ValidationUtils.validatePasswordChange("oldPass123", "newPass123", "newPass123")
        assertTrue(result is PasswordValidationResult.Valid)
    }

    @Test
    fun passwordValidation_shortNewPass_fails() {
        val result = ValidationUtils.validatePasswordChange("oldPass123", "123", "123")
        assertTrue(result is PasswordValidationResult.Invalid)
    }

    @Test
    fun passwordValidation_mismatchConfirm_fails() {
        val result = ValidationUtils.validatePasswordChange("oldPass123", "newPass123", "different123")
        assertTrue(result is PasswordValidationResult.Invalid)
    }

    @Test
    fun passwordValidation_sameAsOld_fails() {
        val result = ValidationUtils.validatePasswordChange("oldPass123", "oldPass123", "oldPass123")
        assertTrue(result is PasswordValidationResult.Invalid)
    }
}
