package com.example.health_assistant.features.journal.domain

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Simplified repository interface for Activity Card operations
 * Only handles the 3 essential metrics: steps, calories, heart rate
 */
interface ActivityCardRepository {

    /**
     * Get all activity cards ordered by date (newest first)
     */
    fun getAllActivityCards(): Flow<List<ActivityCard>>

    /**
     * Get activity card for a specific date
     */
    suspend fun getActivityCardByDate(date: LocalDate): ActivityCard?

    /**
     * Get activity cards within a date range
     */
    fun getActivityCardsByDateRange(startDate: LocalDate, endDate: LocalDate): Flow<List<ActivityCard>>

    /**
     * Get the most recent activity cards (limited)
     */
    fun getRecentActivityCards(limit: Int): Flow<List<ActivityCard>>

    /**
     * Check if activity card exists for a specific date and user
     */
    suspend fun activityCardExistsForDate(date: LocalDate, userId: String = ""): Boolean

    /**
     * Insert or update an activity card
     */
    suspend fun insertActivityCard(activityCard: ActivityCard): Long

    /**
     * Get total count of activity cards
     */
    suspend fun getActivityCardCount(): Int

    /**
     * Clean up duplicate activity cards for a specific date and user
     * Keeps only the latest card (highest ID) and deletes all others
     */
    suspend fun cleanupDuplicateActivityCards(date: LocalDate, userId: String): Int
}