package com.example.health_assistant.features.journal.data

import com.example.health_assistant.features.journal.domain.ActivityCard
import com.example.health_assistant.features.journal.domain.ActivityCardRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Simplified implementation of ActivityCardRepository using existing journal system
 * Only handles the 3 essential metrics: steps, calories, heart rate
 */
@Singleton
class ActivityCardRepositoryImpl @Inject constructor(
    private val journalDao: JournalEntryDao
) : ActivityCardRepository {

    companion object {
        private const val ACTIVITY_TYPE = ActivityCardMapper.ACTIVITY_CARD_TYPE
    }

    override fun getAllActivityCards(): Flow<List<ActivityCard>> {
        return journalDao.getEntriesByType(ACTIVITY_TYPE).map { entities ->
            entities.mapNotNull { ActivityCardMapper.toActivityCard(it) }
        }
    }

    override suspend fun getActivityCardByDate(date: LocalDate): ActivityCard? {
        val startOfDay = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endOfDay = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1

        val entries = journalDao.getEntriesByTypeAndDateRange(ACTIVITY_TYPE, startOfDay, endOfDay)

        // Convert Flow to list and get first entry
        var result: ActivityCard? = null
        entries.collect { entryList ->
            result = entryList.firstOrNull()?.let { ActivityCardMapper.toActivityCard(it) }
        }
        return result
    }

    override fun getActivityCardsByDateRange(startDate: LocalDate, endDate: LocalDate): Flow<List<ActivityCard>> {
        val startTimestamp = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endTimestamp = endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1

        return journalDao.getEntriesByTypeAndDateRange(ACTIVITY_TYPE, startTimestamp, endTimestamp).map { entities ->
            entities.mapNotNull { ActivityCardMapper.toActivityCard(it) }
        }
    }

    override fun getRecentActivityCards(limit: Int): Flow<List<ActivityCard>> {
        return journalDao.getEntriesByType(ACTIVITY_TYPE).map { entities ->
            entities.take(limit).mapNotNull { ActivityCardMapper.toActivityCard(it) }
        }
    }

    override suspend fun activityCardExistsForDate(date: LocalDate): Boolean {
        return getActivityCardByDate(date) != null
    }

    override suspend fun insertActivityCard(activityCard: ActivityCard): Long {
        val journalEntry = ActivityCardMapper.toJournalEntry(activityCard)
        return journalDao.insertEntry(journalEntry)
    }

    override suspend fun getActivityCardCount(): Int {
        return journalDao.getEntryCountByType(ACTIVITY_TYPE)
    }
}