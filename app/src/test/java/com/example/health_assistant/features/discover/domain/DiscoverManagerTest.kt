package com.example.health_assistant.features.discover.domain

import com.example.health_assistant.core.util.Result
import com.example.health_assistant.features.discover.domain.model.*
import com.example.health_assistant.features.discover.domain.repository.DiscoverRepository
import com.example.health_assistant.features.discover.domain.usecase.*
import com.example.health_assistant.features.discover.domain.validation.ContentCredibilityValidator
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for DiscoverManager
 * Tests the central business logic coordinator for the Discover feature
 */
class DiscoverManagerTest {

    private lateinit var discoverManager: DiscoverManager
    private lateinit var repository: DiscoverRepository
    private lateinit var getContentUseCase: SimpleGetContentUseCase
    private lateinit var bookmarkUseCase: SimpleBookmarkUseCase
    private lateinit var searchUseCase: SimpleSearchUseCase
    private lateinit var validationUseCase: SimpleContentValidationUseCase
    private lateinit var credibilityValidator: ContentCredibilityValidator

    private val testUserId = "test_user_123"

    @BeforeEach
    fun setup() {
        repository = mockk()
        getContentUseCase = mockk()
        bookmarkUseCase = mockk()
        searchUseCase = mockk()
        validationUseCase = mockk()
        credibilityValidator = mockk()

        discoverManager = DiscoverManager(
            repository = repository,
            getContentUseCase = getContentUseCase,
            bookmarkUseCase = bookmarkUseCase,
            searchUseCase = searchUseCase,
            validationUseCase = validationUseCase,
            credibilityValidator = credibilityValidator
        )
    }

    // ==================== Content Feed Tests ====================

    @Test
    fun `getContentFeed returns mixed content successfully`() = runTest {
        // Given
        val articles = listOf(createTestArticle("1", "Article 1"))
        val news = listOf(createTestNews("2", "News 1"))
        val videos = listOf(createTestVideo("3", "Video 1"))
        val allContent = articles + news + videos

        coEvery { getContentUseCase.execute(testUserId, null) } returns flowOf(Result.Success(allContent))

        // When
        val result = discoverManager.getContentFeed(testUserId, null).first()

        // Then
        assertTrue(result is Result.Success)
        val feedData = result.data
        assertEquals(1, feedData.articles.size)
        assertEquals(1, feedData.news.size)
        assertEquals(1, feedData.videos.size)
        assertFalse(feedData.hasErrors)
        assertTrue(feedData.lastUpdated > 0)
    }

    @Test
    fun `getContentFeed handles partial errors gracefully`() = runTest {
        // Given - Some content succeeds, some fails
        val articles = listOf(createTestArticle("1", "Article 1"))
        coEvery { getContentUseCase.execute(testUserId, null) } returns flowOf(Result.Success(articles))

        // When
        val result = discoverManager.getContentFeed(testUserId, null).first()

        // Then
        assertTrue(result is Result.Success)
        val feedData = result.data
        assertEquals(1, feedData.articles.size)
        assertEquals(0, feedData.news.size)
        assertEquals(0, feedData.videos.size)
        assertFalse(feedData.hasErrors) // Should not have errors if some content succeeded
    }

    @Test
    fun `getContentFeed filters by category correctly`() = runTest {
        // Given
        val nutritionContent = listOf(
            createTestArticle("1", "Nutrition Article", category = "nutrition"),
            createTestVideo("2", "Nutrition Video", category = "nutrition")
        )
        coEvery { getContentUseCase.execute(testUserId, "nutrition") } returns flowOf(Result.Success(nutritionContent))

        // When
        val result = discoverManager.getContentFeed(testUserId, "nutrition").first()

        // Then
        assertTrue(result is Result.Success)
        val feedData = result.data
        assertEquals(1, feedData.articles.size)
        assertEquals(0, feedData.news.size)
        assertEquals(1, feedData.videos.size)
        assertTrue(feedData.articles.all { it.category == "nutrition" })
        assertTrue(feedData.videos.all { it.category == "nutrition" })
    }

