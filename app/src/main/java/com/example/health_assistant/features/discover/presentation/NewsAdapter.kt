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
 * Simple adapter for displaying health news in horizontal RecyclerView
 */
class NewsAdapter(
    private val onItemClick: (HealthContent) -> Unit = {}
) : ListAdapter<HealthContent, NewsAdapter.NewsViewHolder>(ContentDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewsViewHolder {
        val binding = ItemSimpleContentCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return NewsViewHolder(binding, onItemClick)
    }

    override fun onBindViewHolder(holder: NewsViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class NewsViewHolder(
        private val binding: ItemSimpleContentCardBinding,
        private val onItemClick: (HealthContent) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(news: HealthContent) {
            binding.apply {
                // Set news data
                textTitle.text = news.title
                textDescription.text = news.description
                textSource.text = news.sourceName ?: "Unknown Source"
                textDate.text = news.publishedDate

                // Load image with Glide
                Glide.with(imageContent.context)
                    .load(news.imageUrl)
                    .placeholder(R.drawable.ic_newspaper)
                    .error(R.drawable.ic_newspaper)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(imageContent)

                // Handle click through callback
                root.setOnClickListener {
                    onItemClick(news)
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