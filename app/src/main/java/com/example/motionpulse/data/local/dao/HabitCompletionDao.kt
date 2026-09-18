package com.example.motionpulse.data.local.dao

import androidx.room.*
import com.example.motionpulse.data.local.entity.HabitCompletionEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface HabitCompletionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompletion(completion: HabitCompletionEntity)

    @Delete
    suspend fun deleteCompletion(completion: HabitCompletionEntity)

    @Query("SELECT * FROM habit_completions WHERE habitId = :habitId AND date BETWEEN :startDate AND :endDate")
    fun getCompletionsForHabitInRange(
        habitId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<List<HabitCompletionEntity>>

    @Query("SELECT * FROM habit_completions WHERE habitId = :habitId")
    fun getAllCompletionsForHabit(habitId: String): Flow<List<HabitCompletionEntity>>

    @Query("SELECT * FROM habit_completions WHERE updatedAt > :timestamp")
    suspend fun getCompletionsUpdatedAfter(timestamp: java.time.Instant): List<HabitCompletionEntity>

    @Query("SELECT * FROM habit_completions WHERE habitId = :habitId AND date = :date LIMIT 1")
    suspend fun getCompletionByDate(habitId: String, date: LocalDate): HabitCompletionEntity?

    @Query("SELECT * FROM habit_completions")
    fun getAllCompletionsFlow(): Flow<List<HabitCompletionEntity>>
}
