package com.example.health_assistant.features.chatbot

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Simple ViewModel for chat functionality with comprehensive logging
 * Uses minimal DI consistent with the app's existing pattern
 */
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val geminiService: GeminiService
) : ViewModel() {
    
    companion object {
        private const val TAG = "HealthChatbot_ViewModel"
    }
    
    // Messages list - kept in memory only for simplicity
    private val _messages = MutableLiveData<List<ChatMessage>>(emptyList())
    val messages: LiveData<List<ChatMessage>> = _messages
    
    // Loading state
    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading
    
    // Error state
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage
    
    // API key configuration state
    private val _isApiKeyConfigured = MutableLiveData<Boolean>()
    val isApiKeyConfigured: LiveData<Boolean> = _isApiKeyConfigured
    
    init {
        Log.d(TAG, "ChatViewModel initialized")
        checkApiKeyConfiguration()
        addWelcomeMessage()
    }
    
    /**
     * Check if API key is configured
     */
    private fun checkApiKeyConfiguration() {
        val isConfigured = geminiService.isApiKeyConfigured()
        _isApiKeyConfigured.value = isConfigured
        Log.d(TAG, "API key configured: $isConfigured")
    }
    
    /**
     * Add welcome message when chat starts
     */
    private fun addWelcomeMessage() {
        if (geminiService.isApiKeyConfigured()) {
            val welcomeMessage = ChatMessage(
                content = "Hello! I'm your Health Assistant. I can help you with questions about fitness, nutrition, wellness, and general health topics. What would you like to know?",
                isFromUser = false
            )
            addMessage(welcomeMessage)
            Log.d(TAG, "Welcome message added")
        } else {
            Log.d(TAG, "API key not configured, skipping welcome message")
        }
    }
    
    /**
     * Send a message to the health chatbot
     */
    fun sendMessage(content: String) {
        if (content.isBlank()) {
            Log.w(TAG, "Attempted to send empty message")
            return
        }
        
        Log.d(TAG, "Sending user message: ${content.take(100)}...")
        
        // Add user message immediately
        val userMessage = ChatMessage(content = content.trim(), isFromUser = true)
        addMessage(userMessage)
        
        // Clear any previous errors
        _errorMessage.value = null
        
        // Send to API
        _isLoading.value = true
        viewModelScope.launch {
            try {
                Log.d(TAG, "Calling Gemini API...")
                val startTime = System.currentTimeMillis()
                
                val result = geminiService.sendHealthMessage(content)
                
                val responseTime = System.currentTimeMillis() - startTime
                Log.d(TAG, "API response received in ${responseTime}ms")
                
                result.fold(
                    onSuccess = { response ->
                        Log.d(TAG, "API success: ${response.take(100)}...")
                        val botMessage = ChatMessage(content = response, isFromUser = false)
                        addMessage(botMessage)
                    },
                    onFailure = { error ->
                        Log.e(TAG, "API error: ${error.message}", error)
                        handleApiError(error)
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error sending message", e)
                _errorMessage.value = "An unexpected error occurred. Please try again."
            } finally {
                _isLoading.value = false
                Log.d(TAG, "Message sending completed")
            }
        }
    }
    
    /**
     * Handle API errors with user-friendly messages
     */
    private fun handleApiError(error: Throwable) {
        val userFriendlyMessage = when {
            error.message?.contains("API key") == true -> {
                Log.e(TAG, "API key error detected")
                "Please check your API key configuration in settings."
            }
            error.message?.contains("network") == true || error.message?.contains("timeout") == true -> {
                Log.e(TAG, "Network error detected")
                "Network error. Please check your connection and try again."
            }
            error.message?.contains("quota") == true || error.message?.contains("limit") == true -> {
                Log.e(TAG, "Rate limit error detected")
                "API quota exceeded. Please try again later."
            }
            else -> {
                Log.e(TAG, "Generic API error: ${error.message}")
                "Sorry, I couldn't process your request. Please try again."
            }
        }
        
        _errorMessage.value = userFriendlyMessage
        
        // Add error message to chat
        val errorChatMessage = ChatMessage(
            content = "⚠️ $userFriendlyMessage",
            isFromUser = false
        )
        addMessage(errorChatMessage)
    }
    
    /**
     * Add a message to the chat
     */
    private fun addMessage(message: ChatMessage) {
        val currentMessages = _messages.value ?: emptyList()
        val updatedMessages = currentMessages + message
        _messages.value = updatedMessages
        
        Log.d(TAG, "Message added. Total messages: ${updatedMessages.size}")
    }
    
    /**
     * Clear all messages
     */
    fun clearMessages() {
        Log.d(TAG, "Clearing all messages")
        _messages.value = emptyList()
        addWelcomeMessage()
    }
    
    /**
     * Retry the last failed message
     */
    fun retryLastMessage() {
        val messages = _messages.value ?: return
        
        // Find the last user message
        val lastUserMessage = messages.findLast { it.isFromUser }
        if (lastUserMessage != null) {
            Log.d(TAG, "Retrying last message: ${lastUserMessage.content.take(50)}...")
            sendMessage(lastUserMessage.content)
        } else {
            Log.w(TAG, "No user message found to retry")
        }
    }
    
    /**
     * Save API key
     */
    fun saveApiKey(apiKey: String) {
        Log.d(TAG, "Saving API key")
        geminiService.saveApiKey(apiKey)
        checkApiKeyConfiguration()
        
        // Add welcome message after API key is configured
        if (geminiService.isApiKeyConfigured()) {
            clearMessages() // This will add welcome message
        }
    }
    
    /**
     * Validate API key with comprehensive logging
     */
    fun validateApiKey(apiKey: String, onResult: (Boolean) -> Unit) {
        Log.d(TAG, "Starting API key validation in ViewModel...")
        Log.d(TAG, "API key length: ${apiKey.length}")
        Log.d(TAG, "API key format check: starts with 'AI' = ${apiKey.startsWith("AI")}")
        
        viewModelScope.launch {
            try {
                Log.d(TAG, "Calling GeminiService.validateApiKey()...")
                val startTime = System.currentTimeMillis()
                
                val isValid = geminiService.validateApiKey(apiKey)
                
                val validationTime = System.currentTimeMillis() - startTime
                Log.d(TAG, "API key validation completed in ${validationTime}ms")
                Log.d(TAG, "Validation result: $isValid")
                
                // Call the result callback on the main thread
                onResult(isValid)
                
            } catch (e: Exception) {
                Log.e(TAG, "Exception during API key validation: ${e.javaClass.simpleName}")
                Log.e(TAG, "Exception message: ${e.message}")
                Log.e(TAG, "Exception stack trace: ${e.stackTrace.contentToString()}")
                
                // Always return false on exception
                onResult(false)
            }
        }
    }
    
    /**
     * Clear error message
     */
    fun clearError() {
        _errorMessage.value = null
        Log.d(TAG, "Error message cleared")
    }
    
    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "ChatViewModel cleared")
    }
}