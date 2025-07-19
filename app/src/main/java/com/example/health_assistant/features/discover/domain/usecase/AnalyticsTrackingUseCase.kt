package com.example.health_assistant.features.discover.domain.usecase

import javax.inject.Inject
import javax.inject.Singleton
import com.example.health_assistant.features.discover.domain.analytics.AnalyticsManager
import com.example.health_assistant.features.discover.domain.analytics.RecommendationEngine
import com.example.health_assistant.features.discover.domain.analytics.ABTestManager
import com.example.health_assistant.features.discover.domain.model.DiscoverContent
import com.example.health_assistant.features.discover.data.entity.ContentRecommendationEntity
import com.example.health_assistant.features.discover.data.entity.UserEngagementEntity
import kotlinx.coroutines.flow.Flow

/**
 * Use case for handling all analytics and engagement tracking operations
 * Provides a clean interface for the presentation layer to interact with analytics
 */
@Singleton
class AnalyticsTrackingUseCase @Inject constructor(
    private val analyticsManager: AnalyticsManager,
    private val recommendationEngine: RecommendationEngine,
    private val abTestManager: ABTestManager
) {
    
    // Content interaction tracking
    suspend fun trackContentView(content: DiscoverContent, source: String = "feed") {
        analyticsManager.trackContentView(content, source)
    }
    
    suspend fun trackReadingStart(content: DiscoverContent) {
        analyticsManager.trackReadingStart(content)
    }
    
    suspend fun trackReadingProgress(content: DiscoverContent, progress: Float, duration: Long) {
        analyticsManager.trackReadingProgress(content, progress, duration)
    }
    
    suspend fun trackReadingComplete(content: DiscoverContent, totalDuration: Long) {
        analyticsManager.trackReadingComplete(content, totalDuration)
    }
    
    suspend fun trackBookmark(content: DiscoverContent, isBookmarked: Boolean) {
        analyticsManager.trackBookmark(content, isBookmarked)
    }
    
    suspend fun trackShare(content: DiscoverContent, shareMethod: String) {
        analyticsManager.trackShare(content, shareMethod)
    }
    
    suspend fun trackSearch(userId: String, query: String, resultsCount: Int) {
        analyticsManager.trackSearch(userId, query, resultsCount)
    }
    
    // User engagement analytics
    suspend fun getUserEngagementStats(userId: String): List<UserEngagementEntity> {
        return analyticsManager.getUserEngagementStats(userId)
    }
    
    suspend fun getTrendingContent(days: Int = 7, limit: Int = 10): List<String> {
        return analyticsManager.getTrendingContent(days, limit)
    }
    
    suspend fun getCategoryEngagementStats(userId: String, days: Int = 30): Flow<List<com.example.health_assistant.features.discover.domain.analytics.CategoryEngagementStats>> {
        return analyticsManager.getCategoryEngagementStats(userId, days)
    }
    
    // Content recommendations
    suspend fun generateRecommendations(userId: String, limit: Int = 20): List<ContentRecommendationEntity> {
        return recommendationEngine.generateRecommendations(userId, limit)
    }
    
    suspend fun getActiveRecommendations(userId: String, type: String? = null, limit: Int = 10): List<ContentRecommendationEntity> {
        return recommendationEngine.getActiveRecommendations(userId, type, limit)
    }
    
    suspend fun trackRecommendationPerformance(recommendationId: String, action: String) {
        recommendationEngine.trackRecommendationPerformance(recommendationId, action)
    }
    
    // A/B Testing
    suspend fun getTestVariant(userId: String, testName: String): String? {
        return abTestManager.getTestVariant(userId, testName)
    }
    
    suspend fun recordABTestImpression(userId: String, testName: String) {
        abTestManager.recordImpression(userId, testName)
    }
    
    suspend fun recordABTestClick(userId: String, testName: String) {
        abTestManager.recordClick(userId, testName)
    }
    
    suspend fun recordABTestConversion(userId: String, testName: String, eventType: String) {
        abTestManager.recordConversion(userId, testName, eventType)
    }
    
    suspend fun recordABTestEngagementTime(userId: String, testName: String, duration: Long) {
        abTestManager.recordEngagementTime(userId, testName, duration)
    }
    
    // Convenience methods for A/B test variants
    suspend fun getContentLayoutVariant(userId: String): String {
        return abTestManager.getContentLayoutVariant(userId)
    }
    
    suspend fun getRecommendationAlgorithmVariant(userId: String): String {
        return abTestManager.getRecommendationAlgorithmVariant(userId)
    }
    
    suspend fun getReadingProgressVariant(userId: String): String {
        return abTestManager.getReadingProgressVariant(userId)
    }
    
    // Session management
    fun startNewAnalyticsSession() {
        analyticsManager.startNewSession()
    }
    
    // Cleanup operations
    suspend fun performAnalyticsCleanup() {
        analyticsManager.cleanupOldAnalytics()
        recommendationEngine.cleanupExpiredRecommendations()
        abTestManager.deactivateExpiredTests()
    }
}