package com.example.health_assistant.utils

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * Health data monitoring utility for tracking performance and data flow metrics
 */
object HealthDataMonitor {
    private const val TAG = "HealthDataMonitor"
    
    // Performance tracking
    private val operationCounts = ConcurrentHashMap<String, AtomicInteger>()
    private val operationDurations = ConcurrentHashMap<String, AtomicLong>()
    private val errorCounts = ConcurrentHashMap<String, AtomicInteger>()
    
    // Cache metrics
    private val cacheHits = AtomicInteger(0)
    private val cacheMisses = AtomicInteger(0)
    
    // Data integrity metrics
    private val dataValidationResults = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    private val lastDataIntegrityCheck = MutableStateFlow<String>("")
    
    // System health metrics
    private val _systemHealth = MutableStateFlow(SystemHealthStatus.HEALTHY)
    val systemHealth: StateFlow<SystemHealthStatus> = _systemHealth
    
    enum class SystemHealthStatus {
        HEALTHY,
        WARNING,
        CRITICAL
    }
    
    /**
     * Track operation performance
     */
    fun trackOperation(operation: String, duration: Long, success: Boolean) {
        // Update operation count
        operationCounts.computeIfAbsent(operation) { AtomicInteger(0) }.incrementAndGet()
        
        // Update duration
        operationDurations.computeIfAbsent(operation) { AtomicLong(0) }.addAndGet(duration)
        
        // Update error count if failed
        if (!success) {
            errorCounts.computeIfAbsent(operation) { AtomicInteger(0) }.incrementAndGet()
        }
        
        // Log performance metric
        HealthDataLogger.logPerformanceMetric(operation, duration, 1)
        
        // Check if we need to update system health
        updateSystemHealth()
    }
    
    /**
     * Track cache operations
     */
    fun trackCacheOperation(hit: Boolean) {
        if (hit) {
            cacheHits.incrementAndGet()
        } else {
            cacheMisses.incrementAndGet()
        }
    }
    
    /**
     * Get cache hit rate
     */
    fun getCacheHitRate(): Float {
        val hits = cacheHits.get()
        val misses = cacheMisses.get()
        val total = hits + misses
        return if (total > 0) hits.toFloat() / total else 0f
    }
    
    /**
     * Track data validation result
     */
    fun trackDataValidation(userId: String, isValid: Boolean) {
        val currentResults = dataValidationResults.value.toMutableMap()
        currentResults[userId] = isValid
        dataValidationResults.value = currentResults
        
        // Update last check timestamp
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        lastDataIntegrityCheck.value = timestamp
    }
    
    /**
     * Get operation statistics
     */
    fun getOperationStats(operation: String): OperationStats? {
        val count = operationCounts[operation]?.get() ?: return null
        val totalDuration = operationDurations[operation]?.get() ?: 0
        val errors = errorCounts[operation]?.get() ?: 0
        
        return OperationStats(
            operation = operation,
            count = count,
            averageDuration = if (count > 0) totalDuration / count else 0,
            errorRate = if (count > 0) errors.toFloat() / count else 0f,
            successRate = if (count > 0) (count - errors).toFloat() / count else 0f
        )
    }
    
    /**
     * Get all operation statistics
     */
    fun getAllOperationStats(): List<OperationStats> {
        return operationCounts.keys.mapNotNull { getOperationStats(it) }
    }
    
    /**
     * Update system health based on metrics
     */
    private fun updateSystemHealth() {
        val allStats = getAllOperationStats()
        
        // Check for critical issues
        val hasCriticalErrors = allStats.any { it.errorRate > 0.5f }
        val hasSlowOperations = allStats.any { it.averageDuration > 5000 } // 5 seconds
        
        // Check for warning conditions
        val hasModerateErrors = allStats.any { it.errorRate > 0.2f }
        val cacheHitRate = getCacheHitRate()
        val hasLowCacheHitRate = cacheHitRate < 0.5f && (cacheHits.get() + cacheMisses.get()) > 10
        
        val newStatus = when {
            hasCriticalErrors || hasSlowOperations -> SystemHealthStatus.CRITICAL
            hasModerateErrors || hasLowCacheHitRate -> SystemHealthStatus.WARNING
            else -> SystemHealthStatus.HEALTHY
        }
        
        if (newStatus != _systemHealth.value) {
            _systemHealth.value = newStatus
            Log.i(TAG, "System health status changed to: $newStatus")
        }
    }
    
    /**
     * Generate health report
     */
    fun generateHealthReport(): HealthReport {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        val stats = getAllOperationStats()
        val cacheHitRate = getCacheHitRate()
        val totalOperations = operationCounts.values.sumOf { it.get() }
        val totalErrors = errorCounts.values.sumOf { it.get() }
        
        return HealthReport(
            timestamp = timestamp,
            systemHealth = _systemHealth.value,
            totalOperations = totalOperations,
            totalErrors = totalErrors,
            overallErrorRate = if (totalOperations > 0) totalErrors.toFloat() / totalOperations else 0f,
            cacheHitRate = cacheHitRate,
            operationStats = stats,
            lastDataIntegrityCheck = lastDataIntegrityCheck.value
        )
    }
    
    /**
     * Reset all metrics
     */
    fun resetMetrics() {
        operationCounts.clear()
        operationDurations.clear()
        errorCounts.clear()
        cacheHits.set(0)
        cacheMisses.set(0)
        dataValidationResults.value = emptyMap()
        lastDataIntegrityCheck.value = ""
        _systemHealth.value = SystemHealthStatus.HEALTHY
        
        Log.i(TAG, "All metrics reset")
    }
    
    /**
     * Log current health report
     */
    fun logHealthReport() {
        val report = generateHealthReport()
        Log.i(TAG, "=== HEALTH DATA MONITOR REPORT ===")
        Log.i(TAG, "Timestamp: ${report.timestamp}")
        Log.i(TAG, "System Health: ${report.systemHealth}")
        Log.i(TAG, "Total Operations: ${report.totalOperations}")
        Log.i(TAG, "Total Errors: ${report.totalErrors}")
        Log.i(TAG, "Overall Error Rate: ${String.format("%.2f", report.overallErrorRate * 100)}%")
        Log.i(TAG, "Cache Hit Rate: ${String.format("%.2f", report.cacheHitRate * 100)}%")
        Log.i(TAG, "Last Data Integrity Check: ${report.lastDataIntegrityCheck}")
        
        report.operationStats.forEach { stat ->
            Log.i(TAG, "Operation: ${stat.operation} | Count: ${stat.count} | Avg Duration: ${stat.averageDuration}ms | Success Rate: ${String.format("%.2f", stat.successRate * 100)}%")
        }
        Log.i(TAG, "=== END REPORT ===")
    }
    
    data class OperationStats(
        val operation: String,
        val count: Int,
        val averageDuration: Long,
        val errorRate: Float,
        val successRate: Float
    )
    
    data class HealthReport(
        val timestamp: String,
        val systemHealth: SystemHealthStatus,
        val totalOperations: Int,
        val totalErrors: Int,
        val overallErrorRate: Float,
        val cacheHitRate: Float,
        val operationStats: List<OperationStats>,
        val lastDataIntegrityCheck: String
    )
}