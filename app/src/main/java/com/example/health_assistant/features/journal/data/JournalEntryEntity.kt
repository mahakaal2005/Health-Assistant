package com.example.health_assistant.features.journal.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

/**
 * Entity class for journal entries in the database
 * Uses a flexible structure to store different types of journal entries
 */
@Entity(
    tableName = "journal_entries",
    indices = [Index("userId"), Index("type")] // Add indices for faster queries
)
data class JournalEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val type: String, // "mood", "heart_rate", "blood_pressure", "workout", "weight", "sleep", "note"

    // Flexible fields that can be used for different entry types
    val title: String? = null,        // Used for descriptions, activity types, etc.
    val content: String? = null,      // Used for notes, measurements, etc.
    val description: String? = null,  // Used for additional details

    // Numeric fields for health measurements
    val moodLevel: Int? = null,       // 1-5 for mood entries
    val numericValue1: Double? = null, // Used for weight, bpm, systolic pressure, duration
    val numericValue2: Double? = null, // Used for diastolic pressure, sleep quality
    val unit: String? = null,         // Unit of measurement
    val emoji: String? = null,        // For mood entries
    val state: String? = null,        // For context like "resting", "active"
    
    // User association
    val userId: String = ""           // User ID to associate entries with specific users
)