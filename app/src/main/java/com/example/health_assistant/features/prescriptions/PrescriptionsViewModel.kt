package com.example.health_assistant.features.prescriptions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.health_assistant.data.model.DiseaseCategory
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
    private val prescriptionRepository: PrescriptionRepository
) : ViewModel() {

    // Search query for filtering prescriptions
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // UI state for loading, empty states, etc.
    private val _uiState = MutableStateFlow(PrescriptionsUiState())
    val uiState: StateFlow<PrescriptionsUiState> = _uiState.asStateFlow()

    // Combined flow of prescriptions with search filtering
    val prescriptions: StateFlow<List<PrescriptionItem>> = combine(
        prescriptionRepository.getAllPrescriptions(),
        _searchQuery
    ) { prescriptions, query ->
        val filteredPrescriptions = if (query.isBlank()) {
            prescriptions
        } else {
            PrescriptionUtils.searchByDoctorName(prescriptions, query)
        }

        // Group prescriptions by category and create UI items
        createPrescriptionItems(filteredPrescriptions)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        // Monitor prescriptions for empty state
        viewModelScope.launch {
            prescriptions.collect { items ->
                val isEmpty = items.filterIsInstance<PrescriptionItem.PrescriptionCard>().isEmpty()
                _uiState.value = _uiState.value.copy(
                    isEmpty = isEmpty,
                    isLoading = false
                )
            }
        }
    }

    /**
     * Update search query for real-time filtering
     */
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    /**
     * Clear search query
     */
    fun clearSearch() {
        _searchQuery.value = ""
    }

    /**
     * Delete a prescription by ID
     */
    fun deletePrescription(prescriptionId: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                val result = prescriptionRepository.deletePrescription(prescriptionId)
                result.fold(
                    onSuccess = {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            message = "Prescription deleted successfully"
                        )
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Failed to delete prescription: ${error.message}"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to delete prescription: ${e.message}"
                )
            }
        }
    }

    /**
     * Update an existing prescription
     */
    fun updatePrescription(prescription: Prescription) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                val result = prescriptionRepository.updatePrescription(prescription)
                result.fold(
                    onSuccess = {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            message = "Prescription updated successfully"
                        )
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Failed to update prescription: ${error.message}"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to update prescription: ${e.message}"
                )
            }
        }
    }

    /**
     * Get prescription by ID
     */
    suspend fun getPrescriptionById(prescriptionId: String): Prescription? {
        return prescriptionRepository.getPrescriptionById(prescriptionId)
    }

    /**
     * Clear any displayed message
     */
    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    /**
     * Create prescription items for RecyclerView (headers + cards)
     */
    private fun createPrescriptionItems(prescriptions: List<Prescription>): List<PrescriptionItem> {
        if (prescriptions.isEmpty()) return emptyList()

        val grouped = PrescriptionUtils.groupAndSortPrescriptions(prescriptions)
        val items = mutableListOf<PrescriptionItem>()

        grouped.forEach { (category, categoryPrescriptions) ->
            // Add category header
            items.add(
                PrescriptionItem.CategoryHeader(
                    category = category,
                    prescriptionCount = categoryPrescriptions.size
                )
            )

            // Add prescription cards
            categoryPrescriptions.forEach { prescription ->
                items.add(PrescriptionItem.PrescriptionCard(prescription))
            }
        }

        return items
    }
}

/**
 * UI state for prescriptions screen
 */
data class PrescriptionsUiState(
    val isLoading: Boolean = true,
    val isEmpty: Boolean = false,
    val message: String? = null,
    val error: String? = null
)

/**
 * Sealed class for different item types in RecyclerView
 */
sealed class PrescriptionItem {
    data class CategoryHeader(
        val category: DiseaseCategory,
        val prescriptionCount: Int
    ) : PrescriptionItem()

    data class PrescriptionCard(
        val prescription: Prescription
    ) : PrescriptionItem()
}