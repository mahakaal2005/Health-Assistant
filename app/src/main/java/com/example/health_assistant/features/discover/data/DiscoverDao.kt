package com.example.health_assistant.features.discover.data

import androidx.room.*
import com.example.health_assistant.features.discover.data.entity.HealthArticleEntity
import com.example.health_assistant.features.discover.data.entity.HealthNewsEntity
import com.example.health_assistant.features.discover.data.entity.EducationalVideoEntity
import com.example.health_assistant.features.discover.data.entity.ContentBookmarkEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Discover feature content
 * Provides CRUD operations, search functionality, and cache management
 */
@Dao
interface DiscoverDao {

    // ==================== HEALTH ARTICLES ====================
    
    /**
     * Get health articles with optional category filtering
     */
    @Query("""
        SELECT * FROM health_articles 
        WHERE userId = :userId 
        AND (:category IS NULL OR category = :category) 
        ORDER BY publishedDate DESC 
        LIMIT :limit
    """)
    suspend fun getHealthArticles(userId: String, category: String?, limit: Int): List<HealthArticleEntity>

    /**
     * Get health articles as Flow for reactive updates
     */
    @Query("""
        SELECT * FROM health_articles 
        WHERE userId = :userId 
        AND (:category IS NULL OR category = :category) 
        ORDER BY publishedDate DESC 
        LIMIT :limit
    """)
    fun getHealthArticlesFlow(userId: String, category: String?, limit: Int): Flow<List<HealthArticleEntity>>

    /**
     * Get a specific health article by ID
     */
    @Query("SELECT * FROM health_articles WHERE id = :articleId AND userId = :userId")
    suspend fun getHealthArticleById(articleId: String, userId: String): HealthArticleEntity?

    /**
     * Insert or update health articles
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHealthArticles(articles: List<HealthArticleEntity>)

    /**
     * Insert or update a single health article
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHealthArticle(article: HealthArticleEntity)

    /**
     * Update reading progress for an article
     */
    @Query("UPDATE health_articles SET readProgress = :progress WHERE id = :articleId AND userId = :userId")
    suspend fun updateArticleReadingProgress(articleId: String, progress: Float, userId: String)

    /**
     * Update bookmark status for an article
     */
    @Query("UPDATE health_articles SET isBookmarked = :isBookmarked WHERE id = :articleId AND userId = :userId")
    suspend fun updateArticleBookmarkStatus(articleId: String, isBookmarked: Boolean, userId: String)

    /**
     * Get bookmarked articles
     */
    @Query("SELECT * FROM health_articles WHERE userId = :userId AND isBookmarked = 1 ORDER BY publishedDate DESC")
    suspend fun getBookmarkedArticles(userId: String): List<HealthArticleEntity>

    /**
     * Search articles by title, content, or tags
     */
    @Query("""
        SELECT * FROM health_articles 
        WHERE userId = :userId 
        AND (title LIKE '%' || :query || '%' 
             OR content LIKE '%' || :query || '%' 
             OR tags LIKE '%' || :query || '%')
        ORDER BY credibilityScore DESC, publishedDate DESC
        LIMIT :limit
    """)
    suspend fun searchHealthArticles(userId: String, query: String, limit: Int): List<HealthArticleEntity>

    /**
     * Get articles by credibility score threshold
     */
    @Query("""
        SELECT * FROM health_articles 
        WHERE userId = :userId 
        AND credibilityScore >= :minCredibilityScore 
        ORDER BY credibilityScore DESC, publishedDate DESC 
        LIMIT :limit
    """)
    suspend fun getArticlesByCredibility(userId: String, minCredibilityScore: Int, limit: Int): List<HealthArticleEntity>

    // ==================== HEALTH NEWS ====================

    /**
     * Get health news with optional category filtering
     */
    @Query("""
        SELECT * FROM health_news 
        WHERE userId = :userId 
        AND (:category IS NULL OR category = :category) 
        ORDER BY publishedDate DESC 
        LIMIT :limit
    """)
    suspend fun getHealthNews(userId: String, category: String?, limit: Int): List<HealthNewsEntity>

    /**
     * Get health news as Flow for reactive updates
     */
    @Query("""
        SELECT * FROM health_news 
        WHERE userId = :userId 
        AND (:category IS NULL OR category = :category) 
        ORDER BY publishedDate DESC 
        LIMIT :limit
    """)
    fun getHealthNewsFlow(userId: String, category: String?, limit: Int): Flow<List<HealthNewsEntity>>

