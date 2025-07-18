package com.example.health_assistant.features.journal.data

import com.example.health_assistant.auth.session.SessionManager
import com.example.health_assistant.features.journal.domain.ActivityCard
import com.example.health_assistant.features.journal.domain.ActivityCardRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import android.util.Log

@Singleton
class ActivityCardRepositoryImpl @Inject constructor(
    private val journalDao: JournalEntryDao,
    private val sessionManager: SessionManager,
    private val activityCardMapper: ActivityCardMapper
) : ActivityCardRepository {

    companion object {
        private const val TAG = "ActivityCardRepo"
        private const val ACTIVITY_TYPE = "activity_card"
    }
    
    private fun getCurrentUserId(): String {
        val userId = sessionManager.getCurrentUserId()
        if (userId.isNullOrEmpty()) {
            Log.w(TAG, "No user ID available, using default")
            return "default_user"
        }
        return userId
    }

    override fun getAllActivityCards(): Flow<List<ActivityCard>> {
        val userId = getCurrentUserId()
        return journalDao.getEntriesByTypeAndUserId(ACTIVITY_TYPE, userId)
            .map { entities ->
                entities.mapNotNull { entity ->
                    try {
                        activityCardMapper.toActivityCard(entity)?.copy(userId = userId)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error mapping activity card", e)
                        null
                    }
                }
            }
    }

    override suspend fun getActivityCardByDate(date: LocalDate): ActivityCard? {
        val startTimestamp = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endTimestamp = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1
        val userId = getCurrentUserId()

        return journalDao.getEntriesByTypeAndDateRangeAndUserId(ACTIVITY_TYPE, startTimestamp, endTimestamp, userId)
            .map { entities ->
                entities.firstOrNull()?.let { entity ->
                    try {
                        activityCardMapper.toActivityCard(entity)?.copy(userId = userId)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error mapping activity card", e)
                        null
                    }
                }
            }
            .first()
    }

    override fun getActivityCardsByDateRange(startDate: LocalDate, endDate: LocalDate): Flow<List<ActivityCard>> {
        val startTimestamp = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endTimestamp = endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1
        val userId = getCurrentUserId()

        return journalDao.getEntriesByTypeAndDateRangeAndUserId(ACTIVITY_TYPE, startTimestamp, endTimestamp, userId)
            .map { entities ->
                entities.mapNotNull { entity ->
                    try {
                        activityCardMapper.toActivityCard(entity)?.copy(userId = userId)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error mapping activity card", e)
                        null
                    }
                }
            }
    }

    override fun getRecentActivityCards(limit: Int): Flow<List<ActivityCard>> {
        val userId = getCurrentUserId()
        return journalDao.getRecentEntriesByTypeAndUserId(ACTIVITY_TYPE, userId, limit)
            .map { entities ->
                entities.mapNotNull { entity ->
                    try {
                        activityCardMapper.toActivityCard(entity)?.copy(userId = userId)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error mapping activity card", e)
                        null
                    }
                }
            }
    }

    override suspend fun activityCardExistsForDate(date: LocalDate, userId: String): Boolean {
        try {
            val startTimestamp = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val endTimestamp = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1
            
            // Use provided user ID or get from session manager
            val effectiveUserId = if (userId.isNotEmpty()) {
                userId
            } else {
                getCurrentUserId()
            }
            
            val entries = journalDao.getEntriesByTypeAndDateRangeAndUserId(ACTIVITY_TYPE, startTimestamp, endTimestamp, effectiveUserId).first()
            Log.d(TAG, "Checking card for date $date: found ${entries.size} cards for user $effectiveUserId")
            
            return entries.isNotEmpty()
        } catch (e: Exception) {
            Log.e(TAG, "Error checking if card exists for date $date and user $userId", e)
            return false
        }
    }

    override suspend fun insertActivityCard(activityCard: ActivityCard): Long {
        try {
            val userId = getCurrentUserId()
            val startTimestamp = activityCard.date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val endTimestamp = activityCard.date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1
            
            // Always use the current user's ID for new cards
            val cardWithUserId = activityCard.copy(userId = userId)
            
            // Get existing entry for this date and user
            val existingEntries = journalDao.getEntriesByTypeAndDateRangeAndUserId(ACTIVITY_TYPE, startTimestamp, endTimestamp, userId).first()
            val existingEntry = existingEntries.firstOrNull()
            
            val journalEntry = activityCardMapper.toJournalEntry(cardWithUserId).copy(userId = userId)
            
            return if (existingEntry != null) {
                // Update existing entry
                Log.d(TAG, "Updating existing card for date ${activityCard.date} with ID ${existingEntry.id}")
                journalDao.updateEntry(journalEntry.copy(id = existingEntry.id))
                existingEntry.id
            } else {
                // Create new entry
                Log.d(TAG, "Creating new card for date ${activityCard.date}")
                journalDao.insertEntry(journalEntry)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting activity card for date ${activityCard.date}", e)
            throw e
        }
    }

    override suspend fun getActivityCardCount(): Int {
        val userId = getCurrentUserId()
        val entries = journalDao.getAllEntriesByUserId(userId).first()
        return entries.count { it.type == ACTIVITY_TYPE }
    }
}