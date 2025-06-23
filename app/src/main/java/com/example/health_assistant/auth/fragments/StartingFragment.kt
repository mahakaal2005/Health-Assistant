package com.example.health_assistant.auth.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.health_assistant.R
import com.example.health_assistant.databinding.AuthFragmentStartingBinding
import com.example.health_assistant.features.onboarding.OnboardingPreferencesRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class StartingFragment : Fragment() {

    private var _binding: AuthFragmentStartingBinding? = null
    // This property is only valid between onCreateView and onDestroyView
    private val binding get() = _binding!!

    @Inject
    lateinit var onboardingPreferencesRepository: OnboardingPreferencesRepository

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = AuthFragmentStartingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set up the "Get Started" button click listener first (so it works regardless of other operations)
        binding.btnGetStarted.setOnClickListener {
            // Navigate to onboarding instead of directly to account decision
            findNavController().navigate(R.id.action_startingFragment_to_onboardingFragment)
        }

        // Try to check onboarding status, but safely handle any initialization issues
        try {
            checkOnboardingStatus()
        } catch (e: Exception) {
            // If there's any issue with repository initialization, just stay on the starting screen
            // This allows the user to press "Get Started" and continue the flow
        }
    }

    /**
     * Checks if onboarding has been completed and navigates accordingly
     * Only call this after Hilt has a chance to inject dependencies
     */
    private fun checkOnboardingStatus() {
        if (::onboardingPreferencesRepository.isInitialized) {
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val isOnboardingCompleted = onboardingPreferencesRepository.isOnboardingCompleted.firstOrNull() ?: false

                    if (isOnboardingCompleted) {
                        // If onboarding is completed, navigate directly to account decision
                        findNavController().navigate(R.id.action_startingFragment_to_accountDecisionFragment)
                    }
                    // Otherwise, stay on this screen for the user to press "Get Started"
                } catch (e: Exception) {
                    // Safely handle any exceptions during flow collection
                }
            }
        }
        // If repository isn't initialized, do nothing - user can still press "Get Started"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Avoid memory leaks
    }
}