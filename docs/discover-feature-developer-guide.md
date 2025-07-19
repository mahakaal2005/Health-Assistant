# Discover Feature Developer Guide

## Overview

The Discover feature is a comprehensive health content platform that provides users with curated articles, news, and educational videos. This guide covers the architecture, implementation details, and extension points for developers working on the Discover feature.

## Architecture Overview

### High-Level Architecture

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│  Presentation   │────│   Domain Layer   │────│   Data Layer    │
│     Layer       │    │  (Business Logic)│    │                 │
└─────────────────┘    └──────────────────┘    └─────────────────┘
         │                        │                        │
    ┌────▼────┐              ┌────▼────┐              ┌────▼────┐
    │Fragment │              │Use Cases│              │Repository│
    │ViewModel│              │ Manager │              │   DAO   │
    │ Adapter │              │Validator│              │Firebase │
    └─────────┘              └─────────┘              └─────────┘
```

### Key Components

1. **Presentation Layer**
   - `DiscoverFragment` - Main UI container
   - `DiscoverViewModel` - UI state management
   - `DiscoverContentAdapter` - RecyclerView adapter for mixed content
   - `ArticleReaderFragment` - Full article reading experience
   - `VideoPlayerFragment` - Video playback interface
   - `BookmarksFragment` - Bookmark management

2. **Domain Layer**
   - `DiscoverManager` - Central business logic coordinator
   - Use Cases - Specific business operations
   - `ContentCredibilityValidator` - Content validation logic
   - Domain Models - Pure business objects

3. **Data Layer**
   - `DiscoverRepository` - Data access abstraction
   - `DiscoverDao` - Room database operations
   - Firebase integration - Remote data sync
   - `ContentCacheManager` - Offline content management

## Data Models

### Core Content Types

```kotlin
// Base content interface
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
        val content: String,
        val authorName: String,
        val authorCredentials: String,
        val sourceUrl: String,
        val readingTimeMinutes: Int,
        val credibilityScore: Int,
        val isBookmarked: Boolean,
        val readProgress: Float
    ) : DiscoverContent()
    
    data class News(
        override val id: String,
        override val title: String,
        override val publishedDate: Long,
        override val category: String,
        override val imageUrl: String?,
        val summary: String,
        val sourcePublication: String,
        val isBreakingNews: Boolean,
        val externalUrl: String
    ) : DiscoverContent()
    
    data class Video(
        override val id: String,
        override val title: String,
        override val publishedDate: Long,
        override val category: String,
        override val imageUrl: String?,
        val description: String,
        val videoUrl: String,
        val durationSeconds: Int,
        val expertName: String,
        val difficultyLevel: String,
        val watchProgress: Float
    ) : DiscoverContent()
}
```

### Database Entities

The feature uses Room entities for local storage:

- `HealthArticleEntity` - Article storage
- `HealthNewsEntity` - News storage  
- `EducationalVideoEntity` - Video storage
- `ContentBookmarkEntity` - Bookmark tracking

### Firebase Models

Firebase models handle remote data synchronization:

- `FirebaseHealthArticle` - Remote article data
- `FirebaseHealthNews` - Remote news data
- `FirebaseEducationalVideo` - Remote video data

## Key Features Implementation

### 1. Content Loading and Caching

The feature implements an offline-first approach:

```kotlin
// Repository pattern for offline-first data access
override suspend fun getHealthArticles(
    userId: String, 
    category: String?, 
    limit: Int
): Flow<Result<List<DiscoverContent.Article>>> {
    return flow {
        // 1. Emit cached data immediately
        val cachedArticles = discoverDao.getHealthArticlesFlow(userId, category, limit)
        emit(Result.Success(cachedArticles.first().map { it.toDomain() }))
        
        // 2. Background sync from Firebase
        try {
            val remoteArticles = syncArticlesFromFirebase(category, limit)
            discoverDao.insertHealthArticles(remoteArticles.map { it.toEntity(userId) })
            emit(Result.Success(remoteArticles.map { it.toDomain() }))
        } catch (e: Exception) {
            // Keep cached data, log sync failure
            Log.w("DiscoverRepo", "Sync failed, using cached data", e)
        }
    }
}
```

### 2. Content Validation and Credibility

Content credibility is validated using multiple factors:

```kotlin
class ContentCredibilityValidator {
    fun validateContent(content: DiscoverContent): ContentValidationResult {
        val score = calculateCredibilityScore(content)
        val warnings = generateWarnings(content)
        
        return ContentValidationResult(
            isCredible = score >= 3,
            credibilityScore = score,
            warnings = warnings,
            lastValidated = System.currentTimeMillis()
        )
    }
    
