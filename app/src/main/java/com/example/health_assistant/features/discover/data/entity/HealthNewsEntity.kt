package com.example.health_assistant.features.discover.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

/**
 * Room entity for health news in the Discover section
 * Stores curated health news and medical updates
 */
@Entity(
    tableName = "health_news",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["category"]),
        Index(value = ["publishedDate"]),
        Index(value = ["sourceCredibility"]),
        Index(value = ["isBreakingNews"]),
        Index(value = ["userId", "category"]),
        Index(value = ["userId", "isBreakingNews"])
    ]
)
data class HealthNewsEntity(
    @PrimaryKey 
    val id: String,
    val headline: String,
    val summary: String,
    val fullContent: String?,
    val category: String,
    val sourcePublication: String,
    val sourceCredibility: String, // "peer-reviewed", "medical-journal", "health-organization"
    val publishedDate: Long,
    val imageUrl: String?,
    val externalUrl: String,
    val isBreakingNews: Boolean = false,
    val relevanceScore: Int, // Algorithm-based relevance to user
    val userId: String = ""
)