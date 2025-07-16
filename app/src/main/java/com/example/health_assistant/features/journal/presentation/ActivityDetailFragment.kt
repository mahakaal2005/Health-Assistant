package com.example.health_assistant.features.journal.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.health_assistant.databinding.FragmentActivityDetailBinding
import com.example.health_assistant.features.journal.domain.ActivityCard
import com.example.health_assistant.features.journal.workers.ActivityCardScheduler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
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
        // Initialize UI to loading state
        showLoading()

        // Initialize triple ring progress to 0
        binding.tripleRingProgress.setStepsProgress(0, 10000)
        binding.tripleRingProgress.setCaloriesProgress(0, 500)
        binding.tripleRingProgress.setHeartPointsProgress(0, 10)
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
        viewTodaysActivity()
    }

    private fun displayActivityCard(activityCard: ActivityCard) {
        hideLoading()

        with(binding) {
            // Update date header
            val dateFormatter = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")
            dateHeaderText.text = activityCard.date.format(dateFormatter)

            // Update center summary
            centerSummaryText.text = "Today's\nActivity"

            // Update metric values
            stepsValueText.text = activityCard.stepCount.toString()
            caloriesValueText.text = activityCard.caloriesBurned.toString()
            heartPointsValueText.text = activityCard.heartPoints.toString()

            // Debug: Log the actual values
            android.util.Log.d("ActivityDetail", "Steps: ${activityCard.stepCount}, Calories: ${activityCard.caloriesBurned}, Heart Points: ${activityCard.heartPoints}")

            // Post the progress update to ensure the view is laid out first
            tripleRingProgress.post {
                // Update the triple ring progress widget with current and target values
                tripleRingProgress.setStepsProgress(activityCard.stepCount, 10000)
                tripleRingProgress.setCaloriesProgress(activityCard.caloriesBurned, 500)
                tripleRingProgress.setHeartPointsProgress(activityCard.heartPoints, 10)

                android.util.Log.d("ActivityDetail", "Progress updated for Steps: ${activityCard.stepCount}/10000, Calories: ${activityCard.caloriesBurned}/500, Heart Points: ${activityCard.heartPoints}/10")
            }
        }
    }

    private fun showLoading() {
        with(binding) {
            dateHeaderText.text = "Loading..."
            centerSummaryText.text = "Loading\nActivity"
            stepsValueText.text = "..."
            caloriesValueText.text = "..."
            heartPointsValueText.text = "..."

            // Show placeholder text
            placeholderText.visibility = View.VISIBLE
            placeholderText.text = "Loading today's activity data..."

            // Reset progress
            tripleRingProgress.setStepsProgress(0, 10000)
            tripleRingProgress.setCaloriesProgress(0, 500)
            tripleRingProgress.setHeartPointsProgress(0, 10)
        }
    }

    private fun hideLoading() {
        binding.placeholderText.visibility = View.GONE
    }

    private fun showError(error: String) {
        with(binding) {
            placeholderText.visibility = View.VISIBLE
            placeholderText.text = "Error loading activity data: $error"

            dateHeaderText.text = "Error"
            centerSummaryText.text = "No Data\nAvailable"
            stepsValueText.text = "0"
            caloriesValueText.text = "0"
            heartPointsValueText.text = "0"

            // Reset progress
            tripleRingProgress.setStepsProgress(0, 10000)
            tripleRingProgress.setCaloriesProgress(0, 500)
            tripleRingProgress.setHeartPointsProgress(0, 10)
        }
    }

    private fun viewTodaysActivity() {
        showLoading()

        lifecycleScope.launch {
            try {
                val todayCard = viewModel.getTodaysActivityCard()

                if (todayCard != null) {
                    displayActivityCard(todayCard)
                } else {
                    showNoDataState()
                }
            } catch (e: Exception) {
                showError(e.message ?: "Unknown error occurred")
            }
        }
    }

    private fun showNoDataState() {
        with(binding) {
            placeholderText.visibility = View.VISIBLE
            placeholderText.text = "No activity data available for today.\n\nActivity cards are automatically generated at midnight.\nCheck back tomorrow!"

            dateHeaderText.text = "Today's Activity"
            centerSummaryText.text = "No Data\nYet"
            stepsValueText.text = "0"
            caloriesValueText.text = "0"
            heartPointsValueText.text = "0"

            // Reset rings to 0
            tripleRingProgress.setStepsProgress(0, 10000)
            tripleRingProgress.setCaloriesProgress(0, 500)
            tripleRingProgress.setHeartPointsProgress(0, 10)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}