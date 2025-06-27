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

                android.util.Log.d("CompleteProfile", "About to save health info: gender=${personalHealthInfo.gender}, height=${personalHealthInfo.height}, weight=${personalHealthInfo.weight}, birthday=${personalHealthInfo.birthday}")

                // Step 1: Save personal health info locally first
                val healthInfoResult = userProfileRepository.updatePersonalHealthInfo(personalHealthInfo)

                if (healthInfoResult is Result.Success) {
                    android.util.Log.d("CompleteProfile", "Health info saved successfully to DataStore")

                    // Step 2: Mark profile as complete
                    val completeResult = userProfileRepository.markProfileComplete()

                    if (completeResult is Result.Success) {
                        android.util.Log.d("CompleteProfile", "Profile marked as complete")

                        // Step 3: Get profile and sync to Firestore (non-blocking)
                        launch {
                            try {
                                val profileResult = userProfileRepository.getUserProfile()
                                if (profileResult is Result.Success && profileResult.data != null) {
                                    userProfileRepository.updateUserProfileInFirestore(profileResult.data)
                                    android.util.Log.d("CompleteProfile", "Firestore sync completed")
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("CompleteProfile", "Firestore sync failed", e)
                            }
                        }

                        // Always stop loading and navigate after successful local save
                        _uiState.value = _uiState.value.copy(isLoading = false)
                        _events.emit(CompleteProfileEvent.ShowSuccess("Profile saved successfully!"))
                        _events.emit(CompleteProfileEvent.NavigateToHome)
                    } else {
                        android.util.Log.e("CompleteProfile", "Failed to mark profile complete: ${(completeResult as? Result.Error)?.message}")
                        _uiState.value = _uiState.value.copy(isLoading = false)
                        _events.emit(CompleteProfileEvent.ShowError("Failed to complete profile. Please try again."))
                    }
                } else {
                    android.util.Log.e("CompleteProfile", "Failed to save health info: ${(healthInfoResult as? Result.Error)?.message}")
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    _events.emit(CompleteProfileEvent.ShowError("Failed to save profile data. Please try again."))
                }
            } catch (e: Exception) {
                android.util.Log.e("CompleteProfile", "Exception during save", e)
                _uiState.value = _uiState.value.copy(isLoading = false)
                _events.emit(CompleteProfileEvent.ShowError("Error saving profile. Please try again."))
            }
        }
    }

    /**
     * Reset loading state - used when navigation fails
     */
    fun resetLoadingState() {
        _uiState.value = _uiState.value.copy(isLoading = false)
    }

    fun skipProfile() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                // Mark profile as complete even when skipped
                when (val result = userProfileRepository.markProfileComplete()) {
                    is Result.Success -> {
                        // Also sync the "completed but skipped" status to Firestore
                        when (val profileResult = userProfileRepository.getUserProfile()) {
                            is Result.Success -> {
                                profileResult.data?.let { userProfile ->
                                    val updatedProfile = userProfile.copy(isProfileComplete = true)
                                    // Non-blocking Firestore sync
                                    userProfileRepository.updateUserProfileInFirestore(updatedProfile)
                                }
                            }
                            is Result.Error -> { /* Continue anyway */ }
                            is Result.Loading -> { /* Handle if needed */ }
                        }
                        _events.emit(CompleteProfileEvent.NavigateToHome)
                    }
                    is Result.Error -> {
                        _events.emit(CompleteProfileEvent.ShowError(result.message))
                    }
                    is Result.Loading -> { /* Handle if needed */ }
                }
            } catch (e: Exception) {
                _events.emit(CompleteProfileEvent.ShowError("Error completing profile. Please try again."))
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
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