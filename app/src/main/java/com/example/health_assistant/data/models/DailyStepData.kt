package com.example.health_assistant.data.models

import java.time.LocalDate

/**
 * Data model representing daily step count data
 */
data class DailyStepData(
    val date: LocalDate,
    val steps: Int,
    val goal: Int = 10000, // Default daily goal
    val calories: Int = 0, // Daily calories burned
    val caloriesGoal: Int = 300, // Default daily calories goal
    val heartPoints: Int = 0, // Daily heart points earned
    val heartPointsGoal: Int = 50, // Default daily heart points goal
    val dayOfWeek: String = date.dayOfWeek.name
) {
    /**
     * Calculate the percentage of goal achieved
     */
    val goalPercentage: Float
        get() = if (goal > 0) (steps.toFloat() / goal.toFloat()) * 100f else 0f

    /**
     * Check if daily goal was achieved
     */
    val isGoalAchieved: Boolean
        get() = steps >= goal

    /**
     * Get abbreviated day name (Mon, Tue, etc.)
     */
    val shortDayName: String
        get() = when (date.dayOfWeek.value) {
            1 -> "Mon"
            2 -> "Tue"
            3 -> "Wed"
            4 -> "Thu"
            5 -> "Fri"
            6 -> "Sat"
            7 -> "Sun"
            else -> "???"
        }
}

/**
 * Data model for weekly step summary
 */
data class WeeklyStepSummary(
    val dailyData: List<DailyStepData>,
    val weeklyGoal: Int = 70000 // Default weekly goal (10k * 7 days)
) {
    /**
     * Total steps for the week
     */
    val totalSteps: Int
        get() = dailyData.sumOf { it.steps }

    /**
     * Daily average steps
     */
    val dailyAverage: Int
        get() = if (dailyData.isNotEmpty()) totalSteps / dailyData.size else 0

    /**
     * Weekly goal progress percentage
     */
    val weeklyGoalPercentage: Float
        get() = if (weeklyGoal > 0) (totalSteps.toFloat() / weeklyGoal.toFloat()) * 100f else 0f

    /**
     * Number of days where goal was achieved
     */
    val goalAchievedDays: Int
        get() = dailyData.count { it.isGoalAchieved }

    /**
     * Best day of the week (highest step count)
     */
    val bestDay: DailyStepData?
        get() = dailyData.maxByOrNull { it.steps }
}