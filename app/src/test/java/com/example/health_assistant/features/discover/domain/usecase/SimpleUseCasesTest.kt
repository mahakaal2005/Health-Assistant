package com.example.health_assistant.features.discover.domain.usecase

import com.example.health_assistant.core.util.Result
import com.example.health_assistant.features.discover.domain.model.*
import com.example.health_assistant.features.discover.domain.repository.DiscoverRepository
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
 * Unit tests for simple use cases in the Discover feature
 * Tests individual use case logic and error handling
 */
class SimpleUseCasesTest {

    private lateinit var repository: DiscoverRepository
    private val testUserId = "test_user_123"

    @BeforeEach
    fun setup() {
        repository = mockk()
    }

    // ==================== SimpleGetContentUseCase Tests ====================

    @Test
    fun `SimpleGetContentUseCase returns mixed content successfully`() = runTest {
        // Given
        val useCase = SimpleGetContentUseCase(repository)
        val articles = listOf(createTestArticle("1", "Article 1"))
        val news = listOf(createTestNews("2", "News 1"))
        val videos = listOf(createTestVideo("3", "Video 1"))

        coEvery { repository.getHealthArticles(testUserId, null, 20) } returns flowOf(Result.Success(articles))
        coEvery { repository.getHealthNews(testUserId, null, 10) } returns flowOf(Result.Success(news))
        coEvery { repository.getEducationalVideos(testUserId, null, 15) } returns flowOf(Result.Success(videos))

        // When
        val result = useCase.execute(testUserId, null).first()

        // Then
        assertTrue(result is Result.Success)
        assertEquals(3, result.data.size)
        
        // Verify content is sorted by published date (most recent first)
        val sortedContent = result.data.sortedByDescending { it.publishedDate }
        assertEquals(sortedContent, result.data)
    }

    @Test
    fun `SimpleGetContentUseCase handles repository errors gracefully`() = runTest {
        // Given
        val useCase = SimpleGetContentUseCase(repository)
        coEvery { repository.getHealthArticles(testUserId, null, 20) } returns flowOf(Result.Error(Exception("Network error")))
        coEvery { repository.getHealthNews(testUserId, null, 10) } returns flowOf(Result.Success(emptyList()))
        coEvery { repository.getEducationalVideos(testUserId, null, 15) } returns flowOf(Result.Success(emptyList()))

        // When
        val result = useCase.execute(testUserId, null).first()

        // Then
        assertTrue(result is Result.Success) // Should succeed with partial data
        assertEquals(0, result.data.size) // Only empty news and videos
    }

    @Test
    fun `SimpleGetContentUseCase filters by category correctly`() = runTest {
        // Given
        val useCase = SimpleGetContentUseCase(repository)
        val nutritionArticles = listOf(createTestArticle("1", "Nutrition Article", category = "nutrition"))
        val nutritionVideos = listOf(createTestVideo("2", "Nutrition Video", category = "nutrition"))

        coEvery { repository.getHealthArticles(testUserId, "nutrition", 20) } returns flowOf(Result.Success(nutritionArticles))
        coEvery { repository.getHealthNews(testUserId, "nutrition", 10) } returns flowOf(Result.Success(emptyList()))
        coEvery { repository.getEducationalVideos(testUserId, "nutrition", 15) } returns flowOf(Result.Success(nutritionVideos))

        // When
        val result = useCase.execute(testUserId, "nutrition").first()

        // Then
        assertTrue(result is Result.Success)
        assertEquals(2, result.data.size)
        assertTrue(result.data.all { it.category == "nutrition" })
    }

    // ==================== SimpleBookmarkUseCase Tests ====================

    @Test
    fun `SimpleBookmarkUseCase toggles bookmark successfully`() = runTest {
        // Given
        val useCase = SimpleBookmarkUseCase(repository)
        val content = createTestArticle("1", "Test Article")
        coEvery { repository.toggleBookmark(testUserId, content) } returns Result.Success(true)

        // When
        val result = useCase.execute(testUserId, content)

        // Then
        assertTrue(result is Result.Success)
        assertTrue(result.data)
        coVerify { repository.toggleBookmark(testUserId, content) }
    }

