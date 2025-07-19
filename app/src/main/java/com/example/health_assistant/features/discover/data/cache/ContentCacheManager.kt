package com.example.health_assistant.features.discover.data.cache

import android.content.Context
import android.util.Log
import com.example.health_assistant.core.util.Result
import com.example.health_assistant.features.discover.data.DiscoverDao
import com.example.health_assistant.features.discover.data.entity.*
import com.example.health_assistant.features.discover.domain.model.DiscoverContent
import com.example.health_assistant.features.discover.domain.model.HealthContentCategory
import com.example.health_assistant.features.discover.domain.repository.CacheStatistics
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages intelligent offline content storage with LRU eviction policies
 * Handles automatic prefetching of essential health information
 */
@Singleton
class ContentCacheManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val discoverDao: DiscoverDao
) {
    
    companion object {
        private const val TAG = "ContentCacheManager"
        
        // Cache size limits
        private const val MAX_ARTICLES_CACHE = 200
        private const val MAX_NEWS_CACHE = 100
        private const val MAX_VIDEOS_CACHE = 50
        private const val MAX_TOTAL_CACHE_SIZE_MB = 100L
        
        // Content retention periods
        private const val ESSENTIAL_CONTENT_RETENTION_DAYS = 90L
        private const val REGULAR_CONTENT_RETENTION_DAYS = 30L
        private const val NEWS_RETENTION_DAYS = 14L
        
        // Prefetch priorities
        private val HIGH_PRIORITY_CATEGORIES = listOf(
            "preventive-care", "emergency", "chronic-conditions"
        )
        private const val PREFETCH_BATCH_SIZE = 20
    }

    // Cache status tracking
    private val _cacheStatus = MutableStateFlow<CacheStatus>(CacheStatus.Idle)
    val cacheStatus: Flow<CacheStatus> = _cacheStatus.asStateFlow()
    
    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val syncStatus: Flow<SyncStatus> = _syncStatus.asStateFlow()

    // ==================== CACHE MANAGEMENT ====================

    /**
     * Initialize cache with essential health content prefetching
     */
    suspend fun initializeCache(userId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                _cacheStatus.value = CacheStatus.Initializing
                
                // Clean up old content first
                cleanupExpiredContent(userId)
                
                // Check if essential content needs prefetching
                val needsPrefetch = shouldPrefetchEssentialContent(userId)
                if (needsPrefetch) {
                    prefetchEssentialContent(userId)
                }
                
                _cacheStatus.value = CacheStatus.Ready
                Log.d(TAG, "Cache initialized successfully for user: $userId")
                Result.Success(Unit)
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize cache", e)
                _cacheStatus.value = CacheStatus.Error("Cache initialization failed")
                Result.Error(e, "Cache initialization failed: ${e.message}")
            }
        }
    }

    /**
     * Prefetch essential health content for offline access
     */
    suspend fun prefetchEssentialContent(userId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                _cacheStatus.value = CacheStatus.Prefetching
                
                // Prefetch high-priority categories
                for (category in HIGH_PRIORITY_CATEGORIES) {
                    prefetchCategoryContent(userId, category, PREFETCH_BATCH_SIZE)
                }
                
                // Mark essential content for extended retention
                markEssentialContent(userId)
                
                _cacheStatus.value = CacheStatus.Ready
                Log.d(TAG, "Essential content prefetched for user: $userId")
                Result.Success(Unit)
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to prefetch essential content", e)
                _cacheStatus.value = CacheStatus.Error("Prefetch failed")
                Result.Error(e, "Prefetch failed: ${e.message}")
            }
        }
    }

    /**
     * Manage cache size with LRU eviction policy
     */
    suspend fun manageCacheSize(userId: String): Result<CacheCleanupResult> {
        return withContext(Dispatchers.IO) {
            try {
                _cacheStatus.value = CacheStatus.Cleaning
                
                val initialStats = getCacheStatistics(userId)
                var cleanupResult = CacheCleanupResult()
                
                // Check if cache size exceeds limits
                if (initialStats.totalArticles > MAX_ARTICLES_CACHE) {
                    val articlesRemoved = evictOldestArticles(userId, initialStats.totalArticles - MAX_ARTICLES_CACHE)
                    cleanupResult = cleanupResult.copy(articlesRemoved = articlesRemoved)
                }
                
                if (initialStats.totalNews > MAX_NEWS_CACHE) {
                    val newsRemoved = evictOldestNews(userId, initialStats.totalNews - MAX_NEWS_CACHE)
                    cleanupResult = cleanupResult.copy(newsRemoved = newsRemoved)
                }
                
                if (initialStats.totalVideos > MAX_VIDEOS_CACHE) {
                    val videosRemoved = evictOldestVideos(userId, initialStats.totalVideos - MAX_VIDEOS_CACHE)
                    cleanupResult = cleanupResult.copy(videosRemoved = videosRemoved)
                }
                
                // Clean up expired content
                val expiredRemoved = cleanupExpiredContent(userId)
                cleanupResult = cleanupResult.copy(
                    expiredContentRemoved = expiredRemoved,
                    totalSpaceFreed = calculateSpaceFreed(cleanupResult)
                )
                
                _cacheStatus.value = CacheStatus.Ready
                Log.d(TAG, "Cache cleanup completed: $cleanupResult")
                Result.Success(cleanupResult)
                
            } catch (e: Exception) {
                Log.e(TAG, "Cache cleanup failed", e)
                _cacheStatus.value = CacheStatus.Error("Cleanup failed")
                Result.Error(e, "Cache cleanup failed: ${e.message}")
            }
        }
    }

    /**
     * Get comprehensive cache statistics
     */
    suspend fun getCacheStatistics(userId: String): CacheStatistics {
        return withContext(Dispatchers.IO) {
            try {
                val articleCount = discoverDao.getArticleCount(userId)
                val newsCount = discoverDao.getNewsCount(userId)
                val videoCount = discoverDao.getVideoCount(userId)
                val bookmarkCount = discoverDao.getBookmarkCount(userId)
                val offlineVideoCount = discoverDao.getOfflineVideoCount(userId)
                val lastSyncTime = getLastSyncTime(userId)
                
                // Estimate cache size (rough calculation)
                val estimatedCacheSize = estimateCacheSize(articleCount, newsCount, videoCount)
                
                CacheStatistics(
                    totalArticles = articleCount,
                    totalNews = newsCount,
                    totalVideos = videoCount,
                    totalBookmarks = bookmarkCount,
                    offlineVideos = offlineVideoCount,
                    cacheSize = estimatedCacheSize,
                    lastSyncTime = lastSyncTime
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get cache statistics", e)
                CacheStatistics(
                    totalArticles = 0,
                    totalNews = 0,
                    totalVideos = 0,
                    totalBookmarks = 0,
                    offlineVideos = 0,
                    cacheSize = 0L,
                    lastSyncTime = 0L
                )
            }
        }
    }

    /**
     * Check if content is available offline
     */
    suspend fun isContentAvailableOffline(
        contentId: String,
        contentType: String,
        userId: String
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                when (contentType) {
                    "article" -> discoverDao.getHealthArticleById(contentId, userId) != null
                    "news" -> discoverDao.getHealthNewsById(contentId, userId) != null
                    "video" -> {
                        val video = discoverDao.getEducationalVideoById(contentId, userId)
                        video?.isDownloadedOffline == true
                    }
                    else -> false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking offline availability", e)
                false
            }
        }
    }

    /**
     * Get offline content availability status
     */
    suspend fun getOfflineContentStatus(userId: String): OfflineContentStatus {
        return withContext(Dispatchers.IO) {
            try {
                val stats = getCacheStatistics(userId)
                val essentialContentAvailable = checkEssentialContentAvailability(userId)
                val lastSyncAge = System.currentTimeMillis() - stats.lastSyncTime
                val syncFreshness = when {
                    lastSyncAge < TimeUnit.HOURS.toMillis(1) -> SyncFreshness.Fresh
                    lastSyncAge < TimeUnit.HOURS.toMillis(24) -> SyncFreshness.Recent
                    lastSyncAge < TimeUnit.DAYS.toMillis(7) -> SyncFreshness.Stale
                    else -> SyncFreshness.VeryStale
                }
                
                OfflineContentStatus(
                    isOfflineReady = stats.totalArticles > 0 || stats.totalNews > 0,
                    essentialContentAvailable = essentialContentAvailable,
                    totalCachedItems = stats.totalArticles + stats.totalNews + stats.totalVideos,
                    cacheSize = stats.cacheSize,
                    syncFreshness = syncFreshness,
                    lastSyncTime = stats.lastSyncTime
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error getting offline content status", e)
                OfflineContentStatus()
            }
        }
    }

    // ==================== SYNC STATUS MANAGEMENT ====================

    /**
     * Update sync status for UI indicators
     */
    fun updateSyncStatus(status: SyncStatus) {
        _syncStatus.value = status
    }

    /**
     * Get current sync status
     */
    fun getCurrentSyncStatus(): SyncStatus = _syncStatus.value

    // ==================== PRIVATE HELPER METHODS ====================

    private suspend fun shouldPrefetchEssentialContent(userId: String): Boolean {
        val essentialContentCount = discoverDao.getEssentialContentCount(userId)
        return essentialContentCount < PREFETCH_BATCH_SIZE
    }

    private suspend fun prefetchCategoryContent(userId: String, category: String, limit: Int) {
        // This would typically trigger a sync operation for specific category
        // For now, we'll mark the intent and let the repository handle the actual sync
        Log.d(TAG, "Prefetch requested for category: $category, limit: $limit")
    }

    private suspend fun markEssentialContent(userId: String) {
        val currentTime = System.currentTimeMillis()
        val essentialRetentionTime = currentTime + TimeUnit.DAYS.toMillis(ESSENTIAL_CONTENT_RETENTION_DAYS)
        
        // Mark high-priority category content as essential
        for (category in HIGH_PRIORITY_CATEGORIES) {
            discoverDao.markContentAsEssential(userId, category, essentialRetentionTime)
        }
    }

    private suspend fun evictOldestArticles(userId: String, countToRemove: Int): Int {
        return try {
            val oldestArticles = discoverDao.getOldestArticles(userId, countToRemove)
            // Filter out high-priority categories (essential content)
            val nonEssentialArticles = oldestArticles.filter { article ->
                !HIGH_PRIORITY_CATEGORIES.contains(article.category)
            }
            
            for (article in nonEssentialArticles) {
                discoverDao.deleteHealthArticle(article.id, userId)
            }
            
            nonEssentialArticles.size
        } catch (e: Exception) {
            Log.e(TAG, "Error evicting oldest articles", e)
            0
        }
    }

    private suspend fun evictOldestNews(userId: String, countToRemove: Int): Int {
        return try {
            val oldestNews = discoverDao.getOldestNews(userId, countToRemove)
            
            for (news in oldestNews) {
                discoverDao.deleteHealthNews(news.id, userId)
            }
            
            oldestNews.size
        } catch (e: Exception) {
            Log.e(TAG, "Error evicting oldest news", e)
            0
        }
    }

    private suspend fun evictOldestVideos(userId: String, countToRemove: Int): Int {
        return try {
            val oldestVideos = discoverDao.getOldestVideos(userId, countToRemove)
            val nonDownloadedVideos = oldestVideos.filter { !it.isDownloadedOffline }
            
            for (video in nonDownloadedVideos) {
                discoverDao.deleteEducationalVideo(video.id, userId)
            }
            
            nonDownloadedVideos.size
        } catch (e: Exception) {
            Log.e(TAG, "Error evicting oldest videos", e)
            0
        }
    }

    private suspend fun cleanupExpiredContent(userId: String): Int {
        val currentTime = System.currentTimeMillis()
        val regularCutoff = currentTime - TimeUnit.DAYS.toMillis(REGULAR_CONTENT_RETENTION_DAYS)
        val newsCutoff = currentTime - TimeUnit.DAYS.toMillis(NEWS_RETENTION_DAYS)
        
        var removedCount = 0
        
        try {
            // Clean up expired articles (excluding essential content)
            removedCount += discoverDao.cleanupExpiredArticles(regularCutoff, userId)
            
            // Clean up expired news
            removedCount += discoverDao.cleanupExpiredNews(newsCutoff, userId)
            
            // Clean up expired videos (excluding downloaded ones)
            removedCount += discoverDao.cleanupExpiredVideos(regularCutoff, userId)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up expired content", e)
        }
        
        return removedCount
    }

    private suspend fun checkEssentialContentAvailability(userId: String): Boolean {
        return try {
            val essentialCount = discoverDao.getEssentialContentCount(userId)
            essentialCount >= PREFETCH_BATCH_SIZE / 2 // At least half of prefetch batch
        } catch (e: Exception) {
            Log.e(TAG, "Error checking essential content availability", e)
            false
        }
    }

    private fun estimateCacheSize(articleCount: Int, newsCount: Int, videoCount: Int): Long {
        // Rough estimation: articles ~50KB, news ~30KB, videos ~5MB (metadata only)
        val articleSize = articleCount * 50 * 1024L
        val newsSize = newsCount * 30 * 1024L
        val videoSize = videoCount * 5 * 1024L // Metadata only, not actual video files
        
        return articleSize + newsSize + videoSize
    }

    private fun calculateSpaceFreed(cleanupResult: CacheCleanupResult): Long {
        // Estimate space freed based on removed items
        val articleSpace = cleanupResult.articlesRemoved * 50 * 1024L
        val newsSpace = cleanupResult.newsRemoved * 30 * 1024L
        val videoSpace = cleanupResult.videosRemoved * 5 * 1024L
        val expiredSpace = cleanupResult.expiredContentRemoved * 40 * 1024L // Average
        
        return articleSpace + newsSpace + videoSpace + expiredSpace
    }

    private suspend fun getLastSyncTime(userId: String): Long {
        return try {
            discoverDao.getLastSyncTime(userId) ?: 0L
        } catch (e: Exception) {
            Log.e(TAG, "Error getting last sync time", e)
            0L
        }
    }
}

