package com.example.health_assistant.features.discover.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.health_assistant.databinding.ItemHealthTopicBinding
import com.example.health_assistant.features.discover.model.HealthTopic

/**
 * Adapter for displaying featured health topics in a horizontal carousel
 */
class FeaturedTopicsAdapter(
    private val onTopicClick: (HealthTopic) -> Unit
) : ListAdapter<HealthTopic, FeaturedTopicsAdapter.TopicViewHolder>(TopicDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TopicViewHolder {
        val binding = ItemHealthTopicBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TopicViewHolder(binding, onTopicClick)
    }

    override fun onBindViewHolder(holder: TopicViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class TopicViewHolder(
        private val binding: ItemHealthTopicBinding,
        private val onTopicClick: (HealthTopic) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(topic: HealthTopic) {
            binding.apply {
                // Set text data
                topicTitle.text = topic.title
                topicDescription.text = topic.description

                // Set category label if available, or hide it
                if (!topic.category.isNullOrEmpty()) {
                    topicCategory.text = topic.category
                    topicCategory.visibility = android.view.View.VISIBLE
                } else {
                    topicCategory.visibility = android.view.View.GONE
                }

                // For a real app, you would load the image with a library like Glide/Coil
                // For now, just set a placeholder
                topicImage.setImageResource(android.R.drawable.ic_menu_gallery)

                // Set click listener on the item
                root.setOnClickListener {
                    onTopicClick(topic)
                }
            }
        }
    }
}

/**
 * DiffUtil callback for efficient RecyclerView updates
 */
private class TopicDiffCallback : DiffUtil.ItemCallback<HealthTopic>() {
    override fun areItemsTheSame(oldItem: HealthTopic, newItem: HealthTopic): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: HealthTopic, newItem: HealthTopic): Boolean {
        return oldItem == newItem
    }
}