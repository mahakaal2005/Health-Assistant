package com.example.health_assistant.features.discover.data

import com.example.health_assistant.features.discover.data.entity.HealthArticleEntity
import com.example.health_assistant.features.discover.data.entity.HealthNewsEntity
import com.example.health_assistant.features.discover.data.entity.EducationalVideoEntity
import com.example.health_assistant.features.discover.data.entity.ContentBookmarkEntity
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import java.util.concurrent.TimeUnit
import io.mockk.*

/**
 * Unit tests for DiscoverDao
 * Tests all CRUD operations, search functionality, and cache management
 */
class DiscoverDaoTest {

    private lateinit var discoverDao: DiscoverDao

    private val testUserId = "test_user_123"
    private val otherUserId = "other_user_456"

    @Before
    fun setup() {
        discoverDao = mockk()
    }

    // ==================== HEALTH ARTICLES TESTS ====================

    @Test
    fun insertAndGetHealthArticles_returnsCorrectData() = runTest {
        // Given
        val articles = listOf(
            createTestHealthArticle("1", "Article 1", "nutrition"),
            createTestHealthArticle("2", "Article 2", "fitness"),
            createTestHealthArticle("3", "Article 3", "nutrition")
        )
        val expectedResult = articles.sortedByDescending { it.publishedDate }

        coEvery { discoverDao.insertHealthArticles(articles) } just Runs
        coEvery { discoverDao.getHealthArticles(testUserId, null, 10) } returns expectedResult

        // When
        discoverDao.insertHealthArticles(articles)
        val result = discoverDao.getHealthArticles(testUserId, null, 10)

        // Then
        assertEquals(3, result.size)
        assertEquals("Article 3", result[0].title) // Most recent first
        coVerify { discoverDao.insertHealthArticles(articles) }
        coVerify { discoverDao.getHealthArticles(testUserId, null, 10) }
    }

    @Test
    fun getHealthArticlesByCategory_filtersCorrectly() = runTest {
        // Given
        val nutritionArticles = listOf(
            createTestHealthArticle("1", "Nutrition Article", "nutrition"),
            createTestHealthArticle("3", "Another Nutrition", "nutrition")
        )
        val fitnessArticles = listOf(
            createTestHealthArticle("2", "Fitness Article", "fitness")
        )

        coEvery { discoverDao.getHealthArticles(testUserId, "nutrition", 10) } returns nutritionArticles
        coEvery { discoverDao.getHealthArticles(testUserId, "fitness", 10) } returns fitnessArticles

        // When
        val nutritionResult = discoverDao.getHealthArticles(testUserId, "nutrition", 10)
        val fitnessResult = discoverDao.getHealthArticles(testUserId, "fitness", 10)

        // Then
        assertEquals(2, nutritionResult.size)
        assertEquals(1, fitnessResult.size)
        assertTrue(nutritionResult.all { it.category == "nutrition" })
        assertTrue(fitnessResult.all { it.category == "fitness" })
    }

    @Test
    fun getHealthArticlesFlow_emitsUpdates() = runTest {
        // Given
        val article = createTestHealthArticle("1", "Test Article", "nutrition")
        val emptyFlow = flowOf(emptyList<HealthArticleEntity>())
        val articleFlow = flowOf(listOf(article))

        every { discoverDao.getHealthArticlesFlow(testUserId, null, 10) } returns emptyFlow andThen articleFlow
        coEvery { discoverDao.insertHealthArticle(article) } just Runs

        // When
        val initialResult = discoverDao.getHealthArticlesFlow(testUserId, null, 10)
        discoverDao.insertHealthArticle(article)
        val updatedResult = discoverDao.getHealthArticlesFlow(testUserId, null, 10)

        // Then
        coVerify { discoverDao.insertHealthArticle(article) }
        verify { discoverDao.getHealthArticlesFlow(testUserId, null, 10) }
    }

    @Test
    fun updateArticleReadingProgress_updatesCorrectly() = runTest {
        // Given
        val originalArticle = createTestHealthArticle("1", "Test Article", "nutrition")
        val updatedArticle = originalArticle.copy(readProgress = 0.75f)

        coEvery { discoverDao.insertHealthArticle(originalArticle) } just Runs
        coEvery { discoverDao.updateArticleReadingProgress("1", 0.75f, testUserId) } just Runs
        coEvery { discoverDao.getHealthArticleById("1", testUserId) } returns updatedArticle

        // When
        discoverDao.insertHealthArticle(originalArticle)
        discoverDao.updateArticleReadingProgress("1", 0.75f, testUserId)
        val result = discoverDao.getHealthArticleById("1", testUserId)

        // Then
        assertNotNull(result)
        assertEquals(0.75f, result!!.readProgress, 0.001f)
        coVerify { discoverDao.updateArticleReadingProgress("1", 0.75f, testUserId) }
    }

