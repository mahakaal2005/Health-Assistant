package com.example.health_assistant.data.repository.interfaces

import com.example.health_assistant.core.util.Result
import com.example.health_assistant.features.health.model.HealthMetrics
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for health-related data operations with robust error handling
 */
interface HealthRepository {
    /**
     * Get user's daily health metrics
     * @param date Date in format YYYY-MM-DD
     * @return Flow of Result<HealthMetrics?> containing health data or error
     */
    fun getDailyHealthMetrics(date: String): Flow<Result<HealthMetrics?>>

    /**
     * Save daily health metrics
     * @param healthMetrics Health metrics to save
     * @return Result indicating success or failure
     */
    suspend fun saveDailyHealthMetrics(healthMetrics: HealthMetrics): Result<Unit>

    /**
     * Get health metrics for a date range
     * @param startDate Start date in format YYYY-MM-DD
     * @param endDate End date in format YYYY-MM-DD
     * @return Result containing list of health metrics or error
     */
    suspend fun getHealthMetricsRange(startDate: String, endDate: String): Result<List<HealthMetrics>>

    /**
     * Update step count for today
     * @param steps Number of steps
     * @return Result indicating success or failure
     */
    suspend fun updateStepCount(steps: Int): Result<Unit>

    /**
     * Update water intake for today
     * @param waterIntake Water intake in liters
     * @return Result indicating success or failure
     */
    suspend fun updateWaterIntake(waterIntake: Float): Result<Unit>

    /**
     * Update sleep duration for today
     * @param sleepHours Sleep duration in hours
     * @return Result indicating success or failure
     */
    suspend fun updateSleepDuration(sleepHours: Float): Result<Unit>

    /**
     * Sync health data with cloud storage
     * @return Result indicating success or failure
     */
    suspend fun syncHealthData(): Result<Unit>
}

/**
 * Data class representing daily health metrics
 */
data class HealthMetrics(
    val date: String, // YYYY-MM-DD format
    val steps: Int = 0,
    val waterIntake: Float = 0f, // in liters
    val sleepHours: Float = 0f,
    val calories: Int = 0,
    val exerciseMinutes: Int = 0,
    val heartRate: Int? = null, // optional heart rate data
    val weight: Float? = null, // optional weight in kg
    val bloodPressure: BloodPressure? = null,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Data class for blood pressure readings
 */
data class BloodPressure(
    val systolic: Int,
    val diastolic: Int,
    val timestamp: Long = System.currentTimeMillis()
)