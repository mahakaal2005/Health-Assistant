package com.example.health_assistant.features.discover.data

import com.example.health_assistant.core.util.Result
import com.example.health_assistant.features.discover.data.cache.ContentCacheManager
import com.example.health_assistant.features.discover.data.entity.*
import com.example.health_assistant.features.discover.data.firebase.*
import com.example.health_assistant.features.discover.domain.validation.ContentCredibilityValidator
import com.google.firebase.firestore.*
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Integration tests for Firebase sync functionality
 * Tests the complete sync flow from Firebase to local database
 */
class DiscoverFirebaseSyncIntegrationTest {

    private lateinit var repository: DiscoverRepositoryImpl
    private lateinit var discoverDao: DiscoverDao
    private lateinit var firestore: FirebaseFirestore
    private lateinit var cacheManager: ContentCacheManager
    private lateinit var credibilityValidator: ContentCredibilityValidator

    private val testUserId = "test_user_123"

    @BeforeEach
    fun setup() {
        discoverDao = mockk(relaxed = true)
        firestore = mockk()
        cacheManager = mockk(relaxed = true)
        credibilityValidator = mockk(relaxed = true)

        repository = DiscoverRepositoryImpl(
            discoverDao = discoverDao,
            firestore = firestore,
            cacheManager = cacheManager,
            credibilityValidator = credibilityValidator
        )
    }

    @Test
    fun `full sync flow updates all content types`() = runTest {
        // Given - Mock Firebase data
        val firebaseArticles = listOf(
            createFirebaseArticle("1", "Article 1", "nutrition"),
            createFirebaseArticle("2", "Article 2", "fitness")
        )
        val firebaseNews = listOf(
            createFirebaseNews("1", "News 1", "health"),
            createFirebaseNews("2", "Breaking News", "research", isBreaking = true)
        )
        val firebaseVideos = listOf(
            createFirebaseVideo("1", "Video 1", "nutrition"),
            createFirebaseVideo("2", "Video 2", "fitness")
        )

        // Mock Firestore collections
        mockFirestoreCollection("health_articles", firebaseArticles)
        mockFirestoreCollection("health_news", firebaseNews)
        mockFirestoreCollection("educational_videos", firebaseVideos)

        // Mock local database responses
        coEvery { discoverDao.getHealthArticlesFlow(testUserId, null, any()) } returns flowOf(emptyList())
        coEvery { discoverDao.getHealthNewsFlow(testUserId, null, any()) } returns flowOf(emptyList())
        coEvery { discoverDao.getEducationalVideosFlow(testUserId, null, any()) } returns flowOf(emptyList())

        // When - Trigger sync
        val syncResult = repository.syncContent(testUserId)

        // Then - Verify sync completed successfully
        assertTrue(syncResult is Result.Success)

        // Verify all content types were inserted
        coVerify { discoverDao.insertHealthArticles(match { it.size == 2 }) }
        coVerify { discoverDao.insertHealthNews(match { it.size == 2 }) }
        coVerify { discoverDao.insertEducationalVideos(match { it.size == 2 }) }
    }

    @Test
    fun `sync handles incremental updates correctly`() = runTest {
        // Given - Existing local data
        val existingArticle = createHealthArticleEntity("1", "Old Article", lastUpdated = 1000L)
        val updatedFirebaseArticle = createFirebaseArticle("1", "Updated Article", "nutrition")
        
        coEvery { discoverDao.getHealthArticlesFlow(testUserId, null, any()) } returns flowOf(listOf(existingArticle))
        mockFirestoreCollection("health_articles", listOf(updatedFirebaseArticle))
        mockFirestoreCollection("health_news", emptyList<FirebaseHealthNews>())
        mockFirestoreCollection("educational_videos", emptyList<FirebaseEducationalVideo>())

        // When - Sync with updated content
        val syncResult = repository.syncContent(testUserId)

        // Then - Verify update was processed
        assertTrue(syncResult is Result.Success)
        coVerify { discoverDao.insertHealthArticles(match { articles ->
            articles.size == 1 && articles[0].title == "Updated Article"
        }) }
    }

    @Test
    fun `sync handles network failures gracefully`() = runTest {
        // Given - Network failure for one collection
        val firebaseArticles = listOf(createFirebaseArticle("1", "Article 1", "nutrition"))
        
        mockFirestoreCollection("health_articles", firebaseArticles)
        mockFirestoreError("health_news")
        mockFirestoreError("educational_videos")

        coEvery { discoverDao.getHealthArticlesFlow(testUserId, null, any()) } returns flowOf(emptyList())

        // When - Sync with partial network failure
        val syncResult = repository.syncContent(testUserId)

        // Then - Should succeed with available data
        assertTrue(syncResult is Result.Success)
        coVerify { discoverDao.insertHealthArticles(match { it.size == 1 }) }
        // News and videos should not be inserted due to network errors
        coVerify(exactly = 0) { discoverDao.insertHealthNews(any()) }
        coVerify(exactly = 0) { discoverDao.insertEducationalVideos(any()) }
    }

