package com.example.health_assistant.ui.components

import android.view.Choreographer
import android.util.Log

/**
 * Enhanced performance monitor for premium bottom navigation effects
 * Tracks frame times, memory usage, and provides automatic degradation
 */
class PerformanceMonitor {
    companion object {
        private const val TAG = "PerfMonitor"
        private const val TARGET_FRAME_TIME_MS = 16.67f // 60fps
        private const val DEGRADATION_THRESHOLD_MS = 33.33f // 30fps
        private const val CRITICAL_THRESHOLD_MS = 50.0f // 20fps
        private const val SAMPLE_SIZE = 10
        private const val MEMORY_CHECK_INTERVAL = 30 // frames
    }
    
    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            val frameTime = (frameTimeNanos - lastFrameTime) / 1_000_000f
            if (lastFrameTime != 0L) {
                recordFrameTime(frameTime)
            }
            lastFrameTime = frameTimeNanos
            
            if (isMonitoring) {
                Choreographer.getInstance().postFrameCallback(this)
            }
        }
    }
    
    private var lastFrameTime = 0L
    private var isMonitoring = false
    private val frameTimes = mutableListOf<Float>()
    private var performanceListener: PerformanceListener? = null
    private var frameCount = 0
    private var initialMemoryUsage = 0L
    private var currentPerformanceState = PerformanceState.OPTIMAL
    
    enum class PerformanceState {
        OPTIMAL,    // 60fps+
        DEGRADED,   // 30-60fps
        CRITICAL    // <30fps
    }
    
    interface PerformanceListener {
        fun onPerformanceDegraded(averageFrameTime: Float)
        fun onPerformanceRestored(averageFrameTime: Float)
        fun onPerformanceCritical(averageFrameTime: Float)
        fun onMemoryPressure(memoryUsageMB: Long)
    }
    
    fun startMonitoring(listener: PerformanceListener? = null) {
        if (!isMonitoring) {
            isMonitoring = true
            performanceListener = listener
            lastFrameTime = 0L
            frameTimes.clear()
            frameCount = 0
            initialMemoryUsage = getCurrentMemoryUsage()
            currentPerformanceState = PerformanceState.OPTIMAL
            
            Choreographer.getInstance().postFrameCallback(frameCallback)
            Log.d(TAG, "Performance monitoring started - Initial memory: ${initialMemoryUsage}MB")
        }
    }
    
    fun stopMonitoring() {
        if (isMonitoring) {
            isMonitoring = false
            Choreographer.getInstance().removeFrameCallback(frameCallback)
            Log.d(TAG, "Performance monitoring stopped")
        }
    }
    
    private fun recordFrameTime(frameTime: Float) {
        frameTimes.add(frameTime)
        frameCount++
        
        // Keep only recent samples
        if (frameTimes.size > SAMPLE_SIZE) {
            frameTimes.removeAt(0)
        }
        
        // Check memory usage periodically
        if (frameCount % MEMORY_CHECK_INTERVAL == 0) {
            checkMemoryUsage()
        }
        
        // Check performance every few frames
        if (frameTimes.size >= SAMPLE_SIZE) {
            val averageFrameTime = frameTimes.average().toFloat()
            
            Log.v(TAG, "Frame time: ${frameTime}ms, Average: ${averageFrameTime}ms, Memory: ${getCurrentMemoryUsage()}MB, Device class: ${getDeviceClass()}")
            
            updatePerformanceState(averageFrameTime)
        }
    }
    
    private fun updatePerformanceState(averageFrameTime: Float) {
        val newState = when {
            averageFrameTime > CRITICAL_THRESHOLD_MS -> PerformanceState.CRITICAL
            averageFrameTime > DEGRADATION_THRESHOLD_MS -> PerformanceState.DEGRADED
            else -> PerformanceState.OPTIMAL
        }
        
        if (newState != currentPerformanceState) {
            val oldState = currentPerformanceState
            currentPerformanceState = newState
            
            when (newState) {
                PerformanceState.CRITICAL -> {
                    Log.w(TAG, "Performance critical (${averageFrameTime}ms), disabling all effects")
                    performanceListener?.onPerformanceCritical(averageFrameTime)
                }
                PerformanceState.DEGRADED -> {
                    Log.w(TAG, "Performance degraded (${averageFrameTime}ms), switching to simplified effects")
                    performanceListener?.onPerformanceDegraded(averageFrameTime)
                }
                PerformanceState.OPTIMAL -> {
                    if (oldState != PerformanceState.OPTIMAL) {
                        Log.d(TAG, "Performance restored (${averageFrameTime}ms)")
                        performanceListener?.onPerformanceRestored(averageFrameTime)
                    }
                }
            }
        }
    }
    
    private fun checkMemoryUsage() {
        try {
            val currentMemory = getCurrentMemoryUsage()
            val memoryIncrease = currentMemory - initialMemoryUsage
            
            // Alert if memory usage increased significantly (>50MB)
            if (memoryIncrease > 50) {
                Log.w(TAG, "Memory usage increased by ${memoryIncrease}MB")
                performanceListener?.onMemoryPressure(currentMemory)
            }
            
        } catch (exception: Exception) {
            Log.w(TAG, "Failed to check memory usage", exception)
        }
    }
    
    private fun getCurrentMemoryUsage(): Long {
        return try {
            val runtime = Runtime.getRuntime()
            (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
        } catch (e: Exception) {
            0L
        }
    }
    
    private fun getDeviceClass(): String {
        return try {
            val runtime = Runtime.getRuntime()
            val maxMemory = runtime.maxMemory() / (1024 * 1024)
            when {
                maxMemory >= 1024 -> "HIGH_END"
                maxMemory >= 512 -> "MID_RANGE"
                else -> "LOW_END"
            }
        } catch (e: Exception) {
            "UNKNOWN"
        }
    }
    
    fun getCurrentAverageFrameTime(): Float {
        return if (frameTimes.isNotEmpty()) {
            frameTimes.average().toFloat()
        } else {
            0f
        }
    }
    
    fun isPerformanceOptimal(): Boolean {
        return currentPerformanceState == PerformanceState.OPTIMAL
    }
    
    fun getCurrentPerformanceState(): PerformanceState {
        return currentPerformanceState
    }
    
    fun getPerformanceMetrics(): PerformanceMetrics {
        return PerformanceMetrics(
            averageFrameTime = getCurrentAverageFrameTime(),
            currentMemoryUsage = getCurrentMemoryUsage(),
            performanceState = currentPerformanceState,
            frameCount = frameCount,
            memoryIncrease = getCurrentMemoryUsage() - initialMemoryUsage
        )
    }
    
    data class PerformanceMetrics(
        val averageFrameTime: Float,
        val currentMemoryUsage: Long,
        val performanceState: PerformanceState,
        val frameCount: Int,
        val memoryIncrease: Long
    )
}