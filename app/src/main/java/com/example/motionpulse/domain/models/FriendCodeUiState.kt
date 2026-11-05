package com.example.motionpulse.domain.models

import com.example.motionpulse.data.local.entity.UserProfileEntity

sealed class FriendSearchUiState {
    object Idle : FriendSearchUiState()
    object Typing : FriendSearchUiState()
    object Loading : FriendSearchUiState()
    data class Found(val profile: UserProfileEntity) : FriendSearchUiState()
    data class Error(val message: String, val debugCode: String? = null) : FriendSearchUiState()
    data class RateLimited(val seconds: Int) : FriendSearchUiState()
}
