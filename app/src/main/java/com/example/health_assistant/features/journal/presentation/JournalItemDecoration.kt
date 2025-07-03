package com.example.health_assistant.features.journal.presentation

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.text.format.DateUtils
import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.health_assistant.R
import com.example.health_assistant.features.journal.data.JournalEntryEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Custom ItemDecoration that adds date headers and separators between journal entries
 */
class JournalItemDecoration(
    private val context: Context,
    private val getEntryAtPosition: (Int) -> JournalEntryEntity?
) : RecyclerView.ItemDecoration() {

    private val headerPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.colorOnSurface)
        textSize = context.resources.getDimensionPixelSize(R.dimen.header_text_size).toFloat()
        isFakeBoldText = true
    }

    private val dividerPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.divider)
        strokeWidth = context.resources.getDimensionPixelSize(R.dimen.divider_height).toFloat()
    }

    private val headerHeight = context.resources.getDimensionPixelSize(R.dimen.header_height)
    private val dividerHeight = context.resources.getDimensionPixelSize(R.dimen.divider_height)
    private val horizontalPadding = context.resources.getDimensionPixelSize(R.dimen.header_padding)

    private val dateFormat = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
    private val calendar = Calendar.getInstance()

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val position = parent.getChildAdapterPosition(view)
        if (position < 0) return

        val showHeader = shouldShowHeaderForPosition(position)

        if (showHeader) {
            outRect.top = headerHeight
        }

        // Always add space for divider except for last item
        if (position < parent.adapter?.itemCount?.minus(1) ?: 0) {
            outRect.bottom = dividerHeight
        }
    }

    override fun onDraw(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        val left = parent.paddingLeft.toFloat()
        val right = parent.width - parent.paddingRight.toFloat()

        // Draw headers
        for (i in 0 until parent.childCount) {
            val child = parent.getChildAt(i)
            val position = parent.getChildAdapterPosition(child)
            if (position < 0) continue

            if (shouldShowHeaderForPosition(position)) {
                val entry = getEntryAtPosition(position) ?: continue
                val date = Date(entry.timestamp)
                val headerText = getHeaderText(date)

                val top = child.top - headerHeight.toFloat()
                val textY = top + headerHeight * 0.65f  // Position text vertically centered in header

                canvas.drawText(
                    headerText,
                    left + horizontalPadding,
                    textY,
                    headerPaint
                )
            }

            // Draw divider
            if (position < parent.adapter?.itemCount?.minus(1) ?: 0) {
                val dividerTop = child.bottom.toFloat()
                canvas.drawLine(left, dividerTop, right, dividerTop, dividerPaint)
            }
        }
    }

    private fun shouldShowHeaderForPosition(position: Int): Boolean {
        if (position == 0) return true

        val current = getEntryAtPosition(position)?.timestamp ?: return false
        val previous = getEntryAtPosition(position - 1)?.timestamp ?: return true

        val currentDate = Date(current)
        val previousDate = Date(previous)

        // If dates are from different days, show a header
        return !isSameDay(currentDate, previousDate)
    }

    private fun isSameDay(date1: Date, date2: Date): Boolean {
        calendar.time = date1
        val year1 = calendar.get(Calendar.YEAR)
        val day1 = calendar.get(Calendar.DAY_OF_YEAR)

        calendar.time = date2
        val year2 = calendar.get(Calendar.YEAR)
        val day2 = calendar.get(Calendar.DAY_OF_YEAR)

        return year1 == year2 && day1 == day2
    }

    private fun getHeaderText(date: Date): String {
        // If it's today, yesterday, or within the last week, use relative date
        val flags = DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_WEEKDAY
        val now = System.currentTimeMillis()

        if (DateUtils.isToday(date.time)) {
            return "Today"
        } else if (DateUtils.isToday(date.time + DateUtils.DAY_IN_MILLIS)) {
            return "Yesterday"
        } else if (now - date.time < DateUtils.WEEK_IN_MILLIS) {
            return DateUtils.formatDateTime(context, date.time, flags)
        }

        // Otherwise use the date format
        return dateFormat.format(date)
    }
}
