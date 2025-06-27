package com.example.health_assistant.features.profile.data

import com.example.health_assistant.data.repository.interfaces.UserProfile
import com.example.health_assistant.features.profile.state.Gender
import com.example.health_assistant.features.profile.state.ProfileData
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/**
 * Unit tests for ProfileFieldMapper
 * Tests data mapping and transformation logic
 */
@RunWith(JUnit4::class)
class ProfileFieldMapperTest {

    private val sampleUserProfile = UserProfile(
        userId = "test-user-id",
        email = "test@example.com",
        displayName = "Test User",
        photoUrl = "https://example.com/photo.jpg",
        birthday = "1990-05-15",
        gender = "Male",
        height = 175f,
        weight = 70f,
        isProfileComplete = true,
        createdAt = 1234567890L
    )

    private val sampleProfileData = ProfileData(
        userId = "test-user-id",
        email = "test@example.com",
        displayName = "Test User",
        photoUrl = "https://example.com/photo.jpg",
        birthday = "1990-05-15",
        gender = Gender.MALE,
        height = 175f,
        weight = 70f,
        isProfileComplete = true,
        createdAt = 1234567890L
    )

    @Test
    fun `mapUserProfileToProfileData should convert correctly`() {
        // When
        val result = ProfileFieldMapper.mapUserProfileToProfileData(sampleUserProfile)

        // Then
        assertEquals("Should map user ID", "test-user-id", result.userId)
        assertEquals("Should map email", "test@example.com", result.email)
        assertEquals("Should map display name", "Test User", result.displayName)
        assertEquals("Should map photo URL", "https://example.com/photo.jpg", result.photoUrl)
        assertEquals("Should map birthday", "1990-05-15", result.birthday)
        assertEquals("Should map gender", Gender.MALE, result.gender)
        assertEquals("Should map height", 175f, result.height)
        assertEquals("Should map weight", 70f, result.weight)
        assertEquals("Should map completion status", true, result.isProfileComplete)
        assertEquals("Should map creation time", 1234567890L, result.createdAt)
    }

    @Test
    fun `mapUserProfileToProfileData should handle null values correctly`() {
        // Given
        val userProfileWithNulls = UserProfile(
            userId = "test-user-id",
            email = "test@example.com",
            displayName = null,
            photoUrl = null,
            birthday = null,
            gender = null,
            height = null,
            weight = null,
            isProfileComplete = false,
            createdAt = 1234567890L
        )

        // When
        val result = ProfileFieldMapper.mapUserProfileToProfileData(userProfileWithNulls)

        // Then
        assertEquals("Should default display name to empty", "", result.displayName)
        assertNull("Should keep photo URL as null", result.photoUrl)
        assertNull("Should keep birthday as null", result.birthday)
        assertNull("Should keep gender as null", result.gender)
        assertNull("Should keep height as null", result.height)
        assertNull("Should keep weight as null", result.weight)
        assertFalse("Should map completion status", result.isProfileComplete)
    }

    @Test
    fun `mapUserProfileToProfileData should handle empty strings correctly`() {
        // Given
        val userProfileWithEmptyStrings = UserProfile(
            userId = "test-user-id",
            email = "test@example.com",
            displayName = "",
            photoUrl = "",
            birthday = "",
            gender = "",
            height = null,
            weight = null,
            isProfileComplete = false,
            createdAt = 1234567890L
        )

        // When
        val result = ProfileFieldMapper.mapUserProfileToProfileData(userProfileWithEmptyStrings)

        // Then
        assertEquals("Should keep empty display name as empty", "", result.displayName)
        assertNull("Should convert empty photo URL to null", result.photoUrl)
        assertNull("Should convert empty birthday to null", result.birthday)
        assertNull("Should convert empty gender to null", result.gender)
    }

    @Test
    fun `mapProfileDataToUserProfile should convert correctly`() {
        // When
        val result = ProfileFieldMapper.mapProfileDataToUserProfile(sampleProfileData)

        // Then
        assertEquals("Should map user ID", "test-user-id", result.userId)
        assertEquals("Should map email", "test@example.com", result.email)
        assertEquals("Should map display name", "Test User", result.displayName)
        assertEquals("Should map photo URL", "https://example.com/photo.jpg", result.photoUrl)
        assertEquals("Should map birthday", "1990-05-15", result.birthday)
        assertEquals("Should map gender", "Male", result.gender)
        assertEquals("Should map height", 175f, result.height)
        assertEquals("Should map weight", 70f, result.weight)
        assertEquals("Should map completion status", true, result.isProfileComplete)
        assertEquals("Should map creation time", 1234567890L, result.createdAt)
    }

    @Test
    fun `createDefaultProfileData should create reasonable defaults`() {
        // When
        val result = ProfileFieldMapper.createDefaultProfileData("user-123", "john.doe@example.com")

        // Then
        assertEquals("Should set user ID", "user-123", result.userId)
        assertEquals("Should set email", "john.doe@example.com", result.email)
        assertEquals("Should extract display name from email", "John Doe", result.displayName)
        assertNull("Should set photo URL to null", result.photoUrl)
        assertNull("Should set birthday to null", result.birthday)
        assertNull("Should set gender to null", result.gender)
        assertNull("Should set height to null", result.height)
        assertNull("Should set weight to null", result.weight)
        assertFalse("Should set profile as incomplete", result.isProfileComplete)
        assertTrue("Should set recent creation time", result.createdAt > 0)
    }

