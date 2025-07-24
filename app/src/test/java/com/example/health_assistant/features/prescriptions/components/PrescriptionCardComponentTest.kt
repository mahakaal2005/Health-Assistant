package com.example.health_assistant.features.prescriptions.components

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.health_assistant.core.design.components.HealthCardComponent
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Unit tests for prescription card styling components
 * Tests HealthCardComponent integration with prescription display functionality
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class PrescriptionCardComponentTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `test prescription card component creation with primary style`() {
        // Given
        val cardComponent = HealthCardComponent.createPrimaryCard(context)
        
        // When
        val cardType = cardComponent.getCardType()
        
        // Then
        assertEquals(HealthCardComponent.HealthCardType.PRIMARY, cardType)
        assertNotNull(cardComponent)
    }

    @Test
    fun `test prescription card component creation with secondary style`() {
        // Given
        val cardComponent = HealthCardComponent.createSecondaryCard(context)
        
        // When
        val cardType = cardComponent.getCardType()
        
        // Then
        assertEquals(HealthCardComponent.HealthCardType.SECONDARY, cardType)
        assertNotNull(cardComponent)
    }

    @Test
    fun `test prescription card component creation with elevated style`() {
        // Given
        val cardComponent = HealthCardComponent.createElevatedCard(context)
        
        // When
        val cardType = cardComponent.getCardType()
        
        // Then
        assertEquals(HealthCardComponent.HealthCardType.ELEVATED, cardType)
        assertNotNull(cardComponent)
    }

    @Test
    fun `test prescription card component accessibility properties`() {
        // Given
        val cardComponent = HealthCardComponent.createPrimaryCard(context)
        
        // When & Then
        assertEquals(true, cardComponent.isFocusable)
        assertEquals(false, cardComponent.isFocusableInTouchMode)
        assertNotNull(cardComponent.contentDescription)
    }

    @Test
    fun `test prescription card component styling consistency`() {
        // Given
        val primaryCard = HealthCardComponent.createPrimaryCard(context)
        val secondaryCard = HealthCardComponent.createSecondaryCard(context)
        val elevatedCard = HealthCardComponent.createElevatedCard(context)
        
        // When & Then - All cards should have consistent corner radius
        assertEquals(primaryCard.radius, secondaryCard.radius)
        assertEquals(secondaryCard.radius, elevatedCard.radius)
        
        // All cards should have minimum height for accessibility
        assertEquals(true, primaryCard.minimumHeight > 0)
        assertEquals(true, secondaryCard.minimumHeight > 0)
        assertEquals(true, elevatedCard.minimumHeight > 0)
    }
}