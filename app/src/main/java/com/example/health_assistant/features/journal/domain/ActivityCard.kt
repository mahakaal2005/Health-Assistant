package com.example.health_assistant.features.journal.domain

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index
import java.time.LocalDate

/**
 * Simplified Activity Card with only essential health metrics:
 * - Steps
 * - Calories
 * - Heart Points
 */
@Entity(
    tableName = "activity_cards",
    indices = [Index("userId"), Index("date")] // Add indices for faster queries
)
data class ActivityCard(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: LocalDate,
    val stepCount: Int = 0,
    val caloriesBurned: Int = 0,
    val heartPoints: Int = 0, // Heart points earned (0-10)
    val userId: String = "" // User ID to associate activity cards with specific users
)