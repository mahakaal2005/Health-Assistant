package com.example.health_assistant.features.discover.domain.validation

import com.example.health_assistant.features.discover.domain.model.ContentCredibilityLevel
import com.example.health_assistant.features.discover.domain.model.DiscoverContent
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

/**
 * Integration tests for ContentCredibilityValidator
 * Tests real-world scenarios and complex validation cases
 */
class ContentCredibilityIntegrationTest {

    private lateinit var validator: ContentCredibilityValidator
    
    @Before
    fun setUp() {
        validator = ContentCredibilityValidator()
    }

    @Test
    fun `validates real-world medical journal article`() {
        // Given - Realistic medical journal article
        val article = DiscoverContent.Article(
            id = "nejm-2024-001",
            title = "Efficacy of Novel Cardiovascular Treatment: A Randomized Controlled Trial",
            publishedDate = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30),
            category = "cardiovascular",
            imageUrl = "https://nejm.org/images/article-001.jpg",
            userId = "user123",
            summary = "A comprehensive study examining the efficacy of a novel cardiovascular treatment in a randomized controlled trial setting.",
            content = "Background: Cardiovascular disease remains a leading cause of mortality worldwide. This peer-reviewed study presents findings from a randomized controlled trial examining the efficacy of a novel treatment approach. Methods: We conducted a double-blind, placebo-controlled study with 1,200 participants over 24 months. Results: The treatment group showed significant improvement in cardiovascular outcomes compared to placebo (p<0.001). Conclusion: This peer-reviewed research demonstrates the potential for improved patient outcomes through evidence-based treatment protocols.",
            authorName = "Dr. Sarah Mitchell",
            authorCredentials = "M.D., Ph.D., Professor of Cardiology, Harvard Medical School",
            sourceUrl = "https://nejm.org/doi/10.1056/NEJMoa2024001",
            lastUpdated = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(15),
            readingTimeMinutes = 12,
            tags = listOf("cardiovascular", "clinical-trial", "peer-reviewed", "cardiology"),
            isBookmarked = false,
            readProgress = 0f,
            credibilityScore = 4
        )
        
        // When
        val result = validator.validateArticleCredibility(article)
        
