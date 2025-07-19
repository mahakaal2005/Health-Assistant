package com.example.health_assistant.features.discover.domain.model

import java.io.Serializable

/**
 * Sealed class representing different types of content in the Discover section
 * Provides a unified interface for mixed content display
 */
sealed class DiscoverContent : Serializable {
    abstract val id: String
    abstract val title: String
    abstract val publishedDate: Long
    abstract val category: String
    abstract val imageUrl: String?
    abstract val userId: String

    /**
     * Health article content
     */
    data class Article(
        override val id: String,
        override val title: String,
        override val publishedDate: Long,
        override val category: String,
        override val imageUrl: String?,
        override val userId: String,
        val summary: String,
        val content: String,
        val authorName: String,
        val authorCredentials: String,
        val sourceUrl: String,
        val lastUpdated: Long,
        val readingTimeMinutes: Int,
        val tags: List<String>,
        val isBookmarked: Boolean,
        val readProgress: Float,
        val credibilityScore: Int
    ) : DiscoverContent()

    /**
     * Health news content
     */
    data class News(
        override val id: String,
        override val title: String,
        override val publishedDate: Long,
        override val category: String,
        override val imageUrl: String?,
        override val userId: String,
        val summary: String,
        val fullContent: String?,
        val sourcePublication: String,
        val sourceCredibility: String,
        val externalUrl: String,
        val isBreakingNews: Boolean,
        val relevanceScore: Int
    ) : DiscoverContent()

    /**
     * Educational video content
     */
    data class Video(
        override val id: String,
        override val title: String,
        override val publishedDate: Long,
        override val category: String,
        override val imageUrl: String?,
        override val userId: String,
        val description: String,
        val thumbnailUrl: String,
        val videoUrl: String,
        val durationSeconds: Int,
        val difficultyLevel: String,
        val expertName: String,
        val expertCredentials: String,
        val watchProgress: Float,
        val isDownloadedOffline: Boolean,
        val transcriptAvailable: Boolean
    ) : DiscoverContent()

    /**
     * Get content type as string for identification
     */
    fun getContentType(): String = when (this) {
        is Article -> "article"
        is News -> "news"
        is Video -> "video"
    }

    /**
     * Get display title for UI
     */
    fun getDisplayTitle(): String = when (this) {
        is Article -> title
        is News -> title
        is Video -> title
    }

    /**
     * Get content summary for preview
     */
    fun getContentSummary(): String = when (this) {
        is Article -> summary
        is News -> summary
        is Video -> description
    }

    /**
     * Check if content is bookmarked
     */
    fun getBookmarkStatus(): Boolean = when (this) {
        is Article -> isBookmarked
        is News -> false // News items don't have bookmark status in entity
        is Video -> false // Videos don't have bookmark status in entity
    }

    /**
     * Get reading/watching progress
     */
    fun getProgress(): Float = when (this) {
        is Article -> readProgress
        is News -> 0f // News items don't track progress
        is Video -> watchProgress
    }
}