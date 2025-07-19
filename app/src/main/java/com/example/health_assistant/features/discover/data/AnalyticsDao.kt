package com.example.health_assistant.features.discover.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.example.health_assistant.features.discover.data.entity.ContentAnalyticsEntity
import com.example.health_assistant.features.discover.data.entity.UserEngagementEntity
import com.example.health_assistant.features.discover.data.entity.ContentRecommendationEntity
import com.example.health_assistant.features.discover.data.entity.ABTestEntity

/**
 * Data Access Object for analytics and engagement tracking
 * Provides methods for storing and retrieving user engagement metrics
 */
@Dao
interface AnalyticsDao {

    // Content Analytics Operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnalyticsEvent(event: ContentAnalyticsEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnalyticsEvents(events: List<ContentAnalyticsEntity>)

    @Query("SELECT * FROM content_analytics WHERE userId = :userId AND contentId = :contentId ORDER BY timestamp DESC")
    suspend fun getContentAnalytics(userId: String, contentId: String): List<ContentAnalyticsEntity>

    @Query("SELECT * FROM content_analytics WHERE userId = :userId AND eventType = :eventType AND timestamp >= :startTime ORDER BY timestamp DESC")
    suspend fun getAnalyticsByEventType(userId: String, eventType: String, startTime: Long): List<ContentAnalyticsEntity>

    @Query("SELECT * FROM content_analytics WHERE userId = :userId AND timestamp >= :startTime ORDER BY timestamp DESC")
    suspend fun getRecentAnalytics(userId: String, startTime: Long): List<ContentAnalyticsEntity>

    @Query("SELECT contentId, COUNT(*) as viewCount FROM content_analytics WHERE eventType = 'view' AND timestamp >= :startTime GROUP BY contentId ORDER BY viewCount DESC LIMIT :limit")
    suspend fun getTrendingContent(startTime: Long, limit: Int): List<TrendingContentResult>

    @Query("SELECT AVG(duration) FROM content_analytics WHERE userId = :userId AND contentId = :contentId AND eventType = 'read_complete'")
    suspend fun getAverageReadingTime(userId: String, contentId: String): Long?

    @Query("DELETE FROM content_analytics WHERE timestamp < :cutoffTime")
    suspend fun cleanupOldAnalytics(cutoffTime: Long)

    // User Engagement Operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserEngagement(engagement: UserEngagementEntity)

    @Query("SELECT * FROM user_engagement WHERE userId = :userId")
    suspend fun getUserEngagement(userId: String): List<UserEngagementEntity>

    @Query("SELECT * FROM user_engagement WHERE userId = :userId AND category = :category AND contentType = :contentType")
    suspend fun getUserEngagementByCategory(userId: String, category: String, contentType: String): UserEngagementEntity?

    @Query("SELECT * FROM user_engagement WHERE userId = :userId ORDER BY engagementScore DESC LIMIT :limit")
    suspend fun getTopEngagementCategories(userId: String, limit: Int): List<UserEngagementEntity>

    @Query("UPDATE user_engagement SET totalViews = totalViews + 1, lastEngagement = :timestamp, lastUpdated = :timestamp WHERE userId = :userId AND category = :category AND contentType = :contentType")
    suspend fun incrementViewCount(userId: String, category: String, contentType: String, timestamp: Long)

    @Query("UPDATE user_engagement SET totalReadingTime = totalReadingTime + :duration, averageReadingTime = (totalReadingTime + :duration) / totalViews, lastEngagement = :timestamp, lastUpdated = :timestamp WHERE userId = :userId AND category = :category AND contentType = :contentType")
    suspend fun updateReadingTime(userId: String, category: String, contentType: String, duration: Long, timestamp: Long)

    // Content Recommendations Operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecommendation(recommendation: ContentRecommendationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecommendations(recommendations: List<ContentRecommendationEntity>)

    @Query("SELECT * FROM content_recommendations WHERE userId = :userId AND (expiresAt = 0 OR expiresAt > :currentTime) ORDER BY score DESC LIMIT :limit")
    suspend fun getActiveRecommendations(userId: String, currentTime: Long, limit: Int): List<ContentRecommendationEntity>

