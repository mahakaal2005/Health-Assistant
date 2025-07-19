package com.example.health_assistant.features.discover.data

import com.example.health_assistant.core.util.Result
import com.example.health_assistant.features.discover.data.cache.ContentCacheManager
import com.example.health_assistant.features.discover.data.entity.*
import com.example.health_assistant.features.discover.data.firebase.*
import com.example.health_assistant.features.discover.data.mapper.toEntity
import com.example.health_assistant.features.discover.data.mapper.toDomain
import com.example.health_assistant.features.discover.domain.model.*
import com.example.health_assistant.features.discover.domain.repository.DiscoverRepository
import com.example.health_assistant.features.discover.domain.validation.ContentCredibilityValidator
import com.google.firebase.firestore.*
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Comprehensive unit tests for DiscoverRepositoryImpl
 * Tests all repository methods, error handling, and data flow
 */
class DiscoverRepositoryImplComprehensiveTest {

    private lateinit var repository: DiscoverRepository
    private lateinit var discoverDao: DiscoverDao
    private lateinit var firestore: FirebaseFirestore
    private lateinit var cacheManager: ContentCacheManager
    private lateinit var credibilityValidator: ContentCredibilityValidator

    private val testUserId = "test_user_123"

    @BeforeEach
    fun setup() {
        discoverDao = mockk()
        firestore = mockk()
        cacheManager = mockk()
        credibilityValidator = mockk()

        repository = DiscoverRepositoryImpl(
            discoverDao = discoverDao,
            firestore = firestore,
            cacheManager = cacheManager,
            credibilityValidator = credibilityValidator
        )
    }

    // ==================== HEALTH ARTICLES TESTS ====================

    @Test
    fun `getHealthArticles returns cached data first then syncs`() = runTest {
        // Given
        val cachedArticles = listOf(createTestHealthArticleEntity())
        val remoteArticles = listOf(createTestFirebaseHealthArticle())
        
        coEvery { discoverDao.getHealthArticlesFlow(testUserId, null, 20) } returns flowOf(cachedArticles)
        mockFirestoreCollection("health_articles", remoteArticles)
        coEvery { discoverDao.insertHealthArticles(any()) } just Runs

        // When
        val result = repository.getHealthArticles(testUserId, null, 20).first()

        // Then
        assertTrue(result is Result.Success)
        assertEquals(1, result.data.size)
        assertEquals("test-article-1", result.data[0].id)
        
        // Verify background sync was triggered
        coVerify { discoverDao.insertHealthArticles(any()) }
    }

    @Test
    fun `getHealthArticles handles network error gracefully`() = runTest {
        // Given
        val cachedArticles = listOf(createTestHealthArticleEntity())
        coEvery { discoverDao.getHealthArticlesFlow(testUserId, null, 20) } returns flowOf(cachedArticles)
        mockFirestoreError()

        // When
        val result = repository.getHealthArticles(testUserId, null, 20).first()

        // Then
        assertTrue(result is Result.Success) // Should still return cached data
        assertEquals(1, result.data.size)
    }

    @Test
    fun `getHealthArticleById returns correct article`() = runTest {
        // Given
        val article = createTestHealthArticleEntity()
        coEvery { discoverDao.getHealthArticleById("test-article-1", testUserId) } returns article

        // When
        val result = repository.getHealthArticleById("test-article-1", testUserId)

        // Then
        assertTrue(result is Result.Success)
        assertEquals("test-article-1", result.data.id)
        assertEquals("Test Article", result.data.title)
    }

    @Test
    fun `getHealthArticleById returns error when not found`() = runTest {
        // Given
        coEvery { discoverDao.getHealthArticleById("nonexistent", testUserId) } returns null

        // When
        val result = repository.getHealthArticleById("nonexistent", testUserId)

        // Then
        assertTrue(result is Result.Error)
    }

    @Test
    fun `updateReadingProgress updates article progress`() = runTest {
        // Given
        coEvery { discoverDao.updateArticleReadingProgress("test-article-1", 0.75f, testUserId) } just Runs

        // When
        val result = repository.updateReadingProgress("test-article-1", 0.75f, testUserId)

        // Then
        assertTrue(result is Result.Success)
        coVerify { discoverDao.updateArticleReadingProgress("test-article-1", 0.75f, testUserId) }
    }

    // ==================== HEALTH NEWS TESTS ====================

