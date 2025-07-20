package com.example.health_assistant.utils

import android.util.Log
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Error tracking utility for health data operations
 * Tracks and categorizes errors to help with debugging and monitoring
 */
object HealthDataErrorTracker {
    private const val TAG = "HealthDataErrorTracker"
    
    // Error categories
    enum class ErrorCategory {
        DATA_CORRUPTION,
        NETWORK_ERROR,
        PERMISSION_ERROR,
        VALIDATION_ERROR,
        STORAGE_ERROR,
        SENSOR_ERROR,
        UNKNOWN_ERROR
    }
    
    // Error tracking
    private val errorCounts = ConcurrentHashMap<ErrorCategory, AtomicInteger>()
    private val recentErrors = mutableListOf<ErrorRecord>()
    private val maxRecentErrors = 100
    
    data class ErrorRecord(
        val timestamp: String,
        val category: ErrorCategory,
        val operation: String,
        val userId: String,
        val errorMessage: String,
        val stackTrace: String?
    )
    
    /**
     * Track an error
     */
    fun trackError(
        category: ErrorCategory,
        operation: String,
        userId: String,
        error: Throwable,
        context: String = ""
    ) {
        // Increment error count for category
        errorCounts.computeIfAbsent(category) { AtomicInteger(0) }.incrementAndGet()
        
        // Create error record
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        val errorRecord = ErrorRecord(
            timestamp = timestamp,
            category = category,
            operation = operation,
            userId = userId,
            errorMessage = "${error.message} | Context: $context",
            stackTrace = error.stackTraceToString()
        )
        
        // Add to recent errors (thread-safe)
        synchronized(recentErrors) {
            recentErrors.add(errorRecord)
            
            // Keep only the most recent errors
            if (recentErrors.size > maxRecentErrors) {
                recentErrors.removeAt(0)
            }
        }
        
        // Log the error
        HealthDataLogger.logCriticalError(operation, userId, error, context)
        
        // Log error summary
        Log.e(TAG, "Error tracked: Category=$category, Operation=$operation, User=$userId, Message=${error.message}")
    }
    
    /**
     * Get error count for a category
     */
    fun getErrorCount(category: ErrorCategory): Int {
        return errorCounts[category]?.get() ?: 0
    }
    
    /**
     * Get total error count
     */
    fun getTotalErrorCount(): Int {
        return errorCounts.values.sumOf { it.get() }
    }
    
    /**
     * Get recent errors
     */
    fun getRecentErrors(limit: Int = 10): List<ErrorRecord> {
        synchronized(recentErrors) {
            return recentErrors.takeLast(limit)
        }
    }
    
    /**
     * Get errors for a specific user
     */
    fun getErrorsForUser(userId: String, limit: Int = 10): List<ErrorRecord> {
        synchronized(recentErrors) {
            return recentErrors.filter { it.userId == userId }.takeLast(limit)
        }
    }
    
    /**
     * Get errors for a specific operation
     */
    fun getErrorsForOperation(operation: String, limit: Int = 10): List<ErrorRecord> {
        synchronized(recentErrors) {
            return recentErrors.filter { it.operation == operation }.takeLast(limit)
        }
    }
    
    /**
     * Get error statistics
     */
    fun getErrorStatistics(): Map<ErrorCategory, Int> {
        return errorCounts.mapValues { it.value.get() }
    }
    
    /**
     * Check if error rate is concerning for a category
     */
    fun isErrorRateConcerning(category: ErrorCategory, threshold: Int = 10): Boolean {
        return getErrorCount(category) >= threshold
    }
    
    /**
     * Generate error report
     */
    fun generateErrorReport(): String {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        val totalErrors = getTotalErrorCount()
        val statistics = getErrorStatistics()
        val recentErrorsList = getRecentErrors(5)
        
        val report = StringBuilder()
        report.appendLine("=== HEALTH DATA ERROR REPORT ===")
        report.appendLine("Generated: $timestamp")
        report.appendLine("Total Errors: $totalErrors")
        report.appendLine()
        
        report.appendLine("Error Statistics by Category:")
        statistics.forEach { (category, count) ->
            val percentage = if (totalErrors > 0) (count * 100.0 / totalErrors) else 0.0
            report.appendLine("  $category: $count (${String.format("%.1f", percentage)}%)")
        }
        report.appendLine()
        
        report.appendLine("Recent Errors:")
        recentErrorsList.forEach { error ->
            report.appendLine("  [${error.timestamp}] ${error.category} - ${error.operation}")
            report.appendLine("    User: ${error.userId}")
            report.appendLine("    Message: ${error.errorMessage}")
            report.appendLine()
        }
        
        report.appendLine("=== END ERROR REPORT ===")
        return report.toString()
    }
    
    /**
     * Log error report
     */
    fun logErrorReport() {
        val report = generateErrorReport()
        Log.i(TAG, report)
    }
    
    /**
     * Clear all error data
     */
    fun clearErrors() {
        errorCounts.clear()
        synchronized(recentErrors) {
            recentErrors.clear()
        }
        Log.i(TAG, "All error data cleared")
    }
    
    /**
     * Categorize error based on exception type and message
     */
    fun categorizeError(error: Throwable): ErrorCategory {
        val message = error.message?.lowercase() ?: ""
        val className = error.javaClass.simpleName.lowercase()
        
        return when {
            message.contains("permission") || message.contains("denied") -> ErrorCategory.PERMISSION_ERROR
            message.contains("network") || message.contains("connection") || message.contains("timeout") -> ErrorCategory.NETWORK_ERROR
            message.contains("validation") || message.contains("invalid") -> ErrorCategory.VALIDATION_ERROR
            message.contains("storage") || message.contains("database") || message.contains("file") -> ErrorCategory.STORAGE_ERROR
            message.contains("sensor") || message.contains("hardware") -> ErrorCategory.SENSOR_ERROR
            message.contains("corrupt") || message.contains("integrity") -> ErrorCategory.DATA_CORRUPTION
            className.contains("sql") || className.contains("database") -> ErrorCategory.STORAGE_ERROR
            className.contains("network") || className.contains("http") -> ErrorCategory.NETWORK_ERROR
            else -> ErrorCategory.UNKNOWN_ERROR
        }
    }
    
    /**
     * Track error with automatic categorization
     */
    fun trackError(operation: String, userId: String, error: Throwable, context: String = "") {
        val category = categorizeError(error)
        trackError(category, operation, userId, error, context)
    }
}