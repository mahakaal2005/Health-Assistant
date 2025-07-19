# Design Document

## Overview

The Discover Section is designed as an educational health content platform that integrates seamlessly with the existing Health Assistant app architecture. The feature follows the established MVVM + Repository + Hilt DI pattern, utilizing Room for offline-first data storage and Firebase for content synchronization. The design prioritizes clean, distraction-free reading experiences while providing reliable access to credible health information.

## Architecture

### High-Level Architecture
```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│  DiscoverFragment │────│ DiscoverViewModel │────│ DiscoverRepository │
│   (UI Layer)     │    │  (Business Logic) │    │  (Data Layer)     │
└─────────────────┘    └──────────────────┘    └─────────────────┘
                                                         │
                        ┌────────────────────────────────┼────────────────────────────────┐
                        │                                │                                │
                ┌───────▼────────┐              ┌───────▼────────┐              ┌───────▼────────┐
                │  Room Database │              │ Firebase Store │              │ Content Cache  │
                │  (Offline)     │              │  (Remote)      │              │  (Images/Video)│
                └────────────────┘              └────────────────┘              └────────────────┘
```

### Data Flow Pattern
```
1. User opens Discover → DiscoverFragment loads
2. ViewModel requests content → Repository checks local cache first
3. If cache exists → Display cached content immediately
4. Background sync → Firebase fetches new content
5. New content → Update local cache → Refresh UI
6. User interactions → Track reading progress → Save bookmarks
```

## Components and Interfaces

### 1. Data Models

#### Core Content Models
```kotlin
// Health Article Entity
@Entity(tableName = "health_articles")
data class HealthArticle(
    @PrimaryKey val id: String,
    val title: String,
    val summary: String,
    val content: String,
    val category: String, // nutrition, fitness, mental-health, preventive-care
    val authorName: String,
    val authorCredentials: String,
    val sourceUrl: String,
    val publishedDate: Long,
    val lastUpdated: Long,
    val readingTimeMinutes: Int,
    val imageUrl: String?,
    val tags: List<String>,
    val isBookmarked: Boolean = false,
    val readProgress: Float = 0f, // 0.0 to 1.0
    val credibilityScore: Int, // 1-5 rating for source reliability
    val userId: String = ""
)

// Health News Entity
@Entity(tableName = "health_news")
data class HealthNews(
    @PrimaryKey val id: String,
    val headline: String,
    val summary: String,
    val fullContent: String?,
    val category: String,
    val sourcePublication: String,
    val sourceCredibility: String, // "peer-reviewed", "medical-journal", "health-organization"
    val publishedDate: Long,
    val imageUrl: String?,
    val externalUrl: String,
    val isBreakingNews: Boolean = false,
    val relevanceScore: Int, // Algorithm-based relevance to user
    val userId: String = ""
)

// Educational Video Entity
@Entity(tableName = "educational_videos")
data class EducationalVideo(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val category: String,
    val thumbnailUrl: String,
    val videoUrl: String,
    val durationSeconds: Int,
    val difficultyLevel: String, // "beginner", "intermediate", "advanced"
    val expertName: String,
    val expertCredentials: String,
    val publishedDate: Long,
    val watchProgress: Float = 0f, // 0.0 to 1.0
    val isDownloadedOffline: Boolean = false,
    val transcriptAvailable: Boolean = false,
    val userId: String = ""
)

// Content Bookmark Entity
@Entity(tableName = "content_bookmarks")
data class ContentBookmark(
    @PrimaryKey val id: String,
    val contentId: String,
    val contentType: String, // "article", "news", "video"
    val bookmarkedDate: Long,
    val userId: String
)
```

