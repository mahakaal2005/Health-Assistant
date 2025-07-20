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
import java.time.format.DateTimeFormatter

object ChartManager {

    /**
     * Enhanced implementation of setupChart to handle missing historical data gracefully
     * This method ensures consistent chart display regardless of available data completeness
     */
    fun setupChart(chart: BarChart, weeklyData: List<DailyStepData>, chartType: String) {
        // Handle empty data case
        if (weeklyData.isEmpty()) {
            // Instead of clearing the chart, create a default empty dataset
            val emptyData = createEmptyWeeklyData()
            
            // Configure chart with empty data
            configureChartAppearance(chart)
            setupXAxis(chart, emptyData)
            setupYAxis(chart, chartType)
            
            // Create empty bar data
            val barData = createBarData(emptyData, chartType)
            chart.data = barData
            
            // Refresh the chart
            chart.invalidate()
            return
        }
        
        // Ensure we have exactly 7 days of data
        val validatedData = if (weeklyData.size != 7) {
            // Create a complete 7-day dataset
            createComplete7DayDataset(weeklyData)
        } else {
            weeklyData
        }
        
        // Configure chart appearance
        configureChartAppearance(chart)
        setupXAxis(chart, validatedData)
        setupYAxis(chart, chartType)
        
        // Create bar data
        val barData = createBarData(validatedData, chartType)
        chart.data = barData
        
        // Animate and refresh the chart
        chart.animateY(1000)
        chart.invalidate()
    }
    
    /**
     * Create a complete 7-day dataset from partial data
     */
    private fun createComplete7DayDataset(partialData: List<DailyStepData>): List<DailyStepData> {
        // If we have no data, return an empty dataset
        if (partialData.isEmpty()) {
            return createEmptyWeeklyData()
        }
        
        // Find the start date (should be Monday)
        val firstDate = partialData.minByOrNull { it.date }?.date ?: LocalDate.now()
        val dayOfWeek = firstDate.dayOfWeek.value
        val startDate = if (dayOfWeek == 1) {
            // Already Monday
            firstDate
        } else {
            // Adjust to previous Monday
            firstDate.minusDays((dayOfWeek - 1).toLong())
        }
        
        // Create a map of existing data by date string
        val dataByDate = partialData.associateBy { 
            it.date.format(DateTimeFormatter.ISO_LOCAL_DATE) 
        }
        
        // Create data for all 7 days
        return (0..6).map { dayOffset ->
            val date = startDate.plusDays(dayOffset.toLong())
            val dateString = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
            
            // Use existing data if available, otherwise create zero-value data
            dataByDate[dateString] ?: DailyStepData(
                date = date,
                steps = 0,
                goal = 9000,
                calories = 0,
                caloriesGoal = 300,
                heartPoints = 0,
                heartPointsGoal = 50
            )
        }
    }
    
    /**
     * Create an empty weekly dataset with zero values
     */
    private fun createEmptyWeeklyData(): List<DailyStepData> {
        val today = LocalDate.now()
        val dayOfWeek = today.dayOfWeek.value
        val startDate = today.minusDays((dayOfWeek - 1).toLong()) // Start from Monday
        
        return (0..6).map { dayOffset ->
            val date = startDate.plusDays(dayOffset.toLong())
            DailyStepData(
                date = date,
                steps = 0,
                goal = 9000,
                calories = 0,
                caloriesGoal = 300,
                heartPoints = 0,
                heartPointsGoal = 50
            )
        }
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

    /**
     * Enhanced implementation of setupXAxis to ensure consistent day labels
     * This method uses fixed day labels regardless of the actual data
     */
    private fun setupXAxis(chart: BarChart, weeklyData: List<DailyStepData>) {
        val xAxis = chart.xAxis
        
        // Always use consistent day labels regardless of actual data
        val dayLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        
        // Configure X-axis appearance
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.setDrawAxisLine(false)
        xAxis.textColor = Color.BLACK
        xAxis.textSize = 11f
        
        // Set day labels
        xAxis.valueFormatter = IndexAxisValueFormatter(dayLabels)
        
        // Ensure exactly 7 labels for 7 days
        xAxis.granularity = 1f
        xAxis.labelCount = 7
        xAxis.setLabelCount(7, false)
        
        // Set axis limits to ensure proper spacing
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