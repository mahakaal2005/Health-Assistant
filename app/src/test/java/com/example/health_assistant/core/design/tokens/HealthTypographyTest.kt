package com.example.health_assistant.core.design.tokens

import com.example.health_assistant.R
import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for HealthTypography design system tokens
 * 
 * Tests typography token consistency, hierarchy, and proper resource references
 */
class HealthTypographyTest {

    @Test
    fun `headline typography should have valid resource references`() {
        // Test that all headline typography tokens reference valid resources
        assertEquals(R.style.TextAppearance_Health_Headline1, HealthTypography.Headline.large)
        assertEquals(R.style.TextAppearance_Health_Headline2, HealthTypography.Headline.medium)
        assertEquals(R.style.TextAppearance_HealthAssistant_Title, HealthTypography.Headline.small)
    }

    @Test
    fun `title typography should have valid resource references`() {
        // Test that all title typography tokens reference valid resources
        assertEquals(R.style.TextAppearance_HealthAssistant_SectionTitle, HealthTypography.Title.large)
        assertEquals(R.style.TextAppearance_HealthAssistant_CardTitle, HealthTypography.Title.medium)
        assertEquals(R.style.TextAppearance_HealthAssistant_Subtitle, HealthTypography.Title.small)
    }

    @Test
    fun `body typography should have valid resource references`() {
        // Test that all body typography tokens reference valid resources
        assertEquals(R.style.TextAppearance_Health_Body1, HealthTypography.Body.large)
        assertEquals(R.style.TextAppearance_Health_Body2, HealthTypography.Body.medium)
        assertEquals(R.style.TextAppearance_HealthAssistant_Body, HealthTypography.Body.small)
    }

    @Test
    fun `caption typography should have valid resource references`() {
        // Test that all caption typography tokens reference valid resources
        assertEquals(R.style.TextAppearance_HealthAssistant_Caption, HealthTypography.Caption.default)
        assertEquals(R.style.TextAppearance_HealthAssistant_TipText, HealthTypography.Caption.tip)
    }

    @Test
    fun `special typography should have valid resource references`() {
        // Test that all special typography tokens reference valid resources
        assertEquals(R.style.TextAppearance_HealthAssistant_Greeting, HealthTypography.Special.greeting)
        assertEquals(R.style.TextAppearance_Health_Accent, HealthTypography.Special.accent)
        assertEquals(R.style.TextAppearance_HealthAssistant_Date, HealthTypography.Special.date)
    }

    @Test
    fun `legacy typography should maintain backward compatibility`() {
        // Test that legacy typography mappings are preserved
        assertEquals(R.style.TextAppearance_Health_Headline1, HealthTypography.Legacy.healthHeadline1)
        assertEquals(R.style.TextAppearance_Health_Headline2, HealthTypography.Legacy.healthHeadline2)
        assertEquals(R.style.TextAppearance_Health_Accent, HealthTypography.Legacy.healthAccent)
        assertEquals(R.style.TextAppearance_Health_Body1, HealthTypography.Legacy.healthBody1)
        assertEquals(R.style.TextAppearance_Health_Body2, HealthTypography.Legacy.healthBody2)
        assertEquals(R.style.TextAppearance_HealthAssistant_Title, HealthTypography.Legacy.healthAssistantTitle)
        assertEquals(R.style.TextAppearance_HealthAssistant_Subtitle, HealthTypography.Legacy.healthAssistantSubtitle)
        assertEquals(R.style.TextAppearance_HealthAssistant_Body, HealthTypography.Legacy.healthAssistantBody)
        assertEquals(R.style.TextAppearance_HealthAssistant_Caption, HealthTypography.Legacy.healthAssistantCaption)
    }

