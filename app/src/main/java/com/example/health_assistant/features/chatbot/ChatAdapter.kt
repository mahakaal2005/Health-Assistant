package com.example.health_assistant.features.chatbot

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.health_assistant.databinding.ItemChatMessageBinding

/**
 * Simple adapter for chat messages with proper logging
 * Follows the app's existing adapter patterns
 */
class ChatAdapter : ListAdapter<ChatMessage, ChatAdapter.ChatViewHolder>(ChatDiffCallback()) {
    
    companion object {
        private const val TAG = "HealthChatbot_Adapter"
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        Log.d(TAG, "Creating new ViewHolder")
        val binding = ItemChatMessageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ChatViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val message = getItem(position)
        Log.d(TAG, "Binding message at position $position, isFromUser: ${message.isFromUser}")
        holder.bind(message)
    }
    
    override fun getItemCount(): Int {
        val count = super.getItemCount()
        Log.d(TAG, "Total messages: $count")
        return count
    }
    
    class ChatViewHolder(private val binding: ItemChatMessageBinding) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(message: ChatMessage) {
            try {
                if (message.isFromUser) {
                    // Show user message bubble
                    binding.userMessageContainer.visibility = View.VISIBLE
                    binding.botMessageContainer.visibility = View.GONE
                    
                    binding.userMessageText.text = message.content
                    binding.userMessageTime.text = message.getFormattedTime()
                    
                    Log.d(TAG, "Bound user message: ${message.content.take(50)}...")
                } else {
                    // Show bot message bubble
                    binding.userMessageContainer.visibility = View.GONE
                    binding.botMessageContainer.visibility = View.VISIBLE
                    
                    binding.botMessageText.text = message.content
                    binding.botMessageTime.text = message.getFormattedTime()
                    
                    Log.d(TAG, "Bound bot message: ${message.content.take(50)}...")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error binding message at position $adapterPosition", e)
            }
        }
    }
    
    /**
     * DiffUtil callback for efficient list updates
     */
    private class ChatDiffCallback : DiffUtil.ItemCallback<ChatMessage>() {
        override fun areItemsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
            // Since we don't have IDs, use timestamp and content
            return oldItem.timestamp == newItem.timestamp && oldItem.isFromUser == newItem.isFromUser
        }
        
        override fun areContentsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
            return oldItem == newItem
        }
    }
}