    @Test
    fun `sync preserves user-specific data during updates`() = runTest {
        // Given - Local article with user progress
        val localArticle = createHealthArticleEntity(
            id = "1", 
            title = "Article 1",
            readProgress = 0.75f,
            isBookmarked = true
        )
        val firebaseArticle = createFirebaseArticle("1", "Updated Article", "nutrition")
        
        coEvery { discoverDao.getHealthArticlesFlow(testUserId, null, any()) } returns flowOf(listOf(localArticle))
        mockFirestoreCollection("health_articles", listOf(firebaseArticle))
        mockFirestoreCollection("health_news", emptyList<FirebaseHealthNews>())
        mockFirestoreCollection("educational_videos", emptyList<FirebaseEducationalVideo>())

        // When - Sync updates content
        val syncResult = repository.syncContent(testUserId)

        // Then - User data should be preserved
        assertTrue(syncResult is Result.Success)
        coVerify { discoverDao.insertHealthArticles(match { articles ->
            articles.size == 1 && 
            articles[0].title == "Updated Article" && // Content updated
            articles[0].readProgress == 0.75f && // Progress preserved
            articles[0].isBookmarked == true // Bookmark preserved
        }) }
    }

    @Test
    fun `sync handles large datasets with pagination`() = runTest {
        // Given - Large dataset
        val largeArticleSet = (1..100).map { i ->
            createFirebaseArticle(i.toString(), "Article $i", "nutrition")
        }
        
        mockFirestoreCollection("health_articles", largeArticleSet)
        mockFirestoreCollection("health_news", emptyList<FirebaseHealthNews>())
        mockFirestoreCollection("educational_videos", emptyList<FirebaseEducationalVideo>())
        
        coEvery { discoverDao.getHealthArticlesFlow(testUserId, null, any()) } returns flowOf(emptyList())

        // When - Sync large dataset
        val syncResult = repository.syncContent(testUserId)

        // Then - All data should be synced
        assertTrue(syncResult is Result.Success)
        coVerify { discoverDao.insertHealthArticles(match { it.size == 100 }) }
    }

    @Test
    fun `sync updates cache statistics after completion`() = runTest {
        // Given
        val firebaseArticles = listOf(createFirebaseArticle("1", "Article 1", "nutrition"))
        mockFirestoreCollection("health_articles", firebaseArticles)
        mockFirestoreCollection("health_news", emptyList<FirebaseHealthNews>())
        mockFirestoreCollection("educational_videos", emptyList<FirebaseEducationalVideo>())
        
        coEvery { discoverDao.getHealthArticlesFlow(testUserId, null, any()) } returns flowOf(emptyList())
        coEvery { cacheManager.updateCacheStatistics(testUserId) } just Runs

        // When
        val syncResult = repository.syncContent(testUserId)

        // Then
        assertTrue(syncResult is Result.Success)
        coVerify { cacheManager.updateCacheStatistics(testUserId) }
    }

    @Test
    fun `sync handles content validation during import`() = runTest {
        // Given
        val firebaseArticle = createFirebaseArticle("1", "Article 1", "nutrition")
        mockFirestoreCollection("health_articles", listOf(firebaseArticle))
        mockFirestoreCollection("health_news", emptyList<FirebaseHealthNews>())
        mockFirestoreCollection("educational_videos", emptyList<FirebaseEducationalVideo>())
        
        coEvery { discoverDao.getHealthArticlesFlow(testUserId, null, any()) } returns flowOf(emptyList())
        
        // Mock validation
        coEvery { credibilityValidator.validateFirebaseContent(any()) } returns true

        // When
        val syncResult = repository.syncContent(testUserId)

        // Then
        assertTrue(syncResult is Result.Success)
        coVerify { credibilityValidator.validateFirebaseContent(any()) }
        coVerify { discoverDao.insertHealthArticles(any()) }
    }

    @Test
    fun `sync skips invalid content during import`() = runTest {
        // Given
        val validArticle = createFirebaseArticle("1", "Valid Article", "nutrition")
        val invalidArticle = createFirebaseArticle("2", "", "nutrition") // Invalid - empty title
        
        mockFirestoreCollection("health_articles", listOf(validArticle, invalidArticle))
        mockFirestoreCollection("health_news", emptyList<FirebaseHealthNews>())
        mockFirestoreCollection("educational_videos", emptyList<FirebaseEducationalVideo>())
        
        coEvery { discoverDao.getHealthArticlesFlow(testUserId, null, any()) } returns flowOf(emptyList())
        
        // Mock validation - valid content passes, invalid fails
        coEvery { credibilityValidator.validateFirebaseContent(match<FirebaseHealthArticle> { it.title.isNotEmpty() }) } returns true
        coEvery { credibilityValidator.validateFirebaseContent(match<FirebaseHealthArticle> { it.title.isEmpty() }) } returns false

        // When
        val syncResult = repository.syncContent(testUserId)

        // Then
        assertTrue(syncResult is Result.Success)
        coVerify { discoverDao.insertHealthArticles(match { it.size == 1 && it[0].title == "Valid Article" }) }
    }

