package com.example.motionpulse.domain.auth

enum class AuthErrorType {
    INVALID_CREDENTIALS,
    USER_NOT_FOUND,
    EMAIL_NOT_VERIFIED,
    NETWORK_ERROR,
    TOO_MANY_ATTEMPTS,
    ACCOUNT_DISABLED,
    GOOGLE_SIGN_IN_CANCELLED,
    GOOGLE_SIGN_IN_FAILED,
    PROFILE_SYNC_FAILED,
    UNKNOWN
}

sealed class AuthState {
    object Idle : AuthState()
    object Verifying : AuthState()
    object OnboardingRequired : AuthState()
    data class AwaitingEmailVerification(val email: String) : AuthState()
    data class Authenticated(val uid: String, val isNewUser: Boolean = false) : AuthState()
    data class Error(val type: AuthErrorType, val message: String) : AuthState()
}
