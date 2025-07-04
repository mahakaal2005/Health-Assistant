package com.example.health_assistant.features.journal.data

import com.example.health_assistant.features.journal.domain.JournalEntry
import com.example.health_assistant.features.journal.domain.JournalRepository
import com.example.health_assistant.features.journal.db.toDomain
import com.example.health_assistant.features.journal.db.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of JournalRepository
 * Handles data operations and converts between entity and domain models
 */
@Singleton
class JournalRepositoryImpl @Inject constructor(
    private val dao: JournalEntryDao
) : JournalRepository {

    override fun getAllEntries(): Flow<List<JournalEntry>> {
        return dao.getAllEntries().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getEntriesByType(type: String): Flow<List<JournalEntry>> {
        return dao.getEntriesByType(type).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getEntriesByDateRange(startTime: Long, endTime: Long): Flow<List<JournalEntry>> {
        return dao.getEntriesByDateRange(startTime, endTime).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getRecentEntries(limit: Int): Flow<List<JournalEntry>> {
        return dao.getRecentEntries(limit).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getEntryById(id: Long): JournalEntry? {
        return dao.getEntryById(id)?.toDomain()
    }

    override suspend fun insertEntry(entry: JournalEntry): Long {
        return dao.insertEntry(entry.toEntity())
    }

    override suspend fun updateEntry(entry: JournalEntry) {
        dao.updateEntry(entry.toEntity())
    }

    override suspend fun deleteEntry(entry: JournalEntry) {
        dao.deleteEntry(entry.toEntity())
    }

    override suspend fun deleteEntryById(id: Long) {
        dao.deleteEntryById(id)
    }

    override suspend fun getAllTypes(): List<String> {
        return dao.getAllTypes()
    }

    override suspend fun getEntryCount(): Int {
        return dao.getEntryCount()
    }
}