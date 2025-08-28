package com.example.health_assistant.ui.components

import android.animation.ObjectAnimator
import android.content.Context
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.animation.BounceInterpolator
import com.google.android.material.floatingactionbutton.FloatingActionButton

/**
 * Manages the AI Chatbot FloatingActionButton behavior
 * Handles animations, haptic feedback, and auto-hide/show functionality
 */
class ChatbotFabManager(
    private val context: Context,
    private val fab: FloatingActionButton
) {
    companion object {
        private const val TAG = "ChatbotFAB"
        private const val ENTRANCE_ANIMATION_DURATION = 300L
        private const val HIDE_ANIMATION_DURATION = 200L
        private const val SHOW_ANIMATION_DURATION = 250L
    }
    
    private var isVisible = true
    private var isAnimating = false
    private var fabClickListener: (() -> Unit)? = null
    
    init {
        setupFab()
    }
    
    private fun setupFab() {
        try {
            Log.d(TAG, "Setting up AI Chatbot FAB")
            
            // Set click listener with haptic feedback
            fab.setOnClickListener {
                handleFabClick()
            }
            
            // Apply entrance animation
            playEntranceAnimation()
            
            Log.d(TAG, "AI Chatbot FAB setup complete")
            
        } catch (exception: Exception) {
            Log.e(TAG, "Failed to setup FAB", exception)
        }
    }
    
    private fun handleFabClick() {
        try {
            // Provide haptic feedback
            fab.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            
            Log.d(TAG, "FAB clicked, launching AI chatbot")
            
            // Trigger click listener
            fabClickListener?.invoke()
            
        } catch (exception: Exception) {
            Log.e(TAG, "Failed to handle FAB click", exception)
        }
    }
    
    private fun playEntranceAnimation() {
        try {
            // Start with FAB scaled down
            fab.scaleX = 0.0f
            fab.scaleY = 0.0f
            
            // Animate scale up with bounce
            val scaleXAnimator = ObjectAnimator.ofFloat(fab, "scaleX", 0.0f, 1.0f)
            val scaleYAnimator = ObjectAnimator.ofFloat(fab, "scaleY", 0.0f, 1.0f)
            
            scaleXAnimator.duration = ENTRANCE_ANIMATION_DURATION
            scaleYAnimator.duration = ENTRANCE_ANIMATION_DURATION
            
            scaleXAnimator.interpolator = BounceInterpolator()
            scaleYAnimator.interpolator = BounceInterpolator()
            
            scaleXAnimator.start()
            scaleYAnimator.start()
            
            Log.d(TAG, "FAB entrance animation started")
            
        } catch (exception: Exception) {
            Log.w(TAG, "FAB entrance animation failed", exception)
            // Fallback to instant show
            fab.scaleX = 1.0f
            fab.scaleY = 1.0f
        }
    }
    
    /**
     * Hides the FAB with scale-down animation
     */
    fun hideFab() {
        if (!isVisible || isAnimating) return
        
        try {
            isAnimating = true
            
            val scaleXAnimator = ObjectAnimator.ofFloat(fab, "scaleX", 1.0f, 0.0f)
            val scaleYAnimator = ObjectAnimator.ofFloat(fab, "scaleY", 1.0f, 0.0f)
            
            scaleXAnimator.duration = HIDE_ANIMATION_DURATION
            scaleYAnimator.duration = HIDE_ANIMATION_DURATION
            
            scaleXAnimator.addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    fab.visibility = View.GONE
                    isVisible = false
                    isAnimating = false
                    Log.d(TAG, "FAB hidden")
                }
            })
            
            scaleXAnimator.start()
            scaleYAnimator.start()
            
            Log.d(TAG, "FAB auto-hide triggered by scroll")
            
        } catch (exception: Exception) {
            Log.e(TAG, "Failed to hide FAB", exception)
            isAnimating = false
        }
    }
    
    /**
     * Shows the FAB with scale-up animation
     */
    fun showFab() {
        if (isVisible || isAnimating) return
        
        try {
            isAnimating = true
            fab.visibility = View.VISIBLE
            
            val scaleXAnimator = ObjectAnimator.ofFloat(fab, "scaleX", 0.0f, 1.0f)
            val scaleYAnimator = ObjectAnimator.ofFloat(fab, "scaleY", 0.0f, 1.0f)
            
            scaleXAnimator.duration = SHOW_ANIMATION_DURATION
            scaleYAnimator.duration = SHOW_ANIMATION_DURATION
            
            scaleXAnimator.addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    isVisible = true
                    isAnimating = false
                    Log.d(TAG, "FAB shown")
                }
            })
            
            scaleXAnimator.start()
            scaleYAnimator.start()
            
            Log.d(TAG, "FAB auto-show triggered")
            
        } catch (exception: Exception) {
            Log.e(TAG, "Failed to show FAB", exception)
            isAnimating = false
        }
    }
    
    /**
     * Sets the click listener for the FAB
     */
    fun setOnClickListener(listener: () -> Unit) {
        fabClickListener = listener
        Log.d(TAG, "FAB click listener set")
    }
    
    /**
     * Checks if the FAB is currently visible
     */
    fun isVisible(): Boolean = isVisible
    
    /**
     * Enables or disables the FAB
     */
    fun setEnabled(enabled: Boolean) {
        fab.isEnabled = enabled
        fab.alpha = if (enabled) 1.0f else 0.5f
        Log.d(TAG, "FAB ${if (enabled) "enabled" else "disabled"}")
    }
}