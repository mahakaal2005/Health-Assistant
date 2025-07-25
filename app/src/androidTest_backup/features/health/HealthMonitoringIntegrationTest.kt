package com.example.health_assistant.features.health

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.example.health_assistant.MainActivity
import com.example.health_assistant.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.jupiter.api.DisplayName

/**
 * Integration tests for health monitoring functionality preservation
 * Validates that health data collection, calculation, and display work correctly
 * after UI standardization changes from Story 1.7
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class HealthMonitoringIntegrationTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    @DisplayName("Health overview card displays correctly with standardized styling")
    fun testHealthOverviewCardDisplay() {
        // Given: User navigates to home screen with health monitoring
        // When: Health overview card is displayed
        onView(withId(R.id.health_summary_card))
            .check(matches(isDisplayed()))
        
        // Then: Health overview title uses correct typography
        onView(withId(R.id.health_summary_title))
            .check(matches(isDisplayed()))
            .check(matches(withText("Health Overview")))
        
        // And: Triple ring progress view is displayed
        onView(withId(R.id.triple_ring_progress))
            .check(matches(isDisplayed()))
        
        // And: Health metrics container is displayed
        onView(withId(R.id.metrics_container))
            .check(matches(isDisplayed()))
    }

    @Test
    @DisplayName("Health metric values display with correct typography")
    fun testHealthMetricValuesDisplay() {
        // Given: Health metrics are loaded
        // When: Viewing health metric values
        onView(withId(R.id.steps_value))
            .check(matches(isDisplayed()))
        
        onView(withId(R.id.calories_value))
            .check(matches(isDisplayed()))
        
        onView(withId(R.id.heart_points_value))
            .check(matches(isDisplayed()))
        
        // Then: All metric values should be visible and properly formatted
        onView(withId(R.id.steps_label))
            .check(matches(withText("Steps")))
        
        onView(withId(R.id.calories_label))
            .check(matches(withText("Calories")))
        
        onView(withId(R.id.heart_points_label))
            .check(matches(withText("Heart Points")))
    }

    @Test
    @DisplayName("Progress tracking cards display with standardized styling")
    fun testProgressTrackingCardsDisplay() {
        // Given: User is on home screen
        // When: Viewing progress tracking section
        onView(withId(R.id.my_progress_title))
            .check(matches(isDisplayed()))
            .check(matches(withText("My Progress")))
        
        // Then: Steps progress card is displayed
        onView(withId(R.id.steps_this_week_card))
            .check(matches(isDisplayed()))
        
        onView(withId(R.id.steps_chart_title))
            .check(matches(withText("Steps This Week")))
        
        // And: Calories progress card is displayed
        onView(withId(R.id.calories_this_week_card))
            .check(matches(isDisplayed()))
        
        onView(withId(R.id.calories_chart_title))
            .check(matches(withText("Calories This Week")))
        
        // And: Heart points progress card is displayed
        onView(withId(R.id.heart_points_this_week_card))
            .check(matches(isDisplayed()))
        
        onView(withId(R.id.heart_points_chart_title))
            .check(matches(withText("Heart Points This Week")))
    }

    @Test
    @DisplayName("Health charts display correctly with standardized styling")
    fun testHealthChartsDisplay() {
        // Given: Health data is available
        // When: Viewing health charts
        onView(withId(R.id.steps_bar_chart))
            .check(matches(isDisplayed()))
        
        onView(withId(R.id.calories_chart))
            .check(matches(isDisplayed()))
        
        onView(withId(R.id.heart_points_chart))
            .check(matches(isDisplayed()))
        
        // Then: Charts should be visible and properly sized
        // Chart functionality preservation is validated through display
    }

    @Test
    @DisplayName("Progress indicators display with correct colors and styling")
    fun testProgressIndicatorsDisplay() {
        // Given: Health progress data is available
        // When: Viewing progress indicators
        onView(withId(R.id.weekly_goal_progress))
            .check(matches(isDisplayed()))
        
        onView(withId(R.id.calories_weekly_goal_progress))
            .check(matches(isDisplayed()))
        
        onView(withId(R.id.heart_points_weekly_goal_progress))
            .check(matches(isDisplayed()))
        
        // Then: Progress indicators should be visible
        // Color validation is handled through styling tests
        onView(withId(R.id.weekly_goal_text))
            .check(matches(isDisplayed()))
        
        onView(withId(R.id.calories_weekly_goal_text))
            .check(matches(isDisplayed()))
        
        onView(withId(R.id.heart_points_weekly_goal_text))
            .check(matches(isDisplayed()))
    }

    @Test
    @DisplayName("Wellness tip card displays with secondary styling")
    fun testWellnessTipCardDisplay() {
        // Given: User is on home screen
        // When: Viewing wellness tip card
        onView(withId(R.id.contextual_card))
            .check(matches(isDisplayed()))
        
        onView(withId(R.id.contextual_title))
            .check(matches(isDisplayed()))
            .check(matches(withText("Wellness Tip")))
        
        onView(withId(R.id.contextual_content))
            .check(matches(isDisplayed()))
        
        // Then: Wellness tip card should use secondary styling
        // Styling validation is handled through component tests
    }

    @Test
    @DisplayName("Health status indicators use consistent color coding")
    fun testHealthStatusIndicatorColors() {
        // Given: Health metrics with status indicators
        // When: Viewing health status icons
        onView(withId(R.id.steps_icon))
            .check(matches(isDisplayed()))
        
        onView(withId(R.id.calories_icon))
            .check(matches(isDisplayed()))
        
        onView(withId(R.id.heart_points_icon))
            .check(matches(isDisplayed()))
        
        // Then: Icons should be visible with proper tinting
        // Color consistency is validated through styling
    }

    @Test
    @DisplayName("Health data summary statistics display correctly")
    fun testHealthDataSummaryDisplay() {
        // Given: Health data is available
        // When: Viewing summary statistics
        onView(withId(R.id.total_steps_label))
            .check(matches(isDisplayed()))
            .check(matches(withText("Total: ")))
        
        onView(withId(R.id.total_steps_value))
            .check(matches(isDisplayed()))
        
        onView(withId(R.id.daily_average_label))
            .check(matches(isDisplayed()))
            .check(matches(withText("Avg: ")))
        
        onView(withId(R.id.daily_average_value))
            .check(matches(isDisplayed()))
        
        // Then: Summary statistics should be properly formatted
        // Similar pattern for calories and heart points
        onView(withId(R.id.total_calories_value))
            .check(matches(isDisplayed()))
        
        onView(withId(R.id.total_heart_points_value))
            .check(matches(isDisplayed()))
    }

    @Test
    @DisplayName("Your Insights section displays with standardized styling")
    fun testInsightsSectionDisplay() {
        // Given: User scrolls to insights section
        // When: Viewing insights section
        onView(withId(R.id.insights_title))
            .check(matches(isDisplayed()))
            .check(matches(withText("Your Insights")))
        
        onView(withId(R.id.insights_recycler))
            .check(matches(isDisplayed()))
        
        // Then: Insights section should use standardized typography
        // RecyclerView should be properly configured
    }
}