        // Then
        assertTrue("Medical journal article should be highly credible", result.isCredible)
        assertEquals("Should achieve maximum credibility score", 5, result.credibilityScore)
        assertEquals("Should be classified as peer-reviewed", ContentCredibilityLevel.PEER_REVIEWED, result.credibilityLevel)
        assertTrue("Should have minimal warnings for high-quality content", result.warnings.size <= 1)
        assertNotNull("Should provide validation notes", result.validationNotes)
        assertTrue("Validation notes should mention peer-reviewed quality", 
            result.validationNotes?.contains("peer-reviewed") == true)
    }

    @Test
    fun `validates questionable health blog post`() {
        // Given - Low-quality blog post with red flags
        val article = DiscoverContent.Article(
            id = "blog-2020-suspicious",
            title = "Miracle Cure for All Diseases - Doctors Hate This One Trick!",
            publishedDate = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1200), // Over 3 years old
            category = "alternative-medicine",
            imageUrl = "https://sketchy-health-blog.com/miracle-cure.jpg",
            userId = "user456",
            summary = "Discover the amazing secret that pharmaceutical companies don't want you to know!",
            content = "This revolutionary treatment will cure everything! No side effects, guaranteed results! Testimonials from satisfied customers prove its effectiveness.",
            authorName = "Health Guru Bob",
            authorCredentials = "", // No credentials
            sourceUrl = "https://sketchy-health-blog.com/miracle-cure-article",
            lastUpdated = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(800), // Very outdated
            readingTimeMinutes = 3,
            tags = listOf("miracle", "cure", "alternative"),
            isBookmarked = false,
            readProgress = 0f,
            credibilityScore = 1 // Already low base score
        )
        
        // When
        val result = validator.validateArticleCredibility(article)
        
        // Then
        assertFalse("Questionable content should not be credible", result.isCredible)
        assertEquals("Should have minimum credibility score", 1, result.credibilityScore)
        assertEquals("Should be classified as unverified", ContentCredibilityLevel.UNVERIFIED, result.credibilityLevel)
        assertTrue("Should have multiple warnings", result.warnings.size >= 2)
        assertTrue("Should warn about outdated content", 
            result.warnings.any { it.contains("over 2 years old") })
        assertTrue("Should warn about missing credentials",
            result.warnings.any { it.contains("Author credentials not provided") })
    }

    @Test
    fun `validates breaking health news from reputable source`() {
        // Given - Recent breaking news from trusted source
        val news = DiscoverContent.News(
            id = "reuters-breaking-2024",
            title = "WHO Declares New Health Emergency: Global Response Coordinated",
            publishedDate = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(6),
            category = "public-health",
            imageUrl = "https://reuters.com/health-emergency.jpg",
            userId = "user789",
            summary = "World Health Organization announces coordinated global response to emerging health threat.",
            fullContent = "The World Health Organization today declared a public health emergency following reports of a new infectious disease outbreak. Health officials are coordinating international response efforts.",
            sourcePublication = "Reuters Health",
            sourceCredibility = "health-organization",
            externalUrl = "https://reuters.com/health/who-declares-emergency-2024",
            isBreakingNews = true,
            relevanceScore = 5
        )
        
        // When
        val result = validator.validateNewsCredibility(news)
        
        // Then
        assertTrue("Breaking news from reputable source should be credible", result.isCredible)
        assertTrue("Should have high credibility score", result.credibilityScore >= 4)
        assertTrue("Should have no warnings for recent breaking news", result.warnings.isEmpty())
        assertEquals("Should be classified appropriately", ContentCredibilityLevel.HEALTH_ORGANIZATION, result.credibilityLevel)
    }

    @Test
    fun `validates educational video from medical expert`() {
        // Given - High-quality educational video
        val video = DiscoverContent.Video(
            id = "mayo-clinic-video-2024",
            title = "Understanding Heart Disease: Prevention and Treatment Options",
            publishedDate = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(60),
            category = "cardiovascular",
            imageUrl = "https://mayoclinic.org/video-thumbnails/heart-disease.jpg",
            userId = "user101",
            description = "Comprehensive overview of heart disease prevention and treatment options, presented by leading cardiologists from Mayo Clinic.",
            thumbnailUrl = "https://mayoclinic.org/thumbnails/heart-disease-thumb.jpg",
            videoUrl = "https://mayoclinic.org/videos/heart-disease-education.mp4",
            durationSeconds = 1800, // 30 minutes - reasonable length
            difficultyLevel = "intermediate",
            expertName = "Dr. Michael Rodriguez",
            expertCredentials = "M.D., FACC, Director of Preventive Cardiology, Mayo Clinic",
            watchProgress = 0f,
            isDownloadedOffline = false,
            transcriptAvailable = true
        )
        
        // When
        val result = validator.validateVideoCredibility(video)
        
        // Then
        assertTrue("Expert medical video should be credible", result.isCredible)
        assertTrue("Should have high credibility score", result.credibilityScore >= 4)
        assertTrue("Should have minimal warnings", result.warnings.size <= 1)
        assertNotNull("Should provide validation notes", result.validationNotes)
    }

    @Test
    fun `validates mixed content batch for consistency`() {
        // Given - Mixed batch of content with varying quality
        val highQualityArticle = createHighQualityArticle()
        val lowQualityArticle = createLowQualityArticle()
        val reputableNews = createReputableNews()
        val questionableNews = createQuestionableNews()
        val expertVideo = createExpertVideo()
        val amateurVideo = createAmateurVideo()
        
        // When
        val results = listOf(
            validator.validateArticleCredibility(highQualityArticle),
            validator.validateArticleCredibility(lowQualityArticle),
            validator.validateNewsCredibility(reputableNews),
            validator.validateNewsCredibility(questionableNews),
            validator.validateVideoCredibility(expertVideo),
            validator.validateVideoCredibility(amateurVideo)
        )
        
        // Then
        val credibleCount = results.count { it.isCredible }
        val nonCredibleCount = results.count { !it.isCredible }
        
        assertEquals("Should have 3 credible items", 3, credibleCount)
        assertEquals("Should have 3 non-credible items", 3, nonCredibleCount)
        
        // Verify score distribution
        val scores = results.map { it.credibilityScore }
        assertTrue("Should have range of scores", scores.distinct().size > 1)
        assertTrue("All scores should be in valid range", scores.all { it in 1..5 })
    }

    @Test
    fun `validates content with special characters and unicode`() {
        // Given - Content with special characters
        val article = DiscoverContent.Article(
            id = "unicode-test",
            title = "Salud Cardiovascular: Prevención y Tratamiento (Español)",
            publishedDate = System.currentTimeMillis(),
            category = "cardiovascular",
            imageUrl = null,
            userId = "user-unicode",
            summary = "Artículo sobre salud cardiovascular en español con caracteres especiales: ñ, á, é, í, ó, ú",
            content = "Este artículo médico contiene información sobre prevención cardiovascular. Incluye datos científicos y recomendaciones médicas basadas en evidencia. Los símbolos médicos como ♥ y ℞ también están presentes.",
            authorName = "Dra. María José González-Pérez",
            authorCredentials = "M.D., Ph.D., Cardióloga",
            sourceUrl = "https://revista-medica-española.es/articulo-123",
            lastUpdated = System.currentTimeMillis(),
            readingTimeMinutes = 8,
            tags = listOf("cardiovascular", "español", "prevención"),
            isBookmarked = false,
            readProgress = 0f,
            credibilityScore = 3
        )
        
        // When
        val result = validator.validateArticleCredibility(article)
        
        // Then
        assertNotNull("Should handle unicode content", result)
        assertTrue("Should process special characters correctly", result.credibilityScore > 0)
        assertFalse("Should not crash on unicode", result.warnings.any { it.contains("error") })
    }

    // Helper methods for creating test content
    
    private fun createHighQualityArticle() = DiscoverContent.Article(
        id = "high-quality", title = "Evidence-Based Treatment", publishedDate = System.currentTimeMillis(),
        category = "medicine", imageUrl = null, userId = "test", summary = "High quality summary",
        content = "Peer-reviewed research demonstrates effectiveness", authorName = "Dr. Expert",
        authorCredentials = "M.D., Ph.D.", sourceUrl = "https://pubmed.ncbi.nlm.nih.gov/123",
        lastUpdated = System.currentTimeMillis(), readingTimeMinutes = 10, tags = listOf("research"),
        isBookmarked = false, readProgress = 0f, credibilityScore = 4
    )
    
    private fun createLowQualityArticle() = DiscoverContent.Article(
        id = "low-quality", title = "Miracle Cure", publishedDate = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1000),
        category = "alternative", imageUrl = null, userId = "test", summary = "Questionable claims",
        content = "Amazing results guaranteed", authorName = "Health Blogger",
        authorCredentials = "", sourceUrl = "https://random-blog.com/cure",
        lastUpdated = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(500), readingTimeMinutes = 3, tags = listOf("miracle"),
        isBookmarked = false, readProgress = 0f, credibilityScore = 1
    )
    
    private fun createReputableNews() = DiscoverContent.News(
        id = "reputable-news", title = "Medical Breakthrough", publishedDate = System.currentTimeMillis(),
        category = "research", imageUrl = null, userId = "test", summary = "Scientific discovery",
        fullContent = "Researchers announce breakthrough", sourcePublication = "Reuters Health",
        sourceCredibility = "medical-journal", externalUrl = "https://reuters.com/health/breakthrough",
        isBreakingNews = false, relevanceScore = 4
    )
    
    private fun createQuestionableNews() = DiscoverContent.News(
        id = "questionable-news", title = "Shocking Health Secret", publishedDate = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(45),
        category = "alternative", imageUrl = null, userId = "test", summary = "Sensational claims",
        fullContent = "Doctors shocked by this discovery", sourcePublication = "Health Gossip Weekly",
        sourceCredibility = "general", externalUrl = "https://gossip-health.com/shocking-secret",
        isBreakingNews = false, relevanceScore = 1
    )
    
    private fun createExpertVideo() = DiscoverContent.Video(
        id = "expert-video", title = "Medical Education", publishedDate = System.currentTimeMillis(),
        category = "education", imageUrl = null, userId = "test", description = "Expert explanation",
        thumbnailUrl = "thumb.jpg", videoUrl = "video.mp4", durationSeconds = 900,
        difficultyLevel = "intermediate", expertName = "Dr. Expert", expertCredentials = "M.D., Specialist",
        watchProgress = 0f, isDownloadedOffline = false, transcriptAvailable = true
    )
    
    private fun createAmateurVideo() = DiscoverContent.Video(
        id = "amateur-video", title = "Health Tips", publishedDate = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(400),
        category = "tips", imageUrl = null, userId = "test", description = "Personal experience",
        thumbnailUrl = "thumb.jpg", videoUrl = "video.mp4", durationSeconds = 120,
        difficultyLevel = "beginner", expertName = "Fitness Enthusiast", expertCredentials = "",
        watchProgress = 0f, isDownloadedOffline = false, transcriptAvailable = false
    )
}