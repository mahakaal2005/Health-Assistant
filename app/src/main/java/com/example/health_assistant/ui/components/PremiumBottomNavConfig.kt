package com.example.health_assistant.ui.components

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

/**
 * Configuration system for premium bottom navigation
 * Handles settings, preferences, and feature flags
 */
class PremiumBottomNavConfig private constructor(private val context: Context) {
    
    companion object {
        private const val TAG = "PremiumNavConfig"
        private const val PREFS_NAME = "premium_bottom_nav_config"
        private const val KEY_ANIMATIONS_ENABLED = "animations_enabled"
        private const val KEY_BLUR_EFFECTS_ENABLED = "blur_effects_enabled"
        private const val KEY_ACCESSIBILITY_MODE = "accessibility_mode"
        private const val KEY_PERFORMANCE_MODE = "performance_mode"
        private const val KEY_FAB_ENABLED = "fab_enabled"
        private const val KEY_PILL_HIGHLIGHT_ENABLED = "pill_highlight_enabled"
        private const val KEY_ANIMATION_DURATION_SCALE = "animation_duration_scale"
        
        @Volatile
        private var INSTANCE: PremiumBottomNavConfig? = null
        
        fun getInstance(context: Context): PremiumBottomNavConfig {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PremiumBottomNavConfig(context.applicationContext).also { INSTANCE = it }
            }
        }
        
