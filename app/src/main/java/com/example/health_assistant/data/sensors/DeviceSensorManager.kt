package com.example.health_assistant.data.sensors

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import androidx.core.content.edit
import com.example.health_assistant.auth.session.SessionManager
import com.example.health_assistant.services.StepTrackingService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager for device sensors - syncs with StepTrackingService for consistent step counting
 * Ensures step counting works 24/7 and resets properly at midnight
 * Now with proper user isolation for multi-user support
 */
@Singleton
class DeviceSensorManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sessionManager: SessionManager
) : SensorEventListener {

    companion object {
        private const val TAG = "DeviceSensorManager"
        // Use same SharedPreferences as StepTrackingService for consistency
        private const val PREFS_NAME_PREFIX = "step_service_prefs_user_"
        private const val KEY_DAILY_STEPS = "service_daily_steps"
        private const val KEY_LAST_DATE = "service_last_date"
        private const val KEY_INITIAL_STEP_COUNT = "service_initial_step_count"
        private const val KEY_SERVICE_ENABLED = "service_enabled"
    }

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    private val stepDetectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    // State flows for UI observation - these are now user-specific
    private val _stepCount = MutableStateFlow(0)
    val stepCount: StateFlow<Int> = _stepCount.asStateFlow()

    private val _isTracking = MutableStateFlow(false)
    val isTracking: StateFlow<Boolean> = _isTracking.asStateFlow()

    private val _sensorAvailable = MutableStateFlow(false)
    val sensorAvailable: StateFlow<Boolean> = _sensorAvailable.asStateFlow()

    // Internal tracking variables
    private var currentDate: String = getCurrentDateString()
    private var isListenerRegistered = false
    
    // Map of user IDs to their step counts
    private val userStepCounts = mutableMapOf<String, Int>()
    
    // Global step count from sensor (cumulative since boot)
    private var globalStepCount = 0
    
    // Last step increments for each user
    private val lastUserStepIncrements = mutableMapOf<String, Int>()

    init {
        checkSensorAvailability()
        loadCurrentUserStepData()
    }
    
    /**
     * Get user-specific SharedPreferences
     */
    private fun getUserPrefs(): SharedPreferences {
        val userId = getCurrentUserId()
        val prefsName = if (userId.isEmpty()) {
            "${PREFS_NAME_PREFIX}default"
        } else {
            "${PREFS_NAME_PREFIX}${userId}"
        }
        return context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
    }
    
    /**
     * Get current user ID from SessionManager
     */
    private fun getCurrentUserId(): String {
        return sessionManager.getCurrentUserId() ?: ""
    }

    /**
     * Start step tracking - registers sensors and starts background service
     */
    fun startTracking() {
        Log.d(TAG, "Starting step tracking")

        if (!_sensorAvailable.value) {
            Log.e(TAG, "Step counter sensor not available")
            return
        }

        // Register sensor listeners
        registerSensorListeners()
        
        // Start background service for continuous tracking
        startBackgroundService()

        // Load current step data for the current user
        loadCurrentUserStepData()

        _isTracking.value = true
        saveServiceState(true)

        Log.d(TAG, "Step tracking started successfully")
    }

    /**
     * Stop step tracking
     */
    fun stopTracking() {
        Log.d(TAG, "Stopping step tracking")

        unregisterSensorListeners()
        stopBackgroundService()

        _isTracking.value = false
        saveServiceState(false)

        Log.d(TAG, "Step tracking stopped")
    }

    /**
     * Check if sensors are available on device
     */
    private fun checkSensorAvailability() {
        val hasStepCounter = stepCounterSensor != null
        val hasStepDetector = stepDetectorSensor != null

        _sensorAvailable.value = hasStepCounter || hasStepDetector

        Log.d(TAG, "Sensor availability - Counter: $hasStepCounter, Detector: $hasStepDetector")
    }

    /**
     * Register sensor listeners for step tracking
     */
    private fun registerSensorListeners() {
        if (isListenerRegistered) return

        stepCounterSensor?.let { sensor ->
            val registered = sensorManager.registerListener(
                this,
                sensor,
                SensorManager.SENSOR_DELAY_NORMAL
            )

            if (registered) {
                isListenerRegistered = true
                Log.d(TAG, "Step counter sensor registered")
            } else {
                Log.e(TAG, "Failed to register step counter sensor")
            }
        }

        // Fallback to step detector if counter not available
        if (!isListenerRegistered && stepDetectorSensor != null) {
            val registered = sensorManager.registerListener(
                this,
                stepDetectorSensor,
                SensorManager.SENSOR_DELAY_NORMAL
            )

            if (registered) {
                isListenerRegistered = true
                Log.d(TAG, "Step detector sensor registered as fallback")
            }
        }
    }

    /**
     * Unregister sensor listeners
     */
    private fun unregisterSensorListeners() {
        if (isListenerRegistered) {
            sensorManager.unregisterListener(this)
            isListenerRegistered = false
            Log.d(TAG, "Sensor listeners unregistered")
        }
    }

    /**
     * Start background service for 24/7 tracking
     */
    private fun startBackgroundService() {
        try {
            val serviceIntent = Intent(context, StepTrackingService::class.java)
            context.startForegroundService(serviceIntent)
            Log.d(TAG, "Background service started")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start background service", e)
        }
    }

    /**
     * Stop background service
     */
    private fun stopBackgroundService() {
        try {
            val serviceIntent = Intent(context, StepTrackingService::class.java)
            context.stopService(serviceIntent)
            Log.d(TAG, "Background service stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop background service", e)
        }
    }

    /**
     * Handle sensor data changes
     */
    override fun onSensorChanged(event: SensorEvent?) {
        event?.let { sensorEvent ->
            when (sensorEvent.sensor.type) {
                Sensor.TYPE_STEP_COUNTER -> {
                    handleStepCounterData(sensorEvent.values[0].toLong())
                }
                Sensor.TYPE_STEP_DETECTOR -> {
                    handleStepDetectorData()
                }
            }
        }
    }

    /**
     * Handle step counter sensor data (cumulative steps since boot)
     * EMERGENCY FIX: Limit step increments to prevent UI freezing
     */
    private fun handleStepCounterData(totalSteps: Long) {
        coroutineScope.launch {
            checkForDateChange()
            
            // Update global step count
            val previousGlobalStepCount = globalStepCount
            globalStepCount = totalSteps.toInt()
            
            // Calculate increment
            var stepIncrement = 0
            
            // Handle device reboot case where step counter resets to 0
            if (globalStepCount < previousGlobalStepCount && previousGlobalStepCount > 1000) {
                // Device likely rebooted, treat current steps as the increment
                stepIncrement = globalStepCount
            } else {
                // Normal case - calculate increment
                stepIncrement = maxOf(0, globalStepCount - previousGlobalStepCount)
            }
            
            // EMERGENCY FIX: Limit step increment to prevent unrealistic values
            if (stepIncrement > 1000) {
                Log.w(TAG, "Large step increment detected ($stepIncrement), limiting to 100 to prevent UI issues")
                stepIncrement = 100
            }
            
            // Only update if there's a positive increment (user actually walked)
            if (stepIncrement > 0) {
                // Update the current user's step count
                updateCurrentUserSteps(stepIncrement)
            }
        }
    }

    /**
     * Handle step detector sensor data (individual step events)
     */
    private fun handleStepDetectorData() {
        coroutineScope.launch {
            checkForDateChange()
            
            // Update the current user's step count by 1
            updateCurrentUserSteps(1)
        }
    }
    
    /**
     * Update the current user's step count
     */
    private fun updateCurrentUserSteps(stepIncrement: Int) {
        val userId = getCurrentUserId()
        if (userId.isEmpty()) {
            Log.d(TAG, "No user logged in, skipping step update")
            return
        }
        
        // Get current user's step count or default to 0
        val currentUserSteps = userStepCounts[userId] ?: 0
        
        // Add the increment to the user's step count
        val newUserSteps = currentUserSteps + stepIncrement
        
        // Update the user step counts map
        userStepCounts[userId] = newUserSteps
        
        // Update the step count flow
        _stepCount.value = newUserSteps
        
        // Save to user-specific SharedPreferences
        saveUserStepCount(userId, newUserSteps)
        
        Log.d(TAG, "Updated steps for user $userId: $currentUserSteps + $stepIncrement = $newUserSteps")
    }
    
    /**
     * Save user step count to SharedPreferences
     */
    private fun saveUserStepCount(userId: String, steps: Int) {
        try {
            val prefsName = "${PREFS_NAME_PREFIX}${userId}"
            val userPrefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            userPrefs.edit {
                putInt(KEY_DAILY_STEPS, steps)
                putString(KEY_LAST_DATE, getCurrentDateString())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving user step count", e)
        }
    }

    /**
     * Load step data for the current user
     */
    private fun loadCurrentUserStepData() {
        coroutineScope.launch {
            try {
                val userId = getCurrentUserId()
                if (userId.isEmpty()) {
                    Log.d(TAG, "No user logged in, skipping step data loading")
                    _stepCount.value = 0
                    return@launch
                }
                
                // Get user-specific SharedPreferences
                val prefsName = "${PREFS_NAME_PREFIX}${userId}"
                val userPrefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
                
                // Check for date change
                val lastDate = userPrefs.getString(KEY_LAST_DATE, getCurrentDateString()) ?: getCurrentDateString()
                val today = getCurrentDateString()
                
                if (lastDate != today) {
                    // Date has changed, reset step count for this user
                    userPrefs.edit {
                        putInt(KEY_DAILY_STEPS, 0)
                        putString(KEY_LAST_DATE, today)
                    }
                    userStepCounts[userId] = 0
                    _stepCount.value = 0
                    Log.d(TAG, "Date changed for user $userId, reset step count to 0")
                } else {
                    // Same date, load saved step count
                    val savedSteps = userPrefs.getInt(KEY_DAILY_STEPS, 0)
                    userStepCounts[userId] = savedSteps
                    _stepCount.value = savedSteps
                    Log.d(TAG, "Loaded step count for user $userId: $savedSteps steps")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading step data", e)
            }
        }
    }

    /**
     * Reset step count for a specific user
     */
    fun resetUserStepCount(userId: String) {
        coroutineScope.launch {
            try {
                // Reset in memory
                userStepCounts[userId] = 0
                
                // Reset in SharedPreferences
                val prefsName = "${PREFS_NAME_PREFIX}${userId}"
                val userPrefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
                userPrefs.edit {
                    putInt(KEY_DAILY_STEPS, 0)
                    putString(KEY_LAST_DATE, getCurrentDateString())
                }
                
                // If this is the current user, also update the flow
                if (userId == getCurrentUserId()) {
                    _stepCount.value = 0
                }
                
                Log.d(TAG, "Reset step count for user $userId")
            } catch (e: Exception) {
                Log.e(TAG, "Error resetting step count for user $userId", e)
            }
        }
    }

    /**
     * EMERGENCY RESET - Force reset all step data and stop all tracking
     * Use this when the UI becomes unresponsive
     */
    fun emergencyReset() {
        try {
            Log.w(TAG, "EMERGENCY RESET - Stopping all tracking and resetting data")
            
            // Stop all sensor listeners immediately
            unregisterSensorListeners()
            
            // Stop background service
            stopBackgroundService()
            
            // Reset all in-memory data
            userStepCounts.clear()
            globalStepCount = 0
            
            // Reset UI state flows
            _stepCount.value = 0
            _isTracking.value = false
            
            // Clear all SharedPreferences data
            val currentUserId = getCurrentUserId()
            if (currentUserId.isNotEmpty()) {
                val prefsName = "${PREFS_NAME_PREFIX}${currentUserId}"
                val userPrefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
                userPrefs.edit().clear().apply()
            }
            
            Log.w(TAG, "EMERGENCY RESET COMPLETED - All tracking stopped and data cleared")
        } catch (e: Exception) {
            Log.e(TAG, "Error during emergency reset", e)
        }
    }

    /**
     * Check if date has changed (midnight reset)
     */
    private fun checkForDateChange() {
        val today = getCurrentDateString()
        
        if (currentDate != today) {
            Log.d(TAG, "Date changed from $currentDate to $today")
            
            // Before resetting, save previous day's data for activity card generation
            savePreviousDayData(currentDate)
            
            // Update current date
            currentDate = today
            
            // Reset step counts for all users at midnight
            resetAllUserStepCounts()
        }
    }
    
    /**
     * Save previous day's data for activity card generation
     */
    private fun savePreviousDayData(previousDate: String) {
        try {
            val userId = getCurrentUserId()
            if (userId.isEmpty()) {
                Log.d(TAG, "No user logged in, skipping previous day data saving")
                return
            }
            
            val steps = userStepCounts[userId] ?: 0
            
            // Save to a special "previous day" SharedPreferences
            val previousDayPrefs = context.getSharedPreferences("previous_day_data_${userId}", Context.MODE_PRIVATE)
            previousDayPrefs.edit {
                putInt("steps", steps)
                putString("date", previousDate)
                putLong("timestamp", System.currentTimeMillis())
            }
            
            Log.d(TAG, "Saved previous day data for user $userId: $steps steps on $previousDate")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving previous day data", e)
        }
    }
    
    /**
     * Get previous day's step count for a user
     */
    fun getPreviousDaySteps(userId: String): Int {
        try {
            val previousDayPrefs = context.getSharedPreferences("previous_day_data_${userId}", Context.MODE_PRIVATE)
            val steps = previousDayPrefs.getInt("steps", 0)
            val date = previousDayPrefs.getString("date", "") ?: ""
            
            Log.d(TAG, "Retrieved previous day data for user $userId: $steps steps on $date")
            return steps
        } catch (e: Exception) {
            Log.e(TAG, "Error getting previous day steps", e)
            return 0
        }
    }
    
    /**
     * Reset step counts for all users (called at midnight)
     */
    private fun resetAllUserStepCounts() {
        coroutineScope.launch {
            try {
                // Reset all in-memory step counts
                userStepCounts.clear()
                
                // Reset current user's step count in the flow
                _stepCount.value = 0
                
                Log.d(TAG, "Reset step counts for all users at midnight")
            } catch (e: Exception) {
                Log.e(TAG, "Error resetting all user step counts", e)
            }
        }
    }

    /**
     * Save service state to SharedPreferences
     */
    private fun saveServiceState(enabled: Boolean) {
        getUserPrefs().edit {
            putBoolean(KEY_SERVICE_ENABLED, enabled)
        }
    }

    /**
     * Get current date as string
     */
    private fun getCurrentDateString(): String {
        return LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
    }

    /**
     * Manually refresh step count from service
     */
    fun refreshStepCount() {
        loadCurrentUserStepData()
    }

    /**
     * Get sensor debug information
     */
    fun getSensorInfo(): String {
        val hasCounter = stepCounterSensor != null
        val hasDetector = stepDetectorSensor != null
        val userId = getCurrentUserId()
        val userSteps = userStepCounts[userId] ?: 0

        return """
            Step Counter Sensor: $hasCounter
            Step Detector Sensor: $hasDetector
            Current User: ${if (userId.isEmpty()) "None" else userId}
            Current User Steps: $userSteps
            Global Step Count: $globalStepCount
            Tracking Active: ${_isTracking.value}
            Date: $currentDate
        """.trimIndent()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not used
    }
}