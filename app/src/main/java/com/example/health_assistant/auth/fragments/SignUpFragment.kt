package com.example.health_assistant.auth.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.health_assistant.R
import com.example.health_assistant.databinding.AuthFragmentSignupBinding
import java.util.Optional

class SignUpFragment : Fragment() {

    private var _binding: AuthFragmentSignupBinding? = null
    // This property is only valid between onCreateView and onDestroyView
    private val binding get() = _binding!!

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

        // Set up the Sign Up button click listener
        binding.signupButton.setOnClickListener {
            // Validate form and attempt account creation
            if (validateForm()) {
                // Simulate successful account creation
                navigateToDashboard()
            }
        }

        // Set up the "Already have an account" prompt click listener
        binding.loginPrompt.setOnClickListener {
            // Navigate to LoginFragment
            findNavController().navigate(R.id.action_signUpFragment_to_loginFragment)
        }
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

    private fun navigateToDashboard() {
        // Method 1: Using Navigation Component with fragment destination
        findNavController().navigate(R.id.action_signUpFragment_to_dashboardFragment)


        // Method 2: Alternative approach - Start MainActivity directly with Intent
        //val intent = Intent(requireContext(), MainActivity::class.java)
        //Clear the back stack so user can't go back to signup with back button
        //intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        //startActivity(intent)
       // Optional: Add a transition animation
        requireActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Avoid memory leaks
    }
}