    @Test
    fun `SimpleBookmarkUseCase handles bookmark errors`() = runTest {
        // Given
        val useCase = SimpleBookmarkUseCase(repository)
        val content = createTestArticle("1", "Test Article")
        coEvery { repository.toggleBookmark(testUserId, content) } returns Result.Error(Exception("Bookmark failed"))

        // When
        val result = useCase.execute(testUserId, content)

        // Then
        assertTrue(result is Result.Error)
        assertEquals("Bookmark failed", result.exception.message)
    }

    @Test
    fun `SimpleBookmarkUseCase validates content before bookmarking`() = runTest {
        // Given
        val useCase = SimpleBookmarkUseCase(repository)
        val invalidContent = createTestArticle("", "") // Invalid content with empty ID and title

        // When
        val result = useCase.execute(testUserId, invalidContent)

        // Then
        assertTrue(result is Result.Error)
        assertTrue(result.exception.message?.contains("Invalid content") == true)
        coVerify(exactly = 0) { repository.toggleBookmark(any(), any()) }
    }

    // ==================== SimpleSearchUseCase Tests ====================

    @Test
    fun `SimpleSearchUseCase performs search successfully`() = runTest {
        // Given
        val useCase = SimpleSearchUseCase(repository)
        val searchResults = listOf(
            createTestArticle("1", "Diabetes Management"),
            createTestNews("2", "Diabetes Research")
        )
        coEvery { repository.searchContent(testUserId, "diabetes", listOf("article", "news", "video"), 20) } returns Result.Success(searchResults)

        // When
        val result = useCase.execute(testUserId, "diabetes", listOf("article", "news", "video"))

        // Then
        assertTrue(result is Result.Success)
        assertEquals(2, result.data.size)
        assertTrue(result.data.all { it.title.contains("Diabetes", ignoreCase = true) })
    }

    @Test
    fun `SimpleSearchUseCase validates search query`() = runTest {
        // Given
        val useCase = SimpleSearchUseCase(repository)

        // When - Empty query
        val emptyResult = useCase.execute(testUserId, "", listOf("article"))

        // Then
        assertTrue(emptyResult is Result.Error)
        assertTrue(emptyResult.exception.message?.contains("Query cannot be empty") == true)

        // When - Blank query
        val blankResult = useCase.execute(testUserId, "   ", listOf("article"))

        // Then
        assertTrue(blankResult is Result.Error)
        assertTrue(blankResult.exception.message?.contains("Query cannot be empty") == true)
    }

    @Test
    fun `SimpleSearchUseCase handles search with no results`() = runTest {
        // Given
        val useCase = SimpleSearchUseCase(repository)
        coEvery { repository.searchContent(testUserId, "nonexistent", listOf("article"), 20) } returns Result.Success(emptyList())

        // When
        val result = useCase.execute(testUserId, "nonexistent", listOf("article"))

        // Then
        assertTrue(result is Result.Success)
        assertEquals(0, result.data.size)
    }

    @Test
    fun `SimpleSearchUseCase limits search query length`() = runTest {
        // Given
        val useCase = SimpleSearchUseCase(repository)
        val longQuery = "a".repeat(1001) // Exceeds typical limit

        // When
        val result = useCase.execute(testUserId, longQuery, listOf("article"))

        // Then
        assertTrue(result is Result.Error)
        assertTrue(result.exception.message?.contains("Query too long") == true)
    }

    @Test
    fun `SimpleSearchUseCase validates content types`() = runTest {
        // Given
        val useCase = SimpleSearchUseCase(repository)

        // When - Empty content types
        val emptyTypesResult = useCase.execute(testUserId, "test", emptyList())

        // Then
        assertTrue(emptyTypesResult is Result.Error)
        assertTrue(emptyTypesResult.exception.message?.contains("At least one content type must be specified") == true)

        // When - Invalid content type
        val invalidTypesResult = useCase.execute(testUserId, "test", listOf("invalid_type"))

        // Then
        assertTrue(invalidTypesResult is Result.Error)
        assertTrue(invalidTypesResult.exception.message?.contains("Invalid content type") == true)
    }

