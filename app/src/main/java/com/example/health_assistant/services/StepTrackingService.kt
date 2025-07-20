package com.example.health_assistant.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.edit
import com.example.health_assistant.main.MainActivity
import com.example.health_assistant.R
import com.example.health_assistant.data.sensors.DeviceSensorManager
import com.example.health_assistant.utils.HealthNotificationManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * Background service for continuous step tracking
 * Ensures step counting works 24/7 even when app is closed
 * Handles midnight resets and battery optimization
 */
@AndroidEntryPoint
class StepTrackingService : Service(), SensorEventListener {

    companion object {
        private const val TAG = "StepTrackingService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "step_tracking_channel"
        private const val CHANNEL_NAME = "Step Tracking"
        private const val PREFS_NAME = "step_service_prefs"
        private const val KEY_DAILY_STEPS = "service_daily_steps"
        private const val KEY_LAST_DATE = "service_last_date"
        private const val KEY_INITIAL_STEP_COUNT = "service_initial_step_count"
        private const val MIDNIGHT_CHECK_INTERVAL = 60_000L // Check every minute
    }

    @Inject
    lateinit var deviceSensorManager: DeviceSensorManager

    @Inject
    lateinit var healthNotificationManager: HealthNotificationManager

    private lateinit var sensorManager: SensorManager
    private lateinit var notificationManager: NotificationManager
    private lateinit var powerManager: PowerManager
    private lateinit var wakeLock: PowerManager.WakeLock

    private var stepCounterSensor: Sensor? = null
    private var stepDetectorSensor: Sensor? = null
    private var isServiceRunning = false
    private var initialStepCount = 0L
    private var currentDate: String = getCurrentDateString()

    // Notification tracking variables
    private var lastNotifiedSteps = 0
    private var lastNotificationTime = 0L
    private val stepGoal = 10000 // Default step goal

    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    private var midnightCheckJob: Job? = null

    // Broadcast receiver for date changes and device events
    private val dateChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_DATE_CHANGED,
                Intent.ACTION_TIME_CHANGED,
                Intent.ACTION_TIMEZONE_CHANGED -> {
                    Log.d(TAG, "Date/time change detected via broadcast")
                    handleMidnightReset()
                }
                Intent.ACTION_BOOT_COMPLETED,
                Intent.ACTION_MY_PACKAGE_REPLACED -> {
                    Log.d(TAG, "Device boot/app update detected")
                    restartService()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")

        initializeService()
        createNotificationChannel()
        registerBroadcastReceivers()
        startMidnightCheckJob()
        
        Log.d(TAG, "Service initialization complete - notifications will work in background")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service start command received")

        if (!isServiceRunning) {
            startForegroundService()
            startStepTracking()
            
            // ADDED: Sync with actual step count on service start
            syncWithDeviceSensorManager()
            
            isServiceRunning = true
        }

        // Return START_STICKY to restart service if killed by system
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        Log.d(TAG, "Service destroyed")

        stopStepTracking()
        unregisterBroadcastReceivers()
        releaseLocks()
        stopMidnightCheckJob()

        super.onDestroy()
    }

    /**
     * Initialize service components
     */
    private fun initializeService() {
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager

        stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        stepDetectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)

