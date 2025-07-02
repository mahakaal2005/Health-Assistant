package com.example.health_assistant.data.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager for device sensors - works without Google Fit app
 * Uses built-in step counter and other sensors directly
 */
@Singleton
class DeviceSensorManager @Inject constructor(
    @ApplicationContext private val context: Context
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    private val stepDetectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)

    private val _stepCount = MutableStateFlow(0)
    val stepCount: StateFlow<Int> = _stepCount.asStateFlow()

    private val _isTracking = MutableStateFlow(false)
    val isTracking: StateFlow<Boolean> = _isTracking.asStateFlow()

    private var initialStepCount = 0
    private var sessionStartSteps = 0
    private var dailySteps = 0

    companion object {
        private const val TAG = "DeviceSensorManager"
        private const val PREFS_NAME = "HealthSensorData"
        private const val KEY_DAILY_STEPS = "daily_steps"
        private const val KEY_LAST_UPDATE_DATE = "last_update_date"
        private const val KEY_SESSION_START_STEPS = "session_start_steps"
    }

    init {
        checkSensorAvailability()
        loadSavedData()
        // CRITICAL FIX: Start tracking immediately if sensors are available
        if (hasSensorsAvailable()) {
            Log.d(TAG, "Sensors available, starting tracking immediately...")
            startTracking()
        }
    }

    /**
     * Check if device has necessary sensors
     */
    fun hasSensorsAvailable(): Boolean {
        val hasStepCounter = stepCounterSensor != null
        val hasStepDetector = stepDetectorSensor != null

        Log.d(TAG, "Sensor availability - Step Counter: $hasStepCounter, Step Detector: $hasStepDetector")
        return hasStepCounter || hasStepDetector
    }

    /**
     * Start tracking steps using device sensors
     */
    fun startTracking(): Boolean {
        if (!hasSensorsAvailable()) {
            Log.w(TAG, "No step sensors available on this device")
            return false
        }

        return try {
            // Try step counter first (more accurate, cumulative)
            stepCounterSensor?.let { sensor ->
                val registered = sensorManager.registerListener(
                    this,
                    sensor,
                    SensorManager.SENSOR_DELAY_UI
                )
                if (registered) {
                    _isTracking.value = true
                    Log.d(TAG, "Step counter sensor registered successfully")
                    return true
                }
            }

            // Fallback to step detector
            stepDetectorSensor?.let { sensor ->
                val registered = sensorManager.registerListener(
                    this,
                    sensor,
                    SensorManager.SENSOR_DELAY_UI
                )
                if (registered) {
                    _isTracking.value = true
                    Log.d(TAG, "Step detector sensor registered successfully")
                    return true
                }
            }

            Log.w(TAG, "Failed to register any step sensors")
            false
        } catch (e: Exception) {
            Log.e(TAG, "Error starting sensor tracking", e)
            false
        }
    }

    /**
     * Stop tracking steps
     */
    fun stopTracking() {
        try {
            sensorManager.unregisterListener(this)
            _isTracking.value = false
            saveData()
            Log.d(TAG, "Sensor tracking stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping sensor tracking", e)
        }
    }

    /**
     * Get current daily step count
     */
    fun getDailySteps(): Int {
        checkAndResetDailyData()
        return dailySteps
    }

    /**
     * Estimate calories burned based on steps
     * Average: 0.04-0.06 calories per step
     */
    fun getEstimatedCalories(): Int {
        val steps = getDailySteps()
        return (steps * 0.05).toInt() // Conservative estimate
    }

    /**
     * Estimate heart points based on activity
     * Rough estimate: 1 heart point per 100 steps of moderate activity
     */
    fun getEstimatedHeartPoints(): Int {
        val steps = getDailySteps()
        return (steps / 100).coerceAtMost(50) // Max 50 points per day
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let { sensorEvent ->
            when (sensorEvent.sensor.type) {
                Sensor.TYPE_STEP_COUNTER -> {
                    handleStepCounterData(sensorEvent.values[0].toInt())
                }
                Sensor.TYPE_STEP_DETECTOR -> {
                    handleStepDetectorData()
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        Log.d(TAG, "Sensor accuracy changed: ${sensor?.name}, accuracy: $accuracy")
    }

    private fun handleStepCounterData(totalSteps: Int) {
        if (initialStepCount == 0) {
            // First reading - initialize
            initialStepCount = totalSteps
            sessionStartSteps = totalSteps - dailySteps
            Log.d(TAG, "Initialized step counter - Total: $totalSteps, Daily: $dailySteps")
        } else {
            // Calculate steps since session start
            val stepsSinceStart = totalSteps - sessionStartSteps
            dailySteps = stepsSinceStart.coerceAtLeast(0)
            _stepCount.value = dailySteps

            Log.d(TAG, "Step counter update - Total: $totalSteps, Daily: $dailySteps")
        }
        saveData()
    }

    private fun handleStepDetectorData() {
        // Step detector gives one event per step
        dailySteps++
        _stepCount.value = dailySteps
        Log.d(TAG, "Step detected - Daily total: $dailySteps")
        saveData()
    }

    private fun checkSensorAvailability() {
        val sensors = sensorManager.getSensorList(Sensor.TYPE_ALL)
        Log.d(TAG, "Available sensors:")
        sensors.forEach { sensor ->
            if (sensor.type == Sensor.TYPE_STEP_COUNTER ||
                sensor.type == Sensor.TYPE_STEP_DETECTOR ||
                sensor.type == Sensor.TYPE_ACCELEROMETER) {
                Log.d(TAG, "- ${sensor.name} (${sensor.vendor})")
            }
        }
    }

    private fun loadSavedData() {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val today = getCurrentDateString()
        val lastUpdateDate = prefs.getString(KEY_LAST_UPDATE_DATE, "")

        if (lastUpdateDate == today) {
            // Same day - load existing data
            dailySteps = prefs.getInt(KEY_DAILY_STEPS, 0)
            sessionStartSteps = prefs.getInt(KEY_SESSION_START_STEPS, 0)
            _stepCount.value = dailySteps
            Log.d(TAG, "Loaded saved data - Daily steps: $dailySteps")
        } else {
            // New day - reset
            dailySteps = 0
            sessionStartSteps = 0
            _stepCount.value = 0
            Log.d(TAG, "New day detected - Reset step count")
        }
    }

    private fun saveData() {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putInt(KEY_DAILY_STEPS, dailySteps)
            .putInt(KEY_SESSION_START_STEPS, sessionStartSteps)
            .putString(KEY_LAST_UPDATE_DATE, getCurrentDateString())
            .apply()
    }

    private fun checkAndResetDailyData() {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val today = getCurrentDateString()
        val lastUpdateDate = prefs.getString(KEY_LAST_UPDATE_DATE, "")

        if (lastUpdateDate != today) {
            // New day - reset everything
            dailySteps = 0
            sessionStartSteps = 0
            initialStepCount = 0
            _stepCount.value = 0
            saveData()
            Log.d(TAG, "Daily data reset for new day")
        }
    }

    private fun getCurrentDateString(): String {
        return java.time.LocalDate.now().toString()
    }

    /**
     * Get sensor info for debugging
     */
    fun getSensorInfo(): String {
        val stepCounter = stepCounterSensor?.let { "${it.name} (${it.vendor})" } ?: "Not available"
        val stepDetector = stepDetectorSensor?.let { "${it.name} (${it.vendor})" } ?: "Not available"

        return """
            Step Counter: $stepCounter
            Step Detector: $stepDetector
            Currently Tracking: ${_isTracking.value}
            Daily Steps: $dailySteps
        """.trimIndent()
    }
}