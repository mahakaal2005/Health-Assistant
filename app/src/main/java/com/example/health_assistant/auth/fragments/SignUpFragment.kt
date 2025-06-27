package com.example.health_assistant.auth.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.health_assistant.R
import com.example.health_assistant.auth.viewmodel.AuthState
import com.example.health_assistant.auth.viewmodel.AuthViewModel
import com.example.health_assistant.databinding.AuthFragmentSignupBinding
import com.example.health_assistant.utils.KeyboardUtils
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SignUpFragment : Fragment() {

    private var _binding: AuthFragmentSignupBinding? = null
    // This property is only valid between onCreateView and onDestroyView
    private val binding get() = _binding!!

    // Inject ViewModel using Hilt
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = AuthFragmentSignupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set up keyboard dismissal when clicking outside edit text fields
        activity?.let { KeyboardUtils.setupUI(it, view) }

        // Observe authentication state changes
        observeAuthState()

        // Initially hide the helper text by setting it to empty
        binding.passwordLayout.helperText = ""

        // Set up focus listeners to show/hide helper text
        binding.passwordInput.setOnFocusChangeListener { _, hasFocus ->
            // When focused, show helper text with password requirements
            binding.passwordLayout.helperText = if (hasFocus) {
                "At least 8 characters with letters and numbers"
            } else {
                "" // Empty when not focused
            }

            // Force the layout to redraw
            binding.passwordLayout.refreshDrawableState()
        }

        // Set up the Sign Up button click listener
        binding.signupButton.setOnClickListener {
            // Validate form and attempt account creation
            if (validateForm()) {
                // Get user input data
                val email = binding.emailInput.text.toString().trim()
                val password = binding.passwordInput.text.toString().trim()

                // Register user using ViewModel
                viewModel.registerUser(email, password)
            }
        }

        // Set up the "Already have an account" prompt click listener
        binding.loginPrompt.setOnClickListener {
            // Navigate to LoginFragment
            findNavController().navigate(R.id.action_signUpFragment_to_loginFragment)
        }
    }

    private fun observeAuthState() {
        viewModel.authState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is AuthState.Loading -> {
                    setLoadingState(true)
                }
                is AuthState.Success -> {
                    setLoadingState(false)
                    // Check if profile is complete and navigate accordingly
                    navigateAfterSuccessfulSignup()
                }
                is AuthState.Error -> {
                    setLoadingState(false)
                    // Show error message
                    Snackbar.make(
                        binding.root,
                        "Registration failed: ${state.message}",
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun setLoadingState(isLoading: Boolean) {
        binding.signupButton.isEnabled = !isLoading
        binding.progressBar?.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun validateForm(): Boolean {
        val name = binding.nameInput.text.toString().trim()
        val email = binding.emailInput.text.toString().trim()
        val password = binding.passwordInput.text.toString().trim()

        // Basic validation
        if (name.isEmpty()) {
            binding.nameInput.error = "Name cannot be empty"
            return false
        }

        if (email.isEmpty()) {
            binding.emailInput.error = "Email cannot be empty"
            return false
        }

        if (password.isEmpty()) {
            binding.passwordInput.error = "Password cannot be empty"
            return false
        }

        if (password.length < 6) {
            binding.passwordInput.error = "Password must be at least 6 characters"
            return false
        }

        return true
    }

    private fun navigateAfterSuccessfulSignup() {
        try {
            Log.d("SignUpFragment", "Checking profile completion status after successful signup")

            // For new signups, the profile is always incomplete initially
            // Navigate to complete profile screen
            findNavController().navigate(R.id.action_signUpFragment_to_completeProfile)

        } catch (e: Exception) {
            Log.e("SignUpFragment", "Error navigating after signup", e)
            // Fallback to dashboard if navigation fails
            navigateToDashboard()
        }
    }

    private fun navigateToDashboard() {
        try {
            Log.d("SignUpFragment", "Starting navigation to dashboard")

            // Use our custom method in MainActivity to launch with home fragment
            // This ensures consistent behavior between login and signup flows
            com.example.health_assistant.main.MainActivity.startWithHomeFragment(requireContext())

            // Finish the current activity to prevent going back to auth screens
            requireActivity().finish()
        } catch (e: Exception) {
            Log.e("SignUpFragment", "Error navigating to dashboard", e)
            Snackbar.make(
                binding.root,
                "Error navigating to dashboard: ${e.message}",
                Snackbar.LENGTH_LONG
            ).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}