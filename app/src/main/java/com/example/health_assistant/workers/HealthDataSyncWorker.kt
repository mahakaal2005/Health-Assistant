package com.example.health_assistant.workers

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.health_assistant.data.repository.interfaces.HealthRepository
import com.example.health_assistant.features.health.model.HealthMetrics
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Background worker for syncing health data from local sensors
 * Runs periodically to keep health metrics up to date
 */
@HiltWorker
class HealthDataSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val healthRepository: HealthRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "Starting health data sync...")

            // Sync today's metrics from local sensors only
            val syncResult = healthRepository.getTodayMetrics()

            if (syncResult.isSuccess) {
                Log.d(TAG, "Health data sync completed successfully")

                // Check for goal achievements and send notifications
                val metrics = syncResult.getOrNull()
                metrics?.let {
                    checkAndNotifyGoalAchievements(it)
                }

                Result.success()
            } else {
                Log.w(TAG, "Health data sync failed, will retry")
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Health data sync worker failed", e)
            Result.failure()
        }
    }

    /**
     * Check if user has achieved any goals and send notifications
     * Fixed to work with the actual HealthMetrics model structure
     */
    private fun checkAndNotifyGoalAchievements(metrics: HealthMetrics) {
        try {
            // Check step goal achievement - using correct property access
            if (metrics.steps.current >= metrics.steps.target) {
                Log.d(TAG, "Step goal achieved: ${metrics.steps.current}/${metrics.steps.target}")
                // TODO: Send notification when NotificationManager is implemented
                // notificationManager.showAchievementNotification(
                //     "Step Goal Achieved! 🎉",
                //     "You've reached your daily step goal of ${metrics.steps.target} steps!"
                // )
            }

            // Check calorie goal achievement - using correct property access
            if (metrics.calories.current >= metrics.calories.target) {
                Log.d(TAG, "Calorie goal achieved: ${metrics.calories.current}/${metrics.calories.target}")
                // TODO: Send notification when NotificationManager is implemented
                // notificationManager.showAchievementNotification(
                //     "Calorie Goal Achieved! 🔥",
                //     "You've burned ${metrics.calories.current} calories today!"
                // )
            }

            // Check heart points goal achievement - using correct property access
            if (metrics.heartPoints.current >= metrics.heartPoints.target) {
                Log.d(TAG, "Heart points goal achieved: ${metrics.heartPoints.current}/${metrics.heartPoints.target}")
                // TODO: Send notification when NotificationManager is implemented
//                 notificationManager.showAchievementNotification(
//                     "Heart Points Goal Achieved! ❤️",
//                     "You've earned ${metrics.heartPoints.current} heart points today!"
//                 )
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error checking goal achievements", e)
        }
    }

    companion object {
        private const val TAG = "HealthDataSyncWorker"
    }
}