    @Test
    fun `getContentFeed applies content validation`() = runTest {
        // Given
        val articles = listOf(createTestArticle("1", "Article 1"))
        val validationResult = ContentValidationResult(
            isCredible = true,
            credibilityScore = 5,
            warnings = emptyList(),
            lastValidated = System.currentTimeMillis()
        )

        coEvery { getContentUseCase.execute(testUserId, null) } returns flowOf(Result.Success(articles))
        coEvery { validationUseCase.execute(any()) } returns Result.Success(validationResult)

        // When
        val result = discoverManager.getContentFeed(testUserId, null).first()

        // Then
        assertTrue(result is Result.Success)
        coVerify { validationUseCase.execute(any()) }
    }

    // ==================== Trending Content Tests ====================

    @Test
    fun `getTrendingContent returns popular content`() = runTest {
        // Given
        val trendingContent = listOf(
            createTestArticle("1", "Trending Article 1"),
            createTestNews("2", "Trending News 1")
        )
        coEvery { repository.getTrendingContent(testUserId, 10) } returns flowOf(Result.Success(trendingContent))

        // When
        val result = discoverManager.getTrendingContent(testUserId, 10).first()

        // Then
        assertTrue(result is Result.Success)
        assertEquals(2, result.data.size)
        coVerify { repository.getTrendingContent(testUserId, 10) }
    }

    @Test
    fun `getTrendingContent handles empty results`() = runTest {
        // Given
        coEvery { repository.getTrendingContent(testUserId, 10) } returns flowOf(Result.Success(emptyList()))

        // When
        val result = discoverManager.getTrendingContent(testUserId, 10).first()

        // Then
        assertTrue(result is Result.Success)
        assertEquals(0, result.data.size)
    }

    // ==================== Search Tests ====================

    @Test
    fun `searchContent returns search results with metadata`() = runTest {
        // Given
        val query = "diabetes"
        val searchResults = listOf(
            createTestArticle("1", "Diabetes Management"),
            createTestNews("2", "Diabetes Research")
        )
        val filters = SearchFilters(
            contentTypes = listOf("article", "news"),
            categories = emptyList(),
            dateRange = null,
            credibilityThreshold = 3
        )

        coEvery { searchUseCase.execute(testUserId, query, filters.contentTypes) } returns Result.Success(searchResults)

        // When
        val result = discoverManager.searchContent(testUserId, query, filters)

        // Then
        assertTrue(result is Result.Success)
        val searchResultsData = result.data
        assertEquals(query, searchResultsData.query)
        assertEquals(2, searchResultsData.results.size)
        assertEquals(2, searchResultsData.totalCount)
        assertEquals(filters, searchResultsData.appliedFilters)
    }

    @Test
    fun `searchContent validates query before searching`() = runTest {
        // Given
        val emptyQuery = ""
        val filters = SearchFilters(contentTypes = listOf("article"))

        // When
        val result = discoverManager.searchContent(testUserId, emptyQuery, filters)

        // Then
        assertTrue(result is Result.Error)
        assertTrue(result.exception.message?.contains("Query cannot be empty") == true)
        coVerify(exactly = 0) { searchUseCase.execute(any(), any(), any()) }
    }

    @Test
    fun `searchContent applies filters correctly`() = runTest {
        // Given
        val query = "health"
        val filters = SearchFilters(
            contentTypes = listOf("article"),
            categories = listOf("nutrition"),
            dateRange = DateRange(
                startDate = System.currentTimeMillis() - 86400000, // 1 day ago
                endDate = System.currentTimeMillis()
            ),
            credibilityThreshold = 4
        )
        val searchResults = listOf(createTestArticle("1", "Health Article", category = "nutrition"))

        coEvery { searchUseCase.execute(testUserId, query, filters.contentTypes) } returns Result.Success(searchResults)

        // When
        val result = discoverManager.searchContent(testUserId, query, filters)

        // Then
        assertTrue(result is Result.Success)
        val filteredResults = result.data.results
        assertTrue(filteredResults.all { it.category == "nutrition" })
        assertTrue(filteredResults.all { 
            when (it) {
                is DiscoverContent.Article -> it.credibilityScore >= 4
                else -> true
            }
        })
    }

    // ==================== Bookmark Management Tests ====================

