package com.example.motionpulse.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.motionpulse.data.local.dao.*
import com.example.motionpulse.data.local.entity.*

/**
 * Matching Firestore Collection Structure:
 * - users/{uid} -> [UserProfileEntity]
 * - users/{uid}/habits/{habitId} -> [HabitEntity]
 * - users/{uid}/habits/{habitId}/completions/{date} -> [HabitCompletionEntity]
 * - users/{uid}/badges/{badgeId} -> [BadgeEntity]
 * - friendships/{friendshipId} -> [FriendEntity] (contains ownerUid and friendUid)
 */
@Database(
    entities = [
        HabitEntity::class,
        HabitCompletionEntity::class,
        UserProfileEntity::class,
        BadgeEntity::class,
        FriendEntity::class,
        MoodEntity::class
    ],
    version = 8,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
    abstract fun habitCompletionDao(): HabitCompletionDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun badgeDao(): BadgeDao
    abstract fun friendDao(): FriendDao
    abstract fun moodDao(): MoodDao
}
