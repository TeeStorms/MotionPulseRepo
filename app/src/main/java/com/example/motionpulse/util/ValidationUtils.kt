package com.example.motionpulse.util

sealed class NameValidationResult {
    object Valid : NameValidationResult()
    data class Invalid(val reason: String) : NameValidationResult()
}

enum class PasswordStrength {
    WEAK, FAIR, STRONG
}

sealed class PasswordValidationResult {
    object Valid : PasswordValidationResult()
    data class Invalid(val reason: String) : PasswordValidationResult()
}

object ValidationUtils {

    private val NAME_REGEX = Regex("^[a-zA-Z0-9\\p{L}'-]+( [a-zA-Z0-9\\p{L}'-]+)*$")

    fun validateProfileName(rawName: String, currentName: String? = null): NameValidationResult {
        val trimmed = rawName.trim()
        if (trimmed.length !in 2..30) {
            return NameValidationResult.Invalid("Name must be between 2 and 30 characters.")
        }
        if (!trimmed.any { it.isLetter() }) {
            return NameValidationResult.Invalid("Name must contain at least one letter.")
        }
        if (!NAME_REGEX.matches(trimmed)) {
            return NameValidationResult.Invalid("Name contains invalid characters or consecutive spaces.")
        }
        if (currentName != null && trimmed == currentName.trim()) {
            return NameValidationResult.Invalid("New name must be different from current name.")
        }
        return NameValidationResult.Valid
    }

    fun calculatePasswordStrength(password: String): PasswordStrength {
        if (password.length < 6) return PasswordStrength.WEAK
        val hasDigits = password.any { it.isDigit() }
        val hasUpper = password.any { it.isUpperCase() }
        val hasSpecial = password.any { !it.isLetterOrDigit() }

        return when {
            password.length >= 10 && hasDigits && (hasUpper || hasSpecial) -> PasswordStrength.STRONG
            password.length >= 6 && (hasDigits || hasUpper) -> PasswordStrength.FAIR
            else -> PasswordStrength.WEAK
        }
    }

    fun validatePasswordChange(currentPass: String, newPass: String, confirmPass: String): PasswordValidationResult {
        if (currentPass.isEmpty()) {
            return PasswordValidationResult.Invalid("Current password is required.")
        }
        if (newPass.length < 6) {
            return PasswordValidationResult.Invalid("New password must be at least 6 characters.")
        }
        if (newPass == currentPass) {
            return PasswordValidationResult.Invalid("New password must be different from current password.")
        }
        if (newPass != confirmPass) {
            return PasswordValidationResult.Invalid("Passwords do not match.")
        }
        return PasswordValidationResult.Valid
    }

    fun canUnlink(providers: List<String>, targetProvider: String): Boolean {
        if (!providers.contains(targetProvider)) return false
        return providers.size > 1
    }
}
