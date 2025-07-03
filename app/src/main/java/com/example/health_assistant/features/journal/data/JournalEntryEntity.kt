package com.example.health_assistant.features.journal.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.health_assistant.data.local.database.Converters

@Entity(tableName = "journal_entries")
@TypeConverters(Converters::class)
data class JournalEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String = "journal",
    val timestamp: Long = System.currentTimeMillis(),
    val content: String = "",
    val moodLevel: Int = 0,
    val emoji: String = "",
    val description: String = "",
    val activityType: String? = null,
    val duration: Int? = null,
    val summary: String? = null,
    val goalTitle: String? = null,
    val progress: Float? = null,
    val measurementType: String? = null,
    val value: Float? = null,
    val unit: String? = null,
    val previousValue: Float? = null,
    val state: String? = null,
    val systolic: Int? = null,
    val diastolic: Int? = null,
    val steps: Int? = null,
    val stepGoal: Int? = null,
    val activeMinutes: Int? = null,
    val activeMinutesGoal: Int? = null,
    val calories: Int? = null,
    val caloriesGoal: Int? = null,
    val distance: Float? = null
)