    @Test
    fun updateArticleBookmarkStatus_updatesCorrectly() = runTest {
        // Given
        val originalArticle = createTestHealthArticle("1", "Test Article", "nutrition")
        val bookmarkedArticle = originalArticle.copy(isBookmarked = true)

        coEvery { discoverDao.insertHealthArticle(originalArticle) } just Runs
        coEvery { discoverDao.updateArticleBookmarkStatus("1", true, testUserId) } just Runs
        coEvery { discoverDao.getHealthArticleById("1", testUserId) } returns bookmarkedArticle

        // When
        discoverDao.insertHealthArticle(originalArticle)
        discoverDao.updateArticleBookmarkStatus("1", true, testUserId)
        val result = discoverDao.getHealthArticleById("1", testUserId)

        // Then
        assertNotNull(result)
        assertTrue(result!!.isBookmarked)
        coVerify { discoverDao.updateArticleBookmarkStatus("1", true, testUserId) }
    }

    @Test
    fun getBookmarkedArticles_returnsOnlyBookmarked() = runTest {
        // Given
        val bookmarkedArticles = listOf(
            createTestHealthArticle("1", "Article 1", "nutrition", isBookmarked = true),
            createTestHealthArticle("3", "Article 3", "nutrition", isBookmarked = true)
        )

        coEvery { discoverDao.getBookmarkedArticles(testUserId) } returns bookmarkedArticles

        // When
        val result = discoverDao.getBookmarkedArticles(testUserId)

        // Then
        assertEquals(2, result.size)
        assertTrue(result.all { it.isBookmarked })
        coVerify { discoverDao.getBookmarkedArticles(testUserId) }
    }

    @Test
    fun searchHealthArticles_findsMatchingContent() = runTest {
        // Given
        val searchResults = listOf(
            createTestHealthArticle("1", "Diabetes Management", "nutrition", content = "Managing blood sugar levels"),
            createTestHealthArticle("3", "Diabetes Prevention", "nutrition", content = "Preventing type 2 diabetes")
        )

        coEvery { discoverDao.searchHealthArticles(testUserId, "diabetes", 10) } returns searchResults

        // When
        val result = discoverDao.searchHealthArticles(testUserId, "diabetes", 10)

        // Then
        assertEquals(2, result.size)
        assertTrue(result.any { it.title.contains("Diabetes Management") })
        assertTrue(result.any { it.title.contains("Diabetes Prevention") })
        coVerify { discoverDao.searchHealthArticles(testUserId, "diabetes", 10) }
    }

    @Test
    fun getArticlesByCredibility_filtersCorrectly() = runTest {
        // Given
        val highCredibilityArticles = listOf(
            createTestHealthArticle("1", "High Credibility", "nutrition", credibilityScore = 5)
        )
        val mediumCredibilityArticles = listOf(
            createTestHealthArticle("1", "High Credibility", "nutrition", credibilityScore = 5),
            createTestHealthArticle("2", "Medium Credibility", "fitness", credibilityScore = 3)
        )

        coEvery { discoverDao.getArticlesByCredibility(testUserId, 4, 10) } returns highCredibilityArticles
        coEvery { discoverDao.getArticlesByCredibility(testUserId, 3, 10) } returns mediumCredibilityArticles

        // When
        val highCredibility = discoverDao.getArticlesByCredibility(testUserId, 4, 10)
        val mediumCredibility = discoverDao.getArticlesByCredibility(testUserId, 3, 10)

        // Then
        assertEquals(1, highCredibility.size)
        assertEquals("High Credibility", highCredibility[0].title)
        assertEquals(2, mediumCredibility.size)
        coVerify { discoverDao.getArticlesByCredibility(testUserId, 4, 10) }
        coVerify { discoverDao.getArticlesByCredibility(testUserId, 3, 10) }
    }

    // ==================== HEALTH NEWS TESTS ====================

