package com.example.health_assistant.features.discover.domain.model

/**
 * Simple sealed class for API compatibility
 * Used by HealthContentRemoteDataSource
 */
sealed class DiscoverContent {
    abstract val id: String
    abstract val title: String
    abstract val publishedDate: Long
    abstract val category: String
    abstract val imageUrl: String?
    abstract val userId: String

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
}