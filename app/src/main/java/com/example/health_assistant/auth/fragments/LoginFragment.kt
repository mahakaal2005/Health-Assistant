package com.example.health_assistant.auth.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.health_assistant.R
import com.example.health_assistant.auth.viewmodel.AuthState
import com.example.health_assistant.auth.viewmodel.AuthViewModel
import com.example.health_assistant.databinding.AuthFragmentLoginBinding
import com.example.health_assistant.utils.KeyboardUtils
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LoginFragment : Fragment() {

    private var _binding: AuthFragmentLoginBinding? = null
    // This property is only valid between onCreateView and onDestroyView
    private val binding get() = _binding!!

    // Inject ViewModel using Hilt
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = AuthFragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set up keyboard dismissal when clicking outside edit text fields
        activity?.let { KeyboardUtils.setupUI(it, view) }

        // Observe authentication state changes
        observeAuthState()

        // Set up the Login button click listener
        binding.loginButton.setOnClickListener {
            // Validate form and attempt login
            if (validateForm()) {
                // Get email and password from input fields
                val email = binding.emailInput.text.toString().trim()
                val password = binding.passwordInput.text.toString().trim()

                // Authenticate using ViewModel
                viewModel.signInUser(email, password)
            }
        }

        // Set up the "Don't have an account" prompt click listener
        binding.createAccountPrompt.setOnClickListener {
            // Navigate to SignUpFragment
            findNavController().navigate(R.id.action_loginFragment_to_signUpFragment)
        }

        // Set up the Forgot Password click listener
        binding.forgotPassword.setOnClickListener {
            // Show dialog to enter email for password reset
            handlePasswordReset()
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
                    // Navigate to dashboard on successful login
                    navigateToDashboard()
                }
                is AuthState.Error -> {
                    setLoadingState(false)
                    // Show error message
                    Snackbar.make(
                        binding.root,
                        "Login failed: ${state.message}",
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun setLoadingState(isLoading: Boolean) {
        binding.loginButton.isEnabled = !isLoading
        binding.progressBar?.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun validateForm(): Boolean {
        val email = binding.emailInput.text.toString().trim()
        val password = binding.passwordInput.text.toString().trim()

        // Basic validation
        if (email.isEmpty()) {
            binding.emailInput.error = "Email cannot be empty"
            return false
        }

        if (password.isEmpty()) {
            binding.passwordInput.error = "Password cannot be empty"
            return false
        }

        return true
    }

    private fun handlePasswordReset() {
        val email = binding.emailInput.text.toString().trim()

        if (email.isEmpty()) {
            binding.emailInput.error = "Please enter your email first"
            return
        }

        // Send password reset email using ViewModel
        viewModel.resetPassword(email)
    }

    private fun navigateToDashboard() {
        try {
            // Use our custom method in MainActivity to launch with home fragment
            // This will clear the entire task and prevent account decision fragment from showing
            com.example.health_assistant.main.MainActivity.startWithHomeFragment(requireContext())

            // Finish the current activity to prevent going back to auth screens
            requireActivity().finish()
        } catch (e: Exception) {
            android.util.Log.e("LoginFragment", "Navigation error: ${e.message}", e)
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