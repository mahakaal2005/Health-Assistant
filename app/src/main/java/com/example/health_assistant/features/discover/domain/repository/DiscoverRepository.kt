package com.example.health_assistant.features.discover.domain.repository

import com.example.health_assistant.core.util.Result
import com.example.health_assistant.features.discover.domain.model.DiscoverContent
import com.example.health_assistant.features.discover.domain.model.HealthContentCategory
import com.example.health_assistant.features.discover.domain.model.ContentValidationResult
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for Discover section content management
 * Defines the contract for all content operations with offline-first approach
 */
interface DiscoverRepository {

    // ==================== CONTENT FETCHING ====================

    /**
     * Get health articles with optional category filtering
     * Returns Flow for reactive updates with offline-first pattern
     */
    fun getHealthArticles(
        category: String? = null,
        limit: Int = 20
    ): Flow<Result<List<DiscoverContent.Article>>>

    /**
     * Get health news with optional category filtering
     * Returns Flow for reactive updates with offline-first pattern
     */
    fun getHealthNews(
        category: String? = null,
        limit: Int = 10
    ): Flow<Result<List<DiscoverContent.News>>>

    /**
     * Get educational videos with optional category filtering
     * Returns Flow for reactive updates with offline-first pattern
     */
    fun getEducationalVideos(
        category: String? = null,
        limit: Int = 15
    ): Flow<Result<List<DiscoverContent.Video>>>

    /**
     * Get mixed content feed combining articles, news, and videos
     * Sorted by publication date for chronological display
     */
    fun getMixedContentFeed(
        category: String? = null,
        limit: Int = 30
    ): Flow<Result<List<DiscoverContent>>>

    /**
     * Get trending content based on engagement and relevance
     */
    fun getTrendingContent(limit: Int = 20): Flow<Result<List<DiscoverContent>>>

    /**
     * Get breaking news items
     */
    fun getBreakingNews(limit: Int = 5): Flow<Result<List<DiscoverContent.News>>>

    // ==================== CONTENT SEARCH & FILTERING ====================

    /**
     * Search content across all types (articles, news, videos)
     * Returns unified results sorted by relevance and credibility
     */
    suspend fun searchContent(
        query: String,
        contentTypes: List<String> = listOf("article", "news", "video"),
        limit: Int = 50
    ): Result<List<DiscoverContent>>

    /**
     * Get content filtered by category
     */
    suspend fun getContentByCategory(
        category: HealthContentCategory,
        limit: Int = 30
    ): Result<List<DiscoverContent>>

    /**
     * Get content filtered by credibility score threshold
     */
    suspend fun getContentByCredibility(
        minCredibilityScore: Int = 3,
        limit: Int = 30
    ): Result<List<DiscoverContent>>

    // ==================== INDIVIDUAL CONTENT ACCESS ====================

    /**
     * Get specific article by ID
     */
    suspend fun getArticleById(articleId: String): Result<DiscoverContent.Article?>

    /**
     * Get specific news item by ID
     */
    suspend fun getNewsById(newsId: String): Result<DiscoverContent.News?>

    /**
     * Get specific video by ID
     */
    suspend fun getVideoById(videoId: String): Result<DiscoverContent.Video?>

    // ==================== BOOKMARKS & READING PROGRESS ====================

    /**
     * Toggle bookmark status for any content type
     */
    suspend fun toggleBookmark(contentId: String, contentType: String): Result<Boolean>

    /**
     * Add bookmark for content
     */
    suspend fun addBookmark(contentId: String, contentType: String): Result<Unit>

    /**
     * Remove bookmark for content
     */
    suspend fun removeBookmark(contentId: String): Result<Unit>

    /**
     * Check if content is bookmarked
     */
    suspend fun isContentBookmarked(contentId: String): Result<Boolean>

    /**
     * Get all bookmarked content
     */
    fun getBookmarkedContent(): Flow<Result<List<DiscoverContent>>>

    /**
     * Get bookmarks by content type
     */
    fun getBookmarksByType(contentType: String): Flow<Result<List<DiscoverContent>>>

    /**
     * Update reading progress for articles
     */
    suspend fun updateReadingProgress(articleId: String, progress: Float): Result<Unit>

    /**
     * Update watch progress for videos
     */
    suspend fun updateWatchProgress(videoId: String, progress: Float): Result<Unit>

    // ==================== OFFLINE & SYNC MANAGEMENT ====================

    /**
     * Sync content from remote sources (Firebase)
     * Implements background sync with exponential backoff
     */
    suspend fun syncContentFromRemote(): Result<Unit>

    /**
     * Get cached content for offline access
     */
    suspend fun getCachedContent(limit: Int = 50): Result<List<DiscoverContent>>

    /**
     * Download video for offline viewing
     */
    suspend fun downloadVideoForOffline(videoId: String): Result<Unit>

    /**
     * Remove offline video download
     */
    suspend fun removeOfflineVideo(videoId: String): Result<Unit>

    /**
     * Get offline downloaded videos
     */
    suspend fun getOfflineVideos(): Result<List<DiscoverContent.Video>>

    /**
     * Check if content is available offline
     */
    suspend fun isContentAvailableOffline(contentId: String, contentType: String): Boolean

    // ==================== CONTENT VALIDATION & CREDIBILITY ====================

    /**
     * Validate content credibility and generate warnings
     */
    suspend fun validateContentCredibility(contentId: String, contentType: String): Result<ContentValidationResult>

    /**
     * Report content issue (inappropriate, incorrect, outdated)
     */
    suspend fun reportContentIssue(
        contentId: String,
        contentType: String,
        issueType: String,
        description: String
    ): Result<Unit>

    /**
     * Report content issue using ContentReport model
     */
    suspend fun reportContentIssue(
        report: com.example.health_assistant.features.discover.domain.model.ContentReport
    ): Result<Unit>

    /**
     * Get user's submitted content reports
     */
    suspend fun getUserContentReports(userId: String): Result<List<com.example.health_assistant.features.discover.domain.model.ContentReport>>

    /**
     * Get content credibility information
     */
    suspend fun getContentCredibilityInfo(contentId: String, contentType: String): Result<ContentValidationResult>

    // ==================== CACHE MANAGEMENT ====================

    /**
     * Clean up old cached content beyond retention period
     */
    suspend fun cleanupOldContent(retentionDays: Int = 30): Result<Int>

    /**
     * Clean up orphaned bookmarks
     */
    suspend fun cleanupOrphanedBookmarks(): Result<Int>

    /**
     * Get cache statistics for monitoring
     */
    suspend fun getCacheStatistics(): Result<CacheStatistics>

    /**
     * Clear all cached content for current user
     */
    suspend fun clearUserCache(): Result<Unit>

    // ==================== USER PREFERENCES ====================

    /**
     * Get user's preferred content categories
     */
    suspend fun getUserPreferredCategories(): Result<List<HealthContentCategory>>

    /**
     * Update user's preferred content categories
     */
    suspend fun updateUserPreferredCategories(categories: List<HealthContentCategory>): Result<Unit>

    /**
     * Get personalized content recommendations
     */
    fun getPersonalizedRecommendations(limit: Int = 20): Flow<Result<List<DiscoverContent>>>
}

/**
 * Data class for cache statistics
 */
data class CacheStatistics(
    val totalArticles: Int,
    val totalNews: Int,
    val totalVideos: Int,
    val totalBookmarks: Int,
    val offlineVideos: Int,
    val cacheSize: Long, // in bytes
    val lastSyncTime: Long
)