package com.example.health_assistant.features.health.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.health_assistant.core.util.Result
import com.example.health_assistant.data.repository.interfaces.HealthRepository
import com.example.health_assistant.features.health.model.HealthMetrics
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for managing health metrics data for the Home screen
 */
@HiltViewModel
class HealthMetricsViewModel @Inject constructor(
    private val healthRepository: HealthRepository
) : ViewModel() {

    private val _healthMetrics = MutableLiveData<HealthMetrics?>()
    val healthMetrics: LiveData<HealthMetrics?> = _healthMetrics

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    init {
        loadTodayMetrics()
    }

    private fun loadTodayMetrics() {
        viewModelScope.launch {
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
            }
        }
    }

    /**
     * Update steps count
     */
    fun updateSteps(steps: Int) {
        viewModelScope.launch {
            healthRepository.updateStepCount(steps).let { result ->
                if (result is Result.Error) {
                    _error.value = result.message
                }
            }
        }
    }

    /**
     * Update water intake
     */
    fun updateWaterIntake(liters: Float) {
        viewModelScope.launch {
            healthRepository.updateWaterIntake(liters).let { result ->
                if (result is Result.Error) {
                    _error.value = result.message
                }
            }
        }
    }

    /**
     * Update sleep duration
     */
    fun updateSleepDuration(hours: Float) {
        viewModelScope.launch {
            healthRepository.updateSleepDuration(hours).let { result ->
                if (result is Result.Error) {
                    _error.value = result.message
                }
            }
        }
    }

    /**
     * Save current health metrics
     */
    fun saveHealthMetrics(metrics: HealthMetrics) {
        viewModelScope.launch {
            healthRepository.saveDailyHealthMetrics(metrics).let { result ->
                if (result is Result.Error) {
                    _error.value = result.message
                }
            }
        }
    }

    private fun getCurrentDate(): String {
        return java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .format(java.util.Date())
    }
}