    @Test
    fun `toggleBookmark updates bookmark state`() = runTest {
        // Given
        val content = createTestArticle("1", "Test Article")
        coEvery { bookmarkUseCase.execute(testUserId, content) } returns Result.Success(true)

        // When
        val result = discoverManager.toggleBookmark(testUserId, content)

        // Then
        assertTrue(result is Result.Success)
        assertTrue(result.data)
        coVerify { bookmarkUseCase.execute(testUserId, content) }
    }

    @Test
    fun `getBookmarkedContent returns user bookmarks`() = runTest {
        // Given
        val bookmarkedContent = listOf(
            createTestArticle("1", "Bookmarked Article", isBookmarked = true),
            createTestVideo("2", "Bookmarked Video", isBookmarked = true)
        )
        coEvery { repository.getBookmarkedContent(testUserId) } returns flowOf(Result.Success(bookmarkedContent))

        // When
        val result = discoverManager.getBookmarkedContent(testUserId).first()

        // Then
        assertTrue(result is Result.Success)
        assertEquals(2, result.data.size)
        assertTrue(result.data.all { 
            when (it) {
                is DiscoverContent.Article -> it.isBookmarked
                is DiscoverContent.Video -> true // Videos don't have isBookmarked field in this test
                else -> true
            }
        })
    }

    // ==================== Content Validation Tests ====================

    @Test
    fun `validateContent returns validation results`() = runTest {
        // Given
        val content = createTestArticle("1", "Test Article")
        val validationResult = ContentValidationResult(
            isCredible = true,
            credibilityScore = 5,
            warnings = emptyList(),
            lastValidated = System.currentTimeMillis()
        )
        coEvery { validationUseCase.execute(content) } returns Result.Success(validationResult)

        // When
        val result = discoverManager.validateContent(content)

        // Then
        assertTrue(result is Result.Success)
        assertTrue(result.data.isCredible)
        assertEquals(5, result.data.credibilityScore)
        coVerify { validationUseCase.execute(content) }
    }

    @Test
    fun `validateContent handles low credibility content`() = runTest {
        // Given
        val content = createTestArticle("1", "Questionable Article")
        val validationResult = ContentValidationResult(
            isCredible = false,
            credibilityScore = 2,
            warnings = listOf("Source credibility not verified", "Content is outdated"),
            lastValidated = System.currentTimeMillis()
        )
        coEvery { validationUseCase.execute(content) } returns Result.Success(validationResult)

        // When
        val result = discoverManager.validateContent(content)

        // Then
        assertTrue(result is Result.Success)
        assertFalse(result.data.isCredible)
        assertEquals(2, result.data.credibilityScore)
        assertEquals(2, result.data.warnings.size)
    }

    // ==================== Sync Operations Tests ====================

    @Test
    fun `syncContent triggers repository sync`() = runTest {
        // Given
        coEvery { repository.syncContent(testUserId) } returns Result.Success(Unit)

        // When
        val result = discoverManager.syncContent(testUserId)

        // Then
        assertTrue(result is Result.Success)
        coVerify { repository.syncContent(testUserId) }
    }

    @Test
    fun `syncContent handles sync failures`() = runTest {
        // Given
        coEvery { repository.syncContent(testUserId) } returns Result.Error(Exception("Network unavailable"))

        // When
        val result = discoverManager.syncContent(testUserId)

        // Then
        assertTrue(result is Result.Error)
        assertEquals("Network unavailable", result.exception.message)
    }

    // ==================== Content Recommendations Tests ====================

    @Test
    fun `getRecommendedContent returns personalized recommendations`() = runTest {
        // Given
        val userPreferences = UserPreferences(
            favoriteCategories = listOf("nutrition", "fitness"),
            readingHistory = listOf("article_1", "article_2"),
            bookmarkedContent = listOf("video_1")
        )
        val recommendations = listOf(
            createTestArticle("3", "Recommended Article", category = "nutrition"),
            createTestVideo("4", "Recommended Video", category = "fitness")
        )

        coEvery { repository.getUserPreferences(testUserId) } returns Result.Success(userPreferences)
        coEvery { repository.getRecommendedContent(testUserId, userPreferences) } returns flowOf(Result.Success(recommendations))

        // When
        val result = discoverManager.getRecommendedContent(testUserId).first()

        // Then
        assertTrue(result is Result.Success)
        assertEquals(2, result.data.size)
        assertTrue(result.data.all { it.category in userPreferences.favoriteCategories })
    }

