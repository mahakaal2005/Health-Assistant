package com.example.health_assistant.auth.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.health_assistant.R
import com.example.health_assistant.auth.repository.FirebaseAuthRepository
import com.example.health_assistant.databinding.AuthFragmentLoginBinding
import com.google.android.material.snackbar.Snackbar

class LoginFragment : Fragment() {

    private var _binding: AuthFragmentLoginBinding? = null
    // This property is only valid between onCreateView and onDestroyView
    private val binding get() = _binding!!

    // Firebase Authentication Repository
    private lateinit var authRepository: FirebaseAuthRepository

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = AuthFragmentLoginBinding.inflate(inflater, container, false)
        authRepository = FirebaseAuthRepository()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set up the Login button click listener
        binding.loginButton.setOnClickListener {
            // Validate form and attempt login
            if (validateForm()) {
                // Show loading state
                setLoadingState(true)

                // Get email and password from input fields
                val email = binding.emailInput.text.toString().trim()
                val password = binding.passwordInput.text.toString().trim()

                // Authenticate with Firebase
                authRepository.signInUser(
                    email,
                    password,
                    onSuccess = { user ->
                        setLoadingState(false)
                        // Navigate to dashboard on successful login
                        navigateToDashboard()
                    },
                    onFailure = { exception ->
                        setLoadingState(false)
                        // Show error message
                        Snackbar.make(
                            binding.root,
                            "Login failed: ${exception.message}",
                            Snackbar.LENGTH_LONG
                        ).show()
                    }
                )
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
            showPasswordResetDialog()
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

    private fun showPasswordResetDialog() {
        val email = binding.emailInput.text.toString().trim()

        if (email.isEmpty()) {
            binding.emailInput.error = "Please enter your email first"
            return
        }

        // Show loading state
        setLoadingState(true)

        // Send password reset email
        authRepository.resetPassword(
            email,
            onSuccess = {
                setLoadingState(false)
                Snackbar.make(
                    binding.root,
                    "Password reset email sent to $email",
                    Snackbar.LENGTH_LONG
                ).show()
            },
            onFailure = { exception ->
                setLoadingState(false)
                Snackbar.make(
                    binding.root,
                    "Password reset failed: ${exception.message}",
                    Snackbar.LENGTH_LONG
                ).show()
            }
        )
    }

    private fun navigateToDashboard() {
        // Method 1: Using Navigation Component with activity destination
        findNavController().navigate(R.id.action_loginFragment_to_dashboardFragment)

        // Optional: Add a transition animation
        requireActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Avoid memory leaks
    }
}