    /**
     * Get breaking news
     */
    @Query("""
        SELECT * FROM health_news 
        WHERE userId = :userId 
        AND isBreakingNews = 1 
        ORDER BY publishedDate DESC 
        LIMIT :limit
    """)
    suspend fun getBreakingNews(userId: String, limit: Int): List<HealthNewsEntity>

    /**
     * Get a specific news item by ID
     */
    @Query("SELECT * FROM health_news WHERE id = :newsId AND userId = :userId")
    suspend fun getHealthNewsById(newsId: String, userId: String): HealthNewsEntity?

    /**
     * Insert or update health news
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHealthNews(news: List<HealthNewsEntity>)

    /**
     * Insert or update a single news item
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHealthNewsItem(news: HealthNewsEntity)

    /**
     * Search news by headline, summary, or content
     */
    @Query("""
        SELECT * FROM health_news 
        WHERE userId = :userId 
        AND (headline LIKE '%' || :query || '%' 
             OR summary LIKE '%' || :query || '%' 
             OR fullContent LIKE '%' || :query || '%')
        ORDER BY publishedDate DESC
        LIMIT :limit
    """)
    suspend fun searchHealthNews(userId: String, query: String, limit: Int): List<HealthNewsEntity>

    /**
     * Get news by source credibility
     */
    @Query("""
        SELECT * FROM health_news 
        WHERE userId = :userId 
        AND sourceCredibility = :credibility 
        ORDER BY publishedDate DESC 
        LIMIT :limit
    """)
    suspend fun getNewsByCredibility(userId: String, credibility: String, limit: Int): List<HealthNewsEntity>

    // ==================== EDUCATIONAL VIDEOS ====================

    /**
     * Get educational videos with optional category filtering
     */
    @Query("""
        SELECT * FROM educational_videos 
        WHERE userId = :userId 
        AND (:category IS NULL OR category = :category) 
        ORDER BY publishedDate DESC 
        LIMIT :limit
    """)
    suspend fun getEducationalVideos(userId: String, category: String?, limit: Int): List<EducationalVideoEntity>

    /**
     * Get educational videos as Flow for reactive updates
     */
    @Query("""
        SELECT * FROM educational_videos 
        WHERE userId = :userId 
        AND (:category IS NULL OR category = :category) 
        ORDER BY publishedDate DESC 
        LIMIT :limit
    """)
    fun getEducationalVideosFlow(userId: String, category: String?, limit: Int): Flow<List<EducationalVideoEntity>>

    /**
     * Get a specific video by ID
     */
    @Query("SELECT * FROM educational_videos WHERE id = :videoId AND userId = :userId")
    suspend fun getEducationalVideoById(videoId: String, userId: String): EducationalVideoEntity?

    /**
     * Insert or update educational videos
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEducationalVideos(videos: List<EducationalVideoEntity>)

    /**
     * Insert or update a single educational video
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEducationalVideo(video: EducationalVideoEntity)

    /**
     * Update watch progress for a video
     */
    @Query("UPDATE educational_videos SET watchProgress = :progress WHERE id = :videoId AND userId = :userId")
    suspend fun updateVideoWatchProgress(videoId: String, progress: Float, userId: String)

    /**
     * Update offline download status for a video
     */
    @Query("UPDATE educational_videos SET isDownloadedOffline = :isDownloaded WHERE id = :videoId AND userId = :userId")
    suspend fun updateVideoOfflineStatus(videoId: String, isDownloaded: Boolean, userId: String)

    /**
     * Get offline downloaded videos
     */
    @Query("SELECT * FROM educational_videos WHERE userId = :userId AND isDownloadedOffline = 1 ORDER BY publishedDate DESC")
    suspend fun getOfflineVideos(userId: String): List<EducationalVideoEntity>

    /**
     * Search videos by title or description
     */
    @Query("""
        SELECT * FROM educational_videos 
        WHERE userId = :userId 
        AND (title LIKE '%' || :query || '%' 
             OR description LIKE '%' || :query || '%')
        ORDER BY publishedDate DESC
        LIMIT :limit
    """)
    suspend fun searchEducationalVideos(userId: String, query: String, limit: Int): List<EducationalVideoEntity>

