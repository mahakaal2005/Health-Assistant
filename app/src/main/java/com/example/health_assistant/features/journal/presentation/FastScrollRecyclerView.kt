package com.example.health_assistant.features.journal.presentation

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.health_assistant.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

/**
 * Custom RecyclerView that adds a fast scroll thumb and section-based scrolling
 */
class FastScrollRecyclerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RecyclerView(context, attrs, defStyleAttr) {

    companion object {
        private const val TAG = "FastScrollRecyclerView"
        private const val BUBBLE_HIDE_DELAY = 1500L // Hide bubble after 1.5 seconds
    }

    // Interface for providing dates for positions
    interface DateProvider {
        fun getDateForPosition(position: Int): Date?
    }

    // Date provider to be set by the parent fragment/activity
    var dateProvider: DateProvider? = null

    // State flags
    private var isScrollbarVisible = true
    private var hasItems = false

    private val scrollbarWidth = context.resources.getDimensionPixelSize(R.dimen.fast_scroller_width)
    private val scrollbarMinHeight = context.resources.getDimensionPixelSize(R.dimen.fast_scroller_min_height)
    private val scrollbarMargin = context.resources.getDimensionPixelSize(R.dimen.fast_scroller_margin)
    private val scrollbarRadius = context.resources.getDimensionPixelSize(R.dimen.fast_scroller_radius).toFloat()

    private val bubbleHeight = context.resources.getDimensionPixelSize(R.dimen.fast_scroller_bubble_height)
    private val bubbleWidth = context.resources.getDimensionPixelSize(R.dimen.fast_scroller_bubble_width)
    private val bubbleRadius = context.resources.getDimensionPixelSize(R.dimen.fast_scroller_bubble_radius).toFloat()
    private val bubbleMargin = context.resources.getDimensionPixelSize(R.dimen.fast_scroller_bubble_margin)

    private val scrollbarPaint = Paint().apply {
        isAntiAlias = true
        color = ContextCompat.getColor(context, R.color.colorPrimary) // Using existing colorPrimary
        alpha = 100 // Semi-transparent thumb
    }

    private val bubblePaint = Paint().apply {
        isAntiAlias = true
        color = ContextCompat.getColor(context, R.color.colorPrimary) // Using existing colorPrimary
    }

    private val textPaint = Paint().apply {
        isAntiAlias = true
        color = ContextCompat.getColor(context, R.color.colorOnPrimary) // Using existing colorOnPrimary
        textAlign = Paint.Align.CENTER
        textSize = context.resources.getDimensionPixelSize(R.dimen.fast_scroller_text_size).toFloat()
        typeface = Typeface.DEFAULT_BOLD
    }

    private var thumbHeight = 0
    private var thumbOffset = 0f

    private var isDragging = false
    private var currentDate: Date? = null
    private var isBubbleVisible = false

    private val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
    private val hideHandler = Handler(Looper.getMainLooper())
    private val hideRunnable = Runnable {
        isBubbleVisible = false
        invalidate()
    }

    // Observer for adapter data changes
    private val adapterObserver = object : RecyclerView.AdapterDataObserver() {
        override fun onChanged() {
            updateEmptyState()
        }

        override fun onItemRangeChanged(positionStart: Int, itemCount: Int) {
            updateEmptyState()
        }

        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
            updateEmptyState()
        }

