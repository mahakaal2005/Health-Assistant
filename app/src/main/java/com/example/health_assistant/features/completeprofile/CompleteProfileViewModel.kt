package com.example.health_assistant.features.completeprofile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.health_assistant.core.util.Result
import com.example.health_assistant.data.repository.interfaces.PersonalHealthInfo
import com.example.health_assistant.data.repository.interfaces.UserProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for managing Complete Profile screen state and business logic
 */
@HiltViewModel
class CompleteProfileViewModel @Inject constructor(
    private val userProfileRepository: UserProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CompleteProfileUiState())
    val uiState: StateFlow<CompleteProfileUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<CompleteProfileEvent>()
    val events: SharedFlow<CompleteProfileEvent> = _events.asSharedFlow()

    fun setGender(gender: String) {
        _uiState.value = _uiState.value.copy(selectedGender = gender)
        updateFormValidation()
    }

    fun setHeight(height: Float) {
        _uiState.value = _uiState.value.copy(selectedHeight = height)
        updateFormValidation()
    }

    fun setWeight(weight: Float) {
        _uiState.value = _uiState.value.copy(selectedWeight = weight)
        updateFormValidation()
    }

    fun setBirthday(birthday: String) {
        _uiState.value = _uiState.value.copy(selectedBirthday = birthday)
        updateFormValidation()
    }

    private fun updateFormValidation() {
        val currentState = _uiState.value
        val isValid = currentState.selectedGender != null &&
                currentState.selectedHeight != null &&
                currentState.selectedWeight != null &&
                currentState.selectedBirthday != null

        _uiState.value = currentState.copy(isFormValid = isValid)
    }

    fun saveProfile() {
        val currentState = _uiState.value
        if (!currentState.isFormValid) return

        viewModelScope.launch {
            _uiState.value = currentState.copy(isLoading = true)

            try {
                val personalHealthInfo = PersonalHealthInfo(
                    gender = currentState.selectedGender!!,
                    height = currentState.selectedHeight!!,
                    weight = currentState.selectedWeight!!,
                    birthday = currentState.selectedBirthday!!
                )

                when (val result = userProfileRepository.updatePersonalHealthInfo(personalHealthInfo)) {
                    is Result.Success -> {
                        // Mark profile as complete
                        when (val markCompleteResult = userProfileRepository.markProfileComplete()) {
                            is Result.Success -> {
                                _events.emit(CompleteProfileEvent.ShowSuccess("Profile saved successfully!"))
                                _events.emit(CompleteProfileEvent.NavigateToHome)
                            }
                            is Result.Error -> {
                                _events.emit(CompleteProfileEvent.ShowError(markCompleteResult.message))
                            }
                            is Result.Loading -> { /* Handle if needed */ }
                        }
                    }
                    is Result.Error -> {
                        _events.emit(CompleteProfileEvent.ShowError(result.message))
                    }
                    is Result.Loading -> { /* Handle if needed */ }
                }
            } catch (e: Exception) {
                _events.emit(CompleteProfileEvent.ShowError("Error saving profile. Please try again."))
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun skipProfile() {
        viewModelScope.launch {
            // Mark profile as complete even when skipped
            when (val result = userProfileRepository.markProfileComplete()) {
                is Result.Success -> {
                    _events.emit(CompleteProfileEvent.NavigateToHome)
                }
                is Result.Error -> {
                    _events.emit(CompleteProfileEvent.ShowError(result.message))
                }
                is Result.Loading -> { /* Handle if needed */ }
            }
        }
    }
}

/**
 * UI State for Complete Profile screen
 */
data class CompleteProfileUiState(
    val selectedGender: String? = null,
    val selectedHeight: Float? = null,
    val selectedWeight: Float? = null,
    val selectedBirthday: String? = null,
    val isFormValid: Boolean = false,
    val isLoading: Boolean = false
)

/**
 * Events for Complete Profile screen
 */
sealed class CompleteProfileEvent {
    object NavigateToHome : CompleteProfileEvent()
    data class ShowError(val message: String) : CompleteProfileEvent()
    data class ShowSuccess(val message: String) : CompleteProfileEvent()
}