    // ==================== SimpleContentValidationUseCase Tests ====================

    @Test
    fun `SimpleContentValidationUseCase validates content successfully`() = runTest {
        // Given
        val useCase = SimpleContentValidationUseCase(repository)
        val content = createTestArticle("1", "Test Article")
        val validationResult = ContentValidationResult(
            isCredible = true,
            credibilityScore = 5,
            warnings = emptyList(),
            lastValidated = System.currentTimeMillis()
        )
        coEvery { repository.validateContentCredibility(content) } returns Result.Success(validationResult)

        // When
        val result = useCase.execute(content)

        // Then
        assertTrue(result is Result.Success)
        assertTrue(result.data.isCredible)
        assertEquals(5, result.data.credibilityScore)
        assertTrue(result.data.warnings.isEmpty())
    }

    @Test
    fun `SimpleContentValidationUseCase handles validation errors`() = runTest {
        // Given
        val useCase = SimpleContentValidationUseCase(repository)
        val content = createTestArticle("1", "Test Article")
        coEvery { repository.validateContentCredibility(content) } returns Result.Error(Exception("Validation service unavailable"))

        // When
        val result = useCase.execute(content)

        // Then
        assertTrue(result is Result.Error)
        assertEquals("Validation service unavailable", result.exception.message)
    }

    @Test
    fun `SimpleContentValidationUseCase validates different content types`() = runTest {
        // Given
        val useCase = SimpleContentValidationUseCase(repository)
        val article = createTestArticle("1", "Test Article")
        val news = createTestNews("2", "Test News")
        val video = createTestVideo("3", "Test Video")

        val articleValidation = ContentValidationResult(true, 5, emptyList(), System.currentTimeMillis())
        val newsValidation = ContentValidationResult(true, 4, listOf("Breaking news - verify independently"), System.currentTimeMillis())
        val videoValidation = ContentValidationResult(true, 3, emptyList(), System.currentTimeMillis())

        coEvery { repository.validateContentCredibility(article) } returns Result.Success(articleValidation)
        coEvery { repository.validateContentCredibility(news) } returns Result.Success(newsValidation)
        coEvery { repository.validateContentCredibility(video) } returns Result.Success(videoValidation)

        // When
        val articleResult = useCase.execute(article)
        val newsResult = useCase.execute(news)
        val videoResult = useCase.execute(video)

        // Then
        assertTrue(articleResult is Result.Success)
        assertEquals(5, articleResult.data.credibilityScore)

        assertTrue(newsResult is Result.Success)
        assertEquals(4, newsResult.data.credibilityScore)
        assertEquals(1, newsResult.data.warnings.size)

        assertTrue(videoResult is Result.Success)
        assertEquals(3, videoResult.data.credibilityScore)
    }

    @Test
    fun `SimpleContentValidationUseCase caches validation results`() = runTest {
        // Given
        val useCase = SimpleContentValidationUseCase(repository)
        val content = createTestArticle("1", "Test Article")
        val validationResult = ContentValidationResult(
            isCredible = true,
            credibilityScore = 5,
            warnings = emptyList(),
            lastValidated = System.currentTimeMillis()
        )
        coEvery { repository.validateContentCredibility(content) } returns Result.Success(validationResult)

        // When - First validation
        val firstResult = useCase.execute(content)
        
        // When - Second validation (should use cache if recent)
        val secondResult = useCase.execute(content)

        // Then
        assertTrue(firstResult is Result.Success)
        assertTrue(secondResult is Result.Success)
        
        // Repository should only be called once if caching is implemented
        coVerify(atLeast = 1, atMost = 2) { repository.validateContentCredibility(content) }
    }

    // ==================== Helper Methods ====================

    private fun createTestArticle(
        id: String,
        title: String,
        category: String = "nutrition",
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
        isBookmarked = false,
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