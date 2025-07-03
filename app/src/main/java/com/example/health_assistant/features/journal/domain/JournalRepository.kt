package com.example.health_assistant.features.journal.domain

import com.example.health_assistant.features.journal.data.JournalEntryDao
import com.example.health_assistant.features.journal.data.JournalEntryEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class JournalRepository @Inject constructor(
    private val dao: JournalEntryDao
) {
    fun getAllEntries(): Flow<List<JournalEntryEntity>> = dao.getAllEntries()
    fun getEntriesByType(type: String): Flow<List<JournalEntryEntity>> = dao.getEntriesByType(type)
    fun getEntriesByTypes(types: List<String>): Flow<List<JournalEntryEntity>> = dao.getEntriesByTypes(types)
    fun getEntriesByDateRange(start: Long, end: Long): Flow<List<JournalEntryEntity>> = dao.getEntriesByDateRange(start, end)
    fun searchEntries(query: String): Flow<List<JournalEntryEntity>> = dao.searchEntries(query)
    suspend fun insert(entry: JournalEntryEntity) = dao.insert(entry)
    suspend fun update(entry: JournalEntryEntity) = dao.update(entry)
    suspend fun delete(entry: JournalEntryEntity) = dao.delete(entry)
    suspend fun clearAll() = dao.clearAll()
}
