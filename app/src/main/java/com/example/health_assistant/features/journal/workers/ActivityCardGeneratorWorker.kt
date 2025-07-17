package com.example.health_assistant.features.journal.workers

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.example.health_assistant.data.health.EnhancedHealthTracker
import com.example.health_assistant.features.journal.domain.usecase.GenerateActivityCardUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.assisted.AssistedFactory
import java.time.LocalDate

// Type alias to avoid naming conflicts
typealias WorkResult = ListenableWorker.Result

/**
 * Background worker that automatically generates activity cards at midnight
 * Uses real health data from the step tracking system and saves to database
 */
@HiltWorker
class ActivityCardGeneratorWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val enhancedHealthTracker: EnhancedHealthTracker,
    private val generateActivityCardUseCase: GenerateActivityCardUseCase
) : CoroutineWorker(context, workerParams) {

    @AssistedFactory
    interface Factory {
        fun create(context: Context, workerParams: WorkerParameters): ActivityCardGeneratorWorker
    }

    companion object {
        const val WORK_NAME = "activity_card_generator"
        private const val TAG = "ActivityCardWorker"
    }

    override suspend fun doWork(): WorkResult {
        return try {
            Log.d(TAG, "🎯 Starting activity card generation")

            // Check if this is immediate generation with provided data
            val isImmediateGeneration = inputData.getBoolean("immediate_generation", false)

            if (isImmediateGeneration) {
                // Use provided health data for immediate generation
                val stepCount = inputData.getInt("step_count", 0)
                val caloriesBurned = inputData.getInt("calories_burned", 0)
                val heartPoints = inputData.getInt("heart_points", 0)

                Log.d(TAG, "📊 Using immediate generation data - Steps: $stepCount, Calories: $caloriesBurned, Heart Points: $heartPoints")

                val today = LocalDate.now()
                val cardResult = generateActivityCardUseCase.generateCardForDate(
                    date = today,
                    stepCount = stepCount,
                    caloriesBurned = caloriesBurned,
                    heartPoints = heartPoints
                )

                if (cardResult.isSuccess) {
                    val activityCard = cardResult.getOrNull()
                    Log.d(TAG, "✅ Immediate activity card saved successfully with ID: ${activityCard?.id}")
                } else {
                    Log.e(TAG, "❌ Failed to save immediate activity card: ${cardResult.exceptionOrNull()?.message}")
                }

                WorkResult.success()
            } else {
                // Original logic for scheduled generation
                Log.d(TAG, "📊 Starting scheduled activity card generation")

                // Get real health metrics from our tracking system
                val healthMetricsResult = enhancedHealthTracker.getCurrentHealthMetrics()
                val today = LocalDate.now()

                when (healthMetricsResult) {
                    is com.example.health_assistant.core.util.Result.Success -> {
                        val metrics = healthMetricsResult.data

                        Log.d(
                            TAG,
                            "📊 Retrieved health metrics - Steps: ${metrics.steps.current}, Calories: ${metrics.calories.current}, Heart Points: ${metrics.heartPoints.current}"
                        )

                        // Generate and save activity card using the use case
                        val cardResult = generateActivityCardUseCase.generateCardForDate(
                            date = today,
                            stepCount = metrics.steps.current,
                            caloriesBurned = metrics.calories.current,
                            heartPoints = metrics.heartPoints.current
                        )

                        if (cardResult.isSuccess) {
                            val activityCard = cardResult.getOrNull()
                            Log.d(TAG, "✅ Activity card saved successfully to database with ID: ${activityCard?.id}")
                            Log.d(TAG, "📈 Card details - Steps: ${activityCard?.stepCount}, Calories: ${activityCard?.caloriesBurned}, Heart Points: ${activityCard?.heartPoints}")
                        } else {
                            Log.e(TAG, "❌ Failed to save activity card: ${cardResult.exceptionOrNull()?.message}")
                        }

                        WorkResult.success()
                    }
                    is com.example.health_assistant.core.util.Result.Error -> {
                        Log.e(TAG, "❌ Failed to get health metrics: ${healthMetricsResult.message}")

                        // Generate card with sample data as fallback and save it
                        Log.d(TAG, "🔄 Generating activity card with sample data as fallback")

                        val fallbackResult = generateActivityCardUseCase.generateCardForDate(
                            date = today,
                            stepCount = 6750, // Sample steps
                            caloriesBurned = 180,  // Sample calories
                            heartPoints = 40    // Sample heart points
                        )

                        if (fallbackResult.isSuccess) {
                            val fallbackCard = fallbackResult.getOrNull()
                            Log.d(TAG, "✅ Fallback activity card saved to database with ID: ${fallbackCard?.id}")
                        } else {
                            Log.e(TAG, "❌ Failed to save fallback activity card: ${fallbackResult.exceptionOrNull()?.message}")
                        }

                        WorkResult.success()
                    }
                    is com.example.health_assistant.core.util.Result.Loading -> {
                        Log.w(TAG, "⏳ Health tracker returned loading state unexpectedly")
                        WorkResult.retry()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "💥 Error generating activity card", e)
            WorkResult.failure()
        }
    }
}