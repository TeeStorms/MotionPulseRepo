package com.example.motionpulse.data.local.dao

import androidx.room.*
import com.example.motionpulse.data.local.entity.BadgeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BadgeDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBadge(badge: BadgeEntity)

    @Query("SELECT * FROM badges WHERE userId = :userId")
    fun getBadgesForUser(userId: String): Flow<List<BadgeEntity>>

    @Delete
    suspend fun deleteBadge(badge: BadgeEntity)

    @Query("SELECT * FROM badges WHERE triggeredByCompletionId = :completionId")
    suspend fun getBadgesForCompletion(completionId: String): List<BadgeEntity>
}
