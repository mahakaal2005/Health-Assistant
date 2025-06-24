package com.example.health_assistant.features.health.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
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

    // LiveData for health metrics
    val healthMetrics: LiveData<HealthMetrics> = healthRepository.getHealthMetrics().asLiveData()

    /**
     * Update steps count
     */
    fun updateSteps(steps: Int) {
        viewModelScope.launch {
            healthRepository.updateSteps(steps)
        }
    }

    /**
     * Update calories burned
     */
    fun updateCalories(calories: Int) {
        viewModelScope.launch {
            healthRepository.updateCalories(calories)
        }
    }

    /**
     * Update workout duration
     */
    fun updateWorkout(minutes: Int) {
        viewModelScope.launch {
            healthRepository.updateWorkout(minutes)
        }
    }

    /**
     * Update target steps
     */
    fun updateStepsTarget(target: Int) {
        viewModelScope.launch {
            healthRepository.updateStepsTarget(target)
        }
    }

    /**
     * Update target calories
     */
    fun updateCaloriesTarget(target: Int) {
        viewModelScope.launch {
            healthRepository.updateCaloriesTarget(target)
        }
    }

    /**
     * Update target workout duration
     */
    fun updateWorkoutTarget(target: Int) {
        viewModelScope.launch {
            healthRepository.updateWorkoutTarget(target)
        }
    }
}