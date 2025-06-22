package com.example.health_assistant.features.health.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.health_assistant.features.health.model.HealthMetric
import com.example.health_assistant.features.health.model.HealthMetrics

/**
 * ViewModel for managing health metrics data for the Home screen
 */
class HealthMetricsViewModel : ViewModel() {

    // LiveData for health metrics
    private val _healthMetrics = MutableLiveData<HealthMetrics>()
    val healthMetrics: LiveData<HealthMetrics> = _healthMetrics

    init {
        // Initialize with default values
        _healthMetrics.value = HealthMetrics(
            steps = HealthMetric(0, 9000),
            calories = HealthMetric(0, 300),
            workout = HealthMetric(0, 30)
        )

        // In a real app, we would load data from a repository here
        loadHealthMetrics()
    }

    /**
     * Load health metrics data from repository
     * In a real app, this would fetch data from a database or API
     */
    private fun loadHealthMetrics() {
        // Simulate fetching data
        // In a real app, this would be an asynchronous call to a repository
        _healthMetrics.value = HealthMetrics(
            steps = HealthMetric(171, 9000),
            calories = HealthMetric(8, 300),
            workout = HealthMetric(0, 30)
        )
    }

    /**
     * Update steps count
     */
    fun updateSteps(steps: Int) {
        val currentMetrics = _healthMetrics.value ?: return
        _healthMetrics.value = currentMetrics.copy(
            steps = currentMetrics.steps.copy(current = steps)
        )
    }

    /**
     * Update calories burned
     */
    fun updateCalories(calories: Int) {
        val currentMetrics = _healthMetrics.value ?: return
        _healthMetrics.value = currentMetrics.copy(
            calories = currentMetrics.calories.copy(current = calories)
        )
    }

    /**
     * Update workout duration
     */
    fun updateWorkout(minutes: Int) {
        val currentMetrics = _healthMetrics.value ?: return
        _healthMetrics.value = currentMetrics.copy(
            workout = currentMetrics.workout.copy(current = minutes)
        )
    }

    /**
     * Update target steps
     */
    fun updateStepsTarget(target: Int) {
        val currentMetrics = _healthMetrics.value ?: return
        _healthMetrics.value = currentMetrics.copy(
            steps = currentMetrics.steps.copy(target = target)
        )
    }

    /**
     * Update target calories
     */
    fun updateCaloriesTarget(target: Int) {
        val currentMetrics = _healthMetrics.value ?: return
        _healthMetrics.value = currentMetrics.copy(
            calories = currentMetrics.calories.copy(target = target)
        )
    }

    /**
     * Update target workout duration
     */
    fun updateWorkoutTarget(target: Int) {
        val currentMetrics = _healthMetrics.value ?: return
        _healthMetrics.value = currentMetrics.copy(
            workout = currentMetrics.workout.copy(target = target)
        )
    }
}