package com.example.health_assistant.features.discover.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

/**
 * Entity for tracking user engagement patterns and preferences
 * Used for content recommendation algorithms and personalization
 */
@Entity(
    tableName = "user_engagement",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["category"]),
        Index(value = ["contentType"]),
        Index(value = ["lastUpdated"]),
        Index(value = ["userId", "category"]),
        Index(value = ["userId", "contentType"])
    ]
)
data class UserEngagementEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val category: String, // Health content category
    val contentType: String, // "article", "news", "video"
    val totalViews: Int = 0,
    val totalReadingTime: Long = 0L, // Total time spent reading/watching in milliseconds
    val averageReadingTime: Long = 0L, // Average time per content piece
    val completionRate: Float = 0f, // Percentage of content completed on average
    val bookmarkRate: Float = 0f, // Percentage of content bookmarked
    val shareRate: Float = 0f, // Percentage of content shared
    val engagementScore: Float = 0f, // Calculated engagement score (0.0 to 1.0)
    val preferenceWeight: Float = 0f, // User preference weight for this category/type
    val lastEngagement: Long = 0L, // Timestamp of last engagement
    val lastUpdated: Long = System.currentTimeMillis()
)