package com.example.health_assistant.utils

import android.graphics.Color
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.example.health_assistant.data.models.DailyStepData
import java.time.LocalDate

object ChartManager {

    fun setupChart(chart: BarChart, weeklyData: List<DailyStepData>, chartType: String) {
        if (weeklyData.isEmpty()) {
            chart.clear()
            return
        }

        configureChartAppearance(chart)
        setupXAxis(chart, weeklyData)
        setupYAxis(chart, chartType) // Pass chartType to set proper Y-axis scaling

        val barData = createBarData(weeklyData, chartType)
        chart.data = barData

        chart.animateY(1000)
        chart.invalidate()
    }

    private fun configureChartAppearance(chart: BarChart) {
        chart.description.isEnabled = false
        chart.legend.isEnabled = false
        chart.setTouchEnabled(true)
        chart.isDragEnabled = false
        chart.setScaleEnabled(false)
        chart.setPinchZoom(false)
        chart.setDrawGridBackground(false)
        chart.setDrawBorders(false)
        chart.setBackgroundColor(Color.TRANSPARENT)
        chart.axisRight.isEnabled = false
        chart.setExtraOffsets(8f, 16f, 8f, 8f)
    }

    private fun setupXAxis(chart: BarChart, weeklyData: List<DailyStepData>) {
        val xAxis = chart.xAxis
        // FIXED: Use consistent day labels regardless of actual data
        val dayLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.setDrawAxisLine(false)
        xAxis.textColor = Color.BLACK
        xAxis.textSize = 11f
        xAxis.valueFormatter = IndexAxisValueFormatter(dayLabels)
        xAxis.granularity = 1f
        xAxis.labelCount = 7  // Always 7 days
        xAxis.setLabelCount(7, false) // Ensure exactly 7 labels for 7 days
        xAxis.axisMinimum = -0.5f
        xAxis.axisMaximum = 6.5f
    }

    private fun setupYAxis(chart: BarChart, chartType: String) {
        val yAxis = chart.axisLeft
        yAxis.setDrawGridLines(true)
        yAxis.setDrawAxisLine(false)
        yAxis.textColor = Color.BLACK
        yAxis.textSize = 10f
        yAxis.axisMinimum = 0f
        yAxis.gridColor = Color.parseColor("#30000000")
        yAxis.gridLineWidth = 0.5f

        // FIXED: Y-axis scaling to match exact goal values from health overview card
        yAxis.axisMaximum = when (chartType) {
            "steps" -> 10000f      // Slightly above 9K goal for better visualization
            "calories" -> 350f     // Slightly above 300 goal for better visualization
            "heartPoints" -> 60f   // Slightly above 50 goal for better visualization
            else -> 100f
        }

        // Set appropriate label count for clean grid lines
        yAxis.labelCount = when (chartType) {
            "steps" -> 6        // 0, 2000, 4000, 6000, 8000, 10000
            "calories" -> 8     // 0, 50, 100, 150, 200, 250, 300, 350
            "heartPoints" -> 7  // 0, 10, 20, 30, 40, 50, 60
            else -> 5
        }
        yAxis.setLabelCount(yAxis.labelCount, false)
    }

    private fun createBarData(weeklyData: List<DailyStepData>, chartType: String): BarData {
        val entries = weeklyData.mapIndexed { index, data ->
            val value = when (chartType) {
                "steps" -> data.steps.toFloat()
                "calories" -> data.calories.toFloat()
                "heartPoints" -> data.heartPoints.toFloat()
                else -> 0f
            }
            BarEntry(index.toFloat(), value)
        }

        val dataSet = BarDataSet(entries, chartType)
        dataSet.color = when (chartType) {
            "steps" -> Color.parseColor("#FFA726") // Orange
            "calories" -> Color.parseColor("#66BB6A") // Green
            "heartPoints" -> Color.parseColor("#42A5F5") // Blue
            else -> Color.GRAY
        }
        dataSet.setDrawValues(false)
        dataSet.isHighlightEnabled = true
        dataSet.barBorderWidth = 0f

        val barData = BarData(dataSet)
        barData.barWidth = 0.6f
        return barData
    }

    fun generateSampleWeeklyData(): List<DailyStepData> {
        val today = LocalDate.now()
        val startOfWeek = today.minusDays(today.dayOfWeek.value.toLong() - 1)

        return (0..6).map { dayOffset ->
            val date = startOfWeek.plusDays(dayOffset.toLong())
            val isToday = date.isEqual(today)
            val isFuture = date.isAfter(today)

            DailyStepData(
                date = date,
                steps = if (isFuture) 0 else if (isToday) (3000..8000).random() else (4000..12000).random(),
                goal = 10000,
                calories = if (isFuture) 0 else if (isToday) (150..400).random() else (200..500).random(),
                caloriesGoal = 300,
                heartPoints = if (isFuture) 0 else if (isToday) (10..40).random() else (15..60).random(),
                heartPointsGoal = 50
            )
        }
    }
}