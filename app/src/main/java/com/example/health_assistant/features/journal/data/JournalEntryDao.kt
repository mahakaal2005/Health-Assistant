package com.example.health_assistant.features.journal.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface JournalEntryDao {
    @Query("SELECT * FROM journal_entries ORDER BY timestamp DESC")
    fun getAllEntries(): Flow<List<JournalEntryEntity>>

    @Query("SELECT * FROM journal_entries WHERE id = :id")
    suspend fun getEntryById(id: Long): JournalEntryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: JournalEntryEntity): Long

    @Update
    suspend fun updateEntry(entry: JournalEntryEntity)

    @Delete
    suspend fun deleteEntry(entry: JournalEntryEntity)

    @Query("SELECT * FROM journal_entries WHERE content LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%'")
    fun searchEntries(query: String): Flow<List<JournalEntryEntity>>

    @Query("SELECT * FROM journal_entries WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    fun getEntriesByDateRange(startTime: Long, endTime: Long): Flow<List<JournalEntryEntity>>

    @Query("SELECT * FROM journal_entries WHERE moodLevel = :moodLevel ORDER BY timestamp DESC")
    fun getEntriesByMood(moodLevel: Int): Flow<List<JournalEntryEntity>>

    @Query("SELECT * FROM journal_entries WHERE type = :type ORDER BY timestamp DESC")
    fun getEntriesByType(type: String): Flow<List<JournalEntryEntity>>
}
