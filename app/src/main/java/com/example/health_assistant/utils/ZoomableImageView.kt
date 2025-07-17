package com.example.health_assistant.utils

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Matrix
import android.graphics.PointF
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import androidx.appcompat.widget.AppCompatImageView
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Custom ImageView that supports pinch-to-zoom and pan functionality
 * Features: Smooth zooming, panning, double-tap to zoom, boundaries checking
 */
class ZoomableImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    companion object {
        private const val MIN_ZOOM = 1f
        private const val MAX_ZOOM = 5f
        private const val DOUBLE_TAP_ZOOM = 2f

        // Touch modes
        private const val NONE = 0
        private const val DRAG = 1
        private const val ZOOM = 2
    }

    // Matrix for transformations
    private val matrix = Matrix()
    private val savedMatrix = Matrix()
    private val baseMatrix = Matrix() // Store the initial fit-to-screen matrix

    // Touch handling
    private var mode = NONE
    private val start = PointF()
    private val mid = PointF()
    private var oldDist = 1f
    private var d = 0f
    private var newRot = 0f
    private var lastEvent: FloatArray? = null

    // Gesture detectors
    private lateinit var scaleDetector: ScaleGestureDetector
    private lateinit var gestureDetector: GestureDetector

    // Current scale and boundaries
    private var currentScale = 1f
    private var baseScale = 1f // Store the initial fit-to-screen scale
    private var originalWidth = 0f
    private var originalHeight = 0f
    private var viewWidth = 0
    private var viewHeight = 0

    init {
        super.setScaleType(ScaleType.MATRIX)
        setupGestureDetectors()
    }

    private fun setupGestureDetectors() {
        scaleDetector = ScaleGestureDetector(context, ScaleListener())
        gestureDetector = GestureDetector(context, GestureListener())
    }

    override fun setImageDrawable(drawable: Drawable?) {
        super.setImageDrawable(drawable)
        if (drawable != null) {
            // Reset transformations when new image is set
            resetImageMatrix()
        }
    }

    /**
     * Reset image to fit screen
     */
    private fun resetImageMatrix() {
        // Wait for view to be measured
        post {
            val drawable = drawable ?: return@post

            viewWidth = width - paddingLeft - paddingRight
            viewHeight = height - paddingTop - paddingBottom

            if (viewWidth <= 0 || viewHeight <= 0) return@post

            originalWidth = drawable.intrinsicWidth.toFloat()
            originalHeight = drawable.intrinsicHeight.toFloat()

            if (originalWidth <= 0 || originalHeight <= 0) return@post

            // Calculate scale to fit image in view
            val scaleX = viewWidth / originalWidth
            val scaleY = viewHeight / originalHeight
            val scale = min(scaleX, scaleY)

            // Center the image
            val dx = (viewWidth - originalWidth * scale) / 2f
            val dy = (viewHeight - originalHeight * scale) / 2f

            // Reset and store the base matrix
            matrix.reset()
            matrix.postScale(scale, scale)
            matrix.postTranslate(dx, dy)
            baseMatrix.set(matrix)
            
            // Store the base scale
            baseScale = scale
            currentScale = scale

            imageMatrix = matrix
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)

        val curr = PointF(event.x, event.y)

        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                savedMatrix.set(matrix)
                start.set(curr)
                mode = DRAG
                lastEvent = null
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                oldDist = spacing(event)
                if (oldDist > 10f) {
                    savedMatrix.set(matrix)
                    midPoint(mid, event)
                    mode = ZOOM
                }
                lastEvent = FloatArray(4)
                lastEvent!![0] = event.getX(0)
                lastEvent!![1] = event.getX(1)
                lastEvent!![2] = event.getY(0)
                lastEvent!![3] = event.getY(1)
                d = rotation(event)
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                mode = NONE
                lastEvent = null
                
                // If scale is close to MIN_ZOOM, snap back to exact MIN_ZOOM
                if (currentScale < MIN_ZOOM * 1.1f && currentScale > MIN_ZOOM * 0.9f) {
                    resetImageMatrix()
                }
            }

            MotionEvent.ACTION_MOVE -> {
                if (mode == DRAG) {
                    matrix.set(savedMatrix)
                    val dx = curr.x - start.x
                    val dy = curr.y - start.y
                    matrix.postTranslate(dx, dy)
                    checkBounds()
                } else if (mode == ZOOM) {
                    val newDist = spacing(event)
                    if (newDist > 10f) {
                        matrix.set(savedMatrix)
                        val scale = newDist / oldDist
                        matrix.postScale(scale, scale, mid.x, mid.y)
                        checkScale()
                        checkBounds()
                    }
                }
            }
        }

        imageMatrix = matrix
        return true
    }

    private fun checkScale() {
        val values = FloatArray(9)
        matrix.getValues(values)
        val scaleX = values[Matrix.MSCALE_X]
        val scaleY = values[Matrix.MSCALE_Y]
        val scale = max(scaleX, scaleY)

        if (scale < MIN_ZOOM || scale > MAX_ZOOM) {
            val constrainedScale = max(MIN_ZOOM, min(scale, MAX_ZOOM))
            val scaleFactor = constrainedScale / scale
            matrix.postScale(scaleFactor, scaleFactor, mid.x, mid.y)
        }

        currentScale = max(scaleX, scaleY)
    }

    private fun checkBounds() {
        val values = FloatArray(9)
        matrix.getValues(values)

        val scaleX = values[Matrix.MSCALE_X]
        val scaleY = values[Matrix.MSCALE_Y]
        val transX = values[Matrix.MTRANS_X]
        val transY = values[Matrix.MTRANS_Y]

        val imageWidth = originalWidth * scaleX
        val imageHeight = originalHeight * scaleY

        var deltaX = 0f
        var deltaY = 0f

        // Check horizontal bounds
        if (imageWidth <= viewWidth) {
            // Image is smaller than view, center it
            deltaX = (viewWidth - imageWidth) / 2f - transX
        } else {
            // Image is larger than view, check boundaries
            if (transX > 0) {
                deltaX = -transX
            } else if (transX + imageWidth < viewWidth) {
                deltaX = viewWidth - imageWidth - transX
            }
        }

        // Check vertical bounds
        if (imageHeight <= viewHeight) {
            // Image is smaller than view, center it
            deltaY = (viewHeight - imageHeight) / 2f - transY
        } else {
            // Image is larger than view, check boundaries
            if (transY > 0) {
                deltaY = -transY
            } else if (transY + imageHeight < viewHeight) {
                deltaY = viewHeight - imageHeight - transY
            }
        }

        matrix.postTranslate(deltaX, deltaY)
    }

    private fun spacing(event: MotionEvent): Float {
        val x = event.getX(0) - event.getX(1)
        val y = event.getY(0) - event.getY(1)
        return sqrt((x * x + y * y).toDouble()).toFloat()
    }

    private fun midPoint(point: PointF, event: MotionEvent) {
        val x = event.getX(0) + event.getX(1)
        val y = event.getY(0) + event.getY(1)
        point.set(x / 2, y / 2)
    }

    private fun rotation(event: MotionEvent): Float {
        val deltaX = (event.getX(0) - event.getX(1)).toDouble()
        val deltaY = (event.getY(0) - event.getY(1)).toDouble()
        val radians = kotlin.math.atan2(deltaY, deltaX)
        return Math.toDegrees(radians).toFloat()
    }

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            mode = ZOOM
            return true
        }

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            var scaleFactor = detector.scaleFactor
            val prevScale = currentScale
            currentScale *= scaleFactor

            // Limit scale
            if (currentScale > MAX_ZOOM) {
                currentScale = MAX_ZOOM
                scaleFactor = MAX_ZOOM / prevScale
            } else if (currentScale < MIN_ZOOM) {
                currentScale = MIN_ZOOM
                scaleFactor = MIN_ZOOM / prevScale
            }

            matrix.postScale(scaleFactor, scaleFactor, detector.focusX, detector.focusY)
            checkBounds()
            imageMatrix = matrix
            return true
        }
        
        override fun onScaleEnd(detector: ScaleGestureDetector) {
            // If scale is very close to MIN_ZOOM, snap back to exact MIN_ZOOM
            if (currentScale < MIN_ZOOM * 1.05f) {
                resetImageMatrix()
            }
        }
    }

    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent): Boolean {
            // If already zoomed in, reset to original size
            if (currentScale > baseScale * 1.1f) {
                resetImageMatrix()
            } else {
                // Zoom in to double tap zoom level
                val targetScale = min(DOUBLE_TAP_ZOOM, MAX_ZOOM)
                val scaleFactor = targetScale / currentScale
                matrix.postScale(scaleFactor, scaleFactor, e.x, e.y)
                currentScale = targetScale
                checkBounds()
                imageMatrix = matrix
            }
            return true
        }

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            // Let parent handle single tap (for closing dialog)
            performClick()
            return super.onSingleTapConfirmed(e)
        }
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        viewWidth = w - paddingLeft - paddingRight
        viewHeight = h - paddingTop - paddingBottom
        if (drawable != null) {
            resetImageMatrix()
        }
    }
}