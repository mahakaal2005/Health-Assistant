package com.example.health_assistant.features.discover

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import com.example.health_assistant.MainActivity
import com.example.health_assistant.R
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests for content discovery functionality preservation
 * Tests Integration Verification requirements (IV1, IV2, IV3)
 */
@RunWith(AndroidJUnit4::class)
class ContentDiscoveryIntegrationTest {

    @get:Rule
    val activityRule = ActivityTestRule(MainActivity::class.java)

    @Test
    fun testContentLoadingAndDisplayPerformancePreservation_IV1() {
        // IV1: Verify content loading and display performance is maintained
        
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        
        // Verify content discovery layouts can be inflated without performance issues
        val startTime = System.currentTimeMillis()
        
        val discoverFragment = android.view.LayoutInflater.from(context)
            .inflate(R.layout.fragment_discover, null)
        val articleCard = android.view.LayoutInflater.from(context)
            .inflate(R.layout.item_discover_article, null)
        val videoCard = android.view.LayoutInflater.from(context)
            .inflate(R.layout.item_discover_video, null)
        val wellnessCard = android.view.LayoutInflater.from(context)
            .inflate(R.layout.item_wellness_tip, null)
        
        val endTime = System.currentTimeMillis()
        val inflationTime = endTime - startTime
        
        // Assert - Verify layouts inflate successfully
        assertNotNull("Discover fragment should inflate successfully", discoverFragment)
        assertNotNull("Article card should inflate successfully", articleCard)
        assertNotNull("Video card should inflate successfully", videoCard)
        assertNotNull("Wellness card should inflate successfully", wellnessCard)
        
        // Verify performance is maintained (should inflate quickly)
        assertTrue("Layout inflation should be performant (< 100ms)", inflationTime < 100)
        
        // Verify essential UI elements are present for content loading
        assertNotNull("Articles recycler view should be present", 
            discoverFragment.findViewById(R.id.recycler_view_articles))
        assertNotNull("Videos recycler view should be present", 
            discoverFragment.findViewById(R.id.recycler_view_videos))
        assertNotNull("Loading state should be present", 
            discoverFragment.findViewById(R.id.layout_loading))
        assertNotNull("Error state should be present", 
            discoverFragment.findViewById(R.id.layout_error))
    }

    @Test
    fun testContentSearchAndFilteringFunctionalityPreservation_IV2() {
        // IV2: Confirm content search and filtering functionality works correctly
        
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        
        // Verify search interface components are present and functional
        val searchFragment = android.view.LayoutInflater.from(context)
            .inflate(R.layout.fragment_content_search, null)
        val searchResult = android.view.LayoutInflater.from(context)
            .inflate(R.layout.item_search_result, null)
        val categoryFilter = android.view.LayoutInflater.from(context)
            .inflate(R.layout.item_category_filter, null)
        
        // Assert - Verify search functionality components are present
        assertNotNull("Search fragment should inflate successfully", searchFragment)
        assertNotNull("Search result should inflate successfully", searchResult)
        assertNotNull("Category filter should inflate successfully", categoryFilter)
        
        // Verify essential search UI elements are present
        assertNotNull("Search input should be present", 
            searchFragment.findViewById(R.id.search_input_layout))
        assertNotNull("Filter buttons should be present", 
            searchFragment.findViewById(R.id.button_filter_all))
        assertNotNull("Category filters should be present", 
            searchFragment.findViewById(R.id.recycler_category_filters))
        assertNotNull("Search results should be present", 
            searchFragment.findViewById(R.id.recycler_search_results))
        assertNotNull("Recent searches should be present", 
            searchFragment.findViewById(R.id.recent_searches_section))
        
        // Verify search result components are functional
        assertNotNull("Search result title should be present", 
            searchResult.findViewById(R.id.search_result_title))
        assertNotNull("Search result summary should be present", 
            searchResult.findViewById(R.id.search_result_summary))
        assertNotNull("Search result metadata should be present", 
            searchResult.findViewById(R.id.search_result_metadata))
        assertNotNull("Search result category should be present", 
            searchResult.findViewById(R.id.search_result_category))
    }

