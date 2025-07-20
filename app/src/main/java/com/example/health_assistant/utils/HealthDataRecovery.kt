package com.example.health_assistant.utils

import android.util.Log
import com.example.health_assistant.core.util.Result
import com.example.health_assistant.data.models.DailyStepData
import com.example.health_assistant.data.repository.interfaces.HealthRepository
import com.example.health_assistant.features.health.model.HealthMetrics
import com.example.health_assistant.features.health.model.HealthMetric
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Utility class for recovering from health data errors
 */
class HealthDataRecovery(private val healthRepository: HealthRepository) {
    private val TAG = "HealthDataRecovery"
    
    /**
     * Recover daily step data for a specific date
     * @return recovered data or fallback data if recovery failed
     */
    suspend fun recoverDailyStepData(userId: String, date: LocalDate): DailyStepData {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Attempting to recover daily step data for user $userId on $date")
                
                // Try to get from repository
                val dateString = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
                val result = healthRepository.getDailyStepData(date)
                
                if (result is Result.Success) {
                    Log.d(TAG, "Successfully recovered daily step data from repository")
                    return@withContext result.data
                }
                
                // Try to get from health metrics
                val metricsResult = healthRepository.getPreviousDayMetrics(userId, dateString)
                
                if (metricsResult is Result.Success && metricsResult.data != null) {
                    Log.d(TAG, "Recovered daily step data from previous day metrics")
                    return@withContext DailyStepData(
                        date = date,
                        steps = metricsResult.data.steps.current,
                        goal = metricsResult.data.steps.target,
                        calories = metricsResult.data.calories.current,
                        caloriesGoal = metricsResult.data.calories.target,
                        heartPoints = metricsResult.data.heartPoints.current,
                        heartPointsGoal = metricsResult.data.heartPoints.target
                    )
                }
                
                // Create fallback data
                Log.d(TAG, "Creating fallback data for $date")
                val fallbackData = DailyStepData(
                    date = date,
                    steps = 0,
                    goal = 9000,
                    calories = 0,
                    caloriesGoal = 300,
                    heartPoints = 0,
                    heartPointsGoal = 50
                )
                
                // Save fallback data for future use
                healthRepository.saveDailyStepData(fallbackData)
                
                return@withContext fallbackData
            } catch (e: Exception) {
                Log.e(TAG, "Error recovering daily step data", e)
                
                // Return fallback data
                return@withContext DailyStepData(
                    date = date,
                    steps = 0,
                    goal = 9000,
                    calories = 0,
                    caloriesGoal = 300,
                    heartPoints = 0,
                    heartPointsGoal = 50
                )
            }
        }
    }
    
    /**
     * Recover weekly step data
     * @return recovered data or fallback data if recovery failed
     */
    suspend fun recoverWeeklyStepData(userId: String, startDate: LocalDate): List<DailyStepData> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Attempting to recover weekly step data for user $userId starting on $startDate")
                
                // Try to get from repository
                val result = healthRepository.getWeeklyStepData(startDate)
                
                if (result is Result.Success && result.data.size == 7) {
                    Log.d(TAG, "Successfully recovered weekly step data from repository")
                    return@withContext result.data
                }
                
                // Recover each day individually
                val recoveredData = mutableListOf<DailyStepData>()
                
                for (dayOffset in 0..6) {
                    val date = startDate.plusDays(dayOffset.toLong())
                    val dailyData = recoverDailyStepData(userId, date)
                    recoveredData.add(dailyData)
                }
                
                Log.d(TAG, "Recovered weekly step data day by day")
                return@withContext recoveredData
            } catch (e: Exception) {
                Log.e(TAG, "Error recovering weekly step data", e)
                
                // Return fallback data
                return@withContext (0..6).map { dayOffset ->
                    val date = startDate.plusDays(dayOffset.toLong())
                    DailyStepData(
                        date = date,
                        steps = 0,
                        goal = 9000,
                        calories = 0,
                        caloriesGoal = 300,
                        heartPoints = 0,
                        heartPointsGoal = 50
                    )
                }
            }
        }
    }
    
    /**
     * Recover health metrics for a specific date
     * @return recovered metrics or fallback metrics if recovery failed
     */
    suspend fun recoverHealthMetrics(userId: String, dateString: String): HealthMetrics {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Attempting to recover health metrics for user $userId on $dateString")
                
                // Try to get from repository
                val metricsFlow = healthRepository.getDailyHealthMetrics(dateString)
                
                var metrics: HealthMetrics? = null
                metricsFlow.collect { result ->
                    if (result is Result.Success) {
                        metrics = result.data
                    }
                }
                
                if (metrics != null) {
                    Log.d(TAG, "Successfully recovered health metrics from repository")
                    return@withContext metrics!!
                }
                
                // Try to get from previous day metrics
                val previousDayResult = healthRepository.getPreviousDayMetrics(userId, dateString)
                
                if (previousDayResult is Result.Success && previousDayResult.data != null) {
                    Log.d(TAG, "Recovered health metrics from previous day metrics")
                    return@withContext previousDayResult.data
                }
                
                // Create fallback metrics
                Log.d(TAG, "Creating fallback metrics for $dateString")
                return@withContext HealthMetrics(
                    steps = HealthMetric(0, 9000),
                    calories = HealthMetric(0, 300),
                    heartPoints = HealthMetric(0, 50)
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error recovering health metrics", e)
                
                // Return fallback metrics
                return@withContext HealthMetrics(
                    steps = HealthMetric(0, 9000),
                    calories = HealthMetric(0, 300),
                    heartPoints = HealthMetric(0, 50)
                )
            }
        }
    }
}