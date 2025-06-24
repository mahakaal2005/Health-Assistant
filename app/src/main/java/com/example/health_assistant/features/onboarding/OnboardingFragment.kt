package com.example.health_assistant.features.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.example.health_assistant.R
import com.example.health_assistant.databinding.FragmentOnboardingBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Fragment for the onboarding experience using ViewPager2
 */
@AndroidEntryPoint
class OnboardingFragment : Fragment() {
    private var _binding: FragmentOnboardingBinding? = null
    private val binding get() = _binding!!

    private val viewModel: OnboardingViewModel by viewModels()
    private lateinit var pagerAdapter: OnboardingPagerAdapter

    // Store the callback as a property to properly remove it later
    private val pageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            viewModel.setCurrentPage(position)
            updateButtonsForPosition(position)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOnboardingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViewPager()
        setupObservers()
        setupClickListeners()
    }

    private fun setupViewPager() {
        pagerAdapter = OnboardingPagerAdapter()

        binding.onboardingViewPager.apply {
            adapter = pagerAdapter

            // Register page change callback to update the ViewModel
            registerOnPageChangeCallback(pageChangeCallback)
        }

        // Connect the dots indicator with ViewPager2
        binding.dotsIndicator.attachTo(binding.onboardingViewPager)
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.currentPage.collectLatest { position ->
                binding.onboardingViewPager.currentItem = position
                updateButtonsForPosition(position)
            }
        }
    }

    private fun setupClickListeners() {
        // Next button click handler
        binding.nextButton.setOnClickListener {
            val currentPosition = binding.onboardingViewPager.currentItem
            val isLastPage = viewModel.isLastPage(currentPosition, pagerAdapter.itemCount)

            if (isLastPage) {
                completeOnboarding()
            } else {
                // Navigate to next page
                binding.onboardingViewPager.currentItem = currentPosition + 1
            }
        }

        // Back button click handler
        binding.backButton.setOnClickListener {
            val currentPosition = binding.onboardingViewPager.currentItem
            if (currentPosition > 0) {
                // Navigate to previous page
                binding.onboardingViewPager.currentItem = currentPosition - 1
            }
        }

        // Skip button click handler (now at top-right)
        binding.skipButton.setOnClickListener {
            // Add debug logging to verify the click handler is being called
            android.util.Log.d("OnboardingFragment", "Skip button clicked")
            completeOnboarding()
        }
    }

    private fun updateButtonsForPosition(position: Int) {
        val isLastPage = viewModel.isLastPage(position, pagerAdapter.itemCount)
        val isFirstPage = position == 0

        // Change the text of the next button to "Get Started" on the last page
        binding.nextButton.text = if (isLastPage) {
            getString(R.string.get_started)
        } else {
            getString(R.string.next)
        }

        // Hide "Back" button on the first page, show on others
        binding.backButton.visibility = if (isFirstPage) View.INVISIBLE else View.VISIBLE

        // Hide "Skip" button on the last page
        binding.skipButton.visibility = if (isLastPage) View.GONE else View.VISIBLE
    }

    private fun completeOnboarding() {
        // Mark onboarding as completed
        viewModel.completeOnboarding()

        // Navigate to the appropriate destination (auth or main flow)
        findNavController().navigate(R.id.action_onboardingFragment_to_authFragment)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Properly unregister the callback to prevent memory leaks
        binding.onboardingViewPager.unregisterOnPageChangeCallback(pageChangeCallback)
        _binding = null
    }
}