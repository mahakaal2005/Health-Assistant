package com.example.health_assistant.features.discover.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

/**
 * Entity for A/B testing framework to optimize content presentation
 * Tracks user assignment to test variants and performance metrics
 */
@Entity(
    tableName = "ab_tests",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["testName"]),
        Index(value = ["variant"]),
        Index(value = ["isActive"]),
        Index(value = ["startDate"]),
        Index(value = ["userId", "testName"]),
        Index(value = ["testName", "variant"])
    ]
)
data class ABTestEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val testName: String, // Name of the A/B test
    val variant: String, // Test variant assigned to user ("A", "B", "control", etc.)
    val isActive: Boolean = true, // Whether test is currently active
    val assignedAt: Long = System.currentTimeMillis(), // When user was assigned to variant
    val startDate: Long, // Test start date
    val endDate: Long, // Test end date
    val conversionEvents: String = "[]", // JSON array of conversion events tracked
    val metadata: String = "{}", // Test-specific metadata and configuration
    val impressions: Int = 0, // Number of times variant was shown
    val clicks: Int = 0, // Number of clicks/interactions
    val conversions: Int = 0, // Number of successful conversions
    val engagementTime: Long = 0L, // Total engagement time for this variant
    val lastInteraction: Long = 0L // Last interaction timestamp
)