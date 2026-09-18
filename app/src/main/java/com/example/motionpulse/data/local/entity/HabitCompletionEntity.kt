package com.example.motionpulse.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate

enum class CompletionStatus {
    COMPLETED, MISSED, SKIPPED
}

@Entity(
    tableName = "habit_completions",
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["habitId"])]
)
data class HabitCompletionEntity(
    @PrimaryKey val id: String = "",
    val habitId: String = "",
    val date: LocalDate = LocalDate.now(),
    val status: CompletionStatus = CompletionStatus.MISSED,
    val numericValueLogged: Float? = null,
    val loggedAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
)
