package com.example.health_assistant.features.discover.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

/**
 * Room entity for educational videos in the Discover section
 * Stores video content with playback progress tracking
 */
@Entity(
    tableName = "educational_videos",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["category"]),
        Index(value = ["publishedDate"]),
        Index(value = ["difficultyLevel"]),
        Index(value = ["isDownloadedOffline"]),
        Index(value = ["userId", "category"]),
        Index(value = ["userId", "isDownloadedOffline"])
    ]
)
data class EducationalVideoEntity(
    @PrimaryKey 
    val id: String,
    val title: String,
    val description: String,
    val category: String,
    val thumbnailUrl: String,
    val videoUrl: String,
    val durationSeconds: Int,
    val difficultyLevel: String, // "beginner", "intermediate", "advanced"
    val expertName: String,
    val expertCredentials: String,
    val publishedDate: Long,
    val watchProgress: Float = 0f, // 0.0 to 1.0
    val isDownloadedOffline: Boolean = false,
    val transcriptAvailable: Boolean = false,
    val userId: String = ""
)