    @Test
    fun `getHealthNews returns cached data first then syncs`() = runTest {
        // Given
        val cachedNews = listOf(createTestHealthNewsEntity())
        val remoteNews = listOf(createTestFirebaseHealthNews())
        
        coEvery { discoverDao.getHealthNewsFlow(testUserId, null, 10) } returns flowOf(cachedNews)
        mockFirestoreCollection("health_news", remoteNews)
        coEvery { discoverDao.insertHealthNews(any()) } just Runs

        // When
        val result = repository.getHealthNews(testUserId, null, 10).first()

        // Then
        assertTrue(result is Result.Success)
        assertEquals(1, result.data.size)
        assertEquals("test-news-1", result.data[0].id)
        
        coVerify { discoverDao.insertHealthNews(any()) }
    }

    @Test
    fun `getBreakingNews returns only breaking news`() = runTest {
        // Given
        val breakingNews = listOf(createTestHealthNewsEntity(isBreaking = true))
        coEvery { discoverDao.getBreakingNews(testUserId, 10) } returns breakingNews

        // When
        val result = repository.getBreakingNews(testUserId, 10)

        // Then
        assertTrue(result is Result.Success)
        assertEquals(1, result.data.size)
        assertTrue(result.data[0].isBreakingNews)
    }

    // ==================== EDUCATIONAL VIDEOS TESTS ====================

    @Test
    fun `getEducationalVideos returns cached data first then syncs`() = runTest {
        // Given
        val cachedVideos = listOf(createTestEducationalVideoEntity())
        val remoteVideos = listOf(createTestFirebaseEducationalVideo())
        
        coEvery { discoverDao.getEducationalVideosFlow(testUserId, null, 15) } returns flowOf(cachedVideos)
        mockFirestoreCollection("educational_videos", remoteVideos)
        coEvery { discoverDao.insertEducationalVideos(any()) } just Runs

        // When
        val result = repository.getEducationalVideos(testUserId, null, 15).first()

        // Then
        assertTrue(result is Result.Success)
        assertEquals(1, result.data.size)
        assertEquals("test-video-1", result.data[0].id)
        
        coVerify { discoverDao.insertEducationalVideos(any()) }
    }

    @Test
    fun `updateVideoWatchProgress updates video progress`() = runTest {
        // Given
        coEvery { discoverDao.updateVideoWatchProgress("test-video-1", 0.5f, testUserId) } just Runs

        // When
        val result = repository.updateVideoWatchProgress("test-video-1", 0.5f, testUserId)

        // Then
        assertTrue(result is Result.Success)
        coVerify { discoverDao.updateVideoWatchProgress("test-video-1", 0.5f, testUserId) }
    }

    // ==================== SEARCH FUNCTIONALITY TESTS ====================

    @Test
    fun `searchContent returns mixed content results`() = runTest {
        // Given
        val searchResults = listOf(
            ContentSearchResult("article", "1", "Diabetes Article", System.currentTimeMillis(), "nutrition", null),
            ContentSearchResult("news", "2", "Diabetes News", System.currentTimeMillis(), "health", null),
            ContentSearchResult("video", "3", "Diabetes Video", System.currentTimeMillis(), "nutrition", null)
        )
        coEvery { discoverDao.searchAllContent(testUserId, "diabetes", 20) } returns searchResults

        // When
        val result = repository.searchContent(testUserId, "diabetes", listOf("article", "news", "video"), 20)

        // Then
        assertTrue(result is Result.Success)
        assertEquals(3, result.data.size)
        
        val contentTypes = result.data.map { 
            when (it) {
                is DiscoverContent.Article -> "article"
                is DiscoverContent.News -> "news"
                is DiscoverContent.Video -> "video"
            }
        }.toSet()
        
        assertTrue(contentTypes.contains("article"))
        assertTrue(contentTypes.contains("news"))
        assertTrue(contentTypes.contains("video"))
    }

    @Test
    fun `searchContent handles empty query`() = runTest {
        // When
        val result = repository.searchContent(testUserId, "", listOf("article"), 20)

        // Then
        assertTrue(result is Result.Error)
    }

    // ==================== BOOKMARK FUNCTIONALITY TESTS ====================

