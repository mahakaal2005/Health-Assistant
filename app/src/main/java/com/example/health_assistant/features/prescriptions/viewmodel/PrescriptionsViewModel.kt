package com.example.health_assistant.features.prescriptions.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.health_assistant.auth.session.SessionManager
import com.example.health_assistant.core.util.Result
import com.example.health_assistant.data.model.Prescription
import com.example.health_assistant.data.repository.interfaces.PrescriptionRepository
import com.example.health_assistant.features.prescriptions.utils.PrescriptionUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for managing prescription data and UI state
 * Follows MVVM pattern with StateFlow for reactive UI updates
 */
@HiltViewModel
class PrescriptionsViewModel @Inject constructor(
    private val prescriptionRepository: PrescriptionRepository,
    private val sessionManager: SessionManager,
    private val categoryManager: com.example.health_assistant.data.manager.CategoryManager
) : ViewModel() {

    // Search query for filtering prescriptions
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Simple category filter - just a string, not complex sets
    private val _currentCategoryFilter = MutableStateFlow("All Categories")
    val currentCategoryFilter: StateFlow<String> = _currentCategoryFilter.asStateFlow()

    // UI state for loading, empty states, etc.
    private val _uiState = MutableStateFlow(PrescriptionsUiState())
    val uiState: StateFlow<PrescriptionsUiState> = _uiState.asStateFlow()

    // All prescriptions from repository
    private val _allPrescriptions = MutableStateFlow<List<Prescription>>(emptyList())

    // Get current user ID
    private val currentUserId: String
        get() = sessionManager.getCurrentUserId() ?: ""

    // Simple combined flow - much easier to understand
    val prescriptions: StateFlow<List<Prescription>> = combine(
        _allPrescriptions,
        _searchQuery,
        _currentCategoryFilter
    ) { prescriptions, query, categoryFilter ->
        // Simple filtering logic that extends your existing search
        val filteredPrescriptions = searchPrescriptions(prescriptions, query, categoryFilter)

        // Return prescriptions sorted by date (newest first)
        filteredPrescriptions.sortedByDescending { it.dateAdded }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        loadPrescriptions()
    }

    /**
     * Load prescriptions for current user
     */
    private fun loadPrescriptions() {
        viewModelScope.launch {
            val userId = currentUserId
            android.util.Log.d("PrescriptionsViewModel", "Loading prescriptions for user: $userId")

            if (userId.isNotEmpty()) {
                updateUiState { copy(isLoading = true) }

                prescriptionRepository.getAllPrescriptions().collect { result ->
                    when (result) {
                        is Result.Success -> {
                            android.util.Log.d("PrescriptionsViewModel", "Loaded ${result.data.size} prescriptions")
                            // Filter prescriptions by current user if userId is set in prescription
                            val userPrescriptions = if (userId.isNotEmpty()) {
                                result.data.filter { it.userId == userId || it.userId.isNullOrEmpty() }
                            } else {
                                result.data
                            }
                            _allPrescriptions.value = userPrescriptions
                            updateUiState {
                                copy(
                                    isLoading = false,
                                    errorMessage = null,
                                    isEmpty = userPrescriptions.isEmpty()
                                )
                            }
                        }
                        is Result.Error -> {
                            android.util.Log.e("PrescriptionsViewModel", "Error loading prescriptions: ${result.message}")
                            updateUiState {
                                copy(
                                    isLoading = false,
                                    errorMessage = result.message
                                )
                            }
                        }
                        is Result.Loading -> {
                            android.util.Log.d("PrescriptionsViewModel", "Loading prescriptions...")
                            updateUiState { copy(isLoading = true) }
                        }
                    }
                }
            } else {
                android.util.Log.w("PrescriptionsViewModel", "No user ID available")
                updateUiState {
                    copy(
                        isLoading = false,
                        errorMessage = "User not logged in"
                    )
                }
            }
        }
    }

    /**
     * Update search query
     */
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    /**
     * Delete a prescription
     */
    fun deletePrescription(prescriptionId: String) {
        viewModelScope.launch {
            updateUiState { copy(isLoading = true) }

            val result = prescriptionRepository.deletePrescription(prescriptionId.toLong())
            when (result) {
                is Result.Success -> {
                    updateUiState {
                        copy(
                            isLoading = false,
                            successMessage = "Prescription deleted successfully"
                        )
                    }
                }
                is Result.Error -> {
                    updateUiState {
                        copy(
                            isLoading = false,
                            errorMessage = result.message
                        )
                    }
                }
                is Result.Loading -> {
                    // Keep loading state
                }
            }
        }
    }

    /**
     * Refresh prescriptions
     */
    fun refreshPrescriptions() {
        loadPrescriptions()
    }

    /**
     * Get prescription by ID - Fixed return type and error handling
     */
    suspend fun getPrescriptionById(prescriptionId: String): Prescription? {
        return when (val result = prescriptionRepository.getPrescriptionById(prescriptionId.toLong())) {
            is Result.Success -> result.data
            is Result.Error -> null
            is Result.Loading -> null
        }
    }

    /**
     * Update an existing prescription
     */
    fun updatePrescription(prescription: Prescription) {
        viewModelScope.launch {
            updateUiState { copy(isLoading = true) }

            val result = prescriptionRepository.updatePrescription(prescription)
            when (result) {
                is Result.Success -> {
                    updateUiState {
                        copy(
                            isLoading = false,
                            successMessage = "Prescription updated successfully"
                        )
                    }
                }
                is Result.Error -> {
                    updateUiState {
                        copy(
                            isLoading = false,
                            errorMessage = result.message
                        )
                    }
                }
                is Result.Loading -> {
                    // Keep loading state
                }
            }
        }
    }

    /**
     * Update current category filter
     */
    fun updateCategoryFilter(category: String) {
        _currentCategoryFilter.value = category
    }

    /**
     * Clear category filter
     */
    fun clearCategoryFilter() {
        _currentCategoryFilter.value = "All Categories"
    }

    /**
     * Simplified search function - just filters by query and category
     */
    private fun searchPrescriptions(
        prescriptions: List<Prescription>,
        query: String,
        categoryFilter: String
    ): List<Prescription> {
        // Get default category names for "Other" filtering
        val defaultCategoryNames = com.example.health_assistant.data.model.DiseaseCategory.getDefaultCategories()
            .map { it.name }
            .filter { it != "Other" } // Exclude "Other" itself from the default names
        
        return prescriptions.filter { prescription ->
            // Text search across multiple fields
            val matchesQuery = query.isBlank() ||
                prescription.medicationName.contains(query, ignoreCase = true) ||
                prescription.doctorName?.contains(query, ignoreCase = true) == true ||
                prescription.instructions?.contains(query, ignoreCase = true) == true ||
                prescription.notes?.contains(query, ignoreCase = true) == true ||
                prescription.frequency.contains(query, ignoreCase = true)

            // Category filter - handle "All Categories", "Other", and specific categories
            val matchesCategory = when (categoryFilter) {
                "All Categories" -> true
                "Other" -> {
                    // For "Other" filter, match prescriptions that don't match any default category
                    val category = PrescriptionUtils.getCategoryById(prescription.categoryId)
                    category == null || !defaultCategoryNames.contains(category.name)
                }
                else -> {
                    // For specific category filter, match by category name
                    val category = PrescriptionUtils.getCategoryById(prescription.categoryId)
                    category?.name == categoryFilter
                }
            }

            matchesQuery && matchesCategory
        }
    }

    /**
     * Helper function to update UI state
     */
    private fun updateUiState(update: PrescriptionsUiState.() -> PrescriptionsUiState) {
        _uiState.value = _uiState.value.update()
    }

    /**
     * Clear error message
     */
    fun clearErrorMessage() {
        updateUiState { copy(errorMessage = null) }
    }

    /**
     * Clear success message
     */
    fun clearSuccessMessage() {
        updateUiState { copy(successMessage = null) }
    }
}

/**
 * UI state for prescriptions screen
 */
data class PrescriptionsUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val isEmpty: Boolean = false
)