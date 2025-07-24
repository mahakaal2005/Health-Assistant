package com.example.health_assistant.core.design.components

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import com.example.health_assistant.R
import com.example.health_assistant.core.design.tokens.HealthColors
import com.example.health_assistant.core.design.tokens.HealthTypography
import com.google.android.material.button.MaterialButton
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.textview.MaterialTextView
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 * Health Date Time Picker Component
 * 
 * Standardized date and time selection UI for journal entries and other forms.
 * Provides consistent styling and accessibility support for date/time selection.
 * 
 * Features:
 * - Consistent button styling using HealthButton.Secondary
 * - Unified typography using HealthTypography.Label.medium
 * - Material 3 date and time pickers with health-focused styling
 * - Accessibility support with proper content descriptions
 * 
 * Usage:
 * ```
 * val dateTimePicker = HealthDateTimePicker(context)
 * dateTimePicker.setOnDateTimeSelectedListener { date, time ->
 *     // Handle selected date and time
 * }
 * ```
 */
class HealthDateTimePicker @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val dateButton: MaterialButton
    private val timeButton: MaterialButton
    private val dateLabel: MaterialTextView
    private val timeLabel: MaterialTextView

    private var selectedDate: Calendar = Calendar.getInstance()
    private var onDateTimeSelectedListener: ((Date, String) -> Unit)? = null
    private var fragmentManager: FragmentManager? = null

    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    init {
        orientation = VERTICAL
        
        // Inflate the layout
        val inflater = LayoutInflater.from(context)
        inflater.inflate(R.layout.health_date_time_picker, this, true)
        
        // Get references to views
        dateLabel = findViewById(R.id.dateLabel)
        dateButton = findViewById(R.id.dateButton)
        timeLabel = findViewById(R.id.timeLabel)
        timeButton = findViewById(R.id.timeButton)
        
        setupStyling()
        setupClickListeners()
        updateDisplays()
    }

    /**
     * Apply consistent styling using design system tokens
     */
    private fun setupStyling() {
        // Apply typography to labels
        dateLabel.setTextAppearance(HealthTypography.Body.medium)
        timeLabel.setTextAppearance(HealthTypography.Body.medium)
        
        // Apply consistent colors
        dateLabel.setTextColor(ContextCompat.getColor(context, HealthColors.Text.primary))
        timeLabel.setTextColor(ContextCompat.getColor(context, HealthColors.Text.primary))
        
        // Style buttons with secondary styling
        styleButton(dateButton)
        styleButton(timeButton)
        
        // Set accessibility content descriptions
        dateButton.contentDescription = "Select date for journal entry"
        timeButton.contentDescription = "Select time for journal entry"
    }

    /**
     * Apply consistent button styling using design system tokens
     */
    private fun styleButton(button: MaterialButton) {
        // Apply color styling
        button.setTextColor(ContextCompat.getColor(context, HealthColors.Secondary.default))
        button.setBackgroundColor(ContextCompat.getColor(context, HealthColors.Secondary.container))
        button.strokeColor = ContextCompat.getColorStateList(context, HealthColors.Secondary.default)
        
        // Apply dimension styling
        button.strokeWidth = resources.getDimensionPixelSize(R.dimen.ds_border_width_thin)
        button.cornerRadius = resources.getDimensionPixelSize(R.dimen.ds_component_card_radius_small)
        button.minimumHeight = resources.getDimensionPixelSize(R.dimen.ds_component_touch_target)
        
        // Apply typography
        button.setTextAppearance(HealthTypography.Body.medium)
    }

    /**
     * Set up click listeners for date and time buttons
     */
    private fun setupClickListeners() {
        dateButton.setOnClickListener {
            showDatePicker()
        }
        
        timeButton.setOnClickListener {
            showTimePicker()
        }
    }

    /**
     * Show Material Date Picker with health-focused styling
     */
    private fun showDatePicker() {
        // Calculate reasonable date bounds (past 1 year to future 1 year)
        val calendar = Calendar.getInstance()
        val currentDate = calendar.timeInMillis
        
        // Set bounds: 1 year ago to 1 year from now
        calendar.add(Calendar.YEAR, -1)
        val minDate = calendar.timeInMillis
        
        calendar.add(Calendar.YEAR, 2) // 1 year from current (since we subtracted 1)
        val maxDate = calendar.timeInMillis
        
        val constraints = CalendarConstraints.Builder()
            .setStart(minDate)
            .setEnd(maxDate)
            .build()

        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select date for journal entry")
            .setSelection(selectedDate.timeInMillis)
            .setCalendarConstraints(constraints)
            .build()

        datePicker.addOnPositiveButtonClickListener { selection ->
            selectedDate.timeInMillis = selection
            updateDisplays()
            notifyDateTimeSelected()
        }

        // Show the picker using provided fragment manager
        fragmentManager?.let { fm ->
            datePicker.show(fm, "DATE_PICKER")
        }
    }

    /**
     * Show Material Time Picker with health-focused styling
     */
    private fun showTimePicker() {
        val timePicker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setHour(selectedDate.get(Calendar.HOUR_OF_DAY))
            .setMinute(selectedDate.get(Calendar.MINUTE))
            .setTitleText("Select time for journal entry")
            .build()

        timePicker.addOnPositiveButtonClickListener {
            selectedDate.set(Calendar.HOUR_OF_DAY, timePicker.hour)
            selectedDate.set(Calendar.MINUTE, timePicker.minute)
            updateDisplays()
            notifyDateTimeSelected()
        }

        // Show the picker using provided fragment manager
        fragmentManager?.let { fm ->
            timePicker.show(fm, "TIME_PICKER")
        }
    }

    /**
     * Update button displays with current selected date and time
     */
    private fun updateDisplays() {
        dateButton.text = dateFormat.format(selectedDate.time)
        timeButton.text = timeFormat.format(selectedDate.time)
    }

    /**
     * Notify listener of date/time selection
     */
    private fun notifyDateTimeSelected() {
        onDateTimeSelectedListener?.invoke(selectedDate.time, timeFormat.format(selectedDate.time))
    }

    /**
     * Set listener for date/time selection events
     */
    fun setOnDateTimeSelectedListener(listener: (Date, String) -> Unit) {
        onDateTimeSelectedListener = listener
    }

    /**
     * Set fragment manager for showing date/time pickers
     */
    fun setFragmentManager(fm: FragmentManager) {
        fragmentManager = fm
    }

    /**
     * Get the currently selected date
     */
    fun getSelectedDate(): Date = selectedDate.time

    /**
     * Set the selected date programmatically
     */
    fun setSelectedDate(date: Date) {
        selectedDate.time = date
        updateDisplays()
    }

    /**
     * Get the currently selected time as formatted string
     */
    fun getSelectedTimeString(): String = timeFormat.format(selectedDate.time)

    /**
     * Get the currently selected timestamp
     */
    fun getSelectedTimestamp(): Long = selectedDate.timeInMillis

    /**
     * Set the selected date and time to current moment
     */
    fun setToCurrentDateTime() {
        selectedDate = Calendar.getInstance()
        updateDisplays()
    }

    /**
     * Enable or disable the date/time selection
     */
    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        dateButton.isEnabled = enabled
        timeButton.isEnabled = enabled
        
        // Update visual state
        val alpha = if (enabled) 1.0f else 0.6f
        dateButton.alpha = alpha
        timeButton.alpha = alpha
        dateLabel.alpha = alpha
        timeLabel.alpha = alpha
    }

    companion object {
        /**
         * Create a date/time picker with current date/time selected
         */
        fun createWithCurrentDateTime(context: Context): HealthDateTimePicker {
            return HealthDateTimePicker(context).apply {
                setToCurrentDateTime()
            }
        }

        /**
         * Create a date/time picker with specific date selected
         */
        fun createWithDate(context: Context, date: Date): HealthDateTimePicker {
            return HealthDateTimePicker(context).apply {
                setSelectedDate(date)
            }
        }
    }
}