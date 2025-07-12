package com.example.health_assistant.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.health_assistant.services.StepTrackingService
import dagger.hilt.android.AndroidEntryPoint

/**
 * Broadcast receiver to restart step tracking service after device boot
 * Ensures continuous step tracking even after device restarts
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
        private const val PREFS_NAME = "step_counter_prefs"
        private const val KEY_SERVICE_ENABLED = "service_enabled"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Boot receiver triggered: ${intent.action}")

        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_PACKAGE_REPLACED -> {
                handleBootCompleted(context)
            }
        }
    }

    private fun handleBootCompleted(context: Context) {
        // Check if step tracking was previously enabled
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val serviceWasEnabled = prefs.getBoolean(KEY_SERVICE_ENABLED, false)

        if (serviceWasEnabled) {
            Log.d(TAG, "Restarting step tracking service after boot")

            try {
                val serviceIntent = Intent(context, StepTrackingService::class.java)
                context.startForegroundService(serviceIntent)

                Log.d(TAG, "Step tracking service restarted successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to restart step tracking service", e)
            }
        } else {
            Log.d(TAG, "Step tracking service was not enabled, skipping restart")
        }
    }
}