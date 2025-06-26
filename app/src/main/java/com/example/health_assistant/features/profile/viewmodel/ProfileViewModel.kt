package com.example.health_assistant.features.profile.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.health_assistant.core.util.Result
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

    private val _userEmail = MutableLiveData<String?>()
    val userEmail: LiveData<String?> = _userEmail

    private val _userId = MutableLiveData<String?>()
    val userId: LiveData<String?> = _userId

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    init {
        loadUserProfile()
    }

    private fun loadUserProfile() {
        viewModelScope.launch {
            // Load user email
            userProfileRepository.getUserEmail().collect { result ->
                when (result) {
                    is Result.Success -> {
                        _userEmail.value = result.data
                        _error.value = null
                    }
                    is Result.Error -> {
                        _error.value = result.message
                    }
                    is Result.Loading -> {
                        // Handle loading state if needed
                    }
                }
            }
        }

        viewModelScope.launch {
            // Load user ID
            userProfileRepository.getUserId().collect { result ->
                when (result) {
                    is Result.Success -> {
                        _userId.value = result.data
                        _error.value = null
                    }
                    is Result.Error -> {
                        _error.value = result.message
                    }
                    is Result.Loading -> {
                        // Handle loading state if needed
                    }
                }
            }
        }
    }

    /**
     * Save user profile information
     */
    fun saveUserProfile(userId: String, email: String) {
        viewModelScope.launch {
            userProfileRepository.saveUserProfile(userId, email).let { result ->
                if (result is Result.Error) {
                    _error.value = result.message
                }
            }
        }
    }

    /**
     * Clear user profile data
     */
    fun clearUserProfile() {
        viewModelScope.launch {
            userProfileRepository.clearUserProfile().let { result ->
                if (result is Result.Error) {
                    _error.value = result.message
                }
            }
        }
    }

    /**
     * Sign out the user
     */
    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut().let { result ->
                if (result is Result.Error) {
                    _error.value = result.message
                }
            }
        }
    }
}