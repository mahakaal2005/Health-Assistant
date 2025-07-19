package com.example.health_assistant.features.discover.domain.validation

import com.example.health_assistant.features.discover.domain.model.ContentCredibilityLevel
import com.example.health_assistant.features.discover.domain.model.DiscoverContent
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

/**
 * Comprehensive test suite for ContentCredibilityValidator
 * Tests credibility scoring algorithms and warning generation
 */
class ContentCredibilityValidatorTest {

    private lateinit var validator: ContentCredibilityValidator
    
    @Before
    fun setUp() {
        validator = ContentCredibilityValidator()
    }

    // Article Credibility Tests
    
    @Test
    fun `validateArticleCredibility returns high score for peer-reviewed content`() {
        // Given
        val article = createTestArticle(
            credibilityScore = 4,
            sourceUrl = "https://pubmed.ncbi.nlm.nih.gov/12345",
            authorCredentials = "Dr. John Smith, M.D., Ph.D.",
            content = "This peer-reviewed study demonstrates...",
            lastUpdated = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30)
        )
        
        // When
        val result = validator.validateArticleCredibility(article)
        
        // Then
        assertTrue("Article should be credible", result.isCredible)
        assertEquals("Should have high credibility score", 5, result.credibilityScore)
        assertEquals("Should be peer-reviewed level", ContentCredibilityLevel.PEER_REVIEWED, result.credibilityLevel)
        assertTrue("Should have minimal warnings", result.warnings.isEmpty())
    }
    
    @Test
    fun `validateArticleCredibility penalizes outdated content`() {
        // Given - Article over 2 years old
        val article = createTestArticle(
            credibilityScore = 4,
            lastUpdated = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(800)
        )
        
        // When
        val result = validator.validateArticleCredibility(article)
        
        // Then
        assertTrue("Should have outdated warning", 
            result.warnings.any { it.contains("over 2 years old") })
        assertTrue("Score should be reduced", result.credibilityScore < 4)
    }
    
    @Test
    fun `validateArticleCredibility warns about missing author credentials`() {
        // Given
        val article = createTestArticle(
            authorCredentials = "",
            credibilityScore = 3
        )
        
        // When
        val result = validator.validateArticleCredibility(article)
        
        // Then
        assertTrue("Should warn about missing credentials",
            result.warnings.any { it.contains("Author credentials not provided") })
        assertTrue("Score should be reduced", result.credibilityScore < 3)
    }
    
    @Test
    fun `validateArticleCredibility boosts score for trusted medical sources`() {
        // Given
        val article = createTestArticle(
            credibilityScore = 3,
            sourceUrl = "https://www.mayoclinic.org/health-article"
        )
        
        // When
        val result = validator.validateArticleCredibility(article)
        
        // Then
        assertTrue("Score should be boosted", result.credibilityScore > 3)
    }
    
    @Test
    fun `validateArticleCredibility validates reading time accuracy`() {
        // Given - Short content with unrealistic reading time
        val shortContent = "This is a very short article with minimal content."
        val article = createTestArticle(
            content = shortContent,
            readingTimeMinutes = 15 // Unrealistic for short content
        )
        
        // When
        val result = validator.validateArticleCredibility(article)
        
        // Then
        assertTrue("Should warn about inaccurate reading time",
            result.warnings.any { it.contains("Reading time estimate may be inaccurate") })
    }

    // News Credibility Tests
    
    @Test
    fun `validateNewsCredibility assigns correct score based on source credibility`() {
        // Given
        val peerReviewedNews = createTestNews(sourceCredibility = "peer-reviewed")
        val generalNews = createTestNews(sourceCredibility = "general")
        
        // When
        val peerReviewedResult = validator.validateNewsCredibility(peerReviewedNews)
        val generalResult = validator.validateNewsCredibility(generalNews)
        
        // Then
        assertEquals("Peer-reviewed should get max score", 5, peerReviewedResult.credibilityScore)
        assertEquals("General news should get lower score", 2, generalResult.credibilityScore)
        assertTrue("Peer-reviewed should be credible", peerReviewedResult.isCredible)
        assertFalse("General news should not be credible", generalResult.isCredible)
    }
    
    @Test
    fun `validateNewsCredibility warns about old news items`() {
        // Given - News over a month old
        val oldNews = createTestNews(
            publishedDate = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(35),
            isBreakingNews = false
        )
        
        // When
        val result = validator.validateNewsCredibility(oldNews)
        
        // Then
        assertTrue("Should warn about old news",
            result.warnings.any { it.contains("over a month old") })
    }
    
    @Test
    fun `validateNewsCredibility handles breaking news appropriately`() {
        // Given - Old breaking news
        val oldBreakingNews = createTestNews(
            publishedDate = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(10),
            isBreakingNews = true
        )
        
        // When
        val result = validator.validateNewsCredibility(oldBreakingNews)
        
        // Then
        assertTrue("Should warn about outdated breaking news tag",
            result.warnings.any { it.contains("Breaking news tag may be outdated") })
    }
    
    @Test
    fun `validateNewsCredibility boosts score for trusted publications`() {
        // Given
        val trustedNews = createTestNews(
            sourcePublication = "Reuters Health",
            sourceCredibility = "health-publication"
        )
        
        // When
        val result = validator.validateNewsCredibility(trustedNews)
        
        // Then
        assertTrue("Score should be boosted for trusted publication", 
            result.credibilityScore >= 3)
    }

    // Video Credibility Tests
    
    @Test
    fun `validateVideoCredibility scores based on expert credentials`() {
        // Given
        val expertVideo = createTestVideo(expertCredentials = "Dr. Sarah Johnson, M.D., Cardiologist")
        val unknownVideo = createTestVideo(expertCredentials = "")
        
        // When
        val expertResult = validator.validateVideoCredibility(expertVideo)
        val unknownResult = validator.validateVideoCredibility(unknownVideo)
        
        // Then
        assertTrue("Expert video should have higher score", 
            expertResult.credibilityScore > unknownResult.credibilityScore)
        assertTrue("Expert video should be credible", expertResult.isCredible)
        assertTrue("Unknown expert should have warning",
            unknownResult.warnings.any { it.contains("Expert credentials not provided") })
    }
    
    @Test
    fun `validateVideoCredibility penalizes old videos`() {
        // Given - Video over a year old
        val oldVideo = createTestVideo(
            publishedDate = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(400)
        )
        
        // When
        val result = validator.validateVideoCredibility(oldVideo)
        
        // Then
        assertTrue("Should warn about old video",
            result.warnings.any { it.contains("over a year old") })
    }
    
    @Test
    fun `validateVideoCredibility rewards transcript availability`() {
        // Given
        val transcriptVideo = createTestVideo(transcriptAvailable = true)
        val noTranscriptVideo = createTestVideo(transcriptAvailable = false)
        
        // When
        val transcriptResult = validator.validateVideoCredibility(transcriptVideo)
        val noTranscriptResult = validator.validateVideoCredibility(noTranscriptVideo)
        
        // Then
        assertTrue("Transcript video should have higher score",
            transcriptResult.credibilityScore > noTranscriptResult.credibilityScore)
        assertTrue("No transcript should have accessibility warning",
            noTranscriptResult.warnings.any { it.contains("No transcript available") })
    }
    
    @Test
    fun `validateVideoCredibility validates duration reasonableness`() {
        // Given
        val veryShortVideo = createTestVideo(durationSeconds = 30)
        val veryLongVideo = createTestVideo(durationSeconds = 4000)
        
        // When
        val shortResult = validator.validateVideoCredibility(veryShortVideo)
        val longResult = validator.validateVideoCredibility(veryLongVideo)
        
        // Then
        assertTrue("Short video should have warning",
            shortResult.warnings.any { it.contains("Very short video") })
        assertTrue("Long video should have warning",
            longResult.warnings.any { it.contains("Very long video") })
    }

    // Edge Cases and Boundary Tests
    
    @Test
    fun `credibility scores are clamped to valid range`() {
        // Given - Article with extreme base score
        val highScoreArticle = createTestArticle(credibilityScore = 10)
        val lowScoreArticle = createTestArticle(credibilityScore = -5)
        
        // When
        val highResult = validator.validateArticleCredibility(highScoreArticle)
        val lowResult = validator.validateArticleCredibility(lowScoreArticle)
        
        // Then
        assertTrue("High score should be clamped to max", highResult.credibilityScore <= 5)
        assertTrue("Low score should be clamped to min", lowResult.credibilityScore >= 1)
    }
    
    @Test
    fun `validation handles empty and null content gracefully`() {
        // Given
        val emptyContentArticle = createTestArticle(content = "")
        val emptyTitleArticle = createTestArticle(title = "")
        
        // When & Then - Should not throw exceptions
        assertNotNull("Should handle empty content", 
            validator.validateArticleCredibility(emptyContentArticle))
        assertNotNull("Should handle empty title",
            validator.validateArticleCredibility(emptyTitleArticle))
    }
    
    @Test
    fun `validation results include all required fields`() {
        // Given
        val article = createTestArticle()
        
        // When
        val result = validator.validateArticleCredibility(article)
        
        // Then
        assertNotNull("Content ID should be set", result.contentId)
        assertEquals("Content type should be article", "article", result.contentType)
        assertTrue("Credibility score should be in valid range", 
            result.credibilityScore in 1..5)
        assertNotNull("Credibility level should be set", result.credibilityLevel)
        assertNotNull("Warnings list should be initialized", result.warnings)
        assertTrue("Last validated should be recent", 
            result.lastValidated > System.currentTimeMillis() - 1000)
        assertNotNull("Validation notes should be provided", result.validationNotes)
    }

    // Helper Methods for Test Data Creation
    
    private fun createTestArticle(
        id: String = "test-article-1",
        title: String = "Test Health Article",
        publishedDate: Long = System.currentTimeMillis(),
        category: String = "nutrition",
        imageUrl: String? = "https://example.com/image.jpg",
        userId: String = "test-user",
        summary: String = "Test article summary",
        content: String = "This is a comprehensive test article about health topics. It contains detailed information and research findings.",
        authorName: String = "Dr. Test Author",
        authorCredentials: String = "M.D., Ph.D.",
        sourceUrl: String = "https://example.com/article",
        lastUpdated: Long = System.currentTimeMillis(),
        readingTimeMinutes: Int = 5,
        tags: List<String> = listOf("health", "nutrition"),
        isBookmarked: Boolean = false,
        readProgress: Float = 0f,
        credibilityScore: Int = 3
    ): DiscoverContent.Article {
        return DiscoverContent.Article(
            id = id,
            title = title,
            publishedDate = publishedDate,
            category = category,
            imageUrl = imageUrl,
            userId = userId,
            summary = summary,
            content = content,
            authorName = authorName,
            authorCredentials = authorCredentials,
            sourceUrl = sourceUrl,
            lastUpdated = lastUpdated,
            readingTimeMinutes = readingTimeMinutes,
            tags = tags,
            isBookmarked = isBookmarked,
            readProgress = readProgress,
            credibilityScore = credibilityScore
        )
    }
    
    private fun createTestNews(
        id: String = "test-news-1",
        title: String = "Test Health News",
        publishedDate: Long = System.currentTimeMillis(),
        category: String = "medical-news",
        imageUrl: String? = "https://example.com/news-image.jpg",
        userId: String = "test-user",
        summary: String = "Test news summary",
        fullContent: String? = "Full news content here",
        sourcePublication: String = "Test Health Publication",
        sourceCredibility: String = "health-publication",
        externalUrl: String = "https://example.com/news",
        isBreakingNews: Boolean = false,
        relevanceScore: Int = 3
    ): DiscoverContent.News {
        return DiscoverContent.News(
            id = id,
            title = title,
            publishedDate = publishedDate,
            category = category,
            imageUrl = imageUrl,
            userId = userId,
            summary = summary,
            fullContent = fullContent,
            sourcePublication = sourcePublication,
            sourceCredibility = sourceCredibility,
            externalUrl = externalUrl,
            isBreakingNews = isBreakingNews,
            relevanceScore = relevanceScore
        )
    }
    
    private fun createTestVideo(
        id: String = "test-video-1",
        title: String = "Test Health Video",
        publishedDate: Long = System.currentTimeMillis(),
        category: String = "fitness",
        imageUrl: String? = "https://example.com/video-thumb.jpg",
        userId: String = "test-user",
        description: String = "Test video description",
        thumbnailUrl: String = "https://example.com/thumbnail.jpg",
        videoUrl: String = "https://example.com/video.mp4",
        durationSeconds: Int = 300,
        difficultyLevel: String = "beginner",
        expertName: String = "Dr. Video Expert",
        expertCredentials: String = "M.D., Exercise Physiologist",
        watchProgress: Float = 0f,
        isDownloadedOffline: Boolean = false,
        transcriptAvailable: Boolean = false
    ): DiscoverContent.Video {
        return DiscoverContent.Video(
            id = id,
            title = title,
            publishedDate = publishedDate,
            category = category,
            imageUrl = imageUrl,
            userId = userId,
            description = description,
            thumbnailUrl = thumbnailUrl,
            videoUrl = videoUrl,
            durationSeconds = durationSeconds,
            difficultyLevel = difficultyLevel,
            expertName = expertName,
            expertCredentials = expertCredentials,
            watchProgress = watchProgress,
            isDownloadedOffline = isDownloadedOffline,
            transcriptAvailable = transcriptAvailable
        )
    }
}