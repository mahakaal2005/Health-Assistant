package com.example.health_assistant.features.journal

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.health_assistant.auth.session.SessionManager
import com.example.health_assistant.features.journal.domain.ActivityCardRepository
import com.example.health_assistant.features.journal.domain.JournalEntry
import com.example.health_assistant.features.journal.domain.JournalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/**
 * ViewModel for Journal functionality
 * Manages journal entries and user interactions
 * Now with proper user isolation for multi-user support
 */
@HiltViewModel
class JournalViewModel @Inject constructor(
    private val journalRepository: JournalRepository,
    private val sessionManager: SessionManager,
    private val activityCardRepository: ActivityCardRepository
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
     * Get current user ID
     */
    fun getCurrentUserId(): String {
        return sessionManager.getCurrentUserId() ?: ""
    }
    
    /**
     * Clean up duplicate activity cards for a specific date and user
     */
    suspend fun cleanupDuplicateActivityCards(date: LocalDate, userId: String): Int {
        return try {
            activityCardRepository.cleanupDuplicateActivityCards(date, userId)
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up duplicate activity cards", e)
            0
        }
    }

    /**
     * Clean up all duplicate activity cards - SAFE VERSION
     * This method is disabled to prevent UI freezing
     * Use the database-level cleanup in HealthAssistantApplication instead
     */
    fun cleanupAllDuplicateActivityCards() {
        Log.d(TAG, "Cleanup method disabled to prevent UI freezing. Database cleanup runs on app startup.")
        // Method disabled to prevent UI blocking
        // The cleanup is handled by HealthAssistantApplication on startup
    }

    /**
     * Extract LocalDate from a JournalEntry
     */
    private fun extractDateFromEntry(entry: JournalEntry): LocalDate? {
        return when (entry) {
            is JournalEntry.Workout -> {
                try {
                    // Convert timestamp to LocalDate
                    val instant = java.time.Instant.ofEpochMilli(entry.timestamp)
                    instant.atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                } catch (e: Exception) {
                    null
                }
            }
            is JournalEntry.Generic -> {
                try {
                    // Try to parse timestamp to date
                    val instant = java.time.Instant.ofEpochMilli(entry.timestamp)
                    instant.atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                } catch (e: Exception) {
                    null
                }
            }
            else -> null
        }
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

    /**
     * Test method to generate a sample activity card for today
     * This creates a card with realistic sample data for testing
     */
    fun generateTestActivityCard() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val currentUserId = getCurrentUserId()
                if (currentUserId.isEmpty()) {
                    Log.w(TAG, "No user logged in, cannot generate test activity card")
                    return@launch
                }

                // Generate realistic sample data
                val sampleSteps = (5000..12000).random()
                val sampleCalories = (200..600).random()
                val sampleHeartPoints = (10..50).random()

                // Create a test activity card entry
                val testEntry = JournalEntry.Generic(
                    id = 0,
                    timestamp = System.currentTimeMillis(),
                    type = "activity_summary",
                    content = """{"stepCount":$sampleSteps,"caloriesBurned":$sampleCalories,"heartPoints":$sampleHeartPoints}""",
                    userId = currentUserId
                )

                // Add the test entry
                journalRepository.insertEntry(testEntry)
                
                Log.d(TAG, "Generated test activity card for user $currentUserId with Steps: $sampleSteps, Calories: $sampleCalories, Heart Points: $sampleHeartPoints")
            } catch (e: Exception) {
                Log.e(TAG, "Error generating test activity card", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
}