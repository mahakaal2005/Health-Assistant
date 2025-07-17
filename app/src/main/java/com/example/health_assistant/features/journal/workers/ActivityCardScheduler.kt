package com.example.health_assistant.features.journal.workers

import android.content.Context
import android.util.Log
import androidx.work.*
import com.example.health_assistant.auth.session.SessionManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Scheduler for Activity Card generation worker
 * Handles automatic daily scheduling at midnight
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
        
        // Create input data with user ID
        val inputData = workDataOf("user_id" to currentUserId)

        // Create periodic work request for daily execution with relaxed constraints
        val workRequest = PeriodicWorkRequestBuilder<ActivityCardGeneratorWorker>(
            24, TimeUnit.HOURS,
            6, TimeUnit.HOURS // Larger flex interval for better reliability
        )
            .setInitialDelay(initialDelayMinutes, TimeUnit.MINUTES)
            .setInputData(inputData)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                    .setRequiresBatteryNotLow(false) // Remove battery constraint
                    .setRequiresDeviceIdle(false)
                    .build()
            )
            .addTag("activity_card")
            .build()

        // Schedule the work
        workManager.enqueueUniquePeriodicWork(
            ActivityCardGeneratorWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.REPLACE,
            workRequest
        )

        Log.d(TAG, "Scheduled daily activity card generation")

        // Also schedule a backup one-time work for tomorrow in case periodic fails
        scheduleBackupGeneration()
    }

    /**
     * Check for missing activity cards and generate them
     * This ensures we have cards even if the midnight trigger was missed
     */
    fun checkForMissingActivityCards() {
        val workManager = WorkManager.getInstance(context)
        
        // Get current user ID for the work request
        val currentUserId = sessionManager.getCurrentUserId() ?: ""
        
        // Create input data with user ID
        val inputData = workDataOf("user_id" to currentUserId)
        
        // Create a one-time work request to check for missing cards
        val checkRequest = OneTimeWorkRequestBuilder<ActivityCardMissingCheckWorker>()
            .setInputData(inputData)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                    .build()
            )
            .addTag("activity_card_missing_check")
            .build()
            
        // Enqueue the work
        workManager.enqueue(checkRequest)
        
        Log.d(TAG, "Scheduled check for missing activity cards")
    }

    /**
     * Generate activity card for a specific date
     * Used to fill in missing cards
     */
    fun generateCardForDate(date: java.time.LocalDate) {
        val workManager = WorkManager.getInstance(context)
        
        // Get current user ID for the work request
        val currentUserId = sessionManager.getCurrentUserId() ?: ""
        
        // Create input data with the date and user ID
        val inputData = workDataOf(
            "specific_date_generation" to true,
            "year" to date.year,
            "month" to date.monthValue,
            "day" to date.dayOfMonth,
            "check_existing" to true,  // Flag to check if card already exists
            "user_id" to currentUserId
        )
        
        // Create one-time work request for the specific date
        val dateRequest = OneTimeWorkRequestBuilder<ActivityCardGeneratorWorker>()
            .setInputData(inputData)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                    .build()
            )
            .addTag("activity_card_specific_date")
            .build()
            
        // Enqueue the work
        workManager.enqueueUniqueWork(
            "activity_card_date_${date}",
            ExistingWorkPolicy.REPLACE,
            dateRequest
        )
        
        Log.d(TAG, "Scheduled activity card generation for specific date: $date")
    }

    /**
     * Schedule backup one-time generation for tomorrow
     */
    private fun scheduleBackupGeneration() {
        val workManager = WorkManager.getInstance(context)

        val now = LocalDateTime.now()
        val tomorrow = now.toLocalDate().plusDays(1).atTime(1, 0) // 1 AM tomorrow
        val delayMinutes = ChronoUnit.MINUTES.between(now, tomorrow)

        // Get current user ID for the work request
        val currentUserId = sessionManager.getCurrentUserId() ?: ""
        
        // Create input data with user ID
        val inputData = workDataOf("user_id" to currentUserId)

        val backupRequest = OneTimeWorkRequestBuilder<ActivityCardGeneratorWorker>()
            .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
            .setInputData(inputData)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                    .setRequiresBatteryNotLow(false)
                    .build()
            )
            .addTag("activity_card_backup")
            .build()

        workManager.enqueueUniqueWork(
            "activity_card_backup_tomorrow",
            ExistingWorkPolicy.REPLACE,
            backupRequest
        )

        Log.d(TAG, "Scheduled backup activity card generation for tomorrow at 1 AM")
    }

    /**
     * Manually trigger activity card generation (for testing)
     */
    fun generateCardNow() {
        val workManager = WorkManager.getInstance(context)

        // Get current user ID for the work request
        val currentUserId = sessionManager.getCurrentUserId() ?: ""
        
        // Create input data with user ID
        val inputData = workDataOf("user_id" to currentUserId)

        val workRequest = OneTimeWorkRequestBuilder<ActivityCardGeneratorWorker>()
            .setInputData(inputData)
            .addTag("activity_card_manual")
            .build()

        workManager.enqueue(workRequest)
    }

    /**
     * Force generate activity card for testing date changes
     */
    fun forceGenerateCardForToday() {
        val workManager = WorkManager.getInstance(context)

        // Cancel any existing work first
        workManager.cancelUniqueWork(ActivityCardGeneratorWorker.WORK_NAME)
        workManager.cancelAllWorkByTag("activity_card")

        // Get current user ID for the work request
        val currentUserId = sessionManager.getCurrentUserId() ?: ""
        
        // Create input data with user ID
        val inputData = workDataOf("user_id" to currentUserId)

        // Trigger immediate generation
        val immediateRequest = OneTimeWorkRequestBuilder<ActivityCardGeneratorWorker>()
            .setInputData(inputData)
            .addTag("activity_card_force")
            .build()

        workManager.enqueue(immediateRequest)
    }

    /**
     * Cancel all activity card generation work
     */
    fun cancelScheduledGeneration() {
        val workManager = WorkManager.getInstance(context)
        workManager.cancelUniqueWork(ActivityCardGeneratorWorker.WORK_NAME)
        workManager.cancelAllWorkByTag("activity_card")
    }

    /**
     * CRITICAL FIX: Generate activity card immediately with current health data
     * This fixes the issue where cards are only generated on health preview click
     */
    fun generateImmediateCard(stepCount: Int, caloriesBurned: Int, heartPoints: Int) {
        val workManager = WorkManager.getInstance(context)

        // Get current user ID for the work request
        val currentUserId = sessionManager.getCurrentUserId() ?: ""

        // Create input data with current health metrics and user ID
        val inputData = workDataOf(
            "immediate_generation" to true,
            "step_count" to stepCount,
            "calories_burned" to caloriesBurned,
            "heart_points" to heartPoints,
            "check_existing" to true,  // Check if card already exists
            "user_id" to currentUserId
        )

        // Create one-time work request for immediate execution
        val immediateRequest = OneTimeWorkRequestBuilder<ActivityCardGeneratorWorker>()
            .setInputData(inputData)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                    .setRequiresBatteryNotLow(false)
                    .build()
            )
            .addTag("immediate_activity_card")
            .build()

        // Enqueue immediate work
        workManager.enqueueUniqueWork(
            "immediate_activity_card_${System.currentTimeMillis()}",
            ExistingWorkPolicy.REPLACE,
            immediateRequest
        )

        Log.d(TAG, "Scheduled immediate activity card generation with steps: $stepCount, calories: $caloriesBurned, heart points: $heartPoints")
    }
}