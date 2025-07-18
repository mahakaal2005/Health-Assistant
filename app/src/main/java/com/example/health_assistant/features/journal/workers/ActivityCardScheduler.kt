package com.example.health_assistant.features.journal.workers

import android.content.Context
import android.util.Log
import androidx.work.*
import com.example.health_assistant.auth.session.SessionManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Scheduler for Activity Card generation worker
 * Handles automatic daily scheduling at midnight
 * Now with proper user isolation for multi-user support
 */
@Singleton
class ActivityCardScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sessionManager: SessionManager
) {

    companion object {
        private const val TAG = "ActivityCardScheduler"
    }

    /**
     * Schedule daily activity card generation at midnight
     */
    fun scheduleDailyGeneration() {
        val workManager = WorkManager.getInstance(context)

        // Cancel any existing work
        workManager.cancelUniqueWork(ActivityCardGeneratorWorker.WORK_NAME)
        Log.d(TAG, "Cancelled any existing activity card work")

        // Check for missing activity cards first
        checkForMissingActivityCards()

        // Calculate initial delay until next midnight
        val now = LocalDateTime.now()
        val nextMidnight = now.toLocalDate().plusDays(1).atTime(LocalTime.MIDNIGHT)
        val initialDelayMinutes = ChronoUnit.MINUTES.between(now, nextMidnight)

        Log.d(TAG, "Current time: $now")
        Log.d(TAG, "Next midnight: $nextMidnight")
        Log.d(TAG, "Initial delay: $initialDelayMinutes minutes")

        // Get current user ID for the work request
        val currentUserId = sessionManager.getCurrentUserId() ?: ""
        if (currentUserId.isEmpty()) {
            Log.w(TAG, "No user logged in, skipping activity card scheduling")
            return
        }
        
        // Create input data with user ID
        val inputData = workDataOf(
            ActivityCardGeneratorWorker.KEY_USER_ID to currentUserId
        )

        // Create work request
        val workRequest = OneTimeWorkRequestBuilder<ActivityCardGeneratorWorker>()
            .setInitialDelay(initialDelayMinutes, TimeUnit.MINUTES)
            .setInputData(inputData)
            .setConstraints(
                Constraints.Builder()
                    .setRequiresBatteryNotLow(true)
                    .build()
            )
            .addTag("activity_card_generation")
            .build()

        // Enqueue unique work
        workManager.enqueueUniqueWork(
            ActivityCardGeneratorWorker.WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            workRequest
        )

        Log.d(TAG, "Scheduled activity card generation for user $currentUserId at $nextMidnight (in $initialDelayMinutes minutes)")
    }

    /**
     * Force generate activity card for today (for testing)
     */
    fun forceGenerateCardForToday() {
        val workManager = WorkManager.getInstance(context)

        // Get current user ID
        val currentUserId = sessionManager.getCurrentUserId() ?: ""
        if (currentUserId.isEmpty()) {
            Log.w(TAG, "No user logged in, skipping activity card generation")
            return
        }

        // Create input data with today's date and user ID
        val inputData = workDataOf(
            ActivityCardGeneratorWorker.KEY_TARGET_DATE to LocalDate.now().toString(),
            ActivityCardGeneratorWorker.KEY_USER_ID to currentUserId
        )

        // Create immediate work request
        val workRequest = OneTimeWorkRequestBuilder<ActivityCardGeneratorWorker>()
            .setInputData(inputData)
            .addTag("activity_card_immediate_generation")
            .build()

        // Enqueue the work
        workManager.enqueue(workRequest)

        Log.d(TAG, "Forced activity card generation for today for user $currentUserId")
    }

    /**
     * Check for missing activity cards and generate them
     */
    fun checkForMissingActivityCards() {
        val workManager = WorkManager.getInstance(context)
        
        // Get current user ID
        val currentUserId = sessionManager.getCurrentUserId() ?: ""
        if (currentUserId.isEmpty()) {
            Log.w(TAG, "No user logged in, skipping missing activity card check")
            return
        }

        // Check for yesterday's card
        val yesterday = LocalDate.now().minusDays(1)
        
        // Create input data with yesterday's date and user ID
        val inputData = workDataOf(
            ActivityCardGeneratorWorker.KEY_TARGET_DATE to yesterday.toString(),
            ActivityCardGeneratorWorker.KEY_USER_ID to currentUserId
        )

        // Create work request for yesterday's card
        val workRequest = OneTimeWorkRequestBuilder<ActivityCardGeneratorWorker>()
            .setInputData(inputData)
            .addTag("activity_card_backfill")
            .build()

        // Enqueue the work
        workManager.enqueue(workRequest)

        Log.d(TAG, "Checking for missing activity card for yesterday (${yesterday}) for user $currentUserId")
    }

    /**
     * Generate activity card for a specific date
     * Used to fill in missing cards
     */
    fun generateCardForDate(date: LocalDate) {
        val workManager = WorkManager.getInstance(context)
        
        // Get current user ID
        val currentUserId = sessionManager.getCurrentUserId() ?: ""
        if (currentUserId.isEmpty()) {
            Log.w(TAG, "No user logged in, skipping activity card generation for date $date")
            return
        }

        // Create input data with date and user ID
        val inputData = workDataOf(
            ActivityCardGeneratorWorker.KEY_TARGET_DATE to date.toString(),
            ActivityCardGeneratorWorker.KEY_USER_ID to currentUserId
        )
        
        // Create work request for specific date
        val workRequest = OneTimeWorkRequestBuilder<ActivityCardGeneratorWorker>()
            .setInputData(inputData)
            .addTag("activity_card_specific_date")
            .build()
            
        // Enqueue the work with a unique name
        workManager.enqueueUniqueWork(
            "activity_card_date_${date}",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
        
        Log.d(TAG, "Scheduled activity card generation for date $date and user $currentUserId")
    }
}