    @Test
    fun `toggleBookmark adds bookmark when not bookmarked`() = runTest {
        // Given
        val content = createTestDiscoverContentArticle()
        coEvery { discoverDao.isContentBookmarked(content.id, testUserId) } returns false
        coEvery { discoverDao.insertBookmark(any()) } just Runs
        coEvery { discoverDao.updateArticleBookmarkStatus(content.id, true, testUserId) } just Runs

        // When
        val result = repository.toggleBookmark(testUserId, content)

        // Then
        assertTrue(result is Result.Success)
        assertTrue(result.data) // Should return true for bookmarked
        coVerify { discoverDao.insertBookmark(any()) }
        coVerify { discoverDao.updateArticleBookmarkStatus(content.id, true, testUserId) }
    }

    @Test
    fun `toggleBookmark removes bookmark when already bookmarked`() = runTest {
        // Given
        val content = createTestDiscoverContentArticle()
        coEvery { discoverDao.isContentBookmarked(content.id, testUserId) } returns true
        coEvery { discoverDao.removeBookmark(content.id, testUserId) } just Runs
        coEvery { discoverDao.updateArticleBookmarkStatus(content.id, false, testUserId) } just Runs

        // When
        val result = repository.toggleBookmark(testUserId, content)

        // Then
        assertTrue(result is Result.Success)
        assertFalse(result.data) // Should return false for unbookmarked
        coVerify { discoverDao.removeBookmark(content.id, testUserId) }
        coVerify { discoverDao.updateArticleBookmarkStatus(content.id, false, testUserId) }
    }

    @Test
    fun `getBookmarkedContent returns all bookmarked content`() = runTest {
        // Given
        val bookmarks = listOf(
            ContentBookmarkEntity("1", "article-1", "article", System.currentTimeMillis(), testUserId),
            ContentBookmarkEntity("2", "video-1", "video", System.currentTimeMillis(), testUserId)
        )
        val article = createTestHealthArticleEntity(id = "article-1")
        val video = createTestEducationalVideoEntity(id = "video-1")
        
        coEvery { discoverDao.getContentBookmarksFlow(testUserId) } returns flowOf(bookmarks)
        coEvery { discoverDao.getHealthArticleById("article-1", testUserId) } returns article
        coEvery { discoverDao.getEducationalVideoById("video-1", testUserId) } returns video

        // When
        val result = repository.getBookmarkedContent(testUserId).first()

        // Then
        assertTrue(result is Result.Success)
        assertEquals(2, result.data.size)
    }

    // ==================== CONTENT VALIDATION TESTS ====================

    @Test
    fun `validateContentCredibility returns validation result`() = runTest {
        // Given
        val content = createTestDiscoverContentArticle()
        val validationResult = ContentValidationResult(
            isCredible = true,
            credibilityScore = 5,
            warnings = emptyList(),
            lastValidated = System.currentTimeMillis()
        )
        coEvery { credibilityValidator.validateContent(content) } returns validationResult

        // When
        val result = repository.validateContentCredibility(content)

        // Then
        assertTrue(result is Result.Success)
        assertTrue(result.data.isCredible)
        assertEquals(5, result.data.credibilityScore)
    }

    // ==================== SYNC FUNCTIONALITY TESTS ====================

    @Test
    fun `syncContent syncs all content types`() = runTest {
        // Given
        val remoteArticles = listOf(createTestFirebaseHealthArticle())
        val remoteNews = listOf(createTestFirebaseHealthNews())
        val remoteVideos = listOf(createTestFirebaseEducationalVideo())
        
        mockFirestoreCollection("health_articles", remoteArticles)
        mockFirestoreCollection("health_news", remoteNews)
        mockFirestoreCollection("educational_videos", remoteVideos)
        
        coEvery { discoverDao.insertHealthArticles(any()) } just Runs
        coEvery { discoverDao.insertHealthNews(any()) } just Runs
        coEvery { discoverDao.insertEducationalVideos(any()) } just Runs

        // When
        val result = repository.syncContent(testUserId)

        // Then
        assertTrue(result is Result.Success)
        coVerify { discoverDao.insertHealthArticles(any()) }
        coVerify { discoverDao.insertHealthNews(any()) }
        coVerify { discoverDao.insertEducationalVideos(any()) }
    }