    private fun calculateCredibilityScore(content: DiscoverContent): Int {
        var score = 0
        
        // Author credentials
        if (content is DiscoverContent.Article) {
            score += when {
                content.authorCredentials.contains("MD") -> 2
                content.authorCredentials.contains("PhD") -> 2
                content.authorCredentials.contains("RD") -> 1
                else -> 0
            }
        }
        
        // Source reputation
        score += when {
            content.sourceUrl?.contains("nih.gov") == true -> 3
            content.sourceUrl?.contains("mayoclinic.org") == true -> 3
            content.sourceUrl?.contains("webmd.com") == true -> 2
            else -> 1
        }
        
        // Content freshness
        val daysSincePublished = (System.currentTimeMillis() - content.publishedDate) / (24 * 60 * 60 * 1000)
        score += when {
            daysSincePublished < 30 -> 2
            daysSincePublished < 365 -> 1
            else -> 0
        }
        
        return minOf(score, 5) // Cap at 5
    }
}
```

### 3. Search Implementation

Search supports multiple content types with highlighting:

```kotlin
override suspend fun searchContent(
    userId: String,
    query: String,
    contentTypes: List<String>,
    limit: Int
): Result<List<DiscoverContent>> {
    if (query.isBlank()) return Result.Error(Exception("Query cannot be empty"))
    
    return try {
        val searchResults = discoverDao.searchAllContent(userId, query, limit)
        val domainResults = searchResults.mapNotNull { result ->
            when (result.contentType) {
                "article" -> discoverDao.getHealthArticleById(result.contentId, userId)?.toDomain()
                "news" -> discoverDao.getHealthNewsById(result.contentId, userId)?.toDomain()
                "video" -> discoverDao.getEducationalVideoById(result.contentId, userId)?.toDomain()
                else -> null
            }
        }
        
        Result.Success(domainResults)
    } catch (e: Exception) {
        Result.Error(e)
    }
}
```

### 4. Bookmark Management

Bookmarks are synchronized across devices:

```kotlin
override suspend fun toggleBookmark(
    userId: String, 
    content: DiscoverContent
): Result<Boolean> {
    return try {
        val isCurrentlyBookmarked = discoverDao.isContentBookmarked(content.id, userId)
        
        if (isCurrentlyBookmarked) {
            // Remove bookmark
            discoverDao.removeBookmark(content.id, userId)
            updateContentBookmarkStatus(content, false, userId)
            Result.Success(false)
        } else {
            // Add bookmark
            val bookmark = ContentBookmarkEntity(
                id = UUID.randomUUID().toString(),
                contentId = content.id,
                contentType = when (content) {
                    is DiscoverContent.Article -> "article"
                    is DiscoverContent.News -> "news"
                    is DiscoverContent.Video -> "video"
                },
                bookmarkedDate = System.currentTimeMillis(),
                userId = userId
            )
            discoverDao.insertBookmark(bookmark)
            updateContentBookmarkStatus(content, true, userId)
            Result.Success(true)
        }
    } catch (e: Exception) {
        Result.Error(e)
    }
}
```

## Extension Points

### Adding New Content Types

To add a new content type (e.g., Podcast):

1. **Create Domain Model**
```kotlin
data class Podcast(
    override val id: String,
    override val title: String,
    override val publishedDate: Long,
    override val category: String,
    override val imageUrl: String?,
    val description: String,
    val audioUrl: String,
    val durationMinutes: Int,
    val hostName: String,
    val episodeNumber: Int
) : DiscoverContent()
```

2. **Create Database Entity**
```kotlin
@Entity(tableName = "podcasts")
data class PodcastEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val audioUrl: String,
    val durationMinutes: Int,
    val hostName: String,
    val episodeNumber: Int,
    val publishedDate: Long,
    val category: String,
    val imageUrl: String?,
    val userId: String
)
```

3. **Update DAO**
```kotlin
@Dao
interface DiscoverDao {
    // Add podcast-specific queries
    @Query("SELECT * FROM podcasts WHERE userId = :userId ORDER BY publishedDate DESC LIMIT :limit")
    suspend fun getPodcasts(userId: String, limit: Int): List<PodcastEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPodcasts(podcasts: List<PodcastEntity>)
}
```

4. **Update Repository**
```kotlin
interface DiscoverRepository {
    suspend fun getPodcasts(userId: String, limit: Int): Flow<Result<List<DiscoverContent.Podcast>>>
}
```

5. **Update UI Adapter**
```kotlin
class DiscoverContentAdapter {
    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is DiscoverContent.Article -> VIEW_TYPE_ARTICLE
            is DiscoverContent.News -> VIEW_TYPE_NEWS
            is DiscoverContent.Video -> VIEW_TYPE_VIDEO
            is DiscoverContent.Podcast -> VIEW_TYPE_PODCAST // New type
        }
    }
}
```

### Adding New Content Sources

To integrate a new content source:

1. **Create Firebase Model**
```kotlin
data class FirebasePodcast(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val audioUrl: String = "",
    val durationMinutes: Int = 0,
    val hostName: String = "",
    val episodeNumber: Int = 0,
    val publishedDate: Timestamp = Timestamp.now(),
    val category: String = "",
    val imageUrl: String? = null
)
```

2. **Update Sync Logic**
```kotlin
class DiscoverRepositoryImpl {
    override suspend fun syncContent(userId: String): Result<Unit> {
        return try {
            // Existing sync logic...
            
            // Add podcast sync
            val remotePodcasts = fetchPodcastsFromFirebase()
            val podcastEntities = remotePodcasts.map { it.toEntity(userId) }
            discoverDao.insertPodcasts(podcastEntities)
            
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
```

### Customizing Content Validation

To add custom validation rules:

```kotlin
class CustomContentValidator : ContentCredibilityValidator() {
    override fun validateContent(content: DiscoverContent): ContentValidationResult {
        val baseResult = super.validateContent(content)
        
        // Add custom validation logic
        val customWarnings = mutableListOf<String>()
        
        // Example: Check for specific keywords
        if (content.title.contains("miracle cure", ignoreCase = true)) {
            customWarnings.add("Content contains potentially misleading claims")
        }
        
        // Example: Check content age for specific categories
        if (content.category == "research" && isContentOlderThan(content, 6)) {
            customWarnings.add("Research content is older than 6 months")
        }
        
        return baseResult.copy(
            warnings = baseResult.warnings + customWarnings,
            credibilityScore = adjustScoreForCustomRules(baseResult.credibilityScore, content)
        )
    }
}
```

## Testing Strategy

### Unit Tests

Focus on testing business logic in isolation:

```kotlin
@Test
fun `getHealthArticles returns cached data first then syncs`() = runTest {
    // Given
    val cachedArticles = listOf(createTestHealthArticleEntity())
    coEvery { discoverDao.getHealthArticlesFlow(testUserId, null, 20) } returns flowOf(cachedArticles)
    
    // When
    val result = repository.getHealthArticles(testUserId, null, 20).first()
    
    // Then
    assertTrue(result is Result.Success)
    assertEquals(1, result.data.size)
    coVerify { discoverDao.getHealthArticlesFlow(testUserId, null, 20) }
}
```

### Integration Tests

Test complete data flows:

```kotlin
@Test
fun `full sync flow updates all content types`() = runTest {
    // Given - Mock Firebase data
    val firebaseArticles = listOf(createFirebaseArticle())
    mockFirestoreCollection("health_articles", firebaseArticles)
    
    // When - Trigger sync
    val syncResult = repository.syncContent(testUserId)
    
    // Then - Verify sync completed
    assertTrue(syncResult is Result.Success)
    coVerify { discoverDao.insertHealthArticles(any()) }
}
```

### UI Tests

Test user interactions:

```kotlin
@Test
fun bookmarkButton_togglesBookmarkState() {
    // Navigate to Discover tab
    composeTestRule.onNodeWithContentDescription("Discover").performClick()
    
    // Click bookmark button
    composeTestRule.onAllNodesWithTag("bookmark_button")[0].performClick()
    
    // Verify bookmark state changed
    composeTestRule.onNodeWithText("Bookmarked").assertIsDisplayed()
}
```

## Performance Considerations

### Database Optimization

1. **Proper Indexing**
```kotlin
@Entity(
    tableName = "health_articles",
    indices = [
        Index(value = ["userId", "category"]),
        Index(value = ["publishedDate"]),
        Index(value = ["credibilityScore"])
    ]
)
```

2. **Pagination**
```kotlin
@Query("SELECT * FROM health_articles WHERE userId = :userId ORDER BY publishedDate DESC LIMIT :limit OFFSET :offset")
suspend fun getHealthArticlesPaged(userId: String, limit: Int, offset: Int): List<HealthArticleEntity>
```

### Memory Management

1. **Image Loading**
```kotlin
// Use Glide with proper caching
Glide.with(context)
    .load(imageUrl)
    .diskCacheStrategy(DiskCacheStrategy.ALL)
    .placeholder(R.drawable.placeholder_article)
    .into(imageView)
```

2. **RecyclerView Optimization**
```kotlin
class DiscoverContentAdapter : ListAdapter<DiscoverContent, RecyclerView.ViewHolder>(DiffCallback()) {
    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)
        // Clear image loading to prevent memory leaks
        if (holder is ArticleViewHolder) {
            Glide.with(holder.itemView.context).clear(holder.imageView)
        }
    }
}
```

### Network Optimization

1. **Request Batching**
```kotlin
// Batch multiple content type requests
suspend fun syncAllContent(userId: String): Result<Unit> {
    return coroutineScope {
        val articlesDeferred = async { syncArticles(userId) }
        val newsDeferred = async { syncNews(userId) }
        val videosDeferred = async { syncVideos(userId) }
        
        try {
            articlesDeferred.await()
            newsDeferred.await()
            videosDeferred.await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
```

2. **Smart Caching**
```kotlin
class ContentCacheManager {
    suspend fun shouldRefreshContent(contentType: String, userId: String): Boolean {
        val lastSync = getLastSyncTime(contentType, userId)
        val cacheAge = System.currentTimeMillis() - lastSync
        
        return when (contentType) {
            "breaking_news" -> cacheAge > TimeUnit.MINUTES.toMillis(15)
            "articles" -> cacheAge > TimeUnit.HOURS.toMillis(6)
            "videos" -> cacheAge > TimeUnit.DAYS.toMillis(1)
            else -> cacheAge > TimeUnit.HOURS.toMillis(12)
        }
    }
}
```

## Security Considerations

### Content Validation

Always validate content from external sources:

```kotlin
class ContentSecurityValidator {
    fun validateContent(content: Any): Boolean {
        return when (content) {
            is FirebaseHealthArticle -> validateArticle(content)
            is FirebaseHealthNews -> validateNews(content)
            is FirebaseEducationalVideo -> validateVideo(content)
            else -> false
        }
    }
    
    private fun validateArticle(article: FirebaseHealthArticle): Boolean {
        // Check required fields
        if (article.title.isBlank() || article.content.isBlank()) return false
        
        // Validate URLs
        if (!isValidUrl(article.sourceUrl)) return false
        if (article.imageUrl != null && !isValidUrl(article.imageUrl)) return false
        
        // Check content length limits
        if (article.title.length > 200) return false
        if (article.content.length > 50000) return false
        
        return true
    }
}
```

### User Data Protection

Ensure user data is properly isolated:

```kotlin
// Always include userId in queries
@Query("SELECT * FROM health_articles WHERE userId = :userId AND id = :articleId")
suspend fun getHealthArticleById(articleId: String, userId: String): HealthArticleEntity?

// Never expose other users' data
@Query("DELETE FROM health_articles WHERE userId = :userId")
suspend fun deleteAllUserContent(userId: String)
```

## Troubleshooting

### Common Issues

1. **Sync Failures**
   - Check network connectivity
   - Verify Firebase configuration
   - Check user authentication status

2. **Performance Issues**
   - Monitor database query performance
   - Check image loading efficiency
   - Verify proper RecyclerView recycling

3. **Content Not Displaying**
   - Verify data mapping between layers
   - Check content validation rules
   - Ensure proper error handling

### Debugging Tools

1. **Database Inspection**
```kotlin
// Add debug queries for development
@Query("SELECT COUNT(*) FROM health_articles WHERE userId = :userId")
suspend fun getArticleCount(userId: String): Int

@Query("SELECT category, COUNT(*) as count FROM health_articles WHERE userId = :userId GROUP BY category")
suspend fun getContentCountByCategory(userId: String): List<CategoryCount>
```

2. **Logging**
```kotlin
class DiscoverRepositoryImpl {
    companion object {
        private const val TAG = "DiscoverRepository"
    }
    
    override suspend fun syncContent(userId: String): Result<Unit> {
        Log.d(TAG, "Starting content sync for user: $userId")
        
        return try {
            val startTime = System.currentTimeMillis()
            // Sync logic...
            val duration = System.currentTimeMillis() - startTime
            Log.d(TAG, "Content sync completed in ${duration}ms")
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Content sync failed", e)
            Result.Error(e)
        }
    }
}
```

## Best Practices

1. **Always use offline-first approach** - Cache data locally and sync in background
2. **Validate all external content** - Never trust remote data without validation
3. **Implement proper error handling** - Gracefully handle network failures and data errors
4. **Use proper data isolation** - Always filter by userId to prevent data leaks
5. **Optimize for performance** - Use pagination, proper indexing, and efficient image loading
6. **Test thoroughly** - Include unit, integration, and UI tests
7. **Monitor performance** - Track sync times, database query performance, and memory usage

This guide provides a comprehensive overview of the Discover feature architecture and implementation. For specific implementation details, refer to the source code and additional documentation files.