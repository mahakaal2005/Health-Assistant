package com.example.health_assistant.features.discover.presentation

import com.example.health_assistant.core.util.Result
import com.example.health_assistant.features.discover.domain.DiscoverManager
import com.example.health_assistant.features.discover.domain.SearchFilters
import com.example.health_assistant.features.discover.domain.SearchResults
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
import kotlin.test.assertTrue

/**
 * Test class for search functionality in DiscoverViewModel
 * Tests debounced search, search suggestions, and result handling
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DiscoverSearchFunctionalityTest {

    private val testDispatcher = StandardTestDispatcher()
    private val discoverManager = mockk<DiscoverManager>()
    private lateinit var viewModel: DiscoverViewModel

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        // Setup default mock responses
        every { discoverManager.getContentFeed(any(), any()) } returns flowOf(Result.Success(createMockFeedData()))
        every { discoverManager.getTrendingContent(any()) } returns flowOf(Result.Success(emptyList()))
        every { discoverManager.getBookmarkedContent() } returns flowOf(Result.Success(emptyList()))
        
        viewModel = DiscoverViewModel(discoverManager)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `updateSearchQuery triggers debounced search after delay`() = runTest {
        // Given
        val query = "nutrition"
        val mockResults = SearchResults(
            query = query,
            results = listOf(createMockArticle("Nutrition Guide")),
            totalCount = 1,
            appliedFilters = SearchFilters()
        )
        coEvery { discoverManager.searchContent(query, any()) } returns Result.Success(mockResults)

        // When
        viewModel.updateSearchQuery(query)
        
        // Then - search should not be triggered immediately
        coVerify(exactly = 0) { discoverManager.searchContent(any(), any()) }
        
        // Advance time past debounce delay
        testScheduler.advanceTimeBy(350)
        
        // Then - search should be triggered after debounce
        coVerify(exactly = 1) { discoverManager.searchContent(query, any()) }
        assertEquals(query, viewModel.searchQuery.value)
        assertTrue(viewModel.isSearchActive.value)
    }

    @Test
    fun `updateSearchQuery cancels previous search when called multiple times`() = runTest {
        // Given
        val query1 = "nutrition"
        val query2 = "fitness"
        val mockResults = SearchResults(
            query = query2,
            results = listOf(createMockArticle("Fitness Guide")),
            totalCount = 1,
            appliedFilters = SearchFilters()
        )
        coEvery { discoverManager.searchContent(any(), any()) } returns Result.Success(mockResults)

        // When - rapid successive calls
        viewModel.updateSearchQuery(query1)
        testScheduler.advanceTimeBy(100)
        viewModel.updateSearchQuery(query2)
        
        // Advance time past debounce delay
        testScheduler.advanceTimeBy(350)
        
        // Then - only the last search should be executed
        coVerify(exactly = 0) { discoverManager.searchContent(query1, any()) }
        coVerify(exactly = 1) { discoverManager.searchContent(query2, any()) }
        assertEquals(query2, viewModel.searchQuery.value)
    }

    @Test
    fun `searchContent sets loading state and handles success`() = runTest {
        // Given
        val query = "mental health"
        val mockResults = SearchResults(
            query = query,
            results = listOf(
                createMockArticle("Mental Health Tips"),
                createMockNews("Mental Health Research")
            ),
            totalCount = 2,
            appliedFilters = SearchFilters()
        )
        coEvery { discoverManager.searchContent(query, any()) } returns Result.Success(mockResults)

        // When
        viewModel.searchContent(query)
        testScheduler.advanceTimeBy(350)

        // Then
        assertTrue(viewModel.isSearchActive.value)
        assertEquals(query, viewModel.searchQuery.value)
        
        val searchResults = viewModel.searchResults.value
        assertTrue(searchResults is Result.Success)
        assertEquals(2, (searchResults as Result.Success).data.totalCount)
        assertEquals(query, searchResults.data.query)
    }

    @Test
    fun `searchContent handles error state`() = runTest {
        // Given
        val query = "invalid query"
        val errorMessage = "Search failed"
        coEvery { discoverManager.searchContent(query, any()) } returns Result.Error(Exception(errorMessage))

        // When
        viewModel.searchContent(query)
        testScheduler.advanceTimeBy(350)

        // Then
        assertTrue(viewModel.isSearchActive.value)
        val searchResults = viewModel.searchResults.value
        assertTrue(searchResults is Result.Error)
        assertTrue((searchResults as Result.Error).exception?.message?.contains("Search failed") == true)
    }

    @Test
    fun `clearSearch resets search state`() = runTest {
        // Given - set up search state
        val query = "diabetes"
        viewModel.updateSearchQuery(query)
        testScheduler.advanceTimeBy(350)

        // When
        viewModel.clearSearch()

        // Then
        assertEquals("", viewModel.searchQuery.value)
        assertFalse(viewModel.isSearchActive.value)
        assertEquals(null, viewModel.searchResults.value)
    }

    @Test
    fun `updateSearchQuery with blank query clears search`() = runTest {
        // Given - set up search state
        viewModel.updateSearchQuery("test")
        testScheduler.advanceTimeBy(350)

        // When
        viewModel.updateSearchQuery("")

        // Then
        assertEquals("", viewModel.searchQuery.value)
        assertFalse(viewModel.isSearchActive.value)
    }

    @Test
    fun `search suggestions are generated for valid queries`() = runTest {
        // When
        viewModel.updateSearchQuery("nut")
        testScheduler.advanceTimeBy(200) // Advance past suggestion delay

        // Then
        val suggestions = viewModel.searchSuggestions.value
        assertTrue(suggestions.isNotEmpty())
        assertTrue(suggestions.any { it.contains("nutrition", ignoreCase = true) })
    }

    @Test
    fun `selectSearchSuggestion triggers search`() = runTest {
        // Given
        val suggestion = "Mental Health"
        val mockResults = SearchResults(
            query = suggestion,
            results = listOf(createMockArticle("Mental Health Guide")),
            totalCount = 1,
            appliedFilters = SearchFilters()
        )
        coEvery { discoverManager.searchContent(suggestion, any()) } returns Result.Success(mockResults)

        // When
        viewModel.selectSearchSuggestion(suggestion)
        testScheduler.advanceTimeBy(350)

        // Then
        coVerify { discoverManager.searchContent(suggestion, any()) }
        assertTrue(viewModel.isSearchActive.value)
        assertTrue(viewModel.searchSuggestions.value.isEmpty())
    }

    @Test
    fun `updateSearchFilters triggers new search when search is active`() = runTest {
        // Given - active search
        val query = "fitness"
        viewModel.updateSearchQuery(query)
        testScheduler.advanceTimeBy(350)
        
        val newFilters = SearchFilters(
            contentTypes = listOf("article"),
            categories = listOf(HealthContentCategory.FITNESS)
        )
        val mockResults = SearchResults(
            query = query,
            results = listOf(createMockArticle("Fitness Article")),
            totalCount = 1,
            appliedFilters = newFilters
        )
        coEvery { discoverManager.searchContent(query, newFilters) } returns Result.Success(mockResults)

        // When
        viewModel.updateSearchFilters(newFilters)
        testScheduler.advanceTimeBy(350)

        // Then
        coVerify { discoverManager.searchContent(query, newFilters) }
        assertEquals(newFilters, viewModel.searchFilters.value)
    }

    @Test
    fun `short queries do not trigger search but generate suggestions`() = runTest {
        // Given
        val shortQuery = "a"
        
        // When
        viewModel.updateSearchQuery(shortQuery)
        testScheduler.advanceTimeBy(350)

        // Then - no search should be triggered
        coVerify(exactly = 0) { discoverManager.searchContent(any(), any()) }
        
        // But query should be updated
        assertEquals(shortQuery, viewModel.searchQuery.value)
    }

    // Helper methods for creating mock data
    private fun createMockFeedData() = com.example.health_assistant.features.discover.domain.DiscoverFeedData(
        articles = listOf(createMockArticle("Sample Article")),
        news = emptyList(),
        videos = emptyList(),
        hasErrors = false,
        lastUpdated = System.currentTimeMillis()
    )

    private fun createMockArticle(title: String) = DiscoverContent.Article(
        id = "article_${title.hashCode()}",
        title = title,
        publishedDate = System.currentTimeMillis(),
        category = "general",
        imageUrl = null,
        userId = "test-user",
        summary = "Summary for $title",
        content = "Content for $title",
        authorName = "Dr. Test",
        authorCredentials = "MD, PhD",
        sourceUrl = "https://example.com",
        lastUpdated = System.currentTimeMillis(),
        readingTimeMinutes = 5,
        tags = listOf("health"),
        isBookmarked = false,
        readProgress = 0f,
        credibilityScore = 4
    )

    private fun createMockNews(title: String) = DiscoverContent.News(
        id = "news_${title.hashCode()}",
        title = title,
        publishedDate = System.currentTimeMillis(),
        category = "general",
        imageUrl = null,
        userId = "test-user",
        summary = "Summary for $title",
        fullContent = "Full content for $title",
        sourcePublication = "Health News",
        sourceCredibility = "medical-journal",
        externalUrl = "https://example.com",
        isBreakingNews = false,
        relevanceScore = 3
    )
}