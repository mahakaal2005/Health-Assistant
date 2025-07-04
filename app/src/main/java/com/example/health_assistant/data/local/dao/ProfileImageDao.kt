package com.example.health_assistant.data.local.dao

import androidx.room.*
import com.example.health_assistant.data.local.entity.ProfileImageEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for profile image operations
 * Handles profile photo storage and retrieval
 */
@Dao
interface ProfileImageDao {

    @Query("SELECT * FROM profile_images ORDER BY dateCreated DESC")
    fun getAllProfileImages(): Flow<List<ProfileImageEntity>>

    @Query("SELECT * FROM profile_images WHERE isActive = 1 LIMIT 1")
    suspend fun getCurrentProfileImage(): ProfileImageEntity?

    @Query("SELECT * FROM profile_images WHERE isActive = 1 LIMIT 1")
    fun getCurrentProfileImageFlow(): Flow<ProfileImageEntity?>

    @Query("SELECT * FROM profile_images WHERE id = :id")
    suspend fun getProfileImageById(id: Long): ProfileImageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfileImage(profileImage: ProfileImageEntity): Long

    @Update
    suspend fun updateProfileImage(profileImage: ProfileImageEntity)

    @Delete
    suspend fun deleteProfileImage(profileImage: ProfileImageEntity)

    @Query("DELETE FROM profile_images WHERE id = :id")
    suspend fun deleteProfileImageById(id: Long)

    @Query("UPDATE profile_images SET isActive = 0")
    suspend fun deactivateAllProfileImages()

    @Query("UPDATE profile_images SET isActive = 1 WHERE id = :id")
    suspend fun setActiveProfileImage(id: Long)

    @Query("DELETE FROM profile_images WHERE isActive = 0")
    suspend fun deleteInactiveProfileImages()

    @Query("SELECT COUNT(*) FROM profile_images")
    suspend fun getProfileImageCount(): Int

    @Query("DELETE FROM profile_images")
    suspend fun deleteAllProfileImages()
}