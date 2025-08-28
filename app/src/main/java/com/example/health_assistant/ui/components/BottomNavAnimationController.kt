package com.example.health_assistant.ui.components

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.content.Context
import android.util.Log
import android.view.View
import android.view.ViewPropertyAnimator
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.interpolator.view.animation.FastOutLinearInInterpolator
import androidx.interpolator.view.animation.LinearOutSlowInInterpolator

/**
 * Manages micro-animations for premium bottom navigation
 * Provides staggered animation sequences with comprehensive error handling
 */
class BottomNavAnimationController(private val context: Context) {
    
    companion object {
        private const val TAG = "BottomNavAnim"
        private const val PILL_ANIMATION_DURATION = 250L
        private const val ICON_SCALE_DURATION = 200L
        private const val TEXT_FADE_DURATION = 150L
        private const val STAGGER_DELAY = 50L
        private const val ACTIVE_SCALE = 1.05f
        private const val INACTIVE_SCALE = 1.0f
    }
    
    private var isAnimationEnabled = true
    private val activeAnimators = mutableSetOf<ObjectAnimator>()
    private val activeViewAnimators = mutableSetOf<ViewPropertyAnimator>()
    
    /**
     * Animates tab selection with staggered sequence
     */
    fun animateTabSelection(
        pillView: PillHighlightView,
        fromTabIndex: Int,
        toTabIndex: Int,
        iconViews: List<View>,
        textViews: List<View>
    ) {
        if (!isAnimationEnabled) {
            Log.d(TAG, "Animations disabled, skipping animation sequence")
            return
        }
        
        try {
            Log.d(TAG, "Starting animation sequence: pill->icon->text from tab $fromTabIndex to $toTabIndex")
            
            // Clear any existing animations
            clearActiveAnimations()
            
            // Create staggered animation sequence
            val animatorSet = AnimatorSet()
            val animations = mutableListOf<ObjectAnimator>()
            
            // 1. Pill animation (first)
            val pillAnimator = createPillAnimation(pillView, fromTabIndex, toTabIndex)
            animations.add(pillAnimator)
            
            // 2. Icon scale animations (after 50ms delay)
            val iconAnimators = createIconScaleAnimations(iconViews, toTabIndex)
            animations.addAll(iconAnimators)
            
            // 3. Text fade animations (after 100ms delay)
            createTextFadeAnimations(textViews, fromTabIndex, toTabIndex)
            
            // Play pill and icon animations together with stagger
            if (animations.isNotEmpty()) {
                animatorSet.playTogether(animations as Collection<android.animation.Animator>)
                animatorSet.start()
                
                // Track active animators
                activeAnimators.addAll(animations)
            }
            
            Log.d(TAG, "Animation sequence started successfully")
            
        } catch (exception: Exception) {
            Log.e(TAG, "Animation failed, falling back to instant transition", exception)
            // Fallback to instant state change
            applyInstantStateChange(iconViews, textViews, toTabIndex)
        }
    }
    
