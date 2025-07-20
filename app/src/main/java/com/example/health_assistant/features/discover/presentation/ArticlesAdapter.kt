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
 * Simple adapter for displaying health articles in horizontal RecyclerView
 */
class ArticlesAdapter(
    private val onItemClick: (HealthContent) -> Unit = {}
) : ListAdapter<HealthContent, ArticlesAdapter.ArticleViewHolder>(ContentDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArticleViewHolder {
        val binding = ItemSimpleContentCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ArticleViewHolder(binding, onItemClick)
    }

    override fun onBindViewHolder(holder: ArticleViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ArticleViewHolder(
        private val binding: ItemSimpleContentCardBinding,
        private val onItemClick: (HealthContent) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(article: HealthContent) {
            binding.apply {
                // Set article data
                textTitle.text = article.title
                textDescription.text = article.description
                textSource.text = article.sourceName ?: "Unknown Source"
                textDate.text = article.publishedDate

                // Load image with Glide
                Glide.with(imageContent.context)
                    .load(article.imageUrl)
                    .placeholder(R.drawable.ic_article)
                    .error(R.drawable.ic_article)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(imageContent)

                // Handle click through callback
                root.setOnClickListener {
                    onItemClick(article)
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