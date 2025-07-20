package com.example.health_assistant.features.discover.presentation

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.health_assistant.R
import com.example.health_assistant.databinding.ItemSimpleContentCardBinding
import com.example.health_assistant.features.discover.domain.model.HealthContent

/**
 * Simple adapter for displaying health videos in horizontal RecyclerView
 */
class VideosAdapter(
    private val onItemClick: (HealthContent) -> Unit = {}
) : ListAdapter<HealthContent, VideosAdapter.VideoViewHolder>(ContentDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val binding = ItemSimpleContentCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return VideoViewHolder(binding, onItemClick)
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class VideoViewHolder(
        private val binding: ItemSimpleContentCardBinding,
        private val onItemClick: (HealthContent) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(video: HealthContent) {
            binding.apply {
                // Set video data
                textTitle.text = video.title
                textDescription.text = video.description
                textSource.text = video.sourceName ?: "Unknown Source"
                textDate.text = video.publishedDate

                // Load video thumbnail with Glide
                Glide.with(imageContent.context)
                    .load(video.imageUrl)
                    .placeholder(R.drawable.ic_play_arrow)
                    .error(R.drawable.ic_play_arrow)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(imageContent)

                // Handle click through callback
                root.setOnClickListener {
                    onItemClick(video)
                }
            }
        }
    }

    /**
     * DiffUtil callback for efficient list updates
     */
    private class ContentDiffCallback : DiffUtil.ItemCallback<HealthContent>() {
        override fun areItemsTheSame(oldItem: HealthContent, newItem: HealthContent): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: HealthContent, newItem: HealthContent): Boolean {
            return oldItem == newItem
        }
    }
}