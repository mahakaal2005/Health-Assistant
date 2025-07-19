package com.example.health_assistant.features.discover.presentation

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.example.health_assistant.R
import com.example.health_assistant.databinding.ItemBookmarkArticleBinding
import com.example.health_assistant.databinding.ItemBookmarkNewsBinding
import com.example.health_assistant.databinding.ItemBookmarkVideoBinding
import com.example.health_assistant.features.discover.domain.model.DiscoverContent
import java.text.SimpleDateFormat
import java.util.*

/**
 * RecyclerView adapter for displaying bookmarked content
 * Supports different view types for articles, news, and videos
 */
class BookmarksAdapter(
    private val onItemClick: (DiscoverContent) -> Unit,
    private val onBookmarkClick: (DiscoverContent) -> Unit,
    private val onShareClick: (DiscoverContent) -> Unit
) : ListAdapter<DiscoverContent, RecyclerView.ViewHolder>(BookmarkDiffCallback()) {

    private var readingHistory: Map<String, BookmarksViewModel.ReadingHistoryItem> = emptyMap()

    companion object {
        private const val VIEW_TYPE_ARTICLE = 0
        private const val VIEW_TYPE_NEWS = 1
        private const val VIEW_TYPE_VIDEO = 2
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
                val binding = ItemBookmarkArticleBinding.inflate(inflater, parent, false)
                ArticleViewHolder(binding)
            }
            VIEW_TYPE_NEWS -> {
                val binding = ItemBookmarkNewsBinding.inflate(inflater, parent, false)
                NewsViewHolder(binding)
            }
            VIEW_TYPE_VIDEO -> {
                val binding = ItemBookmarkVideoBinding.inflate(inflater, parent, false)
                VideoViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        val historyItem = readingHistory[item.id]
        
        when (holder) {
            is ArticleViewHolder -> holder.bind(item as DiscoverContent.Article, historyItem)
            is NewsViewHolder -> holder.bind(item as DiscoverContent.News, historyItem)
            is VideoViewHolder -> holder.bind(item as DiscoverContent.Video, historyItem)
        }
    }

    fun updateReadingHistory(history: Map<String, BookmarksViewModel.ReadingHistoryItem>) {
        readingHistory = history
        notifyDataSetChanged()
    }

    inner class ArticleViewHolder(
        private val binding: ItemBookmarkArticleBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(article: DiscoverContent.Article, historyItem: BookmarksViewModel.ReadingHistoryItem?) {
            binding.apply {
                titleText.text = article.title
                summaryText.text = article.summary
                authorText.text = article.authorName
                categoryText.text = article.category
                readingTimeText.text = root.context.getString(
                    R.string.reading_time_minutes,
                    article.readingTimeMinutes
                )
                
                // Format published date
                val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                publishedDateText.text = dateFormat.format(Date(article.publishedDate))
                
                // Load article image
                if (!article.imageUrl.isNullOrEmpty()) {
                    articleImageView.visibility = View.VISIBLE
                    Glide.with(root.context)
                        .load(article.imageUrl)
                        .transform(RoundedCorners(16))
                        .placeholder(R.drawable.ic_medical_category)
                        .error(R.drawable.ic_medical_category)
                        .into(articleImageView)
                } else {
                    articleImageView.visibility = View.GONE
                }
                
                // Show credibility score
                credibilityScoreText.text = root.context.getString(
                    R.string.credibility_score,
                    article.credibilityScore
                )
                
                // Show reading progress if available
                if (historyItem != null) {
                    readingProgressBar.visibility = View.VISIBLE
                    readingProgressText.visibility = View.VISIBLE
                    readingProgressBar.progress = (historyItem.progress * 100).toInt()
                    readingProgressText.text = root.context.getString(
                        R.string.reading_progress_percent,
                        (historyItem.progress * 100).toInt()
                    )
                    
                    if (historyItem.isCompleted) {
                        completedIndicator.visibility = View.VISIBLE
                    } else {
                        completedIndicator.visibility = View.GONE
                    }
                } else {
                    readingProgressBar.visibility = View.GONE
                    readingProgressText.visibility = View.GONE
                    completedIndicator.visibility = View.GONE
                }
                
                // Set click listeners
                root.setOnClickListener { onItemClick(article) }
                bookmarkButton.setOnClickListener { onBookmarkClick(article) }
                shareButton.setOnClickListener { onShareClick(article) }
                
                // Bookmark button is always filled since this is bookmarks list
                bookmarkButton.setImageResource(R.drawable.ic_bookmark_filled)
            }
        }
    }

    inner class NewsViewHolder(
        private val binding: ItemBookmarkNewsBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(news: DiscoverContent.News, historyItem: BookmarksViewModel.ReadingHistoryItem?) {
            binding.apply {
                headlineText.text = news.title
                summaryText.text = news.summary
                sourceText.text = news.sourcePublication
                categoryText.text = news.category
                
                // Format published date
                val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                publishedDateText.text = dateFormat.format(Date(news.publishedDate))
                
                // Load news image
                if (!news.imageUrl.isNullOrEmpty()) {
                    newsImageView.visibility = View.VISIBLE
                    Glide.with(root.context)
                        .load(news.imageUrl)
                        .transform(RoundedCorners(16))
                        .placeholder(R.drawable.ic_medical_category)
                        .error(R.drawable.ic_medical_category)
                        .into(newsImageView)
                } else {
                    newsImageView.visibility = View.GONE
                }
                
                // Show breaking news indicator
                if (news.isBreakingNews) {
                    breakingNewsIndicator.visibility = View.VISIBLE
                } else {
                    breakingNewsIndicator.visibility = View.GONE
                }
                
                // Show source credibility
                sourceCredibilityText.text = news.sourceCredibility
                
                // Set click listeners
                root.setOnClickListener { onItemClick(news) }
                bookmarkButton.setOnClickListener { onBookmarkClick(news) }
                shareButton.setOnClickListener { onShareClick(news) }
                
                // Bookmark button is always filled since this is bookmarks list
                bookmarkButton.setImageResource(R.drawable.ic_bookmark_filled)
            }
        }
    }

    inner class VideoViewHolder(
        private val binding: ItemBookmarkVideoBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(video: DiscoverContent.Video, historyItem: BookmarksViewModel.ReadingHistoryItem?) {
            binding.apply {
                titleText.text = video.title
                descriptionText.text = video.description
                expertText.text = video.expertName
                categoryText.text = video.category
                difficultyText.text = video.difficultyLevel
                
                // Format duration
                val minutes = video.durationSeconds / 60
                val seconds = video.durationSeconds % 60
                durationText.text = root.context.getString(
                    R.string.video_duration_format,
                    minutes,
                    seconds
                )
                
                // Format published date
                val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                publishedDateText.text = dateFormat.format(Date(video.publishedDate))
                
                // Load video thumbnail
                Glide.with(root.context)
                    .load(video.thumbnailUrl)
                    .transform(RoundedCorners(16))
                    .placeholder(R.drawable.ic_play_arrow)
                    .error(R.drawable.ic_play_arrow)
                    .into(thumbnailImageView)
                
                // Show watch progress if available
                if (historyItem != null) {
                    watchProgressBar.visibility = View.VISIBLE
                    watchProgressText.visibility = View.VISIBLE
                    watchProgressBar.progress = (historyItem.progress * 100).toInt()
                    watchProgressText.text = root.context.getString(
                        R.string.watch_progress_percent,
                        (historyItem.progress * 100).toInt()
                    )
                    
                    if (historyItem.isCompleted) {
                        completedIndicator.visibility = View.VISIBLE
                    } else {
                        completedIndicator.visibility = View.GONE
                    }
                } else {
                    watchProgressBar.visibility = View.GONE
                    watchProgressText.visibility = View.GONE
                    completedIndicator.visibility = View.GONE
                }
                
                // Show offline indicator if downloaded
                if (video.isDownloadedOffline) {
                    offlineIndicator.visibility = View.VISIBLE
                } else {
                    offlineIndicator.visibility = View.GONE
                }
                
                // Set click listeners
                root.setOnClickListener { onItemClick(video) }
                bookmarkButton.setOnClickListener { onBookmarkClick(video) }
                shareButton.setOnClickListener { onShareClick(video) }
                
                // Bookmark button is always filled since this is bookmarks list
                bookmarkButton.setImageResource(R.drawable.ic_bookmark_filled)
            }
        }
    }
}

class BookmarkDiffCallback : DiffUtil.ItemCallback<DiscoverContent>() {
    override fun areItemsTheSame(oldItem: DiscoverContent, newItem: DiscoverContent): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: DiscoverContent, newItem: DiscoverContent): Boolean {
        return oldItem == newItem
    }
}