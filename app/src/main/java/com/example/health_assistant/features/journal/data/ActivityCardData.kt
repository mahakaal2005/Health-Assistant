package com.example.health_assistant.features.journal.data

/**
 * Simplified data class for JSON serialization of Activity Card data
 * Only stores the 3 essential health metrics
 */
data class ActivityCardData(
    val stepCount: Int = 0,
    val caloriesBurned: Int = 0,
    val heartPoints: Int = 0
)