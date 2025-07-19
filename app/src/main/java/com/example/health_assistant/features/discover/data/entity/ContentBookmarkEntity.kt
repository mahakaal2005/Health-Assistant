package com.example.health_assistant.features.discover.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

/**
 * Room entity for content bookmarks in the Discover section
 * Tracks user bookmarks across different content types
 */
@Entity(
    tableName = "content_bookmarks",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["contentId"]),
        Index(value = ["contentType"]),
        Index(value = ["bookmarkedDate"]),
        Index(value = ["userId", "contentType"]),
        Index(value = ["userId", "bookmarkedDate"])
    ]
)
data class ContentBookmarkEntity(
    @PrimaryKey 
    val id: String,
    val contentId: String,
    val contentType: String, // "article", "news", "video"
    val bookmarkedDate: Long,
    val userId: String
)