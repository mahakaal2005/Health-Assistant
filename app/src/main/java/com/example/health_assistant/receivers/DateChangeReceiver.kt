package com.example.health_assistant.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.health_assistant.data.sensors.DeviceSensorManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Broadcast receiver to handle date/time changes for midnight reset
 * Ensures proper daily step count reset when date changes
 */
@AndroidEntryPoint
class DateChangeReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "DateChangeReceiver"
    }

    @Inject
    lateinit var deviceSensorManager: DeviceSensorManager

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Date change receiver triggered: ${intent.action}")

        when (intent.action) {
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> {
                handleDateTimeChange()
            }
        }
    }

    private fun handleDateTimeChange() {
        Log.d(TAG, "Handling date/time change - triggering step count reset")

        try {
            // Trigger reset in the sensor manager
            deviceSensorManager.resetStepCount()

            Log.d(TAG, "Step count reset completed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to reset step count on date change", e)
        }
    }
}