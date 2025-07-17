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

    @Query("SELECT * FROM journal_entries WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    fun getEntriesByDateRange(startTime: Long, endTime: Long): Flow<List<JournalEntryEntity>>

    @Query("SELECT * FROM journal_entries WHERE timestamp >= :startTime AND timestamp <= :endTime AND userId = :userId ORDER BY timestamp DESC")
    fun getEntriesByDateRangeAndUserId(startTime: Long, endTime: Long, userId: String): Flow<List<JournalEntryEntity>>

    @Query("SELECT * FROM journal_entries WHERE type = :type AND timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    fun getEntriesByTypeAndDateRange(type: String, startTime: Long, endTime: Long): Flow<List<JournalEntryEntity>>

    @Query("SELECT * FROM journal_entries WHERE type = :type AND timestamp >= :startTime AND timestamp <= :endTime AND userId = :userId ORDER BY timestamp DESC")
    fun getEntriesByTypeAndDateRangeAndUserId(type: String, startTime: Long, endTime: Long, userId: String): Flow<List<JournalEntryEntity>>

    @Query("SELECT * FROM journal_entries WHERE id = :id")
    suspend fun getEntryById(id: Long): JournalEntryEntity?

    @Query("SELECT * FROM journal_entries ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentEntries(limit: Int): Flow<List<JournalEntryEntity>>

    @Query("SELECT * FROM journal_entries WHERE userId = :userId ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentEntriesByUserId(userId: String, limit: Int): Flow<List<JournalEntryEntity>>

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

    @Query("DELETE FROM journal_entries WHERE id = :id")
    suspend fun deleteEntryById(id: Long)
    
    @Query("SELECT COUNT(*) FROM journal_entries WHERE userId = :userId")
    suspend fun getEntryCountByUserId(userId: String): Int
}