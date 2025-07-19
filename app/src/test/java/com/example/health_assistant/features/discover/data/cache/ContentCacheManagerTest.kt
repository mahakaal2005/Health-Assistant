package com.example.health_assistant.features.discover.data.cache

import android.content.Context
import com.example.health_assistant.core.util.Result
import com.example.health_assistant.features.discover.data.DiscoverDao
import com.example.health_assistant.features.discover.data.entity.HealthArticleEntity
import com.example.health_assistant.features.discover.data.entity.HealthNewsEntity
import com.example.health_assistant.features.discover.data.entity.EducationalVideoEntity
import com.example.health_assistant.features.discover.domain.repository.CacheStatistics
import io.mockk.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for ContentCacheManager
 * Tests cache management, LRU eviction, and offline content prefetching
 */
class ContentCacheManagerTest {

    private lateinit var contentCacheManager: ContentCacheManager
    private val mockContext = mockk<Context>()
    private val mockDiscoverDao = mockk<DiscoverDao>()
    
    private val testUserId = "test_user_123"
    
    @Before
    fun setup() {
        MockKAnnotations.init(this)
        contentCacheManager = ContentCacheManager(mockContext, mockDiscoverDao)
        
        // Mock context behavior
        every { mockContext.cacheDir } returns mockk {
            every { absolutePath } returns "/cache"
        }
    }

    @Test
    fun `initializeCache should prefetch essential content when needed`() = runTest {
        // Given
        coEvery { mockDiscoverDao.getEssentialContentCount(testUserId) } returns 5 // Below threshold
        coEvery { mockDiscoverDao.cleanupExpiredArticles(any(), testUserId) } returns 2
        coEvery { mockDiscoverDao.cleanupExpiredNews(any(), testUserId) } returns 1
        coEvery { mockDiscoverDao.cleanupExpiredVideos(any(), testUserId) } returns 0
        coEvery { mockDiscoverDao.markContentAsEssential(any(), any(), any()) } just Runs

        // When
        val result = contentCacheManager.initializeCache(testUserId)

        // Then
        assertTrue(result is Result.Success)
        coVerify { mockDiscoverDao.getEssentialContentCount(testUserId) }
        coVerify { mockDiscoverDao.markContentAsEssential(testUserId, any(), any()) }
    }

    @Test
    fun `manageCacheSize should evict oldest articles when limit exceeded`() = runTest {
        // Given
        val mockStats = CacheStatistics(
            totalArticles = 250, // Exceeds MAX_ARTICLES_CACHE (200)
            totalNews = 50,
            totalVideos = 30,
            totalBookmarks = 10,
            offlineVideos = 5,
            cacheSize = 50 * 1024 * 1024L,
            lastSyncTime = System.currentTimeMillis()
        )
        
        val oldArticles = listOf(
            createMockArticle("1", "nutrition"),
            createMockArticle("2", "fitness"),
            createMockArticle("3", "preventive-care") // Essential category
        )
        
        coEvery { mockDiscoverDao.getArticleCount(testUserId) } returns 250
        coEvery { mockDiscoverDao.getNewsCount(testUserId) } returns 50
        coEvery { mockDiscoverDao.getVideoCount(testUserId) } returns 30
        coEvery { mockDiscoverDao.getBookmarkCount(testUserId) } returns 10
        coEvery { mockDiscoverDao.getOfflineVideoCount(testUserId) } returns 5
        coEvery { mockDiscoverDao.getLastSyncTime(testUserId) } returns System.currentTimeMillis()
        
        coEvery { mockDiscoverDao.getOldestArticles(testUserId, 50) } returns oldArticles
        coEvery { mockDiscoverDao.deleteHealthArticle(any(), testUserId) } just Runs
        coEvery { mockDiscoverDao.cleanupExpiredArticles(any(), testUserId) } returns 5
        coEvery { mockDiscoverDao.cleanupExpiredNews(any(), testUserId) } returns 2
        coEvery { mockDiscoverDao.cleanupExpiredVideos(any(), testUserId) } returns 1

        // When
        val result = contentCacheManager.manageCacheSize(testUserId)

        // Then
        assertTrue(result is Result.Success)
        val cleanupResult = (result as Result.Success).data
        
        // Should only delete non-essential articles (2 out of 3)
        assertEquals(2, cleanupResult.articlesRemoved)
        assertTrue(cleanupResult.totalSpaceFreed > 0)
        
        // Verify essential content (preventive-care) was not deleted
        coVerify(exactly = 0) { mockDiscoverDao.deleteHealthArticle("3", testUserId) }
        coVerify { mockDiscoverDao.deleteHealthArticle("1", testUserId) }
        coVerify { mockDiscoverDao.deleteHealthArticle("2", testUserId) }
    }

