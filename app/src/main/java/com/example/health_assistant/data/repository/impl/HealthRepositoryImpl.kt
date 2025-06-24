package com.example.health_assistant.data.repository.impl

import com.example.health_assistant.data.repository.interfaces.HealthRepository
import com.example.health_assistant.features.health.model.HealthMetric
import com.example.health_assistant.features.health.model.HealthMetrics
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of HealthRepository that manages health metrics data
 */
@Singleton
class HealthRepositoryImpl @Inject constructor() : HealthRepository {

    // In-memory cache of health metrics
    private val _healthMetrics = MutableStateFlow(
        HealthMetrics(
            steps = HealthMetric(171, 9000),
            calories = HealthMetric(8, 300),
            workout = HealthMetric(0, 30)
        )
    )

    override fun getHealthMetrics(): Flow<HealthMetrics> = _healthMetrics

    override suspend fun updateSteps(steps: Int) {
        val currentMetrics = _healthMetrics.value
        _healthMetrics.value = currentMetrics.copy(
            steps = currentMetrics.steps.copy(current = steps)
        )
    }

    override suspend fun updateCalories(calories: Int) {
        val currentMetrics = _healthMetrics.value
        _healthMetrics.value = currentMetrics.copy(
            calories = currentMetrics.calories.copy(current = calories)
        )
    }

    override suspend fun updateWorkout(minutes: Int) {
        val currentMetrics = _healthMetrics.value
        _healthMetrics.value = currentMetrics.copy(
            workout = currentMetrics.workout.copy(current = minutes)
        )
    }

    override suspend fun updateStepsTarget(target: Int) {
        val currentMetrics = _healthMetrics.value
        _healthMetrics.value = currentMetrics.copy(
            steps = currentMetrics.steps.copy(target = target)
        )
    }

    override suspend fun updateCaloriesTarget(target: Int) {
        val currentMetrics = _healthMetrics.value
        _healthMetrics.value = currentMetrics.copy(
            calories = currentMetrics.calories.copy(target = target)
        )
    }

    override suspend fun updateWorkoutTarget(target: Int) {
        val currentMetrics = _healthMetrics.value
        _healthMetrics.value = currentMetrics.copy(
            workout = currentMetrics.workout.copy(target = target)
        )
    }
}