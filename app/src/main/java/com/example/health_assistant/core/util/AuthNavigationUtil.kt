package com.example.health_assistant.core.util

import android.content.Context
import android.content.Intent
import android.app.Activity
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.core.app.ActivityOptionsCompat
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Universal navigation utility for handling logout and account deletion flows
 * This ensures consistent navigation behavior across the entire app
 */
object AuthNavigationUtil {

    /**
     * Navigate to AuthActivity after logout or account deletion
     * This method provides a seamless transition without requiring manual app restart
     *
     * @param context The context from which navigation is initiated
     * @param activity The current activity (optional, for animations and finishing)
     */
    fun navigateToAuth(context: Context, activity: Activity? = null) {
        try {
            // Create intent to AuthActivity (same as what SplashActivity does for logged out users)
            val intent = Intent(context, com.example.health_assistant.auth.AuthActivity::class.java)

            // Clear the entire activity stack and start fresh (same flags as SplashActivity)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

            // Add smooth transition animation if activity is available
            activity?.let {
                val options = ActivityOptionsCompat.makeCustomAnimation(
                    context,
                    android.R.anim.fade_in,
                    android.R.anim.fade_out
                )
                context.startActivity(intent, options.toBundle())
                it.finish()
            } ?: run {
                // Start AuthActivity without animation if no activity context
                context.startActivity(intent)
            }

        } catch (e: Exception) {
            // Fallback: restart the app completely (this will go through SplashActivity)
            android.util.Log.e("AuthNavigationUtil", "Direct navigation to AuthActivity failed, restarting app", e)
            restartApp(context)
        }
    }

    /**
     * Navigate to AuthActivity from a Fragment
     * Convenience method for Fragment-based navigation
     *
     * @param fragment The fragment from which navigation is initiated
     */
    fun navigateToAuth(fragment: Fragment) {
        navigateToAuth(fragment.requireContext(), fragment.requireActivity())
    }

    /**
     * Navigate to AuthActivity from an Activity
     * Convenience method for Activity-based navigation
     *
     * @param activity The activity from which navigation is initiated
     */
    fun navigateToAuth(activity: Activity) {
        navigateToAuth(activity, activity)
    }

    /**
     * Fallback method to restart the entire app
     * Used when direct navigation fails
     *
     * @param context The context to restart from
     */
    private fun restartApp(context: Context) {
        try {
            val packageManager = context.packageManager
            val intent = packageManager.getLaunchIntentForPackage(context.packageName)
            intent?.let {
                it.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(it)
            }

            // If we have access to an activity, finish it
            if (context is Activity) {
                context.finish()
            }
        } catch (e: Exception) {
            android.util.Log.e("AuthNavigationUtil", "Failed to restart app", e)
        }
    }

    /**
     * Universal logout handler that combines auth logout with navigation
     * This method should be called from any logout button in the app
     *
     * @param authViewModel The AuthViewModel instance
     * @param context The context for navigation
     * @param activity The current activity (optional)
     * @param onLogoutStart Optional callback when logout starts
     * @param onLogoutSuccess Optional callback when logout succeeds
     * @param onLogoutError Optional callback when logout fails
     */
    fun performUniversalLogout(
        authViewModel: com.example.health_assistant.auth.viewmodel.AuthViewModel,
        context: Context,
        activity: Activity? = null,
        onLogoutStart: (() -> Unit)? = null,
        onLogoutSuccess: (() -> Unit)? = null,
        onLogoutError: ((String) -> Unit)? = null
    ) {
        // Trigger logout start callback
        onLogoutStart?.invoke()

        // Perform logout
        authViewModel.signOut()

        // Navigate to auth immediately (Firebase logout is typically instant)
        try {
            onLogoutSuccess?.invoke()
            navigateToAuth(context, activity)
        } catch (e: Exception) {
            onLogoutError?.invoke("Navigation failed: ${e.message}")
            android.util.Log.e("AuthNavigationUtil", "Universal logout navigation failed", e)
        }
    }

