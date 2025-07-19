package com.example.health_assistant.features.discover.domain.model

import org.junit.Assert.*
import org.junit.Test

/**
 * Test suite for ContentValidationResult and related models
 * Tests model behavior and enum functionality
 */
class ContentValidationResultTest {

    @Test
    fun `ContentCredibilityLevel fromScore returns correct level`() {
        // Test all score mappings
        assertEquals(ContentCredibilityLevel.PEER_REVIEWED, ContentCredibilityLevel.fromScore(5))
        assertEquals(ContentCredibilityLevel.MEDICAL_JOURNAL, ContentCredibilityLevel.fromScore(4))
        assertEquals(ContentCredibilityLevel.HEALTH_ORGANIZATION, ContentCredibilityLevel.fromScore(4))
        assertEquals(ContentCredibilityLevel.MEDICAL_EXPERT, ContentCredibilityLevel.fromScore(3))
        assertEquals(ContentCredibilityLevel.HEALTH_PUBLICATION, ContentCredibilityLevel.fromScore(3))
        assertEquals(ContentCredibilityLevel.GENERAL_NEWS, ContentCredibilityLevel.fromScore(2))
        assertEquals(ContentCredibilityLevel.UNVERIFIED, ContentCredibilityLevel.fromScore(1))
    }

    @Test
    fun `ContentCredibilityLevel fromScore handles invalid scores`() {
        // Test boundary cases
        assertEquals(ContentCredibilityLevel.UNVERIFIED, ContentCredibilityLevel.fromScore(0))
        assertEquals(ContentCredibilityLevel.UNVERIFIED, ContentCredibilityLevel.fromScore(-1))
        assertEquals(ContentCredibilityLevel.UNVERIFIED, ContentCredibilityLevel.fromScore(6))
        assertEquals(ContentCredibilityLevel.UNVERIFIED, ContentCredibilityLevel.fromScore(100))
    }

    @Test
    fun `ContentCredibilityLevel isCredible returns correct values`() {
        // Test credibility threshold
        assertFalse("Score 1 should not be credible", ContentCredibilityLevel.isCredible(1))
        assertFalse("Score 2 should not be credible", ContentCredibilityLevel.isCredible(2))
        assertTrue("Score 3 should be credible", ContentCredibilityLevel.isCredible(3))
        assertTrue("Score 4 should be credible", ContentCredibilityLevel.isCredible(4))
        assertTrue("Score 5 should be credible", ContentCredibilityLevel.isCredible(5))
    }

    @Test
    fun `ContentCredibilityLevel getMinCredibleScore returns correct threshold`() {
        assertEquals("Minimum credible score should be 3", 3, ContentCredibilityLevel.getMinCredibleScore())
    }

    @Test
    fun `ContentCredibilityLevel enum values have correct properties`() {
        // Test PEER_REVIEWED
        val peerReviewed = ContentCredibilityLevel.PEER_REVIEWED
        assertEquals("Peer-Reviewed Study", peerReviewed.displayName)
        assertEquals(5, peerReviewed.score)
        assertEquals("Published in peer-reviewed medical journal", peerReviewed.description)

        // Test MEDICAL_JOURNAL
        val medicalJournal = ContentCredibilityLevel.MEDICAL_JOURNAL
        assertEquals("Medical Journal", medicalJournal.displayName)
        assertEquals(4, medicalJournal.score)
        assertEquals("Published in reputable medical journal", medicalJournal.description)

        // Test HEALTH_ORGANIZATION
        val healthOrg = ContentCredibilityLevel.HEALTH_ORGANIZATION
        assertEquals("Health Organization", healthOrg.displayName)
        assertEquals(4, healthOrg.score)
        assertEquals("From recognized health organization", healthOrg.description)

        // Test MEDICAL_EXPERT
        val medicalExpert = ContentCredibilityLevel.MEDICAL_EXPERT
        assertEquals("Medical Expert", medicalExpert.displayName)
        assertEquals(3, medicalExpert.score)
        assertEquals("Written by verified medical professional", medicalExpert.description)

        // Test HEALTH_PUBLICATION
        val healthPub = ContentCredibilityLevel.HEALTH_PUBLICATION
        assertEquals("Health Publication", healthPub.displayName)
        assertEquals(3, healthPub.score)
        assertEquals("From established health publication", healthPub.description)

        // Test GENERAL_NEWS
        val generalNews = ContentCredibilityLevel.GENERAL_NEWS
        assertEquals("General News", generalNews.displayName)
        assertEquals(2, generalNews.score)
        assertEquals("From general news source", generalNews.description)

        // Test UNVERIFIED
        val unverified = ContentCredibilityLevel.UNVERIFIED
        assertEquals("Unverified", unverified.displayName)
        assertEquals(1, unverified.score)
        assertEquals("Source credibility not verified", unverified.description)
    }

