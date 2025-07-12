package com.example.health_assistant.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

/**
 * Data model for daily step tracking
 * Used for persistent storage and historical data
 */
@Entity(tableName = "daily_steps")
data class DailySteps(
    @PrimaryKey
    val date: String, // Format: yyyy-MM-dd
    val stepCount: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val isManualEntry: Boolean = false,
    val calories: Double = 0.0,
    val distance: Double = 0.0
) {
    companion object {
        fun fromDate(date: LocalDate, steps: Int): DailySteps {
            return DailySteps(
                date = date.toString(),
                stepCount = steps
            )
        }
    }
}

/**
 * Data model for step tracking session
 * Used for tracking continuous sessions
 */
@Entity(tableName = "step_sessions")
data class StepSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val startTime: Long,
    val endTime: Long? = null,
    val stepCount: Int,
    val isActive: Boolean = true
)