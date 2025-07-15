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
 */
@Singleton
class DeviceSensorManager @Inject constructor(
    @ApplicationContext private val context: Context
) : SensorEventListener {

    companion object {
        private const val TAG = "DeviceSensorManager"
        // Use same SharedPreferences as StepTrackingService for consistency
        private const val PREFS_NAME = "step_service_prefs"
        private const val KEY_DAILY_STEPS = "service_daily_steps"
        private const val KEY_LAST_DATE = "service_last_date"
        private const val KEY_INITIAL_STEP_COUNT = "service_initial_step_count"
        private const val KEY_SERVICE_ENABLED = "service_enabled"
    }

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    private val stepDetectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
    private val sharedPrefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    // State flows for UI observation
    private val _stepCount = MutableStateFlow(0)
    val stepCount: StateFlow<Int> = _stepCount.asStateFlow()

    private val _isTracking = MutableStateFlow(false)
    val isTracking: StateFlow<Boolean> = _isTracking.asStateFlow()

    private val _sensorAvailable = MutableStateFlow(false)
    val sensorAvailable: StateFlow<Boolean> = _sensorAvailable.asStateFlow()

    // Internal tracking variables
    private var currentDate: String = getCurrentDateString()
    private var isListenerRegistered = false

    init {
        checkSensorAvailability()
        loadStepDataFromService()
        checkForDateChange()
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

        // Start background service for continuous tracking
        startBackgroundService()

        // Load current step data from service
        loadStepDataFromService()

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
     */
    private fun handleStepCounterData(totalSteps: Long) {
        coroutineScope.launch {
            checkForDateChange()

            val dailySteps = (totalSteps).toInt()
            updateDailySteps(dailySteps)
        }
    }

    /**
     * Handle step detector sensor data (individual step events)
     */
    private fun handleStepDetectorData() {
        coroutineScope.launch {
            checkForDateChange()
            val currentSteps = getCurrentDailySteps()
            updateDailySteps(currentSteps + 1)
        }
    }

    /**
     * Update daily step count and notify observers
     */
    private fun updateDailySteps(steps: Int) {
        val clampedSteps = maxOf(0, steps) // Ensure non-negative
        _stepCount.value = clampedSteps
        saveDailySteps(clampedSteps)

        Log.d(TAG, "Daily steps updated: $clampedSteps")
    }

    /**
     * Check if date has changed (midnight reset)
     */
    private fun checkForDateChange() {
        val today = getCurrentDateString()
        val serviceDate = sharedPrefs.getString(KEY_LAST_DATE, today) ?: today

        if (currentDate != serviceDate) {
            Log.d(TAG, "Date changed detected - syncing with service data")
            currentDate = serviceDate
            loadStepDataFromService()
        }
    }

    /**
     * Load step data from StepTrackingService SharedPreferences
     */
    private fun loadStepDataFromService() {
        val savedSteps = sharedPrefs.getInt(KEY_DAILY_STEPS, 0)
        val savedDate = sharedPrefs.getString(KEY_LAST_DATE, getCurrentDateString()) ?: getCurrentDateString()
        val serviceEnabled = sharedPrefs.getBoolean(KEY_SERVICE_ENABLED, false)

        _stepCount.value = savedSteps
        currentDate = savedDate
        _isTracking.value = serviceEnabled

        Log.d(TAG, "Loaded step data from service - Steps: $savedSteps, Date: $savedDate, Service: $serviceEnabled")

        // Auto-start if service was previously enabled
        if (serviceEnabled && _sensorAvailable.value) {
            startBackgroundService()
        }
    }

    /**
     * Refresh step count from service data
     */
    fun refreshStepCount() {
        val currentSteps = sharedPrefs.getInt(KEY_DAILY_STEPS, 0)
        _stepCount.value = currentSteps
        Log.d(TAG, "Refreshed step count from service: $currentSteps")
    }

    /**
     * Reset daily steps for new day
     */
    private fun resetDailySteps() {
        _stepCount.value = 0
        saveDailySteps(0)
        saveCurrentDate(getCurrentDateString())

        Log.d(TAG, "Daily steps reset for new day")
    }

    /**
     * Load saved data from SharedPreferences
     */
    private fun loadSavedData() {
        val savedSteps = sharedPrefs.getInt(KEY_DAILY_STEPS, 0)
        val savedDate = sharedPrefs.getString(KEY_LAST_DATE, getCurrentDateString()) ?: getCurrentDateString()
        val serviceEnabled = sharedPrefs.getBoolean(KEY_SERVICE_ENABLED, false)

        _stepCount.value = savedSteps
        currentDate = savedDate
        _isTracking.value = serviceEnabled

        Log.d(TAG, "Loaded saved data - Steps: $savedSteps, Date: $savedDate, Service: $serviceEnabled")

        // Auto-start if service was previously enabled
        if (serviceEnabled && _sensorAvailable.value) {
            startBackgroundService()
        }
    }

    /**
     * Save daily steps to SharedPreferences
     */
    private fun saveDailySteps(steps: Int) {
        sharedPrefs.edit { putInt(KEY_DAILY_STEPS, steps) }
    }

    /**
     * Save current date to SharedPreferences
     */
    private fun saveCurrentDate(date: String) {
        sharedPrefs.edit { putString(KEY_LAST_DATE, date) }
    }

    /**
     * Save service state to SharedPreferences
     */
    private fun saveServiceState(enabled: Boolean) {
        sharedPrefs.edit { putBoolean(KEY_SERVICE_ENABLED, enabled) }
    }

    /**
     * Get current daily steps from SharedPreferences
     */
    private fun getCurrentDailySteps(): Int {
        return sharedPrefs.getInt(KEY_DAILY_STEPS, 0)
    }

    /**
     * Get current date as string
     */
    private fun getCurrentDateString(): String {
        return LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
    }

    /**
     * Handle sensor accuracy changes
     */
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        Log.d(TAG, "Sensor accuracy changed: ${sensor?.name}, accuracy: $accuracy")
    }

    /**
     * Manually reset step count (for testing or user request)
     */
    fun resetStepCount() {
        Log.d(TAG, "Manual step count reset requested")
        resetDailySteps()
    }

    /**
     * Get sensor information for debugging
     */
    fun getSensorInfo(): Map<String, Any> {
        return mapOf(
            "stepCounterAvailable" to (stepCounterSensor != null),
            "stepDetectorAvailable" to (stepDetectorSensor != null),
            "isTracking" to _isTracking.value,
            "currentSteps" to _stepCount.value,
            "currentDate" to currentDate
        )
    }
}