    /**
     * Get videos by difficulty level
     */
    @Query("""
        SELECT * FROM educational_videos 
        WHERE userId = :userId 
        AND difficultyLevel = :difficultyLevel 
        ORDER BY publishedDate DESC 
        LIMIT :limit
    """)
    suspend fun getVideosByDifficulty(userId: String, difficultyLevel: String, limit: Int): List<EducationalVideoEntity>

    // ==================== CONTENT BOOKMARKS ====================

    /**
     * Get all bookmarks for a user
     */
    @Query("SELECT * FROM content_bookmarks WHERE userId = :userId ORDER BY bookmarkedDate DESC")
    suspend fun getContentBookmarks(userId: String): List<ContentBookmarkEntity>

    /**
     * Get bookmarks by content type
     */
    @Query("""
        SELECT * FROM content_bookmarks 
        WHERE userId = :userId 
        AND contentType = :contentType 
        ORDER BY bookmarkedDate DESC
    """)
    suspend fun getBookmarksByType(userId: String, contentType: String): List<ContentBookmarkEntity>

    /**
     * Check if content is bookmarked
     */
    @Query("SELECT COUNT(*) > 0 FROM content_bookmarks WHERE contentId = :contentId AND userId = :userId")
    suspend fun isContentBookmarked(contentId: String, userId: String): Boolean

    /**
     * Insert a bookmark
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: ContentBookmarkEntity)

    /**
     * Remove a bookmark
     */
    @Query("DELETE FROM content_bookmarks WHERE contentId = :contentId AND userId = :userId")
    suspend fun removeBookmark(contentId: String, userId: String)

    /**
     * Remove all bookmarks for a user
     */
    @Query("DELETE FROM content_bookmarks WHERE userId = :userId")
    suspend fun removeAllBookmarks(userId: String)

    // ==================== MIXED CONTENT QUERIES ====================

    /**
     * Search across all content types
     */
    @Query("""
        SELECT 'article' as contentType, id, title, publishedDate, category, imageUrl 
        FROM health_articles 
        WHERE userId = :userId 
        AND (title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%' OR tags LIKE '%' || :query || '%')
        
        UNION ALL
        
        SELECT 'news' as contentType, id, headline as title, publishedDate, category, imageUrl 
        FROM health_news 
        WHERE userId = :userId 
        AND (headline LIKE '%' || :query || '%' OR summary LIKE '%' || :query || '%' OR fullContent LIKE '%' || :query || '%')
        
        UNION ALL
        
        SELECT 'video' as contentType, id, title, publishedDate, category, thumbnailUrl as imageUrl 
        FROM educational_videos 
        WHERE userId = :userId 
        AND (title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%')
        
        ORDER BY publishedDate DESC
        LIMIT :limit
    """)
    suspend fun searchAllContent(userId: String, query: String, limit: Int): List<ContentSearchResult>

    /**
     * Get mixed content by category
     */
    @Query("""
        SELECT 'article' as contentType, id, title, publishedDate, category, imageUrl 
        FROM health_articles 
        WHERE userId = :userId AND category = :category
        
        UNION ALL
        
        SELECT 'news' as contentType, id, headline as title, publishedDate, category, imageUrl 
        FROM health_news 
        WHERE userId = :userId AND category = :category
        
        UNION ALL
        
        SELECT 'video' as contentType, id, title, publishedDate, category, thumbnailUrl as imageUrl 
        FROM educational_videos 
        WHERE userId = :userId AND category = :category
        
        ORDER BY publishedDate DESC
        LIMIT :limit
    """)
    suspend fun getContentByCategory(userId: String, category: String, limit: Int): List<ContentSearchResult>

    // ==================== CACHE MANAGEMENT ====================

    /**
     * Clean up old articles beyond retention period
     */
    @Query("DELETE FROM health_articles WHERE publishedDate < :cutoffDate AND userId = :userId")
    suspend fun cleanupOldArticles(cutoffDate: Long, userId: String): Int

    /**
     * Clean up old news beyond retention period
     */
    @Query("DELETE FROM health_news WHERE publishedDate < :cutoffDate AND userId = :userId")
    suspend fun cleanupOldNews(cutoffDate: Long, userId: String): Int

    /**
     * Clean up old videos beyond retention period
     */
    @Query("DELETE FROM educational_videos WHERE publishedDate < :cutoffDate AND userId = :userId")
    suspend fun cleanupOldVideos(cutoffDate: Long, userId: String): Int

