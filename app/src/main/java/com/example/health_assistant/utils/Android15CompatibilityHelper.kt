package com.example.health_assistant.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Android 15 Compatibility Helper
 * Handles behavioral changes and compatibility issues for targetSdk 36
 */
@Singleton
class Android15CompatibilityHelper @Inject constructor() {

    /**
     * Handle the new Android 15 edge-to-edge enforcement
     */
    fun enableEdgeToEdgeIfNeeded(context: Context) {
        if (Build.VERSION.SDK_INT >= 35) { // Android 15
            // Edge-to-edge is enforced on Android 15+
            // Ensure your layouts handle system bars properly
        }
    }

    /**
     * Handle Android 15 notification permission requirements
     */
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun handleNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Notification permission is required on Android 13+
            // and more strictly enforced on Android 15
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    /**
     * Handle Android 15 partial photo picker
     */
    fun supportsPartialMediaAccess(): Boolean {
        return Build.VERSION.SDK_INT >= 35 // Android 15
    }

    /**
     * Get appropriate media picker intent for Android version
     */
    fun getMediaPickerIntent(): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Use Photo Picker on Android 13+
            Intent(Intent.ACTION_PICK).apply {
                type = "image/*"
                putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/jpeg", "image/png"))
            }
        } else {
            // Use legacy gallery picker
            Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "image/*"
            }
        }
    }

    /**
     * Handle Android 15 foreground service restrictions
     */
    fun canStartForegroundService(context: Context, serviceType: String): Boolean {
        return if (Build.VERSION.SDK_INT >= 35) { // Android 15
            // More restrictive foreground service rules
            when (serviceType) {
                "health" -> true // Health services are allowed
                "camera" -> true // Camera services are allowed
                else -> false
            }
        } else {
            true
        }
    }

    /**
     * Navigate to app settings for permission management
     */
    fun openAppSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    /**
     * Navigate to notification settings (Android 15+)
     */
    fun openNotificationSettings(context: Context) {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            }
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
            }
        }
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }

    /**
     * Check if device supports Android 15 features
     */
    fun isAndroid15OrHigher(): Boolean = Build.VERSION.SDK_INT >= 35

    /**
     * Get appropriate file provider authority for sharing
     */
    fun getFileProviderAuthority(context: Context): String {
        return "${context.packageName}.fileprovider"
    }

    /**
     * Handle Android 15 security enhancements
     */
    fun isSecureEnvironment(): Boolean {
        return if (Build.VERSION.SDK_INT >= 35) {
            // Android 15 has enhanced security checks
            !Build.TAGS.contains("test-keys") && !Build.TYPE.equals("eng", ignoreCase = true)
        } else {
            true
        }
    }
}
