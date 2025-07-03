package com.example.health_assistant.features.journal.domain

import com.example.health_assistant.features.journal.data.JournalEntryEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class JournalUseCases @Inject constructor(
    private val repository: JournalRepository
) {
    fun getAllEntries(): Flow<List<JournalEntryEntity>> = repository.getAllEntries()
    fun getEntriesByType(type: String): Flow<List<JournalEntryEntity>> = repository.getEntriesByType(type)
    fun getEntriesByTypes(types: List<String>): Flow<List<JournalEntryEntity>> = repository.getEntriesByTypes(types)
    fun getEntriesByDateRange(start: Long, end: Long): Flow<List<JournalEntryEntity>> = repository.getEntriesByDateRange(start, end)
    fun searchEntries(query: String): Flow<List<JournalEntryEntity>> = repository.searchEntries(query)
    suspend fun addEntry(entry: JournalEntryEntity) = repository.insert(entry)
    suspend fun updateEntry(entry: JournalEntryEntity) = repository.update(entry)
    suspend fun deleteEntry(entry: JournalEntryEntity) = repository.delete(entry)
    suspend fun clearAll() = repository.clearAll()
}
