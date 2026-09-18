package com.example.motionpulse.data.local.dao

import androidx.room.*
import com.example.motionpulse.data.local.entity.HabitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertHabit(habit: HabitEntity)

    @Update
    suspend fun updateHabit(habit: HabitEntity)

    @Delete
    suspend fun deleteHabit(habit: HabitEntity)

    @Query("SELECT * FROM habits WHERE id = :id")
    suspend fun getHabitById(id: String): HabitEntity?

    @Query("SELECT * FROM habits WHERE ownerId = :userId AND title = :title AND isArchived = 0")
    suspend fun getHabitByTitle(userId: String, title: String): HabitEntity?

    @Query("SELECT * FROM habits WHERE ownerId = :userId AND isArchived = 0")
    fun getHabitsForUser(userId: String): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE ownerId = :userId AND isArchived = 1")
    fun getArchivedHabitsForUser(userId: String): Flow<List<HabitEntity>>

    @Query("UPDATE habits SET isArchived = 0, updatedAt = :timestamp WHERE id = :habitId")
    suspend fun unarchiveHabit(habitId: String, timestamp: java.time.Instant)

    @Query("SELECT * FROM habits WHERE isArchived = 0")
    fun getAllActiveHabits(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE updatedAt > :timestamp")
    suspend fun getHabitsUpdatedAfter(timestamp: java.time.Instant): List<HabitEntity>
}