    private fun createPillAnimation(pillView: PillHighlightView, fromTab: Int, toTab: Int): ObjectAnimator {
        return ObjectAnimator.ofPropertyValuesHolder(
            pillView,
            PropertyValuesHolder.ofFloat("scaleX", 0.8f, 1.0f),
            PropertyValuesHolder.ofFloat("scaleY", 0.8f, 1.0f),
            PropertyValuesHolder.ofFloat("alpha", 0.0f, 1.0f)
        ).apply {
            duration = PILL_ANIMATION_DURATION
            interpolator = LinearOutSlowInInterpolator()
            
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    activeAnimators.remove(this@apply)
                    Log.v(TAG, "Pill animation completed")
                }
                
                override fun onAnimationCancel(animation: android.animation.Animator) {
                    activeAnimators.remove(this@apply)
                    Log.v(TAG, "Pill animation cancelled")
                }
            })
        }
    }
    
    private fun createIconScaleAnimations(iconViews: List<View>, activeTabIndex: Int): List<ObjectAnimator> {
        val animators = mutableListOf<ObjectAnimator>()
        
        iconViews.forEachIndexed { index, iconView ->
            val targetScale = if (index == activeTabIndex) ACTIVE_SCALE else INACTIVE_SCALE
            val currentScale = iconView.scaleX
            
            if (currentScale != targetScale) {
                val animator = ObjectAnimator.ofPropertyValuesHolder(
                    iconView,
                    PropertyValuesHolder.ofFloat("scaleX", currentScale, targetScale),
                    PropertyValuesHolder.ofFloat("scaleY", currentScale, targetScale)
                ).apply {
                    duration = ICON_SCALE_DURATION
                    startDelay = STAGGER_DELAY
                    interpolator = if (index == activeTabIndex) {
                        FastOutSlowInInterpolator()
                    } else {
                        FastOutLinearInInterpolator()
                    }
                    
                    addListener(object : android.animation.AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: android.animation.Animator) {
                            activeAnimators.remove(this@apply)
                            Log.v(TAG, "Icon scale animation completed for index $index")
                        }
                        
                        override fun onAnimationCancel(animation: android.animation.Animator) {
                            activeAnimators.remove(this@apply)
                        }
                    })
                }
                
                animators.add(animator)
            }
        }
        
        return animators
    }
    
    private fun createTextFadeAnimations(textViews: List<View>, fromTabIndex: Int, toTabIndex: Int) {
        try {
            // Fade out old active text
            if (fromTabIndex >= 0 && fromTabIndex < textViews.size) {
                val oldTextView = textViews[fromTabIndex]
                val fadeOutAnimator = oldTextView.animate()
                    .alpha(0.7f)
                    .setDuration(TEXT_FADE_DURATION / 2)
                    .setStartDelay(STAGGER_DELAY * 2)
                    .setInterpolator(FastOutLinearInInterpolator())
                    .withEndAction {
                        Log.v(TAG, "Text fade out completed for index $fromTabIndex")
                    }
                
                activeViewAnimators.add(fadeOutAnimator)
            }
            
            // Fade in new active text
            if (toTabIndex >= 0 && toTabIndex < textViews.size) {
                val newTextView = textViews[toTabIndex]
                val fadeInAnimator = newTextView.animate()
                    .alpha(1.0f)
                    .setDuration(TEXT_FADE_DURATION)
                    .setStartDelay(STAGGER_DELAY * 2 + TEXT_FADE_DURATION / 2)
                    .setInterpolator(LinearOutSlowInInterpolator())
                    .withEndAction {
                        Log.v(TAG, "Text fade in completed for index $toTabIndex")
                    }
                
                activeViewAnimators.add(fadeInAnimator)
            }
            
        } catch (exception: Exception) {
            Log.w(TAG, "Text fade animation failed", exception)
        }
    }
    
    private fun applyInstantStateChange(iconViews: List<View>, textViews: List<View>, activeTabIndex: Int) {
        try {
            // Apply icon scales instantly
            iconViews.forEachIndexed { index, iconView ->
                val targetScale = if (index == activeTabIndex) ACTIVE_SCALE else INACTIVE_SCALE
                iconView.scaleX = targetScale
                iconView.scaleY = targetScale
            }
            
            // Apply text alphas instantly
            textViews.forEachIndexed { index, textView ->
                textView.alpha = if (index == activeTabIndex) 1.0f else 0.7f
            }
            
            Log.d(TAG, "Instant state change applied for tab $activeTabIndex")
            
        } catch (exception: Exception) {
            Log.e(TAG, "Failed to apply instant state change", exception)
        }
    }
    
    private fun clearActiveAnimations() {
        try {
            // Cancel ObjectAnimators
            activeAnimators.forEach { animator ->
                if (animator.isRunning) {
                    animator.cancel()
                }
            }
            activeAnimators.clear()
            
            // Cancel ViewPropertyAnimators
            activeViewAnimators.forEach { animator ->
                animator.cancel()
            }
            activeViewAnimators.clear()
            
            Log.v(TAG, "Active animations cleared")
            
        } catch (exception: Exception) {
            Log.w(TAG, "Error clearing animations", exception)
        }
    }
    
    /**
     * Enables or disables animations (for accessibility or performance)
     */
    fun setAnimationsEnabled(enabled: Boolean) {
        if (isAnimationEnabled != enabled) {
            isAnimationEnabled = enabled
            Log.d(TAG, "Animations ${if (enabled) "enabled" else "disabled"}")
            
            if (!enabled) {
                clearActiveAnimations()
            }
        }
    }
    
    /**
     * Checks if animations are currently enabled
     */
    fun areAnimationsEnabled(): Boolean = isAnimationEnabled
    
    /**
     * Cleanup method to be called when the controller is no longer needed
     */
    fun cleanup() {
        clearActiveAnimations()
        Log.d(TAG, "Animation controller cleaned up")
    }
}