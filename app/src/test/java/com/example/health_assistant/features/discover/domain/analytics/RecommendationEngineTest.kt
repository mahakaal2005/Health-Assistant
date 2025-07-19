package com.example.health_assistant.features.discover.domain.analytics

import com.example.health_assistant.features.discover.data.AnalyticsDao
import com.example.health_assistant.features.discover.data.entity.UserEngagementEntity
import com.example.health_assistant.features.discover.data.entity.ContentRecommendationEntity
import com.example.health_assistant.features.discover.data.TrendingContentResult
import com.example.health_assistant.features.discover.domain.repository.DiscoverRepository
import com.example.health_assistant.features.discover.data.entity.HealthArticleEntity
import com.example.health_assistant.core.util.Result
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*
import org.junit.Assert.*

class RecommendationEngineTest {

    @Mock
    private lateinit var analyticsDao: AnalyticsDao

    @Mock
    private lateinit var discoverRepository: DiscoverRepository

    private lateinit var recommendationEngine: RecommendationEngine

    private val testUserId = "test-user-1"

    private val mockUserEngagement = listOf(
        UserEngagementEntity(
            id = "engagement-1",
            userId = testUserId,
            category = "nutrition",
            contentType = "article",
            totalViews = 15,
            totalReadingTime = 900000L,
            averageReadingTime = 60000L,
            completionRate = 0.8f,
            bookmarkRate = 0.4f,
            shareRate = 0.2f,
            engagementScore = 0.75f,
            preferenceWeight = 0.8f,
            lastEngagement = System.currentTimeMillis()
        ),
        UserEngagementEntity(
            id = "engagement-2",
            userId = testUserId,
            category = "fitness",
            contentType = "video",
            totalViews = 8,
            totalReadingTime = 480000L,
            averageReadingTime = 60000L,
            completionRate = 0.6f,
            bookmarkRate = 0.25f,
            shareRate = 0.1f,
            engagementScore = 0.55f,
            preferenceWeight = 0.6f,
            lastEngagement = System.currentTimeMillis()
        )
    )

