package com.example.health_assistant.utils

import android.graphics.Color
import android.graphics.Typeface
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.example.health_assistant.data.models.DailyStepData
import java.text.NumberFormat
import java.util.*

/**
 * Utility class for configuring and populating the steps chart
 */
object StepsChartManager {

    // Chart colors - Updated to avoid triple ring conflicts
    private const val GOAL_ACHIEVED_COLOR = "#8BC34A"  // Light Green (different from triple ring)
    private const val GOAL_NOT_ACHIEVED_COLOR = "#FF7043"  // Deep Orange (different from triple ring)
    private const val TODAY_COLOR = "#3F51B5"  // Indigo (different from triple ring blue)
    private const val GOAL_LINE_COLOR = "#E91E63"  // Pink for goal line
    private const val TEXT_COLOR = "#000000"  // Black for axis text
    private const val GRID_COLOR = "#30000000"  // Light black grid

    /**
     * Configure and populate the chart with step data
     */
    fun setupStepsChart(chart: BarChart, weeklyData: List<DailyStepData>) {
        if (weeklyData.isEmpty()) {
            chart.clear()
            return
        }

        // Configure chart appearance
        configureChartAppearance(chart)

        // Setup axes
        setupXAxis(chart, weeklyData)
        setupYAxis(chart, weeklyData)

        // Create and set data
        val barData = createBarData(weeklyData)
        chart.data = barData

        // Add goal line
        addGoalLine(chart, weeklyData)

        // Animate chart
        chart.animateY(1000, Easing.EaseOutCubic)

        // Refresh chart
        chart.invalidate()
    }

    /**
     * Configure general chart appearance and behavior
     */
    private fun configureChartAppearance(chart: BarChart) {
        chart.apply {
            // Basic configuration
            description.isEnabled = false
            legend.isEnabled = false
            setTouchEnabled(true)
            isDragEnabled = false
            setScaleEnabled(false)
            setPinchZoom(false)
            setDrawGridBackground(false)
            setDrawBorders(false)

            // Background and margins
            setBackgroundColor(Color.TRANSPARENT)
            setExtraOffsets(8f, 16f, 8f, 8f)

            // Disable right Y-axis
            axisRight.isEnabled = false

            // Set viewport
            setVisibleXRangeMaximum(7f)
            setVisibleXRangeMinimum(7f)
        }
    }

