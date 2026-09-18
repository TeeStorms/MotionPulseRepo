package com.example.motionpulse.domain.auth

/**
 * Data class representing the essential profile information of an authenticated user.
 */
data class UserData(
    val userId: String,
    val username: String?,
    val profilePictureUrl: String?,
    val email: String?
)
