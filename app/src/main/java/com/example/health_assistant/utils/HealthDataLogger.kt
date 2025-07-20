package com.example.health_assistant.utils

import android.util.Log
import com.example.health_assistant.data.models.DailyStepData
import com.example.health_assistant.features.health.model.HealthMetrics
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Comprehensive logging utility for health data operations
 * Provides detailed logging for data flow monitoring and debugging
 */
object HealthDataLogger {
    private const val TAG = "HealthDataFlow"
    
    // Log levels
    private const val VERBOSE = 2
    private const val DEBUG = 3
    private const val INFO = 4
    private const val WARN = 5
    private const val ERROR = 6
    
    /**
     * Log data storage operations
     */
    fun logDataStorage(operation: String, userId: String, date: LocalDate, data: DailyStepData) {
        val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
        Log.d(TAG, "[$operation] User: $userId | Date: $dateStr | Steps: ${data.steps} | Calories: ${data.calories} | HeartPoints: ${data.heartPoints}")
    }
    
    /**
     * Log data retrieval operations
     */
    fun logDataRetrieval(operation: String, userId: String, date: LocalDate, success: Boolean, dataSize: Int = 0) {
        val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val status = if (success) "SUCCESS" else "FAILED"
        Log.d(TAG, "[$operation] User: $userId | Date: $dateStr | Status: $status | DataSize: $dataSize")
    }
    
    /**
     * Log weekly data operations
     */
    fun logWeeklyDataOperation(operation: String, userId: String, startDate: LocalDate, dataCount: Int, totalSteps: Int) {
        val startDateStr = startDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val endDate = startDate.plusDays(6)
        val endDateStr = endDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
        Log.d(TAG, "[$operation] User: $userId | Week: $startDateStr to $endDateStr | Days: $dataCount | TotalSteps: $totalSteps")
    }
    
    /**
     * Log day transition events
     */
    fun logDayTransition(userId: String, previousDate: LocalDate, currentDate: LocalDate, preservedData: DailyStepData?) {
        val prevDateStr = previousDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val currDateStr = currentDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val preservedSteps = preservedData?.steps ?: 0
        Log.i(TAG, "[DAY_TRANSITION] User: $userId | From: $prevDateStr | To: $currDateStr | PreservedSteps: $preservedSteps")
    }
    
    /**
     * Log chart data generation
     */
    fun logChartDataGeneration(chartType: String, userId: String, dataPoints: Int, hasGaps: Boolean) {
        val gapStatus = if (hasGaps) "WITH_GAPS" else "COMPLETE"
        Log.d(TAG, "[CHART_GENERATION] Type: $chartType | User: $userId | DataPoints: $dataPoints | Status: $gapStatus")
    }
    
    /**
     * Log data validation results
     */
    fun logDataValidation(operation: String, userId: String, date: LocalDate, isValid: Boolean, issues: List<String> = emptyList()) {
        val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val status = if (isValid) "VALID" else "INVALID"
        val issuesStr = if (issues.isNotEmpty()) " | Issues: ${issues.joinToString(", ")}" else ""
        Log.d(TAG, "[DATA_VALIDATION] $operation | User: $userId | Date: $dateStr | Status: $status$issuesStr")
    }
    
    /**
     * Log error recovery operations
     */
    fun logErrorRecovery(operation: String, userId: String, error: String, recoveryAction: String, success: Boolean) {
        val status = if (success) "RECOVERED" else "FAILED"
        Log.w(TAG, "[ERROR_RECOVERY] $operation | User: $userId | Error: $error | Action: $recoveryAction | Status: $status")
    }
    
    /**
     * Log data maintenance operations
     */
    fun logDataMaintenance(operation: String, userId: String, details: String) {
        Log.i(TAG, "[DATA_MAINTENANCE] $operation | User: $userId | Details: $details")
    }
    
    /**
     * Log performance metrics
     */
    fun logPerformanceMetric(operation: String, duration: Long, dataSize: Int) {
        Log.d(TAG, "[PERFORMANCE] $operation | Duration: ${duration}ms | DataSize: $dataSize")
    }
    
    /**
     * Log cache operations
     */
    fun logCacheOperation(operation: String, userId: String, cacheType: String, hitRate: Float? = null) {
        val hitRateStr = hitRate?.let { " | HitRate: ${String.format("%.2f", it * 100)}%" } ?: ""
        Log.d(TAG, "[CACHE] $operation | User: $userId | Type: $cacheType$hitRateStr")
    }
    
    /**
     * Log data integrity checks
     */
    fun logDataIntegrityCheck(userId: String, date: LocalDate, checkType: String, result: String) {
        val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
        Log.i(TAG, "[DATA_INTEGRITY] User: $userId | Date: $dateStr | Check: $checkType | Result: $result")
    }
    
    /**
     * Log rolling window operations
     */
    fun logRollingWindowOperation(userId: String, windowSize: Int, removedDays: Int, addedDays: Int) {
        Log.d(TAG, "[ROLLING_WINDOW] User: $userId | WindowSize: $windowSize | Removed: $removedDays | Added: $addedDays")
    }
    
    /**
     * Log health metrics updates
     */
    fun logHealthMetricsUpdate(userId: String, date: LocalDate, metrics: HealthMetrics, source: String) {
        val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
        Log.d(TAG, "[METRICS_UPDATE] User: $userId | Date: $dateStr | Source: $source | Steps: ${metrics.steps.current}/${metrics.steps.target} | Calories: ${metrics.calories.current}/${metrics.calories.target} | HeartPoints: ${metrics.heartPoints.current}/${metrics.heartPoints.target}")
    }
    
    /**
     * Log sensor data updates
     */
    fun logSensorDataUpdate(userId: String, sensorType: String, value: Int, increment: Int) {
        Log.d(TAG, "[SENSOR_UPDATE] User: $userId | Sensor: $sensorType | Value: $value | Increment: $increment")
    }
    
    /**
     * Log backup and restore operations
     */
    fun logBackupOperation(operation: String, userId: String, date: LocalDate, success: Boolean, dataSize: Int) {
        val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val status = if (success) "SUCCESS" else "FAILED"
        Log.i(TAG, "[BACKUP] $operation | User: $userId | Date: $dateStr | Status: $status | Size: $dataSize")
    }
    
    /**
     * Log critical errors that affect data integrity
     */
    fun logCriticalError(operation: String, userId: String, error: Throwable, context: String) {
        Log.e(TAG, "[CRITICAL_ERROR] $operation | User: $userId | Context: $context | Error: ${error.message}", error)
    }
    
    /**
     * Log system events that affect health data
     */
    fun logSystemEvent(event: String, userId: String, details: String) {
        Log.i(TAG, "[SYSTEM_EVENT] $event | User: $userId | Details: $details")
    }
}