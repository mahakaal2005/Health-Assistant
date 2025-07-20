package com.example.health_assistant.utils

import android.util.Log
import com.example.health_assistant.data.models.DailyStepData
import com.example.health_assistant.features.health.model.HealthMetrics
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * Utility class for validating health data and ensuring data integrity
 */
object HealthDataValidator {
    private const val TAG = "HealthDataValidator"
    
    /**
     * Validate daily step data
     * @return true if valid, false otherwise
     */
    fun validateDailyStepData(stepData: DailyStepData): Boolean {
        try {
            // Date is non-nullable LocalDate, so no need to check for null
            
            // Validate steps (non-negative)
            if (stepData.steps < 0) {
                Log.e(TAG, "Invalid step data: negative steps value ${stepData.steps}")
                return false
            }
            
            // Validate calories (non-negative)
            if (stepData.calories < 0) {
                Log.e(TAG, "Invalid step data: negative calories value ${stepData.calories}")
                return false
            }
            
            // Validate heart points (non-negative)
            if (stepData.heartPoints < 0) {
                Log.e(TAG, "Invalid step data: negative heart points value ${stepData.heartPoints}")
                return false
            }
            
            // Validate future dates (should have zero values)
            if (stepData.date.isAfter(LocalDate.now())) {
                if (stepData.steps > 0 || stepData.calories > 0 || stepData.heartPoints > 0) {
                    Log.e(TAG, "Invalid step data: future date ${stepData.date} with non-zero values")
                    return false
                }
            }
            
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error validating step data", e)
            return false
        }
    }
    
    /**
     * Validate health metrics
     * @return true if valid, false otherwise
     */
    fun validateHealthMetrics(metrics: HealthMetrics): Boolean {
        try {
            // Validate steps (non-negative)
            if (metrics.steps.current < 0) {
                Log.e(TAG, "Invalid health metrics: negative steps value ${metrics.steps.current}")
                return false
            }
            
            // Validate calories (non-negative)
            if (metrics.calories.current < 0) {
                Log.e(TAG, "Invalid health metrics: negative calories value ${metrics.calories.current}")
                return false
            }
            
            // Validate heart points (non-negative)
            if (metrics.heartPoints.current < 0) {
                Log.e(TAG, "Invalid health metrics: negative heart points value ${metrics.heartPoints.current}")
                return false
            }
            
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error validating health metrics", e)
            return false
        }
    }
    
    /**
     * Validate date string
     * @return true if valid, false otherwise
     */
    fun validateDateString(dateString: String): Boolean {
        return try {
            LocalDate.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE)
            true
        } catch (e: DateTimeParseException) {
            Log.e(TAG, "Invalid date string: $dateString", e)
            false
        }
    }
    
    /**
     * Repair daily step data if needed
     * @return repaired data or null if repair failed
     */
    fun repairDailyStepData(stepData: DailyStepData): DailyStepData? {
        try {
            // Create a copy with valid values
            return DailyStepData(
                date = stepData.date,
                steps = maxOf(0, stepData.steps), // Ensure non-negative
                goal = maxOf(1, stepData.goal), // Ensure positive
                calories = maxOf(0, stepData.calories), // Ensure non-negative
                caloriesGoal = maxOf(1, stepData.caloriesGoal), // Ensure positive
                heartPoints = maxOf(0, stepData.heartPoints), // Ensure non-negative
                heartPointsGoal = maxOf(1, stepData.heartPointsGoal) // Ensure positive
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error repairing step data", e)
            return null
        }
    }
    
    /**
     * Ensure we have a complete 7-day dataset
     * @return complete dataset or empty list if repair failed
     */
    fun ensureComplete7DayDataset(startDate: LocalDate, partialData: List<DailyStepData>): List<DailyStepData> {
        try {
            val completeData = mutableListOf<DailyStepData>()
            
            // Create a map of existing data by date string
            val dataByDate = partialData.associateBy { 
                it.date.format(DateTimeFormatter.ISO_LOCAL_DATE) 
            }
            
            // Create data for all 7 days
            for (dayOffset in 0..6) {
                val date = startDate.plusDays(dayOffset.toLong())
                val dateString = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
                
                // Use existing data if available, otherwise create zero-value data
                val dailyData = dataByDate[dateString] ?: DailyStepData(
                    date = date,
                    steps = 0,
                    goal = 9000,
                    calories = 0,
                    caloriesGoal = 300,
                    heartPoints = 0,
                    heartPointsGoal = 50
                )
                
                // Validate and repair if needed
                val validatedData = if (validateDailyStepData(dailyData)) {
                    dailyData
                } else {
                    repairDailyStepData(dailyData) ?: DailyStepData(
                        date = date,
                        steps = 0,
                        goal = 9000,
                        calories = 0,
                        caloriesGoal = 300,
                        heartPoints = 0,
                        heartPointsGoal = 50
                    )
                }
                
                completeData.add(validatedData)
            }
            
            return completeData
        } catch (e: Exception) {
            Log.e(TAG, "Error ensuring complete 7-day dataset", e)
            return emptyList()
        }
    }
}