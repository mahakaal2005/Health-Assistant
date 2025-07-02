package com.example.health_assistant.data.repository.interfaces

import com.example.health_assistant.core.util.Result
import com.example.health_assistant.features.health.model.HealthMetrics
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing health metrics data
 * Includes Google Fit API integration for real-time health data
 */
interface HealthRepository {

    /**
     * Get daily health metrics for a specific date
     */
    fun getDailyHealthMetrics(date: String): Flow<Result<HealthMetrics?>>

    /**
     * Save daily health metrics
     */
    suspend fun saveDailyHealthMetrics(healthMetrics: HealthMetrics): Result<Unit>

    /**
     * Get health metrics for a date range
     */
    suspend fun getHealthMetricsRange(startDate: String, endDate: String): Result<List<HealthMetrics>>

    /**
     * Update step count for today
     */
    suspend fun updateStepCount(steps: Int): Result<Unit>

    /**
     * Update water intake for today
     */
    suspend fun updateWaterIntake(waterIntake: Float): Result<Unit>

    /**
     * Update sleep duration for today
     */
    suspend fun updateSleepDuration(sleepHours: Float): Result<Unit>

    /**
     * Sync health data from external sources
     */
    suspend fun syncHealthData(): Result<Unit>

    /**
     * NEW: Sync today's health metrics from Google Fit API
     */
    suspend fun syncTodayMetricsFromGoogleFit(): Result<HealthMetrics>

    /**
     * NEW: Sync today's health metrics from Enhanced Health Tracker (device sensors)
     */
    suspend fun syncTodayMetricsFromEnhancedTracker(): Result<HealthMetrics>

    /**
     * Get today's health metrics (convenience method)
     */
    suspend fun getTodayMetrics(): HealthMetrics?

    /**
     * Get weekly health trends
     */
    suspend fun getWeeklyTrends(): List<HealthMetrics>
}