package com.example.health_assistant.features.profile.domain

import kotlinx.coroutines.flow.Flow

/**
 * Domain model for profile image data
 * Clean architecture domain representation
 */
data class ProfileImage(
    val id: Long = 0,
    val imagePath: String,
    val fileName: String,
    val fileSize: Long,
    val mimeType: String = "image/jpeg",
    val dateCreated: Long = System.currentTimeMillis(),
    val dateModified: Long = System.currentTimeMillis(),
    val isActive: Boolean = true,
    val width: Int? = null,
    val height: Int? = null,
    val description: String = "",
    val compressionQuality: Int = 85
)

/**
 * Repository interface for profile image operations
 */
interface ProfileImageRepository {
    fun getAllProfileImages(): Flow<List<ProfileImage>>
    fun getCurrentProfileImage(): Flow<ProfileImage?>

    suspend fun getCurrentProfileImageSync(): ProfileImage?
    suspend fun getProfileImageById(id: Long): ProfileImage?
    suspend fun insertProfileImage(profileImage: ProfileImage): Long
    suspend fun updateProfileImage(profileImage: ProfileImage)
    suspend fun deleteProfileImage(profileImage: ProfileImage)
    suspend fun deleteProfileImageById(id: Long)
    suspend fun setActiveProfileImage(id: Long)
    suspend fun deleteInactiveProfileImages()
    suspend fun getProfileImageCount(): Int
}