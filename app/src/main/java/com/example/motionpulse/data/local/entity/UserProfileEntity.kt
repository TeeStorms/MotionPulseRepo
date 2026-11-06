package com.example.motionpulse.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.PropertyName
import java.time.Instant

@Entity(tableName = "user_profiles")
data class UserProfileEntity(
    @PrimaryKey val uid: String = "",

    @get:PropertyName("displayName") @set:PropertyName("displayName")
    var displayName: String = "",

    @get:PropertyName("email") @set:PropertyName("email")
    var email: String = "",

    @get:PropertyName("avatarUrl") @set:PropertyName("avatarUrl")
    var avatarUrl: String? = null,

    @get:PropertyName("totalAuraXp") @set:PropertyName("totalAuraXp")
    var totalAuraXp: Long = 0,

    @get:PropertyName("currentLevel") @set:PropertyName("currentLevel")
    var currentLevel: Int = 1,

    @get:PropertyName("currentStreak") @set:PropertyName("currentStreak")
    var currentStreak: Int = 0,

    @get:PropertyName("language") @set:PropertyName("language")
    var language: String = "English",

    @get:PropertyName("remindersEnabled") @set:PropertyName("remindersEnabled")
    var remindersEnabled: Boolean = true,

    @get:PropertyName("shareMilestonesWithFriends") @set:PropertyName("shareMilestonesWithFriends")
    var shareMilestonesWithFriends: Boolean = true,

    @get:PropertyName("showHabitNamesInPosts") @set:PropertyName("showHabitNamesInPosts")
    var showHabitNamesInPosts: Boolean = false,

    @get:PropertyName("fcmToken") @set:PropertyName("fcmToken")
    var fcmToken: String? = null,

    @get:PropertyName("friendCode") @set:PropertyName("friendCode")
    var friendCode: String = "",

    /**
     * Stored as Map<String, Any?> because Firestore may return Integer, Long, or Double
     * for numeric values. Use [getRecentXpAsLong] to safely extract Long values.
     */
    @get:PropertyName("recentXp") @set:PropertyName("recentXp")
    var recentXp: Map<String, Any?> = emptyMap(),

    @get:PropertyName("createdAt") @set:PropertyName("createdAt")
    var createdAt: Instant = Instant.now()
) {
    /**
     * Returns a Map<String, Long> safely extracted from [recentXp].
     * Firestore numeric values can arrive as Integer, Long, Double or String,
     * so we normalize them all to Long.
     */
    fun getRecentXpAsLong(): Map<String, Long> {
        val result = mutableMapOf<String, Long>()
        recentXp.forEach { (key, value) ->
            val longValue = when (value) {
                is Long -> value
                is Int -> value.toLong()
                is Double -> value.toLong()
                is Float -> value.toLong()
                is Number -> value.toLong()
                is String -> value.toLongOrNull() ?: 0L
                else -> 0L
            }
            if (longValue > 0L) result[key] = longValue
        }
        return result
    }
}
