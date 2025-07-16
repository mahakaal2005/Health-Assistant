package com.example.health_assistant.data.local.database

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * TypeConverters for Room database
 * Handles conversion of complex types to/from database storage formats
 */
class Converters {

    private val gson = Gson()

    @TypeConverter
    fun fromStringList(value: List<String>?): String? {
        return if (value == null) null else gson.toJson(value)
    }

    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        return if (value == null) null else {
            val listType = object : TypeToken<List<String>>() {}.type
            gson.fromJson(value, listType)
        }
    }

    @TypeConverter
    fun fromIntList(value: List<Int>?): String? {
        return if (value == null) null else gson.toJson(value)
    }

    @TypeConverter
    fun toIntList(value: String?): List<Int>? {
        return if (value == null) null else {
            val listType = object : TypeToken<List<Int>>() {}.type
            gson.fromJson(value, listType)
        }
    }

    @TypeConverter
    fun fromLongList(value: List<Long>?): String? {
        return if (value == null) null else gson.toJson(value)
    }

    @TypeConverter
    fun toLongList(value: String?): List<Long>? {
        return if (value == null) null else {
            val listType = object : TypeToken<List<Long>>() {}.type
            gson.fromJson(value, listType)
        }
    }

    // Date converters for prescription domain model compatibility
    @TypeConverter
    fun fromDate(date: java.util.Date?): Long? {
        return date?.time
    }

    @TypeConverter
    fun toDate(timestamp: Long?): java.util.Date? {
        return timestamp?.let { java.util.Date(it) }
    }

    // LocalDate converters for ActivityCard
    @TypeConverter
    fun fromLocalDate(date: java.time.LocalDate?): String? {
        return date?.toString()
    }

    @TypeConverter
    fun toLocalDate(dateString: String?): java.time.LocalDate? {
        return dateString?.let { java.time.LocalDate.parse(it) }
    }

    // Additional converters for robust data handling
    @TypeConverter
    fun fromBoolean(value: Boolean): Int {
        return if (value) 1 else 0
    }

    @TypeConverter
    fun toBoolean(value: Int): Boolean {
        return value == 1
    }

    @TypeConverter
    fun fromStringMap(value: Map<String, String>?): String? {
        return if (value == null) null else gson.toJson(value)
    }

    @TypeConverter
    fun toStringMap(value: String?): Map<String, String>? {
        return if (value == null) null else {
            val mapType = object : TypeToken<Map<String, String>>() {}.type
            gson.fromJson(value, mapType)
        }
    }
}