package com.example.health_assistant.features.prescriptions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.health_assistant.auth.session.SessionManager
import com.example.health_assistant.core.util.Result
import com.example.health_assistant.data.model.DiseaseCategory
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

    // Get current user ID
    private val currentUserId: String
        get() = sessionManager.getCurrentUserId() ?: ""

    // Combined flow of prescriptions with search filtering
    val prescriptions: StateFlow<List<PrescriptionItem>> = combine(
        flow {
            if (currentUserId.isNotEmpty()) {
                prescriptionRepository.getAllPrescriptions(currentUserId).collect { result ->
                    emit(result)
                }
            } else {
                emit(Result.Success(emptyList<Prescription>()))
            }
        },
        _searchQuery
    ) { prescriptionResult, query ->
        when (prescriptionResult) {
            is Result.Success -> {
                val prescriptions = prescriptionResult.data
                val filteredPrescriptions = if (query.isBlank()) {
                    prescriptions
                } else {
                    searchByDoctorName(prescriptions, query)
                }

                // Group prescriptions by category and create UI items
                createPrescriptionItems(filteredPrescriptions)
            }
            is Result.Error -> {
                updateUiState { copy(errorMessage = prescriptionResult.message) }
                emptyList()
            }
            is Result.Loading -> {
                updateUiState { copy(isLoading = true) }
                emptyList()
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

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
        viewModelScope.launch {
            updateUiState { copy(isLoading = true) }

            if (currentUserId.isNotEmpty()) {
                prescriptionRepository.getAllPrescriptions(currentUserId).collect { result ->
                    when (result) {
                        is Result.Success -> {
                            updateUiState {
                                copy(
                                    isLoading = false,
                                    errorMessage = null
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
                            updateUiState { copy(isLoading = true) }
                        }
                    }
                }
            } else {
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
     * Create prescription items from list for UI display
     */
    private fun createPrescriptionItems(prescriptions: List<Prescription>): List<PrescriptionItem> {
        val grouped = groupAndSortPrescriptions(prescriptions)
        val items = mutableListOf<PrescriptionItem>()

        grouped.entries.forEach { (category, categoryPrescriptions) ->
            // Add category header
            items.add(
                PrescriptionItem.CategoryHeader(
                    category = category,
                    count = categoryPrescriptions.size
                )
            )

            // Add prescription cards
            categoryPrescriptions.forEach { prescription ->
                items.add(
                    PrescriptionItem.PrescriptionCard(
                        prescription = prescription,
                        category = category
                    )
                )
            }
        }

        return items
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
     * Group and sort prescriptions by category
     */
    private fun groupAndSortPrescriptions(prescriptions: List<Prescription>): Map<DiseaseCategory, List<Prescription>> {
        // Get all categories for proper grouping
        val allCategories = DiseaseCategory.getDefaultCategories()
        val categoryMap = allCategories.associateBy { it.id }

        return prescriptions
            .groupBy { prescription ->
                // Find the category object by ID
                categoryMap[prescription.categoryId] ?: DiseaseCategory(
                    id = prescription.categoryId,
                    displayName = "Unknown Category"
                )
            }
            .mapValues { (_, prescriptions) ->
                // Sort prescriptions within each category by date (newest first)
                prescriptions.sortedByDescending { it.dateAdded }
            }
            .toSortedMap(compareBy { it.displayName })
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

/**
 * Sealed class for different types of items in prescription list
 */
sealed class PrescriptionItem {
    data class CategoryHeader(
        val category: DiseaseCategory,
        val count: Int
    ) : PrescriptionItem()

    data class PrescriptionCard(
        val prescription: Prescription,
        val category: DiseaseCategory
    ) : PrescriptionItem()
}