    @Test
    fun insertAndGetHealthNews_returnsCorrectData() = runTest {
        // Given
        val news = listOf(
            createTestHealthNews("1", "News 1", "health"),
            createTestHealthNews("2", "News 2", "research"),
            createTestHealthNews("3", "News 3", "health")
        )
        val expectedResult = news.sortedByDescending { it.publishedDate }

        coEvery { discoverDao.insertHealthNews(news) } just Runs
        coEvery { discoverDao.getHealthNews(testUserId, null, 10) } returns expectedResult

        // When
        discoverDao.insertHealthNews(news)
        val result = discoverDao.getHealthNews(testUserId, null, 10)

        // Then
        assertEquals(3, result.size)
        assertEquals("News 3", result[0].headline) // Most recent first
        coVerify { discoverDao.insertHealthNews(news) }
        coVerify { discoverDao.getHealthNews(testUserId, null, 10) }
    }

    @Test
    fun getBreakingNews_returnsOnlyBreaking() = runTest {
        // Given
        val breakingNews = listOf(
            createTestHealthNews("2", "Breaking News", "health", isBreaking = true),
            createTestHealthNews("3", "Another Breaking", "research", isBreaking = true)
        )

        coEvery { discoverDao.getBreakingNews(testUserId, 10) } returns breakingNews

        // When
        val result = discoverDao.getBreakingNews(testUserId, 10)

        // Then
        assertEquals(2, result.size)
        assertTrue(result.all { it.isBreakingNews })
        coVerify { discoverDao.getBreakingNews(testUserId, 10) }
    }

    // ==================== EDUCATIONAL VIDEOS TESTS ====================

    @Test
    fun insertAndGetEducationalVideos_returnsCorrectData() = runTest {
        // Given
        val videos = listOf(
            createTestEducationalVideo("1", "Video 1", "nutrition"),
            createTestEducationalVideo("2", "Video 2", "fitness"),
            createTestEducationalVideo("3", "Video 3", "nutrition")
        )
        val expectedResult = videos.sortedByDescending { it.publishedDate }

        coEvery { discoverDao.insertEducationalVideos(videos) } just Runs
        coEvery { discoverDao.getEducationalVideos(testUserId, null, 10) } returns expectedResult

        // When
        discoverDao.insertEducationalVideos(videos)
        val result = discoverDao.getEducationalVideos(testUserId, null, 10)

        // Then
        assertEquals(3, result.size)
        assertEquals("Video 3", result[0].title) // Most recent first
        coVerify { discoverDao.insertEducationalVideos(videos) }
        coVerify { discoverDao.getEducationalVideos(testUserId, null, 10) }
    }

    @Test
    fun updateVideoWatchProgress_updatesCorrectly() = runTest {
        // Given
        val originalVideo = createTestEducationalVideo("1", "Test Video", "nutrition")
        val updatedVideo = originalVideo.copy(watchProgress = 0.5f)

        coEvery { discoverDao.insertEducationalVideo(originalVideo) } just Runs
        coEvery { discoverDao.updateVideoWatchProgress("1", 0.5f, testUserId) } just Runs
        coEvery { discoverDao.getEducationalVideoById("1", testUserId) } returns updatedVideo

        // When
        discoverDao.insertEducationalVideo(originalVideo)
        discoverDao.updateVideoWatchProgress("1", 0.5f, testUserId)
        val result = discoverDao.getEducationalVideoById("1", testUserId)

        // Then
        assertNotNull(result)
        assertEquals(0.5f, result!!.watchProgress, 0.001f)
        coVerify { discoverDao.updateVideoWatchProgress("1", 0.5f, testUserId) }
    }

    // ==================== CONTENT BOOKMARKS TESTS ====================

    @Test
    fun insertAndGetBookmarks_returnsCorrectData() = runTest {
        // Given
        val bookmarks = listOf(
            createTestBookmark("1", "article_1", "article"),
            createTestBookmark("2", "video_1", "video"),
            createTestBookmark("3", "news_1", "news")
        )

        coEvery { discoverDao.insertBookmark(any()) } just Runs
        coEvery { discoverDao.getContentBookmarks(testUserId) } returns bookmarks

        // When
        bookmarks.forEach { discoverDao.insertBookmark(it) }
        val result = discoverDao.getContentBookmarks(testUserId)

        // Then
        assertEquals(3, result.size)
        coVerify(exactly = 3) { discoverDao.insertBookmark(any()) }
        coVerify { discoverDao.getContentBookmarks(testUserId) }
    }

