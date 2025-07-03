package com.example.health_assistant.features.journal.presentation

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.health_assistant.R
import com.example.health_assistant.features.journal.domain.JournalEntry
import java.text.SimpleDateFormat
import java.util.*

/**
 * Adapter for displaying journal entries in a calendar view format
 */
class CalendarViewAdapter(
    private val onItemClick: (JournalEntry) -> Unit
) : ListAdapter<CalendarViewAdapter.CalendarDateItem, CalendarViewAdapter.CalendarViewHolder>(CalendarDiffCallback()) {

    // Keep track of entries grouped by date
    private val entriesByDate = mutableMapOf<String, MutableList<JournalEntry>>()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val displayDateFormat = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
    private val dayFormat = SimpleDateFormat("d", Locale.getDefault())
    private val monthFormat = SimpleDateFormat("MMM", Locale.getDefault())

    // List of date items to display
    private val calendarItems = mutableListOf<CalendarDateItem>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CalendarViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_calendar_day, parent, false)
        return CalendarViewHolder(view)
    }

    override fun onBindViewHolder(holder: CalendarViewHolder, position: Int) {
        val dateItem = getItem(position)
        holder.bind(dateItem)
    }

    fun submitCalendarData(entries: List<JournalEntry>) {
        // Group entries by date
        entriesByDate.clear()

        entries.forEach { entry ->
            val dateStr = dateFormat.format(Date(entry.timestamp))
            if (!entriesByDate.containsKey(dateStr)) {
                entriesByDate[dateStr] = mutableListOf()
            }
            entriesByDate[dateStr]?.add(entry)
        }

        // Create calendar items from grouped entries
        calendarItems.clear()
        entriesByDate.forEach { (dateStr, entriesList) ->
            val timestamp = dateFormat.parse(dateStr)?.time ?: System.currentTimeMillis()
            val hasHealthMetrics = entriesList.any {
                it is JournalEntry.Weight || it is JournalEntry.HeartRate ||
                it is JournalEntry.BloodPressure || it is JournalEntry.Measurement
            }
            val hasActivity = entriesList.any {
                it is JournalEntry.Workout || it is JournalEntry.ActivitySummary
            }
            val hasMood = entriesList.any { it is JournalEntry.Mood }

            calendarItems.add(CalendarDateItem(
                timestamp = timestamp,
                entryCount = entriesList.size,
                entries = entriesList,
                hasHealthMetrics = hasHealthMetrics,
                hasActivity = hasActivity,
                hasMood = hasMood
            ))
        }

        // Sort by date, most recent first
        calendarItems.sortByDescending { it.timestamp }

        // Submit the list
        submitList(calendarItems)
    }

    inner class CalendarViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Date elements
        private val calendarDay: TextView = itemView.findViewById(R.id.calendarDay)
        private val calendarMonth: TextView = itemView.findViewById(R.id.calendarMonth)
        private val calendarDateText: TextView = itemView.findViewById(R.id.calendarDateText)
        private val calendarEntryCount: TextView = itemView.findViewById(R.id.calendarEntryCount)

        // Type indicators
        private val healthMetricIndicator: View = itemView.findViewById(R.id.healthMetricIndicator)
        private val activityIndicator: View = itemView.findViewById(R.id.activityIndicator)
        private val moodIndicator: View = itemView.findViewById(R.id.moodIndicator)

        fun bind(dateItem: CalendarDateItem) {
            val date = Date(dateItem.timestamp)

            // Set day and month
            calendarDay.text = dayFormat.format(date)
            calendarMonth.text = monthFormat.format(date).uppercase()

            // Set date text and entry count
            calendarDateText.text = displayDateFormat.format(date)
            calendarEntryCount.text = "${dateItem.entryCount} ${if (dateItem.entryCount == 1) "entry" else "entries"}"

            // Show/hide type indicators
            healthMetricIndicator.visibility = if (dateItem.hasHealthMetrics) View.VISIBLE else View.INVISIBLE
            activityIndicator.visibility = if (dateItem.hasActivity) View.VISIBLE else View.INVISIBLE
            moodIndicator.visibility = if (dateItem.hasMood) View.VISIBLE else View.INVISIBLE

            // Set click listener
            itemView.setOnClickListener {
                if (dateItem.entries.isNotEmpty()) {
                    onItemClick(dateItem.entries.first())
                }
            }
        }
    }

    // Data class to hold information about a calendar day
    data class CalendarDateItem(
        val timestamp: Long,
        val entryCount: Int,
        val entries: List<JournalEntry>,
        val hasHealthMetrics: Boolean,
        val hasActivity: Boolean,
        val hasMood: Boolean
    )

    class CalendarDiffCallback : DiffUtil.ItemCallback<CalendarDateItem>() {
        override fun areItemsTheSame(oldItem: CalendarDateItem, newItem: CalendarDateItem): Boolean {
            return oldItem.timestamp == newItem.timestamp
        }

        override fun areContentsTheSame(oldItem: CalendarDateItem, newItem: CalendarDateItem): Boolean {
            return oldItem == newItem
        }
    }
}
