package com.example.health_assistant.data.fitness

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.fitness.data.Field
import com.google.android.gms.fitness.request.DataReadRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.suspendCoroutine

/**
 * Manager for Google Fit API integration
 * Handles authentication, permissions, and data retrieval for health metrics
 */
@Singleton
class GoogleFitManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "GoogleFitManager"
        const val GOOGLE_FIT_PERMISSIONS_REQUEST_CODE = 1001
    }

    private val fitnessOptions = FitnessOptions.builder()
        .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
        .addDataType(DataType.TYPE_CALORIES_EXPENDED, FitnessOptions.ACCESS_READ)
        .addDataType(DataType.TYPE_HEART_POINTS, FitnessOptions.ACCESS_READ)
        .addDataType(DataType.AGGREGATE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
        .addDataType(DataType.AGGREGATE_CALORIES_EXPENDED, FitnessOptions.ACCESS_READ)
        .addDataType(DataType.AGGREGATE_HEART_POINTS, FitnessOptions.ACCESS_READ)
        .build()

    /**
     * Check if the app has necessary Google Fit permissions
     */
    fun hasPermissions(): Boolean {
        return try {
            val account = GoogleSignIn.getLastSignedInAccount(context)
            account != null && GoogleSignIn.hasPermissions(account, fitnessOptions)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking Google Fit permissions", e)
            false
        }
    }

    /**
     * Request Google Fit permissions from user using Activity Result API
     */
    fun requestPermissions(activity: Activity) {
        try {
            val account = GoogleSignIn.getLastSignedInAccount(context)
            Log.d(TAG, "Requesting Google Fit permissions for account: ${account?.email}")

            if (!GoogleSignIn.hasPermissions(account, fitnessOptions)) {
                GoogleSignIn.requestPermissions(
                    activity,
                    GOOGLE_FIT_PERMISSIONS_REQUEST_CODE,
                    account,
                    fitnessOptions
                )
            } else {
                Log.d(TAG, "Google Fit permissions already granted")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error requesting Google Fit permissions", e)
        }
    }

    /**
     * Check if user is signed into Google account
     */
    fun isGoogleAccountSignedIn(): Boolean {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        return account != null
    }

    /**
     * Get the signed-in Google account
     */
    fun getSignedInAccount() = GoogleSignIn.getLastSignedInAccount(context)

    /**
     * Get today's step count from Google Fit
     */
    suspend fun getTodaySteps(): Int = withContext(Dispatchers.IO) {
        try {
            val account = GoogleSignIn.getLastSignedInAccount(context) ?: return@withContext 0

            val endTime = LocalDateTime.now()
            val startTime = endTime.withHour(0).withMinute(0).withSecond(0).withNano(0)

            val readRequest = DataReadRequest.Builder()
                .aggregate(DataType.TYPE_STEP_COUNT_DELTA)
                .setTimeRange(
                    startTime.toEpochSecond(ZoneOffset.UTC),
                    endTime.toEpochSecond(ZoneOffset.UTC),
                    TimeUnit.SECONDS
                )
                .bucketByTime(1, TimeUnit.DAYS)
                .build()

            val response = Fitness.getHistoryClient(context, account)
                .readData(readRequest)
                .await()

            response.buckets
                .flatMap { it.dataSets }
                .flatMap { it.dataPoints }
                .sumOf { it.getValue(Field.FIELD_STEPS).asInt() }

        } catch (e: Exception) {
            Log.e(TAG, "Error getting steps from Google Fit", e)
            0
        }
    }

    /**
     * Get today's calories burned from Google Fit
     */
    suspend fun getTodayCalories(): Int = withContext(Dispatchers.IO) {
        try {
            val account = GoogleSignIn.getLastSignedInAccount(context) ?: return@withContext 0

            val endTime = LocalDateTime.now()
            val startTime = endTime.withHour(0).withMinute(0).withSecond(0).withNano(0)

            val readRequest = DataReadRequest.Builder()
                .aggregate(DataType.TYPE_CALORIES_EXPENDED)
                .setTimeRange(
                    startTime.toEpochSecond(ZoneOffset.UTC),
                    endTime.toEpochSecond(ZoneOffset.UTC),
                    TimeUnit.SECONDS
                )
                .bucketByTime(1, TimeUnit.DAYS)
                .build()

            val response = Fitness.getHistoryClient(context, account)
                .readData(readRequest)
                .await()

            response.buckets
                .flatMap { it.dataSets }
                .flatMap { it.dataPoints }
                .sumOf { it.getValue(Field.FIELD_CALORIES).asFloat().toInt() }

        } catch (e: Exception) {
            Log.e(TAG, "Error getting calories from Google Fit", e)
            0
        }
    }

    /**
     * Get today's heart points from Google Fit
     */
    suspend fun getTodayHeartPoints(): Int = withContext(Dispatchers.IO) {
        try {
            val account = GoogleSignIn.getLastSignedInAccount(context) ?: return@withContext 0

            val endTime = LocalDateTime.now()
            val startTime = endTime.withHour(0).withMinute(0).withSecond(0).withNano(0)

            val readRequest = DataReadRequest.Builder()
                .aggregate(DataType.TYPE_HEART_POINTS)
                .setTimeRange(
                    startTime.toEpochSecond(ZoneOffset.UTC),
                    endTime.toEpochSecond(ZoneOffset.UTC),
                    TimeUnit.SECONDS
                )
                .bucketByTime(1, TimeUnit.DAYS)
                .build()

            val response = Fitness.getHistoryClient(context, account)
                .readData(readRequest)
                .await()

            response.buckets
                .flatMap { it.dataSets }
                .flatMap { it.dataPoints }
                .sumOf { it.getValue(Field.FIELD_INTENSITY).asFloat().toInt() }

        } catch (e: Exception) {
            Log.e(TAG, "Error getting heart points from Google Fit", e)
            0
        }
    }

    /**
     * Get health metrics for a specific date range
     */
    suspend fun getHealthMetricsForDateRange(
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): Triple<List<Int>, List<Int>, List<Int>> = withContext(Dispatchers.IO) {
        try {
            val account = GoogleSignIn.getLastSignedInAccount(context)
                ?: return@withContext Triple(emptyList(), emptyList(), emptyList())

            // Create read request for the date range
            val readRequest = DataReadRequest.Builder()
                .aggregate(DataType.TYPE_STEP_COUNT_DELTA)
                .aggregate(DataType.TYPE_CALORIES_EXPENDED)
                .aggregate(DataType.TYPE_HEART_POINTS)
                .setTimeRange(
                    startDate.toEpochSecond(ZoneOffset.UTC),
                    endDate.toEpochSecond(ZoneOffset.UTC),
                    TimeUnit.SECONDS
                )
                .bucketByTime(1, TimeUnit.DAYS)
                .build()

            val response = Fitness.getHistoryClient(context, account)
                .readData(readRequest)
                .await()

            val stepsList = mutableListOf<Int>()
            val caloriesList = mutableListOf<Int>()
            val heartPointsList = mutableListOf<Int>()

            response.buckets.forEach { bucket ->
                var dailySteps = 0
                var dailyCalories = 0
                var dailyHeartPoints = 0

                bucket.dataSets.forEach { dataSet ->
                    when (dataSet.dataType) {
                        DataType.AGGREGATE_STEP_COUNT_DELTA -> {
                            dailySteps = dataSet.dataPoints.sumOf {
                                it.getValue(Field.FIELD_STEPS).asInt()
                            }
                        }
                        DataType.AGGREGATE_CALORIES_EXPENDED -> {
                            dailyCalories = dataSet.dataPoints.sumOf {
                                it.getValue(Field.FIELD_CALORIES).asFloat().toInt()
                            }
                        }
                        DataType.AGGREGATE_HEART_POINTS -> {
                            dailyHeartPoints = dataSet.dataPoints.sumOf {
                                it.getValue(Field.FIELD_INTENSITY).asFloat().toInt()
                            }
                        }
                    }
                }

                stepsList.add(dailySteps)
                caloriesList.add(dailyCalories)
                heartPointsList.add(dailyHeartPoints)
            }

            Triple(stepsList, caloriesList, heartPointsList)

        } catch (e: Exception) {
            Log.e(TAG, "Error getting health metrics for date range", e)
            Triple(emptyList(), emptyList(), emptyList())
        }
    }

    /**
     * Force refresh of health data - useful after permission grants
     */
    suspend fun refreshAllData(): Triple<Int, Int, Int> = withContext(Dispatchers.IO) {
        try {
            val steps = getTodaySteps()
            val calories = getTodayCalories()
            val heartPoints = getTodayHeartPoints()

            Log.d(TAG, "Refreshed all data - Steps: $steps, Calories: $calories, Heart Points: $heartPoints")
            Triple(steps, calories, heartPoints)
        } catch (e: Exception) {
            Log.e(TAG, "Error refreshing all data", e)
            Triple(0, 0, 0)
        }
    }
}