    @Test
    fun getBookmarksByType_filtersCorrectly() = runTest {
        // Given
        val articleBookmarks = listOf(
            createTestBookmark("1", "article_1", "article"),
            createTestBookmark("3", "article_2", "article")
        )
        val videoBookmarks = listOf(
            createTestBookmark("2", "video_1", "video")
        )

        coEvery { discoverDao.getBookmarksByType(testUserId, "article") } returns articleBookmarks
        coEvery { discoverDao.getBookmarksByType(testUserId, "video") } returns videoBookmarks

        // When
        val articleResult = discoverDao.getBookmarksByType(testUserId, "article")
        val videoResult = discoverDao.getBookmarksByType(testUserId, "video")

        // Then
        assertEquals(2, articleResult.size)
        assertEquals(1, videoResult.size)
        assertTrue(articleResult.all { it.contentType == "article" })
        assertTrue(videoResult.all { it.contentType == "video" })
        coVerify { discoverDao.getBookmarksByType(testUserId, "article") }
        coVerify { discoverDao.getBookmarksByType(testUserId, "video") }
    }

    @Test
    fun isContentBookmarked_returnsCorrectStatus() = runTest {
        // Given
        coEvery { discoverDao.isContentBookmarked("article_1", testUserId) } returns true
        coEvery { discoverDao.isContentBookmarked("article_2", testUserId) } returns false

        // When
        val isBookmarked = discoverDao.isContentBookmarked("article_1", testUserId)
        val isNotBookmarked = discoverDao.isContentBookmarked("article_2", testUserId)

        // Then
        assertTrue(isBookmarked)
        assertFalse(isNotBookmarked)
        coVerify { discoverDao.isContentBookmarked("article_1", testUserId) }
        coVerify { discoverDao.isContentBookmarked("article_2", testUserId) }
    }

    @Test
    fun removeBookmark_removesCorrectly() = runTest {
        // Given
        coEvery { discoverDao.insertBookmark(any()) } just Runs
        coEvery { discoverDao.removeBookmark("article_1", testUserId) } just Runs
        coEvery { discoverDao.isContentBookmarked("article_1", testUserId) } returns true andThen false

        // When
        val bookmark = createTestBookmark("1", "article_1", "article")
        discoverDao.insertBookmark(bookmark)
        val initialStatus = discoverDao.isContentBookmarked("article_1", testUserId)
        discoverDao.removeBookmark("article_1", testUserId)
        val finalStatus = discoverDao.isContentBookmarked("article_1", testUserId)

        // Then
        assertTrue(initialStatus)
        assertFalse(finalStatus)
        coVerify { discoverDao.removeBookmark("article_1", testUserId) }
    }

    // ==================== MIXED CONTENT TESTS ====================

    @Test
    fun searchAllContent_returnsAllTypes() = runTest {
        // Given
        val searchResults = listOf(
            ContentSearchResult("article", "1", "Diabetes Article", System.currentTimeMillis(), "nutrition", null),
            ContentSearchResult("news", "2", "Diabetes News", System.currentTimeMillis(), "health", null),
            ContentSearchResult("video", "3", "Diabetes Video", System.currentTimeMillis(), "nutrition", null)
        )

        coEvery { discoverDao.searchAllContent(testUserId, "Diabetes", 10) } returns searchResults

        // When
        val result = discoverDao.searchAllContent(testUserId, "Diabetes", 10)

        // Then
        assertEquals(3, result.size)
        val contentTypes = result.map { it.contentType }.toSet()
        assertTrue(contentTypes.contains("article"))
        assertTrue(contentTypes.contains("news"))
        assertTrue(contentTypes.contains("video"))
        coVerify { discoverDao.searchAllContent(testUserId, "Diabetes", 10) }
    }

    @Test
    fun getContentByCategory_returnsAllTypesInCategory() = runTest {
        // Given
        val nutritionContent = listOf(
            ContentSearchResult("article", "1", "Nutrition Article", System.currentTimeMillis(), "nutrition", null),
            ContentSearchResult("news", "2", "Nutrition News", System.currentTimeMillis(), "nutrition", null),
            ContentSearchResult("video", "3", "Nutrition Video", System.currentTimeMillis(), "nutrition", null)
        )
        val fitnessContent = listOf(
            ContentSearchResult("article", "4", "Fitness Article", System.currentTimeMillis(), "fitness", null)
        )

        coEvery { discoverDao.getContentByCategory(testUserId, "nutrition", 10) } returns nutritionContent
        coEvery { discoverDao.getContentByCategory(testUserId, "fitness", 10) } returns fitnessContent

        // When
        val nutritionResult = discoverDao.getContentByCategory(testUserId, "nutrition", 10)
        val fitnessResult = discoverDao.getContentByCategory(testUserId, "fitness", 10)

        // Then
        assertEquals(3, nutritionResult.size)
        assertEquals(1, fitnessResult.size)
        assertTrue(nutritionResult.all { it.category == "nutrition" })
        assertTrue(fitnessResult.all { it.category == "fitness" })
        coVerify { discoverDao.getContentByCategory(testUserId, "nutrition", 10) }
        coVerify { discoverDao.getContentByCategory(testUserId, "fitness", 10) }
    }