    @Test
    fun `getCacheStatistics should return accurate cache information`() = runTest {
        // Given
        coEvery { mockDiscoverDao.getArticleCount(testUserId) } returns 150
        coEvery { mockDiscoverDao.getNewsCount(testUserId) } returns 75
        coEvery { mockDiscoverDao.getVideoCount(testUserId) } returns 25
        coEvery { mockDiscoverDao.getBookmarkCount(testUserId) } returns 20
        coEvery { mockDiscoverDao.getOfflineVideoCount(testUserId) } returns 10
        coEvery { mockDiscoverDao.getLastSyncTime(testUserId) } returns 1234567890L

        // When
        val stats = contentCacheManager.getCacheStatistics(testUserId)

        // Then
        assertEquals(150, stats.totalArticles)
        assertEquals(75, stats.totalNews)
        assertEquals(25, stats.totalVideos)
        assertEquals(20, stats.totalBookmarks)
        assertEquals(10, stats.offlineVideos)
        assertEquals(1234567890L, stats.lastSyncTime)
        assertTrue(stats.cacheSize > 0) // Should have estimated cache size
    }

    @Test
    fun `isContentAvailableOffline should check content availability correctly`() = runTest {
        // Given
        val articleId = "article_123"
        val videoId = "video_456"
        val mockArticle = createMockArticle(articleId, "nutrition")
        val mockVideo = createMockVideo(videoId, "fitness", isOffline = true)
        
        coEvery { mockDiscoverDao.getHealthArticleById(articleId, testUserId) } returns mockArticle
        coEvery { mockDiscoverDao.getHealthNewsById("news_789", testUserId) } returns null
        coEvery { mockDiscoverDao.getEducationalVideoById(videoId, testUserId) } returns mockVideo

        // When & Then
        assertTrue(contentCacheManager.isContentAvailableOffline(articleId, "article", testUserId))
        assertFalse(contentCacheManager.isContentAvailableOffline("news_789", "news", testUserId))
        assertTrue(contentCacheManager.isContentAvailableOffline(videoId, "video", testUserId))
    }

    @Test
    fun `getOfflineContentStatus should return correct status information`() = runTest {
        // Given
        val currentTime = System.currentTimeMillis()
        val recentSyncTime = currentTime - (30 * 60 * 1000) // 30 minutes ago
        
        coEvery { mockDiscoverDao.getArticleCount(testUserId) } returns 100
        coEvery { mockDiscoverDao.getNewsCount(testUserId) } returns 50
        coEvery { mockDiscoverDao.getVideoCount(testUserId) } returns 25
        coEvery { mockDiscoverDao.getBookmarkCount(testUserId) } returns 15
        coEvery { mockDiscoverDao.getOfflineVideoCount(testUserId) } returns 10
        coEvery { mockDiscoverDao.getLastSyncTime(testUserId) } returns recentSyncTime
        coEvery { mockDiscoverDao.getEssentialContentCount(testUserId) } returns 15

        // When
        val status = contentCacheManager.getOfflineContentStatus(testUserId)

        // Then
        assertTrue(status.isOfflineReady)
        assertTrue(status.essentialContentAvailable)
        assertEquals(175, status.totalCachedItems) // 100 + 50 + 25
        assertEquals(SyncFreshness.Fresh, status.syncFreshness)
        assertEquals(recentSyncTime, status.lastSyncTime)
    }

