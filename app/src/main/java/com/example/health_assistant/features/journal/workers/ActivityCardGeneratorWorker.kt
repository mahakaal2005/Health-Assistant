package com.example.health_assistant.features.journal.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.health_assistant.auth.session.SessionManager
import com.example.health_assistant.core.util.Result
import com.example.health_assistant.data.health.EnhancedHealthTracker
import com.example.health_assistant.data.repository.interfaces.HealthRepository
import com.example.health_assistant.features.journal.domain.usecase.GenerateActivityCardUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

/**
 * Worker for generating daily activity cards
 * Runs at midnight to summarize the day's health metrics
 * Now with proper user isolation for multi-user support
 */
class ActivityCardGeneratorWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted private val params: WorkerParameters,
    private val generateActivityCardUseCase: GenerateActivityCardUseCase,
    private val enhancedHealthTracker: EnhancedHealthTracker,
    private val healthRepository: HealthRepository,
    private val sessionManager: SessionManager
) : CoroutineWorker(context, params) {

    companion object {
        const val WORK_NAME = "activity_card_generator_work"
        private const val TAG = "ActivityCardGenerator"
        const val KEY_TARGET_DATE = "target_date"
        const val KEY_USER_ID = "user_id"
    }

    override suspend fun doWork(): Result {
        try {
            // Get user ID from input data or session
            val userId = inputData.getString(KEY_USER_ID) ?: sessionManager.getCurrentUserId()
            if (userId.isNullOrEmpty()) {
                Log.w(TAG, "No user ID provided or logged in, skipping activity card generation")
                return Result.failure()
            }

            // Get target date from input data or use today
            val targetDate = inputData.getString(KEY_TARGET_DATE)?.let {
                LocalDate.parse(it)
            } ?: LocalDate.now()

            Log.d(TAG, "Generating activity card for user $userId and date $targetDate")

            // Check if card already exists for this date and user
            if (generateActivityCardUseCase.activityCardExistsForDate(targetDate)) {
                Log.d(TAG, "Activity card already exists for user $userId and date $targetDate")
                return Result.success()
            }

            // Get health metrics for the day
            val healthMetricsResult = enhancedHealthTracker.getCurrentHealthMetrics()

            return when (healthMetricsResult) {
                is com.example.health_assistant.core.util.Result.Success -> {
                    val metrics = healthMetricsResult.data
                    if (metrics == null) {
                        Log.w(TAG, "No health metrics available for date $targetDate")
                        return Result.failure()
                    }

                    // Generate and save activity card using the use case
                    val cardResult = generateActivityCardUseCase.generateCardForDate(
                        date = targetDate,
                        stepCount = metrics.steps.current,
                        caloriesBurned = metrics.calories.current,
                        heartPoints = metrics.heartPoints.current,
                        userId = userId
                    )

                    if (cardResult.isSuccess) {
                        val activityCard = cardResult.getOrNull()
                        Log.d(TAG, "Activity card saved successfully for user $userId with ID: ${activityCard?.id}")
                        Log.d(TAG, "Card details - Steps: ${activityCard?.stepCount}, Calories: ${activityCard?.caloriesBurned}, Heart Points: ${activityCard?.heartPoints}")
                        Result.success(workDataOf("card_id" to (activityCard?.id ?: 0)))
                    } else {
                        Log.e(TAG, "Failed to save activity card: ${cardResult.exceptionOrNull()?.message}")
                        Result.failure()
                    }
                }
                is com.example.health_assistant.core.util.Result.Error -> {
                    Log.e(TAG, "Failed to get health metrics: ${healthMetricsResult.message}")
                    Result.failure()
                }
                else -> {
                    Log.e(TAG, "Unexpected result type from health repository")
                    Result.failure()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating activity card", e)
            return Result.failure()
        }
    }
}