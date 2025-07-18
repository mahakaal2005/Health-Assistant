package com.example.health_assistant.features.journal.utils

import android.content.Context
import android.util.Log
import com.example.health_assistant.features.journal.data.JournalEntryDao
import com.example.health_assistant.features.journal.domain.ActivityCardRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Utility class for cleaning up duplicate activity cards
 * Provides methods to clean up cards for all users and dates
 */
@Singleton
class ActivityCardCleanupUtil @Inject constructor(
    private val activityCardRepository: ActivityCardRepository,
    private val journalEntryDao: JournalEntryDao
) {
    companion object {
        private const val TAG = "ActivityCardCleanupUtil"
        private const val ACTIVITY_TYPE = "activity_summary"
    }
    
    /**
     * Clean up all duplicate activity cards in the database
     * This is a heavy operation and should be used sparingly
     * CRITICAL: Only deletes activity_summary entries, not other journal types
     */
    fun cleanupAllDuplicateCards() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Get all unique user IDs
                val userIds = journalEntryDao.getAllDistinctUserIds()
                Log.d(TAG, "Found ${userIds.size} users with activity cards")
                
                var totalDeleted = 0
                
                // For each user, clean up their cards
                for (userId in userIds) {
                    // Get all dates with activity cards for this user
                    val dates = getActivityCardDatesForUser(userId)
                    Log.d(TAG, "Found ${dates.size} dates with activity cards for user $userId")
                    
                    // For each date, clean up duplicate cards
                    for (date in dates) {
                        // Only clean up activity_summary type entries
                        val deletedCount = activityCardRepository.cleanupDuplicateActivityCards(date, userId)
                        totalDeleted += deletedCount
                        Log.d(TAG, "Deleted $deletedCount duplicate activity cards for date $date and user $userId")
                    }
                }
                
                Log.d(TAG, "Cleanup complete. Deleted $totalDeleted duplicate activity cards")
            } catch (e: Exception) {
                Log.e(TAG, "Error cleaning up duplicate activity cards", e)
            }
        }
    }
    
    /**
     * Get all dates with activity cards for a specific user
     */
    private suspend fun getActivityCardDatesForUser(userId: String): List<LocalDate> {
        return withContext(Dispatchers.IO) {
            try {
                val entries = journalEntryDao.getDistinctDatesForUserAndType(ACTIVITY_TYPE, userId)
                entries.map { timestamp ->
                    val instant = java.time.Instant.ofEpochMilli(timestamp)
                    instant.atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                }.distinct()
            } catch (e: Exception) {
                Log.e(TAG, "Error getting activity card dates for user $userId", e)
                emptyList()
            }
        }
    }
} 