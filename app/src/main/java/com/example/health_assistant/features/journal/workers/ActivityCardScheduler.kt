package com.example.health_assistant.features.journal.workers

import android.content.Context
import android.util.Log
import androidx.work.*
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Scheduler for Activity Card generation worker
 * Handles automatic daily scheduling at midnight
 */
@Singleton
class ActivityCardScheduler @Inject constructor(
    @ApplicationContext private val context: Context
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

        // Calculate initial delay until next midnight
        val now = LocalDateTime.now()
        val nextMidnight = now.toLocalDate().plusDays(1).atTime(LocalTime.MIDNIGHT)
        val initialDelayMinutes = ChronoUnit.MINUTES.between(now, nextMidnight)

        Log.d(TAG, "Current time: $now")
        Log.d(TAG, "Next midnight: $nextMidnight")
        Log.d(TAG, "Initial delay: $initialDelayMinutes minutes")

        // Create periodic work request for daily execution with relaxed constraints
        val workRequest = PeriodicWorkRequestBuilder<ActivityCardGeneratorWorker>(
            24, TimeUnit.HOURS,
            6, TimeUnit.HOURS // Larger flex interval for better reliability
        )
            .setInitialDelay(initialDelayMinutes, TimeUnit.MINUTES)
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
     * Schedule backup one-time generation for tomorrow
     */
    private fun scheduleBackupGeneration() {
        val workManager = WorkManager.getInstance(context)

        val now = LocalDateTime.now()
        val tomorrow = now.toLocalDate().plusDays(1).atTime(1, 0) // 1 AM tomorrow
        val delayMinutes = ChronoUnit.MINUTES.between(now, tomorrow)

        val backupRequest = OneTimeWorkRequestBuilder<ActivityCardGeneratorWorker>()
            .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
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

        val workRequest = OneTimeWorkRequestBuilder<ActivityCardGeneratorWorker>()
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

        // Trigger immediate generation
        val immediateRequest = OneTimeWorkRequestBuilder<ActivityCardGeneratorWorker>()
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

        // Create input data with current health metrics
        val inputData = workDataOf(
            "immediate_generation" to true,
            "step_count" to stepCount,
            "calories_burned" to caloriesBurned,
            "heart_points" to heartPoints
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