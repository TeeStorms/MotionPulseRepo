package com.example.motionpulse.data.local

import androidx.room.TypeConverter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.LocalDate

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Instant? {
        return value?.let { Instant.ofEpochMilli(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Instant?): Long? {
        return date?.toEpochMilli()
    }

    @TypeConverter
    fun fromLocalDate(value: String?): LocalDate? {
        return value?.let { LocalDate.parse(it) }
    }

    @TypeConverter
    fun localDateToString(date: LocalDate?): String? {
        return date?.toString()
    }

    @TypeConverter
    fun fromStringList(value: String?): List<String>? {
        return value?.let { Json.decodeFromString<List<String>>(it) }
    }

    @TypeConverter
    fun fromList(list: List<String>?): String? {
        return list?.let { Json.encodeToString(it) }
    }

    @TypeConverter
    fun fromMoodLevel(level: com.example.motionpulse.data.local.entity.MoodLevel?): String? {
        return level?.name
    }

    @TypeConverter
    fun toMoodLevel(value: String?): com.example.motionpulse.data.local.entity.MoodLevel? {
        return value?.let { com.example.motionpulse.data.local.entity.MoodLevel.valueOf(it) }
    }

    @TypeConverter
    fun fromMoodFactors(factors: List<com.example.motionpulse.data.local.entity.MoodFactor>?): String? {
        return factors?.let { Json.encodeToString(it.map { it.name }) }
    }

    @TypeConverter
    fun toMoodFactors(value: String?): List<com.example.motionpulse.data.local.entity.MoodFactor>? {
        return value?.let {
            val names = Json.decodeFromString<List<String>>(it)
            names.map { com.example.motionpulse.data.local.entity.MoodFactor.valueOf(it) }
        }
    }
}
