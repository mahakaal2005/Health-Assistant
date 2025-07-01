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
 * Fragment for the onboarding experience using ViewPager2 with swipe-only navigation
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
            // Update ViewModel with current page for state management
            viewModel.setCurrentPage(position)
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
        pagerAdapter = OnboardingPagerAdapter {
            // Handle Get Started button click - navigate to AccountDecisionFragment
            navigateToAccountDecision()
        }

        binding.onboardingViewPager.apply {
            adapter = pagerAdapter
            // Register page change callback to update the ViewModel
            registerOnPageChangeCallback(pageChangeCallback)
        }

        // Connect the dots indicator with ViewPager2
        binding.dotsIndicator.attachTo(binding.onboardingViewPager)
    }

    private fun setupObservers() {
        // Observe current page changes for state management
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.currentPage.collectLatest { position ->
                // Sync ViewPager position if needed (for programmatic navigation)
                if (binding.onboardingViewPager.currentItem != position) {
                    binding.onboardingViewPager.setCurrentItem(position, true)
                }

                // Hide skip button on last page since we show Get Started button
                updateSkipButtonVisibility(position)
            }
        }
    }

    private fun setupClickListeners() {
        // Skip button click handler - navigate directly to AccountDecisionFragment
        binding.skipButton.setOnClickListener {
            navigateToAccountDecision()
        }
    }

    /**
     * Navigate to AccountDecisionFragment and complete onboarding
     */
    private fun navigateToAccountDecision() {
        // Mark onboarding as completed
        viewModel.completeOnboarding()

        // Navigate directly to AccountDecisionFragment
        findNavController().navigate(R.id.action_onboardingFragment_to_authFragment)
    }

    /**
     * Update skip button visibility based on current page
     */
    private fun updateSkipButtonVisibility(position: Int) {
        val isLastPage = position == pagerAdapter.itemCount - 1
        // Hide skip button on last page to avoid confusion with Get Started button
        binding.skipButton.visibility = if (isLastPage) View.GONE else View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Properly unregister the callback to prevent memory leaks
        binding.onboardingViewPager.unregisterOnPageChangeCallback(pageChangeCallback)
        _binding = null
    }
}