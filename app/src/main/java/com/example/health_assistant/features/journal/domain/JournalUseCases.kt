package com.example.health_assistant.features.journal.domain

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use cases for journal operations
 * Encapsulates business logic for journal entries
 */
data class JournalUseCases @Inject constructor(
    val getAllEntries: GetAllEntriesUseCase,
    val getEntriesByType: GetEntriesByTypeUseCase,
    val getRecentEntries: GetRecentEntriesUseCase,
    val addEntry: AddEntryUseCase,
    val updateEntry: UpdateEntryUseCase,
    val deleteEntry: DeleteEntryUseCase,
    val getEntryById: GetEntryByIdUseCase,
    val getEntriesByDateRange: GetEntriesByDateRangeUseCase
)

class GetAllEntriesUseCase @Inject constructor(
    private val repository: JournalRepository
) {
    operator fun invoke(): Flow<List<JournalEntry>> {
        return repository.getAllEntries()
    }
}

class GetEntriesByTypeUseCase @Inject constructor(
    private val repository: JournalRepository
) {
    operator fun invoke(type: String): Flow<List<JournalEntry>> {
        return repository.getEntriesByType(type)
    }
}

class GetRecentEntriesUseCase @Inject constructor(
    private val repository: JournalRepository
) {
    operator fun invoke(limit: Int = 20): Flow<List<JournalEntry>> {
        return repository.getRecentEntries(limit)
    }
}

class AddEntryUseCase @Inject constructor(
    private val repository: JournalRepository
) {
    suspend operator fun invoke(entry: JournalEntry): Result<Long> {
        return try {
            val id = repository.insertEntry(entry)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class UpdateEntryUseCase @Inject constructor(
    private val repository: JournalRepository
) {
    suspend operator fun invoke(entry: JournalEntry): Result<Unit> {
        return try {
            repository.updateEntry(entry)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class DeleteEntryUseCase @Inject constructor(
    private val repository: JournalRepository
) {
    suspend operator fun invoke(entry: JournalEntry): Result<Unit> {
        return try {
            repository.deleteEntry(entry)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class GetEntryByIdUseCase @Inject constructor(
    private val repository: JournalRepository
) {
    suspend operator fun invoke(id: Long): JournalEntry? {
        return repository.getEntryById(id)
    }
}

class GetEntriesByDateRangeUseCase @Inject constructor(
    private val repository: JournalRepository
) {
    operator fun invoke(startTime: Long, endTime: Long): Flow<List<JournalEntry>> {
        return repository.getEntriesByDateRange(startTime, endTime)
    }
}