#### Content Categories
```kotlin
enum class HealthContentCategory(val displayName: String) {
    NUTRITION("Nutrition & Diet"),
    FITNESS("Fitness & Exercise"),
    MENTAL_HEALTH("Mental Health"),
    PREVENTIVE_CARE("Preventive Care"),
    CHRONIC_CONDITIONS("Chronic Conditions"),
    SEASONAL_HEALTH("Seasonal Health"),
    MEDICAL_NEWS("Medical News"),
    RESEARCH_UPDATES("Research Updates")
}

enum class ContentCredibility(val displayName: String, val score: Int) {
    PEER_REVIEWED("Peer-Reviewed Study", 5),
    MEDICAL_JOURNAL("Medical Journal", 4),
    HEALTH_ORGANIZATION("Health Organization", 4),
    MEDICAL_EXPERT("Medical Expert", 3),
    HEALTH_PUBLICATION("Health Publication", 3),
    GENERAL_NEWS("General News", 2)
}
```

### 2. Repository Layer

#### DiscoverRepository Interface
```kotlin
interface DiscoverRepository {
    // Content Fetching
    suspend fun getHealthArticles(category: String? = null, limit: Int = 20): Flow<Result<List<HealthArticle>>>
    suspend fun getHealthNews(limit: Int = 10): Flow<Result<List<HealthNews>>>
    suspend fun getEducationalVideos(category: String? = null, limit: Int = 15): Flow<Result<List<EducationalVideo>>>
    
    // Content Search & Filtering
    suspend fun searchContent(query: String, contentTypes: List<String>): Result<List<Any>>
    suspend fun getContentByCategory(category: HealthContentCategory): Result<List<Any>>
    suspend fun getTrendingContent(): Result<List<Any>>
    
    // Bookmarks & Reading Progress
    suspend fun bookmarkContent(contentId: String, contentType: String): Result<Unit>
    suspend fun removeBookmark(contentId: String): Result<Unit>
    suspend fun getBookmarkedContent(): Flow<Result<List<Any>>>
    suspend fun updateReadingProgress(contentId: String, progress: Float): Result<Unit>
    
    // Offline & Sync
    suspend fun syncContentFromFirebase(): Result<Unit>
    suspend fun getCachedContent(): Result<List<Any>>
    suspend fun downloadVideoForOffline(videoId: String): Result<Unit>
    
    // Content Validation
    suspend fun reportContentIssue(contentId: String, issueType: String, description: String): Result<Unit>
    suspend fun getContentCredibilityInfo(contentId: String): Result<ContentCredibility>
}
```

#### Repository Implementation Structure
```kotlin
@Singleton
class DiscoverRepositoryImpl @Inject constructor(
    private val discoverDao: DiscoverDao,
    private val firebaseFirestore: FirebaseFirestore,
    private val contentCacheManager: ContentCacheManager,
    private val credibilityValidator: CredibilityValidator
) : DiscoverRepository {
    
    // Offline-first pattern: Check local cache → Display → Background sync
    override suspend fun getHealthArticles(category: String?, limit: Int): Flow<Result<List<HealthArticle>>> {
        return flow {
            // Emit cached data first
            val cachedArticles = discoverDao.getHealthArticles(category, limit)
            emit(Result.Success(cachedArticles))
            
            // Background sync from Firebase
            try {
                val remoteArticles = fetchArticlesFromFirebase(category, limit)
                discoverDao.insertArticles(remoteArticles)
                emit(Result.Success(remoteArticles))
            } catch (e: Exception) {
                // Keep cached data, log sync failure
                Log.w("DiscoverRepo", "Sync failed, using cached data", e)
            }
        }
    }
}
```

### 3. Database Layer (Room)

