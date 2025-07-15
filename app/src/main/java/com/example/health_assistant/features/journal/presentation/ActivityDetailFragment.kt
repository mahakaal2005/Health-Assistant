package com.example.health_assistant.features.journal.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.health_assistant.databinding.FragmentActivityDetailBinding
import com.example.health_assistant.features.journal.domain.ActivityCard
import com.example.health_assistant.features.journal.workers.ActivityCardScheduler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class ActivityDetailFragment : Fragment() {
    private var _binding: FragmentActivityDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ActivityCardViewModel by viewModels()

    @Inject
    lateinit var activityCardScheduler: ActivityCardScheduler

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentActivityDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        observeViewModel()
        loadActivityCard()
    }

    private fun setupUI() {
        binding.placeholderText.text = "Loading today's activity overview..."

        // Add view-only testing buttons (no manual creation)
        val viewTodayButton = Button(requireContext()).apply {
            text = "📅 View Today's Activity"
            setOnClickListener {
                viewTodaysActivity()
            }
        }

        val viewAllButton = Button(requireContext()).apply {
            text = "📋 View All Activity Cards"
            setOnClickListener {
                viewAllActivityCards()
            }
        }

        val statusButton = Button(requireContext()).apply {
            text = "⚙️ Check Auto-Generation Status"
            setOnClickListener {
                checkAutoGenerationStatus()
            }
        }

        val forceGenerateButton = Button(requireContext()).apply {
            text = "🔄 Force Generate Card (Testing)"
            setOnClickListener {
                forceGenerateCardForTesting()
            }
        }

        // Add buttons to layout
        val parentLayout = binding.root as? android.widget.LinearLayout
        parentLayout?.let {
            it.addView(viewTodayButton, 0)
            it.addView(viewAllButton, 1)
            it.addView(statusButton, 2)
            it.addView(forceGenerateButton, 3)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.selectedCard.collect { activityCard ->
                activityCard?.let { displayActivityCard(it) }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { uiState ->
                when {
                    uiState.isLoading -> showLoading()
                    uiState.error != null -> showError(uiState.error)
                    else -> hideLoading()
                }
            }
        }
    }

    private fun loadActivityCard() {
        // Try to load today's existing activity card (view-only)
        viewTodaysActivity()
    }

    private fun displayActivityCard(activityCard: ActivityCard) {
        with(binding) {
            val dateFormatter = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")

            placeholderText.text = buildString {
                append("📅 ${activityCard.date.format(dateFormatter)}\n\n")
                append("🚶 Steps: ${activityCard.stepCount}\n")
                append("🔥 Calories: ${activityCard.caloriesBurned} cal\n")
                append("💖 Heart Points: ${activityCard.heartPoints}/10\n")
            }
        }
    }

    private fun showLoading() {
        binding.placeholderText.text = "Loading activity details..."
    }

    private fun hideLoading() {
        // Loading hidden when data is displayed
    }

    private fun showError(error: String) {
        binding.placeholderText.text = "Error: $error"
    }

    private fun viewTodaysActivity() {
        binding.placeholderText.text = "📅 Loading today's activity card...\n\nSearching for automatically generated data..."

        lifecycleScope.launch {
            // Try to get today's existing activity card
            val todayCard = viewModel.getTodaysActivityCard()

            if (todayCard != null) {
                displayActivityCard(todayCard)
            } else {
                binding.placeholderText.text = "📅 No activity card for today yet.\n\n" +
                        "Activity cards are automatically generated at midnight.\n" +
                        "Check back tomorrow or wait for midnight generation."
            }
        }
    }

    private fun viewAllActivityCards() {
        binding.placeholderText.text = "📋 Loading all activity cards...\n\nFetching automatically generated cards..."

        lifecycleScope.launch {
            viewModel.uiState.collect { uiState ->
                if (uiState.activityCards.isNotEmpty()) {
                    val cardsList = buildString {
                        append("📊 Total Activity Cards: ${uiState.activityCards.size}\n")
                        append("(All automatically generated at midnight)\n\n")
                        uiState.activityCards.take(5).forEach { card ->
                            append("📅 ${card.date}\n")
                            append("🚶 ${card.stepCount} steps\n")
                            append("🔥 ${card.caloriesBurned} cal\n")
                            append("💖 ${card.heartPoints}/10 points\n\n")
                        }
                        if (uiState.activityCards.size > 5) {
                            append("... and ${uiState.activityCards.size - 5} more cards")
                        }
                    }
                    binding.placeholderText.text = cardsList
                    return@collect
                } else {
                    binding.placeholderText.text = "📋 No activity cards yet.\n\n" +
                            "Activity cards are automatically generated at midnight daily.\n" +
                            "Your first card will appear tomorrow at 00:00."
                }
            }
        }

        // Trigger loading
        viewModel.loadRecentActivityCards(30)
    }

    private fun checkAutoGenerationStatus() {
        binding.placeholderText.text = "⚙️ Auto-Generation Status:\n\n" +
                "✅ Background scheduler: Active\n" +
                "⏰ Generation time: Every day at midnight (00:00)\n" +
                "📊 Tracks: Steps, Calories, Heart Rate\n" +
                "👀 User access: Read-only viewing\n" +
                "💾 Storage: Permanent (cannot be deleted)\n\n" +
                "Next generation: Tonight at midnight"
    }

    private fun forceGenerateCardForTesting() {
        binding.placeholderText.text = "🔄 Forcing activity card generation...\n\n" +
                "This is a manual trigger for testing purposes."

        lifecycleScope.launch {
            // Force activity card generation
            viewModel.generateTodaysActivityCard()

            binding.placeholderText.text = "✅ Activity card generation triggered.\n\n" +
                    "Check today's activity overview for updates."
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}