    @Test
    fun `syncContent handles partial failures`() = runTest {
        // Given
        val remoteArticles = listOf(createTestFirebaseHealthArticle())
        mockFirestoreCollection("health_articles", remoteArticles)
        mockFirestoreError("health_news")
        mockFirestoreError("educational_videos")
        
        coEvery { discoverDao.insertHealthArticles(any()) } just Runs

        // When
        val result = repository.syncContent(testUserId)

        // Then
        assertTrue(result is Result.Success) // Should succeed with partial data
        coVerify { discoverDao.insertHealthArticles(any()) }
    }

    // ==================== CACHE MANAGEMENT TESTS ====================

    @Test
    fun `cleanupOldContent removes old cached content`() = runTest {
        // Given
        val cutoffTime = System.currentTimeMillis() - (180 * 24 * 60 * 60 * 1000L) // 6 months ago
        coEvery { discoverDao.cleanupOldArticles(cutoffTime, testUserId) } returns 5
        coEvery { discoverDao.cleanupOldNews(cutoffTime, testUserId) } returns 3
        coEvery { discoverDao.cleanupOldVideos(cutoffTime, testUserId) } returns 2
        coEvery { discoverDao.cleanupOrphanedBookmarks(testUserId) } returns 1

        // When
        val result = repository.cleanupOldContent(testUserId)

        // Then
        assertTrue(result is Result.Success)
        assertEquals(11, result.data) // Total deleted items
        coVerify { discoverDao.cleanupOldArticles(cutoffTime, testUserId) }
        coVerify { discoverDao.cleanupOldNews(cutoffTime, testUserId) }
        coVerify { discoverDao.cleanupOldVideos(cutoffTime, testUserId) }
        coVerify { discoverDao.cleanupOrphanedBookmarks(testUserId) }
    }

    @Test
    fun `getCacheStatistics returns correct statistics`() = runTest {
        // Given
        val expectedStats = CacheStatistics(
            articleCount = 10,
            newsCount = 5,
            videoCount = 8,
            bookmarkCount = 3
        )
        coEvery { discoverDao.getCacheStatistics(testUserId) } returns expectedStats

        // When
        val result = repository.getCacheStatistics(testUserId)

        // Then
        assertTrue(result is Result.Success)
        assertEquals(10, result.data.articleCount)
        assertEquals(5, result.data.newsCount)
        assertEquals(8, result.data.videoCount)
        assertEquals(3, result.data.bookmarkCount)
    }

    // ==================== ERROR HANDLING TESTS ====================

    @Test
    fun `repository handles database exceptions`() = runTest {
        // Given
        coEvery { discoverDao.getHealthArticlesFlow(testUserId, null, 20) } throws RuntimeException("Database error")

        // When & Then
        assertThrows<RuntimeException> {
            repository.getHealthArticles(testUserId, null, 20).first()
        }
    }

    @Test
    fun `repository handles firestore exceptions`() = runTest {
        // Given
        val cachedArticles = listOf(createTestHealthArticleEntity())
        coEvery { discoverDao.getHealthArticlesFlow(testUserId, null, 20) } returns flowOf(cachedArticles)
        mockFirestoreError()

        // When
        val result = repository.getHealthArticles(testUserId, null, 20).first()

        // Then
        assertTrue(result is Result.Success) // Should fallback to cached data
    }

    // ==================== HELPER METHODS ====================

    private fun createTestHealthArticleEntity(
        id: String = "test-article-1",
        title: String = "Test Article"
    ) = HealthArticleEntity(
        id = id,
        title = title,
        summary = "Test summary",
        content = "Test content",
        category = "nutrition",
        authorName = "Dr. Test",
        authorCredentials = "MD",
        sourceUrl = "https://test.com",
        publishedDate = System.currentTimeMillis(),
        lastUpdated = System.currentTimeMillis(),
        readingTimeMinutes = 5,
        imageUrl = "https://test.com/image.jpg",
        tags = listOf("health", "nutrition"),
        isBookmarked = false,
        readProgress = 0f,
        credibilityScore = 5,
        userId = testUserId
    )

    private fun createTestHealthNewsEntity(
        id: String = "test-news-1",
        isBreaking: Boolean = false
    ) = HealthNewsEntity(
        id = id,
        headline = "Test News",
        summary = "Test summary",
        fullContent = "Test content",
        category = "health",
        sourcePublication = "Test Journal",
        sourceCredibility = "medical-journal",
        publishedDate = System.currentTimeMillis(),
        imageUrl = "https://test.com/image.jpg",
        externalUrl = "https://test.com/news",
        isBreakingNews = isBreaking,
        relevanceScore = 5,
        userId = testUserId
    )

