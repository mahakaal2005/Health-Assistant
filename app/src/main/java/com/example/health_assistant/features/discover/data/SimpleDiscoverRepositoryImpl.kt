package com.example.health_assistant.features.discover.data

import android.util.Log
import com.example.health_assistant.core.util.Result
import com.example.health_assistant.features.discover.domain.model.DiscoverSections
import com.example.health_assistant.features.discover.domain.model.HealthContent
import com.example.health_assistant.features.discover.domain.model.ContentType
import com.example.health_assistant.features.discover.domain.repository.DiscoverRepository
import com.example.health_assistant.features.discover.data.remote.HealthContentRemoteDataSource
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Simple repository implementation that fetches content from APIs
 * and converts to simple HealthContent objects
 */
@Singleton
class SimpleDiscoverRepositoryImpl @Inject constructor(
    private val remoteDataSource: HealthContentRemoteDataSource
) : DiscoverRepository {

    companion object {
        private const val TAG = "SimpleDiscoverRepo"
    }

    override suspend fun getDiscoverContent(): Result<DiscoverSections> {
        return try {
            Log.d(TAG, "Fetching discover content from APIs")
            
            coroutineScope {
                // Fetch all content types concurrently
                val articlesDeferred = async { fetchArticles() }
                val newsDeferred = async { fetchNews() }
                val videosDeferred = async { fetchVideos() }
                
                val articles = articlesDeferred.await()
                val news = newsDeferred.await()
                val videos = videosDeferred.await()
                
                val sections = DiscoverSections(
                    articles = articles,
                    news = news,
                    videos = videos
                )
                
                Log.d(TAG, "Successfully fetched content: ${articles.size} articles, ${news.size} news, ${videos.size} videos")
                Result.Success(sections)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch discover content", e)
            Result.Error(e)
        }
    }

    override suspend fun refreshContent(): Result<DiscoverSections> {
        Log.d(TAG, "Refreshing discover content")
        return getDiscoverContent()
    }

    private suspend fun fetchArticles(): List<HealthContent> {
        return try {
            when (val result = remoteDataSource.fetchHealthArticles(limit = 10)) {
                is Result.Success -> {
                    result.data.map { article ->
                        HealthContent(
                            id = article.id,
                            title = article.title,
                            description = article.summary,
                            imageUrl = article.imageUrl,
                            sourceUrl = article.sourceUrl,
                            publishedDate = formatDate(article.publishedDate),
                            contentType = ContentType.ARTICLE,
                            sourceName = article.authorName
                        )
                    }
                }
                is Result.Error -> {
                    Log.w(TAG, "Failed to fetch articles: ${result.exception?.message}")
                    emptyList()
                }
                is Result.Loading -> emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching articles", e)
            emptyList()
        }
    }

    private suspend fun fetchNews(): List<HealthContent> {
        return try {
            when (val result = remoteDataSource.fetchHealthNews(limit = 8)) {
                is Result.Success -> {
                    result.data.map { news ->
                        HealthContent(
                            id = news.id,
                            title = news.title,
                            description = news.summary,
                            imageUrl = news.imageUrl,
                            sourceUrl = news.externalUrl,
                            publishedDate = formatDate(news.publishedDate),
                            contentType = ContentType.NEWS,
                            sourceName = news.sourcePublication
                        )
                    }
                }
                is Result.Error -> {
                    Log.w(TAG, "Failed to fetch news: ${result.exception?.message}")
                    emptyList()
                }
                is Result.Loading -> emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching news", e)
            emptyList()
        }
    }

    private suspend fun fetchVideos(): List<HealthContent> {
        return try {
            when (val result = remoteDataSource.fetchEducationalVideos(limit = 6)) {
                is Result.Success -> {
                    result.data.map { video ->
                        HealthContent(
                            id = video.id,
                            title = video.title,
                            description = video.description,
                            imageUrl = video.thumbnailUrl,
                            sourceUrl = video.videoUrl,
                            publishedDate = formatDate(video.publishedDate),
                            contentType = ContentType.VIDEO,
                            sourceName = video.expertName
                        )
                    }
                }
                is Result.Error -> {
                    Log.w(TAG, "Failed to fetch videos: ${result.exception?.message}")
                    emptyList()
                }
                is Result.Loading -> emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching videos", e)
            emptyList()
        }
    }

    private fun formatDate(timestamp: Long): String {
        val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        return formatter.format(Date(timestamp))
    }
}