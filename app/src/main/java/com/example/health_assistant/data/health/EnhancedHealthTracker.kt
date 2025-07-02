package com.example.health_assistant.data.health

import android.util.Log
import com.example.health_assistant.core.util.Result
import com.example.health_assistant.data.fitness.GoogleFitManager
import com.example.health_assistant.data.sensors.DeviceSensorManager
import com.example.health_assistant.features.health.model.HealthMetric
import com.example.health_assistant.features.health.model.HealthMetrics
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Enhanced Health Tracking Manager that works WITHOUT requiring Google Fit app
 * Uses device sensors as primary source, Google Fit as optional enhancement
 */
@Singleton
class EnhancedHealthTracker @Inject constructor(
    private val deviceSensorManager: DeviceSensorManager,
    private val googleFitManager: GoogleFitManager
) {

    companion object {
        private const val TAG = "EnhancedHealthTracker"
    }

    /**
     * Initialize health tracking - starts device sensors immediately
     */
    fun initialize(): Boolean {
        Log.d(TAG, "Initializing enhanced health tracking...")

        // Always try device sensors first (works without Google Fit app)
        val sensorStarted = deviceSensorManager.startTracking()

        if (sensorStarted) {
            Log.d(TAG, "Device sensors started successfully - app works independently!")
            return true
        } else {
            Log.w(TAG, "Device sensors not available - will rely on manual entry")
            return false
        }
    }

    /**
     * Get current health metrics using the best available source
     */
    suspend fun getCurrentHealthMetrics(): Result<HealthMetrics> {
        return try {
            // Always use device sensors as primary source
            val sensorSteps = deviceSensorManager.getDailySteps()
            val sensorCalories = deviceSensorManager.getEstimatedCalories()
            val sensorHeartPoints = deviceSensorManager.getEstimatedHeartPoints()

            Log.d(TAG, "Device sensor data - Steps: $sensorSteps, Calories: $sensorCalories, Heart Points: $sensorHeartPoints")

            // Try to enhance with Google Fit data if available (optional)
            val enhancedMetrics = if (googleFitManager.hasPermissions()) {
                try {
                    Log.d(TAG, "Google Fit available - enhancing sensor data...")
                    val fitSteps = googleFitManager.getTodaySteps()
                    val fitCalories = googleFitManager.getTodayCalories()
                    val fitHeartPoints = googleFitManager.getTodayHeartPoints()

                    // Use the higher value between sensors and Google Fit
                    HealthMetrics(
                        steps = HealthMetric(maxOf(sensorSteps, fitSteps), 9000),
                        calories = HealthMetric(maxOf(sensorCalories, fitCalories), 300),
                        heartPoints = HealthMetric(maxOf(sensorHeartPoints, fitHeartPoints), 50)
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Google Fit enhancement failed, using sensor data: ${e.message}")
                    createSensorBasedMetrics(sensorSteps, sensorCalories, sensorHeartPoints)
                }
            } else {
                // No Google Fit - use pure sensor data
                Log.d(TAG, "No Google Fit permissions - using pure sensor data")
                createSensorBasedMetrics(sensorSteps, sensorCalories, sensorHeartPoints)
            }

            Result.Success(enhancedMetrics)

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
        val hasSensors = deviceSensorManager.hasSensorsAvailable()
        val hasGoogleFit = googleFitManager.hasPermissions()

        Log.d(TAG, "Health tracking availability - Sensors: $hasSensors, Google Fit: $hasGoogleFit")
        return hasSensors || hasGoogleFit
    }

    /**
     * Get tracking status and capabilities
     */
    fun getTrackingStatus(): TrackingStatus {
        val sensorAvailable = deviceSensorManager.hasSensorsAvailable()
        val sensorTracking = deviceSensorManager.isTracking.value
        val googleFitAvailable = googleFitManager.hasPermissions()

        return TrackingStatus(
            deviceSensorsAvailable = sensorAvailable,
            deviceSensorsTracking = sensorTracking,
            googleFitAvailable = googleFitAvailable,
            primarySource = when {
                sensorTracking -> TrackingSource.DEVICE_SENSORS
                googleFitAvailable -> TrackingSource.GOOGLE_FIT
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
            Google Fit Available: ${status.googleFitAvailable}
            
            $sensorInfo
            
            Current Metrics:
            - Steps: ${deviceSensorManager.getDailySteps()}
            - Estimated Calories: ${deviceSensorManager.getEstimatedCalories()}
            - Estimated Heart Points: ${deviceSensorManager.getEstimatedHeartPoints()}
        """.trimIndent()
    }

    private fun createSensorBasedMetrics(steps: Int, calories: Int, heartPoints: Int): HealthMetrics {
        return HealthMetrics(
            steps = HealthMetric(steps, 9000),
            calories = HealthMetric(calories, 300),
            heartPoints = HealthMetric(heartPoints, 50)
        )
    }

    data class TrackingStatus(
        val deviceSensorsAvailable: Boolean,
        val deviceSensorsTracking: Boolean,
        val googleFitAvailable: Boolean,
        val primarySource: TrackingSource
    )

    enum class TrackingSource {
        DEVICE_SENSORS,
        GOOGLE_FIT,
        MANUAL_ENTRY
    }
}