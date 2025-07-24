package com.example.health_assistant.core.design.tokens

import com.example.health_assistant.R
import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for HealthColors design system tokens
 * 
 * Tests color token accessibility, consistency, and proper resource references
 */
class HealthColorsTest {

    @Test
    fun `primary colors should have valid resource references`() {
        // Test that all primary color tokens reference valid resources
        assertEquals(R.color.health_primary, HealthColors.Primary.default)
        assertEquals(R.color.colorPrimaryVariant, HealthColors.Primary.variant)
        assertEquals(R.color.primary_container, HealthColors.Primary.container)
        assertEquals(R.color.colorOnPrimary, HealthColors.Primary.onPrimary)
    }

    @Test
    fun `secondary colors should have valid resource references`() {
        // Test that all secondary color tokens reference valid resources
        assertEquals(R.color.health_accent, HealthColors.Secondary.default)
        assertEquals(R.color.colorSecondaryVariant, HealthColors.Secondary.variant)
        assertEquals(R.color.secondary_container, HealthColors.Secondary.container)
        assertEquals(R.color.colorOnSecondary, HealthColors.Secondary.onSecondary)
    }

    @Test
    fun `surface colors should have valid resource references`() {
        // Test that all surface color tokens reference valid resources
        assertEquals(R.color.surface_primary, HealthColors.Surface.primary)
        assertEquals(R.color.surface_elevated, HealthColors.Surface.elevated)
        assertEquals(R.color.surface_health, HealthColors.Surface.health)
        assertEquals(R.color.surface_variant, HealthColors.Surface.variant)
        assertEquals(R.color.colorOnSurface, HealthColors.Surface.onSurface)
    }

    @Test
    fun `background colors should have valid resource references`() {
        // Test that all background color tokens reference valid resources
        assertEquals(R.color.background_primary, HealthColors.Background.primary)
        assertEquals(R.color.background_secondary, HealthColors.Background.secondary)
        assertEquals(R.color.colorOnBackground, HealthColors.Background.onBackground)
    }

    @Test
    fun `text colors should have valid resource references`() {
        // Test that all text color tokens reference valid resources
        assertEquals(R.color.text_primary, HealthColors.Text.primary)
        assertEquals(R.color.text_secondary, HealthColors.Text.secondary)
        assertEquals(R.color.textTertiary, HealthColors.Text.tertiary)
        assertEquals(R.color.textDisabled, HealthColors.Text.disabled)
        assertEquals(R.color.text_on_green, HealthColors.Text.onGreen)
    }

    @Test
    fun `card colors should have valid resource references`() {
        // Test that all card color tokens reference valid resources
        assertEquals(R.color.cardBackground, HealthColors.Card.background)
        assertEquals(R.color.card_background_elevated, HealthColors.Card.elevated)
        assertEquals(R.color.card_background_alt, HealthColors.Card.alternative)
        assertEquals(R.color.card_background_translucent, HealthColors.Card.translucent)
    }

    @Test
    fun `semantic colors should have valid resource references`() {
        // Test that all semantic color tokens reference valid resources
        assertEquals(R.color.success, HealthColors.Semantic.success)
        assertEquals(R.color.warning, HealthColors.Semantic.warning)
        assertEquals(R.color.error, HealthColors.Semantic.error)
        assertEquals(R.color.colorOnError, HealthColors.Semantic.onError)
    }

    @Test
    fun `health status colors should have valid resource references`() {
        // Test that all health status color tokens reference valid resources
        assertEquals(R.color.healthExcellent, HealthColors.HealthStatus.excellent)
        assertEquals(R.color.healthGood, HealthColors.HealthStatus.good)
        assertEquals(R.color.healthWarning, HealthColors.HealthStatus.warning)
        assertEquals(R.color.healthPoor, HealthColors.HealthStatus.poor)
    }

    @Test
    fun `interactive colors should have valid resource references`() {
        // Test that all interactive color tokens reference valid resources
        assertEquals(R.color.ripple_color, HealthColors.Interactive.ripple)
        assertEquals(R.color.rippleColorLight, HealthColors.Interactive.rippleLight)
        assertEquals(R.color.outline_variant, HealthColors.Interactive.outline)
        assertEquals(R.color.divider, HealthColors.Interactive.divider)
    }

    @Test
    fun `legacy colors should maintain backward compatibility`() {
        // Test that legacy color mappings are preserved
        assertEquals(R.color.health_primary, HealthColors.Legacy.healthPrimary)
        assertEquals(R.color.health_light, HealthColors.Legacy.healthLight)
        assertEquals(R.color.health_accent, HealthColors.Legacy.healthAccent)
        assertEquals(R.color.health_dark, HealthColors.Legacy.healthDark)
        assertEquals(R.color.primary_color, HealthColors.Legacy.primaryColor)
        assertEquals(R.color.accent_color, HealthColors.Legacy.accentColor)
    }

    @Test
    fun `color token categories should be logically organized`() {
        // Test that color categories follow semantic organization
        
        // Primary and secondary should be different
        assertNotEquals(HealthColors.Primary.default, HealthColors.Secondary.default)
        
        // Surface colors should be distinct
        assertNotEquals(HealthColors.Surface.primary, HealthColors.Surface.elevated)
        assertNotEquals(HealthColors.Surface.primary, HealthColors.Surface.health)
        
        // Text hierarchy should be distinct
        assertNotEquals(HealthColors.Text.primary, HealthColors.Text.secondary)
        assertNotEquals(HealthColors.Text.secondary, HealthColors.Text.tertiary)
        
        // Semantic colors should be distinct
        assertNotEquals(HealthColors.Semantic.success, HealthColors.Semantic.warning)
        assertNotEquals(HealthColors.Semantic.warning, HealthColors.Semantic.error)
    }

    @Test
    fun `health status colors should follow logical progression`() {
        // Test that health status colors are logically distinct
        assertNotEquals(HealthColors.HealthStatus.excellent, HealthColors.HealthStatus.good)
        assertNotEquals(HealthColors.HealthStatus.good, HealthColors.HealthStatus.warning)
        assertNotEquals(HealthColors.HealthStatus.warning, HealthColors.HealthStatus.poor)
    }

    @Test
    fun `color tokens should support theme consistency`() {
        // Test that primary colors align with theme expectations
        assertEquals(HealthColors.Primary.default, HealthColors.Legacy.healthPrimary)
        assertEquals(HealthColors.Secondary.default, HealthColors.Legacy.healthAccent)
        
        // Test that semantic colors are consistent
        assertEquals(HealthColors.Semantic.success, HealthColors.HealthStatus.excellent)
    }
}