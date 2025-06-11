package com.example.health_assistant.auth.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.health_assistant.R
import com.example.health_assistant.databinding.AuthFragmentAccountDecisionBinding

class AccountDecisionFragment : Fragment() {

    private var _binding: AuthFragmentAccountDecisionBinding? = null
    // This property is only valid between onCreateView and onDestroyView
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = AuthFragmentAccountDecisionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set up the Login button click listener
        binding.btnLogin.setOnClickListener {
            // Navigate to LoginFragment
            findNavController().navigate(R.id.action_accountDecisionFragment_to_loginFragment)
        }

        // Set up the Create Account button click listener
        binding.btnCreateAccount.setOnClickListener {
            // Navigate to SignUpFragment
            findNavController().navigate(R.id.action_accountDecisionFragment_to_signUpFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Avoid memory leaks
    }
}