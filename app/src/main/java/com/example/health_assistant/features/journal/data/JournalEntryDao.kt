package com.example.health_assistant.features.journal.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for journal entries
 * Provides database operations for journal entries
 */
@Dao
interface JournalEntryDao {

    @Query("SELECT * FROM journal_entries ORDER BY timestamp DESC")
    fun getAllEntries(): Flow<List<JournalEntryEntity>>

    @Query("SELECT * FROM journal_entries WHERE userId = :userId ORDER BY timestamp DESC")
    fun getAllEntriesByUserId(userId: String): Flow<List<JournalEntryEntity>>

    @Query("SELECT * FROM journal_entries WHERE type = :type ORDER BY timestamp DESC")
    fun getEntriesByType(type: String): Flow<List<JournalEntryEntity>>

    @Query("SELECT * FROM journal_entries WHERE type = :type AND userId = :userId ORDER BY timestamp DESC")
    fun getEntriesByTypeAndUserId(type: String, userId: String): Flow<List<JournalEntryEntity>>

    @Query("SELECT * FROM journal_entries WHERE timestamp BETWEEN :startTimestamp AND :endTimestamp ORDER BY timestamp DESC")
    fun getEntriesByDateRange(startTimestamp: Long, endTimestamp: Long): Flow<List<JournalEntryEntity>>

    @Query("SELECT * FROM journal_entries WHERE timestamp BETWEEN :startTimestamp AND :endTimestamp AND userId = :userId ORDER BY timestamp DESC")
    fun getEntriesByDateRangeAndUserId(startTimestamp: Long, endTimestamp: Long, userId: String): Flow<List<JournalEntryEntity>>

    @Query("SELECT * FROM journal_entries WHERE type = :type AND timestamp BETWEEN :startTimestamp AND :endTimestamp ORDER BY timestamp DESC")
    fun getEntriesByTypeAndDateRange(type: String, startTimestamp: Long, endTimestamp: Long): Flow<List<JournalEntryEntity>>

    @Query("SELECT * FROM journal_entries WHERE type = :type AND timestamp BETWEEN :startTimestamp AND :endTimestamp AND userId = :userId ORDER BY timestamp DESC")
    fun getEntriesByTypeAndDateRangeAndUserId(type: String, startTimestamp: Long, endTimestamp: Long, userId: String): Flow<List<JournalEntryEntity>>

    @Query("SELECT * FROM journal_entries WHERE id = :id")
    suspend fun getEntryById(id: Long): JournalEntryEntity?

    @Query("SELECT * FROM journal_entries ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentEntries(limit: Int): Flow<List<JournalEntryEntity>>

    @Query("SELECT * FROM journal_entries WHERE userId = :userId ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentEntriesByUserId(userId: String, limit: Int): Flow<List<JournalEntryEntity>>

    @Query("SELECT * FROM journal_entries WHERE type = :type ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentEntriesByType(type: String, limit: Int): Flow<List<JournalEntryEntity>>

    @Query("SELECT * FROM journal_entries WHERE type = :type AND userId = :userId ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentEntriesByTypeAndUserId(type: String, userId: String, limit: Int): Flow<List<JournalEntryEntity>>

    @Query("SELECT DISTINCT type FROM journal_entries ORDER BY type")
    suspend fun getAllTypes(): List<String>

    @Query("SELECT DISTINCT type FROM journal_entries WHERE userId = :userId ORDER BY type")
    suspend fun getAllTypesByUserId(userId: String): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: JournalEntryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntries(entries: List<JournalEntryEntity>): List<Long>

    @Update
    suspend fun updateEntry(entry: JournalEntryEntity)

    @Delete
    suspend fun deleteEntry(entry: JournalEntryEntity)

    @Query("DELETE FROM journal_entries WHERE id = :entryId AND userId = :userId")
    suspend fun deleteEntryByIdAndUserId(entryId: Long, userId: String)
    
    @Query("SELECT COUNT(*) FROM journal_entries WHERE userId = :userId")
    suspend fun getEntryCountByUserId(userId: String): Int

    @Query("SELECT COUNT(*) FROM journal_entries WHERE type = :type AND userId = :userId")
    suspend fun getEntryCountByTypeAndUserId(type: String, userId: String): Int
}