package com.example.health_assistant

import android.app.Application
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.hilt.work.HiltWorkerFactory
import com.example.health_assistant.workers.HealthDataSyncWorker
import com.example.health_assistant.features.journal.workers.ActivityCardScheduler
import com.google.firebase.FirebaseApp
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Application class for initializing Firebase and setting up background health sync
 */
@HiltAndroidApp
class HealthAssistantApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var activityCardScheduler: ActivityCardScheduler

    override fun onCreate() {
        super.onCreate()

        // Initialize Firebase
        FirebaseApp.initializeApp(this)

        // Setup periodic health data sync
        setupPeriodicHealthSync()

        // Setup activity card generation using Hilt-injected scheduler
        setupActivityCardGeneration()
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    /**
     * Setup periodic background sync for health data from Google Fit
     * Runs every 2 hours when device conditions are favorable
     */
    private fun setupPeriodicHealthSync() {
        val syncRequest = PeriodicWorkRequestBuilder<HealthDataSyncWorker>(
            2, TimeUnit.HOURS // Sync every 2 hours
        ).setConstraints(
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED) // Works offline
                .setRequiresBatteryNotLow(true) // Only when battery is not low
                .setRequiresDeviceIdle(false) // Can run when device is active
                .build()
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "health_data_sync",
            ExistingPeriodicWorkPolicy.KEEP, // Keep existing work if already scheduled
            syncRequest
        )
    }

    /**
     * Setup automatic daily activity card generation at midnight
     */
    private fun setupActivityCardGeneration() {
        // Use Hilt-injected scheduler instead of manual creation
        activityCardScheduler.scheduleDailyGeneration()
    }
}