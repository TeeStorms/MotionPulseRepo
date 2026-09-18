package com.example.motionpulse.data.local.dao

import androidx.room.*
import com.example.motionpulse.data.local.entity.UserProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: UserProfileEntity)

    @Update
    suspend fun updateProfile(profile: UserProfileEntity)

    @Query("SELECT * FROM user_profiles WHERE uid = :uid")
    fun getProfile(uid: String): Flow<UserProfileEntity?>
}