#### DiscoverDao
```kotlin
@Dao
interface DiscoverDao {
    // Health Articles
    @Query("SELECT * FROM health_articles WHERE userId = :userId AND (:category IS NULL OR category = :category) ORDER BY publishedDate DESC LIMIT :limit")
    suspend fun getHealthArticles(userId: String, category: String?, limit: Int): List<HealthArticle>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArticles(articles: List<HealthArticle>)
    
    @Query("UPDATE health_articles SET readProgress = :progress WHERE id = :articleId AND userId = :userId")
    suspend fun updateReadingProgress(articleId: String, progress: Float, userId: String)
    
    // Health News
    @Query("SELECT * FROM health_news WHERE userId = :userId ORDER BY publishedDate DESC LIMIT :limit")
    suspend fun getHealthNews(userId: String, limit: Int): List<HealthNews>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNews(news: List<HealthNews>)
    
    // Educational Videos
    @Query("SELECT * FROM educational_videos WHERE userId = :userId AND (:category IS NULL OR category = :category) ORDER BY publishedDate DESC LIMIT :limit")
    suspend fun getEducationalVideos(userId: String, category: String?, limit: Int): List<EducationalVideo>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideos(videos: List<EducationalVideo>)
    
    // Bookmarks
    @Query("SELECT * FROM content_bookmarks WHERE userId = :userId ORDER BY bookmarkedDate DESC")
    suspend fun getBookmarks(userId: String): List<ContentBookmark>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: ContentBookmark)
    
    @Query("DELETE FROM content_bookmarks WHERE contentId = :contentId AND userId = :userId")
    suspend fun removeBookmark(contentId: String, userId: String)
    
    // Search
    @Query("""
        SELECT * FROM health_articles 
        WHERE userId = :userId AND (title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%' OR tags LIKE '%' || :query || '%')
        ORDER BY publishedDate DESC
    """)
    suspend fun searchArticles(userId: String, query: String): List<HealthArticle>
    
    // Cache Management
    @Query("DELETE FROM health_articles WHERE publishedDate < :cutoffDate AND userId = :userId")
    suspend fun cleanupOldArticles(cutoffDate: Long, userId: String)
    
    @Query("DELETE FROM health_news WHERE publishedDate < :cutoffDate AND userId = :userId")
    suspend fun cleanupOldNews(cutoffDate: Long, userId: String)
}
```

### 4. UI Layer

#### DiscoverFragment Architecture
```kotlin
@AndroidEntryPoint
class DiscoverFragment : Fragment() {
    private var _binding: FragmentDiscoverBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: DiscoverViewModel by viewModels()
    private lateinit var contentAdapter: DiscoverContentAdapter
    private lateinit var categoryFilterAdapter: CategoryFilterAdapter
    
    // UI Components
    private fun setupRecyclerView() {
        contentAdapter = DiscoverContentAdapter(
            onArticleClick = { article -> navigateToArticleReader(article) },
            onVideoClick = { video -> navigateToVideoPlayer(video) },
            onBookmarkClick = { content -> viewModel.toggleBookmark(content) },
            onShareClick = { content -> shareContent(content) }
        )
        
        binding.contentRecyclerView.apply {
            adapter = contentAdapter
            layoutManager = LinearLayoutManager(requireContext())
            addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
        }
    }
    
    private fun setupCategoryFilters() {
        categoryFilterAdapter = CategoryFilterAdapter { category ->
            viewModel.filterByCategory(category)
        }
        
        binding.categoryRecyclerView.apply {
            adapter = categoryFilterAdapter
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        }
    }
}
```