    @Query("SELECT * FROM content_recommendations WHERE userId = :userId AND recommendationType = :type AND (expiresAt = 0 OR expiresAt > :currentTime) ORDER BY score DESC LIMIT :limit")
    suspend fun getRecommendationsByType(userId: String, type: String, currentTime: Long, limit: Int): List<ContentRecommendationEntity>

    @Query("UPDATE content_recommendations SET isShown = 1 WHERE id = :recommendationId")
    suspend fun markRecommendationShown(recommendationId: String)

    @Query("UPDATE content_recommendations SET isClicked = 1 WHERE id = :recommendationId")
    suspend fun markRecommendationClicked(recommendationId: String)

    @Query("UPDATE content_recommendations SET isBookmarked = 1 WHERE id = :recommendationId")
    suspend fun markRecommendationBookmarked(recommendationId: String)

    @Query("DELETE FROM content_recommendations WHERE expiresAt > 0 AND expiresAt < :currentTime")
    suspend fun cleanupExpiredRecommendations(currentTime: Long)

    // A/B Testing Operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertABTest(abTest: ABTestEntity)

    @Query("SELECT * FROM ab_tests WHERE userId = :userId AND testName = :testName AND isActive = 1")
    suspend fun getActiveABTest(userId: String, testName: String): ABTestEntity?

    @Query("SELECT * FROM ab_tests WHERE userId = :userId AND isActive = 1")
    suspend fun getActiveABTests(userId: String): List<ABTestEntity>

    @Query("UPDATE ab_tests SET impressions = impressions + 1, lastInteraction = :timestamp WHERE id = :testId")
    suspend fun recordABTestImpression(testId: String, timestamp: Long)

    @Query("UPDATE ab_tests SET clicks = clicks + 1, lastInteraction = :timestamp WHERE id = :testId")
    suspend fun recordABTestClick(testId: String, timestamp: Long)

    @Query("UPDATE ab_tests SET conversions = conversions + 1, lastInteraction = :timestamp WHERE id = :testId")
    suspend fun recordABTestConversion(testId: String, timestamp: Long)

    @Query("UPDATE ab_tests SET engagementTime = engagementTime + :duration, lastInteraction = :timestamp WHERE id = :testId")
    suspend fun updateABTestEngagementTime(testId: String, duration: Long, timestamp: Long)

    @Query("UPDATE ab_tests SET isActive = 0 WHERE endDate < :currentTime")
    suspend fun deactivateExpiredABTests(currentTime: Long)

    // Analytics Aggregation Queries
    @Query("""
        SELECT category, COUNT(*) as count, AVG(duration) as avgDuration 
        FROM content_analytics 
        WHERE userId = :userId AND eventType = 'view' AND timestamp >= :startTime 
        GROUP BY category 
        ORDER BY count DESC
    """)
    suspend fun getCategoryEngagementStats(userId: String, startTime: Long): List<CategoryEngagementResult>

    @Query("""
        SELECT contentType, COUNT(*) as count, AVG(progress) as avgProgress 
        FROM content_analytics 
        WHERE userId = :userId AND eventType IN ('read_progress', 'read_complete') AND timestamp >= :startTime 
        GROUP BY contentType
    """)
    suspend fun getContentTypeEngagementStats(userId: String, startTime: Long): List<ContentTypeEngagementResult>

    @Query("""
        SELECT DATE(timestamp/1000, 'unixepoch') as date, COUNT(*) as count 
        FROM content_analytics 
        WHERE userId = :userId AND eventType = 'view' AND timestamp >= :startTime 
        GROUP BY date 
        ORDER BY date DESC
    """)
    suspend fun getDailyEngagementStats(userId: String, startTime: Long): List<DailyEngagementResult>
}

// Data classes for query results
data class TrendingContentResult(
    val contentId: String,
    val viewCount: Int
)

data class CategoryEngagementResult(
    val category: String,
    val count: Int,
    val avgDuration: Double
)

data class ContentTypeEngagementResult(
    val contentType: String,
    val count: Int,
    val avgProgress: Double
)

data class DailyEngagementResult(
    val date: String,
    val count: Int
)