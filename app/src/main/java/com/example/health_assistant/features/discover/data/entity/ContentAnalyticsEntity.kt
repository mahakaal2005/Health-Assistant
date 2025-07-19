package com.example.health_assistant.features.discover.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

/**
 * Entity for tracking content analytics and user engagement metrics
 * Stores anonymous usage data for content optimization and recommendations
 */
@Entity(
    tableName = "content_analytics",
    indices = [
        Index(value = ["contentId"]),
        Index(value = ["contentType"]),
        Index(value = ["userId"]),
        Index(value = ["sessionId"]),
        Index(value = ["timestamp"]),
        Index(value = ["eventType"]),
        Index(value = ["userId", "contentId"]),
        Index(value = ["userId", "eventType"]),
        Index(value = ["contentId", "eventType"])
    ]
)
data class ContentAnalyticsEntity(
    @PrimaryKey
    val id: String,
    val contentId: String,
    val contentType: String, // "article", "news", "video"
    val userId: String,
    val sessionId: String,
    val eventType: String, // "view", "read_start", "read_progress", "read_complete", "bookmark", "share", "like", "report"
    val timestamp: Long,
    val duration: Long = 0L, // Time spent on content in milliseconds
    val progress: Float = 0f, // Reading/watching progress (0.0 to 1.0)
    val metadata: String = "{}", // JSON string for additional event-specific data
    val category: String = "", // Content category for analytics grouping
    val source: String = "", // How user discovered content (feed, search, recommendation)
    val deviceType: String = "", // "mobile", "tablet" for responsive analytics
    val networkType: String = "" // "wifi", "cellular", "offline" for performance analytics
)