        // Acquire partial wake lock for background processing
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "$TAG::StepTrackingWakeLock"
        )

        loadSavedData()
    }

    /**
     * Start foreground service with notification
     */
    private fun startForegroundService() {
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)

        if (!wakeLock.isHeld) {
            wakeLock.acquire(10*60*1000L) // 10 minutes
        }

        Log.d(TAG, "Foreground service started")
    }

    /**
     * Start step tracking sensors
     */
    private fun startStepTracking() {
        // Register step counter sensor (preferred)
        stepCounterSensor?.let { sensor ->
            val registered = sensorManager.registerListener(
                this,
                sensor,
                SensorManager.SENSOR_DELAY_NORMAL
            )

            if (registered) {
                Log.d(TAG, "Step counter sensor registered in service")
                return
            }
        }

        // Fallback to step detector
        stepDetectorSensor?.let { sensor ->
            val registered = sensorManager.registerListener(
                this,
                sensor,
                SensorManager.SENSOR_DELAY_NORMAL
            )

            if (registered) {
                Log.d(TAG, "Step detector sensor registered in service (fallback)")
            } else {
                Log.e(TAG, "Failed to register any step sensor in service")
            }
        }
    }

    /**
     * Stop step tracking sensors
     */
    private fun stopStepTracking() {
        sensorManager.unregisterListener(this)
        isServiceRunning = false
        Log.d(TAG, "Step tracking stopped in service")
    }

    /**
     * Handle sensor data changes
     */
    override fun onSensorChanged(event: SensorEvent?) {
        event?.let { sensorEvent ->
            when (sensorEvent.sensor.type) {
                Sensor.TYPE_STEP_COUNTER -> {
                    handleStepCounterData(sensorEvent.values[0].toLong())
                }
                Sensor.TYPE_STEP_DETECTOR -> {
                    handleStepDetectorData()
                }
            }
        }
    }

    /**
     * Handle step counter sensor data
     * FIXED: Sync with DeviceSensorManager to show correct step count
     */
    private fun handleStepCounterData(totalSteps: Long) {
        checkForDateChange()

        // Get the actual daily steps from DeviceSensorManager
        val actualDailySteps = deviceSensorManager.stepCount.value
        
        // Update with the actual step count instead of calculating our own
        updateDailySteps(actualDailySteps)
        
        Log.d(TAG, "Synced with DeviceSensorManager: $actualDailySteps steps")
    }

    /**
     * Handle step detector sensor data
     * FIXED: Sync with DeviceSensorManager instead of incrementing our own counter
     */
    private fun handleStepDetectorData() {
        checkForDateChange()
        
        // Get the actual daily steps from DeviceSensorManager
        val actualDailySteps = deviceSensorManager.stepCount.value
        updateDailySteps(actualDailySteps)
        
        Log.d(TAG, "Step detected - synced count: $actualDailySteps steps")
    }

    /**
     * Update daily step count
     */
    private fun updateDailySteps(steps: Int) {
        val clampedSteps = maxOf(0, steps)
        saveDailySteps(clampedSteps)
        updateNotification(clampedSteps)

        // Check for step milestone notifications
        checkAndSendStepNotifications(clampedSteps)

        Log.d(TAG, "Service updated daily steps: $clampedSteps")
    }

    /**
     * Check for date change and handle midnight reset
     */
    private fun checkForDateChange() {
        val today = getCurrentDateString()

        if (currentDate != today) {
            Log.d(TAG, "Service detected date change: $currentDate -> $today")
            handleMidnightReset()
        }
    }

    /**
     * Handle midnight reset
     */
    private fun handleMidnightReset() {
        currentDate = getCurrentDateString()

        // Reset notification tracking
        lastNotifiedSteps = 0
        lastNotificationTime = 0L

        // Sync with DeviceSensorManager for accurate step count after midnight
        syncWithDeviceSensorManager()

        Log.d(TAG, "Service performed midnight reset and synced step count")
    }

    /**
     * Start midnight check job and periodic step sync
     */
    private fun startMidnightCheckJob() {
        midnightCheckJob = serviceScope.launch {
            while (isServiceRunning) {
                try {
                    checkForDateChange()
                    
                    // ADDED: Periodic sync with DeviceSensorManager to ensure notification accuracy
                    syncWithDeviceSensorManager()
                    
                    delay(MIDNIGHT_CHECK_INTERVAL)
                } catch (e: Exception) {
                    Log.e(TAG, "Error in midnight check job", e)
                    delay(MIDNIGHT_CHECK_INTERVAL)
                }
            }
        }
    }

    /**
     * Sync step count with DeviceSensorManager to ensure notification accuracy
     */
    private fun syncWithDeviceSensorManager() {
        try {
            val actualSteps = deviceSensorManager.stepCount.value
            val currentNotificationSteps = getCurrentDailySteps()
            
            // Only update if there's a significant difference
            if (kotlin.math.abs(actualSteps - currentNotificationSteps) > 0) {
                Log.d(TAG, "Syncing notification: $currentNotificationSteps → $actualSteps steps")
                updateDailySteps(actualSteps)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing with DeviceSensorManager", e)
        }
    }

    /**
     * Stop midnight check job
     */
    private fun stopMidnightCheckJob() {
        midnightCheckJob?.cancel()
        midnightCheckJob = null
    }

    /**
     * Register broadcast receivers
     */
    private fun registerBroadcastReceivers() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_DATE_CHANGED)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
            addAction(Intent.ACTION_BOOT_COMPLETED)
            addAction(Intent.ACTION_MY_PACKAGE_REPLACED)
        }

        registerReceiver(dateChangeReceiver, filter)
        Log.d(TAG, "Broadcast receivers registered")
    }

    /**
     * Unregister broadcast receivers
     */
    private fun unregisterBroadcastReceivers() {
        try {
            unregisterReceiver(dateChangeReceiver)
            Log.d(TAG, "Broadcast receivers unregistered")
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering broadcast receivers", e)
        }
    }

    /**
     * Release locks and resources
     */
    private fun releaseLocks() {
        if (wakeLock.isHeld) {
            wakeLock.release()
        }
    }

    /**
     * Restart service (for boot completed events)
     */
    private fun restartService() {
        serviceScope.launch {
            delay(5000) // Wait 5 seconds after boot

            if (getSavedServiceState()) {
                val intent = Intent(this@StepTrackingService, StepTrackingService::class.java)
                startForegroundService(intent)
            }
        }
    }

    /**
     * Create notification channel for Android O+
     */
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Notification for step tracking service"
            setShowBadge(false)
            enableLights(false)
            enableVibration(false)
        }

        notificationManager.createNotificationChannel(channel)
    }

    /**
     * Create service notification
     */
    private fun createNotification(stepCount: Int = getCurrentDailySteps()): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Health Assistant")
            .setContentText("$stepCount steps today")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .setShowWhen(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    /**
     * Update notification with current step count
     */
    private fun updateNotification(stepCount: Int) {
        try {
            val notification = createNotification(stepCount)
            notificationManager.notify(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating notification", e)
        }
    }

    /**
     * Data persistence methods
     */
    private fun saveDailySteps(steps: Int) {
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putInt(KEY_DAILY_STEPS, steps)
        }
    }

    private fun saveCurrentDate(date: String) {
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putString(KEY_LAST_DATE, date)
        }
    }

    private fun saveInitialStepCount(count: Long) {
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putLong(KEY_INITIAL_STEP_COUNT, count)
        }
    }

    private fun saveNotificationState() {
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putInt("last_notified_steps", lastNotifiedSteps)
            putLong("last_notification_time", lastNotificationTime)
        }
    }

    private fun getCurrentDailySteps(): Int {
        return getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_DAILY_STEPS, 0)
    }

    private fun getSavedServiceState(): Boolean {
        return getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean("service_enabled", false)
    }

    private fun loadSavedData() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        currentDate = prefs.getString(KEY_LAST_DATE, getCurrentDateString()) ?: getCurrentDateString()
        
        // Load notification tracking state
        lastNotifiedSteps = prefs.getInt("last_notified_steps", 0)
        lastNotificationTime = prefs.getLong("last_notification_time", 0L)
        
        // REMOVED: initialStepCount logic since we sync directly with DeviceSensorManager
        Log.d(TAG, "Loaded saved data - will sync with DeviceSensorManager for accurate step count")
    }

    private fun getCurrentDateString(): String {
        return LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
    }

    /**
     * Check if step milestone notifications should be sent
     * This runs in the background service, so notifications work even when app is closed
     */
    private fun checkAndSendStepNotifications(currentSteps: Int) {
        try {
            // Avoid sending notifications too frequently (minimum 30 minutes apart)
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastNotificationTime < 30 * 60 * 1000) {
                return
            }

            // Check for step milestones
            when {
                currentSteps >= stepGoal && lastNotifiedSteps < stepGoal -> {
                    // Goal reached notification
                    healthNotificationManager.showStepMilestoneNotification(
                        currentSteps = currentSteps,
                        goalSteps = stepGoal,
                        milestonePercentage = 1.0f
                    )
                    lastNotifiedSteps = currentSteps
                    lastNotificationTime = currentTime
                    saveNotificationState()
                    Log.d(TAG, "Sent goal achievement notification: $currentSteps/$stepGoal")
                }
                currentSteps >= (stepGoal * 0.75).toInt() && lastNotifiedSteps < (stepGoal * 0.75).toInt() -> {
                    // 75% milestone
                    healthNotificationManager.showStepMilestoneNotification(
                        currentSteps = currentSteps,
                        goalSteps = stepGoal,
                        milestonePercentage = 0.75f
                    )
                    lastNotifiedSteps = currentSteps
                    lastNotificationTime = currentTime
                    saveNotificationState()
                    Log.d(TAG, "Sent 75% milestone notification: $currentSteps/$stepGoal")
                }
                currentSteps >= (stepGoal * 0.5).toInt() && lastNotifiedSteps < (stepGoal * 0.5).toInt() -> {
                    // 50% milestone
                    healthNotificationManager.showStepMilestoneNotification(
                        currentSteps = currentSteps,
                        goalSteps = stepGoal,
                        milestonePercentage = 0.5f
                    )
                    lastNotifiedSteps = currentSteps
                    lastNotificationTime = currentTime
                    saveNotificationState()
                    Log.d(TAG, "Sent 50% milestone notification: $currentSteps/$stepGoal")
                }
                currentSteps >= (stepGoal * 0.25).toInt() && lastNotifiedSteps < (stepGoal * 0.25).toInt() -> {
                    // 25% milestone
                    healthNotificationManager.showStepMilestoneNotification(
                        currentSteps = currentSteps,
                        goalSteps = stepGoal,
                        milestonePercentage = 0.25f
                    )
                    lastNotifiedSteps = currentSteps
                    lastNotificationTime = currentTime
                    saveNotificationState()
                    Log.d(TAG, "Sent 25% milestone notification: $currentSteps/$stepGoal")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending step notification", e)
        }
    }

    /**
     * Test method to verify notifications work from the service
     */
    fun testNotification() {
        try {
            Log.d(TAG, "Testing notification from service...")
            healthNotificationManager.sendTestNotification()
        } catch (e: Exception) {
            Log.e(TAG, "Error testing notification", e)
        }
    }

    /**
     * Force sync with DeviceSensorManager and update notification
     * Useful for debugging step count issues
     */
    fun forceSyncStepCount() {
        try {
            Log.d(TAG, "Force syncing step count...")
            syncWithDeviceSensorManager()
        } catch (e: Exception) {
            Log.e(TAG, "Error force syncing step count", e)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        Log.d(TAG, "Service sensor accuracy changed: ${sensor?.name}, accuracy: $accuracy")
    }
}