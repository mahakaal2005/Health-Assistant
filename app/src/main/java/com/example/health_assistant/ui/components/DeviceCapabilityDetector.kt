package com.example.health_assistant.ui.components

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.content.getSystemService

/**
 * Detects device capabilities for performance optimization
 * Provides device classification and capability assessment
 */
class DeviceCapabilityDetector(private val context: Context) {
    
    companion object {
        private const val TAG = "DeviceCapability"
        private const val LOW_MEMORY_THRESHOLD_MB = 256
        private const val MID_MEMORY_THRESHOLD_MB = 512
        private const val HIGH_MEMORY_THRESHOLD_MB = 1024
    }
    
    data class DeviceCapabilities(
        val memoryClass: Int,
        val deviceClass: DeviceClass,
        val supportsBlur: Boolean,
        val supportsHardwareAcceleration: Boolean,
        val apiLevel: Int,
        val performanceClass: PerformanceClass
    )
    
    enum class DeviceClass {
        LOW_END,
        MID_RANGE,
        HIGH_END
    }
    
    enum class PerformanceClass {
        BASIC,
        STANDARD,
        PREMIUM
    }
    
    fun detectCapabilities(): DeviceCapabilities {
        try {
            val activityManager = context.getSystemService<ActivityManager>()
            val memoryClass = activityManager?.memoryClass ?: 256
            val apiLevel = Build.VERSION.SDK_INT
            
            val deviceClass = classifyDevice(memoryClass)
            val performanceClass = determinePerformanceClass(memoryClass, apiLevel)
            val supportsBlur = apiLevel >= Build.VERSION_CODES.S
            val supportsHardwareAcceleration = apiLevel >= Build.VERSION_CODES.LOLLIPOP
            
            val capabilities = DeviceCapabilities(
                memoryClass = memoryClass,
                deviceClass = deviceClass,
                supportsBlur = supportsBlur,
                supportsHardwareAcceleration = supportsHardwareAcceleration,
                apiLevel = apiLevel,
                performanceClass = performanceClass
            )
            
            Log.d(TAG, "Device capabilities detected:")
            Log.d(TAG, "  Memory class: ${memoryClass}MB")
            Log.d(TAG, "  Device class: $deviceClass")
            Log.d(TAG, "  Performance class: $performanceClass")
            Log.d(TAG, "  API level: $apiLevel")
            Log.d(TAG, "  Supports blur: $supportsBlur")
            Log.d(TAG, "  Hardware acceleration: $supportsHardwareAcceleration")
            
            return capabilities
            
        } catch (exception: Exception) {
            Log.e(TAG, "Failed to detect device capabilities", exception)
            
            // Return safe defaults
            return DeviceCapabilities(
                memoryClass = 256,
                deviceClass = DeviceClass.LOW_END,
                supportsBlur = false,
                supportsHardwareAcceleration = true,
                apiLevel = Build.VERSION.SDK_INT,
                performanceClass = PerformanceClass.BASIC
            )
        }
    }
    
    private fun classifyDevice(memoryClass: Int): DeviceClass {
        return when {
            memoryClass >= HIGH_MEMORY_THRESHOLD_MB -> DeviceClass.HIGH_END
            memoryClass >= MID_MEMORY_THRESHOLD_MB -> DeviceClass.MID_RANGE
            else -> DeviceClass.LOW_END
        }
    }
    
    private fun determinePerformanceClass(memoryClass: Int, apiLevel: Int): PerformanceClass {
        return when {
            memoryClass >= HIGH_MEMORY_THRESHOLD_MB && apiLevel >= Build.VERSION_CODES.S -> {
                PerformanceClass.PREMIUM
            }
            memoryClass >= MID_MEMORY_THRESHOLD_MB && apiLevel >= Build.VERSION_CODES.LOLLIPOP -> {
                PerformanceClass.STANDARD
            }
            else -> {
                PerformanceClass.BASIC
            }
        }
    }
    
    fun getOptimalAnimationSettings(capabilities: DeviceCapabilities): AnimationSettings {
        return when (capabilities.performanceClass) {
            PerformanceClass.PREMIUM -> AnimationSettings(
                enableAnimations = true,
                enableBlurEffects = capabilities.supportsBlur,
                animationDuration = 1.0f,
                enableStaggeredAnimations = true
            )
            PerformanceClass.STANDARD -> AnimationSettings(
                enableAnimations = true,
                enableBlurEffects = false,
                animationDuration = 0.8f,
                enableStaggeredAnimations = true
            )
            PerformanceClass.BASIC -> AnimationSettings(
                enableAnimations = false,
                enableBlurEffects = false,
                animationDuration = 0.5f,
                enableStaggeredAnimations = false
            )
        }
    }
    
    data class AnimationSettings(
        val enableAnimations: Boolean,
        val enableBlurEffects: Boolean,
        val animationDuration: Float,
        val enableStaggeredAnimations: Boolean
    )
}