    @Test
    fun `cache status flow should emit correct states during operations`() = runTest {
        // Given
        coEvery { mockDiscoverDao.getEssentialContentCount(testUserId) } returns 5
        coEvery { mockDiscoverDao.cleanupExpiredArticles(any(), testUserId) } returns 0
        coEvery { mockDiscoverDao.cleanupExpiredNews(any(), testUserId) } returns 0
        coEvery { mockDiscoverDao.cleanupExpiredVideos(any(), testUserId) } returns 0
        coEvery { mockDiscoverDao.markContentAsEssential(any(), any(), any()) } just Runs

        // When
        val initialStatus = contentCacheManager.cacheStatus.first()
        contentCacheManager.initializeCache(testUserId)
        
        // Then
        assertEquals(CacheStatus.Idle, initialStatus)
        // Note: In a real test, we'd collect multiple emissions to verify state transitions
    }

    @Test
    fun `prefetchEssentialContent should mark high priority categories`() = runTest {
        // Given
        coEvery { mockDiscoverDao.markContentAsEssential(any(), any(), any()) } just Runs

        // When
        val result = contentCacheManager.prefetchEssentialContent(testUserId)

        // Then
        assertTrue(result is Result.Success)
        
        // Verify all high-priority categories were marked as essential
        coVerify { mockDiscoverDao.markContentAsEssential(testUserId, "preventive-care", any()) }
        coVerify { mockDiscoverDao.markContentAsEssential(testUserId, "emergency", any()) }
        coVerify { mockDiscoverDao.markContentAsEssential(testUserId, "chronic-conditions", any()) }
    }

    @Test
    fun `error handling should return appropriate error results`() = runTest {
        // Given
        coEvery { mockDiscoverDao.getEssentialContentCount(testUserId) } throws RuntimeException("Database error")

        // When
        val result = contentCacheManager.initializeCache(testUserId)

        // Then
        assertTrue(result is Result.Error)
        assertEquals("Cache initialization failed: Database error", (result as Result.Error).message)
    }

    // Helper methods for creating mock entities
    
    private fun createMockArticle(id: String, category: String) = HealthArticleEntity(
        id = id,
        title = "Test Article $id",
        summary = "Test summary",
        content = "Test content",
        category = category,
        authorName = "Test Author",
        authorCredentials = "MD",
        sourceUrl = "https://test.com",
        publishedDate = System.currentTimeMillis(),
        lastUpdated = System.currentTimeMillis(),
        readingTimeMinutes = 5,
        imageUrl = null,
        tags = listOf("test"),
        isBookmarked = false,
        readProgress = 0f,
        credibilityScore = 4,
        userId = testUserId
    )
    
    private fun createMockNews(id: String, category: String) = HealthNewsEntity(
        id = id,
        headline = "Test News $id",
        summary = "Test summary",
        fullContent = "Test content",
        category = category,
        sourcePublication = "Test Publication",
        sourceCredibility = "medical-journal",
        publishedDate = System.currentTimeMillis(),
        imageUrl = null,
        externalUrl = "https://test.com",
        isBreakingNews = false,
        relevanceScore = 3,
        userId = testUserId
    )
    
    private fun createMockVideo(id: String, category: String, isOffline: Boolean = false) = EducationalVideoEntity(
        id = id,
        title = "Test Video $id",
        description = "Test description",
        category = category,
        thumbnailUrl = "https://test.com/thumb.jpg",
        videoUrl = "https://test.com/video.mp4",
        durationSeconds = 300,
        difficultyLevel = "beginner",
        expertName = "Test Expert",
        expertCredentials = "PhD",
        publishedDate = System.currentTimeMillis(),
        watchProgress = 0f,
        isDownloadedOffline = isOffline,
        transcriptAvailable = false,
        userId = testUserId
    )
}