// ==================== DATA CLASSES ====================

/**
 * Represents the current cache status
 */
sealed class CacheStatus {
    object Idle : CacheStatus()
    object Initializing : CacheStatus()
    object Prefetching : CacheStatus()
    object Cleaning : CacheStatus()
    object Ready : CacheStatus()
    data class Error(val message: String) : CacheStatus()
}

/**
 * Represents the current sync status
 */
sealed class SyncStatus {
    object Idle : SyncStatus()
    object Syncing : SyncStatus()
    data class Progress(val percentage: Int, val currentItem: String) : SyncStatus()
    object Success : SyncStatus()
    data class Error(val message: String) : SyncStatus()
    data class Partial(val successCount: Int, val failureCount: Int) : SyncStatus()
}

/**
 * Result of cache cleanup operation
 */
data class CacheCleanupResult(
    val articlesRemoved: Int = 0,
    val newsRemoved: Int = 0,
    val videosRemoved: Int = 0,
    val expiredContentRemoved: Int = 0,
    val totalSpaceFreed: Long = 0L
)

/**
 * Offline content availability status
 */
data class OfflineContentStatus(
    val isOfflineReady: Boolean = false,
    val essentialContentAvailable: Boolean = false,
    val totalCachedItems: Int = 0,
    val cacheSize: Long = 0L,
    val syncFreshness: SyncFreshness = SyncFreshness.VeryStale,
    val lastSyncTime: Long = 0L
)

/**
 * Sync freshness indicator
 */
enum class SyncFreshness {
    Fresh,      // < 1 hour
    Recent,     // < 24 hours
    Stale,      // < 7 days
    VeryStale   // > 7 days
}