    private val mockHealthArticle = HealthArticleEntity(
        id = "article-1",
        title = "Healthy Nutrition Tips",
        summary = "Learn about healthy eating",
        content = "Detailed nutrition content",
        category = "nutrition",
        authorName = "Dr. Smith",
        authorCredentials = "MD, Nutritionist",
        sourceUrl = "https://example.com/article",
        publishedDate = System.currentTimeMillis(),
        lastUpdated = System.currentTimeMillis(),
        readingTimeMinutes = 5,
        imageUrl = "https://example.com/image.jpg",
        tags = "nutrition,health,diet",
        isBookmarked = false,
        readProgress = 0f,
        credibilityScore = 4,
        userId = testUserId
    )

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        recommendationEngine = RecommendationEngine(analyticsDao, discoverRepository)
    }

    @Test
    fun `generateRecommendations should return mixed recommendation types`() = runTest {
        // Given
        val trendingResults = listOf(
            TrendingContentResult("trending-1", 100),
            TrendingContentResult("trending-2", 85)
        )
        
        whenever(analyticsDao.getUserEngagement(testUserId)).thenReturn(mockUserEngagement)
        whenever(analyticsDao.getTopEngagementCategories(testUserId, 5)).thenReturn(mockUserEngagement)
        whenever(analyticsDao.getTrendingContent(any(), any())).thenReturn(trendingResults)
        whenever(analyticsDao.getContentAnalytics(testUserId, "trending-1")).thenReturn(emptyList())
        whenever(analyticsDao.getContentAnalytics(testUserId, "trending-2")).thenReturn(emptyList())
        whenever(discoverRepository.getHealthArticles("nutrition", any())).thenReturn(
            flowOf(Result.Success(listOf(mockHealthArticle)))
        )

        // When
        val recommendations = recommendationEngine.generateRecommendations(testUserId, 10)

        // Then
        assertTrue("Should generate recommendations", recommendations.isNotEmpty())
        assertTrue("Should have trending recommendations", 
            recommendations.any { it.recommendationType == "trending" })
        verify(analyticsDao).getUserEngagement(testUserId)
        verify(analyticsDao).getTopEngagementCategories(testUserId, 5)
    }

    @Test
    fun `generateRecommendations should skip already seen content`() = runTest {
        // Given
        val trendingResults = listOf(TrendingContentResult("seen-content", 100))
        val existingAnalytics = listOf(
            com.example.health_assistant.features.discover.data.entity.ContentAnalyticsEntity(
                id = "analytics-1",
                contentId = "seen-content",
                contentType = "article",
                userId = testUserId,
                sessionId = "session-1",
                eventType = "view",
                timestamp = System.currentTimeMillis()
            )
        )

        whenever(analyticsDao.getUserEngagement(testUserId)).thenReturn(mockUserEngagement)
        whenever(analyticsDao.getTopEngagementCategories(testUserId, 5)).thenReturn(mockUserEngagement)
        whenever(analyticsDao.getTrendingContent(any(), any())).thenReturn(trendingResults)
        whenever(analyticsDao.getContentAnalytics(testUserId, "seen-content")).thenReturn(existingAnalytics)

        // When
        val recommendations = recommendationEngine.generateRecommendations(testUserId, 10)

        // Then
        assertTrue("Should not recommend already seen content",
            recommendations.none { it.contentId == "seen-content" })
    }

    @Test
    fun `getActiveRecommendations should return non-expired recommendations`() = runTest {
        // Given
        val activeRecommendations = listOf(
            ContentRecommendationEntity(
                id = "rec-1",
                userId = testUserId,
                contentId = "content-1",
                contentType = "article",
                recommendationType = "personalized",
                score = 0.8f,
                reason = "Based on your interests",
                algorithmVersion = "1.0",
                category = "nutrition",
                expiresAt = System.currentTimeMillis() + 86400000L // 1 day from now
            )
        )

        whenever(analyticsDao.getActiveRecommendations(eq(testUserId), any(), eq(10)))
            .thenReturn(activeRecommendations)

        // When
        val result = recommendationEngine.getActiveRecommendations(testUserId, limit = 10)

        // Then
        assertEquals(activeRecommendations, result)
        verify(analyticsDao).getActiveRecommendations(eq(testUserId), any(), eq(10))
    }

    @Test
    fun `trackRecommendationPerformance should update recommendation metrics`() = runTest {
        val recommendationId = "rec-1"

        // When
        recommendationEngine.trackRecommendationPerformance(recommendationId, "clicked")

        // Then
        verify(analyticsDao).markRecommendationClicked(recommendationId)
    }

    @Test
    fun `trackRecommendationPerformance should handle different actions`() = runTest {
        val recommendationId = "rec-1"

        // When - Test different actions
        recommendationEngine.trackRecommendationPerformance(recommendationId, "shown")
        recommendationEngine.trackRecommendationPerformance(recommendationId, "bookmarked")

        // Then
        verify(analyticsDao).markRecommendationShown(recommendationId)
        verify(analyticsDao).markRecommendationBookmarked(recommendationId)
    }

    @Test
    fun `cleanupExpiredRecommendations should call DAO cleanup`() = runTest {
        // When
        recommendationEngine.cleanupExpiredRecommendations()

        // Then
        verify(analyticsDao).cleanupExpiredRecommendations(any())
    }

    @Test
    fun `generateRecommendations should handle empty user engagement gracefully`() = runTest {
        // Given
        whenever(analyticsDao.getUserEngagement(testUserId)).thenReturn(emptyList())
        whenever(analyticsDao.getTopEngagementCategories(testUserId, 5)).thenReturn(emptyList())
        whenever(analyticsDao.getTrendingContent(any(), any())).thenReturn(emptyList())

        // When
        val recommendations = recommendationEngine.generateRecommendations(testUserId, 10)

        // Then
        assertTrue("Should handle empty engagement gracefully", recommendations.isEmpty())
    }

    @Test
    fun `generateRecommendations should prioritize high engagement categories`() = runTest {
        // Given
        val highEngagementCategory = mockUserEngagement.maxByOrNull { it.engagementScore }!!
        
        whenever(analyticsDao.getUserEngagement(testUserId)).thenReturn(mockUserEngagement)
        whenever(analyticsDao.getTopEngagementCategories(testUserId, 5)).thenReturn(listOf(highEngagementCategory))
        whenever(analyticsDao.getTrendingContent(any(), any())).thenReturn(emptyList())
        whenever(discoverRepository.getHealthArticles(highEngagementCategory.category, any())).thenReturn(
            flowOf(Result.Success(listOf(mockHealthArticle)))
        )
        whenever(analyticsDao.getContentAnalytics(testUserId, mockHealthArticle.id)).thenReturn(emptyList())

        // When
        val recommendations = recommendationEngine.generateRecommendations(testUserId, 10)

        // Then
        assertTrue("Should generate recommendations for high engagement category",
            recommendations.any { it.category == highEngagementCategory.category })
    }
}