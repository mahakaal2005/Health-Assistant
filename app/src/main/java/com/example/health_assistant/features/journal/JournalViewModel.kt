package com.example.health_assistant.features.journal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.health_assistant.features.journal.domain.JournalEntry
import com.example.health_assistant.features.journal.domain.JournalUseCases
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers

/**
 * Simplified ViewModel for journal entries without calendar functionality
 */
@HiltViewModel
class JournalViewModel @Inject constructor(
    private val useCases: JournalUseCases
) : ViewModel() {

    // Filter state
    private val _selectedFilterType = MutableStateFlow(JournalFilterType.ALL)
    val selectedFilterType: StateFlow<JournalFilterType> = _selectedFilterType.asStateFlow()

    // Filter chips state
    private val _filterChips = MutableStateFlow(createInitialFilterChips())
    val filterChips: StateFlow<List<FilterChip>> = _filterChips.asStateFlow()

    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Journal entries based on selected filter
    private val recentEntries = _selectedFilterType.flatMapLatest { filterType ->
        when (filterType) {
            JournalFilterType.ALL -> {
                useCases.getAllEntries()
            }
            else -> {
                val entryTypes = filterType.getEntryTypes()
                if (entryTypes.size == 1) {
                    useCases.getEntriesByType(entryTypes.first())
                } else {
                    // For multiple types, get all entries and filter
                    useCases.getAllEntries().map { entries ->
                        entries.filter { it.type in entryTypes }
                    }
                }
            }
        }
    }.flowOn(Dispatchers.IO)

    val entries: StateFlow<List<JournalEntry>> = recentEntries
        .onStart { _isLoading.value = true }
        .onEach { _isLoading.value = false }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = emptyList()
        )

    private fun createInitialFilterChips(): List<FilterChip> {
        return listOf(
            FilterChip(JournalFilterType.ALL, "All", true),
            FilterChip(JournalFilterType.NOTES, "Notes"),
            FilterChip(JournalFilterType.HEALTH, "Health"),
            FilterChip(JournalFilterType.ACTIVITY, "Activity"),
            FilterChip(JournalFilterType.MOOD, "Mood")
        )
    }

    /**
     * Update selected filter and refresh filter chips
     */
    fun selectFilter(filterType: JournalFilterType) {
        if (_selectedFilterType.value == filterType) return

        _selectedFilterType.value = filterType

        // Update filter chips selection state
        val updatedChips = _filterChips.value.map { chip ->
            chip.copy(isSelected = chip.type == filterType)
        }
        _filterChips.value = updatedChips
    }

    /**
     * Add a new journal entry
     */
    fun addEntry(entry: JournalEntry) {
        viewModelScope.launch {
            try {
                useCases.addEntry(entry)
            } catch (e: Exception) {
                // Handle error - you could add error state here
            }
        }
    }

    /**
     * Delete a journal entry
     */
    fun deleteEntry(entry: JournalEntry) {
        viewModelScope.launch {
            try {
                useCases.deleteEntry(entry)
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
}