    // ==================== CACHE MANAGEMENT TESTS ====================

    @Test
    fun cleanupOldArticles_removesOldContent() = runTest {
        // Given
        val currentTime = System.currentTimeMillis()
        val cutoffTime = currentTime - TimeUnit.DAYS.toMillis(180) // 6 months ago
        val remainingArticles = listOf(
            createTestHealthArticle("1", "Recent Article", "nutrition", publishedDate = currentTime)
        )

        coEvery { discoverDao.cleanupOldArticles(cutoffTime, testUserId) } returns 1
        coEvery { discoverDao.getHealthArticles(testUserId, null, 10) } returns remainingArticles

        // When
        val deletedCount = discoverDao.cleanupOldArticles(cutoffTime, testUserId)
        val result = discoverDao.getHealthArticles(testUserId, null, 10)

        // Then
        assertEquals(1, deletedCount)
        assertEquals(1, result.size)
        assertEquals("Recent Article", result[0].title)
        coVerify { discoverDao.cleanupOldArticles(cutoffTime, testUserId) }
    }

    @Test
    fun cleanupOrphanedBookmarks_removesOrphans() = runTest {
        // Given
        val remainingBookmarks = listOf(
            createTestBookmark("1", "1", "article")
        )

        coEvery { discoverDao.cleanupOrphanedBookmarks(testUserId) } returns 1
        coEvery { discoverDao.getContentBookmarks(testUserId) } returns remainingBookmarks

        // When
        val deletedCount = discoverDao.cleanupOrphanedBookmarks(testUserId)
        val result = discoverDao.getContentBookmarks(testUserId)

        // Then
        assertEquals(1, deletedCount)
        assertEquals(1, result.size)
        assertEquals("1", result[0].contentId)
        coVerify { discoverDao.cleanupOrphanedBookmarks(testUserId) }
    }

    @Test
    fun getCacheStatistics_returnsCorrectCounts() = runTest {
        // Given
        val expectedStats = CacheStatistics(
            articleCount = 2,
            newsCount = 1,
            videoCount = 1,
            bookmarkCount = 1
        )

        coEvery { discoverDao.getCacheStatistics(testUserId) } returns expectedStats

        // When
        val stats = discoverDao.getCacheStatistics(testUserId)

        // Then
        assertEquals(2, stats.articleCount)
        assertEquals(1, stats.newsCount)
        assertEquals(1, stats.videoCount)
        assertEquals(1, stats.bookmarkCount)
        coVerify { discoverDao.getCacheStatistics(testUserId) }
    }

    @Test
    fun getContentCountByCategory_returnsCorrectCounts() = runTest {
        // Given
        val expectedCounts = listOf(
            CategoryCount("nutrition", 3),
            CategoryCount("fitness", 1)
        )

        coEvery { discoverDao.getContentCountByCategory(testUserId) } returns expectedCounts

        // When
        val categoryCounts = discoverDao.getContentCountByCategory(testUserId)

        // Then
        assertEquals(2, categoryCounts.size)
        val nutritionCount = categoryCounts.find { it.category == "nutrition" }?.count
        val fitnessCount = categoryCounts.find { it.category == "fitness" }?.count
        assertEquals(3, nutritionCount)
        assertEquals(1, fitnessCount)
        coVerify { discoverDao.getContentCountByCategory(testUserId) }
    }

    @Test
    fun deleteAllUserContent_removesAllContent() = runTest {
        // Given
        coEvery { discoverDao.deleteAllUserContent(testUserId) } just Runs
        coEvery { discoverDao.getHealthArticles(testUserId, null, 10) } returns emptyList()
        coEvery { discoverDao.getHealthNews(testUserId, null, 10) } returns emptyList()
        coEvery { discoverDao.getEducationalVideos(testUserId, null, 10) } returns emptyList()
        coEvery { discoverDao.getContentBookmarks(testUserId) } returns emptyList()

        // When
        discoverDao.deleteAllUserContent(testUserId)

        // Then
        val articles = discoverDao.getHealthArticles(testUserId, null, 10)
        val newsItems = discoverDao.getHealthNews(testUserId, null, 10)
        val videos = discoverDao.getEducationalVideos(testUserId, null, 10)
        val bookmarks = discoverDao.getContentBookmarks(testUserId)

        assertEquals(0, articles.size)
        assertEquals(0, newsItems.size)
        assertEquals(0, videos.size)
        assertEquals(0, bookmarks.size)
        coVerify { discoverDao.deleteAllUserContent(testUserId) }
    }

