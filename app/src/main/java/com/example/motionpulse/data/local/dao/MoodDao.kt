package com.example.motionpulse.data.local.dao

import androidx.room.*
import com.example.motionpulse.data.local.entity.MoodEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface MoodDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateMood(mood: MoodEntity)

    @Query("SELECT * FROM moods WHERE userId = :userId AND date = :date LIMIT 1")
    suspend fun getMoodForDate(userId: String, date: LocalDate): MoodEntity?

    @Query("SELECT * FROM moods WHERE userId = :userId AND date = :date LIMIT 1")
    fun getMoodFlowForDate(userId: String, date: LocalDate): Flow<MoodEntity?>

    @Query("SELECT * FROM moods WHERE userId = :userId AND date BETWEEN :startDate AND :endDate")
    fun getMoodsForDateRange(userId: String, startDate: LocalDate, endDate: LocalDate): Flow<List<MoodEntity>>
}
