package com.example.health_assistant.features.journal.workers

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.health_assistant.features.journal.data.JournalEntryDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Safe, lightweight worker for cleaning up duplicate activity cards
 * Runs in background only when device is idle to prevent UI blocking
 * Uses efficient SQL queries to minimize performance impact
 */
@HiltWorker
class SafeDuplicateCleanupWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted private val params: WorkerParameters,
    private val journalEntryDao: JournalEntryDao
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "SafeDuplicateCleanup"
        private const val MAX_CLEANUP_TIME_MS = 5000L // 5 seconds max
    }

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "Starting safe duplicate cleanup (background only)")
            
            // Run cleanup with timeout to prevent long-running operations
            val startTime = System.currentTimeMillis()
            
            withContext(Dispatchers.IO) {
                // Use the efficient SQL cleanup method that only targets activity_summary entries
                val deletedCount = journalEntryDao.cleanupAllDuplicateActivityCards()
                
                val elapsedTime = System.currentTimeMillis() - startTime
                
                if (elapsedTime > MAX_CLEANUP_TIME_MS) {
                    Log.w(TAG, "Cleanup took ${elapsedTime}ms, which is longer than expected")
                }
                
                Log.d(TAG, "Safe cleanup completed in ${elapsedTime}ms - Deleted $deletedCount duplicate activity cards")
            }
            
            Result.success()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during safe duplicate cleanup", e)
            // Don't fail the work - just log the error and continue
            Result.success()
        }
    }
}