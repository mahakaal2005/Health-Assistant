package com.example.health_assistant.data.models

import java.time.LocalDate

/**
 * Data model for weekly health summary
 * Used for storing and managing weekly aggregated health data
 */
data class WeeklyHealthSummary(
    val userId: String,
    val weekStartDate: LocalDate,
    val weekEndDate: LocalDate,
    val totalSteps: Int,
    val averageSteps: Int,
    val totalCalories: Int,
    val averageCalories: Int,
    val totalHeartPoints: Int,
    val averageHeartPoints: Int,
    val stepsGoalAchieved: Boolean,
    val caloriesGoalAchieved: Boolean,
    val heartPointsGoalAchieved: Boolean,
    val weeklyStepsGoal: Int = 63000, // 9000 * 7 days
    val weeklyCaloriesGoal: Int = 2100, // 300 * 7 days
    val weeklyHeartPointsGoal: Int = 350, // 50 * 7 days
    val createdAt: LocalDate = LocalDate.now(),
    val updatedAt: LocalDate = LocalDate.now()
) {
    /**
     * Calculate steps goal achievement percentage
     */
    val stepsGoalPercentage: Int
        get() = if (weeklyStepsGoal > 0) ((totalSteps.toFloat() / weeklyStepsGoal) * 100).toInt() else 0

    /**
     * Calculate calories goal achievement percentage
     */
    val caloriesGoalPercentage: Int
        get() = if (weeklyCaloriesGoal > 0) ((totalCalories.toFloat() / weeklyCaloriesGoal) * 100).toInt() else 0

    /**
     * Calculate heart points goal achievement percentage
     */
    val heartPointsGoalPercentage: Int
        get() = if (weeklyHeartPointsGoal > 0) ((totalHeartPoints.toFloat() / weeklyHeartPointsGoal) * 100).toInt() else 0

    /**
     * Check if user achieved all weekly goals
     */
    val allGoalsAchieved: Boolean
        get() = stepsGoalAchieved && caloriesGoalAchieved && heartPointsGoalAchieved

    /**
     * Get overall weekly score (0-100)
     */
    val weeklyScore: Int
        get() = ((stepsGoalPercentage + caloriesGoalPercentage + heartPointsGoalPercentage) / 3).coerceAtMost(100)
}