package com.example.health_assistant.features.journal.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.example.health_assistant.features.journal.data.JournalEntryEntity

@Dao
interface JournalDao {
    @Query("SELECT * FROM journal_entries ORDER BY timestamp DESC")
    fun getAllEntries(): Flow<List<JournalEntryEntity>>

    @Query("SELECT * FROM journal_entries WHERE type = :type ORDER BY timestamp DESC")
    fun getEntriesByType(type: String): Flow<List<JournalEntryEntity>>

    @Query("SELECT * FROM journal_entries WHERE timestamp BETWEEN :start AND :end ORDER BY timestamp DESC")
    fun getEntriesByDateRange(start: Long, end: Long): Flow<List<JournalEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: JournalEntryEntity): Long

    @Update
    suspend fun update(entry: JournalEntryEntity)

    @Delete
    suspend fun delete(entry: JournalEntryEntity)

    @Query("DELETE FROM journal_entries")
    suspend fun clearAll()
}
