package com.example.health_assistant.features.discover.domain.usecase

import com.example.health_assistant.features.discover.domain.analytics.AnalyticsManager
import com.example.health_assistant.features.discover.domain.analytics.RecommendationEngine
import com.example.health_assistant.features.discover.domain.analytics.ABTestManager
import com.example.health_assistant.features.discover.domain.model.DiscoverContent
import com.example.health_assistant.features.discover.data.entity.ContentRecommendationEntity
import com.example.health_assistant.features.discover.data.entity.UserEngagementEntity
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*
import org.junit.Assert.*

class AnalyticsTrackingUseCaseTest {

    @Mock
    private lateinit var analyticsManager: AnalyticsManager

    @Mock
    private lateinit var recommendationEngine: RecommendationEngine

    @Mock
    private lateinit var abTestManager: ABTestManager

    private lateinit var analyticsTrackingUseCase: AnalyticsTrackingUseCase

    private val testContent = DiscoverContent.Article(
        id = "test-article-1",
        title = "Test Health Article",
        publishedDate = System.currentTimeMillis(),
        category = "nutrition",
        imageUrl = "https://example.com/image.jpg",
        userId = "test-user-1",
        summary = "Test article summary",
        content = "Test article content",
        authorName = "Dr. Test",
        authorCredentials = "MD, PhD",
        sourceUrl = "https://example.com/article",
        lastUpdated = System.currentTimeMillis(),
        readingTimeMinutes = 5,
        tags = listOf("nutrition", "health", "diet"),
        isBookmarked = false,
        readProgress = 0f,
        credibilityScore = 4
    )

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        analyticsTrackingUseCase = AnalyticsTrackingUseCase(
            analyticsManager,
            recommendationEngine,
            abTestManager
        )
    }

    @Test
    fun `trackContentView should delegate to analytics manager`() = runTest {
        val source = "feed"

        // When
        analyticsTrackingUseCase.trackContentView(testContent, source)

        // Then
        verify(analyticsManager).trackContentView(testContent, source)
    }

    @Test
    fun `trackReadingStart should delegate to analytics manager`() = runTest {
        // When
        analyticsTrackingUseCase.trackReadingStart(testContent)

        // Then
        verify(analyticsManager).trackReadingStart(testContent)
    }

    @Test
    fun `trackReadingProgress should delegate to analytics manager`() = runTest {
        val progress = 0.5f
        val duration = 30000L

        // When
        analyticsTrackingUseCase.trackReadingProgress(testContent, progress, duration)

        // Then
        verify(analyticsManager).trackReadingProgress(testContent, progress, duration)
    }

    @Test
    fun `trackReadingComplete should delegate to analytics manager`() = runTest {
        val totalDuration = 300000L

        // When
        analyticsTrackingUseCase.trackReadingComplete(testContent, totalDuration)

        // Then
        verify(analyticsManager).trackReadingComplete(testContent, totalDuration)
    }

    @Test
    fun `trackBookmark should delegate to analytics manager`() = runTest {
        val isBookmarked = true

        // When
        analyticsTrackingUseCase.trackBookmark(testContent, isBookmarked)

        // Then
        verify(analyticsManager).trackBookmark(testContent, isBookmarked)
    }

    @Test
    fun `trackShare should delegate to analytics manager`() = runTest {
        val shareMethod = "whatsapp"

        // When
        analyticsTrackingUseCase.trackShare(testContent, shareMethod)

        // Then
        verify(analyticsManager).trackShare(testContent, shareMethod)
    }

    @Test
    fun `trackSearch should delegate to analytics manager`() = runTest {
        val userId = "test-user-1"
        val query = "nutrition tips"
        val resultsCount = 15

        // When
        analyticsTrackingUseCase.trackSearch(userId, query, resultsCount)

        // Then
        verify(analyticsManager).trackSearch(userId, query, resultsCount)
    }

    @Test
    fun `getUserEngagementStats should delegate to analytics manager`() = runTest {
        val userId = "test-user-1"
        val mockStats = listOf(
            UserEngagementEntity(
                id = "engagement-1",
                userId = userId,
                category = "nutrition",
                contentType = "article",
                totalViews = 10,
                totalReadingTime = 600000L,
                averageReadingTime = 60000L,
                completionRate = 0.8f,
                bookmarkRate = 0.3f,
                shareRate = 0.1f,
                engagementScore = 0.7f,
                preferenceWeight = 0.8f,
                lastEngagement = System.currentTimeMillis()
            )
        )

        whenever(analyticsManager.getUserEngagementStats(userId)).thenReturn(mockStats)

        // When
        val result = analyticsTrackingUseCase.getUserEngagementStats(userId)

        // Then
        assertEquals(mockStats, result)
        verify(analyticsManager).getUserEngagementStats(userId)
    }

    @Test
    fun `getTrendingContent should delegate to analytics manager`() = runTest {
        val days = 7
        val limit = 10
        val mockTrending = listOf("content-1", "content-2", "content-3")

        whenever(analyticsManager.getTrendingContent(days, limit)).thenReturn(mockTrending)

        // When
        val result = analyticsTrackingUseCase.getTrendingContent(days, limit)

        // Then
        assertEquals(mockTrending, result)
        verify(analyticsManager).getTrendingContent(days, limit)
    }

    @Test
    fun `generateRecommendations should delegate to recommendation engine`() = runTest {
        val userId = "test-user-1"
        val limit = 20
        val mockRecommendations = listOf(
            ContentRecommendationEntity(
                id = "rec-1",
                userId = userId,
                contentId = "content-1",
                contentType = "article",
                recommendationType = "personalized",
                score = 0.8f,
                reason = "Based on your interests",
                algorithmVersion = "1.0",
                category = "nutrition"
            )
        )

        whenever(recommendationEngine.generateRecommendations(userId, limit)).thenReturn(mockRecommendations)

        // When
        val result = analyticsTrackingUseCase.generateRecommendations(userId, limit)

        // Then
        assertEquals(mockRecommendations, result)
        verify(recommendationEngine).generateRecommendations(userId, limit)
    }

    @Test
    fun `getActiveRecommendations should delegate to recommendation engine`() = runTest {
        val userId = "test-user-1"
        val type = "personalized"
        val limit = 10
        val mockRecommendations = listOf(
            ContentRecommendationEntity(
                id = "rec-1",
                userId = userId,
                contentId = "content-1",
                contentType = "article",
                recommendationType = type,
                score = 0.8f,
                reason = "Based on your interests",
                algorithmVersion = "1.0",
                category = "nutrition"
            )
        )

        whenever(recommendationEngine.getActiveRecommendations(userId, type, limit)).thenReturn(mockRecommendations)

        // When
        val result = analyticsTrackingUseCase.getActiveRecommendations(userId, type, limit)

        // Then
        assertEquals(mockRecommendations, result)
        verify(recommendationEngine).getActiveRecommendations(userId, type, limit)
    }

    @Test
    fun `trackRecommendationPerformance should delegate to recommendation engine`() = runTest {
        val recommendationId = "rec-1"
        val action = "clicked"

        // When
        analyticsTrackingUseCase.trackRecommendationPerformance(recommendationId, action)

        // Then
        verify(recommendationEngine).trackRecommendationPerformance(recommendationId, action)
    }

    @Test
    fun `getTestVariant should delegate to AB test manager`() = runTest {
        val userId = "test-user-1"
        val testName = "content_layout_v1"
        val expectedVariant = "card_layout"

        whenever(abTestManager.getTestVariant(userId, testName)).thenReturn(expectedVariant)

        // When
        val result = analyticsTrackingUseCase.getTestVariant(userId, testName)

        // Then
        assertEquals(expectedVariant, result)
        verify(abTestManager).getTestVariant(userId, testName)
    }

    @Test
    fun `recordABTestImpression should delegate to AB test manager`() = runTest {
        val userId = "test-user-1"
        val testName = "content_layout_v1"

        // When
        analyticsTrackingUseCase.recordABTestImpression(userId, testName)

        // Then
        verify(abTestManager).recordImpression(userId, testName)
    }

    @Test
    fun `recordABTestClick should delegate to AB test manager`() = runTest {
        val userId = "test-user-1"
        val testName = "content_layout_v1"

        // When
        analyticsTrackingUseCase.recordABTestClick(userId, testName)

        // Then
        verify(abTestManager).recordClick(userId, testName)
    }

    @Test
    fun `recordABTestConversion should delegate to AB test manager`() = runTest {
        val userId = "test-user-1"
        val testName = "content_layout_v1"
        val eventType = "bookmark"

        // When
        analyticsTrackingUseCase.recordABTestConversion(userId, testName, eventType)

        // Then
        verify(abTestManager).recordConversion(userId, testName, eventType)
    }

    @Test
    fun `recordABTestEngagementTime should delegate to AB test manager`() = runTest {
        val userId = "test-user-1"
        val testName = "reading_progress_indicator"
        val duration = 30000L

        // When
        analyticsTrackingUseCase.recordABTestEngagementTime(userId, testName, duration)

        // Then
        verify(abTestManager).recordEngagementTime(userId, testName, duration)
    }

    @Test
    fun `getContentLayoutVariant should delegate to AB test manager`() = runTest {
        val userId = "test-user-1"
        val expectedVariant = "card_layout"

        whenever(abTestManager.getContentLayoutVariant(userId)).thenReturn(expectedVariant)

        // When
        val result = analyticsTrackingUseCase.getContentLayoutVariant(userId)

        // Then
        assertEquals(expectedVariant, result)
        verify(abTestManager).getContentLayoutVariant(userId)
    }

    @Test
    fun `getRecommendationAlgorithmVariant should delegate to AB test manager`() = runTest {
        val userId = "test-user-1"
        val expectedVariant = "hybrid"

        whenever(abTestManager.getRecommendationAlgorithmVariant(userId)).thenReturn(expectedVariant)

        // When
        val result = analyticsTrackingUseCase.getRecommendationAlgorithmVariant(userId)

        // Then
        assertEquals(expectedVariant, result)
        verify(abTestManager).getRecommendationAlgorithmVariant(userId)
    }

    @Test
    fun `getReadingProgressVariant should delegate to AB test manager`() = runTest {
        val userId = "test-user-1"
        val expectedVariant = "progress_bar"

        whenever(abTestManager.getReadingProgressVariant(userId)).thenReturn(expectedVariant)

        // When
        val result = analyticsTrackingUseCase.getReadingProgressVariant(userId)

        // Then
        assertEquals(expectedVariant, result)
        verify(abTestManager).getReadingProgressVariant(userId)
    }

    @Test
    fun `startNewAnalyticsSession should delegate to analytics manager`() {
        // When
        analyticsTrackingUseCase.startNewAnalyticsSession()

        // Then
        verify(analyticsManager).startNewSession()
    }

    @Test
    fun `performAnalyticsCleanup should delegate to all managers`() = runTest {
        // When
        analyticsTrackingUseCase.performAnalyticsCleanup()

        // Then
        verify(analyticsManager).cleanupOldAnalytics()
        verify(recommendationEngine).cleanupExpiredRecommendations()
        verify(abTestManager).deactivateExpiredTests()
    }
}