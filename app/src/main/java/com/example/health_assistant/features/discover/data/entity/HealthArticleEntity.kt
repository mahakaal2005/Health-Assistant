package com.example.health_assistant.features.discover.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

/**
 * Room entity for health articles in the Discover section
 * Stores educational health content with credibility information
 */
@Entity(
    tableName = "health_articles",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["category"]),
        Index(value = ["publishedDate"]),
        Index(value = ["credibilityScore"]),
        Index(value = ["userId", "category"]),
        Index(value = ["userId", "isBookmarked"])
    ]
)
data class HealthArticleEntity(
    @PrimaryKey 
    val id: String,
    val title: String,
    val summary: String,
    val content: String,
    val category: String, // nutrition, fitness, mental-health, preventive-care
    val authorName: String,
    val authorCredentials: String,
    val sourceUrl: String,
    val publishedDate: Long,
    val lastUpdated: Long,
    val readingTimeMinutes: Int,
    val imageUrl: String?,
    val tags: List<String>,
    val isBookmarked: Boolean = false,
    val readProgress: Float = 0f, // 0.0 to 1.0
    val credibilityScore: Int, // 1-5 rating for source reliability
    val userId: String = ""
)