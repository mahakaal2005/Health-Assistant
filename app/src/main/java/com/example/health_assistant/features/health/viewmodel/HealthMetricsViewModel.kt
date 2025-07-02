package com.example.health_assistant.features.health.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.health_assistant.core.util.Result
import com.example.health_assistant.data.fitness.GoogleFitManager
import com.example.health_assistant.data.repository.interfaces.HealthRepository
import com.example.health_assistant.features.health.model.HealthMetrics
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * ViewModel for managing health metrics data for the Home screen
 * Now includes Google Fit API integration for real-time health data
 */
@HiltViewModel
class HealthMetricsViewModel @Inject constructor(
    private val healthRepository: HealthRepository,
    private val googleFitManager: GoogleFitManager
) : ViewModel() {

    private val _healthMetrics = MutableLiveData<HealthMetrics?>()
    val healthMetrics: LiveData<HealthMetrics?> = _healthMetrics

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _syncStatus = MutableLiveData<SyncStatus>()
    val syncStatus: LiveData<SyncStatus> = _syncStatus

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    init {
        loadTodayMetrics()
        // Auto-sync if Google Fit permissions are available
        if (hasGoogleFitPermissions()) {
            syncFromGoogleFit()
        }
    }

    private fun loadTodayMetrics() {
        viewModelScope.launch {
            _isLoading.value = true
            val currentDate = getCurrentDate()
            healthRepository.getDailyHealthMetrics(currentDate).collect { result ->
                when (result) {
                    is Result.Success -> {
                        _healthMetrics.value = result.data
                        _error.value = null
                    }
                    is Result.Error -> {
                        _error.value = result.message
                    }
                    is Result.Loading -> {
                        // Handle loading state if needed
                    }
                }
                _isLoading.value = false
            }
        }
    }

    /**
     * NEW: Sync health metrics from Google Fit API
     */
    fun syncFromGoogleFit() {
        viewModelScope.launch {
            _syncStatus.value = SyncStatus.SYNCING
            _isLoading.value = true

            val result = healthRepository.syncTodayMetricsFromGoogleFit()

            when (result) {
                is Result.Success -> {
                    _healthMetrics.value = result.data
                    _syncStatus.value = SyncStatus.SUCCESS
                    _error.value = null
                }
                is Result.Error -> {
                    _syncStatus.value = SyncStatus.ERROR
                    _error.value = result.message
                }
                else -> {
                    _syncStatus.value = SyncStatus.ERROR
                    _error.value = "Unknown error occurred during sync"
                }
            }
            _isLoading.value = false
        }
    }

    /**
     * NEW: Sync health metrics from device sensors (works without Google Fit app!)
     */
    fun syncFromDeviceSensors() {
        viewModelScope.launch {
            _syncStatus.value = SyncStatus.SYNCING
            _isLoading.value = true

            val result = healthRepository.syncTodayMetricsFromEnhancedTracker()

            when (result) {
                is Result.Success -> {
                    _healthMetrics.value = result.data
                    _syncStatus.value = SyncStatus.SUCCESS
                    _error.value = null
                }
                is Result.Error -> {
                    _syncStatus.value = SyncStatus.ERROR
                    _error.value = result.message
                }
                else -> {
                    _syncStatus.value = SyncStatus.ERROR
                    _error.value = "Unknown error occurred during sensor sync"
                }
            }
            _isLoading.value = false
        }
    }

    /**
     * NEW: Check if Google Fit permissions are granted
     */
    fun hasGoogleFitPermissions(): Boolean {
        return googleFitManager.hasPermissions()
    }

    /**
     * NEW: Handle Google Fit permission granted
     */
    fun onGoogleFitPermissionGranted() {
        syncFromGoogleFit()
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
        ERROR
    }
}