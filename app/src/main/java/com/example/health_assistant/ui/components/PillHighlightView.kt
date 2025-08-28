package com.example.health_assistant.ui.components

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.View.MeasureSpec
import android.view.ViewOutlineProvider
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.core.content.ContextCompat
import com.example.health_assistant.R

/**
 * Custom view that provides a pill-shaped highlight overlay for bottom navigation tabs
 * Features gradient background, elevation shadow, and smooth position animations
 */
class PillHighlightView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        private const val TAG = "PillHighlight"
        private const val PILL_HEIGHT_DP = 36f  // Slightly larger for Figma design
        private const val PILL_CORNER_RADIUS_DP = 18f  // More rounded for modern look
        private const val PILL_ELEVATION_DP = 4f  // Subtle elevation
        private const val PILL_PADDING_EXTENSION_DP = 12f  // More padding for better coverage
    }

    private val pillRect = RectF()
    private val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val gradientDrawable: GradientDrawable
    private var currentTabIndex = 0
    private var tabCount = 4
    private var pillWidth = 0f
    private var pillHeight = 0f
    private var pillX = 0f
    private var pillY = 0f
    
    // Animation properties
    private var animatedX = 0f
        set(value) {
            field = value
            invalidate()
        }
    
    init {
        Log.d(TAG, "Initializing PillHighlightView")
        
        // Create gradient drawable for pill background
        gradientDrawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dpToPx(PILL_CORNER_RADIUS_DP)
            
            // Set gradient colors using health theme
            val startColor = ContextCompat.getColor(context, R.color.health_primary)
            val endColor = ContextCompat.getColor(context, R.color.health_accent)
            colors = intArrayOf(startColor, endColor)
            gradientType = GradientDrawable.LINEAR_GRADIENT
            orientation = GradientDrawable.Orientation.TOP_BOTTOM
            
            Log.d(TAG, "Gradient colors set - Start: $startColor, End: $endColor")
        }
        
        // Set elevation and shadow
        elevation = dpToPx(PILL_ELEVATION_DP)
        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: android.graphics.Outline) {
                outline.setRoundRect(
                    pillX.toInt(),
                    pillY.toInt(),
                    (pillX + pillWidth).toInt(),
                    (pillY + pillHeight).toInt(),
                    dpToPx(PILL_CORNER_RADIUS_DP)
                )
            }
        }
        
        // Initialize dimensions
        pillHeight = dpToPx(PILL_HEIGHT_DP)
        
        Log.d(TAG, "PillHighlightView initialized with height: ${pillHeight}dp")
    }
    
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // Force the height to match the bottom navigation height (56dp)
        val desiredHeight = dpToPx(56f).toInt()
        val heightSpec = MeasureSpec.makeMeasureSpec(desiredHeight, MeasureSpec.EXACTLY)
        super.onMeasure(widthMeasureSpec, heightSpec)
        
        Log.d(TAG, "Measured: ${measuredWidth}x${measuredHeight}")
    }
    
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        Log.d(TAG, "Size changed: ${w}x${h}, calculating pill dimensions")
        
        calculatePillDimensions()
        updatePillPosition(currentTabIndex, false)
        
        // Ensure pill is visible initially
        if (pillX == 0f && animatedX == 0f) {
            val tabWidth = width.toFloat() / tabCount
            val initialX = (currentTabIndex * tabWidth) + (tabWidth - pillWidth) / 2f
            pillX = initialX
            animatedX = initialX
            invalidate()
        }
    }
    
    private fun calculatePillDimensions() {
        if (width <= 0 || tabCount <= 0) return
        
        val tabWidth = width.toFloat() / tabCount
        
        // Make pill width appropriate for Figma-inspired design
        pillWidth = tabWidth * 0.7f // Use 70% of tab width for better coverage with new spacing
        
        // Center the pill vertically in the view
        pillY = (height - pillHeight) / 2f
        
        Log.d(TAG, "Pill dimensions calculated - Width: $pillWidth, Height: $pillHeight, Y: $pillY")
        Log.d(TAG, "Tab width: $tabWidth, View height: $height")
    }
    
    /**
     * Updates the pill position for the specified tab index
     * @param tabIndex The index of the active tab (0-based)
     * @param animate Whether to animate the position change
     */
    fun updatePillPosition(tabIndex: Int, animate: Boolean = true) {
        if (tabIndex < 0 || tabIndex >= tabCount) {
            Log.w(TAG, "Invalid tab index: $tabIndex, tab count: $tabCount")
            return
        }
        
        val oldTabIndex = currentTabIndex
        currentTabIndex = tabIndex
        
        val tabWidth = width.toFloat() / tabCount
        val targetX = (tabIndex * tabWidth) + (tabWidth - pillWidth) / 2f
        
        Log.d(TAG, "Positioning pill at x=$targetX, width=$pillWidth for tab $tabIndex (tabWidth=$tabWidth)")
        
        if (animate && oldTabIndex != tabIndex && pillX != 0f) {
            animatePillPosition(pillX, targetX)
        } else {
            pillX = targetX
            animatedX = targetX
            invalidate()
        }
        
        // Update outline for elevation shadow
        invalidateOutline()
    }
    
    private fun animatePillPosition(fromX: Float, toX: Float) {
        Log.d(TAG, "Animating pill position from $fromX to $toX")
        
        val animator = ObjectAnimator.ofFloat(this, "animatedX", fromX, toX).apply {
            duration = 250
            interpolator = FastOutSlowInInterpolator()
        }
        
        animator.start()
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        if (pillWidth <= 0 || pillHeight <= 0) return
        
        // Use animated X position
        val currentX = animatedX
        
        // Update pill rectangle
        pillRect.set(
            currentX,
            pillY,
            currentX + pillWidth,
            pillY + pillHeight
        )
        
        // Draw gradient background
        gradientDrawable.setBounds(
            currentX.toInt(),
            pillY.toInt(),
            (currentX + pillWidth).toInt(),
            (pillY + pillHeight).toInt()
        )
        gradientDrawable.draw(canvas)
        
        Log.v(TAG, "Drew pill at x=$currentX, y=$pillY, width=$pillWidth, height=$pillHeight")
    }
    
    /**
     * Sets the number of tabs for position calculations
     */
    fun setTabCount(count: Int) {
        if (count != tabCount) {
            tabCount = count
            Log.d(TAG, "Tab count updated to: $count")
            calculatePillDimensions()
            updatePillPosition(currentTabIndex, false)
        }
    }
    
    /**
     * Gets the current active tab index
     */
    fun getCurrentTabIndex(): Int = currentTabIndex
    
    private fun dpToPx(dp: Float): Float {
        return dp * context.resources.displayMetrics.density
    }
    
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        Log.d(TAG, "PillHighlightView detached from window")
    }
}