package com.example.health_assistant.features.journal.workers

import android.content.Context
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

    /**
     * Schedule daily activity card generation at midnight
     */
    fun scheduleDailyGeneration() {
        val workManager = WorkManager.getInstance(context)

        // Cancel any existing work
        workManager.cancelUniqueWork(ActivityCardGeneratorWorker.WORK_NAME)

        // Calculate initial delay until next midnight
        val now = LocalDateTime.now()
        val nextMidnight = now.toLocalDate().plusDays(1).atTime(LocalTime.MIDNIGHT)
        val initialDelay = ChronoUnit.MINUTES.between(now, nextMidnight)

        // Create periodic work request for daily execution
        val workRequest = PeriodicWorkRequestBuilder<ActivityCardGeneratorWorker>(
            24, TimeUnit.HOURS,
            15, TimeUnit.MINUTES // Flex interval
        )
            .setInitialDelay(initialDelay, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                    .setRequiresBatteryNotLow(true)
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
}