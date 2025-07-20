package com.example.health_assistant.data.repository.impl

import android.util.Log
import com.example.health_assistant.auth.session.SessionManager
import com.example.health_assistant.core.util.Result
import com.example.health_assistant.data.health.EnhancedHealthTracker
import com.example.health_assistant.data.repository.interfaces.HealthRepository
import com.example.health_assistant.features.health.model.HealthMetrics
import com.example.health_assistant.features.health.model.HealthMetric
import com.example.health_assistant.data.models.DailyStepData
import com.example.health_assistant.data.models.WeeklyHealthSummary
import com.example.health_assistant.utils.HealthDataLogger
import com.example.health_assistant.utils.HealthDataMonitor
import com.example.health_assistant.utils.HealthDataErrorTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of HealthRepository that manages health metrics data
 * Uses local device sensors for health data tracking
 * Now with proper user isolation for multi-user support
 */
@Singleton
class HealthRepositoryImpl @Inject constructor(
    private val enhancedHealthTracker: EnhancedHealthTracker,
    private val sessionManager: SessionManager
) : HealthRepository {

    companion object {
        private const val TAG = "HealthRepository"
    }

    // Define getCurrentDate() method first to avoid initialization order issues
    private fun getCurrentDate(): String {
        return LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
    }

    // Get current user ID from session manager
    private fun getCurrentUserId(): String {
        return sessionManager.getCurrentUserId() ?: ""
    }

    // In-memory cache of health metrics by user ID and date
    private val _healthMetricsMap = MutableStateFlow<Map<String, Map<String, HealthMetrics>>>(emptyMap())

    // In-memory cache for daily step data with historical tracking by user ID
    // Enhanced to maintain a rolling 7-day window of data
    private val _dailyStepDataMap = MutableStateFlow<Map<String, Map<String, DailyStepData>>>(emptyMap())
    
    // Cache for previous day data to ensure persistence during day transitions
    private val _previousDayDataCache = MutableStateFlow<Map<String, DailyStepData>>(emptyMap())

    // Weekly data lifecycle management implementation by user ID
    private val _weeklyHealthSummaries = MutableStateFlow<Map<String, WeeklyHealthSummary>>(emptyMap())

    // User-specific step counts - key is userId, value is current step count
    private val _userStepCounts = MutableStateFlow<Map<String, Int>>(emptyMap())

    // Last global step count - used to calculate increments
    private var lastGlobalStepCount = 0

    init {
        // Initialize enhanced health tracking immediately
        val trackingStarted = enhancedHealthTracker.initialize()
        Log.d(TAG, "Enhanced health tracking initialized: $trackingStarted")

        // Start collecting real-time step data from device sensors
        startRealTimeStepTracking()
    }

    // Start real-time step tracking from device sensors
    private fun startRealTimeStepTracking() {
        // This will continuously update health metrics as user walks
        CoroutineScope(Dispatchers.Default).launch {
            enhancedHealthTracker.getStepCountFlow().collect { globalStepCount ->
                // Calculate the increment since last update
                val stepIncrement = calculateStepIncrement(globalStepCount)
                
                // Only update if there's a positive increment (user actually walked)
                if (stepIncrement > 0) {
                    // Update the current user's step count
                    updateCurrentUserSteps(stepIncrement)
                }
                
                // Save the last global step count
                lastGlobalStepCount = globalStepCount
            }
        }
    }

    // Calculate step increment since last update
    private fun calculateStepIncrement(currentGlobalSteps: Int): Int {
        // Handle device reboot case where step counter resets to 0
        if (currentGlobalSteps < lastGlobalStepCount && lastGlobalStepCount > 1000) {
            // Device likely rebooted, treat current steps as the increment
            return currentGlobalSteps
        }
        
        // Normal case - calculate increment
        return maxOf(0, currentGlobalSteps - lastGlobalStepCount)
    }

    // Update the current user's step count
    private suspend fun updateCurrentUserSteps(stepIncrement: Int) {
        val userId = getCurrentUserId()
        if (userId.isEmpty()) {
            Log.d(TAG, "No user logged in, skipping step update")
            return
        }
        
        // Get current user's step count or default to 0
        val currentUserSteps = _userStepCounts.value[userId] ?: 0
        
        // Add the increment to the user's step count
        val newUserSteps = currentUserSteps + stepIncrement
        
        // Update the user step counts map
        val updatedUserStepCounts = _userStepCounts.value.toMutableMap()
        updatedUserStepCounts[userId] = newUserSteps
        _userStepCounts.value = updatedUserStepCounts
        
        // Log sensor data update
        HealthDataLogger.logSensorDataUpdate(userId, "STEP_COUNTER", newUserSteps, stepIncrement)
        
        Log.d(TAG, "Updated steps for user $userId: $currentUserSteps + $stepIncrement = $newUserSteps")
        
        // Now update the health metrics with the new step count
        updateHealthMetricsFromSteps(newUserSteps)
        updateDailyStepData(newUserSteps)
    }

    // Update health metrics using step count
    private suspend fun updateHealthMetricsFromSteps(steps: Int) {
        try {
            val userId = getCurrentUserId()
            if (userId.isEmpty()) {
                Log.w(TAG, "No user logged in, skipping health metrics update")
                return
            }

            val currentDate = getCurrentDate()
            
            // Calculate calories and heart points based on steps
            val calories = calculateCaloriesFromSteps(steps)
            val heartPoints = calculateHeartPointsFromSteps(steps)
            
            // Create health metrics object
            val metrics = HealthMetrics(
                steps = HealthMetric(steps, 9000),
                calories = HealthMetric(calories, 300),
                heartPoints = HealthMetric(heartPoints, 50)
            )
            
            // Get user's metrics map or create new one
            val userMetricsMap = _healthMetricsMap.value[userId] ?: emptyMap()
            val updatedUserMap = userMetricsMap.toMutableMap()
            updatedUserMap[currentDate] = metrics
            
            // Update the global map with the user's updated map
            val globalMap = _healthMetricsMap.value.toMutableMap()
            globalMap[userId] = updatedUserMap
            _healthMetricsMap.value = globalMap

            // Log health metrics update
            HealthDataLogger.logHealthMetricsUpdate(userId, LocalDate.now(), metrics, "SENSOR_DATA")

            Log.d(TAG, "Updated metrics for user $userId - Steps: $steps, Calories: $calories, Heart Points: $heartPoints")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating metrics from steps", e)
        }
    }

    // Update daily step data with enhanced historical data preservation
    private suspend fun updateDailyStepData(steps: Int) {
        try {
            val userId = getCurrentUserId()
            if (userId.isEmpty()) {
                Log.w(TAG, "No user logged in, skipping daily step data update")
                return
            }

            val today = LocalDate.now()
            val todayString = today.format(DateTimeFormatter.ISO_LOCAL_DATE)
            
            // Check if we need to preserve yesterday's data before updating today
            checkAndPreservePreviousDayData(userId, today)

            // Calculate calories and heart points based on steps
            val calories = calculateCaloriesFromSteps(steps)
            val heartPoints = calculateHeartPointsFromSteps(steps)

            val stepData = DailyStepData(
                date = today,
                steps = steps,
                goal = 9000,
                calories = calories,
                caloriesGoal = 300,
                heartPoints = heartPoints,
                heartPointsGoal = 50
            )

            // Get user's step data map or create new one
            val userStepDataMap = _dailyStepDataMap.value[userId] ?: emptyMap()
            val updatedUserMap = userStepDataMap.toMutableMap()
            
            // Store today's data
            updatedUserMap[todayString] = stepData
            
            // Ensure we maintain a rolling 7-day window of data
            maintainRollingSevenDayWindow(updatedUserMap, today)
            
            // Update the global map with the user's updated map
            val globalMap = _dailyStepDataMap.value.toMutableMap()
            globalMap[userId] = updatedUserMap
            _dailyStepDataMap.value = globalMap

            // Log data storage operation
            HealthDataLogger.logDataStorage("UPDATE_DAILY_STEP_DATA", userId, today, stepData)

            Log.d(TAG, "Updated daily step data for user $userId: $steps steps, $calories calories, $heartPoints heart points for $today")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating daily step data", e)
        }
    }
    
    /**
     * Check if we need to preserve the previous day's data before updating today's data
     * This ensures data persistence during day transitions
     */
    private suspend fun checkAndPreservePreviousDayData(userId: String, today: LocalDate) {
        try {
            val yesterday = today.minusDays(1)
            val yesterdayString = yesterday.format(DateTimeFormatter.ISO_LOCAL_DATE)
            
            // Get user's step data map
            val userStepDataMap = _dailyStepDataMap.value[userId] ?: emptyMap()
            
            // Check if we already have yesterday's data
            if (!userStepDataMap.containsKey(yesterdayString)) {
                // We don't have yesterday's data, check if we have it in the previous day cache
                val previousDayData = _previousDayDataCache.value[userId]
                if (previousDayData != null && previousDayData.date.isEqual(yesterday)) {
                    // We have yesterday's data in the cache, add it to the user's step data map
                    val updatedUserMap = userStepDataMap.toMutableMap()
                    updatedUserMap[yesterdayString] = previousDayData
                    
                    // Update the global map with the user's updated map
                    val globalMap = _dailyStepDataMap.value.toMutableMap()
                    globalMap[userId] = updatedUserMap
                    _dailyStepDataMap.value = globalMap
                    
                    Log.d(TAG, "Restored previous day data for user $userId: ${previousDayData.steps} steps for $yesterday")
                }
            }
            
            // Cache today's data for tomorrow
            val todayString = today.format(DateTimeFormatter.ISO_LOCAL_DATE)
            val todayData = userStepDataMap[todayString]
            if (todayData != null) {
                val updatedPreviousDayCache = _previousDayDataCache.value.toMutableMap()
                updatedPreviousDayCache[userId] = todayData
                _previousDayDataCache.value = updatedPreviousDayCache
                
                // Log day transition
                HealthDataLogger.logDayTransition(userId, yesterday, today, todayData)
                
                Log.d(TAG, "Cached today's data for user $userId: ${todayData.steps} steps for $today")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking and preserving previous day data", e)
        }
    }
    
    /**
     * Maintain a rolling 7-day window of data that preserves historical information during day transitions
     * This ensures we keep exactly 7 days of data while preserving historical data when day transitions occur
     */
    private fun maintainRollingSevenDayWindow(userStepDataMap: MutableMap<String, DailyStepData>, today: LocalDate) {
        try {
            // Get all dates in the user's step data map
            val dates = userStepDataMap.keys.mapNotNull { dateString ->
                try {
                    LocalDate.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE)
                } catch (e: Exception) {
                    Log.w(TAG, "Invalid date format in step data map: $dateString")
                    null
                }
            }.sorted()
            
            Log.d(TAG, "Maintaining rolling window: found ${dates.size} dates, today is $today")
            
            // Calculate the rolling 7-day window (today and 6 days back)
            val windowStartDate = today.minusDays(6)
            val windowEndDate = today
            
            // Separate dates into those within the window and those outside
            val datesInWindow = dates.filter { date ->
                !date.isBefore(windowStartDate) && !date.isAfter(windowEndDate)
            }
            
            val datesOutsideWindow = dates.filter { date ->
                date.isBefore(windowStartDate) || date.isAfter(windowEndDate)
            }
            
            Log.d(TAG, "Dates in rolling window (${windowStartDate} to ${windowEndDate}): ${datesInWindow.size}")
            Log.d(TAG, "Dates outside rolling window: ${datesOutsideWindow.size}")
            
            // Remove dates that are outside the rolling 7-day window
            // But preserve recent historical data (don't remove data from yesterday if today just started)
            datesOutsideWindow.forEach { oldDate ->
                // Only remove data that's more than 8 days old to provide a buffer during day transitions
                if (oldDate.isBefore(today.minusDays(8))) {
                    val oldDateString = oldDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
                    userStepDataMap.remove(oldDateString)
                    Log.d(TAG, "Removed old step data for date: $oldDateString (more than 8 days old)")
                } else {
                    Log.d(TAG, "Preserved recent data for date: $oldDate (within buffer period)")
                }
            }
            
            // Ensure we have data for all days in the current rolling window
            for (dayOffset in 0..6) {
                val windowDate = windowStartDate.plusDays(dayOffset.toLong())
                val windowDateString = windowDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
                
                if (!userStepDataMap.containsKey(windowDateString)) {
                    // Create zero-value data for missing days in the window
                    val zeroValueData = createZeroValueStepData(windowDate)
                    userStepDataMap[windowDateString] = zeroValueData
                    Log.d(TAG, "Added zero-value data for missing date in rolling window: $windowDate")
                }
            }
            
            Log.d(TAG, "Rolling 7-day window maintenance completed. Total dates in map: ${userStepDataMap.size}")
        } catch (e: Exception) {
            Log.e(TAG, "Error maintaining rolling 7-day window", e)
        }
    }

    // Calculate realistic calories based on steps
    private fun calculateCaloriesFromSteps(steps: Int): Int {
        // Average person burns about 0.04-0.05 calories per step
        return (steps * 0.045).toInt()
    }

    // Calculate realistic heart points based on steps
    private fun calculateHeartPointsFromSteps(steps: Int): Int {
        // Heart points are earned through moderate to vigorous activity
        val activeSteps = maxOf(0, steps - 2000) // Subtract baseline daily activity
        return (activeSteps / 150).toInt() // More conservative than 100 steps per point
    }

    /**
     * Get real-time step count flow for live UI updates
     * Now with complete user isolation
     */
    override fun getRealTimeStepFlow(): Flow<Int> {
        return _userStepCounts
            .map { userStepCounts ->
                val userId = getCurrentUserId()
                userStepCounts[userId] ?: 0
            }
            .catch { e ->
                Log.e(TAG, "Error in real-time step flow", e)
                emit(0)
            }
    }

    /**
     * Reset step count for a specific user
     * Used when testing or when data becomes corrupted
     */
    override suspend fun resetUserStepCount(userId: String): Result<Unit> {
        return try {
            // Reset user step count in EnhancedHealthTracker
            enhancedHealthTracker.resetUserStepCount(userId)
            
            // Reset in our local cache
            val updatedUserStepCounts = _userStepCounts.value.toMutableMap()
            updatedUserStepCounts[userId] = 0
            _userStepCounts.value = updatedUserStepCounts
            
            // Also update health metrics and daily step data
            if (userId == getCurrentUserId()) {
                updateHealthMetricsFromSteps(0)
                updateDailyStepData(0)
            }
            
            Log.d(TAG, "Reset step count for user $userId")
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error resetting step count for user $userId", e)
            Result.Error(e, "Failed to reset step count")
        }
    }

    /**
     * Enhanced data persistence with date-specific and user-specific caching
     */
    override suspend fun getDailyHealthMetrics(date: String): Flow<Result<HealthMetrics>> {
        return flow {
            emit(Result.Loading)

            try {
                val userId = getCurrentUserId()
                if (userId.isEmpty()) {
                    Log.w(TAG, "No user logged in, returning default metrics")
                    emit(Result.Success(HealthMetrics()))
                    return@flow
                }

                // Get user's metrics map or create new one
                val userMetricsMap = _healthMetricsMap.value[userId] ?: emptyMap()
                
                // Check if we have cached data for this specific date and user
                val cachedMetrics = userMetricsMap[date]

                if (cachedMetrics != null) {
                    Log.d(TAG, "Found cached metrics for user $userId and date: $date")
                    emit(Result.Success(cachedMetrics))
                } else if (date == getCurrentDate()) {
                    // For today, get live data from sensors
                    Log.d(TAG, "Getting live data for user $userId and today: $date")
                    val liveResult = enhancedHealthTracker.getCurrentHealthMetrics()

                    when (liveResult) {
                        is Result.Success -> {
                            // Cache today's data for this user
                            val updatedUserMap = userMetricsMap.toMutableMap()
                            updatedUserMap[date] = liveResult.data
                            
                            // Update the global map with the user's updated map
                            val globalMap = _healthMetricsMap.value.toMutableMap()
                            globalMap[userId] = updatedUserMap
                            _healthMetricsMap.value = globalMap

                            emit(Result.Success(liveResult.data))
                        }
                        is Result.Error -> {
                            emit(Result.Error(liveResult.exception, liveResult.message))
                        }
                        is Result.Loading -> {
                            emit(Result.Loading)
                        }
                    }
                } else {
                    // For historical dates, create default metrics if not cached
                    Log.d(TAG, "Creating default metrics for user $userId and historical date: $date")
                    val defaultMetrics = HealthMetrics(
                        steps = HealthMetric(0, 9000),
                        calories = HealthMetric(0, 300),
                        heartPoints = HealthMetric(0, 50)
                    )

                    // Cache the default metrics for this user
                    val updatedUserMap = userMetricsMap.toMutableMap()
                    updatedUserMap[date] = defaultMetrics
                    
                    // Update the global map with the user's updated map
                    val globalMap = _healthMetricsMap.value.toMutableMap()
                    globalMap[userId] = updatedUserMap
                    _healthMetricsMap.value = globalMap

                    emit(Result.Success(defaultMetrics))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting daily health metrics for date: $date", e)
                emit(Result.Error(e, "Failed to get health metrics: ${e.message}"))
            }
        }
    }

    /**
     * Save health metrics for specific dates with user isolation
     */
    override suspend fun saveHealthMetrics(date: String, metrics: HealthMetrics) {
        try {
            val userId = getCurrentUserId()
            if (userId.isEmpty()) {
                Log.w(TAG, "No user logged in, skipping health metrics save")
                return
            }

            // Get user's metrics map or create new one
            val userMetricsMap = _healthMetricsMap.value[userId] ?: emptyMap()
            val updatedUserMap = userMetricsMap.toMutableMap()
            updatedUserMap[date] = metrics
            
            // Update the global map with the user's updated map
            val globalMap = _healthMetricsMap.value.toMutableMap()
            globalMap[userId] = updatedUserMap
            _healthMetricsMap.value = globalMap

            Log.d(TAG, "Saved health metrics for user $userId and date: $date - Steps: ${metrics.steps.current}")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving health metrics for date: $date", e)
        }
    }

    override suspend fun saveDailyHealthMetrics(healthMetrics: HealthMetrics): Result<Unit> {
        return try {
            val userId = getCurrentUserId()
            if (userId.isEmpty()) {
                Log.w(TAG, "No user logged in, skipping daily health metrics save")
                return Result.Error(Exception("No user logged in"), "No user logged in")
            }
            val currentDate = getCurrentDate()
            
            // Get user's metrics map or create new one
            val userMetricsMap = _healthMetricsMap.value[userId] ?: emptyMap()
            val updatedUserMap = userMetricsMap.toMutableMap()
            updatedUserMap[currentDate] = healthMetrics
            
            // Update the global map with the user's updated map
            val globalMap = _healthMetricsMap.value.toMutableMap()
            globalMap[userId] = updatedUserMap
            _healthMetricsMap.value = globalMap
            
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving health metrics", e)
            Result.Error(e, "Failed to save health metrics")
        }
    }

    override suspend fun getHealthMetricsRange(startDate: String, endDate: String): Result<List<HealthMetrics>> {
        return try {
            val userId = getCurrentUserId()
            if (userId.isEmpty()) {
                return Result.Error(Exception("No user logged in"), "No user logged in")
            }
            val userMetricsMap = _healthMetricsMap.value[userId] ?: emptyMap()
            val metrics = userMetricsMap.values.toList()
            Result.Success(metrics)
        } catch (e: Exception) {
            Result.Error(e, "Failed to get health metrics range")
        }
    }

    override suspend fun updateStepCount(steps: Int): Result<Unit> {
        return try {
            val userId = getCurrentUserId()
            if (userId.isEmpty()) {
                return Result.Error(Exception("No user logged in"), "No user logged in")
            }
            val currentDate = getCurrentDate()
            val currentMetrics = _healthMetricsMap.value[userId]?.get(currentDate) ?: HealthMetrics()
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
            Log.d(TAG, "Syncing from enhanced health tracker (works without Google Fit app)...")

            val result = enhancedHealthTracker.getCurrentHealthMetrics()

            when (result) {
                is Result.Success -> {
                    val metrics = result.data

                    // Save the metrics
                    val saveResult = saveDailyHealthMetrics(metrics)

                    when (saveResult) {
                        is Result.Success -> {
                            Log.d(TAG, "Successfully synced enhanced health data - Steps: ${metrics.steps.current}")
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
            Log.e(TAG, "Error syncing from enhanced tracker", e)
            Result.Error(e, "Failed to sync enhanced health data: ${e.message}")
        }
    }

    override suspend fun getTodayMetrics(): Result<HealthMetrics> {
        return try {
            val userId = getCurrentUserId()
            if (userId.isEmpty()) {
                return Result.Error(Exception("No user logged in"), "No user logged in")
            }
            val metrics = _healthMetricsMap.value[userId]?.get(getCurrentDate())
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

    override suspend fun getDailyStepData(date: LocalDate): Result<DailyStepData> {
        return try {
            val userId = getCurrentUserId()
            if (userId.isEmpty()) {
                return Result.Error(Exception("No user logged in"), "No user logged in")
            }
            val dateString = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
            val stepData = _dailyStepDataMap.value[userId]?.get(dateString)

            if (stepData != null) {
                Result.Success(stepData)
            } else {
                // Create default data for missing dates
                val defaultStepData = if (date.isAfter(LocalDate.now())) {
                    // Future dates have 0 steps
                    DailyStepData(date = date, steps = 0, goal = 10000)
                } else {
                    // CHANGED: Past dates also show 0 steps if no real data exists
                    // This is more honest than showing fake sample data
                    DailyStepData(date = date, steps = 0, goal = 10000)
                }

                // Save the generated data
                saveDailyStepData(defaultStepData)
                Result.Success(defaultStepData)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting daily step data for $date", e)
            Result.Error(e, "Failed to get step data for $date")
        }
    }

    /**
     * Enhanced implementation of getWeeklyStepData to ensure consistent 7-day data retrieval
     * This method guarantees that exactly 7 days of data will be returned, with zero values for missing days
     * Implements rolling 7-day window logic that maintains historical data when day transitions occur
     */
    override suspend fun getWeeklyStepData(startDate: LocalDate): Result<List<DailyStepData>> {
        val startTime = System.currentTimeMillis()
        return try {
            val userId = getCurrentUserId()
            if (userId.isEmpty()) {
                return Result.Error(Exception("No user logged in"), "No user logged in")
            }
            
            val today = LocalDate.now()
            
            // Implement rolling 7-day window logic
            // If no specific startDate is provided or startDate is in the future, use rolling window from today
            val effectiveStartDate = if (startDate.isAfter(today)) {
                today.minusDays(6) // Rolling 7-day window ending today
            } else {
                startDate
            }
            
            Log.d(TAG, "Getting weekly step data from $effectiveStartDate to ${effectiveStartDate.plusDays(6)} for user $userId")
            
            // Ensure we have historical data preserved before retrieving
            ensureHistoricalDataPreservation(userId, today)
            
            val weeklyData = mutableListOf<DailyStepData>()
            
            // Get user's step data map
            val userStepDataMap = _dailyStepDataMap.value[userId] ?: emptyMap()
            
            // Get data for exactly 7 days starting from effectiveStartDate
            for (dayOffset in 0..6) {
                val date = effectiveStartDate.plusDays(dayOffset.toLong())
                val dateString = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
                
                // Try to get existing data first
                val existingData = userStepDataMap[dateString]
                
                if (existingData != null) {
                    // We have data for this date
                    weeklyData.add(existingData)
                    Log.d(TAG, "Found existing step data for $date: ${existingData.steps} steps")
                } else {
                    // No data for this date, create zero-value entry to ensure complete 7-day display
                    val zeroValueData = createZeroValueStepData(date)
                    weeklyData.add(zeroValueData)
                    
                    // Save this zero-value data for consistency and future retrieval
                    saveDailyStepData(zeroValueData)
                    
                    Log.d(TAG, "Created zero-value step data for missing date $date")
                }
            }
            
            // Validate that we have exactly 7 days of data
            if (weeklyData.size != 7) {
                Log.w(TAG, "Expected 7 days of data but got ${weeklyData.size} days")
                
                // Ensure we have exactly 7 days by adding missing entries
                while (weeklyData.size < 7) {
                    val missingDate = if (weeklyData.isEmpty()) {
                        effectiveStartDate
                    } else {
                        weeklyData.last().date.plusDays(1)
                    }
                    
                    val zeroValueData = createZeroValueStepData(missingDate)
                    weeklyData.add(zeroValueData)
                    
                    // Save the missing data
                    saveDailyStepData(zeroValueData)
                    
                    Log.d(TAG, "Added missing zero-value step data for $missingDate")
                }
                
                // If we somehow have more than 7 days, trim the excess
                if (weeklyData.size > 7) {
                    weeklyData.subList(7, weeklyData.size).clear()
                    Log.d(TAG, "Trimmed excess step data to ensure exactly 7 days")
                }
            }
            
            // Sort the data by date to ensure proper chronological order
            weeklyData.sortBy { it.date }
            
            // Maintain rolling 7-day window in storage
            maintainRollingSevenDayWindow(userStepDataMap.toMutableMap(), today)
            
            // Log weekly data operation
            val totalSteps = weeklyData.sumOf { it.steps }
            HealthDataLogger.logWeeklyDataOperation("GET_WEEKLY_STEP_DATA", userId, startDate, weeklyData.size, totalSteps)

            Log.d(TAG, "Retrieved weekly step data: ${weeklyData.size} days from ${weeklyData.firstOrNull()?.date} to ${weeklyData.lastOrNull()?.date}")
            
            // Track operation performance
            val duration = System.currentTimeMillis() - startTime
            HealthDataMonitor.trackOperation("GET_WEEKLY_STEP_DATA", duration, true)
            
            Result.Success(weeklyData)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting weekly step data", e)
            
            // Track operation failure
            val duration = System.currentTimeMillis() - startTime
            HealthDataMonitor.trackOperation("GET_WEEKLY_STEP_DATA", duration, false)
            
            // Track error
            val userId = getCurrentUserId()
            HealthDataErrorTracker.trackError("GET_WEEKLY_STEP_DATA", userId, e, "StartDate: $startDate")
            
            Result.Error(e, "Failed to get weekly step data")
        }
    }
    
    /**
     * Create zero-value DailyStepData for missing days to ensure complete 7-day display
     */
    private fun createZeroValueStepData(date: LocalDate): DailyStepData {
        return DailyStepData(
            date = date,
            steps = 0,
            goal = 9000,
            calories = 0,
            caloriesGoal = 300,
            heartPoints = 0,
            heartPointsGoal = 50
        )
    }
    
    /**
     * Ensure historical data preservation during day transitions
     * This method checks for and preserves data from previous days
     */
    private suspend fun ensureHistoricalDataPreservation(userId: String, today: LocalDate) {
        try {
            // Check if we need to preserve yesterday's data
            val yesterday = today.minusDays(1)
            checkAndPreservePreviousDayData(userId, today)
            
            // Ensure we have a complete 7-day window of data
            ensureCompleteSevenDayWindow(userId, today)
            
            Log.d(TAG, "Historical data preservation completed for user $userId")
        } catch (e: Exception) {
            Log.e(TAG, "Error ensuring historical data preservation", e)
        }
    }

    /**
     * Enhanced implementation of getWeeklyCaloriesData to ensure consistent 7-day data retrieval
     * This method uses the same approach as getWeeklyStepData but returns a List instead of Result
     */
    override suspend fun getWeeklyCaloriesData(startDate: java.util.Date): List<DailyStepData> {
        return try {
            val userId = getCurrentUserId()
            if (userId.isEmpty()) {
                return emptyList()
            }
            
            val weeklyData = mutableListOf<DailyStepData>()
            val today = LocalDate.now()
            val startLocalDate = startDate.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate()
            
            // Get user's step data map
            val userStepDataMap = _dailyStepDataMap.value[userId] ?: emptyMap()
            
            // Get data for exactly 7 days starting from startDate
            for (dayOffset in 0..6) {
                val date = startLocalDate.plusDays(dayOffset.toLong())
                val dateString = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
                
                // Try to get existing data first
                val existingData = userStepDataMap[dateString]
                
                if (existingData != null) {
                    // We have data for this date
                    weeklyData.add(existingData)
                    Log.d(TAG, "Found existing calories data for $date: ${existingData.calories} calories")
                } else {
                    // No data for this date, create zero-value entry
                    val zeroValueData = DailyStepData(
                        date = date,
                        steps = 0,
                        goal = 9000,
                        calories = 0,
                        caloriesGoal = 300,
                        heartPoints = 0,
                        heartPointsGoal = 50
                    )
                    
                    weeklyData.add(zeroValueData)
                    
                    // Also save this zero-value data for consistency
                    saveDailyStepData(zeroValueData)
                    
                    Log.d(TAG, "Created zero-value calories data for $date")
                }
            }
            
            // Validate that we have exactly 7 days of data
            if (weeklyData.size != 7) {
                Log.w(TAG, "Expected 7 days of calories data but got ${weeklyData.size} days")
                
                // Ensure we have exactly 7 days by adding or removing entries
                while (weeklyData.size < 7) {
                    // Add zero-value entries for missing days
                    val missingDate = if (weeklyData.isEmpty()) {
                        startLocalDate
                    } else {
                        weeklyData.last().date.plusDays(1)
                    }
                    
                    weeklyData.add(DailyStepData(
                        date = missingDate,
                        steps = 0,
                        goal = 9000,
                        calories = 0,
                        caloriesGoal = 300,
                        heartPoints = 0,
                        heartPointsGoal = 50
                    ))
                    
                    Log.d(TAG, "Added missing zero-value calories data for $missingDate")
                }
                
                // If we somehow have more than 7 days, trim the excess
                if (weeklyData.size > 7) {
                    weeklyData.subList(7, weeklyData.size).clear()
                    Log.d(TAG, "Trimmed excess calories data to ensure exactly 7 days")
                }
            }
            
            Log.d(TAG, "Retrieved weekly calories data: ${weeklyData.size} days")
            weeklyData
        } catch (e: Exception) {
            Log.e(TAG, "Error getting weekly calories data", e)
            // Return 7 days of zero-value data as fallback
            val startLocalDate = startDate.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate()
            (0..6).map { dayOffset ->
                val date = startLocalDate.plusDays(dayOffset.toLong())
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

    /**
     * Enhanced implementation of getWeeklyHeartPointsData to ensure consistent 7-day data retrieval
     * This method uses the same approach as getWeeklyStepData but returns a List instead of Result
     */
    override suspend fun getWeeklyHeartPointsData(startDate: java.util.Date): List<DailyStepData> {
        return try {
            val userId = getCurrentUserId()
            if (userId.isEmpty()) {
                return emptyList()
            }
            
            val weeklyData = mutableListOf<DailyStepData>()
            val today = LocalDate.now()
            val startLocalDate = startDate.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate()
            
            // Get user's step data map
            val userStepDataMap = _dailyStepDataMap.value[userId] ?: emptyMap()
            
            // Get data for exactly 7 days starting from startDate
            for (dayOffset in 0..6) {
                val date = startLocalDate.plusDays(dayOffset.toLong())
                val dateString = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
                
                // Try to get existing data first
                val existingData = userStepDataMap[dateString]
                
                if (existingData != null) {
                    // We have data for this date
                    weeklyData.add(existingData)
                    Log.d(TAG, "Found existing heart points data for $date: ${existingData.heartPoints} points")
                } else {
                    // No data for this date, create zero-value entry
                    val zeroValueData = DailyStepData(
                        date = date,
                        steps = 0,
                        goal = 9000,
                        calories = 0,
                        caloriesGoal = 300,
                        heartPoints = 0,
                        heartPointsGoal = 50
                    )
                    
                    weeklyData.add(zeroValueData)
                    
                    // Also save this zero-value data for consistency
                    saveDailyStepData(zeroValueData)
                    
                    Log.d(TAG, "Created zero-value heart points data for $date")
                }
            }
            
            // Validate that we have exactly 7 days of data
            if (weeklyData.size != 7) {
                Log.w(TAG, "Expected 7 days of heart points data but got ${weeklyData.size} days")
                
                // Ensure we have exactly 7 days by adding or removing entries
                while (weeklyData.size < 7) {
                    // Add zero-value entries for missing days
                    val missingDate = if (weeklyData.isEmpty()) {
                        startLocalDate
                    } else {
                        weeklyData.last().date.plusDays(1)
                    }
                    
                    weeklyData.add(DailyStepData(
                        date = missingDate,
                        steps = 0,
                        goal = 9000,
                        calories = 0,
                        caloriesGoal = 300,
                        heartPoints = 0,
                        heartPointsGoal = 50
                    ))
                    
                    Log.d(TAG, "Added missing zero-value heart points data for $missingDate")
                }
                
                // If we somehow have more than 7 days, trim the excess
                if (weeklyData.size > 7) {
                    weeklyData.subList(7, weeklyData.size).clear()
                    Log.d(TAG, "Trimmed excess heart points data to ensure exactly 7 days")
                }
            }
            
            Log.d(TAG, "Retrieved weekly heart points data: ${weeklyData.size} days")
            weeklyData
        } catch (e: Exception) {
            Log.e(TAG, "Error getting weekly heart points data", e)
            // Return 7 days of zero-value data as fallback
            val startLocalDate = startDate.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate()
            (0..6).map { dayOffset ->
                val date = startLocalDate.plusDays(dayOffset.toLong())
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

    /**
     * Enhanced implementation of saveDailyStepData to ensure proper persistence
     * This method includes data validation and atomic updates
     */
    /**
     * Enhanced implementation of saveDailyStepData to ensure proper persistence
     * This method includes data validation and atomic updates
     */
    override suspend fun saveDailyStepData(stepData: DailyStepData): Result<Unit> {
        return try {
            val userId = getCurrentUserId()
            if (userId.isEmpty()) {
                Log.w(TAG, "No user logged in, skipping daily step data save")
                return Result.Error(Exception("No user logged in"), "No user logged in")
            }
            
            // Validate the step data
            if (!validateStepData(stepData)) {
                return Result.Error(Exception("Invalid step data"), "Invalid step data")
            }
            
            val dateString = stepData.date.format(DateTimeFormatter.ISO_LOCAL_DATE)
            
            // Get user's step data map or create new one
            val userStepDataMap = _dailyStepDataMap.value[userId] ?: emptyMap()
            val updatedUserMap = userStepDataMap.toMutableMap()
            
            // Store the step data
            updatedUserMap[dateString] = stepData
            
            // Maintain a rolling 7-day window
            maintainRollingSevenDayWindow(updatedUserMap, LocalDate.now())
            
            // Update the global map with the user's updated map (atomic update)
            val globalMap = _dailyStepDataMap.value.toMutableMap()
            globalMap[userId] = updatedUserMap
            _dailyStepDataMap.value = globalMap
            
            // If this is today's data, also update the previous day cache for tomorrow
            if (stepData.date.isEqual(LocalDate.now())) {
                val updatedPreviousDayCache = _previousDayDataCache.value.toMutableMap()
                updatedPreviousDayCache[userId] = stepData
                _previousDayDataCache.value = updatedPreviousDayCache
            }
            
            Log.d(TAG, "Saved daily step data for user $userId: ${stepData.steps} steps for ${stepData.date}")
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving daily step data", e)
            Result.Error(e, "Failed to save daily step data")
        }
    }
    
    /**
     * Validate step data to ensure data integrity
     */
    private fun validateStepData(stepData: DailyStepData): Boolean {
        // Ensure steps is not negative
        if (stepData.steps < 0) {
            Log.w(TAG, "Invalid step data: negative steps value ${stepData.steps}")
            return false
        }
        
        // Ensure calories is not negative
        if (stepData.calories < 0) {
            Log.w(TAG, "Invalid step data: negative calories value ${stepData.calories}")
            return false
        }
        
        // Ensure heart points is not negative
        if (stepData.heartPoints < 0) {
            Log.w(TAG, "Invalid step data: negative heart points value ${stepData.heartPoints}")
            return false
        }
        
        // Ensure date is not in the future
        if (stepData.date.isAfter(LocalDate.now())) {
            // Future dates are allowed but should have zero values
            if (stepData.steps > 0 || stepData.calories > 0 || stepData.heartPoints > 0) {
                Log.w(TAG, "Invalid step data: future date ${stepData.date} with non-zero values")
                return false
            }
        }
        
        return true
    }

    override suspend fun saveWeeklyHealthSummary(userId: String, weekStartDate: LocalDate): Result<Unit> {
        return try {
            Log.d(TAG, "Saving weekly health summary for user $userId, week starting $weekStartDate")

            val weekEndDate = weekStartDate.plusDays(6)
            val weeklyData = mutableListOf<DailyStepData>()

            // Collect all data for the week
            for (dayOffset in 0..6) {
                val date = weekStartDate.plusDays(dayOffset.toLong())
                val dayResult = getDailyStepData(date)
                if (dayResult is Result.Success) {
                    weeklyData.add(dayResult.data)
                }
            }

            if (weeklyData.isNotEmpty()) {
                // Calculate weekly totals and averages
                val totalSteps = weeklyData.sumOf { it.steps }
                val totalCalories = weeklyData.sumOf { it.calories }
                val totalHeartPoints = weeklyData.sumOf { it.heartPoints }

                val avgSteps = totalSteps / weeklyData.size
                val avgCalories = totalCalories / weeklyData.size
                val avgHeartPoints = totalHeartPoints / weeklyData.size

                // Check goal achievements
                val stepsGoalAchieved = totalSteps >= 63000 // 9000 * 7 days
                val caloriesGoalAchieved = totalCalories >= 2100 // 300 * 7 days
                val heartPointsGoalAchieved = totalHeartPoints >= 350 // 50 * 7 days

                // Create weekly summary
                val weeklySummary = WeeklyHealthSummary(
                    userId = userId,
                    weekStartDate = weekStartDate,
                    weekEndDate = weekEndDate,
                    totalSteps = totalSteps,
                    averageSteps = avgSteps,
                    totalCalories = totalCalories,
                    averageCalories = avgCalories,
                    totalHeartPoints = totalHeartPoints,
                    averageHeartPoints = avgHeartPoints,
                    stepsGoalAchieved = stepsGoalAchieved,
                    caloriesGoalAchieved = caloriesGoalAchieved,
                    heartPointsGoalAchieved = heartPointsGoalAchieved
                )

                // Save to cache (in real app, this would go to Room database)
                val weekKey = "${userId}_${weekStartDate}"
                val updatedSummaries = _weeklyHealthSummaries.value.toMutableMap()
                updatedSummaries[weekKey] = weeklySummary
                _weeklyHealthSummaries.value = updatedSummaries

                Log.d(TAG, "Weekly summary saved: $totalSteps steps, $totalCalories kcal, $totalHeartPoints points")
                Result.Success(Unit)
            } else {
                Result.Error(Exception("No data available for week"), "No data for the specified week")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving weekly health summary", e)
            Result.Error(e, "Failed to save weekly health summary")
        }
    }

    override suspend fun cleanupOldWeeklyData(userId: String): Result<Unit> {
        return try {
            Log.d(TAG, "Cleaning up old weekly data for user $userId")

            val today = LocalDate.now()
            val currentWeekStart = today.minusDays(today.dayOfWeek.value.toLong() - 1)
            val cutoffDate = currentWeekStart.minusDays(8) // Keep current week + 1 day buffer

            // Clean up daily step data
            val userStepDataMap = _dailyStepDataMap.value[userId]
            if (userStepDataMap != null) {
                val dailyDataToRemove = mutableListOf<String>()
                userStepDataMap.forEach { (dateString, stepData) ->
                    if (stepData.date.isBefore(cutoffDate)) {
                        dailyDataToRemove.add(dateString)
                    }
                }

                if (dailyDataToRemove.isNotEmpty()) {
                    val globalMap = _dailyStepDataMap.value.toMutableMap()
                    val updatedUserMap = userStepDataMap.toMutableMap()
                    
                    dailyDataToRemove.forEach { dateKey ->
                        updatedUserMap.remove(dateKey)
                    }
                    
                    globalMap[userId] = updatedUserMap
                    _dailyStepDataMap.value = globalMap
                    
                    Log.d(TAG, "Removed ${dailyDataToRemove.size} old daily data entries for user $userId")
                }
            }

            // Clean up old weekly summaries (keep last 4 weeks)
            val weeklyDataToRemove = mutableListOf<String>()
            _weeklyHealthSummaries.value.forEach { (weekKey, summary) ->
                if (summary.userId == userId && summary.weekStartDate.isBefore(cutoffDate.minusWeeks(3))) {
                    weeklyDataToRemove.add(weekKey)
                }
            }

            if (weeklyDataToRemove.isNotEmpty()) {
                val updatedWeeklyData = _weeklyHealthSummaries.value.toMutableMap()
                weeklyDataToRemove.forEach { weekKey ->
                    updatedWeeklyData.remove(weekKey)
                }
                _weeklyHealthSummaries.value = updatedWeeklyData
                Log.d(TAG, "Removed ${weeklyDataToRemove.size} old weekly summary entries")
            }

            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up old weekly data", e)
            Result.Error(e, "Failed to cleanup old data")
        }
    }

    override suspend fun getUserWeeklyHistory(userId: String, weeksCount: Int): Result<List<WeeklyHealthSummary>> {
        return try {
            val userSummaries = _weeklyHealthSummaries.value.values
                .filter { it.userId == userId }
                .sortedByDescending { it.weekStartDate }
                .take(weeksCount)

            Result.Success(userSummaries)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user weekly history", e)
            Result.Error(e, "Failed to get weekly history")
        }
    }

    /**
     * Enhanced implementation of performDailyDataMaintenance to ensure proper data maintenance during day transitions
     * This method preserves previous day data, maintains the rolling 7-day window, and performs weekly summaries
     */
    override suspend fun performDailyDataMaintenance(userId: String): Result<Unit> {
        return try {
            Log.d(TAG, "Performing daily data maintenance for user $userId")

            val today = LocalDate.now()
            val yesterday = today.minusDays(1)
            val yesterdayString = yesterday.format(DateTimeFormatter.ISO_LOCAL_DATE)
            val currentWeekStart = today.minusDays(today.dayOfWeek.value.toLong() - 1)

            // Preserve yesterday's data
            preservePreviousDayMetrics(userId, yesterdayString)
            
            // Ensure we have a complete 7-day window of data
            ensureCompleteSevenDayWindow(userId, today)

            // If it's a new week (Monday), save the previous week's summary
            if (today.dayOfWeek.value == 1) { // Monday
                val lastWeekStart = currentWeekStart.minusWeeks(1)
                saveWeeklyHealthSummary(userId, lastWeekStart)
            }

            // Always clean up old data
            cleanupOldWeeklyData(userId)

            // Log data maintenance completion
            HealthDataLogger.logDataMaintenance("DAILY_MAINTENANCE", userId, "Completed successfully - preserved previous day data, ensured 7-day window, cleaned old data")
            
            Log.d(TAG, "Daily maintenance completed successfully")
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error performing daily maintenance", e)
            Result.Error(e, "Failed to perform daily maintenance")
        }
    }
    
    /**
     * Ensure we have a complete 7-day window of data
     * This method fills in any missing days with zero-value data
     */
    private suspend fun ensureCompleteSevenDayWindow(userId: String, today: LocalDate) {
        try {
            // Calculate the start date (7 days ago)
            val startDate = today.minusDays(6)
            
            // Get user's step data map
            val userStepDataMap = _dailyStepDataMap.value[userId] ?: emptyMap()
            val updatedUserMap = userStepDataMap.toMutableMap()
            
            // Check each day in the 7-day window
            for (dayOffset in 0..6) {
                val date = startDate.plusDays(dayOffset.toLong())
                val dateString = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
                
                // If we don't have data for this date, create zero-value data
                if (!updatedUserMap.containsKey(dateString)) {
                    val zeroValueData = DailyStepData(
                        date = date,
                        steps = 0,
                        goal = 9000,
                        calories = 0,
                        caloriesGoal = 300,
                        heartPoints = 0,
                        heartPointsGoal = 50
                    )
                    
                    updatedUserMap[dateString] = zeroValueData
                    Log.d(TAG, "Created zero-value data for missing date $date")
                }
            }
            
            // Update the global map with the user's updated map
            val globalMap = _dailyStepDataMap.value.toMutableMap()
            globalMap[userId] = updatedUserMap
            _dailyStepDataMap.value = globalMap
            
            Log.d(TAG, "Ensured complete 7-day window of data for user $userId")
        } catch (e: Exception) {
            Log.e(TAG, "Error ensuring complete 7-day window", e)
        }
    }

    /**
     * Preserve previous day's health metrics when date changes
     * This ensures we don't lose data when the date changes
     */
    /**
     * Enhanced implementation of preservePreviousDayMetrics to ensure data persistence during day transitions
     * This method preserves both health metrics and step data for the previous day
     */
    override suspend fun preservePreviousDayMetrics(userId: String, date: String): Result<Unit> {
        return try {
            if (userId.isEmpty()) {
                return Result.Error(Exception("Invalid user ID"), "Invalid user ID")
            }
            
            // Parse the date
            val previousDay = try {
                LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE)
            } catch (e: Exception) {
                return Result.Error(e, "Invalid date format")
            }
            
            // Get the metrics for the specified date
            val metrics = _healthMetricsMap.value[userId]?.get(date)
            
            // Get the step data for the specified date
            val previousDayString = previousDay.format(DateTimeFormatter.ISO_LOCAL_DATE)
            val userStepDataMap = _dailyStepDataMap.value[userId] ?: emptyMap()
            val previousDayData = userStepDataMap[previousDayString]
            
            // Store both metrics and step data
            if (metrics != null) {
                // Store metrics in a special "previous day metrics" cache
                val previousDayKey = "previous_day_${userId}_${date}"
                val userMetricsMap = _healthMetricsMap.value.toMutableMap()
                
                // Get or create the user's metrics map
                val userMetrics = userMetricsMap[userId]?.toMutableMap() ?: mutableMapOf()
                
                // Add the previous day metrics
                userMetrics[previousDayKey] = metrics
                
                // Update the user's metrics in the main map
                userMetricsMap[userId] = userMetrics
                
                // Update the main map
                _healthMetricsMap.value = userMetricsMap
                
                Log.d(TAG, "Preserved previous day metrics for user $userId on date $date: $metrics")
            }
            
            // Store step data in the previous day cache
            if (previousDayData != null) {
                val updatedPreviousDayCache = _previousDayDataCache.value.toMutableMap()
                updatedPreviousDayCache[userId] = previousDayData
                _previousDayDataCache.value = updatedPreviousDayCache
                
                Log.d(TAG, "Preserved previous day step data for user $userId: ${previousDayData.steps} steps for $previousDay")
            }
            
            if (metrics != null || previousDayData != null) {
                Result.Success(Unit)
            } else {
                Log.d(TAG, "No data found to preserve for user $userId on date $date")
                Result.Success(Unit)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error preserving previous day metrics", e)
            Result.Error(e, "Failed to preserve previous day metrics")
        }
    }
    
    /**
     * Get preserved previous day's health metrics
     */
    /**
     * Enhanced implementation of getPreviousDayMetrics to retrieve preserved metrics
     * This method checks both the metrics cache and the step data cache
     */
    override suspend fun getPreviousDayMetrics(userId: String, date: String): Result<HealthMetrics?> {
        return try {
            if (userId.isEmpty()) {
                return Result.Error(Exception("Invalid user ID"), "Invalid user ID")
            }
            
            // Parse the date
            val previousDay = try {
                LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE)
            } catch (e: Exception) {
                return Result.Error(e, "Invalid date format")
            }
            
            // First try to get from the metrics cache
            val previousDayKey = "previous_day_${userId}_${date}"
            val metrics = _healthMetricsMap.value[userId]?.get(previousDayKey)
            
            if (metrics != null) {
                Log.d(TAG, "Retrieved preserved metrics for user $userId on date $date: $metrics")
                return Result.Success(metrics)
            }
            
            // If not found in metrics cache, try to get from the step data cache
            val previousDayData = _previousDayDataCache.value[userId]
            
            if (previousDayData != null && previousDayData.date.format(DateTimeFormatter.ISO_LOCAL_DATE) == date) {
                // Convert DailyStepData to HealthMetrics
                val healthMetrics = HealthMetrics(
                    steps = HealthMetric(previousDayData.steps, previousDayData.goal),
                    calories = HealthMetric(previousDayData.calories, previousDayData.caloriesGoal),
                    heartPoints = HealthMetric(previousDayData.heartPoints, previousDayData.heartPointsGoal)
                )
                
                Log.d(TAG, "Retrieved preserved step data for user $userId on date $date: ${previousDayData.steps} steps")
                return Result.Success(healthMetrics)
            }
            
            // If not found in either cache, try to get from the regular step data map
            val userStepDataMap = _dailyStepDataMap.value[userId] ?: emptyMap()
            val stepData = userStepDataMap[date]
            
            if (stepData != null) {
                // Convert DailyStepData to HealthMetrics
                val healthMetrics = HealthMetrics(
                    steps = HealthMetric(stepData.steps, stepData.goal),
                    calories = HealthMetric(stepData.calories, stepData.caloriesGoal),
                    heartPoints = HealthMetric(stepData.heartPoints, stepData.heartPointsGoal)
                )
                
                Log.d(TAG, "Retrieved step data from map for user $userId on date $date: ${stepData.steps} steps")
                return Result.Success(healthMetrics)
            }
            
            Log.d(TAG, "No preserved metrics found for user $userId on date $date")
            Result.Success(null)
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving preserved metrics", e)
            Result.Error(e, "Failed to retrieve preserved metrics")
        }
    }
}