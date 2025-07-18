package com.example.health_assistant.features.journal.data

import com.example.health_assistant.auth.session.SessionManager
import com.example.health_assistant.features.journal.domain.JournalEntry
import com.example.health_assistant.features.journal.domain.JournalRepository
import com.example.health_assistant.features.journal.db.toDomain
import com.example.health_assistant.features.journal.db.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import android.util.Log

/**
 * Implementation of JournalRepository
 * Handles data operations and converts between entity and domain models
 * Now with proper user isolation for multi-user support
 */
@Singleton
class JournalRepositoryImpl @Inject constructor(
    private val dao: JournalEntryDao,
    private val sessionManager: SessionManager
) : JournalRepository {

    private fun getCurrentUserId(): String {
        return sessionManager.getCurrentUserId() ?: ""
    }

    override fun getAllEntries(): Flow<List<JournalEntry>> {
        val userId = getCurrentUserId()
        return if (userId.isNotEmpty()) {
            dao.getAllEntriesByUserId(userId).map { entities ->
                entities.map { it.toDomain() }
            }
        } else {
            dao.getAllEntries().map { entities ->
                entities.map { it.toDomain() }
            }
        }
    }

    override fun getEntriesByType(type: String): Flow<List<JournalEntry>> {
        val userId = getCurrentUserId()
        return if (userId.isNotEmpty()) {
            dao.getEntriesByTypeAndUserId(type, userId).map { entities ->
                entities.map { it.toDomain() }
            }
        } else {
            dao.getEntriesByType(type).map { entities ->
                entities.map { it.toDomain() }
            }
        }
    }

    override fun getEntriesByDateRange(startTime: Long, endTime: Long): Flow<List<JournalEntry>> {
        val userId = getCurrentUserId()
        return if (userId.isNotEmpty()) {
            dao.getEntriesByDateRangeAndUserId(startTime, endTime, userId).map { entities ->
                entities.map { it.toDomain() }
            }
        } else {
            dao.getEntriesByDateRange(startTime, endTime).map { entities ->
                entities.map { it.toDomain() }
            }
        }
    }

    override fun getRecentEntries(limit: Int): Flow<List<JournalEntry>> {
        val userId = getCurrentUserId()
        return if (userId.isNotEmpty()) {
            dao.getRecentEntriesByUserId(userId, limit).map { entities ->
                entities.map { it.toDomain() }
            }
        } else {
            dao.getRecentEntries(limit).map { entities ->
                entities.map { it.toDomain() }
            }
        }
    }

    override suspend fun getEntryById(id: Long): JournalEntry? {
        val entity = dao.getEntryById(id)
        
        // Only return the entry if it belongs to the current user or if no user is logged in
        val userId = getCurrentUserId()
        return if (userId.isNotEmpty() && entity != null) {
            if (entity.userId == userId) entity.toDomain() else null
        } else {
            entity?.toDomain()
        }
    }

    override suspend fun insertEntry(entry: JournalEntry): Long {
        val userId = getCurrentUserId()
        // Convert to entity and set the userId
        val entity = entry.toEntity().copy(userId = userId)
        return dao.insertEntry(entity)
    }

    override suspend fun updateEntry(entry: JournalEntry) {
        val userId = getCurrentUserId()
        // Convert to entity and ensure the userId is set
        val entity = entry.toEntity().copy(userId = userId)
        dao.updateEntry(entity)
    }

    override suspend fun deleteEntry(entry: JournalEntry) {
        try {
            val userId = getCurrentUserId()
            if (userId.isNotEmpty()) {
                // Check if the entry belongs to the current user
                if (entry.userId.isEmpty() || entry.userId == userId) {
                    dao.deleteEntryByIdAndUserId(entry.id, userId)
                }
            } else {
                // If no user ID, just delete by ID
                dao.deleteEntryByIdAndUserId(entry.id, "")
            }
        } catch (e: Exception) {
            Log.e("JournalRepo", "Error deleting entry", e)
        }
    }

    override suspend fun deleteEntryById(id: Long) {
        // Delete entry by ID and user ID for proper user isolation
        val userId = getCurrentUserId()
        try {
            dao.deleteEntryByIdAndUserId(id, userId)
            Log.d("JournalRepo", "Deleted entry with ID $id for user $userId")
        } catch (e: Exception) {
            Log.e("JournalRepo", "Error deleting entry with ID $id", e)
        }
    }

    override suspend fun getAllTypes(): List<String> {
        val userId = getCurrentUserId()
        return if (userId.isNotEmpty()) {
            dao.getAllTypesByUserId(userId)
        } else {
            dao.getAllTypes()
        }
    }

    override suspend fun getEntryCount(): Int {
        val userId = getCurrentUserId()
        return if (userId.isNotEmpty()) {
            dao.getEntryCountByUserId(userId)
        } else {
            0 // Default to 0 for safety if no user is logged in
        }
    }
}