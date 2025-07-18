package com.example.health_assistant.features.health.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.health_assistant.auth.session.SessionManager
import com.example.health_assistant.core.util.Result
import com.example.health_assistant.data.health.EnhancedHealthTracker
import com.example.health_assistant.data.repository.interfaces.HealthRepository
import com.example.health_assistant.features.health.model.HealthMetrics
import com.example.health_assistant.utils.HealthNotificationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * ViewModel for managing health metrics data for the Home screen
 * Uses device sensors only via EnhancedHealthTracker - no Google Fit required
 * Now with proper user isolation for multi-user support
 */
@HiltViewModel
class HealthMetricsViewModel @Inject constructor(
    private val healthRepository: HealthRepository,
    private val enhancedHealthTracker: EnhancedHealthTracker,
    private val notificationManager: HealthNotificationManager,
    private val sessionManager: SessionManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val TAG = "HealthMetricsViewModel"

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

    init {
        // Initialize device sensor tracking
        initializeDeviceSensors()
        
        // Load metrics for the current user
        loadTodayMetrics()
        
        // Monitor user changes
        monitorUserChanges()
    }

    /**
     * Monitor user changes to reload data when user changes
     */
    private fun monitorUserChanges() {
        viewModelScope.launch {
            try {
                // This is a simplified approach - in a real app, you would observe a Flow from SessionManager
                val currentUserId = sessionManager.getCurrentUserId()
                Log.d(TAG, "Current user ID: $currentUserId")
                
                // IMPORTANT: Do NOT reset step count when user logs in
                // This ensures we preserve their existing data
                
                // When user changes, reload metrics for the current user
                loadTodayMetrics()
                
                // Also refresh metrics immediately to get latest data
                refreshMetrics()
                
                Log.d(TAG, "Health metrics loaded for user $currentUserId")
            } catch (e: Exception) {
                Log.e(TAG, "Error monitoring user changes", e)
            }
        }
    }

    /**
     * Reset step count for a user
     */
    private fun resetUserStepCount(userId: String) {
        viewModelScope.launch {
            try {
                val result = healthRepository.resetUserStepCount(userId)
                when (result) {
                    is Result.Success -> {
                        Log.d(TAG, "Successfully reset step count for user $userId")
                    }
                    is Result.Error -> {
                        Log.e(TAG, "Error resetting step count: ${result.message}")
                    }
                    is Result.Loading -> {
                        // Do nothing
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception resetting step count", e)
            }
        }
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
            
            try {
                healthRepository.getDailyHealthMetrics(currentDate).collectLatest { result ->
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
                            Log.e(TAG, "Error loading metrics: ${result.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                _isLoading.value = false
                _error.value = "Failed to load metrics: ${e.message}"
                Log.e(TAG, "Exception loading metrics", e)
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
                        
                        // Save metrics to repository for current user
                        healthRepository.saveDailyHealthMetrics(metrics)
                        
                        _healthMetrics.value = metrics
                        _error.value = null
                        _syncStatus.value = SyncStatus.SENSOR_TRACKING

                        // Check for step milestone notifications
                        metrics?.let { checkAndSendStepNotifications(it) }
                        
                        Log.d(TAG, "Refreshed metrics for user ${sessionManager.getCurrentUserId()}: Steps=${metrics.steps.current}")
                    }
                    is Result.Error -> {
                        _error.value = result.message
                        _syncStatus.value = SyncStatus.ERROR
                        Log.e(TAG, "Error refreshing metrics: ${result.message}")
                    }
                }
            } catch (e: Exception) {
                _error.value = "Failed to refresh metrics: ${e.message}"
                _syncStatus.value = SyncStatus.ERROR
                Log.e(TAG, "Exception refreshing metrics", e)
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

        // Check for step milestones
        if (currentSteps >= stepGoal && lastNotifiedSteps < stepGoal) {
            // Goal reached notification
            notificationManager.showStepMilestoneNotification(
                currentSteps = currentSteps,
                goalSteps = stepGoal,
                milestonePercentage = 100f
            )
            lastNotifiedSteps = currentSteps
            lastNotificationTime = currentTime
        } else if (currentSteps >= 5000 && lastNotifiedSteps < 5000) {
            // 5000 steps milestone
            notificationManager.showStepMilestoneNotification(
                currentSteps = currentSteps,
                goalSteps = stepGoal,
                milestonePercentage = 50f
            )
            lastNotifiedSteps = currentSteps
            lastNotificationTime = currentTime
        } else if (currentSteps >= 1000 && lastNotifiedSteps < 1000) {
            // 1000 steps milestone
            notificationManager.showStepMilestoneNotification(
                currentSteps = currentSteps,
                goalSteps = stepGoal,
                milestonePercentage = 10f
            )
            lastNotifiedSteps = currentSteps
            lastNotificationTime = currentTime
        }
    }

    /**
     * Get current date as string
     */
    private fun getCurrentDate(): String {
        return LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
    }

    /**
     * Clear any error
     */
    fun clearError() {
        _error.value = null
    }
}

/**
 * Sync status for health metrics
 */
enum class SyncStatus {
    SYNCING,
    SENSOR_TRACKING,
    MANUAL_ONLY,
    ERROR
}