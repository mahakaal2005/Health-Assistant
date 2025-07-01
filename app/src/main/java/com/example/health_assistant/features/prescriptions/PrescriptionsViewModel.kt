package com.example.health_assistant.features.prescriptions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.health_assistant.auth.session.SessionManager
import com.example.health_assistant.core.util.Result
import com.example.health_assistant.data.model.Prescription
import com.example.health_assistant.data.repository.interfaces.PrescriptionRepository
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
    private val sessionManager: SessionManager
) : ViewModel() {

    // Search query for filtering prescriptions
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // UI state for loading, empty states, etc.
    private val _uiState = MutableStateFlow(PrescriptionsUiState())
    val uiState: StateFlow<PrescriptionsUiState> = _uiState.asStateFlow()

    // All prescriptions from repository
    private val _allPrescriptions = MutableStateFlow<List<Prescription>>(emptyList())

    // Get current user ID
    private val currentUserId: String
        get() = sessionManager.getCurrentUserId() ?: ""

    // Combined flow of prescriptions with search filtering - simplified for grid layout
    val prescriptions: StateFlow<List<Prescription>> = combine(
        _allPrescriptions,
        _searchQuery
    ) { prescriptions, query ->
        val filteredPrescriptions = if (query.isBlank()) {
            prescriptions
        } else {
            searchByDoctorName(prescriptions, query)
        }

        // Return prescriptions sorted by date (newest first) - no grouping needed for grid
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

                prescriptionRepository.getAllPrescriptions(userId).collect { result ->
                    when (result) {
                        is Result.Success -> {
                            android.util.Log.d("PrescriptionsViewModel", "Loaded ${result.data.size} prescriptions")
                            _allPrescriptions.value = result.data
                            updateUiState {
                                copy(
                                    isLoading = false,
                                    errorMessage = null,
                                    isEmpty = result.data.isEmpty()
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

            val result = prescriptionRepository.deletePrescription(prescriptionId)
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
        return when (val result = prescriptionRepository.getPrescriptionById(prescriptionId)) {
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

    /**
     * Search prescriptions by doctor name
     */
    private fun searchByDoctorName(prescriptions: List<Prescription>, query: String): List<Prescription> {
        return prescriptions.filter { prescription ->
            prescription.doctorName.contains(query, ignoreCase = true)
        }
    }

    /**
     * Helper function to update UI state
     */
    private fun updateUiState(update: PrescriptionsUiState.() -> PrescriptionsUiState) {
        _uiState.value = _uiState.value.update()
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