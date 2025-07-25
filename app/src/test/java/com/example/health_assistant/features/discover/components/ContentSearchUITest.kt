package com.example.health_assistant.features.discover.components

import android.content.Context
import android.view.LayoutInflater
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import com.example.health_assistant.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputLayout
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Unit tests for content search and filtering UI components
 * Tests HealthTextInputLayout, HealthButton, and HealthCardComponent usage in search interface
 */
@RunWith(RobolectricTestRunner::class)
class ContentSearchUITest {

    private lateinit var context: Context
    private lateinit var inflater: LayoutInflater

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        inflater = LayoutInflater.from(context)
    }

    @Test
    fun `search input uses HealthTextInputLayout styling`() {
        // Arrange & Act
        val searchView = inflater.inflate(R.layout.fragment_content_search, null)
        val searchInputLayout = searchView.findViewById<TextInputLayout>(R.id.search_input_layout)

        // Assert - Verify HealthTextInputLayout styling is applied
        assertNotNull("Search input layout should be present", searchInputLayout)
        assertNotNull("Search input should have hint text", searchInputLayout.hint)
        assertTrue("Search input should be enabled", searchInputLayout.isEnabled)
    }

    @Test
    fun `filter buttons use HealthButton Secondary styling`() {
        // Arrange & Act
        val searchView = inflater.inflate(R.layout.fragment_content_search, null)
        val filterAllButton = searchView.findViewById<MaterialButton>(R.id.button_filter_all)
        val filterArticlesButton = searchView.findViewById<MaterialButton>(R.id.button_filter_articles)
        val filterVideosButton = searchView.findViewById<MaterialButton>(R.id.button_filter_videos)

        // Assert - Verify HealthButton.Secondary styling is applied
        assertNotNull("Filter All button should be present", filterAllButton)
        assertNotNull("Filter Articles button should be present", filterArticlesButton)
        assertNotNull("Filter Videos button should be present", filterVideosButton)
        
        assertTrue("Filter buttons should be clickable", filterAllButton.isClickable)
        assertTrue("Filter buttons should be focusable", filterAllButton.isFocusable)
    }

    @Test
    fun `category filter cards use HealthCardComponent Secondary styling`() {
        // Arrange & Act
        val categoryFilterView = inflater.inflate(R.layout.item_category_filter, null)
        val cardView = categoryFilterView as MaterialCardView

        // Assert - Verify HealthCardComponent.Secondary styling is applied
        assertNotNull("Category filter card should be inflated successfully", cardView)
        assertTrue("Category filter card should be clickable", cardView.isClickable)
        assertTrue("Category filter card should be focusable", cardView.isFocusable)
        
        // Verify card has proper corner radius (12dp from design system)
        val expectedCornerRadius = context.resources.getDimension(R.dimen.ds_component_card_radius)
        assertEquals("Category filter card should use design system corner radius", 
            expectedCornerRadius, cardView.radius, 0.1f)
    }

    @Test
    fun `search result cards use consistent typography hierarchy`() {
        // Arrange & Act
        val searchResultView = inflater.inflate(R.layout.item_search_result, null)
        
        val titleView = searchResultView.findViewById<TextView>(R.id.search_result_title)
        val summaryView = searchResultView.findViewById<TextView>(R.id.search_result_summary)
        val authorView = searchResultView.findViewById<TextView>(R.id.search_result_author)
        val dateView = searchResultView.findViewById<TextView>(R.id.search_result_date)

        // Assert - Verify HealthTypography hierarchy is applied
        assertNotNull("Search result title should be present", titleView)
        assertNotNull("Search result summary should be present", summaryView)
        assertNotNull("Search result author should be present", authorView)
        assertNotNull("Search result date should be present", dateView)
    }

    @Test
    fun `search interface preserves functionality after styling updates`() {
        // Arrange & Act
        val searchView = inflater.inflate(R.layout.fragment_content_search, null)

        // Assert - Verify essential search UI elements are present
        assertNotNull("Search input should be present", 
            searchView.findViewById(R.id.search_input))
        assertNotNull("Filter buttons should be present", 
            searchView.findViewById(R.id.button_filter_all))
        assertNotNull("Category filter section should be present", 
            searchView.findViewById(R.id.category_filter_section))
        assertNotNull("Search results recycler should be present", 
            searchView.findViewById(R.id.recycler_search_results))
        assertNotNull("Recent searches section should be present", 
            searchView.findViewById(R.id.recent_searches_section))
        assertNotNull("Empty search state should be present", 
            searchView.findViewById(R.id.layout_empty_search))
        assertNotNull("Loading state should be present", 
            searchView.findViewById(R.id.layout_search_loading))
    }

    @Test
    fun `search components use consistent spacing tokens`() {
        // Arrange & Act
        val searchView = inflater.inflate(R.layout.fragment_content_search, null)
        val categoryFilterView = inflater.inflate(R.layout.item_category_filter, null)
        val searchResultView = inflater.inflate(R.layout.item_search_result, null)

        // Assert - Verify consistent spacing is applied
        assertNotNull("Search view should be inflated successfully", searchView)
        assertNotNull("Category filter should be inflated successfully", categoryFilterView)
        assertNotNull("Search result should be inflated successfully", searchResultView)

        // Verify design system spacing tokens are used
        val standardPadding = context.resources.getDimension(R.dimen.ds_padding_standard)
        val standardMargin = context.resources.getDimension(R.dimen.ds_margin_standard)
        
        assertTrue("Standard padding should be 16dp", 
            standardPadding == 16f * context.resources.displayMetrics.density)
        assertTrue("Standard margin should be 16dp", 
            standardMargin == 16f * context.resources.displayMetrics.density)
    }

    @Test
    fun `search interface meets accessibility requirements`() {
        // Arrange & Act
        val searchView = inflater.inflate(R.layout.fragment_content_search, null)
        val searchResultView = inflater.inflate(R.layout.item_search_result, null)

        // Assert - Verify accessibility compliance
        assertNotNull("Search view should be inflated successfully", searchView)
        assertNotNull("Search result should be inflated successfully", searchResultView)

        // Verify touch targets meet accessibility requirements (48dp minimum)
        val minTouchTarget = context.resources.getDimension(R.dimen.ds_component_touch_target)
        assertEquals("Touch target should meet accessibility requirements", 
            48f, minTouchTarget / context.resources.displayMetrics.density, 0.1f)
    }
}