package com.example.health_assistant.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.example.health_assistant.data.local.entity.ProfileImageEntity

/**
 * Data Access Object for profile images
 * Provides CRUD operations for locally stored profile photos
 */
@Dao
interface ProfileImageDao {

    /**
     * Get profile image for a specific user
     */
    @Query("SELECT * FROM profile_images WHERE userId = :userId")
    suspend fun getProfileImage(userId: String): ProfileImageEntity?

    /**
     * Get profile image as Flow for reactive updates
     */
    @Query("SELECT * FROM profile_images WHERE userId = :userId")
    fun getProfileImageFlow(userId: String): Flow<ProfileImageEntity?>

    /**
     * Insert or update profile image
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProfileImage(profileImage: ProfileImageEntity)

    /**
     * Delete profile image for specific user
     */
    @Query("DELETE FROM profile_images WHERE userId = :userId")
    suspend fun deleteProfileImage(userId: String)

    /**
     * Delete all profile images (for app cleanup)
     */
    @Query("DELETE FROM profile_images")
    suspend fun deleteAllProfileImages()

    /**
     * Get all profile images (for maintenance/cleanup)
     */
    @Query("SELECT * FROM profile_images ORDER BY updatedAt DESC")
    suspend fun getAllProfileImages(): List<ProfileImageEntity>

    /**
     * Clean up old profile images based on age
     */
    @Query("DELETE FROM profile_images WHERE updatedAt < :cutoffTime")
    suspend fun deleteOldProfileImages(cutoffTime: Long)

    /**
     * Get total storage used by profile images
     */
    @Query("SELECT SUM(fileSize) FROM profile_images")
    suspend fun getTotalStorageUsed(): Long?
}