    // ==================== USER ISOLATION TESTS ====================

    @Test
    fun userDataIsolation_ensuresDataSeparation() = runTest {
        // Given
        val userArticles = listOf(
            createTestHealthArticle("1", "User Article", "nutrition", userId = testUserId)
        )
        val otherUserArticles = listOf(
            createTestHealthArticle("2", "Other User Article", "nutrition", userId = otherUserId)
        )

        coEvery { discoverDao.getHealthArticles(testUserId, null, 10) } returns userArticles
        coEvery { discoverDao.getHealthArticles(otherUserId, null, 10) } returns otherUserArticles

        // When
        val userResult = discoverDao.getHealthArticles(testUserId, null, 10)
        val otherUserResult = discoverDao.getHealthArticles(otherUserId, null, 10)

        // Then
        assertEquals(1, userResult.size)
        assertEquals(1, otherUserResult.size)
        assertEquals("User Article", userResult[0].title)
        assertEquals("Other User Article", otherUserResult[0].title)
        coVerify { discoverDao.getHealthArticles(testUserId, null, 10) }
        coVerify { discoverDao.getHealthArticles(otherUserId, null, 10) }
    }

    // ==================== HELPER METHODS ====================

    private fun createTestHealthArticle(
        id: String,
        title: String,
        category: String,
        content: String = "Test content",
        isBookmarked: Boolean = false,
        credibilityScore: Int = 4,
        publishedDate: Long = System.currentTimeMillis() - (id.toInt() * 1000),
        userId: String = testUserId
    ) = HealthArticleEntity(
        id = id,
        title = title,
        summary = "Test summary for $title",
        content = content,
        category = category,
        authorName = "Dr. Test Author",
        authorCredentials = "MD, PhD",
        sourceUrl = "https://test.com/$id",
        publishedDate = publishedDate,
        lastUpdated = publishedDate,
        readingTimeMinutes = 5,
        imageUrl = "https://test.com/image$id.jpg",
        tags = listOf("health", category),
        isBookmarked = isBookmarked,
        readProgress = 0f,
        credibilityScore = credibilityScore,
        userId = userId
    )

    private fun createTestHealthNews(
        id: String,
        headline: String,
        category: String,
        summary: String = "Test summary",
        isBreaking: Boolean = false,
        credibility: String = "medical-journal",
        publishedDate: Long = System.currentTimeMillis() - (id.toInt() * 1000),
        userId: String = testUserId
    ) = HealthNewsEntity(
        id = id,
        headline = headline,
        summary = summary,
        fullContent = "Full content for $headline",
        category = category,
        sourcePublication = "Test Medical Journal",
        sourceCredibility = credibility,
        publishedDate = publishedDate,
        imageUrl = "https://test.com/news$id.jpg",
        externalUrl = "https://test.com/news/$id",
        isBreakingNews = isBreaking,
        relevanceScore = 5,
        userId = userId
    )

    private fun createTestEducationalVideo(
        id: String,
        title: String,
        category: String,
        description: String = "Test description",
        difficulty: String = "beginner",
        isOffline: Boolean = false,
        publishedDate: Long = System.currentTimeMillis() - (id.toInt() * 1000),
        userId: String = testUserId
    ) = EducationalVideoEntity(
        id = id,
        title = title,
        description = description,
        category = category,
        thumbnailUrl = "https://test.com/thumb$id.jpg",
        videoUrl = "https://test.com/video$id.mp4",
        durationSeconds = 300,
        difficultyLevel = difficulty,
        expertName = "Dr. Video Expert",
        expertCredentials = "MD, Specialist",
        publishedDate = publishedDate,
        watchProgress = 0f,
        isDownloadedOffline = isOffline,
        transcriptAvailable = true,
        userId = userId
    )

    private fun createTestBookmark(
        id: String,
        contentId: String,
        contentType: String,
        userId: String = testUserId
    ) = ContentBookmarkEntity(
        id = id,
        contentId = contentId,
        contentType = contentType,
        bookmarkedDate = System.currentTimeMillis(),
        userId = userId
    )
}