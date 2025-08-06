package com.example.health_assistant.features.chatbot

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.health_assistant.databinding.FragmentChatBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Simple chat fragment with comprehensive UI handling
 * Follows the app's existing fragment patterns with ViewBinding
 */
@AndroidEntryPoint
class ChatFragment : Fragment() {
    
    companion object {
        private const val TAG = "HealthChatbot_Fragment"
    }
    
    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: ChatViewModel by viewModels()
    private lateinit var chatAdapter: ChatAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(TAG, "Creating ChatFragment view")
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "ChatFragment view created")
        
        setupRecyclerView()
        setupInputHandling()
        setupClickListeners()
        observeViewModel()
        
        // Check API key configuration on startup
        checkApiKeyConfiguration()
    }
    
    /**
     * Setup RecyclerView with adapter and layout manager
     */
    private fun setupRecyclerView() {
        Log.d(TAG, "Setting up RecyclerView")
        
        chatAdapter = ChatAdapter()
        
        binding.messagesRecyclerView.apply {
            adapter = chatAdapter
            layoutManager = LinearLayoutManager(context).apply {
                stackFromEnd = true // Start from bottom
            }
            
            // Scroll to bottom when new messages are added
            chatAdapter.registerAdapterDataObserver(object : androidx.recyclerview.widget.RecyclerView.AdapterDataObserver() {
                override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                    super.onItemRangeInserted(positionStart, itemCount)
                    scrollToPosition(chatAdapter.itemCount - 1)
                }
            })
        }
    }
    
    /**
     * Setup input handling for message sending
     */
    private fun setupInputHandling() {
        Log.d(TAG, "Setting up input handling")
        
        // Handle send button on keyboard
        binding.messageInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage()
                true
            } else {
                false
            }
        }
        
        // Enable/disable send button based on input
        binding.messageInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val hasText = !s.isNullOrBlank()
                binding.sendButton.isEnabled = hasText
                binding.sendButton.alpha = if (hasText) 1.0f else 0.5f
            }
        })
    }
    
    /**
     * Setup click listeners for UI elements
     */
    private fun setupClickListeners() {
        Log.d(TAG, "Setting up click listeners")
        
        // Send button click
        binding.sendButton.setOnClickListener {
            sendMessage()
        }
        
        // Settings button click
        binding.settingsButton.setOnClickListener {
            Log.d(TAG, "Settings button clicked")
            showApiKeyDialog()
        }
    }
    
    /**
     * Observe ViewModel LiveData and update UI accordingly
     */
    private fun observeViewModel() {
        Log.d(TAG, "Setting up ViewModel observers")
        
        // Observe messages
        viewModel.messages.observe(viewLifecycleOwner) { messages ->
            Log.d(TAG, "Messages updated: ${messages.size} total")
            chatAdapter.submitList(messages)
            
            // Show/hide empty state
            binding.emptyState.visibility = if (messages.isEmpty()) View.VISIBLE else View.GONE
        }
        
        // Observe loading state
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            Log.d(TAG, "Loading state: $isLoading")
            binding.loadingIndicator.visibility = if (isLoading) View.VISIBLE else View.GONE
            
            // Disable input while loading
            binding.messageInput.isEnabled = !isLoading
            binding.sendButton.isEnabled = !isLoading && !binding.messageInput.text.isNullOrBlank()
        }
        
        // Observe error messages
        viewModel.errorMessage.observe(viewLifecycleOwner) { errorMessage ->
            if (!errorMessage.isNullOrBlank()) {
                Log.w(TAG, "Error message: $errorMessage")
                // Error is already added to chat by ViewModel
                // Show retry option for network errors
                if (errorMessage.contains("Network") || errorMessage.contains("connection")) {
                    showRetrySnackbar()
                }
            }
        }
        
        // Observe API key configuration
        viewModel.isApiKeyConfigured.observe(viewLifecycleOwner) { isConfigured ->
            Log.d(TAG, "API key configured: $isConfigured")
            if (!isConfigured) {
                showApiKeyDialog()
            }
        }
    }
    
    /**
     * Send message from input field
     */
    private fun sendMessage() {
        val messageText = binding.messageInput.text?.toString()?.trim()
        
        if (messageText.isNullOrBlank()) {
            Log.w(TAG, "Attempted to send empty message")
            return
        }
        
        Log.d(TAG, "Sending message: ${messageText.take(50)}...")
        
        // Send message through ViewModel
        viewModel.sendMessage(messageText)
        
        // Clear input field
        binding.messageInput.text?.clear()
        
        // Hide keyboard
        hideKeyboard()
    }
    
    /**
     * Check API key configuration and show dialog if needed
     */
    private fun checkApiKeyConfiguration() {
        Log.d(TAG, "Checking API key configuration")
        // ViewModel will automatically trigger the observer if API key is not configured
    }
    
    /**
     * Show API key configuration dialog
     */
    private fun showApiKeyDialog() {
        Log.d(TAG, "Showing API key dialog")
        
        val dialog = ApiKeyDialogFragment { apiKey ->
            Log.d(TAG, "API key provided from dialog")
            viewModel.saveApiKey(apiKey)
        }
        
        dialog.show(parentFragmentManager, "api_key_dialog")
    }
    
    /**
     * Hide soft keyboard
     */
    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) 
                as android.view.inputmethod.InputMethodManager
        imm.hideSoftInputFromWindow(binding.messageInput.windowToken, 0)
    }
    
    /**
     * Retry last failed message
     */
    private fun retryLastMessage() {
        Log.d(TAG, "Retrying last message")
        viewModel.retryLastMessage()
    }
    
    /**
     * Clear all messages
     */
    private fun clearMessages() {
        Log.d(TAG, "Clearing all messages")
        viewModel.clearMessages()
    }
    
    /**
     * Show retry snackbar for network errors
     */
    private fun showRetrySnackbar() {
        val snackbar = com.google.android.material.snackbar.Snackbar.make(
            binding.root,
            "Network error occurred",
            com.google.android.material.snackbar.Snackbar.LENGTH_LONG
        )
        
        snackbar.setAction("Retry") {
            Log.d(TAG, "Retry button clicked")
            retryLastMessage()
        }
        
        snackbar.setActionTextColor(
            androidx.core.content.ContextCompat.getColor(requireContext(), android.R.color.white)
        )
        
        snackbar.show()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(TAG, "ChatFragment view destroyed")
        _binding = null
    }
    
    override fun onResume() {
        super.onResume()
        Log.d(TAG, "ChatFragment resumed")
    }
    
    override fun onPause() {
        super.onPause()
        Log.d(TAG, "ChatFragment paused")
    }
}