package com.example.health_assistant.data.repository.interfaces

import com.example.health_assistant.core.util.Result
import com.example.health_assistant.features.health.model.HealthMetrics
import com.example.health_assistant.data.models.DailyStepData
import com.example.health_assistant.data.models.WeeklyHealthSummary
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Repository interface for managing health metrics data
 * Includes Google Fit API integration for real-time health data
 */
interface HealthRepository {

    /**
     * Get daily health metrics for a specific date
     */
    suspend fun getDailyHealthMetrics(date: String): Flow<Result<HealthMetrics>>

    /**
     * Save health metrics for specific dates to ensure persistence
     */
    suspend fun saveHealthMetrics(date: String, metrics: HealthMetrics)

    /**
     * Get real-time step count flow for live UI updates
     */
    fun getRealTimeStepFlow(): Flow<Int>

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
     * NEW: Sync today's health metrics from Enhanced Health Tracker (device sensors)
     */
    suspend fun syncTodayMetricsFromEnhancedTracker(): Result<HealthMetrics>

    /**
     * Get today's health metrics (convenience method)
     */
    suspend fun getTodayMetrics(): Result<HealthMetrics>

    /**
     * Get weekly health trends
     */
    suspend fun getWeeklyTrends(): List<HealthMetrics>

    /**
     * NEW: Get daily step data for a specific date
     */
    suspend fun getDailyStepData(date: LocalDate): Result<DailyStepData>

    /**
     * NEW: Get weekly step data for chart display
     */
    suspend fun getWeeklyStepData(startDate: LocalDate): Result<List<DailyStepData>>

    /**
     * NEW: Get weekly calories data for chart display
     */
    suspend fun getWeeklyCaloriesData(startDate: java.util.Date): List<DailyStepData>

    /**
     * NEW: Get weekly heart points data for chart display
     */
    suspend fun getWeeklyHeartPointsData(startDate: java.util.Date): List<DailyStepData>

    /**
     * NEW: Save daily step data
     */
    suspend fun saveDailyStepData(stepData: DailyStepData): Result<Unit>

    /**
     * NEW: Weekly data lifecycle management - saves weekly summary and cleans old data
     */
    suspend fun saveWeeklyHealthSummary(userId: String, weekStartDate: LocalDate): Result<Unit>

    /**
     * NEW: Clean up old weekly data (keeps current week + 1 day buffer)
     */
    suspend fun cleanupOldWeeklyData(userId: String): Result<Unit>

    /**
     * NEW: Get user's weekly health history
     */
    suspend fun getUserWeeklyHistory(userId: String, weeksCount: Int = 4): Result<List<WeeklyHealthSummary>>

    /**
     * NEW: Auto-cleanup trigger for daily data maintenance
     */
    suspend fun performDailyDataMaintenance(userId: String): Result<Unit>

    /**
     * NEW: Reset step count for a specific user
     * Used when testing or when data becomes corrupted
     */
    suspend fun resetUserStepCount(userId: String): Result<Unit>
}