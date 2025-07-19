package com.example.health_assistant.features.discover.presentation

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil3.load
import com.example.health_assistant.R
import com.example.health_assistant.databinding.ItemDiscoverArticleBinding
import com.example.health_assistant.databinding.ItemDiscoverNewsBinding
import com.example.health_assistant.databinding.ItemDiscoverVideoBinding
import com.example.health_assistant.features.discover.domain.model.DiscoverContent
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

/**
 * RecyclerView adapter for displaying mixed content types in the Discover section
 * Supports articles, news, and videos with different view types and search highlighting
 */
class DiscoverContentAdapter(
    private val onArticleClick: (DiscoverContent.Article) -> Unit,
    private val onNewsClick: (DiscoverContent.News) -> Unit,
    private val onVideoClick: (DiscoverContent.Video) -> Unit,
    private val onBookmarkClick: (DiscoverContent) -> Unit,
    private val onShareClick: (DiscoverContent) -> Unit
) : ListAdapter<DiscoverContent, RecyclerView.ViewHolder>(DiscoverContentDiffCallback()) {

    private var searchQuery: String = ""

    companion object {
        private const val VIEW_TYPE_ARTICLE = 1
        private const val VIEW_TYPE_NEWS = 2
        private const val VIEW_TYPE_VIDEO = 3
    }

    /**
     * Update search query for highlighting search terms
     */
    fun updateSearchQuery(query: String) {
        searchQuery = query
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is DiscoverContent.Article -> VIEW_TYPE_ARTICLE
            is DiscoverContent.News -> VIEW_TYPE_NEWS
            is DiscoverContent.Video -> VIEW_TYPE_VIDEO
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_ARTICLE -> {
                val binding = ItemDiscoverArticleBinding.inflate(inflater, parent, false)
                ArticleViewHolder(binding)
            }
            VIEW_TYPE_NEWS -> {
                val binding = ItemDiscoverNewsBinding.inflate(inflater, parent, false)
                NewsViewHolder(binding)
            }
            VIEW_TYPE_VIDEO -> {
                val binding = ItemDiscoverVideoBinding.inflate(inflater, parent, false)
                VideoViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ArticleViewHolder -> holder.bind(getItem(position) as DiscoverContent.Article)
            is NewsViewHolder -> holder.bind(getItem(position) as DiscoverContent.News)
            is VideoViewHolder -> holder.bind(getItem(position) as DiscoverContent.Video)
        }
    }

    /**
     * ViewHolder for health articles
     */
    inner class ArticleViewHolder(
        private val binding: ItemDiscoverArticleBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(article: DiscoverContent.Article) {
            binding.apply {
                // Set article content with search highlighting
                textArticleTitle.text = if (searchQuery.isNotBlank()) {
                    DiscoverContentUtils.highlightSearchTerms(article.title, searchQuery, root.context)
                } else {
                    article.title
                }
                
                textArticleSummary.text = if (searchQuery.isNotBlank()) {
                    val snippet = DiscoverContentUtils.extractSearchSnippet(article.summary, searchQuery, 120)
                    DiscoverContentUtils.highlightSearchTerms(snippet, searchQuery, root.context)
                } else {
                    article.summary
                }
                
                textArticleAuthor.text = article.authorName
                textReadingTime.text = "${article.readingTimeMinutes} min read"
                chipCategory.text = DiscoverContentUtils.formatCategory(article.category)

                // Set credibility score
                textCredibilityScore.text = when {
                    article.credibilityScore >= 4 -> "Verified"
                    article.credibilityScore >= 3 -> "Credible"
                    else -> "Unverified"
                }

                // Set reading progress
                progressReading.progress = (article.readProgress * 100).roundToInt()
                progressReading.visibility = if (article.readProgress > 0) {
                    android.view.View.VISIBLE
                } else {
                    android.view.View.GONE
                }

                // Set bookmark state
                buttonBookmark.setImageResource(
                    if (article.isBookmarked) R.drawable.ic_bookmark_filled 
                    else R.drawable.ic_bookmark
                )

                // Load article image
                article.imageUrl?.let { imageUrl ->
                    imageArticle.load(imageUrl)
                } ?: run {
                    imageArticle.setImageResource(R.drawable.card_light_gray)
                }

                // Set click listeners
                root.setOnClickListener { onArticleClick(article) }
                buttonBookmark.setOnClickListener { onBookmarkClick(article) }
                buttonShare.setOnClickListener { onShareClick(article) }
            }
        }
    }

    /**
     * ViewHolder for health news
     */
    inner class NewsViewHolder(
        private val binding: ItemDiscoverNewsBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(news: DiscoverContent.News) {
            binding.apply {
                // Set news content with search highlighting
                textNewsHeadline.text = if (searchQuery.isNotBlank()) {
                    DiscoverContentUtils.highlightSearchTerms(news.title, searchQuery, root.context)
                } else {
                    news.title
                }
                
                textNewsSummary.text = if (searchQuery.isNotBlank()) {
                    val snippet = DiscoverContentUtils.extractSearchSnippet(news.summary, searchQuery, 100)
                    DiscoverContentUtils.highlightSearchTerms(snippet, searchQuery, root.context)
                } else {
                    news.summary
                }
                
                textNewsSource.text = news.sourcePublication
                textNewsDate.text = formatTimeAgo(news.publishedDate)
                chipNewsCategory.text = DiscoverContentUtils.formatCategory(news.category)

                // Set breaking news badge
                chipBreakingNews.visibility = if (news.isBreakingNews) {
                    android.view.View.VISIBLE
                } else {
                    android.view.View.GONE
                }

                // Set credibility
                textNewsCredibility.text = when (news.sourceCredibility) {
                    "peer-reviewed" -> "Peer-Reviewed"
                    "medical-journal" -> "Medical Journal"
                    "health-organization" -> "Health Org"
                    else -> "News Source"
                }

                // Load news image
                news.imageUrl?.let { imageUrl ->
                    imageNews.load(imageUrl)
                } ?: run {
                    imageNews.setImageResource(R.drawable.card_light_gray)
                }

                // Set click listeners
                root.setOnClickListener { onNewsClick(news) }
                buttonNewsShare.setOnClickListener { onShareClick(news) }
            }
        }
    }

    /**
     * ViewHolder for educational videos
     */
    inner class VideoViewHolder(
        private val binding: ItemDiscoverVideoBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(video: DiscoverContent.Video) {
            binding.apply {
                // Set video content with search highlighting
                textVideoTitle.text = if (searchQuery.isNotBlank()) {
                    DiscoverContentUtils.highlightSearchTerms(video.title, searchQuery, root.context)
                } else {
                    video.title
                }
                
                textVideoDescription.text = if (searchQuery.isNotBlank()) {
                    val snippet = DiscoverContentUtils.extractSearchSnippet(video.description, searchQuery, 100)
                    DiscoverContentUtils.highlightSearchTerms(snippet, searchQuery, root.context)
                } else {
                    video.description
                }
                
                textVideoExpert.text = "${video.expertName}, ${video.expertCredentials}"
                textVideoDifficulty.text = DiscoverContentUtils.formatCategory(video.difficultyLevel)
                chipVideoCategory.text = DiscoverContentUtils.formatCategory(video.category)

                // Set video duration
                textVideoDuration.text = formatDuration(video.durationSeconds)

                // Set watch progress
                if (video.watchProgress > 0) {
                    progressWatching.visibility = android.view.View.VISIBLE
                    textWatchProgress.visibility = android.view.View.VISIBLE
                    progressWatching.progress = (video.watchProgress * 100).roundToInt()
                    textWatchProgress.text = "Watched ${(video.watchProgress * 100).roundToInt()}%"
                } else {
                    progressWatching.visibility = android.view.View.GONE
                    textWatchProgress.visibility = android.view.View.GONE
                }

                // Set offline indicator
                imageOfflineIndicator.visibility = if (video.isDownloadedOffline) {
                    android.view.View.VISIBLE
                } else {
                    android.view.View.GONE
                }

                // Load video thumbnail
                imageVideoThumbnail.load(video.thumbnailUrl)

                // Set click listeners
                root.setOnClickListener { onVideoClick(video) }
                imagePlayButton.setOnClickListener { onVideoClick(video) }
                buttonVideoShare.setOnClickListener { onShareClick(video) }
            }
        }
    }

    /**
     * Format duration from seconds to MM:SS format
     */
    private fun formatDuration(seconds: Int): String {
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return String.format("%d:%02d", minutes, remainingSeconds)
    }

    /**
     * Format timestamp to relative time (e.g., "2 hours ago")
     */
    private fun formatTimeAgo(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        
        return when {
            diff < 60_000 -> "Just now"
            diff < 3600_000 -> "${diff / 60_000} min ago"
            diff < 86400_000 -> "${diff / 3600_000} hours ago"
            diff < 604800_000 -> "${diff / 86400_000} days ago"
            else -> {
                val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
                dateFormat.format(Date(timestamp))
            }
        }
    }
}

