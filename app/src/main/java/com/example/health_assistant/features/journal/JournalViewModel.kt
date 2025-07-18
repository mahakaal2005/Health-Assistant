package com.example.health_assistant.features.journal

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.health_assistant.auth.session.SessionManager
import com.example.health_assistant.features.journal.domain.JournalEntry
import com.example.health_assistant.features.journal.domain.JournalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Journal functionality
 * Manages journal entries and user interactions
 * Now with proper user isolation for multi-user support
 */
@HiltViewModel
class JournalViewModel @Inject constructor(
    private val journalRepository: JournalRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val TAG = "JournalViewModel"

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Journal entries filtered by current user
    val entries: Flow<List<JournalEntry>> = journalRepository.getAllEntries()
        .catch { e ->
            Log.e(TAG, "Error getting journal entries", e)
            emit(emptyList())
        }
        .map { entries ->
            // Filter entries for the current user only
            val currentUserId = sessionManager.getCurrentUserId() ?: ""
            if (currentUserId.isNotEmpty()) {
                entries.filter { it.userId == currentUserId }
            } else {
                // If no user is logged in, return empty list
                emptyList()
            }
        }

    init {
        // Monitor user changes to refresh data
        monitorUserChanges()
    }

    /**
     * Monitor user changes to refresh data when user changes
     */
    private fun monitorUserChanges() {
        viewModelScope.launch {
            try {
                // This is a simplified approach - in a real app, you would observe a Flow from SessionManager
                val currentUserId = sessionManager.getCurrentUserId()
                Log.d(TAG, "Current user ID: $currentUserId")
            } catch (e: Exception) {
                Log.e(TAG, "Error monitoring user changes", e)
            }
        }
    }

    /**
     * Add a new journal entry
     */
    fun addEntry(entry: JournalEntry) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Ensure entry has the current user ID
                val currentUserId = sessionManager.getCurrentUserId() ?: ""
                val entryWithUserId = if (entry.userId.isEmpty() && currentUserId.isNotEmpty()) {
                    // Create a new entry with the current user ID
                    when (entry) {
                        is JournalEntry.Generic -> entry.copy(userId = currentUserId)
                        is JournalEntry.Weight -> entry.copy(userId = currentUserId)
                        is JournalEntry.BloodPressure -> entry.copy(userId = currentUserId)
                        is JournalEntry.Workout -> entry.copy(userId = currentUserId)
                        is JournalEntry.Mood -> entry.copy(userId = currentUserId)
                        else -> entry
                    }
                } else {
                    entry
                }
                
                journalRepository.insertEntry(entryWithUserId)
                Log.d(TAG, "Added journal entry for user $currentUserId")
            } catch (e: Exception) {
                Log.e(TAG, "Error adding journal entry", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Update an existing journal entry
     */
    fun updateEntry(entry: JournalEntry) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Ensure entry has the current user ID
                val currentUserId = sessionManager.getCurrentUserId() ?: ""
                val entryWithUserId = if (entry.userId.isEmpty() && currentUserId.isNotEmpty()) {
                    // Create a new entry with the current user ID
                    when (entry) {
                        is JournalEntry.Generic -> entry.copy(userId = currentUserId)
                        is JournalEntry.Weight -> entry.copy(userId = currentUserId)
                        is JournalEntry.BloodPressure -> entry.copy(userId = currentUserId)
                        is JournalEntry.Workout -> entry.copy(userId = currentUserId)
                        is JournalEntry.Mood -> entry.copy(userId = currentUserId)
                        else -> entry
                    }
                } else {
                    entry
                }
                
                journalRepository.updateEntry(entryWithUserId)
                Log.d(TAG, "Updated journal entry for user $currentUserId")
            } catch (e: Exception) {
                Log.e(TAG, "Error updating journal entry", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Delete a journal entry
     */
    fun deleteEntry(entry: JournalEntry) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                journalRepository.deleteEntry(entry)
                Log.d(TAG, "Deleted journal entry ID: ${entry.id}")
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting journal entry", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Get entry by ID
     */
    suspend fun getEntryById(id: Long): JournalEntry? {
        return try {
            journalRepository.getEntryById(id)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting journal entry by ID: $id", e)
            null
        }
    }
}