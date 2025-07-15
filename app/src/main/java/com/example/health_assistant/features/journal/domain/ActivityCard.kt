package com.example.health_assistant.features.journal.domain

import java.time.LocalDate

/**
 * Simplified Activity Card with only essential health metrics:
 * - Steps
 * - Calories
 * - Heart Points
 */
data class ActivityCard(
    val id: Long,
    val date: LocalDate,
    val stepCount: Int = 0,
    val caloriesBurned: Int = 0,
    val heartPoints: Int = 0 // Heart points earned (0-10)
)