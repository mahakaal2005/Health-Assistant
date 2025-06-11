package com.example.health_assistant.auth.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.health_assistant.R
import com.example.health_assistant.databinding.AuthFragmentLoginBinding

class LoginFragment : Fragment() {

    private var _binding: AuthFragmentLoginBinding? = null
    // This property is only valid between onCreateView and onDestroyView
    private val binding get() = _binding!!

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

        // Set up the Login button click listener
        binding.loginButton.setOnClickListener {
            // Validate form and attempt login
            if (validateForm()) {
                // Simulate successful login
                navigateToDashboard()
            }
        }

        // Set up the "Don't have an account" prompt click listener
        binding.createAccountPrompt.setOnClickListener {
            // Navigate to SignUpFragment
            findNavController().navigate(R.id.action_loginFragment_to_signUpFragment)
        }

        // Set up the Forgot Password click listener
        binding.forgotPassword.setOnClickListener {
            // Handle forgot password flow (not implemented)
            // You could navigate to a password reset fragment
        }
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

    private fun navigateToDashboard() {
        // Navigate to Dashboard (MainActivity)
        findNavController().navigate(R.id.action_loginFragment_to_dashboardActivity)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Avoid memory leaks
    }
}