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

/**
 * Implementation of ActivityCardRepository using existing journal system
 * Maps ActivityCard to/from JournalEntry to maintain compatibility
 * Now with proper user isolation for multi-user support
 */
@Singleton
class ActivityCardRepositoryImpl @Inject constructor(
    private val journalDao: JournalEntryDao,
    private val sessionManager: SessionManager
) : ActivityCardRepository {

    companion object {
        private const val ACTIVITY_TYPE = "activity_card"
    }
    
    private fun getCurrentUserId(): String {
        return sessionManager.getCurrentUserId() ?: ""
    }

    override fun getAllActivityCards(): Flow<List<ActivityCard>> {
        val userId = getCurrentUserId()
        return if (userId.isNotEmpty()) {
            journalDao.getEntriesByTypeAndUserId(ACTIVITY_TYPE, userId).map { entities ->
                entities.mapNotNull { entity ->
                    try {
                        ActivityCard(
                            id = entity.id,
                            date = java.time.Instant.ofEpochMilli(entity.timestamp).atZone(ZoneId.systemDefault()).toLocalDate(),
                            stepCount = entity.content?.substringAfter("steps:")?.substringBefore(",")?.trim()?.toIntOrNull() ?: 0,
                            caloriesBurned = entity.content?.substringAfter("calories:")?.substringBefore(",")?.trim()?.toIntOrNull() ?: 0,
                            heartPoints = entity.content?.substringAfter("heartPoints:")?.trim()?.toIntOrNull() ?: 0,
                            userId = entity.userId
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
            }
        } else {
            journalDao.getEntriesByType(ACTIVITY_TYPE).map { entities ->
                entities.mapNotNull { entity ->
                    try {
                        ActivityCard(
                            id = entity.id,
                            date = java.time.Instant.ofEpochMilli(entity.timestamp).atZone(ZoneId.systemDefault()).toLocalDate(),
                            stepCount = entity.content?.substringAfter("steps:")?.substringBefore(",")?.trim()?.toIntOrNull() ?: 0,
                            caloriesBurned = entity.content?.substringAfter("calories:")?.substringBefore(",")?.trim()?.toIntOrNull() ?: 0,
                            heartPoints = entity.content?.substringAfter("heartPoints:")?.trim()?.toIntOrNull() ?: 0,
                            userId = entity.userId
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
            }
        }
    }

    override suspend fun getActivityCardByDate(date: LocalDate): ActivityCard? {
        val startTimestamp = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endTimestamp = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1
        val userId = getCurrentUserId()

        return if (userId.isNotEmpty()) {
            journalDao.getEntriesByTypeAndDateRangeAndUserId(ACTIVITY_TYPE, startTimestamp, endTimestamp, userId)
                .map { entities ->
                    entities.firstOrNull()?.let { entity ->
                        try {
                            ActivityCard(
                                id = entity.id,
                                date = java.time.Instant.ofEpochMilli(entity.timestamp).atZone(ZoneId.systemDefault()).toLocalDate(),
                                stepCount = entity.content?.substringAfter("steps:")?.substringBefore(",")?.trim()?.toIntOrNull() ?: 0,
                                caloriesBurned = entity.content?.substringAfter("calories:")?.substringBefore(",")?.trim()?.toIntOrNull() ?: 0,
                                heartPoints = entity.content?.substringAfter("heartPoints:")?.trim()?.toIntOrNull() ?: 0,
                                userId = entity.userId
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }
                }
                .first()
        } else {
            journalDao.getEntriesByTypeAndDateRange(ACTIVITY_TYPE, startTimestamp, endTimestamp)
                .map { entities ->
                    entities.firstOrNull()?.let { entity ->
                        try {
                            ActivityCard(
                                id = entity.id,
                                date = java.time.Instant.ofEpochMilli(entity.timestamp).atZone(ZoneId.systemDefault()).toLocalDate(),
                                stepCount = entity.content?.substringAfter("steps:")?.substringBefore(",")?.trim()?.toIntOrNull() ?: 0,
                                caloriesBurned = entity.content?.substringAfter("calories:")?.substringBefore(",")?.trim()?.toIntOrNull() ?: 0,
                                heartPoints = entity.content?.substringAfter("heartPoints:")?.trim()?.toIntOrNull() ?: 0,
                                userId = entity.userId
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }
                }
                .first()
        }
    }

    override fun getActivityCardsByDateRange(startDate: LocalDate, endDate: LocalDate): Flow<List<ActivityCard>> {
        val startTimestamp = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endTimestamp = endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1
        val userId = getCurrentUserId()

        return if (userId.isNotEmpty()) {
            journalDao.getEntriesByTypeAndDateRangeAndUserId(ACTIVITY_TYPE, startTimestamp, endTimestamp, userId).map { entities ->
                entities.mapNotNull { entity ->
                    try {
                        ActivityCard(
                            id = entity.id,
                            date = java.time.Instant.ofEpochMilli(entity.timestamp).atZone(ZoneId.systemDefault()).toLocalDate(),
                            stepCount = entity.content?.substringAfter("steps:")?.substringBefore(",")?.trim()?.toIntOrNull() ?: 0,
                            caloriesBurned = entity.content?.substringAfter("calories:")?.substringBefore(",")?.trim()?.toIntOrNull() ?: 0,
                            heartPoints = entity.content?.substringAfter("heartPoints:")?.trim()?.toIntOrNull() ?: 0,
                            userId = entity.userId
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
            }
        } else {
            journalDao.getEntriesByTypeAndDateRange(ACTIVITY_TYPE, startTimestamp, endTimestamp).map { entities ->
                entities.mapNotNull { entity ->
                    try {
                        ActivityCard(
                            id = entity.id,
                            date = java.time.Instant.ofEpochMilli(entity.timestamp).atZone(ZoneId.systemDefault()).toLocalDate(),
                            stepCount = entity.content?.substringAfter("steps:")?.substringBefore(",")?.trim()?.toIntOrNull() ?: 0,
                            caloriesBurned = entity.content?.substringAfter("calories:")?.substringBefore(",")?.trim()?.toIntOrNull() ?: 0,
                            heartPoints = entity.content?.substringAfter("heartPoints:")?.trim()?.toIntOrNull() ?: 0,
                            userId = entity.userId
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
            }
        }
    }

    override fun getRecentActivityCards(limit: Int): Flow<List<ActivityCard>> {
        val userId = getCurrentUserId()
        
        return if (userId.isNotEmpty()) {
            journalDao.getRecentEntriesByUserId(userId, limit).map { entities ->
                entities.filter { it.type == ACTIVITY_TYPE }.mapNotNull { entity ->
                    try {
                        ActivityCard(
                            id = entity.id,
                            date = java.time.Instant.ofEpochMilli(entity.timestamp).atZone(ZoneId.systemDefault()).toLocalDate(),
                            stepCount = entity.content?.substringAfter("steps:")?.substringBefore(",")?.trim()?.toIntOrNull() ?: 0,
                            caloriesBurned = entity.content?.substringAfter("calories:")?.substringBefore(",")?.trim()?.toIntOrNull() ?: 0,
                            heartPoints = entity.content?.substringAfter("heartPoints:")?.trim()?.toIntOrNull() ?: 0,
                            userId = entity.userId
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
            }
        } else {
            journalDao.getRecentEntries(limit).map { entities ->
                entities.filter { it.type == ACTIVITY_TYPE }.mapNotNull { entity ->
                    try {
                        ActivityCard(
                            id = entity.id,
                            date = java.time.Instant.ofEpochMilli(entity.timestamp).atZone(ZoneId.systemDefault()).toLocalDate(),
                            stepCount = entity.content?.substringAfter("steps:")?.substringBefore(",")?.trim()?.toIntOrNull() ?: 0,
                            caloriesBurned = entity.content?.substringAfter("calories:")?.substringBefore(",")?.trim()?.toIntOrNull() ?: 0,
                            heartPoints = entity.content?.substringAfter("heartPoints:")?.trim()?.toIntOrNull() ?: 0,
                            userId = entity.userId
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
            }
        }
    }

    override suspend fun activityCardExistsForDate(date: LocalDate): Boolean {
        try {
            val startTimestamp = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val endTimestamp = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1
            val userId = getCurrentUserId()
            
            // Use a direct query to check for existence
            val count = if (userId.isNotEmpty()) {
                // Check for user-specific entry
                val entries = journalDao.getEntriesByTypeAndDateRangeAndUserId(ACTIVITY_TYPE, startTimestamp, endTimestamp, userId).first()
                entries.size
            } else {
                // Check for any entry
                val entries = journalDao.getEntriesByTypeAndDateRange(ACTIVITY_TYPE, startTimestamp, endTimestamp).first()
                entries.size
            }
            
            android.util.Log.d("ActivityCardRepo", "Checking card for date $date: found $count cards (timestamps: $startTimestamp to $endTimestamp)")
            
            return count > 0
        } catch (e: Exception) {
            android.util.Log.e("ActivityCardRepo", "Error checking if card exists for date $date", e)
            return false
        }
    }

    override suspend fun insertActivityCard(activityCard: ActivityCard): Long {
        try {
            val userId = getCurrentUserId()
            // First check if a card already exists for this date
            val startTimestamp = activityCard.date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val endTimestamp = activityCard.date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1
            
            // CRITICAL FIX: Use the activityCard's userId if it's set, otherwise use the current user ID
            val effectiveUserId = if (activityCard.userId.isNotEmpty()) activityCard.userId else userId
            
            // Get all existing entries for this date and user
            val existingEntries = if (effectiveUserId.isNotEmpty()) {
                journalDao.getEntriesByTypeAndDateRangeAndUserId(ACTIVITY_TYPE, startTimestamp, endTimestamp, effectiveUserId).first()
            } else {
                journalDao.getEntriesByTypeAndDateRange(ACTIVITY_TYPE, startTimestamp, endTimestamp).first()
            }
                
            // If there are existing entries, update the first one
            val existingEntry = existingEntries.firstOrNull()
            
            val content = "steps:${activityCard.stepCount}, calories:${activityCard.caloriesBurned}, heartPoints:${activityCard.heartPoints}"
            
            return if (existingEntry != null) {
                // Update the existing entry
                android.util.Log.d("ActivityCardRepo", "Updating existing card for date ${activityCard.date} with ID ${existingEntry.id}")
                
                val updatedEntity = existingEntry.copy(
                    content = content,
                    title = "Activity Summary",
                    userId = effectiveUserId // Use the effective user ID
                )
                
                journalDao.updateEntry(updatedEntity)
                existingEntry.id
            } else {
                // Create a new entry
                android.util.Log.d("ActivityCardRepo", "Creating new card for date ${activityCard.date}")
                
                val entity = JournalEntryEntity(
                    id = if (activityCard.id == 0L) 0 else activityCard.id,
                    timestamp = startTimestamp, // Use start of day for consistent timestamp
                    type = ACTIVITY_TYPE,
                    title = "Activity Summary",
                    content = content,
                    userId = effectiveUserId // Use the effective user ID
                )
                
                journalDao.insertEntry(entity)
            }
        } catch (e: Exception) {
            android.util.Log.e("ActivityCardRepo", "Error inserting activity card for date ${activityCard.date}", e)
            throw e
        }
    }

    override suspend fun getActivityCardCount(): Int {
        val userId = getCurrentUserId()
        return if (userId.isNotEmpty()) {
            val entries = journalDao.getAllEntriesByUserId(userId).first()
            entries.count { it.type == ACTIVITY_TYPE }
        } else {
            val entries = journalDao.getAllEntries().first()
            entries.count { it.type == ACTIVITY_TYPE }
        }
    }
}