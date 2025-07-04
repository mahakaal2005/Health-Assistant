package com.example.health_assistant.features.journal.domain

import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for journal entries
 * Defines the contract for journal data operations
 */
interface JournalRepository {

    fun getAllEntries(): Flow<List<JournalEntry>>

    fun getEntriesByType(type: String): Flow<List<JournalEntry>>

    fun getEntriesByDateRange(startTime: Long, endTime: Long): Flow<List<JournalEntry>>

    fun getRecentEntries(limit: Int): Flow<List<JournalEntry>>

    suspend fun getEntryById(id: Long): JournalEntry?

    suspend fun insertEntry(entry: JournalEntry): Long

    suspend fun updateEntry(entry: JournalEntry)

    suspend fun deleteEntry(entry: JournalEntry)

    suspend fun deleteEntryById(id: Long)

    suspend fun getAllTypes(): List<String>

    suspend fun getEntryCount(): Int
}