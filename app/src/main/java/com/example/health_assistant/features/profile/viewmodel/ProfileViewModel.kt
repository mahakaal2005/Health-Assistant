package com.example.health_assistant.features.profile.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.health_assistant.data.repository.interfaces.AuthRepository
import com.example.health_assistant.data.repository.interfaces.UserProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for managing user profile data
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userProfileRepository: UserProfileRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    // Expose user profile data as LiveData
    val userEmail: LiveData<String?> = userProfileRepository.getUserEmail().asLiveData()
    val userId: LiveData<String?> = userProfileRepository.getUserId().asLiveData()

    /**
     * Save user profile information
     */
    fun saveUserProfile(userId: String, email: String) {
        viewModelScope.launch {
            userProfileRepository.saveUserProfile(userId, email)
        }
    }

    /**
     * Sign out the current user
     */
    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            userProfileRepository.clearUserProfile()
        }
    }

    /**
     * Check if a user is currently logged in
     */
    fun isUserLoggedIn(): Boolean {
        return authRepository.isUserLoggedIn()
    }
}