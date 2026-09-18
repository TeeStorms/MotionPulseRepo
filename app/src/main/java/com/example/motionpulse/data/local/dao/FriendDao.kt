package com.example.motionpulse.data.local.dao

import androidx.room.*
import com.example.motionpulse.data.local.entity.FriendEntity
import com.example.motionpulse.data.local.entity.FriendStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface FriendDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFriend(friend: FriendEntity)

    @Update
    suspend fun updateFriend(friend: FriendEntity)

    @Delete
    suspend fun deleteFriend(friend: FriendEntity)

    @Query("SELECT * FROM friends WHERE ownerUid = :userId AND status = 'ACCEPTED'")
    fun getActiveFriendsForUser(userId: String): Flow<List<FriendEntity>>

    @Query("SELECT * FROM friends WHERE ownerUid = :userId")
    fun getAllFriendships(userId: String): Flow<List<FriendEntity>>
}
