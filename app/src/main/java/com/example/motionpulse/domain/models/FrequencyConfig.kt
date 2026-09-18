package com.example.motionpulse.domain.models

import com.example.motionpulse.data.local.entity.FrequencyType
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.DayOfWeek
import java.time.LocalDate

@Serializable
data class FrequencyConfig(
    val type: FrequencyType,
    val timesPerWeek: Int? = null,
    val specificDays: Set<DayOfWeek>? = null,
    val startDate: String? = null // For EVERY_OTHER_DAY, stored as ISO string
) {
    companion object {
        fun decodeSafe(json: String): FrequencyConfig {
            return try {
                if (json.isBlank()) FrequencyConfig(FrequencyType.EVERY_DAY)
                else Json.decodeFromString(json)
            } catch (e: Exception) {
                FrequencyConfig(FrequencyType.EVERY_DAY)
            }
        }
    }

    fun getRemindersPerWeek(): Int {
        return when (type) {
            FrequencyType.EVERY_DAY -> 7
            FrequencyType.X_TIMES_PER_WEEK -> timesPerWeek ?: 3
            FrequencyType.SPECIFIC_DAYS -> specificDays?.size ?: 3
            FrequencyType.EVERY_OTHER_DAY -> 4
        }
    }

    fun getFrequencyLabel(): String {
        return when (type) {
            FrequencyType.EVERY_DAY -> "Everyday"
            FrequencyType.X_TIMES_PER_WEEK -> "${timesPerWeek ?: 3} x a week"
            else -> "Everyday"
        }
    }

    fun isScheduled(date: LocalDate): Boolean {
        return try {
            when (type) {
                FrequencyType.EVERY_DAY -> true
                FrequencyType.X_TIMES_PER_WEEK -> true // Scoring handles the count per week
                FrequencyType.SPECIFIC_DAYS -> specificDays?.contains(date.dayOfWeek) ?: true
                FrequencyType.EVERY_OTHER_DAY -> {
                    val start = startDate?.let { LocalDate.parse(it) } ?: return true
                    val daysBetween = java.time.temporal.ChronoUnit.DAYS.between(start, date)
                    daysBetween % 2 == 0L
                }
            }
        } catch (e: Exception) {
            true // Default to scheduled on error to ensure habit is visible
        }
    }
}
