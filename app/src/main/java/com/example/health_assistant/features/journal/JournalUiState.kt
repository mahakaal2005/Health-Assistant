package com.example.health_assistant.features.journal

/**
 * UI state for the Journal fragment
 */
data class JournalUiState(
    val isLoading: Boolean = false,
    val isEmpty: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)