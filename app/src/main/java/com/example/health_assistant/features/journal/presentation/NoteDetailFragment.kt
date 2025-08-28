package com.example.health_assistant.features.journal.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.health_assistant.databinding.FragmentNoteDetailBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NoteDetailFragment : Fragment() {
    private var _binding: FragmentNoteDetailBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNoteDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupUI()
    }

    private fun setupToolbar() {
        with(binding) {
            // Setup toolbar navigation
            toolbar.setNavigationOnClickListener {
                findNavController().navigateUp()
            }

            // Setup toolbar title
            toolbar.title = "Note"

            // Setup more options button
            moreOptionsButton.setOnClickListener {
                // TODO: Implement options menu (save, delete, share, etc.)
                showOptionsMenu()
            }
        }
    }

    private fun setupUI() {
        with(binding) {
            // Focus on title input when fragment loads
            etNoteTitle.requestFocus()

            // Setup input validation
            setupInputValidation()
        }
    }

    private fun setupInputValidation() {
        with(binding) {
            // Track if user has interacted with fields
            var titleTouched = false
            var contentTouched = false
            
            etNoteTitle.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    titleTouched = true
                } else if (titleTouched) {
                    validateTitle()
                }
            }

            etNoteContent.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    contentTouched = true
                } else if (contentTouched) {
                    validateContent()
                }
            }
        }
    }

    private fun validateTitle(): Boolean {
        val title = binding.etNoteTitle.text?.toString()?.trim()
        return if (title.isNullOrEmpty()) {
            binding.tilNoteTitle.error = "Please enter a title"
            false
        } else {
            binding.tilNoteTitle.error = null
            true
        }
    }

    private fun validateContent(): Boolean {
        val content = binding.etNoteContent.text?.toString()?.trim()
        return if (content.isNullOrEmpty()) {
            binding.tilNoteContent.error = "Please enter some content"
            false
        } else {
            binding.tilNoteContent.error = null
            true
        }
    }

    private fun showOptionsMenu() {
        // TODO: Implement PopupMenu with options like Save, Delete, Share
        // For now, just save the note
        saveNote()
    }

    private fun saveNote() {
        if (validateTitle() && validateContent()) {
            val title = binding.etNoteTitle.text?.toString()?.trim()
            val content = binding.etNoteContent.text?.toString()?.trim()

            // TODO: Implement actual save logic here
            // For now, just navigate back
            findNavController().navigateUp()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}