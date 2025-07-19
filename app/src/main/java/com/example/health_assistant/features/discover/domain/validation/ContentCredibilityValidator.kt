package com.example.health_assistant.features.discover.domain.validation

import com.example.health_assistant.features.discover.domain.model.ContentCredibilityLevel
import com.example.health_assistant.features.discover.domain.model.ContentValidationResult
import com.example.health_assistant.features.discover.domain.model.DiscoverContent
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for validating content credibility and generating warnings
 * Implements business logic for content quality assessment
 */
@Singleton
class ContentCredibilityValidator @Inject constructor() {

    companion object {
        private const val OUTDATED_THRESHOLD_DAYS = 365L
        private const val VERY_OUTDATED_THRESHOLD_DAYS = 730L
        private const val MIN_CREDIBLE_SCORE = 3
        
        // Trusted source domains for credibility scoring
        private val TRUSTED_MEDICAL_DOMAINS = setOf(
            "pubmed.ncbi.nlm.nih.gov",
            "nejm.org",
            "bmj.com",
            "thelancet.com",
            "jamanetwork.com",
            "who.int",
            "cdc.gov",
            "nih.gov",
            "mayoclinic.org",
            "clevelandclinic.org",
            "webmd.com",
            "healthline.com"
        )
        
        private val PEER_REVIEWED_INDICATORS = setOf(
            "peer-reviewed",
            "peer reviewed",
            "journal",
            "study",
            "research",
            "clinical trial"
        )
    }

    /**
     * Validate article credibility and generate comprehensive assessment
     */
    fun validateArticleCredibility(article: DiscoverContent.Article): ContentValidationResult {
        val warnings = mutableListOf<String>()
        val credibilityScore = calculateArticleCredibilityScore(article, warnings)
        val credibilityLevel = ContentCredibilityLevel.fromScore(credibilityScore)
        
        return ContentValidationResult(
            contentId = article.id,
            contentType = "article",
            isCredible = ContentCredibilityLevel.isCredible(credibilityScore),
            credibilityScore = credibilityScore,
            credibilityLevel = credibilityLevel,
            warnings = warnings,
            lastValidated = System.currentTimeMillis(),
            validationNotes = generateValidationNotes(article, credibilityLevel)
        )
    }

    /**
     * Validate news credibility and generate comprehensive assessment
     */
    fun validateNewsCredibility(news: DiscoverContent.News): ContentValidationResult {
        val warnings = mutableListOf<String>()
        val credibilityScore = calculateNewsCredibilityScore(news, warnings)
        val credibilityLevel = ContentCredibilityLevel.fromScore(credibilityScore)
        
        return ContentValidationResult(
            contentId = news.id,
            contentType = "news",
            isCredible = ContentCredibilityLevel.isCredible(credibilityScore),
            credibilityScore = credibilityScore,
            credibilityLevel = credibilityLevel,
            warnings = warnings,
            lastValidated = System.currentTimeMillis(),
            validationNotes = generateValidationNotes(news, credibilityLevel)
        )
    }

    /**
     * Validate video credibility and generate comprehensive assessment
     */
    fun validateVideoCredibility(video: DiscoverContent.Video): ContentValidationResult {
        val warnings = mutableListOf<String>()
        val credibilityScore = calculateVideoCredibilityScore(video, warnings)
        val credibilityLevel = ContentCredibilityLevel.fromScore(credibilityScore)
        
        return ContentValidationResult(
            contentId = video.id,
            contentType = "video",
            isCredible = ContentCredibilityLevel.isCredible(credibilityScore),
            credibilityScore = credibilityScore,
            credibilityLevel = credibilityLevel,
            warnings = warnings,
            lastValidated = System.currentTimeMillis(),
            validationNotes = generateValidationNotes(video, credibilityLevel)
        )
    }

    /**
     * Calculate credibility score for articles
     */
    private fun calculateArticleCredibilityScore(
        article: DiscoverContent.Article,
        warnings: MutableList<String>
    ): Int {
        var score = article.credibilityScore
        
        // Check for outdated content
        val daysSinceUpdate = TimeUnit.MILLISECONDS.toDays(
            System.currentTimeMillis() - article.lastUpdated
        )
        
        when {
            daysSinceUpdate > VERY_OUTDATED_THRESHOLD_DAYS -> {
                warnings.add("This content is over 2 years old and may be outdated")
                score = maxOf(1, score - 2)
            }
            daysSinceUpdate > OUTDATED_THRESHOLD_DAYS -> {
                warnings.add("This content is over a year old")
                score = maxOf(1, score - 1)
            }
        }
        
        // Check source URL credibility
        if (isTrustedMedicalSource(article.sourceUrl)) {
            score = minOf(5, score + 1)
        }
        
        // Check author credentials
        if (hasVerifiedMedicalCredentials(article.authorCredentials)) {
            score = minOf(5, score + 1)
        } else if (article.authorCredentials.isBlank()) {
            warnings.add("Author credentials not provided")
            score = maxOf(1, score - 1)
        }
        
        // Check for peer-reviewed indicators
        if (isPeerReviewedContent(article.content + " " + article.sourceUrl)) {
            score = minOf(5, score + 1)
        }
        
        // Validate reading time reasonableness
        val estimatedReadingTime = estimateReadingTime(article.content)
        if (kotlin.math.abs(article.readingTimeMinutes - estimatedReadingTime) > 5) {
            warnings.add("Reading time estimate may be inaccurate")
        }
        
        return score.coerceIn(1, 5)
    }

