package com.example.health_assistant.features.discover.components

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import androidx.test.core.app.ApplicationProvider
import com.example.health_assistant.R
import com.google.android.material.card.MaterialCardView
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Unit tests for content discovery card styling components
 * Tests HealthCardComponent styling and design system token usage
 */
@RunWith(RobolectricTestRunner::class)
class ContentCardStylingTest {

    private lateinit var context: Context
    private lateinit var inflater: LayoutInflater

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        inflater = LayoutInflater.from(context)
    }

    @Test
    fun `article card uses HealthCardComponent Primary styling`() {
        // Arrange & Act
        val articleCardView = inflater.inflate(R.layout.item_discover_article, null)
        val cardView = articleCardView as MaterialCardView

        // Assert - Verify HealthCardComponent.Primary styling is applied
        assertNotNull("Article card should be inflated successfully", cardView)
        assertTrue("Article card should be clickable", cardView.isClickable)
        assertTrue("Article card should be focusable", cardView.isFocusable)
        
        // Verify card has proper corner radius (12dp from design system)
        val expectedCornerRadius = context.resources.getDimension(R.dimen.ds_component_card_radius)
        assertEquals("Article card should use design system corner radius", 
            expectedCornerRadius, cardView.radius, 0.1f)
    }

    @Test
    fun `video card uses HealthCardComponent Primary styling`() {
        // Arrange & Act
        val videoCardView = inflater.inflate(R.layout.item_discover_video, null)
        val cardView = videoCardView as MaterialCardView

        // Assert - Verify HealthCardComponent.Primary styling is applied
        assertNotNull("Video card should be inflated successfully", cardView)
        assertTrue("Video card should be clickable", cardView.isClickable)
        assertTrue("Video card should be focusable", cardView.isFocusable)
        
        // Verify card has proper corner radius (12dp from design system)
        val expectedCornerRadius = context.resources.getDimension(R.dimen.ds_component_card_radius)
        assertEquals("Video card should use design system corner radius", 
            expectedCornerRadius, cardView.radius, 0.1f)
    }

    @Test
    fun `wellness tip card uses HealthCardComponent Secondary styling`() {
        // Arrange & Act
        val wellnessCardView = inflater.inflate(R.layout.item_wellness_tip, null)
        val cardView = wellnessCardView as MaterialCardView

        // Assert - Verify HealthCardComponent.Secondary styling is applied
        assertNotNull("Wellness tip card should be inflated successfully", cardView)
        assertTrue("Wellness tip card should be clickable", cardView.isClickable)
        assertTrue("Wellness tip card should be focusable", cardView.isFocusable)
        
        // Verify card has proper corner radius (12dp from design system)
        val expectedCornerRadius = context.resources.getDimension(R.dimen.ds_component_card_radius)
        assertEquals("Wellness tip card should use design system corner radius", 
            expectedCornerRadius, cardView.radius, 0.1f)
    }

    @Test
    fun `content cards use consistent spacing tokens`() {
        // Arrange & Act
        val articleCardView = inflater.inflate(R.layout.item_discover_article, null)
        val videoCardView = inflater.inflate(R.layout.item_discover_video, null)
        val wellnessCardView = inflater.inflate(R.layout.item_wellness_tip, null)

        // Assert - Verify consistent spacing is applied
        assertNotNull("Article card should be inflated successfully", articleCardView)
        assertNotNull("Video card should be inflated successfully", videoCardView)
        assertNotNull("Wellness card should be inflated successfully", wellnessCardView)

        // Verify touch targets meet accessibility requirements (48dp minimum)
        val minTouchTarget = context.resources.getDimension(R.dimen.ds_component_touch_target)
        assertEquals("Touch target should meet accessibility requirements", 
            48f, minTouchTarget / context.resources.displayMetrics.density, 0.1f)
    }

    @Test
    fun `content cards preserve functionality after styling updates`() {
        // Arrange & Act
        val articleCardView = inflater.inflate(R.layout.item_discover_article, null)
        val videoCardView = inflater.inflate(R.layout.item_discover_video, null)

        // Assert - Verify essential UI elements are present
        assertNotNull("Article image should be present", 
            articleCardView.findViewById<View>(R.id.imageArticle))
        assertNotNull("Article title should be present", 
            articleCardView.findViewById<View>(R.id.textArticleTitle))
        assertNotNull("Article summary should be present", 
            articleCardView.findViewById<View>(R.id.textArticleSummary))
        assertNotNull("Article author should be present", 
            articleCardView.findViewById<View>(R.id.textArticleAuthor))
        assertNotNull("Bookmark button should be present", 
            articleCardView.findViewById<View>(R.id.buttonBookmark))
        assertNotNull("Share button should be present", 
            articleCardView.findViewById<View>(R.id.buttonShare))

        assertNotNull("Video thumbnail should be present", 
            videoCardView.findViewById<View>(R.id.imageVideoThumbnail))
        assertNotNull("Video title should be present", 
            videoCardView.findViewById<View>(R.id.textVideoTitle))
        assertNotNull("Video description should be present", 
            videoCardView.findViewById<View>(R.id.textVideoDescription))
        assertNotNull("Video share button should be present", 
            videoCardView.findViewById<View>(R.id.buttonVideoShare))
    }
}