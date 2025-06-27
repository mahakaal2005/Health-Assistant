package com.example.health_assistant.data.sync

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.example.health_assistant.core.util.Result
import com.example.health_assistant.data.repository.interfaces.UserProfileRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages automatic profile synchronization between local storage and Firestore
 * Handles periodic sync, network reconnection sync, and app lifecycle sync
 */
@Singleton
class ProfileSyncManager @Inject constructor(
    private val userProfileRepository: UserProfileRepository,
    private val firebaseAuth: FirebaseAuth
) : DefaultLifecycleObserver {

    private val syncScope = CoroutineScope(SupervisorJob())
    private var isSyncEnabled = true

    /**
     * Start monitoring for sync opportunities
     */
    fun startSyncMonitoring(lifecycleOwner: LifecycleOwner) {
        lifecycleOwner.lifecycle.addObserver(this)

        // Sync when auth state changes
        monitorAuthState()
    }

    /**
     * Sync profile data when app comes to foreground
     */
    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        if (isSyncEnabled) {
            syncFromFirestore()
        }
    }

    /**
     * Sync profile data when app goes to background
     */
    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        if (isSyncEnabled) {
            syncToFirestore()
        }
    }

    /**
     * Monitor Firebase Auth state changes and sync accordingly
     */
    private fun monitorAuthState() {
        firebaseAuth.addAuthStateListener { auth ->
            val user = auth.currentUser
            if (user != null && isSyncEnabled) {
                // User is signed in, sync from Firestore
                syncFromFirestore()
            }
        }
    }

    /**
     * Sync local profile data to Firestore
     */
    fun syncToFirestore() {
        val currentUser = firebaseAuth.currentUser ?: return

        syncScope.launch {
            try {
                when (val profileResult = userProfileRepository.getUserProfile()) {
                    is Result.Success -> {
                        profileResult.data?.let { profile ->
                            userProfileRepository.updateUserProfileInFirestore(profile)
                        }
                    }
                    is Result.Error -> {
                        // Log error but don't throw
                        android.util.Log.w("ProfileSync", "Failed to get local profile for sync: ${profileResult.message}")
                    }
                    is Result.Loading -> { /* Handle if needed */ }
                }
            } catch (e: Exception) {
                android.util.Log.e("ProfileSync", "Error syncing to Firestore", e)
            }
        }
    }

    /**
     * Sync profile data from Firestore to local storage
     */
    fun syncFromFirestore() {
        val currentUser = firebaseAuth.currentUser ?: return

        syncScope.launch {
            try {
                when (val result = userProfileRepository.syncUserProfileFromFirestore(currentUser.uid)) {
                    is Result.Success -> {
                        // Sync successful or no remote profile exists
                        android.util.Log.d("ProfileSync", "Profile synced from Firestore successfully")
                    }
                    is Result.Error -> {
                        // Log error but don't throw - app should continue with local data
                        android.util.Log.w("ProfileSync", "Failed to sync from Firestore: ${result.message}")
                    }
                    is Result.Loading -> { /* Handle if needed */ }
                }
            } catch (e: Exception) {
                android.util.Log.e("ProfileSync", "Error syncing from Firestore", e)
            }
        }
    }

    /**
     * Force a bidirectional sync (useful for manual refresh)
     */
    fun forceBidirectionalSync() {
        syncFromFirestore()
        syncToFirestore()
    }

    /**
     * Enable or disable automatic sync
     */
    fun setSyncEnabled(enabled: Boolean) {
        isSyncEnabled = enabled
    }

    /**
     * Check if sync is currently enabled
     */
    fun isSyncEnabled(): Boolean = isSyncEnabled
}