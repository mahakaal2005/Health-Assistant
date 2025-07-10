package com.example.health_assistant.features.journal.presentation

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.example.health_assistant.R
import com.example.health_assistant.databinding.DialogAddJournalEntryBinding
import com.example.health_assistant.features.journal.JournalViewModel
import com.example.health_assistant.features.journal.domain.JournalEntry
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AddJournalEntryDialog : DialogFragment() {

    private var _binding: DialogAddJournalEntryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: JournalViewModel by viewModels()

    private var selectedMoodLevel = 3
    private var selectedMoodEmoji = "😐"

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogAddJournalEntryBinding.inflate(layoutInflater)

        setupSpinner()
        setupMoodSelector()
        setupButtons()

        return MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .create()
    }

    private fun setupSpinner() {
        val entryTypes = arrayOf("Note", "Mood", "Weight", "Heart Rate", "Blood Pressure", "Activity")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, entryTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.entryTypeSpinner.adapter = adapter

        binding.entryTypeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                showContainerForType(entryTypes[position])
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun showContainerForType(type: String) {
        // Hide all containers
        binding.noteEntryContainer.visibility = View.GONE
        binding.moodEntryContainer.visibility = View.GONE
        binding.healthMetricContainer.visibility = View.GONE
        binding.activityContainer.visibility = View.GONE

        // Show relevant container
        when (type) {
            "Note" -> binding.noteEntryContainer.visibility = View.VISIBLE
            "Mood" -> binding.moodEntryContainer.visibility = View.VISIBLE
            "Weight", "Heart Rate", "Blood Pressure" -> {
                binding.healthMetricContainer.visibility = View.VISIBLE
                binding.metricTypeLabel.text = type
            }
            "Activity" -> binding.activityContainer.visibility = View.VISIBLE
        }
    }

    private fun setupMoodSelector() {
        val moodOptions = binding.moodOptions
        val moods = listOf(
            Pair("😞", 1), Pair("🙁", 2), Pair("😐", 3), Pair("🙂", 4), Pair("😃", 5)
        )

        for (i in 0 until moodOptions.childCount) {
            val moodView = moodOptions.getChildAt(i)
            val (emoji, level) = moods[i]

            moodView.setOnClickListener {
                selectedMoodLevel = level
                selectedMoodEmoji = emoji

                // Update UI to show selection
                for (j in 0 until moodOptions.childCount) {
                    moodOptions.getChildAt(j).alpha = if (j == i) 1.0f else 0.5f
                }
            }
        }
    }

    private fun setupButtons() {
         binding.cancelButton.setOnClickListener {
            dismiss()
        }

        binding.saveButton.setOnClickListener {
            saveJournalEntry()
        }
    }

    private fun saveJournalEntry() {
        val selectedType = binding.entryTypeSpinner.selectedItem.toString()

        try {
            val entry = when (selectedType) {
                "Note" -> createNoteEntry()
                "Mood" -> createMoodEntry()
                "Weight" -> createWeightEntry()
                "Heart Rate" -> createHeartRateEntry()
                "Blood Pressure" -> createBloodPressureEntry()
                "Activity" -> createActivityEntry()
                else -> return
            }

            viewModel.addEntry(entry)
            dismiss()
        } catch (e: Exception) {
            // Show error message
            android.util.Log.e("AddJournalDialog", "Error saving entry: ${e.message}")
        }
    }

    private fun createNoteEntry(): JournalEntry {
        val content = binding.noteContentInput.text.toString()

        return JournalEntry.Generic(
            id = 0,
            timestamp = System.currentTimeMillis(),
            type = "note",
            content = content
        )
    }

    private fun createMoodEntry(): JournalEntry {
        val description = binding.moodDescriptionInput.text.toString().ifBlank { "Feeling ${selectedMoodEmoji}" }

        return JournalEntry.Mood(
            id = 0,
            timestamp = System.currentTimeMillis(),
            emoji = selectedMoodEmoji,
            description = description,
            moodLevel = selectedMoodLevel,
            note = ""
        )
    }

    private fun createWeightEntry(): JournalEntry {
        val weight = binding.metricValueInput.text.toString().toDoubleOrNull() ?: 0.0
        val notes = binding.metricNotesInput.text.toString()

        return JournalEntry.Weight(
            id = 0,
            timestamp = System.currentTimeMillis(),
            weight = weight,
            unit = "kg",
            note = notes
        )
    }

    private fun createHeartRateEntry(): JournalEntry {
        val bpm = binding.metricValueInput.text.toString().toIntOrNull() ?: 0
        val notes = binding.metricNotesInput.text.toString()

        return JournalEntry.HeartRate(
            id = 0,
            timestamp = System.currentTimeMillis(),
            bpm = bpm,
            state = "resting",
            note = notes
        )
    }

    private fun createBloodPressureEntry(): JournalEntry {
        val systolic = binding.metricValueInput.text.toString().toIntOrNull() ?: 120
        val notes = binding.metricNotesInput.text.toString()

        return JournalEntry.BloodPressure(
            id = 0,
            timestamp = System.currentTimeMillis(),
            systolic = systolic,
            diastolic = 80, // For simplicity, could add another input field
            note = notes
        )
    }

    private fun createActivityEntry(): JournalEntry {
        val activityType = binding.activityTypeInput.text.toString()
        val duration = binding.durationInput.text.toString().toIntOrNull() ?: 0

        return JournalEntry.Workout(
            id = 0,
            timestamp = System.currentTimeMillis(),
            activityType = activityType,
            duration = duration,
            summary = "$activityType for $duration minutes"
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}