    @Test
    fun `ContentValidationResult data class works correctly`() {
        // Given
        val warnings = listOf("Test warning 1", "Test warning 2")
        val timestamp = System.currentTimeMillis()
        
        val result = ContentValidationResult(
            contentId = "test-content-123",
            contentType = "article",
            isCredible = true,
            credibilityScore = 4,
            credibilityLevel = ContentCredibilityLevel.MEDICAL_JOURNAL,
            warnings = warnings,
            lastValidated = timestamp,
            validationNotes = "Test validation notes"
        )

        // Then
        assertEquals("test-content-123", result.contentId)
        assertEquals("article", result.contentType)
        assertTrue(result.isCredible)
        assertEquals(4, result.credibilityScore)
        assertEquals(ContentCredibilityLevel.MEDICAL_JOURNAL, result.credibilityLevel)
        assertEquals(warnings, result.warnings)
        assertEquals(timestamp, result.lastValidated)
        assertEquals("Test validation notes", result.validationNotes)
    }

    @Test
    fun `ContentValidationResult handles null validation notes`() {
        // Given
        val result = ContentValidationResult(
            contentId = "test-content",
            contentType = "news",
            isCredible = false,
            credibilityScore = 2,
            credibilityLevel = ContentCredibilityLevel.GENERAL_NEWS,
            warnings = emptyList(),
            lastValidated = System.currentTimeMillis(),
            validationNotes = null
        )

        // Then
        assertNull("Validation notes should be null", result.validationNotes)
        assertNotNull("Other fields should be properly set", result.contentId)
    }

    @Test
    fun `ContentIssueReport data class works correctly`() {
        // Given
        val timestamp = System.currentTimeMillis()
        val report = ContentIssueReport(
            contentId = "problematic-content-456",
            contentType = "video",
            issueType = ContentIssueType.INCORRECT,
            description = "This video contains incorrect medical information",
            reportedBy = "user789",
            reportedAt = timestamp
        )

        // Then
        assertEquals("problematic-content-456", report.contentId)
        assertEquals("video", report.contentType)
        assertEquals(ContentIssueType.INCORRECT, report.issueType)
        assertEquals("This video contains incorrect medical information", report.description)
        assertEquals("user789", report.reportedBy)
        assertEquals(timestamp, report.reportedAt)
    }

    @Test
    fun `ContentIssueType enum has correct display names`() {
        assertEquals("Inappropriate Content", ContentIssueType.INAPPROPRIATE.displayName)
        assertEquals("Incorrect Information", ContentIssueType.INCORRECT.displayName)
        assertEquals("Outdated Information", ContentIssueType.OUTDATED.displayName)
        assertEquals("Misleading Claims", ContentIssueType.MISLEADING.displayName)
        assertEquals("Spam Content", ContentIssueType.SPAM.displayName)
        assertEquals("Copyright Violation", ContentIssueType.COPYRIGHT.displayName)
        assertEquals("Other Issue", ContentIssueType.OTHER.displayName)
    }

    @Test
    fun `ContentValidationResult equality and hashCode work correctly`() {
        // Given
        val timestamp = System.currentTimeMillis()
        val warnings = listOf("Warning 1")
        
        val result1 = ContentValidationResult(
            contentId = "test-123",
            contentType = "article",
            isCredible = true,
            credibilityScore = 4,
            credibilityLevel = ContentCredibilityLevel.MEDICAL_JOURNAL,
            warnings = warnings,
            lastValidated = timestamp,
            validationNotes = "Notes"
        )
        
        val result2 = ContentValidationResult(
            contentId = "test-123",
            contentType = "article",
            isCredible = true,
            credibilityScore = 4,
            credibilityLevel = ContentCredibilityLevel.MEDICAL_JOURNAL,
            warnings = warnings,
            lastValidated = timestamp,
            validationNotes = "Notes"
        )
        
        val result3 = ContentValidationResult(
            contentId = "test-456", // Different ID
            contentType = "article",
            isCredible = true,
            credibilityScore = 4,
            credibilityLevel = ContentCredibilityLevel.MEDICAL_JOURNAL,
            warnings = warnings,
            lastValidated = timestamp,
            validationNotes = "Notes"
        )

        // Then
        assertEquals("Identical results should be equal", result1, result2)
        assertEquals("Identical results should have same hash code", result1.hashCode(), result2.hashCode())
        assertNotEquals("Different results should not be equal", result1, result3)
    }

    @Test
    fun `ContentValidationResult toString contains relevant information`() {
        // Given
        val result = ContentValidationResult(
            contentId = "test-content",
            contentType = "article",
            isCredible = true,
            credibilityScore = 4,
            credibilityLevel = ContentCredibilityLevel.MEDICAL_JOURNAL,
            warnings = listOf("Test warning"),
            lastValidated = System.currentTimeMillis(),
            validationNotes = "Test notes"
        )

        // When
        val stringRepresentation = result.toString()

        // Then
        assertTrue("Should contain content ID", stringRepresentation.contains("test-content"))
        assertTrue("Should contain content type", stringRepresentation.contains("article"))
        assertTrue("Should contain credibility info", stringRepresentation.contains("true"))
    }
}