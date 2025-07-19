package com.example.health_assistant.features.discover.domain.analytics

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import com.example.health_assistant.features.discover.data.AnalyticsDao
import com.example.health_assistant.features.discover.data.entity.ContentRecommendationEntity
import com.example.health_assistant.features.discover.domain.model.DiscoverContent
import com.example.health_assistant.features.discover.domain.repository.DiscoverRepository
import com.example.health_assistant.core.util.Result
import kotlinx.coroutines.flow.first
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.sqrt

/**
 * Content recommendation engine based on user engagement patterns
 * Implements collaborative filtering and content-based recommendation algorithms
 */
@Singleton
class RecommendationEngine @Inject constructor(
    private val analyticsDao: AnalyticsDao,
    private val discoverRepository: DiscoverRepository
) {
    
    companion object {
        private const val ALGORITHM_VERSION = "1.0"
        private const val TRENDING_WEIGHT = 0.3f
        private const val PERSONALIZED_WEIGHT = 0.4f
        private const val CATEGORY_WEIGHT = 0.2f
        private const val SIMILARITY_WEIGHT = 0.1f
        private const val RECOMMENDATION_EXPIRY_DAYS = 7L
    }
    
    /**
     * Generate personalized content recommendations for a user
     */
    suspend fun generateRecommendations(
        userId: String,
        limit: Int = 20
    ): List<ContentRecommendationEntity> = withContext(Dispatchers.IO) {
        val recommendations = mutableListOf<ContentRecommendationEntity>()
        
        // Get user engagement patterns
        val userEngagement = analyticsDao.getUserEngagement(userId)
        val topCategories = analyticsDao.getTopEngagementCategories(userId, 5)
        
        // Generate different types of recommendations
        recommendations.addAll(generateTrendingRecommendations(userId, limit / 4))
        recommendations.addAll(generatePersonalizedRecommendations(userId, userEngagement, limit / 2))
        recommendations.addAll(generateCategoryBasedRecommendations(userId, topCategories, limit / 4))
        
        // Sort by score and return top recommendations
        recommendations.sortedByDescending { it.score }.take(limit)
    }
    
    /**
     * Generate trending content recommendations
     */
    private suspend fun generateTrendingRecommendations(
        userId: String,
        limit: Int
    ): List<ContentRecommendationEntity> {
        val trendingContentIds = analyticsDao.getTrendingContent(
            startTime = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L), // Last 7 days
            limit = limit * 2 // Get more to filter out already seen content
        )
        
        val recommendations = mutableListOf<ContentRecommendationEntity>()
        
        for ((index, trendingResult) in trendingContentIds.withIndex()) {
            // Check if user has already seen this content
            val userAnalytics = analyticsDao.getContentAnalytics(userId, trendingResult.contentId)
            if (userAnalytics.isNotEmpty()) continue // Skip already seen content
            
            val score = calculateTrendingScore(trendingResult.viewCount, index)
            
            recommendations.add(
                ContentRecommendationEntity(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    contentId = trendingResult.contentId,
                    contentType = "unknown", // Will be updated when content is fetched
                    recommendationType = "trending",
                    score = score,
                    reason = "Popular content trending this week",
                    algorithmVersion = ALGORITHM_VERSION,
                    category = "",
                    expiresAt = System.currentTimeMillis() + (RECOMMENDATION_EXPIRY_DAYS * 24 * 60 * 60 * 1000L)
                )
            )
            
            if (recommendations.size >= limit) break
        }
        
        return recommendations
    }
    
    /**
     * Generate personalized recommendations based on user engagement patterns
     */
    private suspend fun generatePersonalizedRecommendations(
        userId: String,
        userEngagement: List<com.example.health_assistant.features.discover.data.entity.UserEngagementEntity>,
        limit: Int
    ): List<ContentRecommendationEntity> {
        val recommendations = mutableListOf<ContentRecommendationEntity>()
        
        // Get content for user's preferred categories
        for (engagement in userEngagement.sortedByDescending { it.engagementScore }.take(3)) {
            val contentResult = when (engagement.contentType) {
                "article" -> discoverRepository.getHealthArticles(category = engagement.category, limit = limit / 3)
                "video" -> discoverRepository.getEducationalVideos(category = engagement.category, limit = limit / 3)
                "news" -> discoverRepository.getHealthNews(category = engagement.category, limit = limit / 3)
                else -> continue
            }
            
            when (val result = contentResult.first()) {
                is Result.Success -> {
                    for (content in result.data.take(limit / 3)) {
                        // Skip if user has already seen this content
                        val userAnalytics = analyticsDao.getContentAnalytics(userId, content.id)
                        if (userAnalytics.isNotEmpty()) continue
                        
                        val score = calculatePersonalizedScore(engagement, content)
                        
                        recommendations.add(
                            ContentRecommendationEntity(
                                id = UUID.randomUUID().toString(),
                                userId = userId,
                                contentId = content.id,
                                contentType = content.getContentType(),
                                recommendationType = "personalized",
                                score = score,
                                reason = "Based on your interest in ${engagement.category}",
                                algorithmVersion = ALGORITHM_VERSION,
                                category = content.category,
                                tags = getContentTags(content),
                                expiresAt = System.currentTimeMillis() + (RECOMMENDATION_EXPIRY_DAYS * 24 * 60 * 60 * 1000L)
                            )
                        )
                    }
                }
                is Result.Error -> continue
                is Result.Loading -> continue
            }
        }
        
        return recommendations.take(limit)
    }
    
    /**
     * Generate category-based recommendations
     */
    private suspend fun generateCategoryBasedRecommendations(
        userId: String,
        topCategories: List<com.example.health_assistant.features.discover.data.entity.UserEngagementEntity>,
        limit: Int
    ): List<ContentRecommendationEntity> {
        val recommendations = mutableListOf<ContentRecommendationEntity>()
        
        for (categoryEngagement in topCategories.take(2)) {
            // Get mixed content for this category
            // Get mixed content for this category - simplified approach
            val contentResult = discoverRepository.getHealthArticles(categoryEngagement.category, limit / 2).first()
            
            val allContent = mutableListOf<DiscoverContent>()
            when (contentResult) {
                is Result.Success -> {
                    allContent.addAll(contentResult.data.map { article ->
                        DiscoverContent.Article(
                            id = article.id,
                            title = article.title,
                            publishedDate = article.publishedDate,
                            category = article.category,
                            imageUrl = article.imageUrl,
                            userId = article.userId,
                            summary = article.summary,
                            content = article.content,
                            authorName = article.authorName,
                            authorCredentials = article.authorCredentials,
                            sourceUrl = article.sourceUrl,
                            lastUpdated = article.lastUpdated,
                            readingTimeMinutes = article.readingTimeMinutes,
                            tags = article.tags,
                            isBookmarked = article.isBookmarked,
                            readProgress = article.readProgress,
                            credibilityScore = article.credibilityScore
                        )
                    })
                }
                is Result.Error -> continue
                is Result.Loading -> continue
            }
            
            for (content in allContent.take(limit / 2)) {
                // Skip if user has already seen this content
                val userAnalytics = analyticsDao.getContentAnalytics(userId, content.id)
                if (userAnalytics.isNotEmpty()) continue
                
                val score = calculateCategoryScore(categoryEngagement, content)
                
                recommendations.add(
                    ContentRecommendationEntity(
                        id = UUID.randomUUID().toString(),
                        userId = userId,
                        contentId = content.id,
                        contentType = content.getContentType(),
                        recommendationType = "category_based",
                        score = score,
                        reason = "More ${categoryEngagement.category} content for you",
                        algorithmVersion = ALGORITHM_VERSION,
                        category = content.category,
                        tags = getContentTags(content),
                        expiresAt = System.currentTimeMillis() + (RECOMMENDATION_EXPIRY_DAYS * 24 * 60 * 60 * 1000L)
                    )
                )
            }
        }
        
        return recommendations.take(limit)
    }
    
    /**
     * Calculate trending score based on view count and recency
     */
    private fun calculateTrendingScore(viewCount: Int, position: Int): Float {
        val popularityScore = ln(viewCount.toFloat() + 1) / ln(100f) // Normalize to 0-1 range
        val positionPenalty = exp(-position * 0.1f) // Exponential decay for position
        return (popularityScore * positionPenalty * TRENDING_WEIGHT).coerceIn(0f, 1f)
    }
    
    /**
     * Calculate personalized score based on user engagement patterns
     */
    private fun calculatePersonalizedScore(
        engagement: com.example.health_assistant.features.discover.data.entity.UserEngagementEntity,
        content: DiscoverContent
    ): Float {
        val categoryMatch = if (engagement.category == content.category) 1.0f else 0.5f
        val typeMatch = if (engagement.contentType == content.getContentType()) 1.0f else 0.7f
        val engagementBonus = engagement.engagementScore
        val preferenceBonus = engagement.preferenceWeight
        
        return ((categoryMatch * typeMatch * engagementBonus * preferenceBonus) * PERSONALIZED_WEIGHT)
            .coerceIn(0f, 1f)
    }
    
    /**
     * Calculate category-based score
     */
    private fun calculateCategoryScore(
        categoryEngagement: com.example.health_assistant.features.discover.data.entity.UserEngagementEntity,
        content: DiscoverContent
    ): Float {
        val categoryMatch = if (categoryEngagement.category == content.category) 1.0f else 0.3f
        val engagementScore = categoryEngagement.engagementScore
        val recencyBonus = calculateRecencyBonus(content.publishedDate)
        
        return ((categoryMatch * engagementScore * recencyBonus) * CATEGORY_WEIGHT)
            .coerceIn(0f, 1f)
    }
    
    /**
     * Calculate recency bonus for content freshness
     */
    private fun calculateRecencyBonus(publishedDate: Long): Float {
        val daysSincePublished = (System.currentTimeMillis() - publishedDate) / (24 * 60 * 60 * 1000L)
        return when {
            daysSincePublished <= 1 -> 1.0f
            daysSincePublished <= 7 -> 0.8f
            daysSincePublished <= 30 -> 0.6f
            daysSincePublished <= 90 -> 0.4f
            else -> 0.2f
        }
    }
    
    /**
     * Extract tags from content for recommendation metadata
     */
    private fun getContentTags(content: DiscoverContent): String {
        return when (content) {
            is DiscoverContent.Article -> content.tags.joinToString(",")
            is DiscoverContent.Video -> content.category
            is DiscoverContent.News -> content.category
        }
    }
    
    /**
     * Update recommendation performance metrics
     */
    suspend fun trackRecommendationPerformance(
        recommendationId: String,
        action: String // "shown", "clicked", "bookmarked"
    ) = withContext(Dispatchers.IO) {
        when (action) {
            "shown" -> analyticsDao.markRecommendationShown(recommendationId)
            "clicked" -> analyticsDao.markRecommendationClicked(recommendationId)
            "bookmarked" -> analyticsDao.markRecommendationBookmarked(recommendationId)
        }
    }
    
    /**
     * Get active recommendations for a user
     */
    suspend fun getActiveRecommendations(
        userId: String,
        type: String? = null,
        limit: Int = 10
    ): List<ContentRecommendationEntity> = withContext(Dispatchers.IO) {
        val currentTime = System.currentTimeMillis()
        
        return@withContext if (type != null) {
            analyticsDao.getRecommendationsByType(userId, type, currentTime, limit)
        } else {
            analyticsDao.getActiveRecommendations(userId, currentTime, limit)
        }
    }
    
    /**
     * Cleanup expired recommendations
     */
    suspend fun cleanupExpiredRecommendations() = withContext(Dispatchers.IO) {
        analyticsDao.cleanupExpiredRecommendations(System.currentTimeMillis())
    }
}

