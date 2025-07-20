package com.example.health_assistant.features.discover.presentation

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.health_assistant.R
import com.example.health_assistant.databinding.ItemContentListBinding
import com.example.health_assistant.features.discover.domain.model.ContentType
import com.example.health_assistant.features.discover.domain.model.HealthContent

/**
 * Adapter for displaying health content in a vertical list
 */
class ContentListAdapter(
    private val onItemClick: (HealthContent) -> Unit = {}
) : ListAdapter<HealthContent, ContentListAdapter.ContentViewHolder>(ContentDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContentViewHolder {
        val binding = ItemContentListBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ContentViewHolder(binding, onItemClick)
    }

    override fun onBindViewHolder(holder: ContentViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ContentViewHolder(
        private val binding: ItemContentListBinding,
        private val onItemClick: (HealthContent) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(content: HealthContent) {
            binding.apply {
                // Set content data
                textTitle.text = content.title
                textDescription.text = content.description
                textSource.text = content.sourceName ?: "Unknown Source"
                textDate.text = content.publishedDate

                // Set content type indicator
                val typeText = when (content.contentType) {
                    ContentType.ARTICLE -> "Article"
                    ContentType.NEWS -> "News"
                    ContentType.VIDEO -> "Video"
                }
                textContentType.text = typeText

                // Load image with appropriate placeholder based on content type
                val placeholder = when (content.contentType) {
                    ContentType.ARTICLE -> R.drawable.ic_article
                    ContentType.NEWS -> R.drawable.ic_newspaper
                    ContentType.VIDEO -> R.drawable.ic_play_arrow
                }

                Glide.with(imageContent.context)
                    .load(content.imageUrl)
                    .placeholder(placeholder)
                    .error(placeholder)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(imageContent)

                // Handle click through callback
                root.setOnClickListener {
                    onItemClick(content)
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