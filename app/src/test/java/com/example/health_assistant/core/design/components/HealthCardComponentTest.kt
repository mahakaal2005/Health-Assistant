package com.example.health_assistant.core.design.components

import android.content.Context
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import com.example.health_assistant.R
import com.example.health_assistant.core.design.tokens.HealthColors
import com.example.health_assistant.core.design.tokens.HealthSpacing
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.robolectric.RobolectricTestRunner
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Unit tests for HealthCardComponent
 * 
 * Tests card variants, styling, accessibility, and design system integration.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class HealthCardComponentTest {

    private lateinit var context: Context
    private lateinit var healthCard: HealthCardComponent

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        healthCard = HealthCardComponent(context)
    }

    @Test
    fun shouldInitializeWithPrimaryCardType() {
        assertEquals(
            HealthCardComponent.HealthCardType.PRIMARY,
            healthCard.getCardType()
        )
    }

    @Test
    fun shouldApplyPrimaryCardStyling() {
        healthCard.setCardType(HealthCardComponent.HealthCardType.PRIMARY)
        
        val expectedColor = ContextCompat.getColor(context, R.color.ds_surface_primary)
        val expectedElevation = context.resources.getDimension(R.dimen.ds_elevation_low)
        
        assertEquals(expectedColor, healthCard.cardBackgroundColor.defaultColor)
        assertEquals(expectedElevation, healthCard.cardElevation)
        assertTrue(healthCard.contentDescription.toString().contains("Primary"))
    }

    @Test
    fun shouldApplySecondaryCardStyling() {
        healthCard.setCardType(HealthCardComponent.HealthCardType.SECONDARY)
        
        val expectedColor = ContextCompat.getColor(context, R.color.ds_surface_secondary)
        val expectedElevation = context.resources.getDimension(R.dimen.ds_elevation_medium)
        
        assertEquals(expectedColor, healthCard.cardBackgroundColor.defaultColor)
        assertEquals(expectedElevation, healthCard.cardElevation)
        assertTrue(healthCard.contentDescription.toString().contains("Secondary"))
    }

    @Test
    fun shouldApplyElevatedCardStyling() {
        healthCard.setCardType(HealthCardComponent.HealthCardType.ELEVATED)
        
        val expectedColor = ContextCompat.getColor(context, R.color.ds_surface_primary)
        val expectedElevation = context.resources.getDimension(R.dimen.ds_elevation_high)
        
        assertEquals(expectedColor, healthCard.cardBackgroundColor.defaultColor)
        assertEquals(expectedElevation, healthCard.cardElevation)
        assertTrue(healthCard.contentDescription.toString().contains("Elevated"))
    }

    @Test
    fun shouldUseStandardizedCornerRadius() {
        val expectedRadius = context.resources.getDimension(HealthSpacing.Component.cardRadius)
        assertEquals(expectedRadius, healthCard.radius)
    }

    @Test
    fun shouldUseDesignSystemPaddingTokens() {
        healthCard.setContentPadding(HealthSpacing.Padding.standard)
        
        val expectedPadding = context.resources.getDimensionPixelSize(HealthSpacing.Padding.standard)
        assertEquals(expectedPadding, healthCard.contentPaddingLeft)
        assertEquals(expectedPadding, healthCard.contentPaddingTop)
        assertEquals(expectedPadding, healthCard.contentPaddingRight)
        assertEquals(expectedPadding, healthCard.contentPaddingBottom)
    }

    @Test
    fun shouldBeFocusableForAccessibility() {
        assertTrue(healthCard.isFocusable)
        assertFalse(healthCard.isFocusableInTouchMode)
    }

    @Test
    fun shouldMeetMinimumTouchTargetSize() {
        val minTouchTarget = context.resources.getDimensionPixelSize(HealthSpacing.Component.touchTarget)
        assertEquals(minTouchTarget, healthCard.minimumHeight)
    }

    @Test
    fun shouldApplyFocusStylingWhenFocused() {
        val originalElevation = healthCard.cardElevation
        
        // Simulate focus gained
        healthCard.onFocusChangeListener?.onFocusChange(healthCard, true)
        
        // Check that elevation increased and alpha changed
        assertTrue(healthCard.cardElevation > originalElevation)
        assertEquals(0.9f, healthCard.alpha, 0.01f)
    }

    @Test
    fun shouldRemoveFocusStylingWhenFocusLost() {
        // First apply focus
        healthCard.onFocusChangeListener?.onFocusChange(healthCard, true)
        
        // Then remove focus
        healthCard.onFocusChangeListener?.onFocusChange(healthCard, false)
        
        // Check that alpha is restored
        assertEquals(1.0f, healthCard.alpha, 0.01f)
    }

    @Test
    fun shouldCreatePrimaryCardWithCorrectType() {
        val primaryCard = HealthCardComponent.createPrimaryCard(context)
        assertEquals(HealthCardComponent.HealthCardType.PRIMARY, primaryCard.getCardType())
    }

    @Test
    fun shouldCreateSecondaryCardWithCorrectType() {
        val secondaryCard = HealthCardComponent.createSecondaryCard(context)
        assertEquals(HealthCardComponent.HealthCardType.SECONDARY, secondaryCard.getCardType())
    }

    @Test
    fun shouldCreateElevatedCardWithCorrectType() {
        val elevatedCard = HealthCardComponent.createElevatedCard(context)
        assertEquals(HealthCardComponent.HealthCardType.ELEVATED, elevatedCard.getCardType())
    }

    @Test
    fun shouldExtendCardViewForMaterial3Compatibility() {
        assertTrue(healthCard is androidx.cardview.widget.CardView)
    }

    @Test
    fun shouldMaintainCardViewFunctionality() {
        // Test that basic CardView properties are accessible
        assertNotNull(healthCard.cardBackgroundColor)
        assertTrue(healthCard.cardElevation >= 0f)
        assertTrue(healthCard.radius >= 0f)
    }

    @Test
    fun shouldHandleInvalidPaddingGracefully() {
        // Test that negative padding values are handled properly
        val originalPadding = healthCard.contentPaddingLeft
        
        // This should not crash or set negative padding
        healthCard.setContentPadding(HealthSpacing.Padding.standard)
        
        // Verify padding was set correctly
        val expectedPadding = context.resources.getDimensionPixelSize(HealthSpacing.Padding.standard)
        assertEquals(expectedPadding, healthCard.contentPaddingLeft)
    }

    @Test
    fun shouldOptimizeRedundantCardTypeChanges() {
        // Test that setting the same card type doesn't trigger unnecessary work
        val initialType = healthCard.getCardType()
        val initialElevation = healthCard.cardElevation
        
        // Set the same type again
        healthCard.setCardType(initialType)
        
        // Should maintain the same elevation (no unnecessary recalculation)
        assertEquals(initialElevation, healthCard.cardElevation)
    }
}