/**
 * DiffUtil callback for efficient list updates and smooth animations
 */
class DiscoverContentDiffCallback : DiffUtil.ItemCallback<DiscoverContent>() {
    
    override fun areItemsTheSame(oldItem: DiscoverContent, newItem: DiscoverContent): Boolean {
        return oldItem.id == newItem.id && oldItem.getContentType() == newItem.getContentType()
    }

    override fun areContentsTheSame(oldItem: DiscoverContent, newItem: DiscoverContent): Boolean {
        return when {
            oldItem is DiscoverContent.Article && newItem is DiscoverContent.Article -> {
                oldItem.title == newItem.title &&
                oldItem.summary == newItem.summary &&
                oldItem.isBookmarked == newItem.isBookmarked &&
                oldItem.readProgress == newItem.readProgress &&
                oldItem.credibilityScore == newItem.credibilityScore
            }
            oldItem is DiscoverContent.News && newItem is DiscoverContent.News -> {
                oldItem.title == newItem.title &&
                oldItem.summary == newItem.summary &&
                oldItem.isBreakingNews == newItem.isBreakingNews &&
                oldItem.sourcePublication == newItem.sourcePublication
            }
            oldItem is DiscoverContent.Video && newItem is DiscoverContent.Video -> {
                oldItem.title == newItem.title &&
                oldItem.description == newItem.description &&
                oldItem.watchProgress == newItem.watchProgress &&
                oldItem.isDownloadedOffline == newItem.isDownloadedOffline
            }
            else -> false
        }
    }

    override fun getChangePayload(oldItem: DiscoverContent, newItem: DiscoverContent): Any? {
        // Return specific payloads for partial updates if needed
        val changes = mutableListOf<String>()
        
        when {
            oldItem is DiscoverContent.Article && newItem is DiscoverContent.Article -> {
                if (oldItem.isBookmarked != newItem.isBookmarked) changes.add("bookmark")
                if (oldItem.readProgress != newItem.readProgress) changes.add("progress")
            }
            oldItem is DiscoverContent.Video && newItem is DiscoverContent.Video -> {
                if (oldItem.watchProgress != newItem.watchProgress) changes.add("progress")
                if (oldItem.isDownloadedOffline != newItem.isDownloadedOffline) changes.add("offline")
            }
        }
        
        return if (changes.isNotEmpty()) changes else null
    }
}