    @Test
    fun `typography hierarchy should be logically organized`() {
        // Test that typography categories follow semantic hierarchy
        
        // Headlines should be distinct across sizes
        assertNotEquals(HealthTypography.Headline.large, HealthTypography.Headline.medium)
        assertNotEquals(HealthTypography.Headline.medium, HealthTypography.Headline.small)
        
        // Titles should be distinct across sizes
        assertNotEquals(HealthTypography.Title.large, HealthTypography.Title.medium)
        assertNotEquals(HealthTypography.Title.medium, HealthTypography.Title.small)
        
        // Body text should be distinct across sizes
        assertNotEquals(HealthTypography.Body.large, HealthTypography.Body.medium)
        assertNotEquals(HealthTypography.Body.medium, HealthTypography.Body.small)
        
        // Caption styles should be distinct
        assertNotEquals(HealthTypography.Caption.default, HealthTypography.Caption.tip)
    }

    @Test
    fun `typography categories should be semantically distinct`() {
        // Test that different typography categories are distinct
        
        // Headlines should be different from titles
        assertNotEquals(HealthTypography.Headline.large, HealthTypography.Title.large)
        assertNotEquals(HealthTypography.Headline.medium, HealthTypography.Title.medium)
        
        // Titles should be different from body text
        assertNotEquals(HealthTypography.Title.large, HealthTypography.Body.large)
        assertNotEquals(HealthTypography.Title.medium, HealthTypography.Body.medium)
        
        // Body text should be different from captions
        assertNotEquals(HealthTypography.Body.small, HealthTypography.Caption.default)
    }

    @Test
    fun `special typography should be unique`() {
        // Test that special typography styles are distinct
        assertNotEquals(HealthTypography.Special.greeting, HealthTypography.Special.accent)
        assertNotEquals(HealthTypography.Special.accent, HealthTypography.Special.date)
        assertNotEquals(HealthTypography.Special.greeting, HealthTypography.Special.date)
        
        // Special styles should be different from regular hierarchy
        assertNotEquals(HealthTypography.Special.greeting, HealthTypography.Headline.large)
        assertNotEquals(HealthTypography.Special.accent, HealthTypography.Title.medium)
    }

    @Test
    fun `typography tokens should support consistent theming`() {
        // Test that typography tokens align with design system expectations
        
        // Legacy mappings should match new tokens
        assertEquals(HealthTypography.Headline.large, HealthTypography.Legacy.healthHeadline1)
        assertEquals(HealthTypography.Headline.medium, HealthTypography.Legacy.healthHeadline2)
        assertEquals(HealthTypography.Body.large, HealthTypography.Legacy.healthBody1)
        assertEquals(HealthTypography.Body.medium, HealthTypography.Legacy.healthBody2)
        
        // Special accent should match legacy accent
        assertEquals(HealthTypography.Special.accent, HealthTypography.Legacy.healthAccent)
    }

    @Test
    fun `typography structure should support accessibility`() {
        // Test that typography structure supports proper hierarchy for accessibility
        
        // Each category should have multiple size options for flexibility
        assertNotNull(HealthTypography.Headline.large)
        assertNotNull(HealthTypography.Headline.medium)
        assertNotNull(HealthTypography.Headline.small)
        
        assertNotNull(HealthTypography.Title.large)
        assertNotNull(HealthTypography.Title.medium)
        assertNotNull(HealthTypography.Title.small)
        
        assertNotNull(HealthTypography.Body.large)
        assertNotNull(HealthTypography.Body.medium)
        assertNotNull(HealthTypography.Body.small)
        
        // Caption options should be available
        assertNotNull(HealthTypography.Caption.default)
        assertNotNull(HealthTypography.Caption.tip)
    }

    @Test
    fun `typography tokens should be comprehensive`() {
        // Test that all major typography use cases are covered
        
        // Main content hierarchy
        assertNotNull(HealthTypography.Headline.large)  // Page titles
        assertNotNull(HealthTypography.Title.medium)    // Card titles
        assertNotNull(HealthTypography.Body.large)      // Main content
        assertNotNull(HealthTypography.Caption.default) // Small text
        
        // Special use cases
        assertNotNull(HealthTypography.Special.greeting) // Welcome messages
        assertNotNull(HealthTypography.Special.accent)   // Highlighted text
        assertNotNull(HealthTypography.Special.date)     // Date displays
    }
}