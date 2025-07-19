package com.example.health_assistant.features.discover.presentation

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.health_assistant.core.util.Result
import com.example.health_assistant.features.discover.domain.model.DiscoverContent
import com.example.health_assistant.features.discover.domain.model.ContentValidationResult
import com.example.health_assistant.features.discover.domain.model.ContentCredibilityLevel
import com.example.health_assistant.features.discover.domain.repository.DiscoverRepository
import com.example.health_assistant.features.discover.domain.usecase.SimpleContentValidationUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class ArticleReaderViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var viewModel: ArticleReaderViewModel
    private lateinit var mockRepository: DiscoverRepository
    private lateinit var mockContentValidationUseCase: SimpleContentValidationUseCase

    private val testArticle = DiscoverContent.Article(
        id = "test-article-1",
        title = "Test Health Article",
        publishedDate = System.currentTimeMillis(),
        category = "nutrition",
        imageUrl = "https://example.com/image.jpg",
        userId = "test-user",
        summary = "This is a test article summary",
        content = "This is the full content of the test article",
        authorName = "Dr. Test Author",
        authorCredentials = "MD, PhD",
        sourceUrl = "https://example.com/article",
        lastUpdated = System.currentTimeMillis(),
        readingTimeMinutes = 5,
        tags = listOf("nutrition", "health", "wellness"),
        isBookmarked = false,
        readProgress = 0.0f,
        credibilityScore = 4
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockRepository = mockk()
        mockContentValidationUseCase = mockk()
        
        viewModel = ArticleReaderViewModel(
            discoverRepository = mockRepository,
            contentValidationUseCase = mockContentValidationUseCase
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadArticle should update article state on success`() = runTest {
        // Given
        val articleId = "test-article-1"
        coEvery { mockRepository.getArticleById(articleId) } returns Result.Success(testArticle)

        // When
        viewModel.loadArticle(articleId)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val articleResult = viewModel.article.first()
        assertTrue(articleResult is Result.Success)
        assertEquals(testArticle, (articleResult as Result.Success).data)
        
        val uiState = viewModel.uiState.first()
        assertFalse(uiState.isLoading)
        assertNull(uiState.error)
        
        val isBookmarked = viewModel.isBookmarked.first()
        assertEquals(testArticle.isBookmarked, isBookmarked)
    }

    @Test
    fun `loadArticle should update error state on failure`() = runTest {
        // Given
        val articleId = "test-article-1"
        val errorMessage = "Article not found"
        coEvery { mockRepository.getArticleById(articleId) } returns Result.Error(Exception(errorMessage))

        // When
        viewModel.loadArticle(articleId)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val articleResult = viewModel.article.first()
        assertTrue(articleResult is Result.Error)
        
        val uiState = viewModel.uiState.first()
        assertFalse(uiState.isLoading)
        assertEquals(errorMessage, uiState.error)
    }

    @Test
    fun `loadArticle should handle null article result`() = runTest {
        // Given
        val articleId = "test-article-1"
        coEvery { mockRepository.getArticleById(articleId) } returns Result.Success(null)

        // When
        viewModel.loadArticle(articleId)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val articleResult = viewModel.article.first()
        assertTrue(articleResult is Result.Error)
        
        val uiState = viewModel.uiState.first()
        assertFalse(uiState.isLoading)
        assertEquals("Article not found", uiState.error)
    }

    @Test
    fun `toggleBookmark should update bookmark state on success`() = runTest {
        // Given
        val articleId = "test-article-1"
        coEvery { mockRepository.toggleBookmark(articleId, "article") } returns Result.Success(true)

        // When
        viewModel.toggleBookmark(articleId)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val isBookmarked = viewModel.isBookmarked.first()
        assertTrue(isBookmarked)
        
        val uiState = viewModel.uiState.first()
        assertEquals("Article bookmarked", uiState.message)
        
        coVerify { mockRepository.toggleBookmark(articleId, "article") }
    }

    @Test
    fun `toggleBookmark should handle remove bookmark`() = runTest {
        // Given
        val articleId = "test-article-1"
        coEvery { mockRepository.toggleBookmark(articleId, "article") } returns Result.Success(false)

        // When
        viewModel.toggleBookmark(articleId)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val isBookmarked = viewModel.isBookmarked.first()
        assertFalse(isBookmarked)
        
        val uiState = viewModel.uiState.first()
        assertEquals("Bookmark removed", uiState.message)
    }

    @Test
    fun `toggleBookmark should update error state on failure`() = runTest {
        // Given
        val articleId = "test-article-1"
        val errorMessage = "Network error"
        coEvery { mockRepository.toggleBookmark(articleId, "article") } returns Result.Error(Exception(errorMessage))

        // When
        viewModel.toggleBookmark(articleId)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val uiState = viewModel.uiState.first()
        assertTrue(uiState.error?.contains("Failed to update bookmark") == true)
        assertTrue(uiState.error?.contains(errorMessage) == true)
    }

    @Test
    fun `updateReadingProgress should call repository`() = runTest {
        // Given
        val articleId = "test-article-1"
        val progress = 0.5f
        coEvery { mockRepository.updateReadingProgress(articleId, progress) } returns Result.Success(Unit)

        // When
        viewModel.updateReadingProgress(articleId, progress)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { mockRepository.updateReadingProgress(articleId, progress) }
    }

    @Test
    fun `validateContent should update validation state on success`() = runTest {
        // Given
        val articleId = "test-article-1"
        val validationResult = ContentValidationResult(
            contentId = articleId,
            contentType = "article",
            isCredible = true,
            credibilityScore = 4,
            credibilityLevel = ContentCredibilityLevel.MEDICAL_JOURNAL,
            warnings = emptyList(),
            lastValidated = System.currentTimeMillis()
        )
        coEvery { mockContentValidationUseCase.validateContentCredibility(articleId, "article") } returns Result.Success(validationResult)

        // When
        viewModel.validateContent(articleId)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val contentValidation = viewModel.contentValidation.first()
        assertTrue(contentValidation is Result.Success)
        assertEquals(validationResult, (contentValidation as Result.Success).data)
        
        coVerify { mockContentValidationUseCase.validateContentCredibility(articleId, "article") }
    }

    @Test
    fun `validateContent should handle validation error`() = runTest {
        // Given
        val articleId = "test-article-1"
        val errorMessage = "Validation failed"
        coEvery { mockContentValidationUseCase.validateContentCredibility(articleId, "article") } returns Result.Error(Exception(errorMessage))

        // When
        viewModel.validateContent(articleId)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val contentValidation = viewModel.contentValidation.first()
        assertTrue(contentValidation is Result.Error)
    }

    @Test
    fun `reportContentIssue should update message state on success`() = runTest {
        // Given
        val articleId = "test-article-1"
        val issueType = "inappropriate"
        val description = "This content is inappropriate"
        coEvery { mockRepository.reportContentIssue(articleId, "article", issueType, description) } returns Result.Success(Unit)

        // When
        viewModel.reportContentIssue(articleId, issueType, description)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val uiState = viewModel.uiState.first()
        assertEquals("Thank you for your feedback. We'll review this content.", uiState.message)
        
        coVerify { mockRepository.reportContentIssue(articleId, "article", issueType, description) }
    }

    @Test
    fun `clearError should clear error state`() = runTest {
        // Given - Set an error state first
        val articleId = "test-article-1"
        coEvery { mockRepository.getArticleById(articleId) } returns Result.Error(Exception("Test error"))
        viewModel.loadArticle(articleId)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.clearError()

        // Then
        val uiState = viewModel.uiState.first()
        assertNull(uiState.error)
    }

    @Test
    fun `clearMessage should clear message state`() = runTest {
        // Given - Set a message state first
        val articleId = "test-article-1"
        coEvery { mockRepository.toggleBookmark(articleId, "article") } returns Result.Success(true)
        viewModel.toggleBookmark(articleId)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.clearMessage()

        // Then
        val uiState = viewModel.uiState.first()
        assertNull(uiState.message)
    }

    @Test
    fun `refreshArticle should reload article`() = runTest {
        // Given
        val articleId = "test-article-1"
        coEvery { mockRepository.getArticleById(articleId) } returns Result.Success(testArticle)

        // When
        viewModel.refreshArticle(articleId)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val articleResult = viewModel.article.first()
        assertTrue(articleResult is Result.Success)
        assertEquals(testArticle, (articleResult as Result.Success).data)
        
        coVerify { mockRepository.getArticleById(articleId) }
    }
}