package com.example.health_assistant.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Android 15 (API 36) Permission Manager
 * Handles the new permission model introduced in Android 15
 */
@Singleton
class Android15PermissionManager @Inject constructor() {

    companion object {
        // Android 15 specific permissions
        private val ANDROID_15_PERMISSIONS = arrayOf(
            Manifest.permission.POST_NOTIFICATIONS,
            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
        )

        // Media permissions for Android 13+
        private val MEDIA_PERMISSIONS = arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO
        )

        // Health and fitness permissions
        private val HEALTH_PERMISSIONS = arrayOf(
            Manifest.permission.ACTIVITY_RECOGNITION,
            Manifest.permission.BODY_SENSORS,
            Manifest.permission.BODY_SENSORS_BACKGROUND
        )

        // Camera permission
        private val CAMERA_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA
        )
    }

    /**
     * Check if notification permission is granted (Android 15+)
     */
    fun isNotificationPermissionGranted(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Notifications are granted by default on older versions
        }
    }

    /**
     * Check if media permissions are granted (Android 13+)
     */
    fun isMediaPermissionGranted(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            MEDIA_PERMISSIONS.all { permission ->
                ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
            }
        } else {
            // Check legacy storage permission
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Check if partial media access is granted (Android 15+)
     */
    fun isPartialMediaAccessGranted(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= 35) { // Android 15
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            false
        }
    }

    /**
     * Check if health permissions are granted
     */
    fun areHealthPermissionsGranted(context: Context): Boolean {
        return HEALTH_PERMISSIONS.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Check if camera permission is granted
     */
    fun isCameraPermissionGranted(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Get required permissions based on Android version
     */
    fun getRequiredPermissions(): Array<String> {
        val permissions = mutableListOf<String>()

        // Always add camera permission
        permissions.addAll(CAMERA_PERMISSIONS)

        // Add health permissions
        permissions.addAll(HEALTH_PERMISSIONS)

        // Add media permissions based on Android version
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.addAll(MEDIA_PERMISSIONS)
            permissions.addAll(ANDROID_15_PERMISSIONS)
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        return permissions.toTypedArray()
    }

    /**
     * Create permission launcher for Fragment
     */
    fun createPermissionLauncher(
        fragment: Fragment,
        onPermissionResult: (Map<String, Boolean>) -> Unit
    ): ActivityResultLauncher<Array<String>> {
        return fragment.registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            onPermissionResult(permissions)
        }
    }

    /**
     * Handle permission request results
     */
    fun handlePermissionResults(
        context: Context,
        permissions: Map<String, Boolean>,
        onAllGranted: () -> Unit,
        onSomeGranted: (granted: List<String>, denied: List<String>) -> Unit,
        onAllDenied: () -> Unit
    ) {
        val granted = permissions.filter { it.value }.keys.toList()
        val denied = permissions.filter { !it.value }.keys.toList()

        when {
            denied.isEmpty() -> onAllGranted()
            granted.isNotEmpty() -> onSomeGranted(granted, denied)
            else -> onAllDenied()
        }
    }

    /**
     * Check if we should show rationale for any permission
     */
    fun shouldShowRationale(activity: Activity, permissions: Array<String>): Boolean {
        return permissions.any { permission ->
            activity.shouldShowRequestPermissionRationale(permission)
        }
    }

    /**
     * Get permission rationale message for Android 15
     */
    fun getPermissionRationaleMessage(permission: String): String {
        return when (permission) {
            Manifest.permission.POST_NOTIFICATIONS ->
                "Notifications help you stay updated on your health goals and reminders."

            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO ->
                "Media access is needed to select photos for your profile and health records."

            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED ->
                "Partial photo access lets you choose specific images to share with the app."

            Manifest.permission.CAMERA ->
                "Camera access is needed to take photos for your health records and profile."

            Manifest.permission.ACTIVITY_RECOGNITION ->
                "Activity recognition helps track your fitness and health activities."

            Manifest.permission.BODY_SENSORS,
            Manifest.permission.BODY_SENSORS_BACKGROUND ->
                "Sensor access enables accurate health and fitness tracking."

            else -> "This permission is required for the app to function properly."
        }
    }
}
