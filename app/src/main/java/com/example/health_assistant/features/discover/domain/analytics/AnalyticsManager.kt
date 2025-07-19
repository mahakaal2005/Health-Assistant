package com.example.health_assistant.features.discover.domain.analytics

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import com.example.health_assistant.features.discover.data.AnalyticsDao
import com.example.health_assistant.features.discover.data.entity.ContentAnalyticsEntity
import com.example.health_assistant.features.discover.data.entity.UserEngagementEntity
import com.example.health_assistant.features.discover.domain.model.DiscoverContent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.json.JSONObject

/**
 * Manager for tracking content analytics and user engagement
 * Provides methods for recording user interactions and calculating engagement metrics
 */
@Singleton
class AnalyticsManager @Inject constructor(
    private val analyticsDao: AnalyticsDao,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context
) {
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private var currentSessionId = generateSessionId()
    
    // Session management
    fun startNewSession() {
        currentSessionId = generateSessionId()
    }
    
    private fun generateSessionId(): String = UUID.randomUUID().toString()
    
    // Content interaction tracking
    fun trackContentView(
        content: DiscoverContent,
        source: String = "feed"
    ) {
        coroutineScope.launch {
            val event = ContentAnalyticsEntity(
                id = UUID.randomUUID().toString(),
                contentId = content.id,
                contentType = content.getContentType(),
                userId = content.userId,
                sessionId = currentSessionId,
                eventType = "view",
                timestamp = System.currentTimeMillis(),
                category = content.category,
                source = source,
                deviceType = getDeviceType(),
                networkType = getNetworkType()
            )
            
            analyticsDao.insertAnalyticsEvent(event)
            updateUserEngagement(content.userId, content.category, content.getContentType(), "view")
        }
    }
    
    fun trackReadingStart(
        content: DiscoverContent
    ) {
        coroutineScope.launch {
            val event = ContentAnalyticsEntity(
                id = UUID.randomUUID().toString(),
                contentId = content.id,
                contentType = content.getContentType(),
                userId = content.userId,
                sessionId = currentSessionId,
                eventType = "read_start",
                timestamp = System.currentTimeMillis(),
                category = content.category,
                deviceType = getDeviceType(),
                networkType = getNetworkType()
            )
            
            analyticsDao.insertAnalyticsEvent(event)
        }
    }
    
    fun trackReadingProgress(
        content: DiscoverContent,
        progress: Float,
        duration: Long
    ) {
        coroutineScope.launch {
            val metadata = JSONObject().apply {
                put("reading_speed", if (duration > 0) progress / (duration / 1000.0) else 0.0)
                put("content_length", when (content) {
                    is DiscoverContent.Article -> content.readingTimeMinutes
                    is DiscoverContent.Video -> content.durationSeconds
                    else -> 0
                })
            }
            
            val event = ContentAnalyticsEntity(
                id = UUID.randomUUID().toString(),
                contentId = content.id,
                contentType = content.getContentType(),
                userId = content.userId,
                sessionId = currentSessionId,
                eventType = "read_progress",
                timestamp = System.currentTimeMillis(),
                duration = duration,
                progress = progress,
                metadata = metadata.toString(),
                category = content.category,
                deviceType = getDeviceType(),
                networkType = getNetworkType()
            )
            
            analyticsDao.insertAnalyticsEvent(event)
            updateUserEngagement(content.userId, content.category, content.getContentType(), "progress", duration)
        }
    }
    
    fun trackReadingComplete(
        content: DiscoverContent,
        totalDuration: Long
    ) {
        coroutineScope.launch {
            val metadata = JSONObject().apply {
                put("completion_time", totalDuration)
                put("estimated_reading_time", when (content) {
                    is DiscoverContent.Article -> content.readingTimeMinutes * 60 * 1000
                    is DiscoverContent.Video -> content.durationSeconds * 1000
                    else -> 0
                })
            }
            
            val event = ContentAnalyticsEntity(
                id = UUID.randomUUID().toString(),
                contentId = content.id,
                contentType = content.getContentType(),
                userId = content.userId,
                sessionId = currentSessionId,
                eventType = "read_complete",
                timestamp = System.currentTimeMillis(),
                duration = totalDuration,
                progress = 1.0f,
                metadata = metadata.toString(),
                category = content.category,
                deviceType = getDeviceType(),
                networkType = getNetworkType()
            )
            
            analyticsDao.insertAnalyticsEvent(event)
            updateUserEngagement(content.userId, content.category, content.getContentType(), "complete", totalDuration)
        }
    }
    
    fun trackBookmark(
        content: DiscoverContent,
        isBookmarked: Boolean
    ) {
        coroutineScope.launch {
            val event = ContentAnalyticsEntity(
                id = UUID.randomUUID().toString(),
                contentId = content.id,
                contentType = content.getContentType(),
                userId = content.userId,
                sessionId = currentSessionId,
                eventType = if (isBookmarked) "bookmark" else "unbookmark",
                timestamp = System.currentTimeMillis(),
                category = content.category,
                deviceType = getDeviceType(),
                networkType = getNetworkType()
            )
            
            analyticsDao.insertAnalyticsEvent(event)
            updateUserEngagement(content.userId, content.category, content.getContentType(), "bookmark")
        }
    }
    
    fun trackShare(
        content: DiscoverContent,
        shareMethod: String
    ) {
        coroutineScope.launch {
            val metadata = JSONObject().apply {
                put("share_method", shareMethod)
            }
            
            val event = ContentAnalyticsEntity(
                id = UUID.randomUUID().toString(),
                contentId = content.id,
                contentType = content.getContentType(),
                userId = content.userId,
                sessionId = currentSessionId,
                eventType = "share",
                timestamp = System.currentTimeMillis(),
                metadata = metadata.toString(),
                category = content.category,
                deviceType = getDeviceType(),
                networkType = getNetworkType()
            )
            
            analyticsDao.insertAnalyticsEvent(event)
            updateUserEngagement(content.userId, content.category, content.getContentType(), "share")
        }
    }
    
    fun trackSearch(
        userId: String,
        query: String,
        resultsCount: Int
    ) {
        coroutineScope.launch {
            val metadata = JSONObject().apply {
                put("query", query)
                put("results_count", resultsCount)
                put("query_length", query.length)
            }
            
            val event = ContentAnalyticsEntity(
                id = UUID.randomUUID().toString(),
                contentId = "search_${UUID.randomUUID()}",
                contentType = "search",
                userId = userId,
                sessionId = currentSessionId,
                eventType = "search",
                timestamp = System.currentTimeMillis(),
                metadata = metadata.toString(),
                deviceType = getDeviceType(),
                networkType = getNetworkType()
            )
            
            analyticsDao.insertAnalyticsEvent(event)
        }
    }
    
    // User engagement calculation
    private suspend fun updateUserEngagement(
        userId: String,
        category: String,
        contentType: String,
        eventType: String,
        duration: Long = 0L
    ) {
        val existing = analyticsDao.getUserEngagementByCategory(userId, category, contentType)
        val timestamp = System.currentTimeMillis()
        
        if (existing != null) {
            // Update existing engagement record
            when (eventType) {
                "view" -> analyticsDao.incrementViewCount(userId, category, contentType, timestamp)
                "progress", "complete" -> analyticsDao.updateReadingTime(userId, category, contentType, duration, timestamp)
            }
            
            // Recalculate engagement metrics
            recalculateEngagementMetrics(userId, category, contentType)
        } else {
            // Create new engagement record
            val newEngagement = UserEngagementEntity(
                id = UUID.randomUUID().toString(),
                userId = userId,
                category = category,
                contentType = contentType,
                totalViews = if (eventType == "view") 1 else 0,
                totalReadingTime = duration,
                averageReadingTime = duration,
                completionRate = if (eventType == "complete") 1.0f else 0f,
                bookmarkRate = if (eventType == "bookmark") 1.0f else 0f,
                shareRate = if (eventType == "share") 1.0f else 0f,
                engagementScore = calculateInitialEngagementScore(eventType),
                preferenceWeight = 0.5f,
                lastEngagement = timestamp
            )
            
            analyticsDao.insertUserEngagement(newEngagement)
        }
    }
    
    private suspend fun recalculateEngagementMetrics(
        userId: String,
        category: String,
        contentType: String
    ) {
        val startTime = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000) // Last 30 days
        val analytics = analyticsDao.getAnalyticsByEventType(userId, "view", startTime)
            .filter { it.category == category && it.contentType == contentType }
        
        if (analytics.isEmpty()) return
        
        val totalViews = analytics.size
        val completedReads = analytics.count { it.eventType == "read_complete" }
        val bookmarks = analytics.count { it.eventType == "bookmark" }
        val shares = analytics.count { it.eventType == "share" }
        val totalReadingTime = analytics.sumOf { it.duration }
        
        val completionRate = if (totalViews > 0) completedReads.toFloat() / totalViews else 0f
        val bookmarkRate = if (totalViews > 0) bookmarks.toFloat() / totalViews else 0f
        val shareRate = if (totalViews > 0) shares.toFloat() / totalViews else 0f
        val averageReadingTime = if (totalViews > 0) totalReadingTime / totalViews else 0L
        
        val engagementScore = calculateEngagementScore(completionRate, bookmarkRate, shareRate, averageReadingTime)
        val preferenceWeight = calculatePreferenceWeight(engagementScore, totalViews)
        
        val updatedEngagement = UserEngagementEntity(
            id = UUID.randomUUID().toString(),
            userId = userId,
            category = category,
            contentType = contentType,
            totalViews = totalViews,
            totalReadingTime = totalReadingTime,
            averageReadingTime = averageReadingTime,
            completionRate = completionRate,
            bookmarkRate = bookmarkRate,
            shareRate = shareRate,
            engagementScore = engagementScore,
            preferenceWeight = preferenceWeight,
            lastEngagement = System.currentTimeMillis()
        )
        
        analyticsDao.insertUserEngagement(updatedEngagement)
    }
    
    private fun calculateInitialEngagementScore(eventType: String): Float {
        return when (eventType) {
            "view" -> 0.1f
            "read_start" -> 0.2f
            "read_progress" -> 0.4f
            "read_complete" -> 0.8f
            "bookmark" -> 0.6f
            "share" -> 0.7f
            else -> 0.1f
        }
    }
    
    private fun calculateEngagementScore(
        completionRate: Float,
        bookmarkRate: Float,
        shareRate: Float,
        averageReadingTime: Long
    ): Float {
        val completionWeight = 0.4f
        val bookmarkWeight = 0.3f
        val shareWeight = 0.2f
        val timeWeight = 0.1f
        
        val normalizedTime = minOf(averageReadingTime / (5 * 60 * 1000f), 1.0f) // Normalize to 5 minutes max
        
        return (completionRate * completionWeight) +
                (bookmarkRate * bookmarkWeight) +
                (shareRate * shareWeight) +
                (normalizedTime * timeWeight)
    }
    
    private fun calculatePreferenceWeight(engagementScore: Float, totalViews: Int): Float {
        val baseWeight = engagementScore
        val volumeBonus = minOf(totalViews / 10f, 0.2f) // Up to 20% bonus for high volume
        return minOf(baseWeight + volumeBonus, 1.0f)
    }
    
    // Analytics retrieval
    suspend fun getUserEngagementStats(userId: String): List<UserEngagementEntity> {
        return withContext(Dispatchers.IO) {
            analyticsDao.getUserEngagement(userId)
        }
    }
    
    suspend fun getTrendingContent(days: Int = 7, limit: Int = 10): List<String> {
        return withContext(Dispatchers.IO) {
            val startTime = System.currentTimeMillis() - (days * 24 * 60 * 60 * 1000L)
            analyticsDao.getTrendingContent(startTime, limit).map { it.contentId }
        }
    }
    
    suspend fun getCategoryEngagementStats(userId: String, days: Int = 30): Flow<List<CategoryEngagementStats>> = flow {
        val startTime = System.currentTimeMillis() - (days * 24 * 60 * 60 * 1000L)
        val results = analyticsDao.getCategoryEngagementStats(userId, startTime)
        emit(results.map { 
            CategoryEngagementStats(
                category = it.category,
                viewCount = it.count,
                averageDuration = it.avgDuration.toLong()
            )
        })
    }
    
    // Cleanup operations
    suspend fun cleanupOldAnalytics(daysToKeep: Int = 90) {
        withContext(Dispatchers.IO) {
            val cutoffTime = System.currentTimeMillis() - (daysToKeep * 24 * 60 * 60 * 1000L)
            analyticsDao.cleanupOldAnalytics(cutoffTime)
        }
    }
    
    // Device and network detection
    private fun getDeviceType(): String {
        val configuration = context.resources.configuration
        val screenLayout = configuration.screenLayout and android.content.res.Configuration.SCREENLAYOUT_SIZE_MASK
        
        return when (screenLayout) {
            android.content.res.Configuration.SCREENLAYOUT_SIZE_LARGE,
            android.content.res.Configuration.SCREENLAYOUT_SIZE_XLARGE -> "tablet"
            else -> "mobile"
        }
    }
    
    private fun getNetworkType(): String {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            
            when {
                capabilities == null -> "offline"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "wifi"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "cellular"
                else -> "unknown"
            }
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            when {
                networkInfo == null || !networkInfo.isConnected -> "offline"
                networkInfo.type == ConnectivityManager.TYPE_WIFI -> "wifi"
                networkInfo.type == ConnectivityManager.TYPE_MOBILE -> "cellular"
                else -> "unknown"
            }
        }
    }
}

data class CategoryEngagementStats(
    val category: String,
    val viewCount: Int,
    val averageDuration: Long
)