package com.example.health_assistant.auth.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.health_assistant.data.repository.interfaces.AuthRepository
import com.example.health_assistant.data.repository.interfaces.UserProfileRepository
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for handling authentication operations
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userProfileRepository: UserProfileRepository
) : ViewModel() {

    private val _authState = MutableLiveData<AuthState>()
    val authState: LiveData<AuthState> = _authState

    private val _currentUser = MutableLiveData<FirebaseUser?>()
    val currentUser: LiveData<FirebaseUser?> = _currentUser

    init {
        // Observe the current user from the auth repository
        viewModelScope.launch {
            authRepository.getCurrentUser().collect { user ->
                _currentUser.value = user
            }
        }
    }

    /**
     * Register a new user with email and password
     */
    fun registerUser(email: String, password: String) {
        _authState.value = AuthState.Loading

        viewModelScope.launch {
            authRepository.registerUser(email, password)
                .onSuccess { user ->
                    _authState.value = AuthState.Success
                    // Save user profile data
                    user?.let {
                        userProfileRepository.saveUserProfile(it.uid, it.email ?: "")
                    }
                }
                .onFailure { exception ->
                    _authState.value = AuthState.Error(exception.message ?: "Registration failed")
                }
        }
    }

    /**
     * Sign in an existing user with email and password
     */
    fun signInUser(email: String, password: String) {
        _authState.value = AuthState.Loading

        viewModelScope.launch {
            authRepository.signInUser(email, password)
                .onSuccess { user ->
                    _authState.value = AuthState.Success
                    // Save user profile data
                    user?.let {
                        userProfileRepository.saveUserProfile(it.uid, it.email ?: "")
                    }
                }
                .onFailure { exception ->
                    _authState.value = AuthState.Error(exception.message ?: "Login failed")
                }
        }
    }

    /**
     * Send password reset email
     */
    fun resetPassword(email: String) {
        _authState.value = AuthState.Loading

        viewModelScope.launch {
            authRepository.sendPasswordResetEmail(email)
                .onSuccess {
                    _authState.value = AuthState.Success
                }
                .onFailure { exception ->
                    _authState.value = AuthState.Error(exception.message ?: "Password reset failed")
                }
        }
    }

    /**
     * Check if a user is currently logged in
     */
    fun isUserLoggedIn(): Boolean {
        return authRepository.isUserLoggedIn()
    }
}

/**
 * Sealed class representing the authentication state
 */
sealed class AuthState {
    object Loading : AuthState()
    object Success : AuthState()
    data class Error(val message: String) : AuthState()
}