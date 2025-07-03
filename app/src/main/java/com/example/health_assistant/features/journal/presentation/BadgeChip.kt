package com.example.health_assistant.features.journal.presentation

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import androidx.core.content.ContextCompat
import com.example.health_assistant.R
import com.google.android.material.chip.Chip

/**
 * A custom Chip that displays a count badge on the right side
 */
class BadgeChip @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = com.google.android.material.R.attr.chipStyle
) : Chip(context, attrs, defStyleAttr) {

    private var count = 0
    private val badgePaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.colorChipSelected) // Using existing colorChipSelected
        isAntiAlias = true
    }

    private val textPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.colorOnPrimary) // Using existing colorOnPrimary
        textSize = resources.getDimensionPixelSize(R.dimen.chip_badge_text_size).toFloat()
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }

    private val badgeRadius = resources.getDimensionPixelSize(R.dimen.chip_badge_radius).toFloat()
    private val badgeMargin = resources.getDimensionPixelSize(R.dimen.chip_badge_margin).toFloat()

    fun setCount(count: Int) {
        this.count = count
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (count > 0) {
            // Draw the badge in the right side of the chip
            val cx = width - badgeRadius - badgeMargin
            val cy = height / 2f

            // Draw badge background
            canvas.drawCircle(cx, cy, badgeRadius, badgePaint)

            // Draw badge text
            val displayText = if (count > 99) "99+" else count.toString()
            canvas.drawText(
                displayText,
                cx,
                cy + textPaint.textSize / 3, // Offset to vertically center the text
                textPaint
            )
        }
    }
}
