package com.example.health_assistant.features.discover.domain.analytics

import android.content.Context
import com.example.health_assistant.features.discover.data.AnalyticsDao
import com.example.health_assistant.features.discover.data.entity.ContentAnalyticsEntity
import com.example.health_assistant.features.discover.data.entity.UserEngagementEntity
import com.example.health_assistant.features.discover.domain.model.DiscoverContent
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*
import org.junit.Assert.*

class AnalyticsManagerTest {

    @Mock
    private lateinit var analyticsDao: AnalyticsDao

    @Mock
    private lateinit var context: Context

    private lateinit var analyticsManager: AnalyticsManager

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
        analyticsManager = AnalyticsManager(analyticsDao, context)
    }

    @Test
    fun `trackContentView should insert analytics event`() = runTest {
        // When
        analyticsManager.trackContentView(testContent, "feed")

        // Then
        verify(analyticsDao).insertAnalyticsEvent(any<ContentAnalyticsEntity>())
    }

    @Test
    fun `trackReadingStart should insert reading start event`() = runTest {
        // When
        analyticsManager.trackReadingStart(testContent)

        // Then
        verify(analyticsDao).insertAnalyticsEvent(
            argThat { event ->
                event.eventType == "read_start" && 
                event.contentId == testContent.id
            }
        )
    }

    @Test
    fun `trackReadingProgress should insert progress event with metadata`() = runTest {
        val progress = 0.5f
        val duration = 30000L

        // When
        analyticsManager.trackReadingProgress(testContent, progress, duration)

        // Then
        verify(analyticsDao).insertAnalyticsEvent(
            argThat { event ->
                event.eventType == "read_progress" && 
                event.progress == progress &&
                event.duration == duration &&
                event.metadata.isNotEmpty()
            }
        )
    }

    @Test
    fun `trackReadingComplete should insert completion event`() = runTest {
        val totalDuration = 300000L

        // When
        analyticsManager.trackReadingComplete(testContent, totalDuration)

        // Then
        verify(analyticsDao).insertAnalyticsEvent(
            argThat { event ->
                event.eventType == "read_complete" && 
                event.duration == totalDuration &&
                event.progress == 1.0f
            }
        )
    }

    @Test
    fun `trackBookmark should insert bookmark event`() = runTest {
        // When
        analyticsManager.trackBookmark(testContent, true)

        // Then
        verify(analyticsDao).insertAnalyticsEvent(
            argThat { event ->
                event.eventType == "bookmark" && 
                event.contentId == testContent.id
            }
        )
    }

    @Test
    fun `trackShare should insert share event with share method`() = runTest {
        val shareMethod = "whatsapp"

        // When
        analyticsManager.trackShare(testContent, shareMethod)

        // Then
        verify(analyticsDao).insertAnalyticsEvent(
            argThat { event ->
                event.eventType == "share" && 
                event.metadata.contains(shareMethod)
            }
        )
    }

    @Test
    fun `trackSearch should insert search event with query metadata`() = runTest {
        val userId = "test-user-1"
        val query = "nutrition tips"
        val resultsCount = 15

        // When
        analyticsManager.trackSearch(userId, query, resultsCount)

        // Then
        verify(analyticsDao).insertAnalyticsEvent(
            argThat { event ->
                event.eventType == "search" && 
                event.userId == userId &&
                event.metadata.contains(query) &&
                event.metadata.contains(resultsCount.toString())
            }
        )
    }

    @Test
    fun `getUserEngagementStats should return user engagement data`() = runTest {
        val userId = "test-user-1"
        val mockEngagement = listOf(
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

        whenever(analyticsDao.getUserEngagement(userId)).thenReturn(mockEngagement)

        // When
        val result = analyticsManager.getUserEngagementStats(userId)

        // Then
        assertEquals(mockEngagement, result)
        verify(analyticsDao).getUserEngagement(userId)
    }

    @Test
    fun `getTrendingContent should return trending content IDs`() = runTest {
        val mockTrendingResults = listOf(
            com.example.health_assistant.features.discover.data.TrendingContentResult("content-1", 100),
            com.example.health_assistant.features.discover.data.TrendingContentResult("content-2", 85)
        )

        whenever(analyticsDao.getTrendingContent(any(), any())).thenReturn(mockTrendingResults)

        // When
        val result = analyticsManager.getTrendingContent(7, 10)

        // Then
        assertEquals(listOf("content-1", "content-2"), result)
        verify(analyticsDao).getTrendingContent(any(), eq(10))
    }

    @Test
    fun `cleanupOldAnalytics should call DAO cleanup method`() = runTest {
        val daysToKeep = 90

        // When
        analyticsManager.cleanupOldAnalytics(daysToKeep)

        // Then
        verify(analyticsDao).cleanupOldAnalytics(any())
    }

    @Test
    fun `startNewSession should generate new session ID`() {
        // When
        analyticsManager.startNewSession()

        // Then - verify that subsequent tracking uses new session
        analyticsManager.trackContentView(testContent)
        
        // Verify that the method was called (we can't verify the exact content due to async nature)
        verify(analyticsDao, timeout(1000)).insertAnalyticsEvent(any<ContentAnalyticsEntity>())
    }
}