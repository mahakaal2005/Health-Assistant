package com.example.health_assistant.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.health_assistant.auth.session.SessionManager
import com.example.health_assistant.data.health.EnhancedHealthTracker
import com.example.health_assistant.data.repository.interfaces.HealthRepository
import com.example.health_assistant.data.sensors.DeviceSensorManager
import com.example.health_assistant.features.journal.workers.ActivityCardGeneratorWorker
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import java.time.LocalDate

/**
 * Broadcast receiver to handle date/time changes for midnight reset
 * Ensures proper daily step count reset when date changes
 * Now properly generates activity cards with previous day's data
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
    lateinit var healthRepository: HealthRepository
    
    @Inject
    lateinit var enhancedHealthTracker: EnhancedHealthTracker

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Date change receiver triggered: ${intent.action}")

        when (intent.action) {
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> {
                handleDateTimeChange(context)
            }
        }
    }

    private fun handleDateTimeChange(context: Context) {
        Log.d(TAG, "Handling date/time change - triggering step count reset")

        try {
            // Get current user ID
            val userId = sessionManager.getCurrentUserId()
            if (userId == null) {
                Log.d(TAG, "No user logged in, skipping date change handling")
                return
            }
            
            // Get today's date and yesterday's date
            val today = LocalDate.now()
            val yesterday = today.minusDays(1)
            val yesterdayStr = yesterday.toString()
            
            // Get previous day's data before resetting
            val previousDaySteps = deviceSensorManager.getPreviousDaySteps(userId)
            val caloriesBurned = (previousDaySteps * 0.04).toInt()
            val heartPoints = (previousDaySteps / 100).coerceAtMost(50)
            
            Log.d(TAG, "Previous day data for user $userId: Steps=$previousDaySteps, Calories=$caloriesBurned, HeartPoints=$heartPoints")
            
            // Create work data with previous day's metrics
            val workData = workDataOf(
                ActivityCardGeneratorWorker.KEY_TARGET_DATE to yesterdayStr,
                ActivityCardGeneratorWorker.KEY_USER_ID to userId,
                "steps" to previousDaySteps,
                "calories" to caloriesBurned,
                "heart_points" to heartPoints
            )
            
            // Create and enqueue work request
            val workRequest = OneTimeWorkRequestBuilder<ActivityCardGeneratorWorker>()
                .setInputData(workData)
                .addTag("date_change_activity_card")
                .build()
                
            WorkManager.getInstance(context).enqueue(workRequest)
            
            Log.d(TAG, "Scheduled activity card generation for yesterday with previous day's data")
            
            // Now reset step count for the current user
            deviceSensorManager.resetUserStepCount(userId)
            Log.d(TAG, "Reset step count for user $userId")
            
            Log.d(TAG, "Date change handling completed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to handle date change operations", e)
        }
    }
}