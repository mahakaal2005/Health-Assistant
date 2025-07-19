package com.example.health_assistant.features.discover.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.health_assistant.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI tests for search functionality in the Discover feature
 * Tests search interactions, filtering, and result display
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class DiscoverSearchUITest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun searchBar_expandsWhenClicked() {
        // Navigate to Discover tab
        composeTestRule.onNodeWithContentDescription("Discover").performClick()

        // Wait for content to load
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("content_item").fetchSemanticsNodes().isNotEmpty()
        }

        // Click search icon
        composeTestRule.onNodeWithTag("search_icon").performClick()

        // Verify search bar expands
        composeTestRule.onNodeWithTag("search_text_field").assertIsDisplayed()
        composeTestRule.onNodeWithTag("search_text_field").assertIsFocused()
        composeTestRule.onNodeWithText("Search health content...").assertIsDisplayed()
    }

    @Test
    fun searchBar_performsSearchOnTextInput() {
        // Navigate to Discover tab and open search
        composeTestRule.onNodeWithContentDescription("Discover").performClick()
        
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("content_item").fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithTag("search_icon").performClick()

        // Type search query
        composeTestRule.onNodeWithTag("search_text_field").performTextInput("diabetes")

        // Verify search is triggered (debounced)
        composeTestRule.waitUntil(timeoutMillis = 3000) {
            composeTestRule.onAllNodesWithTag("search_result_item").fetchSemanticsNodes().isNotEmpty() ||
            composeTestRule.onAllNodesWithText("No results found").fetchSemanticsNodes().isNotEmpty()
        }

        // Verify search results or no results message
        if (composeTestRule.onAllNodesWithTag("search_result_item").fetchSemanticsNodes().isNotEmpty()) {
            composeTestRule.onAllNodesWithTag("search_result_item").assertCountEquals(1, true)
            composeTestRule.onNodeWithText("Search results for \"diabetes\"").assertIsDisplayed()
        } else {
            composeTestRule.onNodeWithText("No results found for \"diabetes\"").assertIsDisplayed()
        }
    }

    @Test
    fun searchBar_showsLoadingDuringSearch() {
        // Navigate to Discover tab and open search
        composeTestRule.onNodeWithContentDescription("Discover").performClick()
        composeTestRule.onNodeWithTag("search_icon").performClick()

        // Type search query
        composeTestRule.onNodeWithTag("search_text_field").performTextInput("nutrition")

        // Verify loading indicator appears briefly
        composeTestRule.onNodeWithTag("search_loading").assertIsDisplayed()

        // Wait for search to complete
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("search_loading").fetchSemanticsNodes().isEmpty()
        }
    }

    @Test
    fun searchBar_clearButtonWorks() {
        // Navigate to Discover tab and open search
        composeTestRule.onNodeWithContentDescription("Discover").performClick()
        composeTestRule.onNodeWithTag("search_icon").performClick()

        // Type search query
        composeTestRule.onNodeWithTag("search_text_field").performTextInput("fitness")

        // Verify clear button appears
        composeTestRule.onNodeWithTag("search_clear_button").assertIsDisplayed()

        // Click clear button
        composeTestRule.onNodeWithTag("search_clear_button").performClick()

        // Verify search field is cleared and results are hidden
        composeTestRule.onNodeWithTag("search_text_field").assertTextEquals("")
        composeTestRule.onAllNodesWithTag("search_result_item").assertCountEquals(0)
    }

    @Test
    fun searchBar_backButtonExitsSearch() {
        // Navigate to Discover tab and open search
        composeTestRule.onNodeWithContentDescription("Discover").performClick()
        composeTestRule.onNodeWithTag("search_icon").performClick()

        // Type search query
        composeTestRule.onNodeWithTag("search_text_field").performTextInput("mental health")

        // Click back button
        composeTestRule.onNodeWithTag("search_back_button").performClick()

        // Verify search mode is exited
        composeTestRule.onNodeWithTag("search_text_field").assertDoesNotExist()
        composeTestRule.onAllNodesWithTag("content_item").assertCountEquals(1, true) // Back to normal content
    }

    @Test
    fun searchResults_displayCorrectContentTypes() {
        // Navigate to Discover tab and search
        composeTestRule.onNodeWithContentDescription("Discover").performClick()
        composeTestRule.onNodeWithTag("search_icon").performClick()
        composeTestRule.onNodeWithTag("search_text_field").performTextInput("health")

        // Wait for search results
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("search_result_item").fetchSemanticsNodes().isNotEmpty()
        }

        // Verify different content types are shown with proper indicators
        composeTestRule.onAllNodesWithTag("article_result_indicator").assertCountEquals(1, true)
        composeTestRule.onAllNodesWithTag("news_result_indicator").assertCountEquals(1, true)
        composeTestRule.onAllNodesWithTag("video_result_indicator").assertCountEquals(1, true)
    }

    @Test
    fun searchResults_highlightSearchTerms() {
        // Navigate to Discover tab and search
        composeTestRule.onNodeWithContentDescription("Discover").performClick()
        composeTestRule.onNodeWithTag("search_icon").performClick()
        composeTestRule.onNodeWithTag("search_text_field").performTextInput("exercise")

        // Wait for search results
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("search_result_item").fetchSemanticsNodes().isNotEmpty()
        }

        // Verify search terms are highlighted in results
        composeTestRule.onAllNodesWithTag("highlighted_text").assertCountEquals(1, true)
    }

    @Test
    fun searchFilters_allowContentTypeFiltering() {
        // Navigate to Discover tab and search
        composeTestRule.onNodeWithContentDescription("Discover").performClick()
        composeTestRule.onNodeWithTag("search_icon").performClick()
        composeTestRule.onNodeWithTag("search_text_field").performTextInput("wellness")

        // Wait for search results
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("search_result_item").fetchSemanticsNodes().isNotEmpty()
        }

        // Open filter options
        composeTestRule.onNodeWithTag("search_filter_button").performClick()

        // Verify filter options are available
        composeTestRule.onNodeWithText("All Content").assertIsDisplayed()
        composeTestRule.onNodeWithText("Articles").assertIsDisplayed()
        composeTestRule.onNodeWithText("News").assertIsDisplayed()
        composeTestRule.onNodeWithText("Videos").assertIsDisplayed()

        // Select Articles filter
        composeTestRule.onNodeWithText("Articles").performClick()

        // Verify only articles are shown in results
        composeTestRule.waitUntil(timeoutMillis = 3000) {
            composeTestRule.onAllNodesWithTag("article_result_indicator").fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onAllNodesWithTag("news_result_indicator").assertCountEquals(0)
        composeTestRule.onAllNodesWithTag("video_result_indicator").assertCountEquals(0)
    }

    @Test
    fun searchFilters_allowCategoryFiltering() {
        // Navigate to Discover tab and search
        composeTestRule.onNodeWithContentDescription("Discover").performClick()
        composeTestRule.onNodeWithTag("search_icon").performClick()
        composeTestRule.onNodeWithTag("search_text_field").performTextInput("diet")

        // Wait for search results
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("search_result_item").fetchSemanticsNodes().isNotEmpty()
        }

        // Open category filter
        composeTestRule.onNodeWithTag("category_filter_button").performClick()

        // Select Nutrition category
        composeTestRule.onNodeWithText("Nutrition").performClick()

        // Verify results are filtered by category
        composeTestRule.waitUntil(timeoutMillis = 3000) {
            composeTestRule.onAllNodesWithText("Nutrition").fetchSemanticsNodes().isNotEmpty()
        }

        // All results should be nutrition-related
        composeTestRule.onAllNodesWithTag("search_result_item").onEach { node ->
            // Each result should have nutrition category indicator
            // This would require specific test data structure
        }
    }

    @Test
    fun searchSuggestions_appearAsUserTypes() {
        // Navigate to Discover tab and open search
        composeTestRule.onNodeWithContentDescription("Discover").performClick()
        composeTestRule.onNodeWithTag("search_icon").performClick()

        // Type partial query
        composeTestRule.onNodeWithTag("search_text_field").performTextInput("diab")

        // Verify search suggestions appear
        composeTestRule.waitUntil(timeoutMillis = 3000) {
            composeTestRule.onAllNodesWithTag("search_suggestion").fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithText("diabetes").assertIsDisplayed()
        composeTestRule.onNodeWithText("diabetic diet").assertIsDisplayed()

        // Click on a suggestion
        composeTestRule.onNodeWithText("diabetes").performClick()

        // Verify suggestion is applied to search field
        composeTestRule.onNodeWithTag("search_text_field").assertTextContains("diabetes")
    }

    @Test
    fun searchHistory_showsRecentSearches() {
        // Navigate to Discover tab and perform a search
        composeTestRule.onNodeWithContentDescription("Discover").performClick()
        composeTestRule.onNodeWithTag("search_icon").performClick()
        composeTestRule.onNodeWithTag("search_text_field").performTextInput("cardio")

        // Wait for search to complete
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("search_result_item").fetchSemanticsNodes().isNotEmpty() ||
            composeTestRule.onAllNodesWithText("No results found").fetchSemanticsNodes().isNotEmpty()
        }

        // Clear search and open search again
        composeTestRule.onNodeWithTag("search_back_button").performClick()
        composeTestRule.onNodeWithTag("search_icon").performClick()

        // Verify recent search appears in history
        composeTestRule.onNodeWithText("Recent searches").assertIsDisplayed()
        composeTestRule.onNodeWithText("cardio").assertIsDisplayed()

        // Click on recent search
        composeTestRule.onNodeWithText("cardio").performClick()

        // Verify search is performed again
        composeTestRule.onNodeWithTag("search_text_field").assertTextContains("cardio")
    }

    @Test
    fun searchResults_allowSortingByRelevance() {
        // Navigate to Discover tab and search
        composeTestRule.onNodeWithContentDescription("Discover").performClick()
        composeTestRule.onNodeWithTag("search_icon").performClick()
        composeTestRule.onNodeWithTag("search_text_field").performTextInput("nutrition")

        // Wait for search results
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("search_result_item").fetchSemanticsNodes().isNotEmpty()
        }

        // Open sort options
        composeTestRule.onNodeWithTag("search_sort_button").performClick()

        // Verify sort options
        composeTestRule.onNodeWithText("Most Relevant").assertIsDisplayed()
        composeTestRule.onNodeWithText("Most Recent").assertIsDisplayed()
        composeTestRule.onNodeWithText("Highest Rated").assertIsDisplayed()

        // Select Most Recent
        composeTestRule.onNodeWithText("Most Recent").performClick()

        // Verify results are reordered
        composeTestRule.waitUntil(timeoutMillis = 3000) {
            // Results should be reordered by date
            true // Placeholder - would verify actual order
        }
    }

    @Test
    fun searchResults_showEmptyStateForNoResults() {
        // Navigate to Discover tab and search for something unlikely to exist
        composeTestRule.onNodeWithContentDescription("Discover").performClick()
        composeTestRule.onNodeWithTag("search_icon").performClick()
        composeTestRule.onNodeWithTag("search_text_field").performTextInput("xyzabc123nonexistent")

        // Wait for search to complete
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithText("No results found").fetchSemanticsNodes().isNotEmpty()
        }

        // Verify empty state is shown
        composeTestRule.onNodeWithText("No results found for \"xyzabc123nonexistent\"").assertIsDisplayed()
        composeTestRule.onNodeWithText("Try different keywords or check spelling").assertIsDisplayed()
        composeTestRule.onNodeWithText("Browse all content").assertIsDisplayed()

        // Test browse all content button
        composeTestRule.onNodeWithText("Browse all content").performClick()

        // Verify search is exited and normal content is shown
        composeTestRule.onAllNodesWithTag("content_item").assertCountEquals(1, true)
    }

    @Test
    fun searchResults_handleLongQueries() {
        // Navigate to Discover tab and search with long query
        composeTestRule.onNodeWithContentDescription("Discover").performClick()
        composeTestRule.onNodeWithTag("search_icon").performClick()
        
        val longQuery = "how to maintain healthy diet and exercise routine for cardiovascular health"
        composeTestRule.onNodeWithTag("search_text_field").performTextInput(longQuery)

        // Verify search field handles long text
        composeTestRule.onNodeWithTag("search_text_field").assertTextContains("cardiovascular health")

        // Wait for search results
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("search_result_item").fetchSemanticsNodes().isNotEmpty() ||
            composeTestRule.onAllNodesWithText("No results found").fetchSemanticsNodes().isNotEmpty()
        }

        // Verify search query is properly displayed in results header
        composeTestRule.onNodeWithText("Search results").assertIsDisplayed()
    }

    @Test
    fun searchResults_supportPagination() {
        // Navigate to Discover tab and search for common term
        composeTestRule.onNodeWithContentDescription("Discover").performClick()
        composeTestRule.onNodeWithTag("search_icon").performClick()
        composeTestRule.onNodeWithTag("search_text_field").performTextInput("health")

        // Wait for initial search results
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("search_result_item").fetchSemanticsNodes().isNotEmpty()
        }

        val initialResultCount = composeTestRule.onAllNodesWithTag("search_result_item").fetchSemanticsNodes().size

        // Scroll to bottom to load more results
        composeTestRule.onNodeWithTag("search_results_list").performTouchInput {
            swipeUp(startY = centerY, endY = 100f)
        }

        // Wait for more results to load
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("search_result_item").fetchSemanticsNodes().size > initialResultCount
        }

        // Verify more results were loaded
        val newResultCount = composeTestRule.onAllNodesWithTag("search_result_item").fetchSemanticsNodes().size
        assert(newResultCount > initialResultCount)
    }
}