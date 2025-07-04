package com.example.health_assistant.data.repository.impl

import android.util.Log
import com.example.health_assistant.core.util.Result
import com.example.health_assistant.data.health.EnhancedHealthTracker
import com.example.health_assistant.data.repository.interfaces.HealthRepository
import com.example.health_assistant.features.health.model.HealthMetrics
import com.example.health_assistant.features.health.model.HealthMetric
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of HealthRepository that manages health metrics data
 * Uses local device sensors for health data tracking
 */
@Singleton
class HealthRepositoryImpl @Inject constructor(
    private val enhancedHealthTracker: EnhancedHealthTracker
) : HealthRepository {

    // Define getCurrentDate() method first to avoid initialization order issues
    private fun getCurrentDate(): String {
        return LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
    }

    // In-memory cache of health metrics by date
    private val _healthMetricsMap = MutableStateFlow<Map<String, HealthMetrics>>(
        mapOf(
            getCurrentDate() to HealthMetrics(
                steps = HealthMetric(0, 9000), // Will be updated by device sensors
                calories = HealthMetric(0, 300),
                heartPoints = HealthMetric(0, 50)
            )
        )
    )

    init {
        // NEW: Initialize enhanced health tracking immediately
        val trackingStarted = enhancedHealthTracker.initialize()
        Log.d("HealthRepository", "Enhanced health tracking initialized: $trackingStarted")

        // Start collecting real-time step data from device sensors
        startRealTimeStepTracking()
    }

    // NEW: Start real-time step tracking from device sensors
    private fun startRealTimeStepTracking() {
        // This will continuously update health metrics as user walks
        // Works without Google Fit app!
        CoroutineScope(Dispatchers.Default).launch {
            enhancedHealthTracker.getStepCountFlow().collect { stepCount ->
                updateHealthMetricsFromSensors(stepCount)
            }
        }
    }

    // NEW: Update health metrics using device sensor data
    private suspend fun updateHealthMetricsFromSensors(steps: Int) {
        try {
            val currentDate = getCurrentDate()

            // Get enhanced metrics from the new tracker (includes estimated calories and heart points)
            val enhancedResult = enhancedHealthTracker.getCurrentHealthMetrics()

            if (enhancedResult is Result.Success) {
                val enhancedMetrics = enhancedResult.data
                val updatedMap = _healthMetricsMap.value.toMutableMap()
                updatedMap[currentDate] = enhancedMetrics
                _healthMetricsMap.value = updatedMap

                Log.d("HealthRepository", "Updated from sensors - Steps: ${enhancedMetrics.steps.current}, Calories: ${enhancedMetrics.calories.current}")
            }
        } catch (e: Exception) {
            Log.e("HealthRepository", "Error updating from sensors", e)
        }
    }

    override fun getDailyHealthMetrics(date: String): Flow<Result<HealthMetrics?>> {
        return _healthMetricsMap.map<Map<String, HealthMetrics>, Result<HealthMetrics?>> { metricsMap ->
            Result.Success(metricsMap[date])
        }.catch { exception ->
            // FIX: Proper flow error handling - emit the correct type with explicit generic
            emit(Result.Error(exception, "Unknown error occurred"))
        }
    }

    override suspend fun saveDailyHealthMetrics(healthMetrics: HealthMetrics): Result<Unit> {
        return try {
            val currentMap = _healthMetricsMap.value.toMutableMap()
            currentMap[getCurrentDate()] = healthMetrics
            _healthMetricsMap.value = currentMap
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e("HealthRepository", "Error saving health metrics", e)
            Result.Error(e, "Failed to save health metrics")
        }
    }

    override suspend fun getHealthMetricsRange(startDate: String, endDate: String): Result<List<HealthMetrics>> {
        return try {
            val metrics = _healthMetricsMap.value.values.toList()
            Result.Success(metrics)
        } catch (e: Exception) {
            Result.Error(e, "Failed to get health metrics range")
        }
    }

    override suspend fun updateStepCount(steps: Int): Result<Unit> {
        return try {
            val currentDate = getCurrentDate()
            val currentMetrics = _healthMetricsMap.value[currentDate] ?: HealthMetrics()
            val updatedMetrics = currentMetrics.copy(
                steps = currentMetrics.steps.copy(current = steps)
            )
            saveDailyHealthMetrics(updatedMetrics)
        } catch (e: Exception) {
            // FIX: Added missing return statement
            Result.Error(e, "Failed to update step count")
        }
    }

    override suspend fun updateWaterIntake(waterIntake: Float): Result<Unit> {
        return Result.Success(Unit) // TODO: Implement when water intake is added to HealthMetrics model
    }

    override suspend fun updateSleepDuration(sleepHours: Float): Result<Unit> {
        return Result.Success(Unit) // TODO: Implement when sleep is added to HealthMetrics model
    }

    override suspend fun syncHealthData(): Result<Unit> {
        return try {
            // Use enhanced tracker instead of Google Fit
            val syncResult = syncTodayMetricsFromEnhancedTracker()
            when (syncResult) {
                is Result.Success -> Result.Success(Unit)
                is Result.Error -> Result.Error(syncResult.exception, "Failed to sync health data: ${syncResult.message}")
                is Result.Loading -> Result.Success(Unit) // Treat loading as success for this method
            }
        } catch (e: Exception) {
            Result.Error(e, "Failed to sync health data")
        }
    }

    // NEW: Enhanced sync method that uses device sensors as primary source
    override suspend fun syncTodayMetricsFromEnhancedTracker(): Result<HealthMetrics> {
        return try {
            Log.d("HealthRepository", "Syncing from enhanced health tracker (works without Google Fit app)...")

            val result = enhancedHealthTracker.getCurrentHealthMetrics()

            when (result) {
                is Result.Success -> {
                    val metrics = result.data

                    // Save the metrics
                    val saveResult = saveDailyHealthMetrics(metrics)

                    when (saveResult) {
                        is Result.Success -> {
                            Log.d("HealthRepository", "Successfully synced enhanced health data - Steps: ${metrics.steps.current}")
                            Result.Success(metrics)
                        }
                        is Result.Error -> Result.Error(saveResult.exception, "Failed to save enhanced health data")
                        else -> Result.Success(metrics)
                    }
                }
                is Result.Error -> result
                else -> Result.Error(null, "Unknown error in enhanced health sync")
            }
        } catch (e: Exception) {
            Log.e("HealthRepository", "Error syncing from enhanced tracker", e)
            Result.Error(e, "Failed to sync enhanced health data: ${e.message}")
        }
    }

    override suspend fun getTodayMetrics(): Result<HealthMetrics> {
        return try {
            val metrics = _healthMetricsMap.value[getCurrentDate()]
            if (metrics != null) {
                Result.Success(metrics)
            } else {
                // If no metrics exist, try to get from enhanced tracker
                syncTodayMetricsFromEnhancedTracker()
            }
        } catch (e: Exception) {
            Result.Error(e, "Failed to get today's metrics")
        }
    }

    override suspend fun getWeeklyTrends(): List<HealthMetrics> {
        // For now, return current day's metrics
        // TODO: Implement when Room database is integrated for historical data
        return try {
            val todayResult = getTodayMetrics()
            when (todayResult) {
                is Result.Success -> listOf(todayResult.data)
                is Result.Error -> emptyList()
                is Result.Loading -> emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}