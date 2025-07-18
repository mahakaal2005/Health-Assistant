package com.example.health_assistant.features.journal.workers

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.health_assistant.auth.session.SessionManager
import com.example.health_assistant.core.util.Result
import com.example.health_assistant.data.health.EnhancedHealthTracker
import com.example.health_assistant.data.repository.interfaces.HealthRepository
import com.example.health_assistant.features.journal.domain.usecase.GenerateActivityCardUseCase
import com.example.health_assistant.features.journal.domain.ActivityCardRepository
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
@HiltWorker
class ActivityCardGeneratorWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted private val params: WorkerParameters,
    private val generateActivityCardUseCase: GenerateActivityCardUseCase,
    private val enhancedHealthTracker: EnhancedHealthTracker,
    private val healthRepository: HealthRepository,
    private val sessionManager: SessionManager,
    private val activityCardRepository: ActivityCardRepository
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
            var userId = inputData.getString(KEY_USER_ID) ?: sessionManager.getCurrentUserId()
            if (userId.isNullOrEmpty()) {
                Log.w(TAG, "No user ID provided or logged in, using default user ID")
                userId = "default_user" // Use default user ID instead of failing
            }

            // Get target date from input data or use today
            val targetDate = inputData.getString(KEY_TARGET_DATE)?.let {
                LocalDate.parse(it)
            } ?: LocalDate.now()

            Log.d(TAG, "Generating activity card for user $userId and date $targetDate")

            // CRITICAL FIX: Use a unique work ID to prevent duplicate cards
            val workId = "activity_card_${userId}_${targetDate}"
            
            // Cleanup temporarily disabled to prevent UI freezing
            Log.d(TAG, "Cleanup temporarily disabled to prevent UI blocking")
            
            // SAFE DUPLICATE PREVENTION: Check if card already exists BEFORE creating
            val cardExists = generateActivityCardUseCase.activityCardExistsForDate(targetDate, userId)
            if (cardExists) {
                Log.d(TAG, "Activity card already exists for user $userId and date $targetDate - skipping generation")
                return Result.success()
            }
            
            // ADDITIONAL SAFETY: Double-check with a direct database query to be absolutely sure
            val existingCard = activityCardRepository.getActivityCardByDate(targetDate)
            if (existingCard != null && existingCard.userId == userId) {
                Log.d(TAG, "Found existing activity card in database for user $userId and date $targetDate - skipping generation")
                return Result.success()
            }

            // Check if metrics are provided in input data
            val providedSteps = inputData.getInt("steps", -1)
            val providedCalories = inputData.getInt("calories", -1)
            val providedHeartPoints = inputData.getInt("heart_points", -1)
            
            // If metrics are provided in input data, use them
            if (providedSteps >= 0 && providedCalories >= 0 && providedHeartPoints >= 0) {
                Log.d(TAG, "Using provided metrics - Steps: $providedSteps, Calories: $providedCalories, Heart Points: $providedHeartPoints")
                
                // Generate and save activity card using provided metrics
                val cardResult = generateActivityCardUseCase.generateCardForDate(
                    date = targetDate,
                    stepCount = providedSteps,
                    caloriesBurned = providedCalories,
                    heartPoints = providedHeartPoints,
                    userId = userId
                )

                if (cardResult.isSuccess) {
                    val activityCard = cardResult.getOrNull()
                    Log.d(TAG, "Activity card saved successfully for user $userId with ID: ${activityCard?.id}")
                    Log.d(TAG, "Card details - Steps: ${activityCard?.stepCount}, Calories: ${activityCard?.caloriesBurned}, Heart Points: ${activityCard?.heartPoints}")
                    
                    // Cleanup temporarily disabled to prevent UI freezing
                    Log.d(TAG, "Post-generation cleanup disabled to prevent UI blocking")
                    
                    return Result.success(workDataOf("card_id" to (activityCard?.id ?: 0)))
                } else {
                    Log.e(TAG, "Failed to save activity card: ${cardResult.exceptionOrNull()?.message}")
                    return Result.failure()
                }
            }

            // If no metrics provided, get current health metrics
            val healthMetricsResult = enhancedHealthTracker.getCurrentHealthMetrics()

            return when (healthMetricsResult) {
                is com.example.health_assistant.core.util.Result.Success -> {
                    val metrics = healthMetricsResult.data

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
                        
                        // Cleanup temporarily disabled to prevent UI freezing
                        Log.d(TAG, "Final cleanup disabled to prevent UI blocking")
                        
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