    /**
     * Clean up orphaned bookmarks (bookmarks without corresponding content)
     */
    @Query("""
        DELETE FROM content_bookmarks 
        WHERE userId = :userId 
        AND contentId NOT IN (
            SELECT id FROM health_articles WHERE userId = :userId
            UNION
            SELECT id FROM health_news WHERE userId = :userId
            UNION
            SELECT id FROM educational_videos WHERE userId = :userId
        )
    """)
    suspend fun cleanupOrphanedBookmarks(userId: String): Int

    /**
     * Get cache statistics
     */
    @Query("""
        SELECT 
            (SELECT COUNT(*) FROM health_articles WHERE userId = :userId) as articleCount,
            (SELECT COUNT(*) FROM health_news WHERE userId = :userId) as newsCount,
            (SELECT COUNT(*) FROM educational_videos WHERE userId = :userId) as videoCount,
            (SELECT COUNT(*) FROM content_bookmarks WHERE userId = :userId) as bookmarkCount
    """)
    suspend fun getCacheStatistics(userId: String): CacheStatistics

    /**
     * Get content count by category
     */
    @Query("""
        SELECT category, COUNT(*) as count 
        FROM (
            SELECT category FROM health_articles WHERE userId = :userId
            UNION ALL
            SELECT category FROM health_news WHERE userId = :userId
            UNION ALL
            SELECT category FROM educational_videos WHERE userId = :userId
        ) 
        GROUP BY category 
        ORDER BY count DESC
    """)
    suspend fun getContentCountByCategory(userId: String): List<CategoryCount>

    /**
     * Delete all content for a user (for account deletion)
     */
    @Query("DELETE FROM health_articles WHERE userId = :userId")
    suspend fun deleteAllArticles(userId: String)

    @Query("DELETE FROM health_news WHERE userId = :userId")
    suspend fun deleteAllNews(userId: String)

    @Query("DELETE FROM educational_videos WHERE userId = :userId")
    suspend fun deleteAllVideos(userId: String)

    @Query("DELETE FROM content_bookmarks WHERE userId = :userId")
    suspend fun deleteAllBookmarks(userId: String)

    /**
     * Batch delete all content for a user
     */
    @Transaction
    suspend fun deleteAllUserContent(userId: String) {
        deleteAllArticles(userId)
        deleteAllNews(userId)
        deleteAllVideos(userId)
        deleteAllBookmarks(userId)
    }

    // ==================== CACHE MANAGEMENT EXTENSIONS ====================

    /**
     * Get article count for cache management
     */
    @Query("SELECT COUNT(*) FROM health_articles WHERE userId = :userId")
    suspend fun getArticleCount(userId: String): Int

    /**
     * Get news count for cache management
     */
    @Query("SELECT COUNT(*) FROM health_news WHERE userId = :userId")
    suspend fun getNewsCount(userId: String): Int

    /**
     * Get video count for cache management
     */
    @Query("SELECT COUNT(*) FROM educational_videos WHERE userId = :userId")
    suspend fun getVideoCount(userId: String): Int

    /**
     * Get bookmark count for cache management
     */
    @Query("SELECT COUNT(*) FROM content_bookmarks WHERE userId = :userId")
    suspend fun getBookmarkCount(userId: String): Int

    /**
     * Get offline video count for cache management
     */
    @Query("SELECT COUNT(*) FROM educational_videos WHERE userId = :userId AND isDownloadedOffline = 1")
    suspend fun getOfflineVideoCount(userId: String): Int

    /**
     * Get essential content count (high-priority categories)
     */
    @Query("""
        SELECT COUNT(*) FROM (
            SELECT id FROM health_articles WHERE userId = :userId AND category IN ('preventive-care', 'emergency', 'chronic-conditions')
            UNION
            SELECT id FROM health_news WHERE userId = :userId AND category IN ('preventive-care', 'emergency', 'chronic-conditions')
            UNION
            SELECT id FROM educational_videos WHERE userId = :userId AND category IN ('preventive-care', 'emergency', 'chronic-conditions')
        )
    """)
    suspend fun getEssentialContentCount(userId: String): Int

    /**
     * Mark content as essential for extended retention
     */
    @Query("UPDATE health_articles SET lastUpdated = :retentionTime WHERE userId = :userId AND category = :category")
    suspend fun markArticlesAsEssential(userId: String, category: String, retentionTime: Long)