    /**
     * Setup X-axis (days of week)
     */
    private fun setupXAxis(chart: BarChart, weeklyData: List<DailyStepData>) {
        val xAxis = chart.xAxis
        val dayLabels = weeklyData.map { it.shortDayName }

        xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            setDrawGridLines(false)
            setDrawAxisLine(false)
            textColor = Color.parseColor(TEXT_COLOR)
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            granularity = 1f
            valueFormatter = IndexAxisValueFormatter(dayLabels)
            setLabelCount(dayLabels.size, false)
            setCenterAxisLabels(false)
        }
    }

    /**
     * Setup Y-axis (step counts)
     */
    private fun setupYAxis(chart: BarChart, weeklyData: List<DailyStepData>) {
        val leftAxis = chart.axisLeft
        val maxSteps = weeklyData.maxOfOrNull { it.steps } ?: 10000
        val maxGoal = weeklyData.maxOfOrNull { it.goal } ?: 10000
        val chartMax = maxOf(maxSteps, maxGoal) * 1.1f

        leftAxis.apply {
            axisMinimum = 0f
            axisMaximum = chartMax
            setDrawGridLines(true)
            setDrawAxisLine(false)
            gridColor = Color.parseColor(GRID_COLOR)
            gridLineWidth = 0.5f
            textColor = Color.parseColor(TEXT_COLOR)
            textSize = 10f
            valueFormatter = object : ValueFormatter() {
                private val numberFormat = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
                    maximumFractionDigits = 0
                }

                override fun getFormattedValue(value: Float): String {
                    return when {
                        value >= 1000 -> "${(value / 1000).toInt()}k"
                        else -> numberFormat.format(value.toInt())
                    }
                }
            }
        }
    }

    /**
     * Create bar data from weekly step data
     */
    private fun createBarData(weeklyData: List<DailyStepData>): BarData {
        val entries = weeklyData.mapIndexed { index, dayData ->
            BarEntry(index.toFloat(), dayData.steps.toFloat())
        }

        val colors = weeklyData.mapIndexed { index, dayData ->
            when {
                index == getCurrentDayIndex(weeklyData) -> Color.parseColor(TODAY_COLOR)
                dayData.isGoalAchieved -> Color.parseColor(GOAL_ACHIEVED_COLOR)
                else -> Color.parseColor(GOAL_NOT_ACHIEVED_COLOR)
            }
        }

        val dataSet = BarDataSet(entries, "Steps").apply {
            this.colors = colors
            setDrawValues(true)
            valueTextColor = Color.parseColor(TEXT_COLOR)
            valueTextSize = 9f
            valueTypeface = Typeface.DEFAULT_BOLD
            valueFormatter = object : ValueFormatter() {
                override fun getBarLabel(barEntry: BarEntry?): String {
                    return if (barEntry != null && barEntry.y > 0) {
                        when {
                            barEntry.y >= 1000 -> "${(barEntry.y / 1000).toInt()}k"
                            else -> barEntry.y.toInt().toString()
                        }
                    } else ""
                }
            }
        }

        return BarData(dataSet).apply {
            barWidth = 0.7f
        }
    }

    /**
     * Get the index of the current day in the weekly data
     */
    private fun getCurrentDayIndex(weeklyData: List<DailyStepData>): Int {
        val today = java.time.LocalDate.now()
        return weeklyData.indexOfFirst { it.date == today }
    }

    /**
     * Generate sample data for testing
     */
    fun generateSampleWeeklyData(): List<DailyStepData> {
        val today = java.time.LocalDate.now()
        val startOfWeek = today.minusDays(today.dayOfWeek.value.toLong() - 1)

        return (0..6).map { dayOffset ->
            val date = startOfWeek.plusDays(dayOffset.toLong())
            val isToday = date == today
            val isFuture = date.isAfter(today)

            // Generate realistic step data
            val steps = when {
                isFuture -> 0  // No steps for future days
                isToday -> (5000..8500).random()  // Current day in progress
                else -> when (date.dayOfWeek.value) {
                    6, 7 -> (8000..12000).random()  // Weekend - more active
                    else -> (6000..11000).random()   // Weekdays
                }
            }

            DailyStepData(
                date = date,
                steps = steps,
                goal = 10000
            )
        }
    }

    /**
     * Add a dotted goal line to show the daily step goal
     */
    private fun addGoalLine(chart: BarChart, weeklyData: List<DailyStepData>) {
        try {
            // Get the most common goal from the weekly data
            val goalValue = weeklyData.firstOrNull()?.goal?.toFloat() ?: 10000f

            // Create a limit line for the goal
            val goalLine = LimitLine(goalValue, "Goal").apply {
                lineColor = Color.parseColor(GOAL_LINE_COLOR)
                lineWidth = 2f
                textColor = Color.parseColor(TEXT_COLOR)
                textSize = 9f
                labelPosition = LimitLine.LimitLabelPosition.RIGHT_TOP
                // Create dotted line effect
                enableDashedLine(10f, 5f, 0f) // length, space, phase
            }

            // Add the goal line to the left Y-axis
            chart.axisLeft.addLimitLine(goalLine)
            chart.axisLeft.setDrawLimitLinesBehindData(true)

        } catch (e: Exception) {
            // Goal line is optional - don't crash if it fails
            android.util.Log.w("StepsChartManager", "Could not add goal line: ${e.message}")
        }
    }
}