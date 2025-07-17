package com.example.health_assistant.features.journal.workers

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.health_assistant.auth.session.SessionManager
import com.example.health_assistant.features.journal.domain.usecase.GetActivityCardsUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Worker that checks for missing activity cards and generates them
 * This ensures we have continuous activity data even if the midnight trigger was missed
 */
@HiltWorker
class ActivityCardMissingCheckWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val getActivityCardsUseCase: GetActivityCardsUseCase,
    private val activityCardScheduler: ActivityCardScheduler,
    private val sessionManager: SessionManager
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "ActivityCardMissingCheck"
        private const val MAX_DAYS_TO_CHECK = 7 // Check up to 7 days back
    }

    override suspend fun doWork(): Result {
        try {
            Log.d(TAG, "Starting check for missing activity cards")
            
            // Get specific userId if provided in input data
            val specificUserId = inputData.getString("user_id")
            val currentUserId = specificUserId ?: sessionManager.getCurrentUserId() ?: ""
            
            Log.d(TAG, "Using user ID: $currentUserId")
            
            // Get today's date
            val today = LocalDate.now()
            
            // Check the last MAX_DAYS_TO_CHECK days for missing cards
            for (i in 1..MAX_DAYS_TO_CHECK) {
                val dateToCheck = today.minusDays(i.toLong())
                
                Log.d(TAG, "Checking for activity card on date: $dateToCheck")
                
                // Check if card exists for this date
                val hasCard = getActivityCardsUseCase.hasActivityCardForDate(dateToCheck)
                
                if (!hasCard) {
                    Log.d(TAG, "Missing activity card found for date: $dateToCheck")
                    
                    // Double-check again to prevent race conditions
                    kotlinx.coroutines.delay(100)
                    val stillMissing = !getActivityCardsUseCase.hasActivityCardForDate(dateToCheck)
                    
                    if (stillMissing) {
                        Log.d(TAG, "Confirmed missing card for $dateToCheck - generating now")
                        
                        // Generate card for this date
                        activityCardScheduler.generateCardForDate(dateToCheck)
                        
                        // Add a small delay to prevent overwhelming the system
                        kotlinx.coroutines.delay(500)
                    } else {
                        Log.d(TAG, "Card for $dateToCheck was created by another process - skipping")
                    }
                } else {
                    Log.d(TAG, "Activity card already exists for date: $dateToCheck - skipping")
                }
            }
            
            Log.d(TAG, "Completed check for missing activity cards")
            return Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error checking for missing activity cards", e)
            return Result.failure()
        }
    }
} 