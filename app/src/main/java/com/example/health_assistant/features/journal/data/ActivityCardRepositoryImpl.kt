package com.example.health_assistant.features.journal.data

import com.example.health_assistant.auth.session.SessionManager
import com.example.health_assistant.features.journal.domain.ActivityCard
import com.example.health_assistant.features.journal.domain.ActivityCardRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
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
        private const val ACTIVITY_TYPE = "activity_summary"
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
            
            // CRITICAL FIX: Use direct query to check if card exists
            val count = journalDao.getEntryCountByTypeAndDateRangeAndUserId(
                ACTIVITY_TYPE, 
                startTimestamp, 
                endTimestamp, 
                effectiveUserId
            )
            
            val exists = count > 0
            Log.d(TAG, "Checking card for date $date: found $count cards for user $effectiveUserId, exists=$exists")
            
            return exists
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
            
            // SAFE DUPLICATE PREVENTION: Check for existing entries with timeout
            val existingEntries = try {
                withContext(Dispatchers.IO) {
                    journalDao.getEntriesByTypeAndDateRangeAndUserId(ACTIVITY_TYPE, startTimestamp, endTimestamp, userId).first()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error checking for existing entries, proceeding with caution", e)
                emptyList()
            }
            
            val existingEntry = existingEntries.firstOrNull()
            
            val journalEntry = activityCardMapper.toJournalEntry(cardWithUserId).copy(userId = userId)
            
            return if (existingEntry != null) {
                // Update existing entry instead of creating duplicate
                Log.d(TAG, "DUPLICATE PREVENTION: Updating existing card for date ${activityCard.date} with ID ${existingEntry.id}")
                journalDao.updateEntry(journalEntry.copy(id = existingEntry.id))
                existingEntry.id
            } else {
                // SAFE INSERTION: Create new entry only if none exists
                Log.d(TAG, "SAFE INSERTION: Creating new card for date ${activityCard.date}")
                
                // Double-check right before insertion to prevent race conditions
                val lastMinuteCheck = try {
                    journalDao.getEntryCountByTypeAndDateRangeAndUserId(ACTIVITY_TYPE, startTimestamp, endTimestamp, userId)
                } catch (e: Exception) {
                    0
                }
                
                if (lastMinuteCheck > 0) {
                    Log.w(TAG, "RACE CONDITION DETECTED: Card was created by another process, skipping insertion")
                    return -1 // Indicate that insertion was skipped
                }
                
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

    override suspend fun cleanupDuplicateActivityCards(date: LocalDate, userId: String): Int {
        try {
            val startTimestamp = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val endTimestamp = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1
            
            // Use provided user ID or get from session manager
            val effectiveUserId = if (userId.isNotEmpty()) {
                userId
            } else {
                getCurrentUserId()
            }
            
            // CRITICAL FIX: Use direct SQL query to delete duplicates
            val deletedCount = journalDao.deleteDuplicateEntries(
                ACTIVITY_TYPE,
                startTimestamp,
                endTimestamp,
                effectiveUserId
            )
            
            Log.d(TAG, "Cleaned up $deletedCount duplicate activity cards for date $date and user $effectiveUserId")
            return deletedCount
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up duplicate cards for date $date and user $userId", e)
            return 0
        }
    }
}