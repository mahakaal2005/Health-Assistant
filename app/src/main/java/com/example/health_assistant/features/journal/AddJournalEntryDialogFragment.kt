package com.example.health_assistant.features.journal

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.example.health_assistant.databinding.DialogAddJournalEntryBinding
import com.example.health_assistant.features.journal.domain.JournalEntry
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class AddJournalEntryDialogFragment : DialogFragment() {

    private var _binding: DialogAddJournalEntryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: JournalViewModel by viewModels()

    private var selectedMoodLevel = 3 // Default mood level (neutral)
    private var selectedMoodEmoji = "😐"

    companion object {
        fun newInstance(): AddJournalEntryDialogFragment {
            return AddJournalEntryDialogFragment()
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogAddJournalEntryBinding.inflate(layoutInflater)

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Add Journal Entry")
            .setView(binding.root)
            .create()

        setupUI()
        setupButtons()
        return dialog
    }

    private fun setupUI() {
        setupEntryTypeSpinner()
        setupMoodOptions()
    }

    private fun setupButtons() {
        // Setup Cancel button
        binding.cancelButton.setOnClickListener {
            dismiss()
        }

        // Setup Save button
        binding.saveButton.setOnClickListener {
            val success = saveJournalEntry()
            if (success) {
                dismiss()
            }
        }
    }

    private fun setupEntryTypeSpinner() {
        val entryTypes = arrayOf("Note", "Diary")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, entryTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        binding.entryTypeSpinner.adapter = adapter
        binding.entryTypeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                when (position) {
                    0 -> showNoteEntry()
                    1 -> showDiaryEntry()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupMoodOptions() {
        val moodViews = listOf(
            binding.moodOptions.getChildAt(0),
            binding.moodOptions.getChildAt(1),
            binding.moodOptions.getChildAt(2),
            binding.moodOptions.getChildAt(3),
            binding.moodOptions.getChildAt(4)
        )

        val moodEmojis = listOf("😃", "🙂", "😐", "🙁", "😞")

        moodViews.forEachIndexed { index, view ->
            view?.setOnClickListener {
                selectedMoodLevel = index + 1
                selectedMoodEmoji = moodEmojis[index]

                // Update visual selection (you can add visual feedback here)
                moodViews.forEach { it?.alpha = 0.5f }
                view.alpha = 1.0f
            }
        }
    }

    private fun showNoteEntry() {
        binding.noteEntryContainer.visibility = View.VISIBLE
        binding.moodEntryContainer.visibility = View.GONE
        binding.healthMetricContainer.visibility = View.GONE
        binding.activityContainer.visibility = View.GONE
    }

    private fun showDiaryEntry() {
        binding.noteEntryContainer.visibility = View.VISIBLE
        binding.moodEntryContainer.visibility = View.GONE
        binding.healthMetricContainer.visibility = View.GONE
        binding.activityContainer.visibility = View.GONE
    }

    private fun saveJournalEntry(): Boolean {
        val timestamp = System.currentTimeMillis()
        val selectedType = binding.entryTypeSpinner.selectedItemPosition

        val entry = when (selectedType) {
            0 -> createNoteEntry(timestamp) // Note
            1 -> createDiaryEntry(timestamp) // Diary
            else -> createNoteEntry(timestamp)
        }

        entry?.let {
            viewModel.addEntry(it)
            return true
        }

        return false
    }

    private fun createNoteEntry(timestamp: Long): JournalEntry? {
        val title = binding.noteTitleInput.text.toString().trim()
        val content = binding.noteContentInput.text.toString().trim()

        if (content.isEmpty()) {
            binding.noteContentInput.error = "Please enter some content"
            return null
        }

        return JournalEntry.Generic(
            id = 0L,
            timestamp = timestamp,
            type = "note",
            content = if (title.isNotEmpty()) "$title\n\n$content" else content
        )
    }


    private fun createDiaryEntry(timestamp: Long): JournalEntry? {
        val title = binding.noteTitleInput.text.toString().trim()
        val content = binding.noteContentInput.text.toString().trim()

        if (content.isEmpty()) {
            binding.noteContentInput.error = "Please enter some content"
            return null
        }

        return JournalEntry.Generic(
            id = 0L,
            timestamp = timestamp,
            type = "diary",
            content = if (title.isNotEmpty()) "$title\n\n$content" else content
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}