        fun createDefault(context: Context): PremiumBottomNavConfig {
            Log.w(TAG, "Creating default configuration due to initialization failure")
            return try {
                PremiumBottomNavConfig(context.applicationContext)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create default configuration", e)
                // Return a minimal configuration that won't crash
                PremiumBottomNavConfig(context.applicationContext)
            }
        }
    }
    
    private val sharedPrefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val deviceCapabilities: DeviceCapabilityDetector.DeviceCapabilities
    
    // Configuration data classes
    data class AnimationConfig(
        val enableAnimations: Boolean = true,
        val enableBlurEffects: Boolean = true,
        val animationDurationScale: Float = 1.0f,
        val enableStaggeredAnimations: Boolean = true,
        val pillAnimationDuration: Long = 250L,
        val iconScaleDuration: Long = 200L,
        val textFadeDuration: Long = 150L
    )
    
    data class AccessibilityConfig(
        val enableHighContrast: Boolean = false,
        val enableLargeText: Boolean = false,
        val announceTabChanges: Boolean = true,
        val provideFocusIndicators: Boolean = true,
        val minimumTouchTarget: Float = 48f,
        val preferredTouchTarget: Float = 56f
    )
    
    data class PerformanceConfig(
        val performanceMode: PerformanceMode = PerformanceMode.AUTO,
        val enablePerformanceMonitoring: Boolean = true,
        val frameRateThreshold: Float = 45f,
        val memoryThresholdMB: Long = 50L,
        val enableAutoDegradation: Boolean = true
    )
    
    data class FeatureConfig(
        val enablePillHighlight: Boolean = true,
        val enableFab: Boolean = true,
        val enableMicroAnimations: Boolean = true,
        val enableBackgroundEffects: Boolean = true
    )
    
    enum class PerformanceMode {
        AUTO,       // Automatic based on device capabilities
        PREMIUM,    // All effects enabled
        STANDARD,   // Reduced effects
        BASIC       // Minimal effects
    }
    
    enum class AccessibilityMode {
        AUTO,       // Automatic based on system settings
        ENHANCED,   // Enhanced accessibility features
        STANDARD    // Standard accessibility
    }
    
    init {
        Log.d(TAG, "Initializing premium bottom navigation configuration")
        
        // Detect device capabilities
        val capabilityDetector = DeviceCapabilityDetector(context)
        deviceCapabilities = capabilityDetector.detectCapabilities()
        
        // Load and validate configuration
        loadConfiguration()
        
        Log.d(TAG, "Configuration initialized for device: ${deviceCapabilities.deviceClass}, performance: ${deviceCapabilities.performanceClass}")
    }
    
    private fun loadConfiguration() {
        try {
            // Load configuration with device-appropriate defaults
            val defaultAnimationSettings = getDeviceOptimalAnimationSettings()
            
            Log.d(TAG, "Loading configuration with device-optimal defaults")
            Log.d(TAG, "Device capabilities: memory=${deviceCapabilities.memoryClass}MB, API=${deviceCapabilities.apiLevel}, performance=${deviceCapabilities.performanceClass}")
            
        } catch (exception: Exception) {
            Log.e(TAG, "Failed to load configuration, using defaults", exception)
        }
    }
    
    private fun getDeviceOptimalAnimationSettings(): DeviceCapabilityDetector.AnimationSettings {
        val detector = DeviceCapabilityDetector(context)
        return detector.getOptimalAnimationSettings(deviceCapabilities)
    }
    
    fun getAnimationConfig(): AnimationConfig {
        val defaultSettings = getDeviceOptimalAnimationSettings()
        
        return AnimationConfig(
            enableAnimations = sharedPrefs.getBoolean(KEY_ANIMATIONS_ENABLED, defaultSettings.enableAnimations),
            enableBlurEffects = sharedPrefs.getBoolean(KEY_BLUR_EFFECTS_ENABLED, defaultSettings.enableBlurEffects),
            animationDurationScale = sharedPrefs.getFloat(KEY_ANIMATION_DURATION_SCALE, defaultSettings.animationDuration),
            enableStaggeredAnimations = defaultSettings.enableStaggeredAnimations
        )
    }
    
    fun getAccessibilityConfig(): AccessibilityConfig {
        val accessibilityMode = AccessibilityMode.valueOf(
            sharedPrefs.getString(KEY_ACCESSIBILITY_MODE, AccessibilityMode.AUTO.name) ?: AccessibilityMode.AUTO.name
        )
        
        return when (accessibilityMode) {
            AccessibilityMode.AUTO -> getAutoAccessibilityConfig()
            AccessibilityMode.ENHANCED -> getEnhancedAccessibilityConfig()
            AccessibilityMode.STANDARD -> getStandardAccessibilityConfig()
        }
    }
    
    private fun getAutoAccessibilityConfig(): AccessibilityConfig {
        // Detect system accessibility settings
        val configuration = context.resources.configuration
        val isLargeText = configuration.fontScale > 1.0f
        
        return AccessibilityConfig(
            enableLargeText = isLargeText,
            enableHighContrast = false, // Would need more sophisticated detection
            announceTabChanges = true,
            provideFocusIndicators = true
        )
    }
    
    private fun getEnhancedAccessibilityConfig(): AccessibilityConfig {
        return AccessibilityConfig(
            enableHighContrast = true,
            enableLargeText = true,
            announceTabChanges = true,
            provideFocusIndicators = true,
            preferredTouchTarget = 64f // Larger touch targets
        )
    }
    
    private fun getStandardAccessibilityConfig(): AccessibilityConfig {
        return AccessibilityConfig() // Use defaults
    }
    
    fun getPerformanceConfig(): PerformanceConfig {
        val performanceMode = PerformanceMode.valueOf(
            sharedPrefs.getString(KEY_PERFORMANCE_MODE, PerformanceMode.AUTO.name) ?: PerformanceMode.AUTO.name
        )
        
        return when (performanceMode) {
            PerformanceMode.AUTO -> getAutoPerformanceConfig()
            PerformanceMode.PREMIUM -> getPremiumPerformanceConfig()
            PerformanceMode.STANDARD -> getStandardPerformanceConfig()
            PerformanceMode.BASIC -> getBasicPerformanceConfig()
        }
    }
    
    private fun getAutoPerformanceConfig(): PerformanceConfig {
        return when (deviceCapabilities.performanceClass) {
            DeviceCapabilityDetector.PerformanceClass.PREMIUM -> getPremiumPerformanceConfig()
            DeviceCapabilityDetector.PerformanceClass.STANDARD -> getStandardPerformanceConfig()
            DeviceCapabilityDetector.PerformanceClass.BASIC -> getBasicPerformanceConfig()
        }
    }
    
    private fun getPremiumPerformanceConfig(): PerformanceConfig {
        return PerformanceConfig(
            performanceMode = PerformanceMode.PREMIUM,
            enablePerformanceMonitoring = true,
            frameRateThreshold = 55f, // Higher threshold for premium
            enableAutoDegradation = true
        )
    }
    
    private fun getStandardPerformanceConfig(): PerformanceConfig {
        return PerformanceConfig(
            performanceMode = PerformanceMode.STANDARD,
            enablePerformanceMonitoring = true,
            frameRateThreshold = 45f,
            enableAutoDegradation = true
        )
    }
    
    private fun getBasicPerformanceConfig(): PerformanceConfig {
        return PerformanceConfig(
            performanceMode = PerformanceMode.BASIC,
            enablePerformanceMonitoring = false,
            frameRateThreshold = 30f,
            enableAutoDegradation = false
        )
    }
    
    fun getFeatureConfig(): FeatureConfig {
        val performanceConfig = getPerformanceConfig()
        
        return FeatureConfig(
            enablePillHighlight = sharedPrefs.getBoolean(KEY_PILL_HIGHLIGHT_ENABLED, true),
            enableFab = sharedPrefs.getBoolean(KEY_FAB_ENABLED, true),
            enableMicroAnimations = performanceConfig.performanceMode != PerformanceMode.BASIC,
            enableBackgroundEffects = performanceConfig.performanceMode == PerformanceMode.PREMIUM
        )
    }
    
    // Configuration setters
    fun setAnimationsEnabled(enabled: Boolean) {
        sharedPrefs.edit().putBoolean(KEY_ANIMATIONS_ENABLED, enabled).apply()
        Log.d(TAG, "Animations ${if (enabled) "enabled" else "disabled"}")
    }
    
    fun setBlurEffectsEnabled(enabled: Boolean) {
        sharedPrefs.edit().putBoolean(KEY_BLUR_EFFECTS_ENABLED, enabled).apply()
        Log.d(TAG, "Blur effects ${if (enabled) "enabled" else "disabled"}")
    }
    
    fun setPerformanceMode(mode: PerformanceMode) {
        sharedPrefs.edit().putString(KEY_PERFORMANCE_MODE, mode.name).apply()
        Log.d(TAG, "Performance mode set to: $mode")
    }
    
    fun setAccessibilityMode(mode: AccessibilityMode) {
        sharedPrefs.edit().putString(KEY_ACCESSIBILITY_MODE, mode.name).apply()
        Log.d(TAG, "Accessibility mode set to: $mode")
    }
    
    fun setFabEnabled(enabled: Boolean) {
        sharedPrefs.edit().putBoolean(KEY_FAB_ENABLED, enabled).apply()
        Log.d(TAG, "FAB ${if (enabled) "enabled" else "disabled"}")
    }
    
    // Validation methods
    fun validateConfiguration(): ConfigValidationResult {
        val issues = mutableListOf<String>()
        
        try {
            // Validate animation config
            val animationConfig = getAnimationConfig()
            if (animationConfig.animationDurationScale < 0.1f || animationConfig.animationDurationScale > 3.0f) {
                issues.add("Animation duration scale out of range: ${animationConfig.animationDurationScale}")
            }
            
            // Validate accessibility config
            val accessibilityConfig = getAccessibilityConfig()
            if (accessibilityConfig.minimumTouchTarget < 48f) {
                issues.add("Touch target too small: ${accessibilityConfig.minimumTouchTarget}dp")
            }
            
            // Validate performance config
            val performanceConfig = getPerformanceConfig()
            if (performanceConfig.frameRateThreshold < 15f || performanceConfig.frameRateThreshold > 60f) {
                issues.add("Frame rate threshold out of range: ${performanceConfig.frameRateThreshold}fps")
            }
            
            Log.d(TAG, "Configuration validation complete - ${issues.size} issues found")
            
            return ConfigValidationResult(
                isValid = issues.isEmpty(),
                issues = issues
            )
            
        } catch (exception: Exception) {
            Log.e(TAG, "Configuration validation failed", exception)
            return ConfigValidationResult(
                isValid = false,
                issues = listOf("Validation failed: ${exception.message}")
            )
        }
    }
    
    fun getDeviceCapabilities(): DeviceCapabilityDetector.DeviceCapabilities {
        return deviceCapabilities
    }
    
    fun logCurrentConfiguration() {
        try {
            val animationConfig = getAnimationConfig()
            val accessibilityConfig = getAccessibilityConfig()
            val performanceConfig = getPerformanceConfig()
            val featureConfig = getFeatureConfig()
            
            Log.i(TAG, "Current Configuration:")
            Log.i(TAG, "  Device: ${deviceCapabilities.deviceClass} (${deviceCapabilities.memoryClass}MB)")
            Log.i(TAG, "  Performance: ${deviceCapabilities.performanceClass}")
            Log.i(TAG, "  Animations: ${animationConfig.enableAnimations}")
            Log.i(TAG, "  Blur effects: ${animationConfig.enableBlurEffects}")
            Log.i(TAG, "  Performance mode: ${performanceConfig.performanceMode}")
            Log.i(TAG, "  Accessibility: Large text=${accessibilityConfig.enableLargeText}, High contrast=${accessibilityConfig.enableHighContrast}")
            Log.i(TAG, "  Features: Pill=${featureConfig.enablePillHighlight}, FAB=${featureConfig.enableFab}")
            
        } catch (exception: Exception) {
            Log.e(TAG, "Failed to log configuration", exception)
        }
    }
    
    data class ConfigValidationResult(
        val isValid: Boolean,
        val issues: List<String>
    )
}