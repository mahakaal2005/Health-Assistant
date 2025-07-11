package com.example.health_assistant.utils

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.health_assistant.R
import com.example.health_assistant.main.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Centralized notification manager for Health Assistant app
 * Handles step tracker notifications with Material You design
 */
@Singleton
class HealthNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val CHANNEL_ID_STEP_TRACKER = "step_tracker_channel"
        private const val CHANNEL_ID_HEALTH_SUMMARY = "health_summary_channel"
        private const val NOTIFICATION_ID_STEP_PROGRESS = 1001
        private const val NOTIFICATION_ID_DAILY_SUMMARY = 1002
        private const val NOTIFICATION_ID_MOTIVATION = 1003

        // Step milestone thresholds
        private const val MILESTONE_25_PERCENT = 0.25f
        private const val MILESTONE_50_PERCENT = 0.50f
        private const val MILESTONE_75_PERCENT = 0.75f
        private const val MILESTONE_100_PERCENT = 1.0f
    }

    private val notificationManager = NotificationManagerCompat.from(context)

    init {
        createNotificationChannels()
        android.util.Log.d("HealthNotificationManager", "NotificationManager initialized")
    }

    /**
     * Creates notification channels for different types of health notifications
     */
    private fun createNotificationChannels() {
        android.util.Log.d("HealthNotificationManager", "Creating notification channels...")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Step Tracker Channel
            val stepTrackerChannel = NotificationChannel(
                CHANNEL_ID_STEP_TRACKER,
                "Step Tracker",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for step tracking progress and milestones"
                enableLights(true)
                lightColor = Color.BLUE
                enableVibration(true)
                setShowBadge(true)
            }

            // Health Summary Channel
            val healthSummaryChannel = NotificationChannel(
                CHANNEL_ID_HEALTH_SUMMARY,
                "Health Summary",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Daily health summaries and reports"
                enableLights(false)
                enableVibration(false)
                setShowBadge(true)
            }

            val systemNotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            systemNotificationManager.createNotificationChannel(stepTrackerChannel)
            systemNotificationManager.createNotificationChannel(healthSummaryChannel)

            android.util.Log.d("HealthNotificationManager", "Channels created successfully")
        } else {
            android.util.Log.d("HealthNotificationManager", "Android version < 8, no channels needed")
        }
    }

    /**
     * Shows a step milestone notification
     */
    fun showStepMilestoneNotification(
        currentSteps: Int,
        goalSteps: Int,
        milestonePercentage: Float
    ) {
        if (!areNotificationsEnabled()) {
            android.util.Log.d("HealthNotificationManager", "Notifications are disabled")
            return
        }

        // Fix: Prevent division by zero
        if (goalSteps <= 0) {
            android.util.Log.w("HealthNotificationManager", "Invalid goal steps: $goalSteps")
            return
        }

        val progressPercentage = ((currentSteps.toFloat() / goalSteps) * 100).toInt().coerceAtMost(100)
        android.util.Log.d("HealthNotificationManager", "Creating milestone notification: $currentSteps/$goalSteps ($progressPercentage%)")

        val notification = createStepNotification(
            title = "Step Tracker",
            subtitle = "Daily Steps",
            content = "$currentSteps steps",
            progressText = "$progressPercentage% of $goalSteps",
            progressMax = goalSteps,
            progressCurrent = currentSteps,
            motivationalText = getMilestoneMessage(milestonePercentage)
        )

        try {
            android.util.Log.d("HealthNotificationManager", "Sending milestone notification with ID: $NOTIFICATION_ID_STEP_PROGRESS")
            notificationManager.notify(NOTIFICATION_ID_STEP_PROGRESS, notification)
            android.util.Log.i("HealthNotificationManager", "Milestone notification sent successfully")
        } catch (e: SecurityException) {
            android.util.Log.w("HealthNotificationManager", "Failed to show notification - Security: ${e.message}")
        } catch (e: Exception) {
            android.util.Log.e("HealthNotificationManager", "Unexpected error showing notification", e)
        }
    }

    /**
     * Shows daily step summary notification
     */
    fun showDailySummaryNotification(
        totalSteps: Int,
        goalSteps: Int,
        streakDays: Int = 0
    ) {
        if (!areNotificationsEnabled()) {
            android.util.Log.d("HealthNotificationManager", "Notifications are disabled for daily summary")
            return
        }

        // Fix: Prevent division by zero
        if (goalSteps <= 0) {
            android.util.Log.w("HealthNotificationManager", "Invalid goal steps for summary: $goalSteps")
            return
        }

        val goalAchieved = totalSteps >= goalSteps
        val summaryText = if (goalAchieved) {
            "🎉 Goal achieved! $streakDays day streak"
        } else {
            "Keep going! ${goalSteps - totalSteps} steps to goal"
        }

        val progressPercentage = (totalSteps * 100 / goalSteps).coerceAtMost(100)
        android.util.Log.d("HealthNotificationManager", "Creating daily summary: $totalSteps/$goalSteps")

        val notification = createStepNotification(
            title = "Daily Step Summary",
            subtitle = "Your Progress Today",
            content = "$totalSteps steps",
            progressText = if (goalAchieved) "Goal completed!" else "$progressPercentage% of goal",
            progressMax = goalSteps,
            progressCurrent = totalSteps,
            motivationalText = summaryText,
            channelId = CHANNEL_ID_HEALTH_SUMMARY
        )

        try {
            android.util.Log.d("HealthNotificationManager", "Sending daily summary notification")
            notificationManager.notify(NOTIFICATION_ID_DAILY_SUMMARY, notification)
            android.util.Log.i("HealthNotificationManager", "Daily summary notification sent successfully")
        } catch (e: SecurityException) {
            android.util.Log.w("HealthNotificationManager", "Failed to show daily summary - Security: ${e.message}")
        } catch (e: Exception) {
            android.util.Log.e("HealthNotificationManager", "Unexpected error showing daily summary", e)
        }
    }

    /**
     * Shows motivational reminder notification
     */
    fun showMotivationalReminder(currentSteps: Int, goalSteps: Int) {
        if (!areNotificationsEnabled()) {
            android.util.Log.d("HealthNotificationManager", "Notifications are disabled for motivational reminder")
            return
        }

        if (goalSteps <= 0) {
            android.util.Log.w("HealthNotificationManager", "Invalid goal steps for reminder: $goalSteps")
            return
        }

        val remainingSteps = (goalSteps - currentSteps).coerceAtLeast(0)
        val motivationalText = when {
            currentSteps >= goalSteps -> "Congratulations! You've reached your goal! 🎉"
            remainingSteps <= 1000 -> "Almost there! Just $remainingSteps steps to your goal!"
            remainingSteps <= 3000 -> "You're doing great! $remainingSteps steps to go!"
            else -> "Time to move! Every step counts towards your health."
        }

        android.util.Log.d("HealthNotificationManager", "Creating motivational reminder: $currentSteps/$goalSteps")

        val notification = createStepNotification(
            title = "Step Reminder",
            subtitle = "Stay Active",
            content = "$currentSteps steps today",
            progressText = if (currentSteps >= goalSteps) "Goal achieved!" else "$remainingSteps steps to goal",
            progressMax = goalSteps,
            progressCurrent = currentSteps,
            motivationalText = motivationalText
        )

        try {
            android.util.Log.d("HealthNotificationManager", "Sending motivational reminder notification")
            notificationManager.notify(NOTIFICATION_ID_MOTIVATION, notification)
            android.util.Log.i("HealthNotificationManager", "Motivational reminder sent successfully")
        } catch (e: SecurityException) {
            android.util.Log.w("HealthNotificationManager", "Failed to show reminder - Security: ${e.message}")
        } catch (e: Exception) {
            android.util.Log.e("HealthNotificationManager", "Unexpected error showing reminder", e)
        }
    }

    /**
     * Creates a step tracking notification with progress bar
     */
    private fun createStepNotification(
        title: String,
        subtitle: String,
        content: String,
        progressText: String,
        progressMax: Int,
        progressCurrent: Int,
        motivationalText: String,
        channelId: String = CHANNEL_ID_STEP_TRACKER
    ): android.app.Notification {

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to", "home")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notifications) // Will create this icon
            .setContentTitle(title)
            .setContentText(subtitle)
            .setSubText(getCurrentTime())
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$content\n$progressText\n$motivationalText")
                    .setBigContentTitle(title)
                    .setSummaryText(subtitle)
            )
            .setProgress(progressMax, progressCurrent, false)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    /**
     * Gets appropriate motivational message based on milestone
     */
    private fun getMilestoneMessage(milestonePercentage: Float): String {
        return when (milestonePercentage) {
            MILESTONE_25_PERCENT -> "Great start! You're 25% there! 🚀"
            MILESTONE_50_PERCENT -> "Halfway there! Keep it up! 💪"
            MILESTONE_75_PERCENT -> "Almost there! You're crushing it! 🔥"
            MILESTONE_100_PERCENT -> "Goal achieved! You're amazing! 🎉"
            else -> "Every step counts! Keep moving! 👟"
        }
    }

    /**
     * Gets current time in HH:MM format
     */
    private fun getCurrentTime(): String {
        val calendar = java.util.Calendar.getInstance()
        val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        val minute = calendar.get(java.util.Calendar.MINUTE)
        return String.format("%02d:%02d", hour, minute)
    }

    /**
     * Checks if notifications are enabled for the app
     */
    private fun areNotificationsEnabled(): Boolean {
        val enabled = notificationManager.areNotificationsEnabled()
        android.util.Log.d("HealthNotificationManager", "Notifications enabled: $enabled")
        return enabled
    }

    /**
     * Checks if a specific milestone notification should be shown
     * (to avoid duplicate notifications)
     */
    fun shouldShowMilestoneNotification(
        currentSteps: Int,
        goalSteps: Int,
        lastNotifiedSteps: Int
    ): Float? {
        val currentProgress = currentSteps.toFloat() / goalSteps
        val lastProgress = lastNotifiedSteps.toFloat() / goalSteps

        return when {
            currentProgress >= MILESTONE_100_PERCENT && lastProgress < MILESTONE_100_PERCENT -> MILESTONE_100_PERCENT
            currentProgress >= MILESTONE_75_PERCENT && lastProgress < MILESTONE_75_PERCENT -> MILESTONE_75_PERCENT
            currentProgress >= MILESTONE_50_PERCENT && lastProgress < MILESTONE_50_PERCENT -> MILESTONE_50_PERCENT
            currentProgress >= MILESTONE_25_PERCENT && lastProgress < MILESTONE_25_PERCENT -> MILESTONE_25_PERCENT
            else -> null
        }
    }

    /**
     * Cancels all step-related notifications
     */
    fun cancelAllStepNotifications() {
        notificationManager.cancel(NOTIFICATION_ID_STEP_PROGRESS)
        notificationManager.cancel(NOTIFICATION_ID_DAILY_SUMMARY)
        notificationManager.cancel(NOTIFICATION_ID_MOTIVATION)
    }

    /**
     * Test method to verify notification system is working
     */
    fun sendTestNotification() {
        android.util.Log.d("HealthNotificationManager", "=== STARTING TEST NOTIFICATION ===")

        if (!areNotificationsEnabled()) {
            android.util.Log.w("HealthNotificationManager", "Cannot send test - notifications disabled")
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val systemNotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = systemNotificationManager.getNotificationChannel(CHANNEL_ID_STEP_TRACKER)
            if (channel != null) {
                android.util.Log.d("HealthNotificationManager", "Channel exists: ${channel.name}, importance: ${channel.importance}")

                if (channel.importance == NotificationManager.IMPORTANCE_NONE) {
                    android.util.Log.w("HealthNotificationManager", "Channel importance is NONE - notifications disabled")
                    return
                }
            } else {
                android.util.Log.e("HealthNotificationManager", "Channel does not exist!")
                return
            }
        }

        try {
            android.util.Log.d("HealthNotificationManager", "Creating test notification...")

            val simpleNotification = NotificationCompat.Builder(context, CHANNEL_ID_STEP_TRACKER)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Health Assistant Test")
                .setContentText("Test notification working!")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true)
                .build()

            notificationManager.notify(8888, simpleNotification)
            android.util.Log.i("HealthNotificationManager", "Test notification sent successfully")

        } catch (e: Exception) {
            android.util.Log.e("HealthNotificationManager", "Test notification failed", e)
        }
    }
}