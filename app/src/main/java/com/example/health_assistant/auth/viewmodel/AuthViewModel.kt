package com.example.health_assistant.auth.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.health_assistant.core.util.Result
import com.example.health_assistant.data.repository.interfaces.AuthRepository
import com.example.health_assistant.data.repository.interfaces.UserProfileRepository
import com.example.health_assistant.data.repository.interfaces.UserProfile
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
            authRepository.getCurrentUser().collect { result ->
                when (result) {
                    is Result.Success -> {
                        _currentUser.value = result.data
                    }
                    is Result.Error -> {
                        _currentUser.value = null
                    }
                    is Result.Loading -> {
                        // Keep current value during loading
                    }
                }
            }
        }
    }

    /**
     * Register a new user with email and password
     * Creates user profile in both local DataStore and Firestore
     */
    fun registerUser(email: String, password: String) {
        _authState.value = AuthState.Loading

        viewModelScope.launch {
            val result = authRepository.registerUser(email, password)
            when (result) {
                is Result.Success -> {
                    result.data?.let { user ->
                        // Create complete user profile for Firestore
                        val userProfile = UserProfile(
                            userId = user.uid,
                            email = user.email ?: email,
                            displayName = user.displayName,
                            photoUrl = user.photoUrl?.toString(),
                            createdAt = System.currentTimeMillis(),
                            isProfileComplete = false
                        )

                        // Create profile in Firestore (this also saves to local DataStore)
                        val firestoreResult = userProfileRepository.createUserProfileInFirestore(userProfile)
                        when (firestoreResult) {
                            is Result.Success -> {
                                _authState.value = AuthState.Success
                            }
                            is Result.Error -> {
                                // Firestore failed, but auth succeeded - still allow user to proceed
                                // Save to local DataStore as fallback
                                userProfileRepository.saveUserProfile(user.uid, user.email ?: email)
                                _authState.value = AuthState.Success
                            }
                            is Result.Loading -> {
                                // Should not happen for suspend function
                            }
                        }
                    } ?: run {
                        _authState.value = AuthState.Error("User data not available after signup")
                    }
                }
                is Result.Error -> {
                    _authState.value = AuthState.Error(result.message)
                }
                is Result.Loading -> {
                    // Already handled above
                }
            }
        }
    }

    /**
     * Sign in an existing user with email and password
     * Syncs user profile from Firestore if available
     */
    fun signInUser(email: String, password: String) {
        _authState.value = AuthState.Loading

        viewModelScope.launch {
            val result = authRepository.signInUser(email, password)
            when (result) {
                is Result.Success -> {
                    result.data?.let { user ->
                        // Try to sync profile from Firestore first
                        val syncResult = userProfileRepository.syncUserProfileFromFirestore(user.uid)
                        when (syncResult) {
                            is Result.Success -> {
                                // Successfully synced from Firestore or profile doesn't exist
                                _authState.value = AuthState.Success
                            }
                            is Result.Error -> {
                                // Firestore sync failed, save basic info to local DataStore
                                userProfileRepository.saveUserProfile(user.uid, user.email ?: email)
                                _authState.value = AuthState.Success
                            }
                            is Result.Loading -> {
                                // Should not happen for suspend function
                            }
                        }
                    } ?: run {
                        _authState.value = AuthState.Error("User data not available after sign in")
                    }
                }
                is Result.Error -> {
                    _authState.value = AuthState.Error(result.message)
                }
                is Result.Loading -> {
                    // Already handled above
                }
            }
        }
    }

    /**
     * Send password reset email
     */
    fun resetPassword(email: String) {
        _authState.value = AuthState.Loading

        viewModelScope.launch {
            val result = authRepository.sendPasswordResetEmail(email)
            when (result) {
                is Result.Success -> {
                    _authState.value = AuthState.Success
                }
                is Result.Error -> {
                    _authState.value = AuthState.Error(result.message)
                }
                is Result.Loading -> {
                    // Already handled above
                }
            }
        }
    }

    /**
     * Sign out the current user
     */
    fun signOut() {
        viewModelScope.launch {
            val result = authRepository.signOut()
            when (result) {
                is Result.Success -> {
                    _currentUser.value = null
                    userProfileRepository.clearUserProfile()
                }
                is Result.Error -> {
                    // Handle sign out error if needed
                }
                is Result.Loading -> {
                    // Handle loading state if needed
                }
            }
        }
    }

    /**
     * Check if a user is currently logged in
     */
    fun isUserLoggedIn(): Boolean {
        return authRepository.isUserLoggedIn()
    }

    /**
     * Check if the current user's profile is complete
     */
    suspend fun isProfileComplete(): Boolean {
        return when (val result = userProfileRepository.isProfileComplete()) {
            is Result.Success -> result.data
            else -> false
        }
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