    @Query("UPDATE health_news SET publishedDate = :retentionTime WHERE userId = :userId AND category = :category")
    suspend fun markNewsAsEssential(userId: String, category: String, retentionTime: Long)

    @Query("UPDATE educational_videos SET publishedDate = :retentionTime WHERE userId = :userId AND category = :category")
    suspend fun markVideosAsEssential(userId: String, category: String, retentionTime: Long)

    @Transaction
    suspend fun markContentAsEssential(userId: String, category: String, retentionTime: Long) {
        markArticlesAsEssential(userId, category, retentionTime)
        markNewsAsEssential(userId, category, retentionTime)
        markVideosAsEssential(userId, category, retentionTime)
    }

    /**
     * Get oldest articles for LRU eviction
     */
    @Query("""
        SELECT * FROM health_articles 
        WHERE userId = :userId 
        ORDER BY lastUpdated ASC, publishedDate ASC 
        LIMIT :limit
    """)
    suspend fun getOldestArticles(userId: String, limit: Int): List<HealthArticleEntity>

    /**
     * Get oldest news for LRU eviction
     */
    @Query("""
        SELECT * FROM health_news 
        WHERE userId = :userId 
        ORDER BY publishedDate ASC 
        LIMIT :limit
    """)
    suspend fun getOldestNews(userId: String, limit: Int): List<HealthNewsEntity>

    /**
     * Get oldest videos for LRU eviction
     */
    @Query("""
        SELECT * FROM educational_videos 
        WHERE userId = :userId 
        ORDER BY publishedDate ASC 
        LIMIT :limit
    """)
    suspend fun getOldestVideos(userId: String, limit: Int): List<EducationalVideoEntity>

    /**
     * Delete specific health article
     */
    @Query("DELETE FROM health_articles WHERE id = :articleId AND userId = :userId")
    suspend fun deleteHealthArticle(articleId: String, userId: String)

    /**
     * Delete specific health news
     */
    @Query("DELETE FROM health_news WHERE id = :newsId AND userId = :userId")
    suspend fun deleteHealthNews(newsId: String, userId: String)

    /**
     * Delete specific educational video
     */
    @Query("DELETE FROM educational_videos WHERE id = :videoId AND userId = :userId")
    suspend fun deleteEducationalVideo(videoId: String, userId: String)

    /**
     * Clean up expired articles (excluding essential content with extended retention)
     */
    @Query("""
        DELETE FROM health_articles 
        WHERE userId = :userId 
        AND publishedDate < :cutoffDate 
        AND lastUpdated < :cutoffDate
    """)
    suspend fun cleanupExpiredArticles(cutoffDate: Long, userId: String): Int

    /**
     * Clean up expired news
     */
    @Query("DELETE FROM health_news WHERE userId = :userId AND publishedDate < :cutoffDate")
    suspend fun cleanupExpiredNews(cutoffDate: Long, userId: String): Int

    /**
     * Clean up expired videos (excluding downloaded ones)
     */
    @Query("""
        DELETE FROM educational_videos 
        WHERE userId = :userId 
        AND publishedDate < :cutoffDate 
        AND isDownloadedOffline = 0
    """)
    suspend fun cleanupExpiredVideos(cutoffDate: Long, userId: String): Int

    /**
     * Get last sync time from user preferences or metadata
     */
    @Query("""
        SELECT MAX(lastUpdated) FROM (
            SELECT MAX(lastUpdated) as lastUpdated FROM health_articles WHERE userId = :userId
            UNION
            SELECT MAX(publishedDate) as lastUpdated FROM health_news WHERE userId = :userId
            UNION
            SELECT MAX(publishedDate) as lastUpdated FROM educational_videos WHERE userId = :userId
        )
    """)
    suspend fun getLastSyncTime(userId: String): Long?
}

/**
 * Data class for search results across content types
 */
data class ContentSearchResult(
    val contentType: String,
    val id: String,
    val title: String,
    val publishedDate: Long,
    val category: String,
    val imageUrl: String?
)

/**
 * Data class for cache statistics
 */
data class CacheStatistics(
    val articleCount: Int,
    val newsCount: Int,
    val videoCount: Int,
    val bookmarkCount: Int
)

/**
 * Data class for category content counts
 */
data class CategoryCount(
    val category: String,
    val count: Int
)