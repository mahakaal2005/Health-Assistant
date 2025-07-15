package com.example.health_assistant.features.journal.domain.usecase

import com.example.health_assistant.features.journal.domain.ActivityCard
import com.example.health_assistant.features.journal.domain.ActivityCardRepository
import java.time.LocalDate
import javax.inject.Inject

/**
 * Use case for generating daily activity cards with real health metrics
 * Handles the 3 essential metrics: steps, calories, heart points
 */
class GenerateActivityCardUseCase @Inject constructor(
    private val activityCardRepository: ActivityCardRepository
) {

    /**
     * Generate activity card for a specific date with the 3 essential metrics
     */
    suspend fun generateCardForDate(
        date: LocalDate,
        stepCount: Int = 0,
        caloriesBurned: Int = 0,
        heartPoints: Int = 0
    ): Result<ActivityCard> {
        return try {
            // Check if card already exists for this date
            val existingCard = activityCardRepository.getActivityCardByDate(date)

            val activityCard = if (existingCard != null) {
                // Update existing card with new data (only update if new data is provided)
                existingCard.copy(
                    stepCount = if (stepCount > 0) stepCount else existingCard.stepCount,
                    caloriesBurned = if (caloriesBurned > 0) caloriesBurned else existingCard.caloriesBurned,
                    heartPoints = if (heartPoints > 0) heartPoints else existingCard.heartPoints
                )
            } else {
                // Create new card
                ActivityCard(
                    id = 0, // Will be assigned by Room
                    date = date,
                    stepCount = stepCount,
                    caloriesBurned = caloriesBurned,
                    heartPoints = heartPoints
                )
            }

            val cardId = activityCardRepository.insertActivityCard(activityCard)
            Result.success(activityCard.copy(id = cardId))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}