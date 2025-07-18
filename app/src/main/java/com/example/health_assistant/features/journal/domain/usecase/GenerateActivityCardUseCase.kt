package com.example.health_assistant.features.journal.domain.usecase

import android.util.Log
import com.example.health_assistant.auth.session.SessionManager
import com.example.health_assistant.features.journal.domain.ActivityCard
import com.example.health_assistant.features.journal.domain.ActivityCardRepository
import java.time.LocalDate
import javax.inject.Inject

/**
 * Use case for generating daily activity cards with real health metrics
 * Handles the 3 essential metrics: steps, calories, heart points
 * Now with proper user isolation for multi-user support
 */
class GenerateActivityCardUseCase @Inject constructor(
    private val activityCardRepository: ActivityCardRepository,
    private val sessionManager: SessionManager
) {
    private val TAG = "GenerateActivityCardUseCase"

    /**
     * Check if an activity card already exists for a specific date
     */
    suspend fun activityCardExistsForDate(date: LocalDate): Boolean {
        return try {
            activityCardRepository.activityCardExistsForDate(date)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking if card exists for date $date", e)
            false
        }
    }

    /**
     * Generate activity card for a specific date with the 3 essential metrics
     * and specific user ID
     */
    suspend fun generateCardForDate(
        date: LocalDate,
        stepCount: Int = 0,
        caloriesBurned: Int = 0,
        heartPoints: Int = 0,
        userId: String = ""
    ): Result<ActivityCard> {
        return try {
            // Use provided user ID or get from session manager
            val effectiveUserId = if (userId.isNotEmpty()) {
                userId
            } else {
                sessionManager.getCurrentUserId() ?: ""
            }
            
            if (effectiveUserId.isEmpty()) {
                Log.w(TAG, "No user ID provided or logged in, cannot generate activity card")
                return Result.failure(IllegalStateException("No user ID available"))
            }

            Log.d(TAG, "Generating activity card for user $effectiveUserId and date $date")
            
            val existingCard = activityCardRepository.getActivityCardByDate(date)

            val cardToSave = if (existingCard != null) {
                // Update existing card with new metrics
                // Always use the effective user ID to ensure proper user isolation
                existingCard.copy(
                    stepCount = stepCount,
                    caloriesBurned = caloriesBurned,
                    heartPoints = heartPoints,
                    userId = effectiveUserId
                )
            } else {
                // Create a new card with the effective user ID
                ActivityCard(
                    date = date,
                    stepCount = stepCount,
                    caloriesBurned = caloriesBurned,
                    heartPoints = heartPoints,
                    userId = effectiveUserId
                )
            }

            // Save the card to repository
            val savedCardId = activityCardRepository.insertActivityCard(cardToSave)
            
            // Return the card with the correct ID
            val finalCard = if (existingCard == null) {
                cardToSave.copy(id = savedCardId)
            } else {
                cardToSave
            }

            Log.d(TAG, "Successfully generated/updated activity card with ID ${finalCard.id} for user $effectiveUserId")
            Result.success(finalCard)
        } catch (e: Exception) {
            Log.e(TAG, "Error generating or updating card for date $date", e)
            Result.failure(e)
        }
    }
}