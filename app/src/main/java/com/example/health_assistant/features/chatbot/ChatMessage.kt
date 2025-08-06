package com.example.health_assistant.features.chatbot

import android.util.Log
import java.text.SimpleDateFormat
import java.util.*

/**
 * Simple data class for chat messages with time formatting helpers
 * Follows the app's existing data model patterns
 */
data class ChatMessage(
    val content: String,
    val isFromUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
) {
    companion object {
        private const val TAG = "HealthChatbot_Message"
        private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        private val dateFormat = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
    }
    
    /**
     * Get formatted time for display (e.g., "10:30 AM")
     */
    fun getFormattedTime(): String {
        return try {
            timeFormat.format(Date(timestamp))
        } catch (e: Exception) {
            Log.w(TAG, "Error formatting time for timestamp: $timestamp", e)
            "Now"
        }
    }
    
    /**
     * Get formatted date and time for display (e.g., "Jan 15, 10:30 AM")
     */
    fun getFormattedDateTime(): String {
        return try {
            dateFormat.format(Date(timestamp))
        } catch (e: Exception) {
            Log.w(TAG, "Error formatting date/time for timestamp: $timestamp", e)
            "Now"
        }
    }
    
    /**
     * Check if this message is from today
     */
    fun isFromToday(): Boolean {
        val today = Calendar.getInstance()
        val messageDate = Calendar.getInstance().apply { timeInMillis = timestamp }
        
        return today.get(Calendar.YEAR) == messageDate.get(Calendar.YEAR) &&
               today.get(Calendar.DAY_OF_YEAR) == messageDate.get(Calendar.DAY_OF_YEAR)
    }
    
    /**
     * Check if this is a user message
     */
    fun isUserMessage(): Boolean = isFromUser
    
    /**
     * Check if this is a bot message
     */
    fun isBotMessage(): Boolean = !isFromUser
    
    /**
     * Get message type for logging
     */
    fun getMessageType(): String = if (isFromUser) "USER" else "BOT"
    
    /**
     * Get truncated content for logging (first 100 characters)
     */
    fun getTruncatedContent(): String {
        return if (content.length > 100) {
            "${content.take(100)}..."
        } else {
            content
        }
    }
    
    override fun toString(): String {
        return "ChatMessage(type=${getMessageType()}, content='${getTruncatedContent()}', time=${getFormattedTime()})"
    }
}