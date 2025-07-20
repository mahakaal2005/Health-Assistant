package com.example.health_assistant.features.discover.data.remote

import android.util.Log
import com.example.health_assistant.core.util.Result
import com.example.health_assistant.features.discover.data.remote.api.*
import com.example.health_assistant.features.discover.data.remote.dto.*
import com.example.health_assistant.features.discover.domain.model.DiscoverContent
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Remote data source that aggregates health content from multiple free APIs
 */
@Singleton
class HealthContentRemoteDataSource @Inject constructor(
    private val newsApiService: NewsApiService,
    private val guardianApiService: GuardianApiService,
    private val youtubeApiService: YouTubeApiService,
    private val apiKeyManager: ApiKeyManager
) {
    
    companion object {
        private const val TAG = "HealthContentRemote"
    }
    
    /**
     * Fetch health articles from multiple sources
     */
    suspend fun fetchHealthArticles(
        category: String? = null,
        limit: Int = 20
    ): Result<List<DiscoverContent.Article>> = coroutineScope {
        try {
            Log.d(TAG, "Fetching health articles for category: $category")
            Log.d(TAG, "API Keys configured: ${apiKeyManager.areKeysConfigured()}")
            
            // Fetch from multiple sources concurrently
            val newsApiDeferred = async { fetchFromNewsApi(category, limit / 2) }
            val guardianDeferred = async { fetchFromGuardian(category, limit / 2) }
            
            val results = awaitAll(newsApiDeferred, guardianDeferred)
            val allArticles = results.flatMap { result ->
                when (result) {
                    is Result.Success -> result.data
                    is Result.Error -> {
                        Log.w(TAG, "Source failed: ${result.exception?.message}")
                        emptyList()
                    }
                    is Result.Loading -> emptyList()
                }
            }
            
            // Sort by published date and limit
            val sortedArticles = allArticles
                .sortedByDescending { it.publishedDate }
                .take(limit)
            
            Log.d(TAG, "Successfully fetched ${sortedArticles.size} articles")
            Result.Success(sortedArticles)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch health articles", e)
            Result.Error(e)
        }
    }
    
    /**
     * Fetch health news from news APIs
     */
    suspend fun fetchHealthNews(
        category: String? = null,
        limit: Int = 10
    ): Result<List<DiscoverContent.News>> = coroutineScope {
        try {
            Log.d(TAG, "Fetching health news for category: $category")
            
            val newsDeferred = async { fetchNewsFromNewsApi(category, limit) }
            val guardianNewsDeferred = async { fetchNewsFromGuardian(category, limit / 2) }
            
            val results = awaitAll(newsDeferred, guardianNewsDeferred)
            val allNews = results.flatMap { result ->
                when (result) {
                    is Result.Success -> result.data
                    is Result.Error -> {
                        Log.w(TAG, "News source failed: ${result.exception?.message}")
                        emptyList()
                    }
                    is Result.Loading -> emptyList()
                }
            }
            
            val sortedNews = allNews
                .sortedByDescending { it.publishedDate }
                .take(limit)
            
            Log.d(TAG, "Successfully fetched ${sortedNews.size} news items")
            Result.Success(sortedNews)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch health news", e)
            Result.Error(e)
        }
    }
    
    /**
     * Fetch educational videos from YouTube
     */
    suspend fun fetchEducationalVideos(
        category: String? = null,
        limit: Int = 15
    ): Result<List<DiscoverContent.Video>> = coroutineScope {
        try {
            Log.d(TAG, "Fetching educational videos for category: $category")
            
            val searchQuery = buildVideoSearchQuery(category)
            val response = youtubeApiService.searchHealthVideos(
                query = searchQuery,
                maxResults = limit,
                apiKey = apiKeyManager.youtubeApiKey
            )
            
            if (response.isSuccessful) {
                val videos = response.body()?.items?.mapNotNull { youtubeVideo ->
                    convertYouTubeVideoToDiscoverContent(youtubeVideo, category)
                } ?: emptyList()
                
                Log.d(TAG, "Successfully fetched ${videos.size} videos")
                Result.Success(videos)
            } else {
                Log.e(TAG, "YouTube API error: ${response.code()}")
                Result.Error(Exception("YouTube API error: ${response.code()}"))
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch educational videos", e)
            Result.Error(e)
        }
    }
    
    // Private helper methods for each API
    
    private suspend fun fetchFromNewsApi(
        category: String?,
        limit: Int
    ): Result<List<DiscoverContent.Article>> {
        return try {
            Log.d(TAG, "Fetching from News API...")
            val query = buildNewsQuery(category)
            val response = newsApiService.searchHealthNews(
                query = query,
                pageSize = limit,
                apiKey = apiKeyManager.newsApiKey
            )
            
            Log.d(TAG, "News API response code: ${response.code()}")
            
            if (response.isSuccessful) {
                val articles = response.body()?.articles?.mapNotNull { newsArticle ->
                    convertNewsApiArticleToDiscoverContent(newsArticle, category)
                } ?: emptyList()
                
                Result.Success(articles)
            } else {
                Result.Error(Exception("News API error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    private suspend fun fetchFromGuardian(
        category: String?,
        limit: Int
    ): Result<List<DiscoverContent.Article>> {
        return try {
            val section = mapCategoryToGuardianSection(category)
            val guardianConfig = apiKeyManager.getGuardianApiConfig()
            val response = guardianApiService.searchHealthArticles(
                section = section,
                pageSize = limit,
                showFields = guardianConfig.showFields,
                apiKey = guardianConfig.apiKey
            )
            
            if (response.isSuccessful) {
                val articles = response.body()?.response?.results?.mapNotNull { guardianArticle ->
                    convertGuardianArticleToDiscoverContent(guardianArticle, category)
                } ?: emptyList()
                
                Result.Success(articles)
            } else {
                Result.Error(Exception("Guardian API error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    

    
    private suspend fun fetchNewsFromNewsApi(
        category: String?,
        limit: Int
    ): Result<List<DiscoverContent.News>> {
        return try {
            val response = newsApiService.getHealthTopHeadlines(
                pageSize = limit,
                apiKey = apiKeyManager.newsApiKey
            )
            
            if (response.isSuccessful) {
                val news = response.body()?.articles?.mapNotNull { newsArticle ->
                    convertNewsApiArticleToNews(newsArticle, category)
                } ?: emptyList()
                
                Result.Success(news)
            } else {
                Result.Error(Exception("News API error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    private suspend fun fetchNewsFromGuardian(
        category: String?,
        limit: Int
    ): Result<List<DiscoverContent.News>> {
        return try {
            val guardianConfig = apiKeyManager.getGuardianApiConfig()
            val response = guardianApiService.searchHealthArticles(
                pageSize = limit,
                showFields = guardianConfig.showFields,
                apiKey = guardianConfig.apiKey
            )
            
            if (response.isSuccessful) {
                val news = response.body()?.response?.results?.mapNotNull { guardianArticle ->
                    convertGuardianArticleToNews(guardianArticle, category)
                } ?: emptyList()
                
                Result.Success(news)
            } else {
                Result.Error(Exception("Guardian API error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    // Conversion methods
    
    private suspend fun convertNewsApiArticleToDiscoverContent(
        article: NewsApiArticle,
        category: String?
    ): DiscoverContent.Article? {
        return try {
            val publishedDate = parseDate(article.publishedAt)
            val content = article.content ?: article.description ?: ""
            
            // Use simple content processing
            val aiCategory = category ?: "general"
            val summary = article.description ?: content.take(200) + "..."
            val tags = extractSimpleTags(article.title, content)
            val readingTimeMinutes = estimateReadingTime(content)
            val credibilityScore = 4 // Default credibility score
            
            DiscoverContent.Article(
                id = "news_${article.url.hashCode()}",
                title = article.title,
                publishedDate = publishedDate,
                category = aiCategory,
                imageUrl = article.urlToImage,
                userId = "system",
                summary = summary,
                content = content,
                authorName = article.author ?: "Staff Writer",
                authorCredentials = "Journalist",
                sourceUrl = article.url,
                lastUpdated = publishedDate,
                readingTimeMinutes = readingTimeMinutes,
                tags = tags,
                isBookmarked = false,
                readProgress = 0f,
                credibilityScore = credibilityScore
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to convert NewsAPI article: ${e.message}")
            null
        }
    }
    
    private suspend fun convertYouTubeVideoToDiscoverContent(
        video: YouTubeVideo,
        category: String?
    ): DiscoverContent.Video? {
        return try {
            val publishedDate = parseDate(video.snippet.publishedAt)
            val aiCategory = category ?: "general"
            val tags = extractSimpleTags(video.snippet.title, video.snippet.description)
            
            DiscoverContent.Video(
                id = "youtube_${video.id.videoId}",
                title = video.snippet.title,
                publishedDate = publishedDate,
                category = aiCategory,
                imageUrl = video.snippet.thumbnails.high?.url ?: video.snippet.thumbnails.medium?.url,
                userId = "system",
                description = video.snippet.description,
                thumbnailUrl = video.snippet.thumbnails.high?.url ?: video.snippet.thumbnails.medium?.url ?: "",
                videoUrl = "https://www.youtube.com/watch?v=${video.id.videoId}",
                durationSeconds = 600, // Default, would need additional API call to get exact duration
                difficultyLevel = "intermediate",
                expertName = video.snippet.channelTitle,
                expertCredentials = "Content Creator",
                watchProgress = 0f,
                isDownloadedOffline = false,
                transcriptAvailable = false
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to convert YouTube video: ${e.message}")
            null
        }
    }
    
    // Helper methods
    
    private fun buildNewsQuery(category: String?): String {
        val baseTerms = listOf("health", "medical", "wellness")
        val categoryTerms = when (category) {
            "nutrition" -> listOf("nutrition", "diet", "food", "eating")
            "fitness" -> listOf("fitness", "exercise", "workout", "physical activity")
            "mental-health" -> listOf("mental health", "psychology", "stress", "anxiety")
            "preventive-care" -> listOf("prevention", "screening", "vaccine", "checkup")
            else -> emptyList()
        }
        
        return (baseTerms + categoryTerms).joinToString(" OR ")
    }
    
    private fun buildVideoSearchQuery(category: String?): String {
        val baseQuery = "health education medical"
        return when (category) {
            "nutrition" -> "$baseQuery nutrition diet healthy eating"
            "fitness" -> "$baseQuery fitness exercise workout"
            "mental-health" -> "$baseQuery mental health psychology wellness"
            "preventive-care" -> "$baseQuery prevention screening health tips"
            else -> "$baseQuery wellness tips"
        }
    }
    
    private fun mapCategoryToGuardianSection(category: String?): String {
        return when (category) {
            "mental-health" -> "society/mental-health"
            "nutrition", "fitness" -> "lifeandstyle/health-and-wellbeing"
            else -> "society/health"
        }
    }
    
    private fun parseDate(dateString: String): Long {
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
            format.timeZone = TimeZone.getTimeZone("UTC")
            format.parse(dateString)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }
    
    // Helper methods for content processing
    private fun extractSimpleTags(title: String, content: String): List<String> {
        val healthKeywords = listOf(
            "health", "medical", "nutrition", "fitness", "exercise", "diet", "wellness",
            "mental health", "stress", "sleep", "heart", "diabetes", "prevention",
            "vitamin", "protein", "cardio", "yoga", "meditation", "therapy"
        )
        
        val text = "$title $content".lowercase()
        return healthKeywords.filter { keyword ->
            text.contains(keyword)
        }.take(5)
    }
    
    private fun estimateReadingTime(content: String): Int {
        val wordsPerMinute = 200
        val wordCount = content.split("\\s+".toRegex()).size
        return maxOf(1, wordCount / wordsPerMinute)
    }
    
    // Additional conversion methods would go here...
    private suspend fun convertNewsApiArticleToNews(article: NewsApiArticle, category: String?): DiscoverContent.News? {
        return try {
            val publishedDate = parseDate(article.publishedAt)
            val content = article.content ?: article.description ?: ""
            
            // Use simple content processing
            val aiCategory = category ?: "general"
            val summary = article.description ?: content.take(100) + "..."
            
            DiscoverContent.News(
                id = "newsapi_${article.url.hashCode()}",
                title = article.title,
                publishedDate = publishedDate,
                category = aiCategory,
                imageUrl = article.urlToImage,
                userId = "system",
                summary = summary,
                fullContent = content,
                sourcePublication = article.source.name,
                sourceCredibility = "Medium",
                externalUrl = article.url,
                isBreakingNews = false,
                relevanceScore = 3
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to convert NewsAPI article to news: ${e.message}")
            null
        }
    }
    
    private suspend fun convertGuardianArticleToDiscoverContent(article: GuardianArticle, category: String?): DiscoverContent.Article? {
        return try {
            val fields = article.fields ?: return null
            
            // Extract content from fields
            val title = fields.headline ?: article.webTitle
            val content = fields.body ?: fields.main ?: ""
            val imageUrl = fields.thumbnail
            val author = fields.byline ?: "Guardian Staff"
            val publishedDate = article.webPublicationDate?.let { parseDate(it) } ?: System.currentTimeMillis()
            
            // Use simple content processing
            val aiCategory = category ?: "general"
            val summary = fields.trailText ?: fields.standfirst ?: content.take(200) + "..."
            val tags = extractSimpleTags(title, content)
            val readingTimeMinutes = estimateReadingTime(content)
            val credibilityScore = 4 // Guardian is a credible source
            
            DiscoverContent.Article(
                id = "guardian_${article.id}",
                title = title,
                publishedDate = publishedDate,
                category = aiCategory,
                imageUrl = imageUrl,
                userId = "system",
                summary = summary,
                content = content,
                authorName = author,
                authorCredentials = "Guardian Journalist",
                sourceUrl = article.webUrl ?: "",
                lastUpdated = publishedDate,
                readingTimeMinutes = readingTimeMinutes,
                tags = tags,
                isBookmarked = false,
                readProgress = 0f,
                credibilityScore = credibilityScore
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error converting Guardian article: ${e.message}")
            null
        }
    }
    
    private suspend fun convertGuardianArticleToNews(article: GuardianArticle, category: String?): DiscoverContent.News? {
        return try {
            val fields = article.fields ?: return null
            
            // Extract content from fields
            val title = fields.headline ?: article.webTitle
            val content = fields.body ?: fields.main ?: ""
            val imageUrl = fields.thumbnail
            val author = fields.byline ?: "Guardian Staff"
            val publishedDate = article.webPublicationDate?.let { parseDate(it) } ?: System.currentTimeMillis()
            
            // Use simple content processing
            val aiCategory = category ?: "general"
            val summary = fields.trailText ?: fields.standfirst ?: content.take(100) + "..."
            
            DiscoverContent.News(
                id = "guardian_news_${article.id}",
                title = title,
                publishedDate = publishedDate,
                category = aiCategory,
                imageUrl = imageUrl,
                userId = "system",
                summary = summary,
                fullContent = content,
                sourcePublication = "The Guardian",
                sourceCredibility = "High",
                externalUrl = article.webUrl ?: "",
                isBreakingNews = false,
                relevanceScore = 4
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error converting Guardian news: ${e.message}")
            null
        }
    }
}