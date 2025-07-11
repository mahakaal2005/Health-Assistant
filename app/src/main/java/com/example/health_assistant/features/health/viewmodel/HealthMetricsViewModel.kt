package com.example.health_assistant.features.health.viewmodel

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.health_assistant.core.util.Result
import com.example.health_assistant.data.health.EnhancedHealthTracker
import com.example.health_assistant.data.repository.interfaces.HealthRepository
import com.example.health_assistant.features.health.model.HealthMetrics
import com.example.health_assistant.utils.HealthNotificationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * ViewModel for managing health metrics data for the Home screen
 * Uses device sensors only via EnhancedHealthTracker - no Google Fit required
 */
@HiltViewModel
class HealthMetricsViewModel @Inject constructor(
    private val healthRepository: HealthRepository,
    private val enhancedHealthTracker: EnhancedHealthTracker,
    private val notificationManager: HealthNotificationManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _healthMetrics = MutableLiveData<HealthMetrics?>()
    val healthMetrics: LiveData<HealthMetrics?> = _healthMetrics

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _syncStatus = MutableLiveData<SyncStatus>()
    val syncStatus: LiveData<SyncStatus> = _syncStatus

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // Add notification tracking variables
    private var lastNotifiedSteps = 0
    private var lastNotificationTime = 0L
    private val defaultStepGoal = 10000 // Default step goal

    init {
        // Initialize device sensor tracking
        initializeDeviceSensors()
        loadTodayMetrics()
    }

    private fun initializeDeviceSensors() {
        viewModelScope.launch {
            try {
                val initialized = enhancedHealthTracker.initialize()
                if (initialized) {
                    _syncStatus.value = SyncStatus.SENSOR_TRACKING
                } else {
                    _syncStatus.value = SyncStatus.MANUAL_ONLY
                }
            } catch (e: Exception) {
                _error.value = "Failed to initialize device sensors: ${e.message}"
                _syncStatus.value = SyncStatus.MANUAL_ONLY
            }
        }
    }

    private fun loadTodayMetrics() {
        viewModelScope.launch {
            _isLoading.value = true
            val currentDate = getCurrentDate()
            healthRepository.getDailyHealthMetrics(currentDate).collect { result ->
                when (result) {
                    is Result.Loading -> {
                        _isLoading.value = true
                    }
                    is Result.Success -> {
                        _isLoading.value = false
                        val metrics = result.data
                        _healthMetrics.value = metrics
                        _error.value = null

                        // Check for step milestone notifications
                        metrics?.let { checkAndSendStepNotifications(it) }
                    }
                    is Result.Error -> {
                        _isLoading.value = false
                        _error.value = result.message
                    }
                }
            }
        }
    }

    /**
     * Refresh health metrics from device sensors
     */
    fun refreshMetrics() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Get current metrics from device sensors
                val result = enhancedHealthTracker.getCurrentHealthMetrics()
                when (result) {
                    is Result.Loading -> {
                        _isLoading.value = true
                        _syncStatus.value = SyncStatus.SYNCING
                    }
                    is Result.Success -> {
                        val metrics = result.data
                        _healthMetrics.value = metrics
                        _error.value = null
                        _syncStatus.value = SyncStatus.SENSOR_TRACKING

                        // Check for step milestone notifications
                        metrics?.let { checkAndSendStepNotifications(it) }
                    }
                    is Result.Error -> {
                        _error.value = result.message
                        _syncStatus.value = SyncStatus.ERROR
                    }
                }
            } catch (e: Exception) {
                _error.value = "Failed to refresh metrics: ${e.message}"
                _syncStatus.value = SyncStatus.ERROR
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Check if step milestone notifications should be sent
     */
    private fun checkAndSendStepNotifications(metrics: HealthMetrics) {
        val currentSteps = metrics.steps.current
        val stepGoal = metrics.steps.target

        // Avoid sending notifications too frequently (minimum 30 minutes apart)
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastNotificationTime < 30 * 60 * 1000) {
            return
        }

        // Check if a milestone notification should be sent
        val milestone = notificationManager.shouldShowMilestoneNotification(
            currentSteps = currentSteps,
            goalSteps = stepGoal,
            lastNotifiedSteps = lastNotifiedSteps
        )

        milestone?.let {
            notificationManager.showStepMilestoneNotification(
                currentSteps = currentSteps,
                goalSteps = stepGoal,
                milestonePercentage = it
            )

            // Update tracking variables
            lastNotifiedSteps = currentSteps
            lastNotificationTime = currentTime
        }
    }

    /**
     * Manually trigger daily summary notification
     */
    fun sendDailySummaryNotification() {
        val metrics = _healthMetrics.value
        metrics?.let {
            val stepGoal = it.steps.target
            notificationManager.showDailySummaryNotification(
                totalSteps = it.steps.current,
                goalSteps = stepGoal,
                streakDays = calculateStreakDays() // You can implement streak calculation
            )
        }
    }

    /**
     * Send motivational reminder notification
     */
    fun sendMotivationalReminder() {
        val metrics = _healthMetrics.value
        metrics?.let {
            val stepGoal = it.steps.target
            notificationManager.showMotivationalReminder(
                currentSteps = it.steps.current,
                goalSteps = stepGoal
            )
        }
    }

    /**
     * Calculate streak days (placeholder - implement based on your streak logic)
     */
    private fun calculateStreakDays(): Int {
        // Implement streak calculation logic here
        return 0
    }

    /**
     * NEW: Clear error state
     */
    fun clearError() {
        _error.value = null
    }

    /**
     * NEW: Clear sync status
     */
    fun clearSyncStatus() {
        _syncStatus.value = SyncStatus.IDLE
    }

    /**
     * NEW: Get current date in required format
     */
    private fun getCurrentDate(): String {
        return LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
    }

    /**
     * NEW: Sync status enumeration
     */
    enum class SyncStatus {
        IDLE,
        SYNCING,
        SUCCESS,
        ERROR,
        SENSOR_TRACKING,
        MANUAL_ONLY
    }
}