// Extension functions to convert entities to DiscoverContent
private fun com.example.health_assistant.features.discover.data.entity.HealthArticleEntity.toDiscoverContent(): DiscoverContent.Article {
    return DiscoverContent.Article(
        id = id,
        title = title,
        publishedDate = publishedDate,
        category = category,
        imageUrl = imageUrl,
        userId = userId,
        summary = summary,
        content = content,
        authorName = authorName,
        authorCredentials = authorCredentials,
        sourceUrl = sourceUrl,
        lastUpdated = lastUpdated,
        readingTimeMinutes = readingTimeMinutes,
        tags = tags,
        isBookmarked = isBookmarked,
        readProgress = readProgress,
        credibilityScore = credibilityScore
    )
}

private fun com.example.health_assistant.features.discover.data.entity.EducationalVideoEntity.toDiscoverContent(): DiscoverContent.Video {
    return DiscoverContent.Video(
        id = id,
        title = title,
        publishedDate = publishedDate,
        category = category,
        imageUrl = thumbnailUrl,
        userId = userId,
        description = description,
        thumbnailUrl = thumbnailUrl,
        videoUrl = videoUrl,
        durationSeconds = durationSeconds,
        difficultyLevel = difficultyLevel,
        expertName = expertName,
        expertCredentials = expertCredentials,
        watchProgress = watchProgress,
        isDownloadedOffline = isDownloadedOffline,
        transcriptAvailable = transcriptAvailable
    )
}