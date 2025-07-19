package com.example.health_assistant.features.discover.domain.model

/**
 * Enum representing different levels of content credibility
 * Used for validating and scoring health information sources
 */
enum class ContentCredibility(val displayName: String, val score: Int, val description: String) {
    PEER_REVIEWED("Peer-Reviewed Study", 5, "Published in peer-reviewed medical journals"),
    MEDICAL_JOURNAL("Medical Journal", 4, "Published by established medical journals"),
    HEALTH_ORGANIZATION("Health Organization", 4, "From recognized health organizations"),
    MEDICAL_EXPERT("Medical Expert", 3, "Content by verified medical professionals"),
    HEALTH_PUBLICATION("Health Publication", 3, "From reputable health publications"),
    GENERAL_NEWS("General News", 2, "From general news sources"),
    UNVERIFIED("Unverified", 1, "Source credibility not verified");

    companion object {
        fun fromScore(score: Int): ContentCredibility {
            return values().find { it.score == score } ?: UNVERIFIED
        }

        fun fromString(credibility: String): ContentCredibility {
            return when (credibility.lowercase()) {
                "peer-reviewed" -> PEER_REVIEWED
                "medical-journal" -> MEDICAL_JOURNAL
                "health-organization" -> HEALTH_ORGANIZATION
                "medical-expert" -> MEDICAL_EXPERT
                "health-publication" -> HEALTH_PUBLICATION
                "general-news" -> GENERAL_NEWS
                else -> UNVERIFIED
            }
        }
    }

    /**
     * Returns true if the credibility score meets the minimum threshold for reliable content
     */
    fun isReliable(): Boolean = score >= 3

    /**
     * Returns true if the content should show credibility warnings
     */
    fun requiresWarning(): Boolean = score < 3
}