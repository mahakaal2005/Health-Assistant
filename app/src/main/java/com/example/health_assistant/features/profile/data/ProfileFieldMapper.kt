package com.example.health_assistant.features.profile.data

import com.example.health_assistant.data.repository.interfaces.UserProfile
import com.example.health_assistant.features.profile.state.Gender
import com.example.health_assistant.features.profile.state.ProfileData
import java.text.SimpleDateFormat
import java.util.*

/**
 * Handles complex mapping between different profile data representations
 * Ensures consistent null handling and data transformation across the app
 */
object ProfileFieldMapper {

    private val isoDateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    /**
     * Map UserProfile (repository format) to ProfileData (UI format)
     * Handles all null safety and type conversions
     */
    fun mapUserProfileToProfileData(userProfile: UserProfile): ProfileData {
        return ProfileData(
            userId = userProfile.userId,
            email = userProfile.email,
            displayName = userProfile.displayName ?: "",
            photoUrl = userProfile.photoUrl?.takeIf { it.isNotBlank() },
            birthday = userProfile.birthday?.takeIf { it.isNotBlank() },
            gender = userProfile.gender?.let { genderString ->
                // Handle both string and enum representations
                Gender.fromString(genderString)
            },
            height = userProfile.height,
            weight = userProfile.weight,
            isProfileComplete = userProfile.isProfileComplete,
            createdAt = userProfile.createdAt
        )
    }

    /**
     * Map ProfileData (UI format) to UserProfile (repository format)
     * Ensures all fields are properly formatted for Firestore storage
     */
    fun mapProfileDataToUserProfile(profileData: ProfileData): UserProfile {
        return UserProfile(
            userId = profileData.userId,
            email = profileData.email,
            displayName = profileData.displayName.takeIf { it.isNotBlank() },
            photoUrl = profileData.photoUrl?.takeIf { it.isNotBlank() },
            birthday = profileData.birthday?.takeIf { it.isNotBlank() },
            gender = profileData.gender?.displayName,
            height = profileData.height,
            weight = profileData.weight,
            isProfileComplete = profileData.isProfileComplete,
            createdAt = profileData.createdAt
        )
    }

    /**
     * Create default ProfileData for new users
     * Provides sensible defaults for all optional fields
     */
    fun createDefaultProfileData(userId: String, email: String): ProfileData {
        return ProfileData(
            userId = userId,
            email = email,
            displayName = extractDisplayNameFromEmail(email),
            photoUrl = null,
            birthday = null,
            gender = null,
            height = null,
            weight = null,
            isProfileComplete = false,
            createdAt = System.currentTimeMillis()
        )
    }

    /**
     * Validate and sanitize form input before saving
     * Returns cleaned data ready for storage
     */
    fun sanitizeFormInput(
        displayName: String,
        birthday: String?,
        gender: String?,
        height: String?,
        weight: String?
    ): Map<String, Any?> {
        return mapOf(
            "displayName" to displayName.trim().takeIf { it.isNotBlank() },
            "birthday" to birthday?.trim()?.takeIf { it.isNotBlank() && isValidDateFormat(it) },
            "gender" to gender?.trim()?.takeIf { it.isNotBlank() },
            "height" to height?.trim()?.toFloatOrNull()?.takeIf { it in 50f..300f },
            "weight" to weight?.trim()?.toFloatOrNull()?.takeIf { it in 20f..500f }
        )
    }

    /**
     * Extract display name from email address
     * Provides a reasonable default when no display name is provided
     */
    private fun extractDisplayNameFromEmail(email: String): String {
        return try {
            val localPart = email.substringBefore("@")
            // Convert common patterns to readable names
            localPart
                .replace(".", " ")
                .replace("_", " ")
                .replace("-", " ")
                .split(" ")
                .joinToString(" ") { word ->
                    word.lowercase().replaceFirstChar {
                        if (it.isLowerCase()) it.titlecase() else it.toString()
                    }
                }
                .trim()
        } catch (e: Exception) {
            "User" // Fallback default
        }
    }

    /**
     * Validate date format (YYYY-MM-DD)
     */
    private fun isValidDateFormat(dateString: String): Boolean {
        return try {
            isoDateFormatter.parse(dateString)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Convert display date to ISO format
     */
    fun convertDisplayDateToIso(displayDate: String): String? {
        return try {
            // Handle various display formats
            val displayFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            val date = displayFormatter.parse(displayDate)
            isoDateFormatter.format(date!!)
        } catch (e: Exception) {
            // Try direct parsing if already in ISO format
            if (isValidDateFormat(displayDate)) displayDate else null
        }
    }

    /**
     * Convert ISO date to display format
     */
    fun convertIsoDateToDisplay(isoDate: String): String? {
        return try {
            val date = isoDateFormatter.parse(isoDate)
            val displayFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            displayFormatter.format(date!!)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Check if profile data has meaningful changes
     * Used for determining if save button should be enabled
     */
    fun hasSignificantChanges(original: ProfileData?, current: ProfileData?): Boolean {
        if (original == null || current == null) return false

        return original.displayName != current.displayName ||
                original.birthday != current.birthday ||
                original.gender != current.gender ||
                original.height != current.height ||
                original.weight != current.weight ||
                original.photoUrl != current.photoUrl
    }

    /**
     * Calculate profile completion percentage
     * Helps users understand how complete their profile is
     */
    fun calculateProfileCompleteness(profileData: ProfileData): Int {
        var completedFields = 0
        val totalFields = 6

        // Required field
        if (profileData.displayName.isNotBlank()) completedFields++

        // Optional but recommended fields
        if (!profileData.photoUrl.isNullOrBlank()) completedFields++
        if (!profileData.birthday.isNullOrBlank()) completedFields++
        if (profileData.gender != null) completedFields++
        if (profileData.height != null) completedFields++
        if (profileData.weight != null) completedFields++

        return (completedFields * 100) / totalFields
    }

    /**
     * Get missing fields for profile completion
     * Provides actionable guidance to users
     */
    fun getMissingFields(profileData: ProfileData): List<String> {
        val missingFields = mutableListOf<String>()

        if (profileData.displayName.isBlank()) missingFields.add("Display Name")
        if (profileData.photoUrl.isNullOrBlank()) missingFields.add("Profile Photo")
        if (profileData.birthday.isNullOrBlank()) missingFields.add("Birthday")
        if (profileData.gender == null) missingFields.add("Gender")
        if (profileData.height == null) missingFields.add("Height")
        if (profileData.weight == null) missingFields.add("Weight")

        return missingFields
    }
}