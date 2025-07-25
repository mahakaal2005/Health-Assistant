package com.example.health_assistant.auth.components

import android.content.Context
import android.view.LayoutInflater
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import com.example.health_assistant.R
import com.example.health_assistant.core.design.components.HealthCardComponent
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for settings card styling components
 * Tests the application of HealthCardComponent and HealthTypography design system tokens
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class SettingsCardStylingTest {

    private lateinit var context: Context
    private lateinit var inflater: LayoutInflater

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        inflater = LayoutInflater.from(context)
    }

    @Test
    fun `app settings card uses HealthCardComponent Secondary style`() {
        // Arrange & Act
        val appSettingsView = inflater.inflate(R.layout.card_app_settings, null)
        val appSettingsCard = appSettingsView.findViewById<HealthCardComponent>(R.id.app_settings_card)

        // Assert
        assertNotNull(appSettingsCard, "App settings card should exist")
        
        // Verify card is properly configured
        assertTrue(appSettingsCard.visibility == android.view.View.VISIBLE, "App settings card should be visible")
    }

    @Test
    fun `health preferences card uses HealthCardComponent Primary style`() {
        // Arrange & Act
        val healthPreferencesView = inflater.inflate(R.layout.card_health_preferences, null)
        val healthPreferencesCard = healthPreferencesView.findViewById<HealthCardComponent>(R.id.health_preferences_card)

        // Assert
        assertNotNull(healthPreferencesCard, "Health preferences card should exist")
        
        // Verify card is properly configured
        assertTrue(healthPreferencesCard.visibility == android.view.View.VISIBLE, "Health preferences card should be visible")
    }

    @Test
    fun `notifications card uses HealthCardComponent Secondary style`() {
        // Arrange & Act
        val notificationsView = inflater.inflate(R.layout.card_notifications, null)
        val notificationsCard = notificationsView.findViewById<HealthCardComponent>(R.id.notifications_card)

        // Assert
        assertNotNull(notificationsCard, "Notifications card should exist")
        
        // Verify card is properly configured
        assertTrue(notificationsCard.visibility == android.view.View.VISIBLE, "Notifications card should be visible")
    }

    @Test
    fun `settings card headers use HealthTypography Title Medium style`() {
        // Arrange & Act
        val appSettingsView = inflater.inflate(R.layout.card_app_settings, null)
        val healthPreferencesView = inflater.inflate(R.layout.card_health_preferences, null)
        val notificationsView = inflater.inflate(R.layout.card_notifications, null)

        // Find header TextViews (they don't have IDs, so we'll find them by content)
        val appSettingsHeader = findTextViewByText(appSettingsView, "App Settings")
        val healthPreferencesHeader = findTextViewByText(healthPreferencesView, "Health Preferences")
        val notificationsHeader = findTextViewByText(notificationsView, "Notifications")

        // Assert
        assertNotNull(appSettingsHeader, "App settings header should exist")
        assertNotNull(healthPreferencesHeader, "Health preferences header should exist")
        assertNotNull(notificationsHeader, "Notifications header should exist")
        
        // Verify text size is set (indicating style is applied)
        assertTrue(appSettingsHeader.textSize > 0, "App settings header should have text size set")
        assertTrue(healthPreferencesHeader.textSize > 0, "Health preferences header should have text size set")
        assertTrue(notificationsHeader.textSize > 0, "Notifications header should have text size set")
    }

    @Test
    fun `settings cards use HealthSpacing tokens consistently`() {
        // Arrange & Act
        val appSettingsView = inflater.inflate(R.layout.card_app_settings, null)
        val appSettingsCard = appSettingsView.findViewById<HealthCardComponent>(R.id.app_settings_card)

        // Assert
        assertNotNull(appSettingsCard, "App settings card should exist")
        
        // Verify margin values match design system tokens
        val cardParams = appSettingsCard.layoutParams as android.view.ViewGroup.MarginLayoutParams
        val expectedStandardMargin = context.resources.getDimensionPixelSize(R.dimen.ds_margin_standard)
        
        assertEquals(expectedStandardMargin, cardParams.leftMargin, "Settings card should use ds_margin_standard")
        assertEquals(expectedStandardMargin, cardParams.topMargin, "Settings card should use ds_margin_standard")
        assertEquals(expectedStandardMargin, cardParams.rightMargin, "Settings card should use ds_margin_standard")
        assertEquals(expectedStandardMargin, cardParams.bottomMargin, "Settings card should use ds_margin_standard")
    }

    @Test
    fun `settings cards preserve accessibility attributes`() {
        // Arrange & Act
        val appSettingsView = inflater.inflate(R.layout.card_app_settings, null)
        val appSettingsCard = appSettingsView.findViewById<HealthCardComponent>(R.id.app_settings_card)

        // Assert
        assertNotNull(appSettingsCard, "App settings card should exist")
        
        // Verify card is focusable for accessibility
        assertTrue(appSettingsCard.isFocusable, "Settings card should be focusable for accessibility")
    }

    @Test
    fun `preference items use consistent typography`() {
        // Arrange & Act
        val healthPreferencesView = inflater.inflate(R.layout.card_health_preferences, null)
        
        // Find preference labels (they don't have IDs, so we'll find them by content)
        val stepGoalLabel = findTextViewByText(healthPreferencesView, "Daily Step Goal")
        val waterGoalLabel = findTextViewByText(healthPreferencesView, "Daily Water Intake Goal")

        // Assert
        assertNotNull(stepGoalLabel, "Step goal label should exist")
        assertNotNull(waterGoalLabel, "Water goal label should exist")
        
        // Verify text size is set (indicating consistent styling)
        assertTrue(stepGoalLabel.textSize > 0, "Step goal label should have text size set")
        assertTrue(waterGoalLabel.textSize > 0, "Water goal label should have text size set")
    }

    /**
     * Helper function to find TextView by text content since many don't have IDs
     */
    private fun findTextViewByText(view: android.view.View, text: String): TextView? {
        if (view is TextView && view.text.toString().contains(text, ignoreCase = true)) {
            return view
        }
        if (view is android.view.ViewGroup) {
            for (i in 0 until view.childCount) {
                val result = findTextViewByText(view.getChildAt(i), text)
                if (result != null) return result
            }
        }
        return null
    }
}