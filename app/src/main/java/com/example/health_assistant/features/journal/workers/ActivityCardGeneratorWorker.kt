package com.example.health_assistant.features.journal.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.health_assistant.data.health.EnhancedHealthTracker
import com.example.health_assistant.features.journal.domain.usecase.GenerateActivityCardUseCase
import com.example.health_assistant.core.util.Result
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalDate
import android.util.Log

/**
 * Background worker that automatically generates activity cards at midnight
 * Uses real health data from the step tracking system
 */
@HiltWorker
class ActivityCardGeneratorWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val generateActivityCardUseCase: GenerateActivityCardUseCase,
    private val enhancedHealthTracker: EnhancedHealthTracker
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val WORK_NAME = "activity_card_generator"
        private const val TAG = "ActivityCardWorker"
    }

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "Starting activity card generation for today")

            // Get real health metrics from our tracking system
            val healthMetricsResult = enhancedHealthTracker.getCurrentHealthMetrics()

            when (healthMetricsResult) {
                is com.example.health_assistant.core.util.Result.Success -> {
                    val metrics = healthMetricsResult.data
                    val today = LocalDate.now()

                    Log.d(
                        TAG,
                        "Retrieved health metrics - Steps: ${metrics.steps.current}, Calories: ${metrics.calories.current}, Heart Points: ${metrics.heartPoints.current}"
                    )

                    // Generate activity card with real data
                    val cardResult = generateActivityCardUseCase.generateCardForDate(
                        date = today,
                        stepCount = metrics.steps.current,
                        caloriesBurned = metrics.calories.current,
                        heartPoints = metrics.heartPoints.current
                    )

                    if (cardResult.isSuccess) {
                        Log.d(TAG, "Activity card generated successfully for $today")
                        Result.success()
                    } else {
                        Log.e(TAG, "Failed to generate activity card")
                        Result.retry()
                    }
                }
                is com.example.health_assistant.core.util.Result.Error -> {
                    Log.e(TAG, "Failed to get health metrics: ${healthMetricsResult.message}")
                    // Create card with zero values if we can't get real data
                    val today = LocalDate.now()
                    val cardResult = generateActivityCardUseCase.generateCardForDate(
                        date = today,
                        stepCount = 0,
                        caloriesBurned = 0,
                        heartPoints = 0
                    )

                    if (cardResult.isSuccess) {
                        Result.success()
                    } else {
                        Result.retry()
                    }
                }
                is com.example.health_assistant.core.util.Result.Loading -> {
                    Log.w(TAG, "Health metrics still loading, retrying...")
                    Result.retry()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in activity card generation", e)
            Result.failure()
        }
    }
}