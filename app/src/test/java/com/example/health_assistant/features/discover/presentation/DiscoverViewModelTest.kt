package com.example.health_assistant.features.discover.presentation

import com.example.health_assistant.core.util.Result
import com.example.health_assistant.features.discover.domain.DiscoverManager
import com.example.health_assistant.features.discover.domain.DiscoverFeedData
import com.example.health_assistant.features.discover.domain.SearchResults
import com.example.health_assistant.features.discover.domain.SearchFilters
import com.example.health_assistant.features.discover.domain.model.DiscoverContent
import com.example.health_assistant.features.discover.domain.model.HealthContentCategory
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class DiscoverViewModelTest {

    private lateinit var viewModel: DiscoverViewModel
    private lateinit var discoverManager: DiscoverManager
    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        discoverManager = mockk()
        
        // Setup default mock responses
        every { discoverManager.getContentFeed(any(), any()) } returns flowOf(
            Result.Success(createMockFeedData())
        )
        every { discoverManager.getTrendingContent(any()) } returns flowOf(
            Result.Success(emptyList())
        )
        every { discoverManager.getBookmarkedContent() } returns flowOf(
            Result.Success(emptyList())
        )
        
        viewModel = DiscoverViewModel(discoverManager)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state should be correct`() = runTest {
        // Wait for initialization to complete
        testScheduler.advanceUntilIdle()
        
        val initialState = viewModel.uiState.value
        // After initialization, loading should be false since we have mock data
        assertFalse(initialState.isLoading)
        assertNull(initialState.error)
        assertFalse(initialState.isRefreshing)
    }

    @Test
    fun `should load content feed on initialization`() = runTest {
        // Wait for initialization to complete
        testScheduler.advanceUntilIdle()
        
        // Verify content feed was loaded
        val contentFeed = viewModel.contentFeed.value
        assertTrue(contentFeed is Result.Success)
        
        // Verify UI state updated
        val uiState = viewModel.uiState.value
        assertFalse(uiState.isLoading)
        assertNull(uiState.error)
    }

    @Test
    fun `refreshContent should trigger content reload`() = runTest {
        // Setup mock for refresh
        every { discoverManager.getContentFeed(any(), any()) } returns flowOf(
            Result.Success(createMockFeedData())
        )
        coEvery { discoverManager.syncContent() } returns Result.Success(Unit)
        
        // Trigger refresh
        viewModel.refreshContent()
        testScheduler.advanceUntilIdle()
        
        // Verify sync was called
        coVerify { discoverManager.syncContent() }
        
        // Verify UI state
        val uiState = viewModel.uiState.value
        assertFalse(uiState.isRefreshing)
    }

    @Test
    fun `searchContent should update search state and results`() = runTest {
        val query = "test query"
        val mockResults = SearchResults(
            query = query,
            results = listOf(createMockArticle()),
            totalCount = 1,
            appliedFilters = SearchFilters()
        )
        
        coEvery { discoverManager.searchContent(query, any()) } returns Result.Success(mockResults)
        
        // Perform search
        viewModel.searchContent(query)
        testScheduler.advanceUntilIdle()
        
        // Verify search state
        assertEquals(query, viewModel.searchQuery.value)
        assertTrue(viewModel.isSearchActive.value)
        
        // Verify search results
        val searchResults = viewModel.searchResults.value
        assertTrue(searchResults is Result.Success)
        assertEquals(query, searchResults.data.query)
    }

    @Test
    fun `clearSearch should reset search state`() = runTest {
        // First perform a search
        viewModel.searchContent("test")
        testScheduler.advanceUntilIdle()
        
        // Then clear search
        viewModel.clearSearch()
        
        // Verify search state is cleared
        assertEquals("", viewModel.searchQuery.value)
        assertFalse(viewModel.isSearchActive.value)
        assertNull(viewModel.searchResults.value)
    }

    @Test
    fun `loadContentByCategory should update selected category and reload content`() = runTest {
        val category = HealthContentCategory.NUTRITION
        
        // Load content by category
        viewModel.loadContentByCategory(category)
        testScheduler.advanceUntilIdle()
        
        // Verify category was selected
        assertEquals(category, viewModel.selectedCategory.value)
        
        // Verify content was loaded for category
        coVerify { discoverManager.getContentFeed(category, any()) }
    }

    @Test
    fun `toggleBookmark should call manager and update state`() = runTest {
        val content = createMockArticle()
        coEvery { discoverManager.toggleBookmark(content) } returns Result.Success(true)
        every { discoverManager.getBookmarkedContent() } returns flowOf(
            Result.Success(listOf(content))
        )
        
        // Toggle bookmark
        viewModel.toggleBookmark(content)
        testScheduler.advanceUntilIdle()
        
        // Verify manager was called
        coVerify { discoverManager.toggleBookmark(content) }
        
        // Verify UI state updated
        val uiState = viewModel.uiState.value
        assertEquals(content.id, uiState.lastBookmarkAction?.contentId)
        assertTrue(uiState.lastBookmarkAction?.isBookmarked == true)
    }

    @Test
    fun `error handling should update UI state`() = runTest {
        val errorMessage = "Test error"
        every { discoverManager.getContentFeed(any(), any()) } returns flowOf(
            Result.Error(Exception(errorMessage))
        )
        
        // Create new ViewModel to trigger error
        val errorViewModel = DiscoverViewModel(discoverManager)
        testScheduler.advanceUntilIdle()
        
        // Verify error state
        val uiState = errorViewModel.uiState.value
        assertFalse(uiState.isLoading)
        assertTrue(uiState.error?.contains(errorMessage) == true)
    }

    @Test
    fun `clearError should remove error from state`() = runTest {
        // Set error state manually
        viewModel.clearError()
        
        // Verify error is cleared
        val uiState = viewModel.uiState.value
        assertNull(uiState.error)
    }

    @Test
    fun `setViewMode should update view mode in state`() {
        // Set grid view mode
        viewModel.setViewMode(ViewMode.GRID)
        
        // Verify view mode updated
        val uiState = viewModel.uiState.value
        assertEquals(ViewMode.GRID, uiState.viewMode)
    }

    // Helper methods for creating mock data
    private fun createMockFeedData(): DiscoverFeedData {
        return DiscoverFeedData(
            articles = listOf(createMockArticle()),
            news = emptyList(),
            videos = emptyList(),
            hasErrors = false,
            lastUpdated = System.currentTimeMillis()
        )
    }

    private fun createMockArticle(): DiscoverContent.Article {
        return DiscoverContent.Article(
            id = "test-article-1",
            title = "Test Article",
            publishedDate = System.currentTimeMillis(),
            category = "nutrition",
            imageUrl = "https://example.com/image.jpg",
            userId = "user-1",
            summary = "Test article summary",
            content = "Test article content",
            authorName = "Test Author",
            authorCredentials = "MD",
            sourceUrl = "https://example.com/article",
            lastUpdated = System.currentTimeMillis(),
            readingTimeMinutes = 5,
            tags = listOf("health", "nutrition"),
            isBookmarked = false,
            readProgress = 0f,
            credibilityScore = 5
        )
    }
}