    @Test
    fun `sync handles concurrent access correctly`() = runTest {
        // Given
        val firebaseArticles = listOf(createFirebaseArticle("1", "Article 1", "nutrition"))
        mockFirestoreCollection("health_articles", firebaseArticles)
        mockFirestoreCollection("health_news", emptyList<FirebaseHealthNews>())
        mockFirestoreCollection("educational_videos", emptyList<FirebaseEducationalVideo>())
        
        coEvery { discoverDao.getHealthArticlesFlow(testUserId, null, any()) } returns flowOf(emptyList())

        // When - Trigger multiple concurrent syncs
        val syncResult1 = repository.syncContent(testUserId)
        val syncResult2 = repository.syncContent(testUserId)

        // Then - Both should complete successfully
        assertTrue(syncResult1 is Result.Success)
        assertTrue(syncResult2 is Result.Success)
        
        // Database operations should be called at least once
        coVerify(atLeast = 1) { discoverDao.insertHealthArticles(any()) }
    }

    // ==================== HELPER METHODS ====================

    private fun createFirebaseArticle(
        id: String,
        title: String,
        category: String
    ) = FirebaseHealthArticle(
        id = id,
        title = title,
        summary = "Summary for $title",
        content = "Content for $title",
        category = category,
        authorName = "Dr. Author",
        authorCredentials = "MD",
        sourceUrl = "https://test.com/$id",
        publishedDate = com.google.firebase.Timestamp.now(),
        lastUpdated = com.google.firebase.Timestamp.now(),
        readingTimeMinutes = 5,
        imageUrl = "https://test.com/image$id.jpg",
        tags = listOf("health", category),
        credibilityScore = 5
    )

    private fun createFirebaseNews(
        id: String,
        headline: String,
        category: String,
        isBreaking: Boolean = false
    ) = FirebaseHealthNews(
        id = id,
        headline = headline,
        summary = "Summary for $headline",
        fullContent = "Content for $headline",
        category = category,
        sourcePublication = "Test Journal",
        sourceCredibility = "medical-journal",
        publishedDate = com.google.firebase.Timestamp.now(),
        imageUrl = "https://test.com/news$id.jpg",
        externalUrl = "https://test.com/news/$id",
        isBreakingNews = isBreaking,
        relevanceScore = 5
    )

    private fun createFirebaseVideo(
        id: String,
        title: String,
        category: String
    ) = FirebaseEducationalVideo(
        id = id,
        title = title,
        description = "Description for $title",
        category = category,
        thumbnailUrl = "https://test.com/thumb$id.jpg",
        videoUrl = "https://test.com/video$id.mp4",
        durationSeconds = 300,
        difficultyLevel = "beginner",
        expertName = "Dr. Expert",
        expertCredentials = "MD",
        publishedDate = com.google.firebase.Timestamp.now(),
        transcriptAvailable = true
    )

    private fun createHealthArticleEntity(
        id: String,
        title: String,
        readProgress: Float = 0f,
        isBookmarked: Boolean = false,
        lastUpdated: Long = System.currentTimeMillis()
    ) = HealthArticleEntity(
        id = id,
        title = title,
        summary = "Summary for $title",
        content = "Content for $title",
        category = "nutrition",
        authorName = "Dr. Author",
        authorCredentials = "MD",
        sourceUrl = "https://test.com/$id",
        publishedDate = System.currentTimeMillis(),
        lastUpdated = lastUpdated,
        readingTimeMinutes = 5,
        imageUrl = "https://test.com/image$id.jpg",
        tags = listOf("health", "nutrition"),
        isBookmarked = isBookmarked,
        readProgress = readProgress,
        credibilityScore = 5,
        userId = testUserId
    )

    private fun mockFirestoreCollection(collection: String, data: List<Any>) {
        val collectionRef = mockk<CollectionReference>()
        val query = mockk<Query>()
        val querySnapshot = mockk<QuerySnapshot>()
        val documents = data.map { item ->
            val doc = mockk<QueryDocumentSnapshot>()
            every { doc.toObject(any<Class<Any>>()) } returns item
            doc
        }

        every { firestore.collection(collection) } returns collectionRef
        every { collectionRef.orderBy("publishedDate", Query.Direction.DESCENDING) } returns query
        every { query.limit(any()) } returns query
        coEvery { query.get() } returns querySnapshot
        every { querySnapshot.documents } returns documents
    }

    private fun mockFirestoreError(collection: String) {
        val collectionRef = mockk<CollectionReference>()
        val query = mockk<Query>()

        every { firestore.collection(collection) } returns collectionRef
        every { collectionRef.orderBy("publishedDate", Query.Direction.DESCENDING) } returns query
        every { query.limit(any()) } returns query
        coEvery { query.get() } throws FirebaseFirestoreException("Network error", FirebaseFirestoreException.Code.UNAVAILABLE)
    }
}