#### DiscoverViewModel
```kotlin
@HiltViewModel
class DiscoverViewModel @Inject constructor(
    private val discoverRepository: DiscoverRepository,
    private val sessionManager: SessionManager
) : ViewModel() {
    
    private val _contentState = MutableLiveData<ContentState>()
    val contentState: LiveData<ContentState> = _contentState
    
    private val _selectedCategory = MutableLiveData<HealthContentCategory?>()
    val selectedCategory: LiveData<HealthContentCategory?> = _selectedCategory
    
    sealed class ContentState {
        object Loading : ContentState()
        data class Success(val content: List<Any>) : ContentState()
        data class Error(val message: String) : ContentState()
        object Empty : ContentState()
    }
    
    fun loadContent() {
        viewModelScope.launch {
            _contentState.value = ContentState.Loading
            
            try {
                val articles = discoverRepository.getHealthArticles(
                    category = _selectedCategory.value?.name?.lowercase(),
                    limit = 20
                ).first()
                
                val news = discoverRepository.getHealthNews(limit = 10).first()
                val videos = discoverRepository.getEducationalVideos(
                    category = _selectedCategory.value?.name?.lowercase(),
                    limit = 15
                ).first()
                
                val combinedContent = mutableListOf<Any>()
                if (articles is Result.Success) combinedContent.addAll(articles.data)
                if (news is Result.Success) combinedContent.addAll(news.data)
                if (videos is Result.Success) combinedContent.addAll(videos.data)
                
                _contentState.value = if (combinedContent.isEmpty()) {
                    ContentState.Empty
                } else {
                    ContentState.Success(combinedContent.sortedByDescending { 
                        when (it) {
                            is HealthArticle -> it.publishedDate
                            is HealthNews -> it.publishedDate
                            is EducationalVideo -> it.publishedDate
                            else -> 0L
                        }
                    })
                }
            } catch (e: Exception) {
                _contentState.value = ContentState.Error(e.message ?: "Failed to load content")
            }
        }
    }
    
    fun filterByCategory(category: HealthContentCategory?) {
        _selectedCategory.value = category
        loadContent()
    }
    
    fun toggleBookmark(content: Any) {
        viewModelScope.launch {
            when (content) {
                is HealthArticle -> discoverRepository.bookmarkContent(content.id, "article")
                is HealthNews -> discoverRepository.bookmarkContent(content.id, "news")
                is EducationalVideo -> discoverRepository.bookmarkContent(content.id, "video")
            }
        }
    }
    
    fun searchContent(query: String) {
        viewModelScope.launch {
            _contentState.value = ContentState.Loading
            
            val searchResults = discoverRepository.searchContent(
                query = query,
                contentTypes = listOf("article", "news", "video")
            )
            
            when (searchResults) {
                is Result.Success -> {
                    _contentState.value = if (searchResults.data.isEmpty()) {
                        ContentState.Empty
                    } else {
                        ContentState.Success(searchResults.data)
                    }
                }
                is Result.Error -> {
                    _contentState.value = ContentState.Error(searchResults.message)
                }
            }
        }
    }
}
```

## Data Models

### Content Aggregation Model
```kotlin
// Unified content model for mixed feed display
sealed class DiscoverContent {
    abstract val id: String
    abstract val title: String
    abstract val publishedDate: Long
    abstract val category: String
    abstract val imageUrl: String?
    
    data class Article(
        override val id: String,
        override val title: String,
        override val publishedDate: Long,
        override val category: String,
        override val imageUrl: String?,
        val summary: String,
        val authorName: String,
        val readingTimeMinutes: Int,
        val credibilityScore: Int
    ) : DiscoverContent()
    
    data class News(
        override val id: String,
        override val title: String,
        override val publishedDate: Long,
        override val category: String,
        override val imageUrl: String?,
        val summary: String,
        val sourcePublication: String,
        val isBreakingNews: Boolean
    ) : DiscoverContent()
    
    data class Video(
        override val id: String,
        override val title: String,
        override val publishedDate: Long,
        override val category: String,
        override val imageUrl: String?,
        val description: String,
        val durationSeconds: Int,
        val expertName: String,
        val difficultyLevel: String
    ) : DiscoverContent()
}
```