    /**
     * Universal account deletion handler that shows password confirmation and handles re-authentication
     * This method should be called from any delete account button in the app
     *
     * @param authViewModel The AuthViewModel instance
     * @param context The context for navigation and dialogs
     * @param coroutineScope The coroutine scope for async operations
     * @param activity The current activity (optional)
     * @param onDeleteStart Optional callback when deletion starts
     * @param onDeleteSuccess Optional callback when deletion succeeds
     * @param onDeleteError Optional callback when deletion fails
     */
    fun performUniversalAccountDeletion(
        authViewModel: com.example.health_assistant.auth.viewmodel.AuthViewModel,
        context: Context,
        coroutineScope: CoroutineScope,
        activity: Activity? = null,
        onDeleteStart: (() -> Unit)? = null,
        onDeleteSuccess: (() -> Unit)? = null,
        onDeleteError: ((String) -> Unit)? = null
    ) {
        try {
            // Get current user email directly from Firebase Auth
            val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            val userEmail = currentUser?.email

            if (userEmail == null) {
                onDeleteError?.invoke("Unable to get user email for re-authentication. Please make sure you're logged in.")
                return
            }

            // Show password confirmation dialog
            showPasswordConfirmationDialog(
                context = context,
                userEmail = userEmail,
                onConfirm = { password ->
                    // Trigger deletion start callback
                    onDeleteStart?.invoke()

                    // Perform account deletion with re-authentication
                    coroutineScope.launch {
                        try {
                            val result = authViewModel.deleteAccountWithReauth(userEmail, password)

                            when (result) {
                                is Result.Success -> {
                                    // Account deletion successful
                                    onDeleteSuccess?.invoke()
                                    navigateToAuth(context, activity)
                                }
                                is Result.Error -> {
                                    // Account deletion failed
                                    val errorMessage = when {
                                        result.message.contains("Re-authentication failed", ignoreCase = true) ->
                                            "Incorrect password. Please try again."
                                        result.message.contains("network", ignoreCase = true) ->
                                            "Network error. Please check your connection and try again."
                                        else -> "Account deletion failed: ${result.message}"
                                    }
                                    onDeleteError?.invoke(errorMessage)
                                }
                                is Result.Loading -> {
                                    // This shouldn't happen with suspend functions
                                    android.util.Log.w("AuthNavigationUtil", "Unexpected loading state in account deletion")
                                }
                            }
                        } catch (e: Exception) {
                            onDeleteError?.invoke("Account deletion failed: ${e.message}")
                            android.util.Log.e("AuthNavigationUtil", "Universal account deletion failed", e)
                        }
                    }
                },
                onCancel = {
                    // User cancelled the operation
                    android.util.Log.d("AuthNavigationUtil", "Account deletion cancelled by user")
                }
            )
        } catch (e: Exception) {
            onDeleteError?.invoke("Failed to initiate account deletion: ${e.message}")
            android.util.Log.e("AuthNavigationUtil", "Failed to initiate account deletion", e)
        }
    }

    /**
     * Shows a password confirmation dialog for account deletion
     *
     * @param context The context for the dialog
     * @param userEmail The user's email address
     * @param onConfirm Callback when password is confirmed
     * @param onCancel Callback when dialog is cancelled
     */
    private fun showPasswordConfirmationDialog(
        context: Context,
        userEmail: String,
        onConfirm: (String) -> Unit,
        onCancel: () -> Unit
    ) {
        val passwordInput = EditText(context).apply {
            hint = "Enter your password"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }

        AlertDialog.Builder(context)
            .setTitle("Confirm Account Deletion")
            .setMessage("To delete your account ($userEmail), please enter your password to confirm this action.\n\nThis action cannot be undone.")
            .setView(passwordInput)
            .setPositiveButton("Delete Account") { _, _ ->
                val password = passwordInput.text.toString().trim()
                if (password.isNotEmpty()) {
                    onConfirm(password)
                } else {
                    android.widget.Toast.makeText(context, "Password cannot be empty", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel") { _, _ ->
                onCancel()
            }
            .setCancelable(false)
            .show()
    }
}