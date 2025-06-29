package com.example.health_assistant.features.health.model

/**
 * Data class representing health metrics for the triple-ring progress view
 */
data class HealthMetrics(
    val steps: HealthMetric = HealthMetric(0, 9000),
    val calories: HealthMetric = HealthMetric(0, 300),
    val heartPoints: HealthMetric = HealthMetric(0, 50)
)

/**
 * Data class representing a single health metric with current and target values
 */
data class HealthMetric(
    val current: Int,
    val target: Int
)