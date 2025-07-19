package com.example.health_assistant.features.discover.domain.model

/**
 * Data class representing content validation and credibility assessment results
 */
data class ContentValidationResult(
    val contentId: String,
    val contentType: String,
    val isCredible: Boolean,
    val credibilityScore: Int, // 1-5 scale
    val credibilityLevel: ContentCredibilityLevel,
    val warnings: List<String>,
    val lastValidated: Long,
    val validationNotes: String? = null
)

/**
 * Enum representing different levels of content credibility
 */
enum class ContentCredibilityLevel(val displayName: String, val score: Int, val description: String) {
    PEER_REVIEWED("Peer-Reviewed Study", 5, "Published in peer-reviewed medical journal"),
    MEDICAL_JOURNAL("Medical Journal", 4, "Published in reputable medical journal"),
    HEALTH_ORGANIZATION("Health Organization", 4, "From recognized health organization"),
    MEDICAL_EXPERT("Medical Expert", 3, "Written by verified medical professional"),
    HEALTH_PUBLICATION("Health Publication", 3, "From established health publication"),
    GENERAL_NEWS("General News", 2, "From general news source"),
    UNVERIFIED("Unverified", 1, "Source credibility not verified");

    companion object {
        /**
         * Get credibility level by score
         */
        fun fromScore(score: Int): ContentCredibilityLevel {
            return values().find { it.score == score } ?: UNVERIFIED
        }

        /**
         * Get minimum credible score threshold
         */
        fun getMinCredibleScore(): Int = 3

        /**
         * Check if score represents credible content
         */
        fun isCredible(score: Int): Boolean = score >= getMinCredibleScore()
    }
}

/**
 * Data class for content issue reporting
 */
data class ContentIssueReport(
    val contentId: String,
    val contentType: String,
    val issueType: ContentIssueType,
    val description: String,
    val reportedBy: String,
    val reportedAt: Long
)

/**
 * Enum for different types of content issues
 */
enum class ContentIssueType(val displayName: String) {
    INAPPROPRIATE("Inappropriate Content"),
    INCORRECT("Incorrect Information"),
    OUTDATED("Outdated Information"),
    MISLEADING("Misleading Claims"),
    SPAM("Spam Content"),
    COPYRIGHT("Copyright Violation"),
    OTHER("Other Issue")
}