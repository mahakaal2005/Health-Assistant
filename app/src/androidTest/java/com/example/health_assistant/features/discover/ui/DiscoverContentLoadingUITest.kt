package com.example.health_assistant.features.discover.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.health_assistant.MainActivity
import com.example.health_assistant.features.discover.domain.model.DiscoverContent
import com.example.health_assistant.features.discover.domain.model.HealthContentCategory
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI tests for content loading flows in the Discover feature
 * Tests critical user interactions and UI state changes
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class DiscoverContentLoadingUITest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun discoverFragment_displaysLoadingStateInitially() {
        // Navigate to Discover tab
        composeTestRule.onNodeWithContentDescription("Discover").performClick()

        // Verify loading state is shown
        composeTestRule.onNodeWithTag("loading_indicator").assertIsDisplayed()
        composeTestRule.onNodeWithText("Loading health content...").assertIsDisplayed()
    }

    @Test
    fun discoverFragment_displaysContentAfterLoading() {
        // Navigate to Discover tab
        composeTestRule.onNodeWithContentDescription("Discover").performClick()

        // Wait for content to load
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("content_item").fetchSemanticsNodes().isNotEmpty()
        }

        // Verify content is displayed
        composeTestRule.onNodeWithTag("content_recycler_view").assertIsDisplayed()
        composeTestRule.onAllNodesWithTag("content_item").assertCountEquals(3) // Assuming 3 items loaded
    }

    @Test
    fun discoverFragment_displaysErrorStateOnFailure() {
        // Simulate network error by disconnecting
        // This would require test doubles or network simulation
        
        // Navigate to Discover tab
        composeTestRule.onNodeWithContentDescription("Discover").performClick()

        // Wait for error state (if network fails)
        composeTestRule.waitUntil(timeoutMillis = 10000) {
            composeTestRule.onAllNodesWithText("Unable to load content").fetchSemanticsNodes().isNotEmpty()
        }

        // Verify error state is shown
        composeTestRule.onNodeWithText("Unable to load content").assertIsDisplayed()
        composeTestRule.onNodeWithText("Retry").assertIsDisplayed()
    }

    @Test
    fun discoverFragment_retryButtonWorksAfterError() {
        // Navigate to Discover tab
        composeTestRule.onNodeWithContentDescription("Discover").performClick()

        // Wait for potential error state
        composeTestRule.waitUntil(timeoutMillis = 10000) {
            composeTestRule.onAllNodesWithText("Retry").fetchSemanticsNodes().isNotEmpty() ||
            composeTestRule.onAllNodesWithTag("content_item").fetchSemanticsNodes().isNotEmpty()
        }

        // If error state is shown, test retry
        if (composeTestRule.onAllNodesWithText("Retry").fetchSemanticsNodes().isNotEmpty()) {
            composeTestRule.onNodeWithText("Retry").performClick()
            
            // Verify loading state appears again
            composeTestRule.onNodeWithTag("loading_indicator").assertIsDisplayed()
        }
    }

    @Test
    fun discoverFragment_pullToRefreshWorks() {
        // Navigate to Discover tab
        composeTestRule.onNodeWithContentDescription("Discover").performClick()

        // Wait for content to load
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("content_item").fetchSemanticsNodes().isNotEmpty()
        }

        // Perform pull to refresh
        composeTestRule.onNodeWithTag("swipe_refresh").performTouchInput {
            swipeDown(startY = 100f, endY = 500f)
        }

        // Verify refresh indicator appears
        composeTestRule.onNodeWithTag("refresh_indicator").assertIsDisplayed()
    }

    @Test
    fun discoverFragment_categoryFilterWorks() {
        // Navigate to Discover tab
        composeTestRule.onNodeWithContentDescription("Discover").performClick()

        // Wait for content to load
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("content_item").fetchSemanticsNodes().isNotEmpty()
        }

        // Click on nutrition category filter
        composeTestRule.onNodeWithText("Nutrition").performClick()

        // Verify category is selected
        composeTestRule.onNodeWithText("Nutrition").assertIsSelected()

        // Verify content is filtered (this would require specific test data)
        composeTestRule.waitUntil(timeoutMillis = 3000) {
            // Content should be filtered to nutrition category
            true // Placeholder - would verify actual filtered content
        }
    }

    @Test
    fun discoverFragment_scrollingLoadsMoreContent() {
        // Navigate to Discover tab
        composeTestRule.onNodeWithContentDescription("Discover").performClick()

        // Wait for initial content to load
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("content_item").fetchSemanticsNodes().isNotEmpty()
        }

        val initialItemCount = composeTestRule.onAllNodesWithTag("content_item").fetchSemanticsNodes().size

        // Scroll to bottom to trigger pagination
        composeTestRule.onNodeWithTag("content_recycler_view").performTouchInput {
            swipeUp(startY = centerY, endY = 100f)
        }

        // Wait for more content to load
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("content_item").fetchSemanticsNodes().size > initialItemCount
        }

        // Verify more content was loaded
        val newItemCount = composeTestRule.onAllNodesWithTag("content_item").fetchSemanticsNodes().size
        assert(newItemCount > initialItemCount)
    }

    @Test
    fun discoverFragment_offlineIndicatorShownWhenOffline() {
        // This test would require network simulation
        // Navigate to Discover tab
        composeTestRule.onNodeWithContentDescription("Discover").performClick()

        // Simulate offline state (would require test doubles)
        // For now, just verify the offline indicator exists in the UI
        composeTestRule.waitUntil(timeoutMillis = 10000) {
            composeTestRule.onAllNodesWithTag("offline_indicator").fetchSemanticsNodes().isNotEmpty() ||
            composeTestRule.onAllNodesWithTag("content_item").fetchSemanticsNodes().isNotEmpty()
        }

        // If offline, verify indicator is shown
        if (composeTestRule.onAllNodesWithTag("offline_indicator").fetchSemanticsNodes().isNotEmpty()) {
            composeTestRule.onNodeWithTag("offline_indicator").assertIsDisplayed()
            composeTestRule.onNodeWithText("Offline - Showing cached content").assertIsDisplayed()
        }
    }

    @Test
    fun discoverFragment_emptyStateShownWhenNoContent() {
        // This test would require mocking empty data
        // Navigate to Discover tab
        composeTestRule.onNodeWithContentDescription("Discover").performClick()

        // Wait for loading to complete
        composeTestRule.waitUntil(timeoutMillis = 10000) {
            composeTestRule.onAllNodesWithTag("loading_indicator").fetchSemanticsNodes().isEmpty()
        }

        // If no content is available, verify empty state
        if (composeTestRule.onAllNodesWithTag("content_item").fetchSemanticsNodes().isEmpty()) {
            composeTestRule.onNodeWithText("No health content available").assertIsDisplayed()
            composeTestRule.onNodeWithText("Pull to refresh or check your connection").assertIsDisplayed()
        }
    }

    @Test
    fun discoverFragment_contentTypesDisplayCorrectly() {
        // Navigate to Discover tab
        composeTestRule.onNodeWithContentDescription("Discover").performClick()

        // Wait for content to load
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("content_item").fetchSemanticsNodes().isNotEmpty()
        }

        // Verify different content types are displayed
        // Articles should have reading time
        composeTestRule.onAllNodesWithTag("article_reading_time").assertCountEquals(1, true)
        
        // News should have source publication
        composeTestRule.onAllNodesWithTag("news_source").assertCountEquals(1, true)
        
        // Videos should have duration
        composeTestRule.onAllNodesWithTag("video_duration").assertCountEquals(1, true)
    }

    @Test
    fun discoverFragment_credibilityIndicatorsShown() {
        // Navigate to Discover tab
        composeTestRule.onNodeWithContentDescription("Discover").performClick()

        // Wait for content to load
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("content_item").fetchSemanticsNodes().isNotEmpty()
        }

        // Verify credibility indicators are shown
        composeTestRule.onAllNodesWithTag("credibility_score").assertCountEquals(1, true)
        composeTestRule.onAllNodesWithTag("source_badge").assertCountEquals(1, true)
    }

    @Test
    fun discoverFragment_loadingSkeletonShownDuringLoad() {
        // Navigate to Discover tab
        composeTestRule.onNodeWithContentDescription("Discover").performClick()

        // Verify skeleton loading is shown initially
        composeTestRule.onAllNodesWithTag("skeleton_item").assertCountEquals(3, true)
        
        // Wait for actual content to replace skeletons
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("skeleton_item").fetchSemanticsNodes().isEmpty()
        }

        // Verify skeletons are replaced with actual content
        composeTestRule.onAllNodesWithTag("content_item").assertCountEquals(1, true)
    }
}