    @Test
    fun `createDefaultProfileData should handle various email formats`() {
        val testCases = mapOf(
            "john.doe@example.com" to "John Doe",
            "jane_smith@test.com" to "Jane Smith",
            "bob-johnson@domain.org" to "Bob Johnson",
            "simple@email.com" to "Simple",
            "test.user.name@long.domain.com" to "Test User Name",
            "noDot@email.com" to "Nodot"
        )

        testCases.forEach { (email, expectedName) ->
            // When
            val result = ProfileFieldMapper.createDefaultProfileData("user-id", email)

            // Then
            assertEquals("Should extract correct name from $email", expectedName, result.displayName)
        }
    }

    @Test
    fun `sanitizeFormInput should clean and validate input`() {
        // When
        val result = ProfileFieldMapper.sanitizeFormInput(
            displayName = "  John Doe  ",
            birthday = "1990-05-15",
            gender = "Male",
            height = "175.5",
            weight = "70"
        )

        // Then
        assertEquals("Should trim display name", "John Doe", result["displayName"])
        assertEquals("Should keep valid birthday", "1990-05-15", result["birthday"])
        assertEquals("Should keep valid gender", "Male", result["gender"])
        assertEquals("Should convert height to float", 175.5f, result["height"])
        assertEquals("Should convert weight to float", 70f, result["weight"])
    }

    @Test
    fun `sanitizeFormInput should filter invalid input`() {
        // When
        val result = ProfileFieldMapper.sanitizeFormInput(
            displayName = "",
            birthday = "invalid-date",
            gender = "",
            height = "1000", // Too high
            weight = "10"     // Too low
        )

        // Then
        assertNull("Should filter empty display name", result["displayName"])
        assertNull("Should filter invalid birthday", result["birthday"])
        assertNull("Should filter empty gender", result["gender"])
        assertNull("Should filter invalid height", result["height"])
        assertNull("Should filter invalid weight", result["weight"])
    }

    @Test
    fun `convertDisplayDateToIso should handle various formats`() {
        // When
        val result1 = ProfileFieldMapper.convertDisplayDateToIso("May 15, 1990")
        val result2 = ProfileFieldMapper.convertDisplayDateToIso("1990-05-15")
        val result3 = ProfileFieldMapper.convertDisplayDateToIso("invalid-date")

        // Then
        assertEquals("Should convert display format", "1990-05-15", result1)
        assertEquals("Should keep ISO format", "1990-05-15", result2)
        assertNull("Should return null for invalid", result3)
    }

    @Test
    fun `convertIsoDateToDisplay should format correctly`() {
        // When
        val result1 = ProfileFieldMapper.convertIsoDateToDisplay("1990-05-15")
        val result2 = ProfileFieldMapper.convertIsoDateToDisplay("invalid-date")

        // Then
        assertEquals("Should convert to display format", "May 15, 1990", result1)
        assertNull("Should return null for invalid", result2)
    }

    @Test
    fun `hasSignificantChanges should detect meaningful changes`() {
        val original = sampleProfileData
        val changedName = original.copy(displayName = "Changed Name")
        val changedHeight = original.copy(height = 180f)
        val identical = original.copy()

        // When/Then
        assertTrue("Should detect name change",
            ProfileFieldMapper.hasSignificantChanges(original, changedName))
        assertTrue("Should detect height change",
            ProfileFieldMapper.hasSignificantChanges(original, changedHeight))
        assertFalse("Should not detect change in identical profiles",
            ProfileFieldMapper.hasSignificantChanges(original, identical))
        assertFalse("Should handle null profiles",
            ProfileFieldMapper.hasSignificantChanges(null, original))
    }

    @Test
    fun `calculateProfileCompleteness should return correct percentage`() {
        // Complete profile
        val completeProfile = sampleProfileData
        val completePercentage = ProfileFieldMapper.calculateProfileCompleteness(completeProfile)
        assertEquals("Should return 100% for complete profile", 100, completePercentage)

        // Minimal profile (only display name)
        val minimalProfile = ProfileData(
            userId = "id",
            email = "email@test.com",
            displayName = "Name",
            photoUrl = null,
            birthday = null,
            gender = null,
            height = null,
            weight = null,
            isProfileComplete = false,
            createdAt = 0L
        )
        val minimalPercentage = ProfileFieldMapper.calculateProfileCompleteness(minimalProfile)
        assertEquals("Should return ~17% for minimal profile", 16, minimalPercentage)
    }

    @Test
    fun `getMissingFields should identify incomplete fields`() {
        // Given
        val incompleteProfile = ProfileData(
            userId = "id",
            email = "email@test.com",
            displayName = "Name",
            photoUrl = null,
            birthday = null,
            gender = null,
            height = null,
            weight = null,
            isProfileComplete = false,
            createdAt = 0L
        )

        // When
        val missingFields = ProfileFieldMapper.getMissingFields(incompleteProfile)

        // Then
        assertEquals("Should identify 5 missing fields", 5, missingFields.size)
        assertTrue("Should include Profile Photo", missingFields.contains("Profile Photo"))
        assertTrue("Should include Birthday", missingFields.contains("Birthday"))
        assertTrue("Should include Gender", missingFields.contains("Gender"))
        assertTrue("Should include Height", missingFields.contains("Height"))
        assertTrue("Should include Weight", missingFields.contains("Weight"))
        assertFalse("Should not include Display Name", missingFields.contains("Display Name"))
    }
}