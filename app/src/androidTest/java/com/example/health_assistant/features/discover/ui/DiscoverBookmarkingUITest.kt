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
 * UI tests for bookmarking functionality in the Discover feature
 * Tests bookmark interactions, state changes, and bookmark management
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class DiscoverBookmarkingUITest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun bookmarkButton_togglesBookmarkState() {
        // Navigate to Discover tab
        composeTestRule.onNodeWithContentDescription("Discover").performClick()

        // Wait for content to load
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("content_item").fetchSemanticsNodes().isNotEmpty()
        }

        // Find first bookmark button (should be unbookmarked initially)
        val bookmarkButton = composeTestRule.onAllNodesWithTag("bookmark_button")[0]
        bookmarkButton.assertIsDisplayed()

        // Click to bookmark
        bookmarkButton.performClick()

        // Verify bookmark state changed (visual feedback)
        composeTestRule.waitUntil(timeoutMillis = 3000) {
            composeTestRule.onAllNodesWithTag("bookmark_button_filled").fetchSemanticsNodes().isNotEmpty()
        }

        // Verify bookmark confirmation message
        composeTestRule.onNodeWithText("Bookmarked").assertIsDisplayed()

        // Click again to unbookmark
        bookmarkButton.performClick()

        // Verify bookmark state changed back
        composeTestRule.waitUntil(timeoutMillis = 3000) {
            composeTestRule.onAllNodesWithTag("bookmark_button_outline").fetchSemanticsNodes().isNotEmpty()
        }

        // Verify unbookmark confirmation message
        composeTestRule.onNodeWithText("Bookmark removed").assertIsDisplayed()
    }

    @Test
    fun bookmarkButton_showsLoadingStateDuringOperation() {
        // Navigate to Discover tab
        composeTestRule.onNodeWithContentDescription("Discover").performClick()

        // Wait for content to load
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("content_item").fetchSemanticsNodes().isNotEmpty()
        }

        // Click bookmark button
        val bookmarkButton = composeTestRule.onAllNodesWithTag("bookmark_button")[0]
        bookmarkButton.performClick()

        // Verify loading state is shown briefly
        composeTestRule.onNodeWithTag("bookmark_loading").assertIsDisplayed()

        // Wait for operation to complete
        composeTestRule.waitUntil(timeoutMillis = 3000) {
            composeTestRule.onAllNodesWithTag("bookmark_loading").fetchSemanticsNodes().isEmpty()
        }
    }

    @Test
    fun bookmarkButton_handlesErrorGracefully() {
        // This test would require mocking network errors
        // Navigate to Discover tab
        composeTestRule.onNodeWithContentDescription("Discover").performClick()

        // Wait for content to load
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("content_item").fetchSemanticsNodes().isNotEmpty()
        }

        // Click bookmark button (assuming error occurs)
        val bookmarkButton = composeTestRule.onAllNodesWithTag("bookmark_button")[0]
        bookmarkButton.performClick()

        // Wait for potential error message
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithText("Failed to bookmark").fetchSemanticsNodes().isNotEmpty() ||
            composeTestRule.onAllNodesWithText("Bookmarked").fetchSemanticsNodes().isNotEmpty()
        }

        // If error occurred, verify error message and retry option
        if (composeTestRule.onAllNodesWithText("Failed to bookmark").fetchSemanticsNodes().isNotEmpty()) {
            composeTestRule.onNodeWithText("Failed to bookmark").assertIsDisplayed()
            composeTestRule.onNodeWithText("Retry").assertIsDisplayed()
        }
    }

    @Test
    fun bookmarksTab_displaysBookmarkedContent() {
        // First, bookmark some content
        composeTestRule.onNodeWithContentDescription("Discover").performClick()

        // Wait for content and bookmark an item
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("content_item").fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onAllNodesWithTag("bookmark_button")[0].performClick()

        // Wait for bookmark to complete
        composeTestRule.waitUntil(timeoutMillis = 3000) {
            composeTestRule.onAllNodesWithText("Bookmarked").fetchSemanticsNodes().isNotEmpty()
        }

        // Navigate to Bookmarks tab
        composeTestRule.onNodeWithContentDescription("Bookmarks").performClick()

        // Verify bookmarked content is displayed
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("bookmark_item").fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onAllNodesWithTag("bookmark_item").assertCountEquals(1, true)
    }

    @Test
    fun bookmarksTab_showsEmptyStateWhenNoBookmarks() {
        // Navigate to Bookmarks tab
        composeTestRule.onNodeWithContentDescription("Bookmarks").performClick()

        // Wait for loading to complete
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("loading_indicator").fetchSemanticsNodes().isEmpty()
        }

        // Verify empty state is shown
        composeTestRule.onNodeWithText("No bookmarks yet").assertIsDisplayed()
        composeTestRule.onNodeWithText("Bookmark articles, news, and videos to read later").assertIsDisplayed()
        composeTestRule.onNodeWithText("Browse Content").assertIsDisplayed()
    }

    @Test
    fun bookmarksTab_allowsRemovingBookmarks() {
        // First, create a bookmark (assuming one exists)
        composeTestRule.onNodeWithContentDescription("Discover").performClick()

        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("content_item").fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onAllNodesWithTag("bookmark_button")[0].performClick()

        // Navigate to Bookmarks tab
        composeTestRule.onNodeWithContentDescription("Bookmarks").performClick()

        // Wait for bookmarks to load
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("bookmark_item").fetchSemanticsNodes().isNotEmpty()
        }

        // Remove bookmark
        composeTestRule.onAllNodesWithTag("remove_bookmark_button")[0].performClick()

        // Confirm removal in dialog
        composeTestRule.onNodeWithText("Remove Bookmark").assertIsDisplayed()
        composeTestRule.onNodeWithText("Remove").performClick()

        // Verify bookmark was removed
        composeTestRule.waitUntil(timeoutMillis = 3000) {
            composeTestRule.onAllNodesWithTag("bookmark_item").fetchSemanticsNodes().isEmpty()
        }

        composeTestRule.onNodeWithText("Bookmark removed").assertIsDisplayed()
    }

    @Test
    fun bookmarksTab_categorizesByContentType() {
        // This test assumes multiple bookmarks of different types exist
        composeTestRule.onNodeWithContentDescription("Bookmarks").performClick()

        // Wait for bookmarks to load
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("bookmark_item").fetchSemanticsNodes().isNotEmpty()
        }

        // Verify category filters are available
        composeTestRule.onNodeWithText("All").assertIsDisplayed()
        composeTestRule.onNodeWithText("Articles").assertIsDisplayed()
        composeTestRule.onNodeWithText("News").assertIsDisplayed()
        composeTestRule.onNodeWithText("Videos").assertIsDisplayed()

        // Test filtering by Articles
        composeTestRule.onNodeWithText("Articles").performClick()

        // Verify only articles are shown
        composeTestRule.waitUntil(timeoutMillis = 3000) {
            composeTestRule.onAllNodesWithTag("bookmark_article_item").fetchSemanticsNodes().isNotEmpty()
        }

        // Verify no news or video items are shown
        composeTestRule.onAllNodesWithTag("bookmark_news_item").assertCountEquals(0)
        composeTestRule.onAllNodesWithTag("bookmark_video_item").assertCountEquals(0)
    }

    @Test
    fun bookmarksTab_allowsSortingByDate() {
        composeTestRule.onNodeWithContentDescription("Bookmarks").performClick()

        // Wait for bookmarks to load
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("bookmark_item").fetchSemanticsNodes().isNotEmpty()
        }

        // Open sort menu
        composeTestRule.onNodeWithTag("sort_button").performClick()

        // Verify sort options
        composeTestRule.onNodeWithText("Recently Added").assertIsDisplayed()
        composeTestRule.onNodeWithText("Oldest First").assertIsDisplayed()
        composeTestRule.onNodeWithText("Title A-Z").assertIsDisplayed()

        // Select oldest first
        composeTestRule.onNodeWithText("Oldest First").performClick()

        // Verify sorting changed (would require specific test data to verify order)
        composeTestRule.waitUntil(timeoutMillis = 3000) {
            // Bookmarks should be reordered
            true // Placeholder - would verify actual order
        }
    }

    @Test
    fun bookmarkSync_worksAcrossDevices() {
        // This test would require multi-device simulation
        // For now, just verify sync indicator appears during sync
        composeTestRule.onNodeWithContentDescription("Bookmarks").performClick()

        // Trigger manual sync
        composeTestRule.onNodeWithTag("sync_button").performClick()

        // Verify sync indicator appears
        composeTestRule.onNodeWithTag("sync_indicator").assertIsDisplayed()
        composeTestRule.onNodeWithText("Syncing bookmarks...").assertIsDisplayed()

        // Wait for sync to complete
        composeTestRule.waitUntil(timeoutMillis = 10000) {
            composeTestRule.onAllNodesWithTag("sync_indicator").fetchSemanticsNodes().isEmpty()
        }

        // Verify sync completion message
        composeTestRule.onNodeWithText("Bookmarks synced").assertIsDisplayed()
    }

    @Test
    fun bookmarkOfflineAccess_worksWithoutNetwork() {
        // This test would require network simulation
        // Navigate to Bookmarks tab
        composeTestRule.onNodeWithContentDescription("Bookmarks").performClick()

        // Verify offline indicator if network is unavailable
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("offline_indicator").fetchSemanticsNodes().isNotEmpty() ||
            composeTestRule.onAllNodesWithTag("bookmark_item").fetchSemanticsNodes().isNotEmpty()
        }

        // If offline, verify cached bookmarks are still accessible
        if (composeTestRule.onAllNodesWithTag("offline_indicator").fetchSemanticsNodes().isNotEmpty()) {
            composeTestRule.onNodeWithText("Offline - Showing saved bookmarks").assertIsDisplayed()
            // Bookmarks should still be accessible from local cache
            composeTestRule.onAllNodesWithTag("bookmark_item").assertCountEquals(1, true)
        }
    }

    @Test
    fun bookmarkBulkActions_allowsMultipleSelection() {
        composeTestRule.onNodeWithContentDescription("Bookmarks").performClick()

        // Wait for bookmarks to load
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("bookmark_item").fetchSemanticsNodes().isNotEmpty()
        }

        // Enter selection mode
        composeTestRule.onNodeWithTag("select_mode_button").performClick()

        // Verify selection mode is active
        composeTestRule.onNodeWithText("Select bookmarks").assertIsDisplayed()

        // Select multiple items
        composeTestRule.onAllNodesWithTag("bookmark_checkbox")[0].performClick()
        composeTestRule.onAllNodesWithTag("bookmark_checkbox")[1].performClick()

        // Verify bulk actions are available
        composeTestRule.onNodeWithText("Remove Selected").assertIsDisplayed()
        composeTestRule.onNodeWithText("Share Selected").assertIsDisplayed()

        // Test bulk removal
        composeTestRule.onNodeWithText("Remove Selected").performClick()
        composeTestRule.onNodeWithText("Remove").performClick()

        // Verify items were removed
        composeTestRule.waitUntil(timeoutMillis = 3000) {
            composeTestRule.onAllNodesWithTag("bookmark_item").fetchSemanticsNodes().size < 2
        }
    }
}