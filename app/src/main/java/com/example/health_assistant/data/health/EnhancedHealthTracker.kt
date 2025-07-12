package com.example.health_assistant.data.health

import android.util.Log
import com.example.health_assistant.core.util.Result
import com.example.health_assistant.data.sensors.DeviceSensorManager
import com.example.health_assistant.features.health.model.HealthMetric
import com.example.health_assistant.features.health.model.HealthMetrics
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Enhanced Health Tracking Manager that works using device sensors only
 * No external dependencies required - works completely offline
 */
@Singleton
class EnhancedHealthTracker @Inject constructor(
    private val deviceSensorManager: DeviceSensorManager
) {

    companion object {
        private const val TAG = "EnhancedHealthTracker"
    }

    /**
     * Initialize health tracking - starts device sensors immediately
     */
    fun initialize(): Boolean {
        Log.d(TAG, "Initializing enhanced health tracking using device sensors only...")

        // Start device sensor tracking
        deviceSensorManager.startTracking()
        val sensorInitialized = deviceSensorManager.sensorAvailable.value

        if (sensorInitialized) {
            Log.d(TAG, "Device sensors initialized successfully")
        } else {
            Log.w(TAG, "Device sensors failed to initialize")
        }

        return sensorInitialized
    }

    /**
     * Get current health metrics using the best available source
     */
    fun getCurrentHealthMetrics(): Result<HealthMetrics> {
        return try {
            // Always use device sensors as primary source
            val sensorSteps = deviceSensorManager.stepCount.value
            val sensorCalories = estimateCaloriesFromSteps(sensorSteps)
            val sensorHeartPoints = estimateHeartPointsFromSteps(sensorSteps)

            Log.d(TAG, "Device sensor data - Steps: $sensorSteps, Calories: $sensorCalories, Heart Points: $sensorHeartPoints")

            // No Google Fit enhancement - use pure sensor data
            Log.d(TAG, "Using pure sensor data")
            val metrics = createSensorBasedMetrics(sensorSteps, sensorCalories, sensorHeartPoints)
            Result.Success(metrics)

        } catch (e: Exception) {
            Log.e(TAG, "Error getting health metrics", e)
            Result.Error(e, "Failed to get health metrics: ${e.message}")
        }
    }

    /**
     * Get real-time step count flow from device sensors
     */
    fun getStepCountFlow(): StateFlow<Int> {
        return deviceSensorManager.stepCount
    }

    /**
     * Check if any health tracking is available
     */
    fun isHealthTrackingAvailable(): Boolean {
        val hasSensors = deviceSensorManager.sensorAvailable.value

        Log.d(TAG, "Health tracking availability - Sensors: $hasSensors")
        return hasSensors
    }

    /**
     * Get tracking status and capabilities
     */
    fun getTrackingStatus(): TrackingStatus {
        val sensorAvailable = deviceSensorManager.sensorAvailable.value
        val sensorTracking = deviceSensorManager.isTracking.value

        return TrackingStatus(
            deviceSensorsAvailable = sensorAvailable,
            deviceSensorsTracking = sensorTracking,
            primarySource = when {
                sensorTracking -> TrackingSource.DEVICE_SENSORS
                else -> TrackingSource.MANUAL_ENTRY
            }
        )
    }

    /**
     * Stop all tracking
     */
    fun stopTracking() {
        deviceSensorManager.stopTracking()
        Log.d(TAG, "All health tracking stopped")
    }

    /**
     * Get debug information
     */
    fun getDebugInfo(): String {
        val status = getTrackingStatus()
        val sensorInfo = deviceSensorManager.getSensorInfo()

        return """
            === Enhanced Health Tracker Debug Info ===
            Primary Source: ${status.primarySource}
            Device Sensors Available: ${status.deviceSensorsAvailable}
            Device Sensors Tracking: ${status.deviceSensorsTracking}
            
            $sensorInfo
            
            Current Metrics:
            - Steps: ${deviceSensorManager.stepCount.value}
            - Estimated Calories: ${estimateCaloriesFromSteps(deviceSensorManager.stepCount.value)}
            - Estimated Heart Points: ${estimateHeartPointsFromSteps(deviceSensorManager.stepCount.value)}
        """.trimIndent()
    }

    private fun createSensorBasedMetrics(steps: Int, calories: Int, heartPoints: Int): HealthMetrics {
        return HealthMetrics(
            steps = HealthMetric(steps, 9000),
            calories = HealthMetric(calories, 300),
            heartPoints = HealthMetric(heartPoints, 50)
        )
    }

    /**
     * Estimate calories burned from step count
     * Basic formula: steps * 0.04 (average calories per step)
     */
    private fun estimateCaloriesFromSteps(steps: Int): Int {
        return (steps * 0.04).toInt()
    }

    /**
     * Estimate heart points from step count
     * Basic formula: 1 heart point per 100 steps of moderate activity
     */
    private fun estimateHeartPointsFromSteps(steps: Int): Int {
        return (steps / 100).coerceAtMost(50) // Cap at 50 heart points per day
    }

    data class TrackingStatus(
        val deviceSensorsAvailable: Boolean,
        val deviceSensorsTracking: Boolean,
        val primarySource: TrackingSource
    )

    enum class TrackingSource {
        DEVICE_SENSORS,
        MANUAL_ENTRY
    }
}