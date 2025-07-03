package com.example.health_assistant.features.journal.db

import androidx.room.TypeConverter
import java.util.Date

/**
 * Type converters for Room database
 * Handles conversion between complex types and primitive types that can be stored in the database
 */
class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
}
