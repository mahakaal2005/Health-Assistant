package com.example.health_assistant.features.journal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.health_assistant.features.journal.domain.JournalEntry
import com.example.health_assistant.features.journal.domain.JournalUseCases
import com.example.health_assistant.features.journal.data.JournalEntryEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.example.health_assistant.features.journal.db.toDomain
import com.example.health_assistant.features.journal.db.toEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import java.util.*

/**
 * ViewModel for journal entries that handles the business logic
 */
@HiltViewModel
class JournalViewModel @Inject constructor(
    private val useCases: JournalUseCases
) : ViewModel() {

    private val _filterState = MutableStateFlow<JournalFilter>(JournalFilter.All)
    val filter: StateFlow<JournalFilter> = _filterState.asStateFlow()

    val entries: StateFlow<List<JournalEntry>> = _filterState
        .flatMapLatest { filterValue ->
            when (filterValue) {
                is JournalFilter.All -> useCases.getAllEntries()
                is JournalFilter.ByType -> useCases.getEntriesByType(filterValue.type)
                is JournalFilter.ByTypes -> useCases.getEntriesByTypes(filterValue.types)
                is JournalFilter.ByDate -> {
                    // Convert Date to start/end of day timestamp
                    val calendar = Calendar.getInstance()
                    calendar.time = filterValue.date
                    calendar.set(Calendar.HOUR_OF_DAY, 0)
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.set(Calendar.SECOND, 0)
                    calendar.set(Calendar.MILLISECOND, 0)
                    val startOfDay = calendar.timeInMillis

                    calendar.set(Calendar.HOUR_OF_DAY, 23)
                    calendar.set(Calendar.MINUTE, 59)
                    calendar.set(Calendar.SECOND, 59)
                    calendar.set(Calendar.MILLISECOND, 999)
                    val endOfDay = calendar.timeInMillis

                    useCases.getEntriesByDateRange(startOfDay, endOfDay)
                }
                is JournalFilter.ByDateRange -> {
                    // Convert Date objects to timestamps for the repository method
                    val fromTimestamp = filterValue.from.time
                    val toTimestamp = filterValue.to.time
                    useCases.getEntriesByDateRange(fromTimestamp, toTimestamp)
                }
                is JournalFilter.Search -> useCases.searchEntries(filterValue.query)
            }
        }
        .map<List<JournalEntryEntity>, List<JournalEntry>> { entityList ->
            entityList.map { entity -> entity.toDomain() }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    /**
     * Loading state to indicate when data is being refreshed
     */
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /**
     * Set the filter for journal entries
     */
    fun setFilter(filter: JournalFilter) {
        _filterState.value = filter
    }

    /**
     * Add a new journal entry
     */
    fun addEntry(entry: JournalEntry) {
        viewModelScope.launch {
            useCases.addEntry(entry.toEntity())
        }
    }

    /**
     * Update an existing journal entry
     */
    fun updateEntry(entry: JournalEntry) {
        viewModelScope.launch {
            useCases.updateEntry(entry.toEntity())
        }
    }

    /**
     * Delete a journal entry
     */
    fun deleteEntry(entry: JournalEntry) {
        viewModelScope.launch {
            useCases.deleteEntry(entry.toEntity())
        }
    }

    /**
     * Clear all journal entries
     */
    fun clearAll() {
        viewModelScope.launch {
            useCases.clearAll()
        }
    }

    /**
     * Add sample data for testing
     */
    fun addDummyData() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val oneDayMs = 24 * 60 * 60 * 1000L

            // Add a note entry
            addEntry(JournalEntry(
                id = 0L,
                type = "note",
                timestamp = now,
                content = "Started a new fitness routine today. Feeling good!"
            ))

            // Add a workout entry
            addEntry(JournalEntry(
                id = 0L,
                type = "workout",
                timestamp = now - oneDayMs,
                activityType = "Running",
                duration = 30,
                summary = "5km run in the park"
            ))

            // Add a goal entry
            addEntry(JournalEntry(
                id = 0L,
                type = "goal",
                timestamp = now - 2 * oneDayMs,
                goalTitle = "Drink more water",
                progress = 75f
            ))

            // Add a weight measurement entry
            addEntry(JournalEntry(
                id = 0L,
                type = "weight",
                timestamp = now - 3 * oneDayMs,
                measurementType = "weight",
                value = 75.5f,
                unit = "kg"
            ))

            // Add a heart rate entry
            addEntry(JournalEntry(
                id = 0L,
                type = "heart_rate",
                timestamp = now - 4 * oneDayMs,
                measurementType = "heart_rate",
                value = 72f,
                unit = "bpm"
            ))

            // Add a mood entry
            addEntry(JournalEntry(
                id = 0L,
                type = "mood",
                timestamp = now - 5 * oneDayMs,
                moodLevel = 4,
                emoji = "😊",
                description = "Feeling energetic today"
            ))
        }
    }

    /**
     * Force refresh entries from the database
     */
    fun refreshEntries() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Set the current filter again to trigger a reload
                val currentFilter = _filterState.value
                _filterState.value = JournalFilter.All
                _filterState.value = currentFilter
            } finally {
                _isLoading.value = false
            }
        }
    }

    companion object {
        fun provideFactory(useCases: JournalUseCases): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    @Suppress("UNCHECKED_CAST")
                    return JournalViewModel(useCases) as T
                }
            }
    }
}