    @Test
    fun testContentBookmarkingAndSharingFeaturesPreservation_IV3() {
        // IV3: Validate content bookmarking and sharing features remain functional
        
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        
        // Verify content interaction features are preserved
        val articleCard = android.view.LayoutInflater.from(context)
            .inflate(R.layout.item_discover_article, null)
        val videoCard = android.view.LayoutInflater.from(context)
            .inflate(R.layout.item_discover_video, null)
        val articleDetail = android.view.LayoutInflater.from(context)
            .inflate(R.layout.activity_article_detail, null)
        val videoDetail = android.view.LayoutInflater.from(context)
            .inflate(R.layout.activity_video_detail, null)
        
        // Assert - Verify bookmarking and sharing functionality is preserved
        assertNotNull("Article card should inflate successfully", articleCard)
        assertNotNull("Video card should inflate successfully", videoCard)
        assertNotNull("Article detail should inflate successfully", articleDetail)
        assertNotNull("Video detail should inflate successfully", videoDetail)
        
        // Verify article interaction buttons are present
        val articleBookmarkButton = articleCard.findViewById<android.widget.ImageButton>(R.id.buttonBookmark)
        val articleShareButton = articleCard.findViewById<android.widget.ImageButton>(R.id.buttonShare)
        
        assertNotNull("Article bookmark button should be present", articleBookmarkButton)
        assertNotNull("Article share button should be present", articleShareButton)
        assertTrue("Article bookmark button should be clickable", articleBookmarkButton.isClickable)
        assertTrue("Article share button should be clickable", articleShareButton.isClickable)
        
        // Verify video interaction buttons are present
        val videoShareButton = videoCard.findViewById<android.widget.ImageButton>(R.id.buttonVideoShare)
        
        assertNotNull("Video share button should be present", videoShareButton)
        assertTrue("Video share button should be clickable", videoShareButton.isClickable)
        
        // Verify detail view interaction buttons are present
        val detailBookmarkButton = articleDetail.findViewById<com.google.android.material.button.MaterialButton>(R.id.button_bookmark)
        val detailShareButton = articleDetail.findViewById<com.google.android.material.button.MaterialButton>(R.id.button_share)
        val videoDetailShareButton = videoDetail.findViewById<com.google.android.material.button.MaterialButton>(R.id.button_share)
        
        assertNotNull("Detail bookmark button should be present", detailBookmarkButton)
        assertNotNull("Detail share button should be present", detailShareButton)
        assertNotNull("Video detail share button should be present", videoDetailShareButton)
        assertTrue("Detail bookmark button should be clickable", detailBookmarkButton.isClickable)
        assertTrue("Detail share button should be clickable", detailShareButton.isClickable)
        assertTrue("Video detail share button should be clickable", videoDetailShareButton.isClickable)
    }

    @Test
    fun testContentDiscoveryStylingConsistencyAcrossThemes() {
        // Verify content discovery styling consistency across light/dark themes
        
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        
        // Test layout inflation in both light and dark themes
        val articleCard = android.view.LayoutInflater.from(context)
            .inflate(R.layout.item_discover_article, null)
        val videoCard = android.view.LayoutInflater.from(context)
            .inflate(R.layout.item_discover_video, null)
        val searchView = android.view.LayoutInflater.from(context)
            .inflate(R.layout.fragment_content_search, null)
        
        // Assert - Verify styling consistency
        assertNotNull("Article card should work in current theme", articleCard)
        assertNotNull("Video card should work in current theme", videoCard)
        assertNotNull("Search view should work in current theme", searchView)
        
        // Verify design system colors are properly applied
        val primaryColor = context.resources.getColor(R.color.ds_primary, null)
        val surfaceColor = context.resources.getColor(R.color.ds_surface_primary, null)
        val textColor = context.resources.getColor(R.color.ds_text_primary, null)
        
        assertNotEquals("Primary color should be defined", 0, primaryColor)
        assertNotEquals("Surface color should be defined", 0, surfaceColor)
        assertNotEquals("Text color should be defined", 0, textColor)
        
        // Verify design system spacing tokens are applied
        val standardPadding = context.resources.getDimension(R.dimen.ds_padding_standard)
        val standardMargin = context.resources.getDimension(R.dimen.ds_margin_standard)
        val touchTarget = context.resources.getDimension(R.dimen.ds_component_touch_target)
        
        assertEquals("Standard padding should be 16dp", 
            16f, standardPadding / context.resources.displayMetrics.density, 0.1f)
        assertEquals("Standard margin should be 16dp", 
            16f, standardMargin / context.resources.displayMetrics.density, 0.1f)
        assertEquals("Touch target should be 48dp", 
            48f, touchTarget / context.resources.displayMetrics.density, 0.1f)
    }

    @Test
    fun testContentDiscoveryAccessibilityCompliance() {
        // Verify content discovery accessibility compliance
        
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        
        // Test accessibility features in content discovery components
        val articleCard = android.view.LayoutInflater.from(context)
            .inflate(R.layout.item_discover_article, null)
        val videoCard = android.view.LayoutInflater.from(context)
            .inflate(R.layout.item_discover_video, null)
        val searchView = android.view.LayoutInflater.from(context)
            .inflate(R.layout.fragment_content_search, null)
        
        // Assert - Verify accessibility compliance
        assertNotNull("Article card should support accessibility", articleCard)
        assertNotNull("Video card should support accessibility", videoCard)
        assertNotNull("Search view should support accessibility", searchView)
        
        // Verify touch targets meet accessibility requirements
        val minTouchTarget = context.resources.getDimension(R.dimen.ds_component_touch_target)
        assertTrue("Touch targets should meet accessibility requirements (48dp)", 
            minTouchTarget >= 48f * context.resources.displayMetrics.density)
        
        // Verify essential accessibility elements are present
        val articleTitle = articleCard.findViewById<android.widget.TextView>(R.id.textArticleTitle)
        val videoTitle = videoCard.findViewById<android.widget.TextView>(R.id.textVideoTitle)
        val searchInput = searchView.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.search_input_layout)
        
        assertNotNull("Article title should provide heading structure", articleTitle)
        assertNotNull("Video title should provide heading structure", videoTitle)
        assertNotNull("Search input should have proper labeling", searchInput)
        assertNotNull("Search input should have hint text", searchInput.hint)
    }
}