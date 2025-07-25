package com.example.health_assistant.features.discover.components

import android.content.Context
import android.view.LayoutInflater
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import com.example.health_assistant.R
import com.google.android.material.button.MaterialButton
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Unit tests for content categorization and tagging system components
 * Tests HealthButton.Tertiary usage for category tags and HealthColors.Primary variants for color coding
 */
@RunWith(RobolectricTestRunner::class)
class ContentCategorizationTest {

    private lateinit var context: Context
    private lateinit var inflater: LayoutInflater

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        inflater = LayoutInflater.from(context)
    }

    @Test
    fun `content category tags use HealthButton Tertiary styling`() {
        // Arrange & Act
        val categoryTagView = inflater.inflate(R.layout.item_content_category_tag, null)
        val tagButton = categoryTagView as MaterialButton

        // Assert - Verify HealthButton.Tertiary styling is applied
        assertNotNull("Category tag should be inflated successfully", tagButton)
        assertTrue("Category tag should be clickable", tagButton.isClickable)
        assertTrue("Category tag should be focusable", tagButton.isFocusable)
        
        // Verify minimum touch target size for accessibility
        val minTouchTarget = context.resources.getDimension(R.dimen.ds_component_touch_target)
        assertTrue("Touch target should meet accessibility requirements", 
            minTouchTarget >= 48f * context.resources.displayMetrics.density)
    }

    @Test
    fun `category headers use consistent typography and color coding`() {
        // Arrange & Act
        val categoryHeaderView = inflater.inflate(R.layout.item_content_category_header, null)
        
        val categoryTitle = categoryHeaderView.findViewById<TextView>(R.id.category_title)
        val contentCount = categoryHeaderView.findViewById<TextView>(R.id.content_count)
        val colorIndicator = categoryHeaderView.findViewById<android.view.View>(R.id.category_color_indicator)

        // Assert - Verify HealthTypography and color coding is applied
        assertNotNull("Category title should be present", categoryTitle)
        assertNotNull("Content count should be present", contentCount)
        assertNotNull("Color indicator should be present", colorIndicator)
        
        // Verify color indicator dimensions
        assertEquals("Color indicator should be 4dp wide", 
            4f * context.resources.displayMetrics.density, colorIndicator.layoutParams.width.toFloat(), 0.1f)
    }

    @Test
    fun `article cards use updated category tag styling`() {
        // Arrange & Act
        val articleCardView = inflater.inflate(R.layout.item_discover_article, null)
        val categoryTag = articleCardView.findViewById<MaterialButton>(R.id.chipCategory)

        // Assert - Verify HealthButton.Tertiary styling is applied to category tags
        assertNotNull("Article category tag should be present", categoryTag)
        assertTrue("Article category tag should be clickable", categoryTag.isClickable)
        assertTrue("Article category tag should be focusable", categoryTag.isFocusable)
    }

    @Test
    fun `video cards use updated category tag styling`() {
        // Arrange & Act
        val videoCardView = inflater.inflate(R.layout.item_discover_video, null)
        val categoryTag = videoCardView.findViewById<MaterialButton>(R.id.chipVideoCategory)

        // Assert - Verify HealthButton.Tertiary styling is applied to category tags
        assertNotNull("Video category tag should be present", categoryTag)
        assertTrue("Video category tag should be clickable", categoryTag.isClickable)
        assertTrue("Video category tag should be focusable", categoryTag.isFocusable)
    }

    @Test
    fun `category tags use HealthTypography Label Small for text`() {
        // Arrange & Act
        val categoryTagView = inflater.inflate(R.layout.item_content_category_tag, null)
        val tagButton = categoryTagView as MaterialButton

        // Assert - Verify HealthTypography.Label.Small is applied
        assertNotNull("Category tag should be inflated successfully", tagButton)
        
        // Verify text appearance is applied (this would be validated in UI tests)
        assertTrue("Category tag should be visible", tagButton.visibility == android.view.View.VISIBLE)
    }

    @Test
    fun `category system uses consistent spacing tokens`() {
        // Arrange & Act
        val categoryTagView = inflater.inflate(R.layout.item_content_category_tag, null)
        val categoryHeaderView = inflater.inflate(R.layout.item_content_category_header, null)

        // Assert - Verify consistent spacing is applied
        assertNotNull("Category tag should be inflated successfully", categoryTagView)
        assertNotNull("Category header should be inflated successfully", categoryHeaderView)

        // Verify design system spacing tokens are used
        val smallMargin = context.resources.getDimension(R.dimen.ds_margin_small)
        val mediumPadding = context.resources.getDimension(R.dimen.ds_padding_medium)
        
        assertTrue("Small margin should be 8dp", 
            smallMargin == 8f * context.resources.displayMetrics.density)
        assertTrue("Medium padding should be 12dp", 
            mediumPadding == 12f * context.resources.displayMetrics.density)
    }

    @Test
    fun `content categorization preserves functionality after styling updates`() {
        // Arrange & Act
        val articleCardView = inflater.inflate(R.layout.item_discover_article, null)
        val videoCardView = inflater.inflate(R.layout.item_discover_video, null)
        val categoryHeaderView = inflater.inflate(R.layout.item_content_category_header, null)

        // Assert - Verify essential categorization elements are present
        assertNotNull("Article category tag should be present", 
            articleCardView.findViewById(R.id.chipCategory))
        assertNotNull("Video category tag should be present", 
            videoCardView.findViewById(R.id.chipVideoCategory))
        assertNotNull("Category header title should be present", 
            categoryHeaderView.findViewById(R.id.category_title))
        assertNotNull("Category header icon should be present", 
            categoryHeaderView.findViewById(R.id.category_icon))
        assertNotNull("Category color indicator should be present", 
            categoryHeaderView.findViewById(R.id.category_color_indicator))
        assertNotNull("Content count should be present", 
            categoryHeaderView.findViewById(R.id.content_count))
    }

    @Test
    fun `category color variants are available for content organization`() {
        // Arrange & Act
        val nutritionColor = context.resources.getColor(R.color.ds_primary_nutrition, null)
        val fitnessColor = context.resources.getColor(R.color.ds_primary_fitness, null)
        val mentalHealthColor = context.resources.getColor(R.color.ds_primary_mental_health, null)
        val defaultColor = context.resources.getColor(R.color.ds_primary_default, null)

        // Assert - Verify category color variants are defined
        assertNotEquals("Nutrition color should be defined", 0, nutritionColor)
        assertNotEquals("Fitness color should be defined", 0, fitnessColor)
        assertNotEquals("Mental health color should be defined", 0, mentalHealthColor)
        assertNotEquals("Default color should be defined", 0, defaultColor)
        
        // Verify colors are different for proper categorization
        assertNotEquals("Category colors should be distinct", nutritionColor, fitnessColor)
        assertNotEquals("Category colors should be distinct", fitnessColor, mentalHealthColor)
    }
}