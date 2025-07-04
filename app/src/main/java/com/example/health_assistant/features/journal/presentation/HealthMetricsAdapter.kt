package com.example.health_assistant.features.journal.presentation

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.health_assistant.R
import com.example.health_assistant.features.journal.domain.JournalEntry
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import java.text.SimpleDateFormat
import java.util.*

/**
 * Adapter for displaying health metrics with charts
 */
class HealthMetricsAdapter(
    private val onItemClick: (JournalEntry) -> Unit
) : ListAdapter<JournalEntry, RecyclerView.ViewHolder>(HealthMetricsDiffCallback()) {

    companion object {
        private const val TYPE_WEIGHT = 0
        private const val TYPE_HEART_RATE = 1
        private const val TYPE_BLOOD_PRESSURE = 2
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is JournalEntry.Weight -> TYPE_WEIGHT
            is JournalEntry.HeartRate -> TYPE_HEART_RATE
            is JournalEntry.BloodPressure -> TYPE_BLOOD_PRESSURE
            else -> throw IllegalArgumentException("Unknown item type at position $position")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_WEIGHT -> {
                val view = layoutInflater.inflate(R.layout.item_health_metric_entry, parent, false)
                WeightViewHolder(view)
            }
            TYPE_HEART_RATE -> {
                val view = layoutInflater.inflate(R.layout.item_health_metric_entry, parent, false)
                HeartRateViewHolder(view)
            }
            TYPE_BLOOD_PRESSURE -> {
                val view = layoutInflater.inflate(R.layout.item_health_metric_entry, parent, false)
                BloodPressureViewHolder(view)
            }
            else -> throw IllegalArgumentException("Unknown view type $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val entry = getItem(position)
        when (holder) {
            is WeightViewHolder -> holder.bind(entry as JournalEntry.Weight)
            is HeartRateViewHolder -> holder.bind(entry as JournalEntry.HeartRate)
            is BloodPressureViewHolder -> holder.bind(entry as JournalEntry.BloodPressure)
        }
    }

    inner class WeightViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val icon: ImageView = itemView.findViewById(R.id.metricIcon)
        private val title: TextView = itemView.findViewById(R.id.metricTitle)
        private val date: TextView = itemView.findViewById(R.id.metricDate)
        private val currentValue: TextView = itemView.findViewById(R.id.metricCurrentValue)
        private val unit: TextView = itemView.findViewById(R.id.metricUnit)
        private val change: TextView = itemView.findViewById(R.id.metricChange)
        private val chart: LineChart = itemView.findViewById(R.id.metricChart)
        private val note: TextView = itemView.findViewById(R.id.metricNote)

        fun bind(entry: JournalEntry.Weight) {
            icon.setImageResource(R.drawable.ic_weight)
            title.text = "Weight"
            date.text = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(entry.timestamp))
            currentValue.text = String.format("%.1f", entry.weight)
            unit.text = entry.unit

            // For now, hide change display since we don't have previous value tracking
            // This could be enhanced by calculating from previous entries
            change.visibility = View.GONE

            // Show note if available
            if (!entry.note.isNullOrEmpty()) {
                note.text = entry.note
                note.visibility = View.VISIBLE
            } else {
                note.visibility = View.GONE
            }

            setupChart(chart, generateSampleWeightData())

            itemView.setOnClickListener { onItemClick(entry) }
        }

        private fun generateSampleWeightData(): List<Entry> {
            // This would normally come from your database as a history of weight entries
            return listOf(
                Entry(0f, 75.5f),
                Entry(1f, 75.2f),
                Entry(2f, 74.8f),
                Entry(3f, 74.9f),
                Entry(4f, 74.5f)
            )
        }
    }

    inner class HeartRateViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val icon: ImageView = itemView.findViewById(R.id.metricIcon)
        private val title: TextView = itemView.findViewById(R.id.metricTitle)
        private val date: TextView = itemView.findViewById(R.id.metricDate)
        private val currentValue: TextView = itemView.findViewById(R.id.metricCurrentValue)
        private val unit: TextView = itemView.findViewById(R.id.metricUnit)
        private val chart: LineChart = itemView.findViewById(R.id.metricChart)
        private val note: TextView = itemView.findViewById(R.id.metricNote)

        fun bind(entry: JournalEntry.HeartRate) {
            icon.setImageResource(R.drawable.ic_heart_rate)
            title.text = "Heart Rate"
            if (entry.state != null) {
                title.text = "Heart Rate (${entry.state})"
            }

            date.text = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(entry.timestamp))
            currentValue.text = entry.bpm.toString()
            unit.text = "BPM"

            // Show note if available
            if (!entry.note.isNullOrEmpty()) {
                note.text = entry.note
                note.visibility = View.VISIBLE
            } else {
                note.visibility = View.GONE
            }

            setupChart(chart, generateSampleHeartRateData())

            itemView.setOnClickListener { onItemClick(entry) }
        }

        private fun generateSampleHeartRateData(): List<Entry> {
            // Sample data
            return listOf(
                Entry(0f, 72f),
                Entry(1f, 68f),
                Entry(2f, 75f),
                Entry(3f, 82f),
                Entry(4f, 76f)
            )
        }
    }

    inner class BloodPressureViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val icon: ImageView = itemView.findViewById(R.id.metricIcon)
        private val title: TextView = itemView.findViewById(R.id.metricTitle)
        private val date: TextView = itemView.findViewById(R.id.metricDate)
        private val currentValue: TextView = itemView.findViewById(R.id.metricCurrentValue)
        private val unit: TextView = itemView.findViewById(R.id.metricUnit)
        private val chart: LineChart = itemView.findViewById(R.id.metricChart)
        private val note: TextView = itemView.findViewById(R.id.metricNote)

        fun bind(entry: JournalEntry.BloodPressure) {
            icon.setImageResource(R.drawable.ic_blood_pressure)
            title.text = "Blood Pressure"
            date.text = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(entry.timestamp))
            currentValue.text = "${entry.systolic}/${entry.diastolic}"
            unit.text = "mmHg"

            // Show note if available
            if (!entry.note.isNullOrEmpty()) {
                note.text = entry.note
                note.visibility = View.VISIBLE
            } else {
                note.visibility = View.GONE
            }

            // Set up two line chart for systolic and diastolic
            setupBloodPressureChart(chart, generateSampleBloodPressureData())

            itemView.setOnClickListener { onItemClick(entry) }
        }

        private fun generateSampleBloodPressureData(): Pair<List<Entry>, List<Entry>> {
            // Sample data for systolic and diastolic
            val systolic = listOf(
                Entry(0f, 120f),
                Entry(1f, 118f),
                Entry(2f, 122f),
                Entry(3f, 119f),
                Entry(4f, 121f)
            )

            val diastolic = listOf(
                Entry(0f, 80f),
                Entry(1f, 79f),
                Entry(2f, 82f),
                Entry(3f, 78f),
                Entry(4f, 80f)
            )

            return Pair(systolic, diastolic)
        }

        private fun setupBloodPressureChart(chart: LineChart, data: Pair<List<Entry>, List<Entry>>) {
            val systolicSet = LineDataSet(data.first, "Systolic")
            systolicSet.color = itemView.context.getColor(R.color.colorError)
            systolicSet.setCircleColor(itemView.context.getColor(R.color.colorError))

            val diastolicSet = LineDataSet(data.second, "Diastolic")
            diastolicSet.color = itemView.context.getColor(R.color.progress_blue)
            diastolicSet.setCircleColor(itemView.context.getColor(R.color.progress_blue))

            val lineData = LineData(systolicSet, diastolicSet)
            chart.data = lineData
            chart.invalidate()
        }
    }

    private fun setupChart(chart: LineChart, entries: List<Entry>) {
        val dataSet = LineDataSet(entries, "Values")
        dataSet.color = chart.context.getColor(R.color.colorPrimary)
        dataSet.setCircleColor(chart.context.getColor(R.color.colorPrimary))

        val lineData = LineData(dataSet)
        chart.data = lineData
        chart.description.isEnabled = false
        chart.legend.isEnabled = false
        chart.axisRight.isEnabled = false
        chart.xAxis.setDrawGridLines(false)
        chart.animateX(500)
        chart.invalidate()
    }

    class HealthMetricsDiffCallback : DiffUtil.ItemCallback<JournalEntry>() {
        override fun areItemsTheSame(oldItem: JournalEntry, newItem: JournalEntry): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: JournalEntry, newItem: JournalEntry): Boolean {
            return oldItem == newItem
        }
    }
}