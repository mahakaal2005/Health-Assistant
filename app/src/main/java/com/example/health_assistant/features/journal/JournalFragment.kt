package com.example.health_assistant.features.journal

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.health_assistant.databinding.FragmentJournalBinding
import com.example.health_assistant.features.journal.presentation.JournalAdapter
import com.example.health_assistant.features.journal.presentation.JournalItemDecoration
import com.example.health_assistant.features.journal.db.toEntity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class JournalFragment : Fragment() {
    private var _binding: FragmentJournalBinding? = null
    private val binding get() = _binding!!

    private val viewModel: JournalViewModel by viewModels()
    private lateinit var journalAdapter: JournalAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentJournalBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        // Initialize adapter with click listener
        journalAdapter = JournalAdapter(
            onEdit = { journalEntry ->
                // Handle edit journal entry
                // You can add navigation to edit screen here
            },
            onDelete = { journalEntry ->
                // Handle delete journal entry
                // You can add delete confirmation dialog here
            }
        )

        // Setup RecyclerView
        binding.recyclerJournalEntries.apply {
            adapter = journalAdapter
            layoutManager = LinearLayoutManager(requireContext())

            // Add item decoration for headers and dividers
            addItemDecoration(
                JournalItemDecoration(requireContext()) { position ->
                    // Provide the entry at position for the decoration
                    journalAdapter.currentList.getOrNull(position)
                }
            )
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.entries.collect { entries ->
                // Show/hide loading
                binding.progressLoading.visibility = View.GONE

                if (entries.isEmpty()) {
                    // Show empty state
                    binding.recyclerJournalEntries.visibility = View.GONE
                    binding.emptyStateView.root.visibility = View.VISIBLE
                } else {
                    // Show entries
                    binding.recyclerJournalEntries.visibility = View.VISIBLE
                    binding.emptyStateView.root.visibility = View.GONE
                    // Convert domain models to entities for the adapter
                    val entryEntities = entries.map { it.toEntity() }
                    journalAdapter.submitList(entryEntities)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}