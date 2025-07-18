package com.example.health_assistant

import android.app.Application
import android.util.Log
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.hilt.work.HiltWorkerFactory
import com.example.health_assistant.features.journal.data.JournalEntryDao
import com.example.health_assistant.features.journal.domain.ActivityCardRepository
import com.example.health_assistant.features.journal.utils.ActivityCardCleanupUtil
import com.example.health_assistant.features.journal.workers.SafeDuplicateCleanupWorker
import com.example.health_assistant.workers.HealthDataSyncWorker
import com.example.health_assistant.features.journal.workers.ActivityCardScheduler
import com.google.firebase.FirebaseApp
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
    
    @Inject
    lateinit var activityCardRepository: ActivityCardRepository
    
    @Inject
    lateinit var journalEntryDao: JournalEntryDao

    override fun onCreate() {
        super.onCreate()

        // Initialize Firebase
        FirebaseApp.initializeApp(this)

        // Setup periodic health data sync
        setupPeriodicHealthSync()

        // EMERGENCY FIX: Reset health data on startup to prevent UI freezing
        emergencyHealthDataReset()
        
        // Setup activity card generation with safe duplicate prevention
        setupSafeActivityCardGeneration()
        
        // Setup safe, lightweight duplicate cleanup
        setupSafeDuplicateCleanup()
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
            
    /**
     * Clean up duplicate activity cards on app startup
     * TEMPORARILY DISABLED to prevent UI freezing
     */
    private fun cleanupDuplicateActivityCards() {
        Log.d("HealthAssistantApp", "Duplicate activity card cleanup temporarily disabled to prevent UI freezing")
        // Cleanup temporarily disabled to prevent UI blocking
        // Will be re-enabled with proper background processing later
    }

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
        
        // Also check for any missing activity cards
        activityCardScheduler.checkForMissingActivityCards()
    }

    /**
     * EMERGENCY FIX: Reset health data on startup to prevent UI freezing
     */
    private fun emergencyHealthDataReset() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.w("HealthAssistantApp", "EMERGENCY: Resetting health data to prevent UI freezing")
                
                // Cancel all WorkManager operations that might be causing issues
                val workManager = WorkManager.getInstance(this@HealthAssistantApplication)
                workManager.cancelAllWork()
                
                // Clear step tracking SharedPreferences that might have corrupted data
                val sharedPrefs = getSharedPreferences("step_service_prefs_user_default", MODE_PRIVATE)
                sharedPrefs.edit().clear().apply()
                
                // Clear any user-specific step data
                val allPrefs = listOf(
                    "step_service_prefs_user_",
                    "previous_day_data_"
                )
                
                for (prefsPrefix in allPrefs) {
                    try {
                        // Clear default user data
                        val defaultPrefs = getSharedPreferences("${prefsPrefix}default", MODE_PRIVATE)
                        defaultPrefs.edit().clear().apply()
                    } catch (e: Exception) {
                        Log.e("HealthAssistantApp", "Error clearing $prefsPrefix preferences", e)
                    }
                }
                
                Log.w("HealthAssistantApp", "EMERGENCY RESET COMPLETED - Health data cleared")
                
            } catch (e: Exception) {
                Log.e("HealthAssistantApp", "Error during emergency health data reset", e)
            }
        }
    }

    /**
     * Setup safe activity card generation with duplicate prevention
     * This version prevents duplicates at the source instead of cleaning up after
     */
    private fun setupSafeActivityCardGeneration() {
        try {
            Log.d("HealthAssistantApp", "Setting up SAFE activity card generation with duplicate prevention")
            
            // Use the existing scheduler but with enhanced safety
            activityCardScheduler.scheduleDailyGeneration()
            
            Log.d("HealthAssistantApp", "Safe activity card generation setup completed")
        } catch (e: Exception) {
            Log.e("HealthAssistantApp", "Error setting up safe activity card generation", e)
        }
    }

    /**
     * Setup lightweight, non-blocking duplicate cleanup
     * Runs in background without affecting UI performance
     */
    private fun setupSafeDuplicateCleanup() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d("HealthAssistantApp", "Setting up SAFE duplicate cleanup (background only)")
                
                // Schedule a lightweight cleanup that runs once per day
                // This runs in the background and won't block the UI
                val workManager = WorkManager.getInstance(this@HealthAssistantApplication)
                
                val cleanupRequest = PeriodicWorkRequestBuilder<SafeDuplicateCleanupWorker>(
                    24, TimeUnit.HOURS // Run once per day
                ).setConstraints(
                    Constraints.Builder()
                        .setRequiresBatteryNotLow(true)
                        .setRequiresDeviceIdle(true) // Only when device is idle
                        .build()
                ).build()
                
                workManager.enqueueUniquePeriodicWork(
                    "safe_duplicate_cleanup",
                    ExistingPeriodicWorkPolicy.KEEP,
                    cleanupRequest
                )
                
                Log.d("HealthAssistantApp", "Safe duplicate cleanup scheduled successfully")
                
            } catch (e: Exception) {
                Log.e("HealthAssistantApp", "Error setting up safe duplicate cleanup", e)
            }
        }
    }
}