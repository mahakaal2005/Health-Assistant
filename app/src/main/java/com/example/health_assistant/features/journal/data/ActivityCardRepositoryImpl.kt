package com.example.health_assistant.features.journal.data

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
 */
@Singleton
class ActivityCardRepositoryImpl @Inject constructor(
    private val journalDao: JournalEntryDao
) : ActivityCardRepository {

    companion object {
        private const val ACTIVITY_TYPE = "activity_card"
    }

    override fun getAllActivityCards(): Flow<List<ActivityCard>> {
        return journalDao.getEntriesByType(ACTIVITY_TYPE).map { entities ->
            entities.mapNotNull { entity ->
                try {
                    ActivityCard(
                        id = entity.id,
                        date = java.time.Instant.ofEpochMilli(entity.timestamp).atZone(ZoneId.systemDefault()).toLocalDate(),
                        stepCount = entity.content?.substringAfter("steps:")?.substringBefore(",")?.trim()?.toIntOrNull() ?: 0,
                        caloriesBurned = entity.content?.substringAfter("calories:")?.substringBefore(",")?.trim()?.toIntOrNull() ?: 0,
                        heartPoints = entity.content?.substringAfter("heartPoints:")?.trim()?.toIntOrNull() ?: 0
                    )
                } catch (e: Exception) {
                    null
                }
            }
        }
    }

    override suspend fun getActivityCardByDate(date: LocalDate): ActivityCard? {
        val startTimestamp = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endTimestamp = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        return journalDao.getEntriesByTypeAndDateRange(ACTIVITY_TYPE, startTimestamp, endTimestamp)
            .map { entities ->
                entities.firstOrNull()?.let { entity ->
                    try {
                        ActivityCard(
                            id = entity.id,
                            date = java.time.Instant.ofEpochMilli(entity.timestamp).atZone(ZoneId.systemDefault()).toLocalDate(),
                            stepCount = entity.content?.substringAfter("steps:")?.substringBefore(",")?.trim()?.toIntOrNull() ?: 0,
                            caloriesBurned = entity.content?.substringAfter("calories:")?.substringBefore(",")?.trim()?.toIntOrNull() ?: 0,
                            heartPoints = entity.content?.substringAfter("heartPoints:")?.trim()?.toIntOrNull() ?: 0
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
            }
            .first()
    }

    override fun getActivityCardsByDateRange(startDate: LocalDate, endDate: LocalDate): Flow<List<ActivityCard>> {
        val startTimestamp = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endTimestamp = endDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        return journalDao.getEntriesByTypeAndDateRange(ACTIVITY_TYPE, startTimestamp, endTimestamp).map { entities ->
            entities.mapNotNull { entity ->
                try {
                    ActivityCard(
                        id = entity.id,
                        date = java.time.Instant.ofEpochMilli(entity.timestamp).atZone(ZoneId.systemDefault()).toLocalDate(),
                        stepCount = entity.content?.substringAfter("steps:")?.substringBefore(",")?.trim()?.toIntOrNull() ?: 0,
                        caloriesBurned = entity.content?.substringAfter("calories:")?.substringBefore(",")?.trim()?.toIntOrNull() ?: 0,
                        heartPoints = entity.content?.substringAfter("heartPoints:")?.trim()?.toIntOrNull() ?: 0
                    )
                } catch (e: Exception) {
                    null
                }
            }
        }
    }

    override fun getRecentActivityCards(limit: Int): Flow<List<ActivityCard>> {
        return journalDao.getRecentEntries(limit).map { entities ->
            entities.filter { it.type == ACTIVITY_TYPE }.mapNotNull { entity ->
                try {
                    ActivityCard(
                        id = entity.id,
                        date = java.time.Instant.ofEpochMilli(entity.timestamp).atZone(ZoneId.systemDefault()).toLocalDate(),
                        stepCount = entity.content?.substringAfter("steps:")?.substringBefore(",")?.trim()?.toIntOrNull() ?: 0,
                        caloriesBurned = entity.content?.substringAfter("calories:")?.substringBefore(",")?.trim()?.toIntOrNull() ?: 0,
                        heartPoints = entity.content?.substringAfter("heartPoints:")?.trim()?.toIntOrNull() ?: 0
                    )
                } catch (e: Exception) {
                    null
                }
            }
        }
    }

    override suspend fun activityCardExistsForDate(date: LocalDate): Boolean {
        return getActivityCardByDate(date) != null
    }

    override suspend fun insertActivityCard(activityCard: ActivityCard): Long {
        val content = "steps:${activityCard.stepCount}, calories:${activityCard.caloriesBurned}, heartPoints:${activityCard.heartPoints}"
        val entity = JournalEntryEntity(
            id = if (activityCard.id == 0L) 0 else activityCard.id,
            timestamp = activityCard.date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            type = ACTIVITY_TYPE,
            title = "Activity Summary",
            content = content
        )
        return journalDao.insertEntry(entity)
    }

    override suspend fun getActivityCardCount(): Int {
        return journalDao.getEntryCountByType(ACTIVITY_TYPE)
    }
}