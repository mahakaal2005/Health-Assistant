package com.example.health_assistant.data.repository.interfaces

import com.example.health_assistant.features.health.model.HealthMetrics
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for health-related data operations
 */
interface HealthRepository {
    /**
     * Get user's health metrics as a Flow for real-time updates
     * @return Flow of HealthMetrics
     */
    fun getHealthMetrics(): Flow<HealthMetrics>

    /**
     * Update steps count
     * @param steps Number of steps
     */
    suspend fun updateSteps(steps: Int)

    /**
     * Update calories burned
     * @param calories Number of calories
     */
    suspend fun updateCalories(calories: Int)

    /**
     * Update workout duration
     * @param minutes Duration in minutes
     */
    suspend fun updateWorkout(minutes: Int)

    /**
     * Update target steps
     * @param target Target number of steps
     */
    suspend fun updateStepsTarget(target: Int)

    /**
     * Update target calories
     * @param target Target number of calories
     */
    suspend fun updateCaloriesTarget(target: Int)

    /**
     * Update target workout duration
     * @param target Target duration in minutes
     */
    suspend fun updateWorkoutTarget(target: Int)
}