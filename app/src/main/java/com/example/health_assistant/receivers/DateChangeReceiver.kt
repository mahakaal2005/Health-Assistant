package com.example.health_assistant.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.health_assistant.auth.session.SessionManager
import com.example.health_assistant.data.sensors.DeviceSensorManager
import com.example.health_assistant.features.journal.workers.ActivityCardScheduler
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * Broadcast receiver to handle date/time changes for midnight reset
 * Ensures proper daily step count reset when date changes
 */
@AndroidEntryPoint
class DateChangeReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "DateChangeReceiver"
    }

    @Inject
    lateinit var deviceSensorManager: DeviceSensorManager
    
    @Inject
    lateinit var sessionManager: SessionManager
    
    @Inject
    lateinit var activityCardScheduler: ActivityCardScheduler

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Date change receiver triggered: ${intent.action}")

        when (intent.action) {
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> {
                handleDateTimeChange()
            }
        }
    }

    private fun handleDateTimeChange() {
        Log.d(TAG, "Handling date/time change - triggering step count reset")

        try {
            // Get current user ID
            val userId = sessionManager.getCurrentUserId()
            if (userId != null) {
                // Trigger reset for the current user
                deviceSensorManager.resetUserStepCount(userId)
                Log.d(TAG, "Reset step count for user $userId")
            } else {
                Log.d(TAG, "No user logged in, skipping step count reset")
            }
            
            // Generate activity card for previous day, not today
            // This ensures we don't create a card with 0 steps for the new day
            val yesterday = java.time.LocalDate.now().minusDays(1)
            Log.d(TAG, "Generating activity card for previous day: $yesterday")
            
            // Use a background thread to avoid blocking the receiver
            kotlinx.coroutines.GlobalScope.launch {
                try {
                    // Just generate the card directly - the repository layer will handle deduplication
                    Log.d(TAG, "Requesting card generation for $yesterday")
                    activityCardScheduler.generateCardForDate(yesterday)
                } catch (e: Exception) {
                    Log.e(TAG, "Error in background card generation", e)
                }
            }
            
            Log.d(TAG, "Step count reset completed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to handle date change operations", e)
        }
    }
}