        override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
            updateEmptyState()
        }
    }

    // AdapterDataObserver can only be registered when adapter is set
    private var adapterObserverRegistered = false

    init {
        // This will be called when the RecyclerView is first initialized
        Log.d(TAG, "Initializing FastScrollRecyclerView")
    }

    /**
     * Override setAdapter to add our observer when an adapter is set
     */
    override fun setAdapter(adapter: Adapter<*>?) {
        // Remove observer from old adapter
        getAdapter()?.unregisterAdapterDataObserver(adapterObserver)
        adapterObserverRegistered = false

        // Set new adapter
        super.setAdapter(adapter)

        // Add observer to new adapter if not null
        adapter?.registerAdapterDataObserver(adapterObserver)
        adapterObserverRegistered = adapter != null

        // Update state immediately
        updateEmptyState()
    }

    /**
     * Updates the state based on whether adapter has items
     */
    private fun updateEmptyState() {
        val adapter = adapter
        hasItems = adapter != null && adapter.itemCount > 0
        isScrollbarVisible = hasItems
        invalidate()

        Log.d(TAG, "Updated empty state: hasItems=$hasItems")
    }

    // Updated function to safely get entry date for a position
    private fun getEntryDateAtPosition(position: Int): Date? {
        return dateProvider?.getDateForPosition(position)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Don't draw scrollbar when there are no items
        if (!isScrollbarVisible || !hasItems) {
            return
        }

        try {
            val availableHeight = height - paddingTop - paddingBottom
            if (availableHeight <= 0) {
                return // Can't draw in zero or negative height
            }

            val scrollExtent = computeVerticalScrollExtent()
            val scrollRange = computeVerticalScrollRange()

            // Safety check for empty or invalid values
            if (scrollRange <= 0 || scrollExtent <= 0 || availableHeight <= 0) {
                return
            }

            // Safely calculate thumb height
            thumbHeight = max(scrollbarMinHeight, (availableHeight * scrollExtent) / scrollRange)

            // Draw scrollbar track
            val trackLeft = width - scrollbarWidth - scrollbarMargin
            val trackRight = width - scrollbarMargin
            val trackTop = paddingTop.toFloat()
            val trackBottom = height - paddingBottom.toFloat()

            // Draw scrollbar thumb
            val thumbLeft = trackLeft
            val thumbRight = trackRight

            // Safely calculate available scroll height
            val availableScrollHeight = availableHeight - thumbHeight
            if (availableScrollHeight <= 0) {
                return // No space for scrolling
            }

            // Safely calculate scroll offset with division-by-zero protection
            val scrollOffset = if (scrollRange > scrollExtent) {
                computeVerticalScrollOffset() / (scrollRange - scrollExtent).toFloat()
            } else {
                0f
            }

            thumbOffset = trackTop + scrollOffset * availableScrollHeight
            val thumbTop = thumbOffset
            val thumbBottom = thumbOffset + thumbHeight

            // Draw the thumb
            canvas.drawRoundRect(
                thumbLeft.toFloat(),
                thumbTop,
                thumbRight.toFloat(),
                thumbBottom,
                scrollbarRadius,
                scrollbarRadius,
                scrollbarPaint
            )

            // Draw date bubble if visible
            if (isBubbleVisible && currentDate != null) {
                val bubbleLeft = thumbLeft - bubbleWidth - bubbleMargin
                val bubbleRight = bubbleLeft + bubbleWidth
                val bubbleTop = thumbTop
                val bubbleBottom = bubbleTop + bubbleHeight

                // Draw bubble background
                canvas.drawRoundRect(
                    bubbleLeft.toFloat(),
                    bubbleTop,
                    bubbleRight.toFloat(),
                    bubbleBottom,
                    bubbleRadius,
                    bubbleRadius,
                    bubblePaint
                )

                // Draw date text in bubble
                val dateText = try {
                    dateFormat.format(currentDate!!)
                } catch (e: Exception) {
                    Log.e(TAG, "Error formatting date", e)
                    "" // Use empty string as fallback
                }

                // Only draw text if we have a valid formatted date
                if (dateText.isNotEmpty()) {
                    canvas.drawText(
                        dateText,
                        (bubbleLeft + bubbleWidth / 2).toFloat(),
                        bubbleTop + bubbleHeight / 2 + textPaint.textSize / 3,
                        textPaint
                    )
                }
            }
        } catch (e: Exception) {
            // Catch any unexpected exceptions during drawing to prevent crashes
            Log.e(TAG, "Error drawing fast scroller", e)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Don't handle touch events when no items or scrollbar is invisible
        if (!isScrollbarVisible || !hasItems) {
            return super.onTouchEvent(event)
        }

        val result = super.onTouchEvent(event)

        try {
            if (event.action == MotionEvent.ACTION_DOWN) {
                if (event.x >= width - scrollbarWidth - scrollbarMargin * 2) {
                    isDragging = true
                    handleTouchEvent(event.y)
                    return true
                }
            } else if (event.action == MotionEvent.ACTION_MOVE) {
                if (isDragging) {
                    handleTouchEvent(event.y)
                    return true
                }
            } else if (event.action == MotionEvent.ACTION_UP) {
                if (isDragging) {
                    isDragging = false
                    hideHandler.postDelayed(hideRunnable, BUBBLE_HIDE_DELAY)
                    return true
                }
            }
        } catch (e: Exception) {
            // Catch any unexpected exceptions during touch handling
            Log.e(TAG, "Error handling touch event", e)
            isDragging = false
        }

        return result
    }

    private fun handleTouchEvent(y: Float) {
        // Cancel any pending hide operations
        hideHandler.removeCallbacks(hideRunnable)

        try {
            val availableHeight = height - paddingTop - paddingBottom - thumbHeight

            // Safety check for invalid height
            if (availableHeight <= 0) {
                return
            }

            val normalizedY = min(availableHeight.toFloat(), max(0f, y - paddingTop - thumbHeight / 2))
            val scrollRatio = normalizedY / availableHeight.toFloat()

            // Calculate position to scroll to
            val scrollExtent = computeVerticalScrollExtent()
            val scrollRange = computeVerticalScrollRange()

            // Safety check for empty or invalid values
            if (scrollRange <= 0 || scrollExtent <= 0) {
                return
            }

            // Safely calculate target scroll position
            val actualScrollRange = max(0, scrollRange - scrollExtent)
            val targetScroll = (actualScrollRange * scrollRatio).toInt()
            scrollTo(0, targetScroll)

            // Find the date at the current scroll position
            val layoutManager = layoutManager
            if (layoutManager != null) {
                val firstVisiblePosition = layoutManager.findFirstVisibleItemPosition()
                if (firstVisiblePosition != NO_POSITION) {
                    val entryDate = getEntryDateAtPosition(firstVisiblePosition)
                    if (entryDate != null) {
                        currentDate = entryDate
                        isBubbleVisible = true
                    }
                }
            }

            invalidate()
        } catch (e: Exception) {
            // Catch any unexpected exceptions
            Log.e(TAG, "Error handling scroll touch", e)
        }
    }

    // Helper extension function for finding first visible position
    private fun LayoutManager?.findFirstVisibleItemPosition(): Int {
        if (this is androidx.recyclerview.widget.LinearLayoutManager) {
            return findFirstVisibleItemPosition()
        }
        return NO_POSITION
    }

    override fun scrollTo(x: Int, y: Int) {
        try {
            scrollBy(0, y - computeVerticalScrollOffset())
        } catch (e: Exception) {
            Log.e(TAG, "Error during scrollTo", e)
        }
    }
}
