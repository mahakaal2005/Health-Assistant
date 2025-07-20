package com.example.health_assistant.features.discover.data.remote

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages API keys for external health content services
 * 
 * SETUP INSTRUCTIONS:
 * 
 * 1. News API (Free - 1000 requests/day):
 *    - Go to https://newsapi.org/register
 *    - Sign up for free account
 *    - Get your API key
 *    - Replace NEWS_API_KEY below
 * 
 * 2. Guardian API (Free - unlimited):
 *    - Go to https://open-platform.theguardian.com/access/
 *    - Register for free API key
 *    - Option A: Create JSON file at app/src/main/assets/api_keys/guardian_api_key.json
 *    - Option B: Replace guardianDirectApiKey below
 * 
 * 3. YouTube Data API v3 (Free - 10,000 units/day):
 *    - Go to Google Cloud Console
 *    - Enable YouTube Data API v3
 *    - Create credentials (API key)
 *    - Replace YOUTUBE_API_KEY below
 * 
 * 4. Google AI (Gemini) API:
 *    - Go to https://makersuite.google.com/app/apikey
 *    - Create API key (you mentioned you have Google AI Pro)
 *    - Replace GOOGLE_AI_API_KEY below
 * 

 */
@Singleton
class ApiKeyManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    // TODO: Replace these with your actual API keys
    val newsApiKey: String = "61750c9a7d934ed9b1673b8fb5dd1a34"
    val youtubeApiKey: String = "AIzaSyDbkJ3ROQoojwRfOEpKjB9C1QuLMKLae_w"
    val googleAiApiKey: String = "AIzaSyCar4_XjApCcYixNXCnPTQ0eNYs3cGiY2k"

    
    // Guardian API key - reads from JSON file or falls back to direct key
    val guardianApiKey: String by lazy {
        readGuardianApiKey()
    }
    
    // Direct Guardian API key fallback
    private val guardianDirectApiKey: String = "YOUR_GUARDIAN_API_KEY_HERE"
    
    /**
     * Reads Guardian API key from JSON file or falls back to direct key
     */
    private fun readGuardianApiKey(): String {
        return try {
            val inputStream = context.assets.open("api_keys/guardian_api_key.json")
            val size = inputStream.available()
            val buffer = ByteArray(size)
            inputStream.read(buffer)
            inputStream.close()
            val json = JSONObject(String(buffer, Charsets.UTF_8))
            val apiKey = json.getString("api-key")
            Log.d("ApiKeyManager", "Guardian API key loaded from JSON file")
            apiKey
        } catch (e: Exception) {
            Log.d("ApiKeyManager", "Guardian JSON file not found, using direct API key: ${e.message}")
            guardianDirectApiKey
        }
    }
    
    /**
     * Get Guardian API configuration from JSON file
     */
    fun getGuardianApiConfig(): GuardianApiConfig {
        return try {
            val inputStream = context.assets.open("api_keys/guardian_api_key.json")
            val size = inputStream.available()
            val buffer = ByteArray(size)
            inputStream.read(buffer)
            inputStream.close()
            val json = JSONObject(String(buffer, Charsets.UTF_8))
            
            GuardianApiConfig(
                apiKey = json.getString("api-key"),
                format = json.optString("format", "json"),
                showFields = json.optString("show-fields", "headline,standfirst,trailText,byline,main,body,thumbnail")
            )
        } catch (e: Exception) {
            Log.d("ApiKeyManager", "Using default Guardian API config: ${e.message}")
            GuardianApiConfig(
                apiKey = guardianDirectApiKey,
                format = "json",
                showFields = "headline,standfirst,trailText,byline,main,body,thumbnail"
            )
        }
    }
    
    /**
     * Check if all required API keys are configured
     */
    fun areKeysConfigured(): Boolean {
        val newsConfigured = newsApiKey != "YOUR_NEWS_API_KEY_HERE"
        val guardianConfigured = guardianApiKey != "YOUR_GUARDIAN_API_KEY_HERE"
        val youtubeConfigured = youtubeApiKey != "YOUR_YOUTUBE_API_KEY_HERE"
        val googleAiConfigured = googleAiApiKey != "YOUR_GOOGLE_AI_API_KEY_HERE"
        
        Log.d("ApiKeyManager", "API Key Status:")
        Log.d("ApiKeyManager", "News API: ${if (newsConfigured) "✓ Configured" else "✗ Missing"}")
        Log.d("ApiKeyManager", "Guardian API: ${if (guardianConfigured) "✓ Configured" else "✗ Missing"}")
        Log.d("ApiKeyManager", "YouTube API: ${if (youtubeConfigured) "✓ Configured" else "✗ Missing"}")
        Log.d("ApiKeyManager", "Google AI API: ${if (googleAiConfigured) "✓ Configured" else "✗ Missing"}")
        
        return newsConfigured && guardianConfigured && youtubeConfigured && googleAiConfigured
    }
    
    /**
     * Check if at least some API keys are configured (more lenient check)
     */
    fun hasAnyKeysConfigured(): Boolean {
        val newsConfigured = newsApiKey != "YOUR_NEWS_API_KEY_HERE"
        val guardianConfigured = guardianApiKey != "YOUR_GUARDIAN_API_KEY_HERE"
        val youtubeConfigured = youtubeApiKey != "YOUR_YOUTUBE_API_KEY_HERE"
        
        return newsConfigured || guardianConfigured || youtubeConfigured
    }
    
    /**
     * Get list of missing API keys
     */
    fun getMissingKeys(): List<String> {
        val missing = mutableListOf<String>()
        
        if (newsApiKey == "YOUR_NEWS_API_KEY_HERE") {
            missing.add("News API Key (newsapi.org)")
        }
        if (guardianApiKey == "YOUR_GUARDIAN_API_KEY_HERE") {
            missing.add("Guardian API Key (open-platform.theguardian.com)")
        }
        if (youtubeApiKey == "YOUR_YOUTUBE_API_KEY_HERE") {
            missing.add("YouTube API Key (Google Cloud Console)")
        }
        if (googleAiApiKey == "YOUR_GOOGLE_AI_API_KEY_HERE") {
            missing.add("Google AI API Key (makersuite.google.com)")
        }
        
        return missing
    }
    
    /**
     * Get setup instructions for missing keys
     */
    fun getSetupInstructions(): String {
        return """
        🔑 API Setup Instructions:
        
        1. News API (Free - 1000 requests/day):
           • Visit: https://newsapi.org/register
           • Sign up for free account
           • Copy your API key
           
        2. Guardian API (Free - unlimited):
           • Visit: https://open-platform.theguardian.com/access/
           • Register for free API key
           • Option A: Create JSON file at app/src/main/assets/api_keys/guardian_api_key.json
           • Option B: Update guardianDirectApiKey in ApiKeyManager.kt
           
        3. YouTube Data API v3 (Free - 10,000 units/day):
           • Visit: Google Cloud Console
           • Enable YouTube Data API v3
           • Create credentials (API key)
           • Copy your API key
           
        4. Google AI (Gemini) API:
           • Visit: https://makersuite.google.com/app/apikey
           • Create API key (you have Google AI Pro)
           • Copy your API key
           
        5. Update ApiKeyManager.kt with your keys
        
        All these APIs are FREE with generous limits!
        """.trimIndent()
    }
}

/**
 * Configuration data class for Guardian API
 */
data class GuardianApiConfig(
    val apiKey: String,
    val format: String = "json",
    val showFields: String = "headline,standfirst,trailText,byline,main,body,thumbnail"
)