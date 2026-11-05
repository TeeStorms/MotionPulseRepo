package com.example.motionpulse.domain.models

import com.example.motionpulse.data.local.entity.UserProfileEntity

sealed class FriendLookupResult {
    data class Found(val profile: UserProfileEntity) : FriendLookupResult()
    object NotFound : FriendLookupResult()
    data class Self(val message: String) : FriendLookupResult()
    data class InvalidFormat(val message: String) : FriendLookupResult()
    data class RateLimited(val seconds: Int) : FriendLookupResult()
    data class Offline(val message: String) : FriendLookupResult()
    data class PermissionDenied(val message: String) : FriendLookupResult()
    data class Error(val message: String) : FriendLookupResult()
}