### Firebase Content Structure
```kotlin
// Firebase Firestore collection structure
/*
/health_content/
  /articles/
    /{articleId}/
      - title: String
      - summary: String
      - content: String
      - category: String
      - authorName: String
      - authorCredentials: String
      - sourceUrl: String
      - publishedDate: Timestamp
      - lastUpdated: Timestamp
      - readingTimeMinutes: Number
      - imageUrl: String
      - tags: Array<String>
      - credibilityScore: Number
      
  /news/
    /{newsId}/
      - headline: String
      - summary: String
      - fullContent: String
      - category: String
      - sourcePublication: String
      - sourceCredibility: String
      - publishedDate: Timestamp
      - imageUrl: String
      - externalUrl: String
      - isBreakingNews: Boolean
      
  /videos/
    /{videoId}/
      - title: String
      - description: String
      - category: String
      - thumbnailUrl: String
      - videoUrl: String
      - durationSeconds: Number
      - difficultyLevel: String
      - expertName: String
      - expertCredentials: String
      - publishedDate: Timestamp
      - transcriptAvailable: Boolean
*/
```

## Error Handling

### Offline-First Error Strategy
```kotlin
class DiscoverErrorHandler {
    fun handleContentLoadError(error: Throwable): ContentState {
        return when (error) {
            is NetworkException -> {
                // Show cached content with offline indicator
                ContentState.OfflineMode("Showing cached content")
            }
            is ContentNotFoundException -> {
                ContentState.Empty
            }
            is FirebaseException -> {
                // Retry with exponential backoff
                ContentState.Error("Sync failed, retrying...")
            }
            else -> {
                ContentState.Error("Unable to load content")
            }
        }
    }
    
    fun handleVideoPlaybackError(error: Throwable): VideoState {
        return when (error) {
            is NetworkException -> VideoState.RequiresNetwork
            is StorageException -> VideoState.DownloadFailed
            else -> VideoState.PlaybackError
        }
    }
}
```

### Content Validation
```kotlin
class ContentCredibilityValidator {
    fun validateArticleCredibility(article: HealthArticle): ValidationResult {
        val score = calculateCredibilityScore(
            authorCredentials = article.authorCredentials,
            sourceUrl = article.sourceUrl,
            lastUpdated = article.lastUpdated
        )
        
        return ValidationResult(
            isCredible = score >= 3,
            credibilityScore = score,
            warnings = generateWarnings(article)
        )
    }
    
    private fun generateWarnings(article: HealthArticle): List<String> {
        val warnings = mutableListOf<String>()
        
        if (System.currentTimeMillis() - article.lastUpdated > TimeUnit.DAYS.toMillis(365)) {
            warnings.add("This content is over a year old")
        }
        
        if (article.credibilityScore < 3) {
            warnings.add("Source credibility not verified")
        }
        
        return warnings
    }
}
```

## Testing Strategy

### Unit Testing Approach
```kotlin
// Repository Testing
@Test
fun `getHealthArticles returns cached data first then syncs`() = runTest {
    // Given
    val cachedArticles = listOf(mockHealthArticle())
    whenever(discoverDao.getHealthArticles(any(), any(), any())).thenReturn(cachedArticles)
    
    // When
    val result = repository.getHealthArticles().first()
    
    // Then
    assertThat(result).isInstanceOf(Result.Success::class.java)
    assertThat((result as Result.Success).data).isEqualTo(cachedArticles)
    
    // Verify background sync is triggered
    verify(firebaseFirestore).collection("health_content")
}

// ViewModel Testing
@Test
fun `loadContent updates state correctly`() = runTest {
    // Given
    val mockArticles = listOf(mockHealthArticle())
    whenever(repository.getHealthArticles(any(), any())).thenReturn(flowOf(Result.Success(mockArticles)))
    
    // When
    viewModel.loadContent()
    
    // Then
    assertThat(viewModel.contentState.value).isInstanceOf(ContentState.Success::class.java)
}
```

### Integration Testing
```kotlin
@Test
fun `offline content access works without network`() = runTest {
    // Given - Populate local database
    database.discoverDao().insertArticles(listOf(mockHealthArticle()))
    
    // When - Simulate no network
    repository.getHealthArticles().test {
        // Then - Should return cached content
        val result = awaitItem()
        assertThat(result).isInstanceOf(Result.Success::class.java)
        awaitComplete()
    }
}
```