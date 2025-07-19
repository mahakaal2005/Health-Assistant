package com.example.health_assistant.features.discover.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

/**
 * Entity for storing content recommendations based on user engagement patterns
 * Supports personalized content delivery and A/B testing
 */
@Entity(
    tableName = "content_recommendations",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["contentId"]),
        Index(value = ["recommendationType"]),
        Index(value = ["score"]),
        Index(value = ["createdAt"]),
        Index(value = ["userId", "recommendationType"]),
        Index(value = ["userId", "score"])
    ]
)
data class ContentRecommendationEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val contentId: String,
    val contentType: String, // "article", "news", "video"
    val recommendationType: String, // "trending", "personalized", "similar", "category_based"
    val score: Float, // Recommendation confidence score (0.0 to 1.0)
    val reason: String, // Human-readable reason for recommendation
    val algorithmVersion: String, // Version of recommendation algorithm used
    val category: String,
    val tags: String = "[]", // JSON array of relevant tags
    val metadata: String = "{}", // Additional recommendation metadata
    val isShown: Boolean = false, // Whether recommendation was displayed to user
    val isClicked: Boolean = false, // Whether user clicked on recommendation
    val isBookmarked: Boolean = false, // Whether user bookmarked recommended content
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = 0L // When recommendation expires (0 = never)
)