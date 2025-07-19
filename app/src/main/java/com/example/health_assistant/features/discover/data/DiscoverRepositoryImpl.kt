package com.example.health_assistant.features.discover.data

import com.example.health_assistant.core.util.Result
import com.example.health_assistant.features.discover.domain.model.ContentValidationResult
import com.example.health_assistant.features.discover.domain.model.ContentCredibilityLevel
import com.example.health_assistant.features.discover.domain.model.DiscoverContent
import com.example.health_assistant.features.discover.domain.model.HealthContentCategory
import com.example.health_assistant.features.discover.domain.model.ContentReport
import com.example.health_assistant.features.discover.domain.error.DiscoverError
import com.example.health_assistant.features.discover.domain.error.ErrorMapper
import com.example.health_assistant.features.discover.domain.error.RetryManager
import com.example.health_assistant.features.discover.domain.repository.DiscoverRepository
import com.example.health_assistant.features.discover.domain.repository.CacheStatistics
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.delay
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Minimal implementation of DiscoverRepository for dependency injection verification
 * This is a placeholder implementation to ensure DI setup works correctly
 */
@Singleton
class DiscoverRepositoryImpl @Inject constructor(
    private val discoverDao: DiscoverDao,
    private val firestore: FirebaseFirestore,
    private val errorMapper: ErrorMapper,
    private val retryManager: RetryManager
) : DiscoverRepository {

    // Sample data implementations for immediate functionality
    override fun getHealthArticles(category: String?, limit: Int): Flow<Result<List<DiscoverContent.Article>>> {
        return flow {
            emit(Result.Loading)
            try {
                // Simulate network delay
                kotlinx.coroutines.delay(500)
                
                val sampleArticles = createSampleArticles().let { articles ->
                    if (category != null) {
                        articles.filter { it.category == category }
                    } else {
                        articles
                    }
                }.take(limit)
                
                emit(Result.Success(sampleArticles))
            } catch (e: Exception) {
                emit(Result.Error(e))
            }
        }
    }

    override fun getHealthNews(category: String?, limit: Int): Flow<Result<List<DiscoverContent.News>>> {
        return flow {
            emit(Result.Loading)
            try {
                kotlinx.coroutines.delay(300)
                
                val sampleNews = createSampleNews().let { news ->
                    if (category != null) {
                        news.filter { it.category == category }
                    } else {
                        news
                    }
                }.take(limit)
                
                emit(Result.Success(sampleNews))
            } catch (e: Exception) {
                emit(Result.Error(e))
            }
        }
    }

    override fun getEducationalVideos(category: String?, limit: Int): Flow<Result<List<DiscoverContent.Video>>> {
        return flow {
            emit(Result.Loading)
            try {
                kotlinx.coroutines.delay(400)
                
                val sampleVideos = createSampleVideos().let { videos ->
                    if (category != null) {
                        videos.filter { it.category == category }
                    } else {
                        videos
                    }
                }.take(limit)
                
                emit(Result.Success(sampleVideos))
            } catch (e: Exception) {
                emit(Result.Error(e))
            }
        }
    }

    override fun getMixedContentFeed(category: String?, limit: Int): Flow<Result<List<DiscoverContent>>> {
        return flow {
            emit(Result.Loading)
            try {
                kotlinx.coroutines.delay(600)
                
                val allContent = mutableListOf<DiscoverContent>()
                allContent.addAll(createSampleArticles())
                allContent.addAll(createSampleNews())
                allContent.addAll(createSampleVideos())
                
                val filteredContent = if (category != null) {
                    allContent.filter { it.category == category }
                } else {
                    allContent
                }.sortedByDescending { it.publishedDate }.take(limit)
                
                emit(Result.Success(filteredContent))
            } catch (e: Exception) {
                emit(Result.Error(e))
            }
        }
    }

    override fun getTrendingContent(limit: Int): Flow<Result<List<DiscoverContent>>> {
        return flow {
            emit(Result.Loading)
            try {
                delay(400)
                
                // Return a mix of trending content (most recent items)
                val allContent = mutableListOf<DiscoverContent>()
                allContent.addAll(createSampleArticles().take(2))
                allContent.addAll(createSampleNews().take(2))
                allContent.addAll(createSampleVideos().take(2))
                
                val trendingContent = allContent.sortedByDescending { it.publishedDate }.take(limit)
                emit(Result.Success(trendingContent))
            } catch (e: Exception) {
                emit(Result.Error(e))
            }
        }
    }

    override fun getBreakingNews(limit: Int): Flow<Result<List<DiscoverContent.News>>> {
        return flowOf(Result.Success(emptyList()))
    }

    override suspend fun searchContent(query: String, contentTypes: List<String>, limit: Int): Result<List<DiscoverContent>> {
        return Result.Success(emptyList())
    }

    override suspend fun getContentByCategory(category: HealthContentCategory, limit: Int): Result<List<DiscoverContent>> {
        return Result.Success(emptyList())
    }

    override suspend fun getContentByCredibility(minCredibilityScore: Int, limit: Int): Result<List<DiscoverContent>> {
        return Result.Success(emptyList())
    }

    override suspend fun getArticleById(articleId: String): Result<DiscoverContent.Article?> {
        return Result.Success(null)
    }

    override suspend fun getNewsById(newsId: String): Result<DiscoverContent.News?> {
        return Result.Success(null)
    }

    override suspend fun getVideoById(videoId: String): Result<DiscoverContent.Video?> {
        return Result.Success(null)
    }

    override suspend fun toggleBookmark(contentId: String, contentType: String): Result<Boolean> {
        return Result.Success(false)
    }

    override suspend fun addBookmark(contentId: String, contentType: String): Result<Unit> {
        return Result.Success(Unit)
    }

    override suspend fun removeBookmark(contentId: String): Result<Unit> {
        return Result.Success(Unit)
    }

    override suspend fun isContentBookmarked(contentId: String): Result<Boolean> {
        return Result.Success(false)
    }

    override fun getBookmarkedContent(): Flow<Result<List<DiscoverContent>>> {
        return flowOf(Result.Success(emptyList()))
    }

    override fun getBookmarksByType(contentType: String): Flow<Result<List<DiscoverContent>>> {
        return flowOf(Result.Success(emptyList()))
    }

    override suspend fun updateReadingProgress(articleId: String, progress: Float): Result<Unit> {
        return Result.Success(Unit)
    }

    override suspend fun updateWatchProgress(videoId: String, progress: Float): Result<Unit> {
        return Result.Success(Unit)
    }

    override suspend fun syncContentFromRemote(): Result<Unit> {
        return Result.Success(Unit)
    }

    override suspend fun getCachedContent(limit: Int): Result<List<DiscoverContent>> {
        return Result.Success(emptyList())
    }

    override suspend fun downloadVideoForOffline(videoId: String): Result<Unit> {
        return Result.Success(Unit)
    }

    override suspend fun removeOfflineVideo(videoId: String): Result<Unit> {
        return Result.Success(Unit)
    }

    override suspend fun getOfflineVideos(): Result<List<DiscoverContent.Video>> {
        return Result.Success(emptyList())
    }

    override suspend fun isContentAvailableOffline(contentId: String, contentType: String): Boolean {
        return false
    }

    override suspend fun validateContentCredibility(contentId: String, contentType: String): Result<ContentValidationResult> {
        return Result.Success(ContentValidationResult(
            contentId = contentId,
            contentType = contentType,
            isCredible = true,
            credibilityScore = 3,
            credibilityLevel = ContentCredibilityLevel.MEDICAL_EXPERT,
            warnings = emptyList(),
            lastValidated = System.currentTimeMillis()
        ))
    }

    override suspend fun reportContentIssue(contentId: String, contentType: String, issueType: String, description: String): Result<Unit> {
        return try {
            retryManager.executeWithRetry { attempt ->
                Log.d("DiscoverRepo", "Reporting content issue (attempt $attempt): $contentId")
                
                val reportData = mapOf(
                    "contentId" to contentId,
                    "contentType" to contentType,
                    "issueType" to issueType,
                    "description" to description,
                    "timestamp" to System.currentTimeMillis(),
                    "status" to "pending"
                )
                
                firestore.collection("content_reports")
                    .add(reportData)
                    .await()
                
                Log.d("DiscoverRepo", "Content report submitted successfully")
            }.fold(
                onSuccess = { Result.Success(Unit) },
                onFailure = { throwable ->
                    val error = errorMapper.mapThrowableToDiscoverError(throwable)
                    Log.e("DiscoverRepo", "Failed to report content issue", throwable)
                    Result.Error(throwable, error.userMessage)
                }
            )
        } catch (e: Exception) {
            val error = errorMapper.mapThrowableToDiscoverError(e)
            Log.e("DiscoverRepo", "Exception reporting content issue", e)
            Result.Error(e, error.userMessage)
        }
    }

    override suspend fun reportContentIssue(report: ContentReport): Result<Unit> {
        return try {
            retryManager.executeWithRetry { attempt ->
                Log.d("DiscoverRepo", "Submitting content report (attempt $attempt): ${report.id}")
                
                val reportData = mapOf(
                    "contentId" to report.contentId,
                    "contentType" to report.contentType,
                    "reportType" to report.reportType.name,
                    "description" to report.description,
                    "reporterUserId" to report.reporterUserId,
                    "timestamp" to report.timestamp,
                    "status" to report.status.name
                )
                
                firestore.collection("content_reports")
                    .add(reportData)
                    .await()
                
                Log.d("DiscoverRepo", "Content report submitted successfully")
            }.fold(
                onSuccess = { Result.Success(Unit) },
                onFailure = { throwable ->
                    val error = errorMapper.mapThrowableToDiscoverError(throwable)
                    Log.e("DiscoverRepo", "Failed to submit content report", throwable)
                    Result.Error(throwable, error.userMessage)
                }
            )
        } catch (e: Exception) {
            val error = errorMapper.mapThrowableToDiscoverError(e)
            Log.e("DiscoverRepo", "Exception submitting content report", e)
            Result.Error(e, error.userMessage)
        }
    }

    override suspend fun getUserContentReports(userId: String): Result<List<ContentReport>> {
        return try {
            retryManager.executeWithRetry { attempt ->
                Log.d("DiscoverRepo", "Fetching user reports (attempt $attempt): $userId")
                
                val querySnapshot = firestore.collection("content_reports")
                    .whereEqualTo("reporterUserId", userId)
                    .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .get()
                    .await()
                
                val reports = querySnapshot.documents.mapNotNull { document ->
                    try {
                        ContentReport(
                            id = document.id,
                            contentId = document.getString("contentId") ?: "",
                            contentType = document.getString("contentType") ?: "",
                            reportType = com.example.health_assistant.features.discover.domain.model.ContentReportType.valueOf(
                                document.getString("reportType") ?: "OTHER"
                            ),
                            description = document.getString("description") ?: "",
                            reporterUserId = document.getString("reporterUserId") ?: "",
                            timestamp = document.getLong("timestamp") ?: 0L,
                            status = com.example.health_assistant.features.discover.domain.model.ContentReportStatus.valueOf(
                                document.getString("status") ?: "PENDING"
                            )
                        )
                    } catch (e: Exception) {
                        Log.w("DiscoverRepo", "Failed to parse report document: ${document.id}", e)
                        null
                    }
                }
                
                Log.d("DiscoverRepo", "Fetched ${reports.size} user reports")
                reports
            }.fold(
                onSuccess = { Result.Success(it) },
                onFailure = { throwable ->
                    val error = errorMapper.mapThrowableToDiscoverError(throwable)
                    Log.e("DiscoverRepo", "Failed to fetch user reports", throwable)
                    Result.Error(throwable, error.userMessage)
                }
            )
        } catch (e: Exception) {
            val error = errorMapper.mapThrowableToDiscoverError(e)
            Log.e("DiscoverRepo", "Exception fetching user reports", e)
            Result.Error(e, error.userMessage)
        }
    }

    override suspend fun getContentCredibilityInfo(contentId: String, contentType: String): Result<ContentValidationResult> {
        return Result.Success(ContentValidationResult(
            contentId = contentId,
            contentType = contentType,
            isCredible = true,
            credibilityScore = 3,
            credibilityLevel = ContentCredibilityLevel.MEDICAL_EXPERT,
            warnings = emptyList(),
            lastValidated = System.currentTimeMillis()
        ))
    }

    override suspend fun cleanupOldContent(retentionDays: Int): Result<Int> {
        return Result.Success(0)
    }

    override suspend fun cleanupOrphanedBookmarks(): Result<Int> {
        return Result.Success(0)
    }

    override suspend fun getCacheStatistics(): Result<CacheStatistics> {
        return Result.Success(CacheStatistics(
            totalArticles = 0,
            totalNews = 0,
            totalVideos = 0,
            totalBookmarks = 0,
            offlineVideos = 0,
            cacheSize = 0L,
            lastSyncTime = 0L
        ))
    }

    override suspend fun clearUserCache(): Result<Unit> {
        return Result.Success(Unit)
    }

    override suspend fun getUserPreferredCategories(): Result<List<HealthContentCategory>> {
        return Result.Success(emptyList())
    }

    override suspend fun updateUserPreferredCategories(categories: List<HealthContentCategory>): Result<Unit> {
        return Result.Success(Unit)
    }

    override fun getPersonalizedRecommendations(limit: Int): Flow<Result<List<DiscoverContent>>> {
        return flowOf(Result.Success(emptyList()))
    }

    // Sample data creation methods
    private fun createSampleArticles(): List<DiscoverContent.Article> {
        val currentTime = System.currentTimeMillis()
        return listOf(
            DiscoverContent.Article(
                id = "article_1",
                title = "10 Essential Nutrients for Optimal Health",
                publishedDate = currentTime - 86400000, // 1 day ago
                category = "nutrition",
                imageUrl = "https://images.unsplash.com/photo-1490645935967-10de6ba17061?w=400",
                userId = "system",
                summary = "Discover the key nutrients your body needs for optimal health and where to find them in everyday foods.",
                content = "A comprehensive guide to essential nutrients including vitamins, minerals, and macronutrients that support overall health and wellbeing.",
                authorName = "Dr. Sarah Johnson",
                authorCredentials = "MD, Nutritionist",
                sourceUrl = "https://healthjournal.com/nutrients",
                lastUpdated = currentTime - 86400000,
                readingTimeMinutes = 8,
                tags = listOf("nutrition", "vitamins", "health", "diet"),
                isBookmarked = false,
                readProgress = 0f,
                credibilityScore = 5
            ),
            DiscoverContent.Article(
                id = "article_2",
                title = "The Science of Sleep: Why Rest Matters",
                publishedDate = currentTime - 172800000, // 2 days ago
                category = "mental-health",
                imageUrl = "https://images.unsplash.com/photo-1541781774459-bb2af2f05b55?w=400",
                userId = "system",
                summary = "Understanding the importance of quality sleep for mental health, cognitive function, and physical recovery.",
                content = "An in-depth look at sleep cycles, the impact of sleep deprivation, and evidence-based strategies for better sleep hygiene.",
                authorName = "Dr. Michael Chen",
                authorCredentials = "MD, Sleep Specialist",
                sourceUrl = "https://sleepresearch.org/science",
                lastUpdated = currentTime - 172800000,
                readingTimeMinutes = 12,
                tags = listOf("sleep", "mental-health", "recovery", "wellness"),
                isBookmarked = false,
                readProgress = 0f,
                credibilityScore = 5
            ),
            DiscoverContent.Article(
                id = "article_3",
                title = "Heart-Healthy Exercise: A Beginner's Guide",
                publishedDate = currentTime - 259200000, // 3 days ago
                category = "fitness",
                imageUrl = "https://images.unsplash.com/photo-1571019613454-1cb2f99b2d8b?w=400",
                userId = "system",
                summary = "Learn how to start an exercise routine that supports cardiovascular health, even if you're new to fitness.",
                content = "A practical guide to cardiovascular exercise, including safe starting points, progression strategies, and heart rate monitoring.",
                authorName = "Dr. Lisa Rodriguez",
                authorCredentials = "MD, Cardiologist",
                sourceUrl = "https://heartfoundation.org/exercise",
                lastUpdated = currentTime - 259200000,
                readingTimeMinutes = 10,
                tags = listOf("fitness", "cardio", "heart-health", "exercise"),
                isBookmarked = false,
                readProgress = 0f,
                credibilityScore = 4
            ),
            DiscoverContent.Article(
                id = "article_4",
                title = "Managing Stress in Modern Life",
                publishedDate = currentTime - 345600000, // 4 days ago
                category = "mental-health",
                imageUrl = "https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=400",
                userId = "system",
                summary = "Practical strategies for managing stress and maintaining mental wellbeing in today's fast-paced world.",
                content = "Evidence-based techniques for stress management including mindfulness, breathing exercises, and lifestyle modifications.",
                authorName = "Dr. Amanda Thompson",
                authorCredentials = "PhD, Clinical Psychology",
                sourceUrl = "https://mentalwellness.org/stress",
                lastUpdated = currentTime - 345600000,
                readingTimeMinutes = 7,
                tags = listOf("stress", "mental-health", "mindfulness", "wellness"),
                isBookmarked = false,
                readProgress = 0f,
                credibilityScore = 4
            ),
            DiscoverContent.Article(
                id = "article_5",
                title = "Understanding Diabetes Prevention",
                publishedDate = currentTime - 432000000, // 5 days ago
                category = "preventive-care",
                imageUrl = "https://images.unsplash.com/photo-1559757148-5c350d0d3c56?w=400",
                userId = "system",
                summary = "Learn about risk factors for diabetes and evidence-based prevention strategies.",
                content = "A comprehensive overview of Type 2 diabetes prevention, including dietary approaches, exercise recommendations, and lifestyle modifications.",
                authorName = "Dr. Robert Kim",
                authorCredentials = "MD, Endocrinologist",
                sourceUrl = "https://diabetesprevention.org/guide",
                lastUpdated = currentTime - 432000000,
                readingTimeMinutes = 15,
                tags = listOf("diabetes", "prevention", "diet", "health"),
                isBookmarked = false,
                readProgress = 0f,
                credibilityScore = 5
            )
        )
    }

    private fun createSampleNews(): List<DiscoverContent.News> {
        val currentTime = System.currentTimeMillis()
        return listOf(
            DiscoverContent.News(
                id = "news_1",
                title = "New Study Links Mediterranean Diet to Longevity",
                publishedDate = currentTime - 43200000, // 12 hours ago
                category = "nutrition",
                imageUrl = "https://images.unsplash.com/photo-1498837167922-ddd27525d352?w=400",
                userId = "system",
                summary = "Recent research shows significant health benefits of Mediterranean-style eating patterns.",
                fullContent = "A comprehensive 10-year study involving 25,000 participants has demonstrated that adherence to Mediterranean diet patterns significantly increases longevity and reduces risk of chronic diseases.",
                sourcePublication = "Health Research Journal",
                sourceCredibility = "medical-journal",
                externalUrl = "https://healthresearch.com/mediterranean-study",
                isBreakingNews = true,
                relevanceScore = 5
            ),
            DiscoverContent.News(
                id = "news_2",
                title = "WHO Updates Physical Activity Guidelines",
                publishedDate = currentTime - 86400000, // 1 day ago
                category = "fitness",
                imageUrl = "https://images.unsplash.com/photo-1571019613454-1cb2f99b2d8b?w=400",
                userId = "system",
                summary = "World Health Organization releases updated recommendations for physical activity across all age groups.",
                fullContent = "The World Health Organization has released comprehensive updates to global physical activity guidelines, emphasizing the importance of regular movement for all demographics.",
                sourcePublication = "World Health Organization",
                sourceCredibility = "government-health",
                externalUrl = "https://who.int/activity-guidelines",
                isBreakingNews = false,
                relevanceScore = 4
            ),
            DiscoverContent.News(
                id = "news_3",
                title = "Mental Health Apps Show Promise in Clinical Trial",
                publishedDate = currentTime - 172800000, // 2 days ago
                category = "mental-health",
                imageUrl = "https://images.unsplash.com/photo-1559757175-0eb30cd8c063?w=400",
                userId = "system",
                summary = "Digital mental health interventions demonstrate effectiveness in supporting therapy outcomes.",
                fullContent = "A randomized controlled trial involving 500 participants showed that mental health apps can significantly improve treatment outcomes when used alongside traditional therapy.",
                sourcePublication = "Digital Health Today",
                sourceCredibility = "health-tech",
                externalUrl = "https://digitalhealthtoday.com/mental-health-apps",
                isBreakingNews = false,
                relevanceScore = 4
            ),
            DiscoverContent.News(
                id = "news_4",
                title = "Breakthrough in Early Cancer Detection",
                publishedDate = currentTime - 259200000, // 3 days ago
                category = "preventive-care",
                imageUrl = "https://images.unsplash.com/photo-1576091160399-112ba8d25d1f?w=400",
                userId = "system",
                summary = "New screening technology shows improved accuracy in detecting early-stage cancers.",
                fullContent = "Researchers have developed a new blood test that can detect multiple types of cancer in their earliest stages with 95% accuracy, potentially revolutionizing preventive care.",
                sourcePublication = "Medical Advances Quarterly",
                sourceCredibility = "medical-journal",
                externalUrl = "https://medicaladvances.com/cancer-detection",
                isBreakingNews = false,
                relevanceScore = 5
            )
        )
    }

    private fun createSampleVideos(): List<DiscoverContent.Video> {
        val currentTime = System.currentTimeMillis()
        return listOf(
            DiscoverContent.Video(
                id = "video_1",
                title = "5-Minute Morning Yoga Routine",
                publishedDate = currentTime - 86400000, // 1 day ago
                category = "fitness",
                imageUrl = "https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=400",
                userId = "system",
                description = "Start your day with this gentle yoga sequence designed to energize your body and mind.",
                videoUrl = "https://example.com/yoga-routine.mp4",
                thumbnailUrl = "https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=400",
                durationSeconds = 300,
                difficultyLevel = "beginner",
                expertName = "Sarah Williams",
                expertCredentials = "Certified Yoga Instructor",
                watchProgress = 0f,
                isDownloadedOffline = false,
                transcriptAvailable = true
            ),
            DiscoverContent.Video(
                id = "video_2",
                title = "Healthy Meal Prep for Busy Professionals",
                publishedDate = currentTime - 172800000, // 2 days ago
                category = "nutrition",
                imageUrl = "https://images.unsplash.com/photo-1490645935967-10de6ba17061?w=400",
                userId = "system",
                description = "Learn efficient meal preparation techniques to maintain healthy eating habits with a busy schedule.",
                videoUrl = "https://example.com/meal-prep.mp4",
                thumbnailUrl = "https://images.unsplash.com/photo-1490645935967-10de6ba17061?w=400",
                durationSeconds = 720,
                difficultyLevel = "beginner",
                expertName = "Chef Maria Garcia",
                expertCredentials = "Registered Dietitian",
                watchProgress = 0f,
                isDownloadedOffline = false,
                transcriptAvailable = true
            ),
            DiscoverContent.Video(
                id = "video_3",
                title = "Understanding Anxiety: Coping Strategies",
                publishedDate = currentTime - 259200000, // 3 days ago
                category = "mental-health",
                imageUrl = "https://images.unsplash.com/photo-1559757148-5c350d0d3c56?w=400",
                userId = "system",
                description = "Learn practical techniques for managing anxiety and building emotional resilience.",
                videoUrl = "https://example.com/anxiety-coping.mp4",
                thumbnailUrl = "https://images.unsplash.com/photo-1559757148-5c350d0d3c56?w=400",
                durationSeconds = 900,
                difficultyLevel = "intermediate",
                expertName = "Dr. James Wilson",
                expertCredentials = "Licensed Clinical Psychologist",
                watchProgress = 0f,
                isDownloadedOffline = false,
                transcriptAvailable = true
            ),
            DiscoverContent.Video(
                id = "video_4",
                title = "Home Workout: No Equipment Needed",
                publishedDate = currentTime - 345600000, // 4 days ago
                category = "fitness",
                imageUrl = "https://images.unsplash.com/photo-1571019613454-1cb2f99b2d8b?w=400",
                userId = "system",
                description = "A complete bodyweight workout you can do anywhere, perfect for maintaining fitness at home.",
                videoUrl = "https://example.com/home-workout.mp4",
                thumbnailUrl = "https://images.unsplash.com/photo-1571019613454-1cb2f99b2d8b?w=400",
                durationSeconds = 1200,
                difficultyLevel = "intermediate",
                expertName = "Mike Johnson",
                expertCredentials = "Certified Personal Trainer",
                watchProgress = 0f,
                isDownloadedOffline = false,
                transcriptAvailable = false
            )
        )
    }
}