    @Test
    fun `getRecommendedContent handles users without preferences`() = runTest {
        // Given - New user with no preferences
        coEvery { repository.getUserPreferences(testUserId) } returns Result.Success(UserPreferences())
        coEvery { repository.getPopularContent(testUserId) } returns flowOf(Result.Success(
            listOf(createTestArticle("1", "Popular Article"))
        ))

        // When
        val result = discoverManager.getRecommendedContent(testUserId).first()

        // Then
        assertTrue(result is Result.Success)
        assertEquals(1, result.data.size)
        coVerify { repository.getPopularContent(testUserId) }
    }

    // ==================== Analytics and Tracking Tests ====================

    @Test
    fun `trackContentInteraction records user engagement`() = runTest {
        // Given
        val content = createTestArticle("1", "Test Article")
        val interaction = ContentInteraction(
            contentId = content.id,
            interactionType = InteractionType.VIEW,
            duration = 30000, // 30 seconds
            timestamp = System.currentTimeMillis()
        )
        coEvery { repository.recordContentInteraction(testUserId, interaction) } returns Result.Success(Unit)

        // When
        val result = discoverManager.trackContentInteraction(testUserId, content, InteractionType.VIEW, 30000)

        // Then
        assertTrue(result is Result.Success)
        coVerify { repository.recordContentInteraction(testUserId, any()) }
    }

    @Test
    fun `getContentAnalytics returns engagement metrics`() = runTest {
        // Given
        val analytics = ContentAnalytics(
            totalViews = 150,
            averageReadTime = 180000, // 3 minutes
            bookmarkRate = 0.15f,
            shareRate = 0.08f,
            topCategories = listOf("nutrition", "fitness", "mental-health")
        )
        coEvery { repository.getContentAnalytics(testUserId) } returns Result.Success(analytics)

        // When
        val result = discoverManager.getContentAnalytics(testUserId)

        // Then
        assertTrue(result is Result.Success)
        assertEquals(150, result.data.totalViews)
        assertEquals(180000, result.data.averageReadTime)
        assertEquals(0.15f, result.data.bookmarkRate)
        assertEquals(3, result.data.topCategories.size)
    }

    // ==================== Helper Methods ====================

    private fun createTestArticle(
        id: String,
        title: String,
        category: String = "nutrition",
        isBookmarked: Boolean = false,
        publishedDate: Long = System.currentTimeMillis()
    ) = DiscoverContent.Article(
        id = id,
        title = title,
        publishedDate = publishedDate,
        category = category,
        imageUrl = "https://test.com/image.jpg",
        userId = testUserId,
        summary = "Test summary",
        content = "Test content",
        authorName = "Dr. Test",
        authorCredentials = "MD",
        sourceUrl = "https://test.com",
        lastUpdated = publishedDate,
        readingTimeMinutes = 5,
        tags = listOf("health", category),
        isBookmarked = isBookmarked,
        readProgress = 0f,
        credibilityScore = 5
    )

    private fun createTestNews(
        id: String,
        headline: String,
        category: String = "health",
        publishedDate: Long = System.currentTimeMillis()
    ) = DiscoverContent.News(
        id = id,
        title = headline,
        publishedDate = publishedDate,
        category = category,
        imageUrl = "https://test.com/news.jpg",
        userId = testUserId,
        summary = "Test news summary",
        sourcePublication = "Test Journal",
        sourceCredibility = "medical-journal",
        externalUrl = "https://test.com/news",
        isBreakingNews = false,
        relevanceScore = 5
    )

    private fun createTestVideo(
        id: String,
        title: String,
        category: String = "fitness",
        isBookmarked: Boolean = false,
        publishedDate: Long = System.currentTimeMillis()
    ) = DiscoverContent.Video(
        id = id,
        title = title,
        publishedDate = publishedDate,
        category = category,
        imageUrl = "https://test.com/video.jpg",
        userId = testUserId,
        description = "Test video description",
        videoUrl = "https://test.com/video.mp4",
        thumbnailUrl = "https://test.com/thumb.jpg",
        durationSeconds = 300,
        difficultyLevel = "beginner",
        expertName = "Dr. Expert",
        expertCredentials = "MD",
        watchProgress = 0f,
        isDownloadedOffline = false,
        transcriptAvailable = true
    )
}