    private fun createTestEducationalVideoEntity(
        id: String = "test-video-1"
    ) = EducationalVideoEntity(
        id = id,
        title = "Test Video",
        description = "Test description",
        category = "nutrition",
        thumbnailUrl = "https://test.com/thumb.jpg",
        videoUrl = "https://test.com/video.mp4",
        durationSeconds = 300,
        difficultyLevel = "beginner",
        expertName = "Dr. Expert",
        expertCredentials = "MD",
        publishedDate = System.currentTimeMillis(),
        watchProgress = 0f,
        isDownloadedOffline = false,
        transcriptAvailable = true,
        userId = testUserId
    )

    private fun createTestFirebaseHealthArticle() = FirebaseHealthArticle(
        id = "test-article-1",
        title = "Test Article",
        summary = "Test summary",
        content = "Test content",
        category = "nutrition",
        authorName = "Dr. Test",
        authorCredentials = "MD",
        sourceUrl = "https://test.com",
        publishedDate = com.google.firebase.Timestamp.now(),
        lastUpdated = com.google.firebase.Timestamp.now(),
        readingTimeMinutes = 5,
        imageUrl = "https://test.com/image.jpg",
        tags = listOf("health", "nutrition"),
        credibilityScore = 5
    )

    private fun createTestFirebaseHealthNews() = FirebaseHealthNews(
        id = "test-news-1",
        headline = "Test News",
        summary = "Test summary",
        fullContent = "Test content",
        category = "health",
        sourcePublication = "Test Journal",
        sourceCredibility = "medical-journal",
        publishedDate = com.google.firebase.Timestamp.now(),
        imageUrl = "https://test.com/image.jpg",
        externalUrl = "https://test.com/news",
        isBreakingNews = false,
        relevanceScore = 5
    )

    private fun createTestFirebaseEducationalVideo() = FirebaseEducationalVideo(
        id = "test-video-1",
        title = "Test Video",
        description = "Test description",
        category = "nutrition",
        thumbnailUrl = "https://test.com/thumb.jpg",
        videoUrl = "https://test.com/video.mp4",
        durationSeconds = 300,
        difficultyLevel = "beginner",
        expertName = "Dr. Expert",
        expertCredentials = "MD",
        publishedDate = com.google.firebase.Timestamp.now(),
        transcriptAvailable = true
    )

    private fun createTestDiscoverContentArticle() = DiscoverContent.Article(
        id = "test-article-1",
        title = "Test Article",
        publishedDate = System.currentTimeMillis(),
        category = "nutrition",
        imageUrl = "https://test.com/image.jpg",
        userId = testUserId,
        summary = "Test summary",
        content = "Test content",
        authorName = "Dr. Test",
        authorCredentials = "MD",
        sourceUrl = "https://test.com",
        lastUpdated = System.currentTimeMillis(),
        readingTimeMinutes = 5,
        tags = listOf("health", "nutrition"),
        isBookmarked = false,
        readProgress = 0f,
        credibilityScore = 5
    )

    private fun mockFirestoreCollection(collection: String, data: List<Any>) {
        val collectionRef = mockk<CollectionReference>()
        val query = mockk<Query>()
        val querySnapshot = mockk<QuerySnapshot>()
        val documents = data.map { item ->
            val doc = mockk<QueryDocumentSnapshot>()
            every { doc.toObject(any<Class<Any>>()) } returns item
            doc
        }

        every { firestore.collection(collection) } returns collectionRef
        every { collectionRef.orderBy("publishedDate", Query.Direction.DESCENDING) } returns query
        every { query.limit(any()) } returns query
        coEvery { query.get() } returns querySnapshot
        every { querySnapshot.documents } returns documents
    }

    private fun mockFirestoreError(collection: String = "health_articles") {
        val collectionRef = mockk<CollectionReference>()
        val query = mockk<Query>()

        every { firestore.collection(collection) } returns collectionRef
        every { collectionRef.orderBy("publishedDate", Query.Direction.DESCENDING) } returns query
        every { query.limit(any()) } returns query
        coEvery { query.get() } throws FirebaseFirestoreException("Network error", FirebaseFirestoreException.Code.UNAVAILABLE)
    }
}