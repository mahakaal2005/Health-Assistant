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
 * Simple adapter for displaying health content in horizontal RecyclerViews
 * Handles all content types (articles, news, videos) with the same layout
 */
class SimpleContentAdapter : ListAdapter<HealthContent, SimpleContentAdapter.ContentViewHolder>(ContentDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContentViewHolder {
        val binding = ItemSimpleContentCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ContentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ContentViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ContentViewHolder(
        private val binding: ItemSimpleContentCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(content: HealthContent) {
            binding.apply {
                // Set content data
                textTitle.text = content.title
                textDescription.text = content.description
                textSource.text = content.sourceName ?: "Unknown Source"
                textDate.text = content.publishedDate

                // Load image with Glide
                Glide.with(imageContent.context)
                    .load(content.imageUrl)
                    .placeholder(R.drawable.ic_heart)
                    .error(R.drawable.ic_heart)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(imageContent)

                // Handle click to open content in browser
                root.setOnClickListener {
                    openContentInBrowser(content)
                }
            }
        }

        private fun openContentInBrowser(content: HealthContent) {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(content.sourceUrl))
                binding.root.context.startActivity(intent)
            } catch (e: Exception) {
                // Handle case where no browser is available
                // Could show a toast or log the error
                e.printStackTrace()
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