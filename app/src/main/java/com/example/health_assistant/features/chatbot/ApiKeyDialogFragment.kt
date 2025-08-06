package com.example.health_assistant.features.chatbot

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.example.health_assistant.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import dagger.hilt.android.AndroidEntryPoint

/**
 * Simple dialog for API key configuration
 * Follows Material Design patterns
 */
@AndroidEntryPoint
class ApiKeyDialogFragment(
    private val onApiKeySaved: (String) -> Unit
) : DialogFragment() {
    
    companion object {
        private const val TAG = "HealthChatbot_ApiKeyDialog"
    }
    
    private val viewModel: ChatViewModel by viewModels()
    
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        Log.d(TAG, "Creating API key dialog")
        
        val context = requireContext()
        val inflater = LayoutInflater.from(context)
        
        // Create custom layout for the dialog
        val dialogView = inflater.inflate(R.layout.dialog_api_key, null)
        val apiKeyLayout = dialogView.findViewById<TextInputLayout>(R.id.api_key_input_layout)
        val apiKeyInput = dialogView.findViewById<TextInputEditText>(R.id.api_key_input)
        
        val dialog = MaterialAlertDialogBuilder(context)
            .setTitle("Configure Gemini API Key")
            .setMessage("Enter your Google Gemini API key to enable the health chatbot. You can get a free API key from Google AI Studio.")
            .setView(dialogView)
            .setPositiveButton("Save", null) // Set to null to prevent auto-dismiss
            .setNeutralButton("Save Without Validation") { _, _ ->
                val apiKey = apiKeyInput.text?.toString()?.trim()
                if (!apiKey.isNullOrBlank()) {
                    Log.d(TAG, "Saving API key without validation (bypass mode)")
                    onApiKeySaved(apiKey)
                    Toast.makeText(context, "API key saved (validation bypassed)", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Please enter an API key", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                Log.d(TAG, "API key dialog cancelled")
                dialog.dismiss()
            }
            .setCancelable(false) // Force user to configure API key
            .create()
            
        // Override the positive button click to prevent auto-dismiss
        dialog.setOnShowListener {
            val saveButton = dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)
            saveButton.setOnClickListener {
                val apiKey = apiKeyInput.text?.toString()?.trim()
                if (!apiKey.isNullOrBlank()) {
                    Log.d(TAG, "User provided API key for validation")
                    
                    // Log API key format details
                    Log.d(TAG, "API key length: ${apiKey.length}")
                    Log.d(TAG, "API key starts with 'AI': ${apiKey.startsWith("AI")}")
                    Log.d(TAG, "API key first 10 chars: ${apiKey.take(10)}")
                    Log.d(TAG, "API key last 10 chars: ${apiKey.takeLast(10)}")
                    
                    // Disable the button during validation
                    saveButton.isEnabled = false
                    saveButton.text = "Validating..."
                    
                    validateAndSaveApiKey(apiKey) { success ->
                        // Re-enable the button
                        saveButton.isEnabled = true
                        saveButton.text = "Save"
                        
                        if (success) {
                            dialog.dismiss()
                        }
                    }
                } else {
                    Log.w(TAG, "Empty API key provided")
                    Toast.makeText(context, "Please enter a valid API key", Toast.LENGTH_SHORT).show()
                }
            }
        }
        
        return dialog
    }
    
    /**
     * Validate API key and save if valid
     */
    private fun validateAndSaveApiKey(apiKey: String, onComplete: (Boolean) -> Unit) {
        Log.d(TAG, "Starting API key validation process...")
        Log.d(TAG, "API key length: ${apiKey.length}")
        Log.d(TAG, "API key starts with 'AI': ${apiKey.startsWith("AI")}")
        
        // Show loading state
        Toast.makeText(requireContext(), "Validating API key...", Toast.LENGTH_SHORT).show()
        
        viewModel.validateApiKey(apiKey) { isValid ->
            Log.d(TAG, "API key validation result: $isValid")
            
            if (isValid) {
                Log.d(TAG, "API key is valid, saving")
                onApiKeySaved(apiKey)
                Toast.makeText(requireContext(), "API key saved successfully!", Toast.LENGTH_SHORT).show()
                onComplete(true)
            } else {
                Log.e(TAG, "API key validation failed")
                
                // Provide more helpful error message based on the validation result
                val errorMessage = when {
                    !apiKey.startsWith("AI") -> "Invalid API key format. Gemini API keys should start with 'AI'."
                    apiKey.length < 20 -> "API key seems too short. Please check your key."
                    else -> "Validation failed. The API might be temporarily overloaded. Try 'Save Without Validation' to proceed."
                }
                
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
                onComplete(false)
            }
        }
    }
}