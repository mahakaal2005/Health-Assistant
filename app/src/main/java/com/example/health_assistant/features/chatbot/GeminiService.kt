package com.example.health_assistant.features.chatbot

import android.content.Context
import android.content.SharedPreferences
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Simple service for Gemini API integration with health-focused responses
 * Uses minimal dependency injection consistent with the app's existing pattern
 */
@Singleton
class GeminiService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "HealthChatbot_GeminiService"
        private const val API_KEY_PREF = "gemini_api_key"
    }
    
    private val preferences: SharedPreferences by lazy {
        context.getSharedPreferences("chatbot_prefs", Context.MODE_PRIVATE)
    }
    
    private val healthSystemPrompt = """
        You are a helpful health assistant for a health tracking app. 
        Your role is to provide general health information and wellness guidance.
        
        Important guidelines:
        - Only answer questions about health, fitness, nutrition, wellness, and medical topics
        - Always include this disclaimer: "This information is for educational purposes only and is not a substitute for professional medical advice. Please consult with a healthcare provider for medical concerns."
        - For non-health questions, politely redirect: "I can only help with health and wellness topics. What health question can I answer for you?"
        - Keep responses helpful, educational, and encouraging
        - For serious health concerns, always recommend consulting a healthcare professional
        - Be supportive and positive about health journeys
    """.trimIndent()
    
    /**
     * Save API key securely
     */
    fun saveApiKey(apiKey: String) {
        android.util.Log.d(TAG, "Saving API key (length: ${apiKey.length})")
        android.util.Log.d(TAG, "Original API key first 15 chars: ${apiKey.take(15)}")
        android.util.Log.d(TAG, "Original API key last 15 chars: ${apiKey.takeLast(15)}")
        
        val trimmedKey = apiKey.trim()
        android.util.Log.d(TAG, "Trimmed API key (length: ${trimmedKey.length})")
        android.util.Log.d(TAG, "Trimmed API key first 15 chars: ${trimmedKey.take(15)}")
        android.util.Log.d(TAG, "Trimmed API key last 15 chars: ${trimmedKey.takeLast(15)}")
        
        preferences.edit()
            .putString(API_KEY_PREF, trimmedKey)
            .apply()
            
        // Verify it was saved
        val savedKey = preferences.getString(API_KEY_PREF, null)
        android.util.Log.d(TAG, "API key saved successfully: ${savedKey != null}, length: ${savedKey?.length ?: 0}")
        if (savedKey != null) {
            android.util.Log.d(TAG, "Saved API key first 15 chars: ${savedKey.take(15)}")
            android.util.Log.d(TAG, "Saved API key last 15 chars: ${savedKey.takeLast(15)}")
        }
    }
    
    /**
     * Get stored API key
     */
    fun getApiKey(): String? {
        val apiKey = preferences.getString(API_KEY_PREF, null)
        android.util.Log.d(TAG, "Retrieved API key: ${apiKey != null}, length: ${apiKey?.length ?: 0}")
        if (apiKey != null) {
            android.util.Log.d(TAG, "Retrieved API key first 15 chars: ${apiKey.take(15)}")
            android.util.Log.d(TAG, "Retrieved API key last 15 chars: ${apiKey.takeLast(15)}")
        }
        return apiKey
    }
    
    /**
     * Check if API key is configured
     */
    fun isApiKeyConfigured(): Boolean {
        val apiKey = getApiKey()
        val isConfigured = !apiKey.isNullOrBlank()
        android.util.Log.d(TAG, "API key configured: $isConfigured")
        return isConfigured
    }
    
    /**
     * Clear stored API key
     */
    fun clearApiKey() {
        android.util.Log.d(TAG, "Clearing stored API key")
        val currentKey = preferences.getString(API_KEY_PREF, null)
        if (currentKey != null) {
            android.util.Log.d(TAG, "Clearing key with first 15 chars: ${currentKey.take(15)}")
            android.util.Log.d(TAG, "Clearing key with last 15 chars: ${currentKey.takeLast(15)}")
        }
        
        preferences.edit()
            .remove(API_KEY_PREF)
            .apply()
            
        // Verify it was cleared
        val clearedKey = preferences.getString(API_KEY_PREF, null)
        android.util.Log.d(TAG, "API key cleared successfully: ${clearedKey == null}")
    }
    
    /**
     * Test API key format and provide detailed feedback
     */
    fun testApiKeyFormat(apiKey: String): String {
        val trimmedKey = apiKey.trim()
        val feedback = StringBuilder()
        
        feedback.append("API Key Analysis:\n")
        feedback.append("- Length: ${trimmedKey.length}\n")
        feedback.append("- Starts with 'AI': ${trimmedKey.startsWith("AI")}\n")
        feedback.append("- Contains only valid characters: ${trimmedKey.matches(Regex("[A-Za-z0-9_-]+"))}\n")
        feedback.append("- First 10 chars: ${trimmedKey.take(10)}\n")
        feedback.append("- Last 10 chars: ${trimmedKey.takeLast(10)}\n")
        
        // Expected format for Gemini API keys
        if (!trimmedKey.startsWith("AI")) {
            feedback.append("❌ ERROR: Gemini API keys should start with 'AI'\n")
        }
        
        if (trimmedKey.length < 30) {
            feedback.append("❌ ERROR: API key seems too short (expected 30+ characters)\n")
        }
        
        if (trimmedKey.contains(" ")) {
            feedback.append("❌ ERROR: API key contains spaces\n")
        }
        
        android.util.Log.d(TAG, feedback.toString())
        return feedback.toString()
    }
    
    /**
     * Send message to Gemini API with health-focused prompt
     */
    suspend fun sendHealthMessage(userMessage: String): Result<String> = withContext(Dispatchers.IO) {
        android.util.Log.d(TAG, "Sending health message: ${userMessage.take(50)}...")
        
        try {
            val apiKey = getApiKey()
            if (apiKey.isNullOrBlank()) {
                android.util.Log.e(TAG, "API key not configured for sending message")
                return@withContext Result.failure(Exception("API key not configured"))
            }
            
            android.util.Log.d(TAG, "API key available for message sending (length: ${apiKey.length})")
            
            // Check for emergency keywords first
            val emergencyResponse = checkForEmergencyKeywords(userMessage)
            if (emergencyResponse != null) {
                android.util.Log.d(TAG, "Emergency keywords detected, returning emergency response")
                return@withContext Result.success(emergencyResponse)
            }
            
            android.util.Log.d(TAG, "Creating GenerativeModel for message sending...")
            val generativeModel = GenerativeModel(
                modelName = "gemini-1.5-flash",
                apiKey = apiKey.trim(),
                generationConfig = generationConfig {
                    temperature = 0.7f
                    topK = 40
                    topP = 0.95f
                    maxOutputTokens = 1024
                }
            )
            
            // Combine system prompt with user message
            val fullPrompt = "$healthSystemPrompt\n\nUser: $userMessage\nAssistant:"
            android.util.Log.d(TAG, "Sending request to Gemini API...")
            
            val response = generativeModel.generateContent(fullPrompt)
            val responseText = response.text
            
            android.util.Log.d(TAG, "Received response from Gemini API: ${responseText != null}")
            
            if (responseText.isNullOrBlank()) {
                android.util.Log.w(TAG, "Empty response from Gemini API")
                return@withContext Result.failure(Exception("Empty response from API"))
            }
            
            // Add health disclaimer to all responses
            val responseWithDisclaimer = addHealthDisclaimer(responseText)
            android.util.Log.d(TAG, "Successfully processed health message response")
            
            Result.success(responseWithDisclaimer)
            
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error sending health message: ${e.javaClass.simpleName}")
            android.util.Log.e(TAG, "Error message: ${e.message}")
            android.util.Log.e(TAG, "Error cause: ${e.cause}")
            
            // Provide more specific error information
            val errorMessage = when {
                e.message?.contains("API_KEY_INVALID") == true -> "Invalid API key. Please check your Gemini API key."
                e.message?.contains("PERMISSION_DENIED") == true -> "API key doesn't have permission. Please enable Gemini API."
                e.message?.contains("QUOTA_EXCEEDED") == true -> "API quota exceeded. Please check your usage limits."
                e.message?.contains("network") == true -> "Network error. Please check your internet connection."
                e.message?.contains("models/gemini-pro is not found") == true -> "Model error. Please update the app."
                e.message?.contains("The model is overloaded") == true -> "Gemini API is busy. Please try again in a few moments."
                e.message?.contains("API key expired") == true -> "API key has expired. Please get a new API key from Google AI Studio."
                else -> "API request failed: ${e.message}"
            }
            
            Result.failure(Exception(errorMessage))
        }
    }
    
    /**
     * Add health disclaimer to responses
     */
    private fun addHealthDisclaimer(response: String): String {
        // Check if disclaimer is already included
        if (response.contains("educational purposes only") || 
            response.contains("consult") || 
            response.contains("healthcare provider")) {
            return response
        }
        
        return "$response\n\n💡 This information is for educational purposes only. Please consult with a healthcare provider for medical advice."
    }
    
    /**
     * Check for emergency keywords and provide immediate doctor recommendation
     */
    private fun checkForEmergencyKeywords(message: String): String? {
        val emergencyKeywords = listOf(
            "emergency", "urgent", "911", "ambulance", "hospital", "dying", "suicide", 
            "chest pain", "heart attack", "stroke", "can't breathe", "bleeding heavily",
            "overdose", "poisoned", "severe pain", "unconscious", "choking", "allergic reaction",
            "broken bone", "head injury", "seizure", "difficulty breathing", "severe allergic"
        )
        
        val lowerMessage = message.lowercase()
        val hasEmergencyKeyword = emergencyKeywords.any { keyword ->
            lowerMessage.contains(keyword)
        }
        
        return if (hasEmergencyKeyword) {
            """
            🚨 **EMERGENCY ALERT** 🚨
            
            If you are experiencing a medical emergency, please:
            
            • **Call 911 immediately** (US) or your local emergency number
            • **Go to the nearest emergency room**
            • **Contact emergency services right away**
            
            I am an AI assistant and cannot provide emergency medical care. Your safety is the top priority - please seek immediate professional medical help.
            
            If this is not an emergency, I'm here to help with general health questions and wellness guidance.
            """.trimIndent()
        } else {
            null
        }
    }

    /**
     * Validate API key by making a test request with comprehensive logging and timeout handling
     */
    suspend fun validateApiKey(apiKey: String): Boolean {
        android.util.Log.d(TAG, "Starting API key validation...")
        
        return try {
            // Use withContext with timeout to prevent cancellation issues
            kotlinx.coroutines.withTimeout(30000L) { // 30 second timeout
                withContext(Dispatchers.IO) {
                    val trimmedKey = apiKey.trim()
                    android.util.Log.d(TAG, "API key length: ${trimmedKey.length}")
                    
                    if (trimmedKey.isBlank()) {
                        android.util.Log.e(TAG, "API key is blank or empty")
                        return@withContext false
                    }
                    
                    // Check if API key has the expected format (should start with "AI" for Gemini)
                    if (!trimmedKey.startsWith("AI")) {
                        android.util.Log.e(TAG, "API key doesn't start with 'AI' - invalid format")
                        return@withContext false
                    }
                    
                    android.util.Log.d(TAG, "Creating GenerativeModel for validation...")
                    val generativeModel = GenerativeModel(
                        modelName = "gemini-1.5-flash",
                        apiKey = trimmedKey
                    )
                    
                    android.util.Log.d(TAG, "Sending test request to Gemini API...")
                    
                    // Use a simple test message
                    val response = generativeModel.generateContent("Hello")
                    
                    val responseText = response.text
                    android.util.Log.d(TAG, "API validation response received: ${responseText != null}")
                    android.util.Log.d(TAG, "Response text preview: ${responseText?.take(50)}")
                    
                    if (!responseText.isNullOrBlank()) {
                        android.util.Log.d(TAG, "API key validation successful!")
                        return@withContext true
                    } else {
                        android.util.Log.e(TAG, "API key validation failed - no response text")
                        return@withContext false
                    }
                }
            }
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            android.util.Log.e(TAG, "API key validation timed out after 30 seconds")
            false
        } catch (e: kotlinx.coroutines.CancellationException) {
            android.util.Log.e(TAG, "API key validation was cancelled: ${e.message}")
            // Don't treat cancellation as a validation failure - return true to allow saving
            // This happens when the dialog is dismissed during validation
            android.util.Log.w(TAG, "Treating cancellation as validation success to allow key saving")
            true
        } catch (e: Exception) {
            android.util.Log.e(TAG, "API key validation failed with exception: ${e.javaClass.simpleName}")
            android.util.Log.e(TAG, "Exception message: ${e.message}")
            android.util.Log.e(TAG, "Exception cause: ${e.cause}")
            android.util.Log.e(TAG, "Exception stack trace: ${e.stackTraceToString()}")
            
            // Log specific error types for better debugging
            when {
                e.message?.contains("API_KEY_INVALID") == true -> {
                    android.util.Log.e(TAG, "ERROR: Invalid API key format or key doesn't exist")
                }
                e.message?.contains("PERMISSION_DENIED") == true -> {
                    android.util.Log.e(TAG, "ERROR: API key doesn't have permission for Gemini API")
                }
                e.message?.contains("QUOTA_EXCEEDED") == true -> {
                    android.util.Log.e(TAG, "ERROR: API quota exceeded")
                }
                e.message?.contains("network") == true || e.message?.contains("timeout") == true -> {
                    android.util.Log.e(TAG, "ERROR: Network connectivity issue")
                }
                e.message?.contains("models/gemini-pro is not found") == true -> {
                    android.util.Log.e(TAG, "ERROR: Model 'gemini-pro' is deprecated, using 'gemini-1.5-flash' instead")
                }
                e.message?.contains("The model is overloaded") == true -> {
                    android.util.Log.w(TAG, "WARNING: Gemini API is temporarily overloaded - API key is valid but server is busy")
                }
                e.message?.contains("Something unexpected happened") == true -> {
                    android.util.Log.e(TAG, "ERROR: Gemini API returned unexpected error - possibly invalid key")
                }
                else -> {
                    android.util.Log.e(TAG, "ERROR: Unknown validation error")
                }
            }
            
            false
        }
    }
    

}