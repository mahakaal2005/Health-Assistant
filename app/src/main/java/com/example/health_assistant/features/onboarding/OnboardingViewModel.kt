package com.example.health_assistant.features.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Onboarding flow
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val onboardingPreferencesRepository: OnboardingPreferencesRepository
) : ViewModel() {

    private val _currentPage = MutableStateFlow(0)
    val currentPage: StateFlow<Int> = _currentPage

    /**
     * Updates the current page in the onboarding flow
     */
    fun setCurrentPage(position: Int) {
        _currentPage.value = position
    }

    /**
     * Completes the onboarding process and saves the completion state
     */
    fun completeOnboarding() {
        viewModelScope.launch {
            onboardingPreferencesRepository.setOnboardingCompleted()
        }
    }

    /**
     * Checks if this is the last page in the onboarding flow
     */
    fun isLastPage(position: Int, pageCount: Int): Boolean {
        return position == pageCount - 1
    }
}