    /**
     * Calculate credibility score for news items
     */
    private fun calculateNewsCredibilityScore(
        news: DiscoverContent.News,
        warnings: MutableList<String>
    ): Int {
        var score = when (news.sourceCredibility.lowercase()) {
            "peer-reviewed" -> 5
            "medical-journal" -> 4
            "health-organization" -> 4
            "medical-expert" -> 3
            "health-publication" -> 3
            else -> 2
        }
        
        // Check publication recency for news
        val daysSincePublished = TimeUnit.MILLISECONDS.toDays(
            System.currentTimeMillis() - news.publishedDate
        )
        
        if (daysSincePublished > 30 && !news.isBreakingNews) {
            warnings.add("This news item is over a month old")
        }
        
        // Check source publication credibility
        if (isTrustedNewsSource(news.sourcePublication)) {
            score = minOf(5, score + 1)
        }
        
        // Breaking news gets special handling
        if (news.isBreakingNews && daysSincePublished > 7) {
            warnings.add("Breaking news tag may be outdated")
        }
        
        // Check external URL credibility
        if (isTrustedMedicalSource(news.externalUrl)) {
            score = minOf(5, score + 1)
        }
        
        return score.coerceIn(1, 5)
    }

    /**
     * Calculate credibility score for videos
     */
    private fun calculateVideoCredibilityScore(
        video: DiscoverContent.Video,
        warnings: MutableList<String>
    ): Int {
        var score = 3 // Default score for videos
        
        // Check expert credentials
        if (hasVerifiedMedicalCredentials(video.expertCredentials)) {
            score = minOf(5, score + 2)
        } else if (video.expertCredentials.isBlank()) {
            warnings.add("Expert credentials not provided")
            score = maxOf(1, score - 1)
        }
        
        // Check video age
        val daysSincePublished = TimeUnit.MILLISECONDS.toDays(
            System.currentTimeMillis() - video.publishedDate
        )
        
        if (daysSincePublished > OUTDATED_THRESHOLD_DAYS) {
            warnings.add("This video is over a year old")
            score = maxOf(1, score - 1)
        }
        
        // Check if transcript is available (indicates higher quality)
        if (video.transcriptAvailable) {
            score = minOf(5, score + 1)
        } else {
            warnings.add("No transcript available for accessibility")
        }
        
        // Validate duration reasonableness
        if (video.durationSeconds < 60) {
            warnings.add("Very short video - may lack comprehensive information")
        } else if (video.durationSeconds > 3600) {
            warnings.add("Very long video - consider watching in segments")
        }
        
        return score.coerceIn(1, 5)
    }

    /**
     * Check if source URL is from a trusted medical domain
     */
    private fun isTrustedMedicalSource(url: String): Boolean {
        return TRUSTED_MEDICAL_DOMAINS.any { domain ->
            url.lowercase().contains(domain)
        }
    }

    /**
     * Check if source is a trusted news publication
     */
    private fun isTrustedNewsSource(publication: String): Boolean {
        val trustedPublications = setOf(
            "reuters health",
            "associated press",
            "bbc health",
            "cnn health",
            "npr health",
            "washington post health",
            "new york times health"
        )
        
        return trustedPublications.any { trusted ->
            publication.lowercase().contains(trusted)
        }
    }

    /**
     * Check if content has peer-reviewed indicators
     */
    private fun isPeerReviewedContent(text: String): Boolean {
        val lowerText = text.lowercase()
        return PEER_REVIEWED_INDICATORS.any { indicator ->
            lowerText.contains(indicator)
        }
    }

    /**
     * Check if author has verified medical credentials
     */
    private fun hasVerifiedMedicalCredentials(credentials: String): Boolean {
        val medicalCredentials = setOf(
            "md", "m.d.", "doctor", "dr.", "phd", "ph.d.",
            "rn", "r.n.", "nurse practitioner", "np",
            "physician assistant", "pa", "pharmd", "pharm.d."
        )
        
        val lowerCredentials = credentials.lowercase()
        return medicalCredentials.any { credential ->
            lowerCredentials.contains(credential)
        }
    }

    /**
     * Estimate reading time based on content length
     */
    private fun estimateReadingTime(content: String): Int {
        val wordsPerMinute = 200 // Average reading speed
        val wordCount = content.split("\\s+".toRegex()).size
        return maxOf(1, (wordCount / wordsPerMinute))
    }

    /**
     * Generate validation notes for content
     */
    private fun generateValidationNotes(content: DiscoverContent, credibilityLevel: ContentCredibilityLevel): String {
        return when (credibilityLevel) {
            ContentCredibilityLevel.PEER_REVIEWED -> "This content comes from peer-reviewed medical research and represents the highest quality of evidence-based information."
            ContentCredibilityLevel.MEDICAL_JOURNAL -> "This content is published in a reputable medical journal and follows established editorial standards."
            ContentCredibilityLevel.HEALTH_ORGANIZATION -> "This content comes from a recognized health organization with established medical expertise."
            ContentCredibilityLevel.MEDICAL_EXPERT -> "This content is authored by a verified medical professional with relevant credentials."
            ContentCredibilityLevel.HEALTH_PUBLICATION -> "This content comes from an established health publication with editorial oversight."
            ContentCredibilityLevel.GENERAL_NEWS -> "This content comes from a general news source. Consider verifying information with medical sources."
            ContentCredibilityLevel.UNVERIFIED -> "The credibility of this content source has not been verified. Use caution and consult healthcare professionals."
        }
    }
}