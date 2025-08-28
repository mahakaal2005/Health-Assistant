package com.example.health_assistant.ui.components

import android.content.Context
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat
import com.example.health_assistant.R

/**
 * Manages background effects for premium bottom navigation
 * Handles API level detection and provides appropriate fallbacks
 */
object BackgroundEffectManager {
    private const val TAG = "BackgroundEffect"
    private const val BLUR_RADIUS = 10f
    
    /**
     * Applies the appropriate background effect based on device capabilities
     */
    fun applyBackgroundEffect(view: View, context: Context) {
        try {
            Log.d(TAG, "API Level: ${Build.VERSION.SDK_INT}, applying background effect")
            
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                    // Android 12+ (API 31+) - Use RenderEffect blur
                    applyRenderEffectBlur(view)
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP -> {
                    // Android 5+ (API 21+) - Use elevation with translucent background
                    applyElevationEffect(view, context)
                }
                else -> {
                    // Older versions - Use solid background with subtle shadow
                    applySolidBackgroundEffect(view, context)
                }
            }
            
        } catch (exception: Exception) {
            Log.w(TAG, "Background effect failed, using elevation fallback", exception)
            applyElevationEffect(view, context)
        }
    }
    
    private fun applyRenderEffectBlur(view: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                val blurEffect = RenderEffect.createBlurEffect(
                    BLUR_RADIUS, BLUR_RADIUS, Shader.TileMode.CLAMP
                )
                view.setRenderEffect(blurEffect)
                
                Log.d(TAG, "RenderEffect blur applied successfully")
            } catch (exception: Exception) {
                Log.w(TAG, "RenderEffect blur failed", exception)
                throw exception
            }
        }
    }
    
    private fun applyElevationEffect(view: View, context: Context) {
        try {
            // Set elevation for shadow effect
            view.elevation = context.resources.getDimension(R.dimen.ds_elevation_medium)
            
            // Apply translucent background
            view.setBackgroundResource(R.drawable.premium_bottom_nav_background)
            
            Log.d(TAG, "Elevation effect applied with translucent background")
            
        } catch (exception: Exception) {
            Log.e(TAG, "Elevation effect failed", exception)
            applySolidBackgroundEffect(view, context)
        }
    }
    
    private fun applySolidBackgroundEffect(view: View, context: Context) {
        try {
            // Use solid white background with subtle elevation
            view.setBackgroundColor(ContextCompat.getColor(context, R.color.white))
            view.elevation = context.resources.getDimension(R.dimen.ds_elevation_low)
            
            Log.d(TAG, "Solid background effect applied as fallback")
            
        } catch (exception: Exception) {
            Log.e(TAG, "Solid background effect failed", exception)
        }
    }
    
    /**
     * Checks if blur effects are supported on this device
     */
    fun isBlurSupported(): Boolean {
        val isSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        Log.d(TAG, "Blur supported: $isSupported (API ${Build.VERSION.SDK_INT})")
        return isSupported
    }
    
    /**
     * Gets the appropriate background style based on device capabilities
     */
    fun getBackgroundStyle(): BackgroundStyle {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> BackgroundStyle.BLUR_EFFECT
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP -> BackgroundStyle.ELEVATION_TRANSLUCENT
            else -> BackgroundStyle.SOLID_WITH_SHADOW
        }
    }
    
    enum class BackgroundStyle {
        BLUR_EFFECT,
        ELEVATION_TRANSLUCENT,
        SOLID_WITH_SHADOW
    }
}