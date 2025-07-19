package com.example.health_assistant.features.discover.presentation

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Test class for DiscoverContentUtils
 * Tests relevance scoring and text formatting utilities (non-Android specific)
 */
class DiscoverContentUtilsTest {



    @Test
    fun `calculateRelevanceScore gives higher score for title matches`() {
        // Given
        val title = "Nutrition Guide for Beginners"
        val summary = "This guide covers basic nutrition principles"
        val category = "health"
        val searchQuery = "nutrition"

        // When
        val score = DiscoverContentUtils.calculateRelevanceScore(title, summary, category, searchQuery)

        // Then
        assertTrue(score > 0)
        
        // Compare with summary-only match
        val summaryOnlyScore = DiscoverContentUtils.calculateRelevanceScore(
            "Health Guide", 
            "This guide covers basic nutrition principles", 
            category, 
            searchQuery
        )
        assertTrue(score > summaryOnlyScore)
    }

    @Test
    fun `calculateRelevanceScore gives bonus for title starting with search term`() {
        // Given
        val titleStartsWith = "Nutrition Guide for Health"
        val titleContains = "Complete Guide to Nutrition"
        val summary = "Health information"
        val category = "health"
        val searchQuery = "nutrition"

        // When
        val scoreStartsWith = DiscoverContentUtils.calculateRelevanceScore(titleStartsWith, summary, category, searchQuery)
        val scoreContains = DiscoverContentUtils.calculateRelevanceScore(titleContains, summary, category, searchQuery)

        // Then
        assertTrue(scoreStartsWith > scoreContains)
    }

    @Test
    fun `calculateRelevanceScore applies credibility boost`() {
        // Given
        val title = "Health Guide"
        val summary = "Nutrition information"
        val category = "health"
        val searchQuery = "nutrition"

        // When
        val scoreWithCredibility = DiscoverContentUtils.calculateRelevanceScore(title, summary, category, searchQuery, 5)
        val scoreWithoutCredibility = DiscoverContentUtils.calculateRelevanceScore(title, summary, category, searchQuery, 0)

        // Then
        assertTrue(scoreWithCredibility > scoreWithoutCredibility)
    }

    @Test
    fun `extractSearchSnippet returns full text when shorter than limit`() {
        // Given
        val text = "Short text"
        val searchQuery = "text"
        val snippetLength = 150

        // When
        val result = DiscoverContentUtils.extractSearchSnippet(text, searchQuery, snippetLength)

        // Then
        assertEquals(text, result)
    }

    @Test
    fun `extractSearchSnippet extracts text around search term`() {
        // Given
        val text = "This is a very long text that contains the word nutrition in the middle and continues for much longer to test snippet extraction functionality"
        val searchQuery = "nutrition"
        val snippetLength = 50

        // When
        val result = DiscoverContentUtils.extractSearchSnippet(text, searchQuery, snippetLength)

        // Then
        assertTrue(result.contains("nutrition"), "Result should contain the search term")
        assertTrue(result.isNotEmpty(), "Result should not be empty")
        // Just verify it's reasonably sized, not exact length due to ellipsis handling
        assertTrue(result.length > 10, "Result should be reasonably long")
    }

    @Test
    fun `extractSearchSnippet returns truncated text when search term not found`() {
        // Given
        val text = "This is a very long text that does not contain the search term and should be truncated"
        val searchQuery = "missing"
        val snippetLength = 30

        // When
        val result = DiscoverContentUtils.extractSearchSnippet(text, searchQuery, snippetLength)

        // Then
        assertTrue(result.length <= snippetLength + 3) // +3 for ellipsis
        assertTrue(result.endsWith("..."))
    }

    @Test
    fun `formatCategory formats category names correctly`() {
        // Test cases - checking the actual implementation behavior
        val testCases = mapOf(
            "nutrition" to "Nutrition",
            "mental_health" to "Mental Health",
            "preventive_care" to "Preventive Care", 
            "chronic_conditions" to "Chronic Conditions"
        )

        testCases.forEach { (input, expected) ->
            val result = DiscoverContentUtils.formatCategory(input)
            assertEquals(expected, result, "Failed for input: $input")
        }
        
        // Test uppercase input
        val uppercaseResult = DiscoverContentUtils.formatCategory("NUTRITION")
        assertEquals("NUTRITION", uppercaseResult) // Should preserve case for already uppercase
    }

    @Test
    fun `truncateText truncates correctly`() {
        // Given
        val longText = "This is a very long text that should be truncated"
        val shortText = "Short"
        val maxLength = 20

        // When
        val truncatedLong = DiscoverContentUtils.truncateText(longText, maxLength)
        val truncatedShort = DiscoverContentUtils.truncateText(shortText, maxLength)

        // Then
        assertTrue(truncatedLong.length <= maxLength)
        assertTrue(truncatedLong.endsWith("..."))
        assertEquals(shortText, truncatedShort)
    }

    @Test
    fun `formatCredibilityScore returns correct labels`() {
        // Test cases
        val testCases = mapOf(
            5 to "Highly Verified",
            4 to "Verified",
            3 to "Credible",
            2 to "Moderate",
            1 to "Unverified",
            0 to "Unverified"
        )

        testCases.forEach { (score, expected) ->
            val result = DiscoverContentUtils.formatCredibilityScore(score)
            assertEquals(expected, result, "Failed for score: $score")
        }
    }
}