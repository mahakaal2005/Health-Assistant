    package com.example.health_assistant.features.journal.domain.usecase

import com.example.health_assistant.features.journal.domain.ActivityCard
import com.example.health_assistant.features.journal.domain.ActivityCardRepository
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject

/**
 * Use case for retrieving activity cards
 * Provides various ways to fetch activity card data
 */
class GetActivityCardsUseCase @Inject constructor(
    private val activityCardRepository: ActivityCardRepository
) {

    /**
     * Get all activity cards ordered by date (newest first)
     */
    fun getAllActivityCards(): Flow<List<ActivityCard>> {
        return activityCardRepository.getAllActivityCards()
    }

    /**
     * Get activity card for a specific date
     */
    suspend fun getActivityCardByDate(date: LocalDate): ActivityCard? {
        return activityCardRepository.getActivityCardByDate(date)
    }

    /**
     * Get activity card for today
     */
    suspend fun getTodayActivityCard(): ActivityCard? {
        return activityCardRepository.getActivityCardByDate(LocalDate.now())
    }

    /**
     * Get recent activity cards (limited count)
     */
    fun getRecentActivityCards(limit: Int = 30): Flow<List<ActivityCard>> {
        return activityCardRepository.getRecentActivityCards(limit)
    }

    /**
     * Get activity cards for the last N days
     */
    fun getActivityCardsLastNDays(days: Int): Flow<List<ActivityCard>> {
        val endDate = LocalDate.now()
        val startDate = endDate.minusDays(days.toLong() - 1)
        return activityCardRepository.getActivityCardsByDateRange(startDate, endDate)
    }

    /**
     * Get activity cards for current week
     */
    fun getThisWeekActivityCards(): Flow<List<ActivityCard>> {
        val today = LocalDate.now()
        val startOfWeek = today.minusDays(today.dayOfWeek.value.toLong() - 1)
        return activityCardRepository.getActivityCardsByDateRange(startOfWeek, today)
    }

    /**
     * Get activity cards for current month
     */
    fun getThisMonthActivityCards(): Flow<List<ActivityCard>> {
        val today = LocalDate.now()
        val startOfMonth = today.withDayOfMonth(1)
        return activityCardRepository.getActivityCardsByDateRange(startOfMonth, today)
    }

    /**
     * Check if activity card exists for a specific date
     */
    suspend fun hasActivityCardForDate(date: LocalDate): Boolean {
        return activityCardRepository.activityCardExistsForDate(date)
    }

    /**
     * Get total count of activity cards
     */
    suspend fun getActivityCardCount(): Int {
        return activityCardRepository.getActivityCardCount()
    }
}