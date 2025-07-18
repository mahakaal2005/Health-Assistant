package com.example.health_assistant.utils

import android.content.Context
import android.util.Log
import androidx.work.WorkManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Emergency utility to fix UI freezing issues
 * Use this when the app becomes unresponsive
 */
object EmergencyUIFix {
    private const val TAG = "EmergencyUIFix"
    
    /**
     * Emergency method to stop all background work and reset UI
     * Call this when the UI becomes unresponsive
     */
    fun emergencyReset(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "Starting emergency UI reset")
                
                // Cancel all WorkManager operations
                val workManager = WorkManager.getInstance(context)
                workManager.cancelAllWork()
                
                Log.d(TAG, "Cancelled all background work")
                
                // Clear any cached data that might be causing issues
                System.gc() // Force garbage collection
                
                Log.d(TAG, "Emergency reset completed")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error during emergency reset", e)
            }
        }
    }
    
    /**
     * Stop only activity card related work
     */
    fun stopActivityCardWork(context: Context) {
        try {
            val workManager = WorkManager.getInstance(context)
            workManager.cancelUniqueWork("activity_card_generator_work")
            workManager.cancelAllWorkByTag("activity_card_generation")
            workManager.cancelAllWorkByTag("activity_card_immediate_generation")
            workManager.cancelAllWorkByTag("activity_card_sample_generation")
            workManager.cancelAllWorkByTag("activity_card_backfill")
            workManager.cancelAllWorkByTag("activity_card_specific_date")
            
            Log.d(TAG, "Stopped all activity card related work")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping activity card work", e)
        }
    }
}