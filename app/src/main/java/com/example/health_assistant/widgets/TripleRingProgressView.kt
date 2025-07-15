package com.example.health_assistant.widgets

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.core.content.ContextCompat
import com.example.health_assistant.R

/**
 * A custom view that displays three concentric rings representing different health metrics:
 * - Outer ring (orange): Steps
 * - Middle ring (green): Calories
 * - Inner ring (blue): Heart Points
 *
 * Each ring shows progress towards a goal.
 *
 */
class TripleRingProgressView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Ring colors
    private val outerRingColor = ContextCompat.getColor(context, R.color.progress_orange)
    private val middleRingColor = ContextCompat.getColor(context, R.color.progress_green)
    private val innerRingColor = ContextCompat.getColor(context, R.color.progress_blue)

    // Ring background colors (lighter versions of the progress colors)
    private val outerRingBgColor = adjustAlpha(outerRingColor, 0.2f)
    private val middleRingBgColor = adjustAlpha(middleRingColor, 0.2f)
    private val innerRingBgColor = adjustAlpha(innerRingColor, 0.2f)

    // Paint objects for drawing
    private val outerRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val middleRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val innerRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    // Ring metrics
    private var outerRingProgress = 0f  // Progress values (0-1)
    private var middleRingProgress = 0f
    private var innerRingProgress = 0f

    // Animated progress values
    private var animatedOuterRingProgress = 0f
    private var animatedMiddleRingProgress = 0f
    private var animatedInnerRingProgress = 0f

    // Ring thickness
    private var outerRingWidth = 0f
    private var middleRingWidth = 0f
    private var innerRingWidth = 0f

    // The bounds of the rings
    private val outerRingBounds = RectF()
    private val middleRingBounds = RectF()
    private val innerRingBounds = RectF()

    // Animation duration in milliseconds - optimized for smooth, visible animation
    private val animationDuration = 500L

    // Animation objects
    private var outerRingAnimator: ValueAnimator? = null
    private var middleRingAnimator: ValueAnimator? = null
    private var innerRingAnimator: ValueAnimator? = null

    init {
        // Default ring width ratios can be customized
        outerRingWidth = 30f
        middleRingWidth = 25f
        innerRingWidth = 20f
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        // Calculate dimensions based on the view size
        val minDimension = minOf(w, h).toFloat()
        val centerX = w / 2f
        val centerY = h / 2f

        // Calculate padding to prevent rings from being cut off
        val maxRingWidth = maxOf(outerRingWidth, middleRingWidth, innerRingWidth)
        val padding = maxRingWidth / 2

        // Set up the outer ring bounds
        val outerRingRadius = (minDimension / 2) - padding
        outerRingBounds.set(
            centerX - outerRingRadius,
            centerY - outerRingRadius,
            centerX + outerRingRadius,
            centerY + outerRingRadius
        )
        outerRingPaint.strokeWidth = outerRingWidth

        // Set up the middle ring bounds
        val middleRingRadius = outerRingRadius - outerRingWidth - 8f
        middleRingBounds.set(
            centerX - middleRingRadius,
            centerY - middleRingRadius,
            centerX + middleRingRadius,
            centerY + middleRingRadius
        )
        middleRingPaint.strokeWidth = middleRingWidth

        // Set up the inner ring bounds
        val innerRingRadius = middleRingRadius - middleRingWidth - 8f
        innerRingBounds.set(
            centerX - innerRingRadius,
            centerY - innerRingRadius,
            centerX + innerRingRadius,
            centerY + innerRingRadius
        )
        innerRingPaint.strokeWidth = innerRingWidth
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw outer ring background (full circle)
        outerRingPaint.color = outerRingBgColor
        canvas.drawArc(outerRingBounds, 0f, 360f, false, outerRingPaint)

        // Draw middle ring background
        middleRingPaint.color = middleRingBgColor
        canvas.drawArc(middleRingBounds, 0f, 360f, false, middleRingPaint)

        // Draw inner ring background
        innerRingPaint.color = innerRingBgColor
        canvas.drawArc(innerRingBounds, 0f, 360f, false, innerRingPaint)

        // Draw progress arcs

        // Outer ring progress
        outerRingPaint.color = outerRingColor
        canvas.drawArc(outerRingBounds, -90f, animatedOuterRingProgress * 360, false, outerRingPaint)

        // Middle ring progress
        middleRingPaint.color = middleRingColor
        canvas.drawArc(middleRingBounds, -90f, animatedMiddleRingProgress * 360, false, middleRingPaint)

        // Inner ring progress
        innerRingPaint.color = innerRingColor
        canvas.drawArc(innerRingBounds, -90f, animatedInnerRingProgress * 360, false, innerRingPaint)
    }

    /**
     * Set the progress for the outer ring (steps)
     * @param current Current value
     * @param target Target value
     */
    fun setStepsProgress(current: Int, target: Int) {
        val progress = if (target > 0) (current.toFloat() / target).coerceIn(0f, 1f) else 0f
        animateProgress(progress, outerRingProgress, RING_TYPE_OUTER)
        outerRingProgress = progress
    }

    /**
     * Set the progress for the middle ring (calories)
     * @param current Current value
     * @param target Target value
     */
    fun setCaloriesProgress(current: Int, target: Int) {
        val progress = if (target > 0) (current.toFloat() / target).coerceIn(0f, 1f) else 0f
        animateProgress(progress, middleRingProgress, RING_TYPE_MIDDLE)
        middleRingProgress = progress
    }

    /**
     * Set the progress for the inner ring (heart points)
     * @param current Current value
     * @param target Target value
     */
    fun setHeartPointsProgress(current: Int, target: Int) {
        val progress = if (target > 0) (current.toFloat() / target).coerceIn(0f, 1f) else 0f
        animateProgress(progress, innerRingProgress, RING_TYPE_INNER)
        innerRingProgress = progress
    }

    /**
     * Set the progress for the inner ring (workout duration) - DEPRECATED
     * Use setHeartPointsProgress() instead
     * @param current Current value
     * @param target Target value
     */
    @Deprecated("Use setHeartPointsProgress() instead", ReplaceWith("setHeartPointsProgress(current, target)"))
    fun setWorkoutProgress(current: Int, target: Int) {
        setHeartPointsProgress(current, target)
    }

    /**
     * Animate the progress change for a specified ring
     * Allows all rings to start simultaneously but prevents sequence restarts
     */
    private fun animateProgress(newProgress: Float, oldProgress: Float, ringType: Int) {
        // Check if this specific ring is already animating to prevent individual restarts
        when (ringType) {
            RING_TYPE_OUTER -> {
                if (outerRingAnimator?.isRunning == true) return
                outerRingAnimator?.cancel()
                outerRingAnimator = ValueAnimator.ofFloat(0f, newProgress).apply {
                    duration = animationDuration
                    interpolator = DecelerateInterpolator()
                    addUpdateListener { valueAnimator ->
                        animatedOuterRingProgress = valueAnimator.animatedValue as Float
                        invalidate()
                    }
                    start()
                }
            }
            RING_TYPE_MIDDLE -> {
                if (middleRingAnimator?.isRunning == true) return
                middleRingAnimator?.cancel()
                middleRingAnimator = ValueAnimator.ofFloat(0f, newProgress).apply {
                    duration = animationDuration
                    interpolator = DecelerateInterpolator()
                    addUpdateListener { valueAnimator ->
                        animatedMiddleRingProgress = valueAnimator.animatedValue as Float
                        invalidate()
                    }
                    start()
                }
            }
            RING_TYPE_INNER -> {
                if (innerRingAnimator?.isRunning == true) return
                innerRingAnimator?.cancel()
                innerRingAnimator = ValueAnimator.ofFloat(0f, newProgress).apply {
                    duration = animationDuration
                    interpolator = DecelerateInterpolator()
                    addUpdateListener { valueAnimator ->
                        animatedInnerRingProgress = valueAnimator.animatedValue as Float
                        invalidate()
                    }
                    start()
                }
            }
        }
    }

    /**
     * Cancel all ongoing animations when the view is detached
     */
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        outerRingAnimator?.cancel()
        middleRingAnimator?.cancel()
        innerRingAnimator?.cancel()
    }

    /**
     * Adjust the alpha value of a color
     */
    private fun adjustAlpha(color: Int, factor: Float): Int {
        val alpha = (Color.alpha(color) * factor).toInt()
        val red = Color.red(color)
        val green = Color.green(color)
        val blue = Color.blue(color)
        return Color.argb(alpha, red, green, blue)
    }

    companion object {
        private const val RING_TYPE_OUTER = 0
        private const val RING_TYPE_MIDDLE = 1
        private const val RING_TYPE_INNER = 2
    }
}