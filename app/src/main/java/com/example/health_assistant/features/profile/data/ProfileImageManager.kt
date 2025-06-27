package com.example.health_assistant.features.profile.data

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.example.health_assistant.data.local.dao.ProfileImageDao
import com.example.health_assistant.data.local.entity.ProfileImageEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages profile image operations including local storage, caching, and cleanup
 * Ensures profile photos persist until app uninstall
 */
@Singleton
class ProfileImageManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val profileImageDao: ProfileImageDao
) {

    private val imageDirectory: File by lazy {
        File(context.filesDir, "profile_images").apply {
            if (!exists()) mkdirs()
        }
    }

    /**
     * Save profile image to local storage and database
     */
    suspend fun saveProfileImage(userId: String, imageUri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Delete existing image first
            deleteProfileImage(userId)

            // Copy image to app storage
            val localFile = copyImageToLocalStorage(imageUri)

            // Get image metadata
            val imageMetadata = getImageMetadata(imageUri)

            // Create database entity
            val profileImageEntity = ProfileImageEntity(
                userId = userId,
                originalUri = imageUri.toString(),
                localFilePath = localFile.absolutePath,
                fileName = localFile.name,
                mimeType = imageMetadata.mimeType,
                fileSize = localFile.length(),
                width = imageMetadata.width,
                height = imageMetadata.height
            )

            // Save to database
            profileImageDao.insertOrUpdateProfileImage(profileImageEntity)

            Result.Success(localFile.absolutePath)
        } catch (e: Exception) {
            Result.Error("Failed to save profile image: ${e.message}", e)
        }
    }

    /**
     * Get profile image file path for user
     */
    suspend fun getProfileImagePath(userId: String): String? = withContext(Dispatchers.IO) {
        try {
            val profileImage = profileImageDao.getProfileImage(userId)
            profileImage?.localFilePath?.let { path ->
                val file = File(path)
                if (file.exists()) path else null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Get profile image as Flow for reactive updates
     */
    fun getProfileImageFlow(userId: String): Flow<ProfileImageEntity?> {
        return profileImageDao.getProfileImageFlow(userId)
    }

    /**
     * Delete profile image for user
     */
    suspend fun deleteProfileImage(userId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            // Get existing image info
            val existingImage = profileImageDao.getProfileImage(userId)

            // Delete physical file
            existingImage?.localFilePath?.let { path ->
                val file = File(path)
                if (file.exists()) {
                    file.delete()
                }
            }

            // Delete from database
            profileImageDao.deleteProfileImage(userId)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Check if user has a profile image
     */
    suspend fun hasProfileImage(userId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val profileImage = profileImageDao.getProfileImage(userId)
            profileImage?.localFilePath?.let { path ->
                File(path).exists()
            } ?: false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get storage usage statistics
     */
    suspend fun getStorageStats(): StorageStats = withContext(Dispatchers.IO) {
        try {
            val totalSize = profileImageDao.getTotalStorageUsed() ?: 0L
            val imageCount = profileImageDao.getAllProfileImages().size
            StorageStats(
                totalSizeBytes = totalSize,
                imageCount = imageCount,
                averageSizeBytes = if (imageCount > 0) totalSize / imageCount else 0L
            )
        } catch (e: Exception) {
            StorageStats(0L, 0, 0L)
        }
    }

    /**
     * Clean up old or orphaned images
     */
    suspend fun cleanupStorage(maxAgeMillis: Long = 30L * 24 * 60 * 60 * 1000): CleanupResult = withContext(Dispatchers.IO) {
        try {
            val cutoffTime = System.currentTimeMillis() - maxAgeMillis

            // Get images to be deleted for stats
            val allImages = profileImageDao.getAllProfileImages()
            val imagesToDelete = allImages.filter { it.updatedAt < cutoffTime }

            // Delete old files
            var deletedFiles = 0
            var freedSpace = 0L

            imagesToDelete.forEach { image ->
                val file = File(image.localFilePath)
                if (file.exists()) {
                    freedSpace += file.length()
                    if (file.delete()) {
                        deletedFiles++
                    }
                }
            }

            // Delete from database
            profileImageDao.deleteOldProfileImages(cutoffTime)

            // Clean up orphaned files
            val orphanedFiles = cleanupOrphanedFiles()

            CleanupResult(
                deletedImageCount = deletedFiles,
                freedSpaceBytes = freedSpace,
                orphanedFilesDeleted = orphanedFiles
            )
        } catch (e: Exception) {
            CleanupResult(0, 0L, 0)
        }
    }

    /**
     * Copy image from URI to local app storage
     */
    private suspend fun copyImageToLocalStorage(imageUri: Uri): File = withContext(Dispatchers.IO) {
        val fileName = generateUniqueFileName()
        val localFile = File(imageDirectory, fileName)

        context.contentResolver.openInputStream(imageUri)?.use { inputStream ->
            FileOutputStream(localFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        } ?: throw IllegalStateException("Could not open input stream for image")

        localFile
    }

    /**
     * Get image metadata
     */
    private suspend fun getImageMetadata(imageUri: Uri): ImageMetadata = withContext(Dispatchers.IO) {
        try {
            val mimeType = context.contentResolver.getType(imageUri)

            context.contentResolver.openInputStream(imageUri)?.use { inputStream ->
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeStream(inputStream, null, options)

                ImageMetadata(
                    mimeType = mimeType,
                    width = options.outWidth.takeIf { it > 0 },
                    height = options.outHeight.takeIf { it > 0 }
                )
            } ?: ImageMetadata(mimeType, null, null)
        } catch (e: Exception) {
            ImageMetadata(null, null, null)
        }
    }

    /**
     * Generate unique filename for image
     */
    private fun generateUniqueFileName(): String {
        val timestamp = System.currentTimeMillis()
        val uuid = UUID.randomUUID().toString().take(8)
        return "profile_${timestamp}_${uuid}.jpg"
    }

    /**
     * Clean up orphaned files (files without database entries)
     */
    private suspend fun cleanupOrphanedFiles(): Int = withContext(Dispatchers.IO) {
        try {
            val databaseFiles = profileImageDao.getAllProfileImages()
                .mapNotNull { it.localFilePath }
                .map { File(it).name }
                .toSet()

            val actualFiles = imageDirectory.listFiles() ?: return@withContext 0
            var deletedCount = 0

            actualFiles.forEach { file ->
                if (file.isFile && !databaseFiles.contains(file.name)) {
                    if (file.delete()) {
                        deletedCount++
                    }
                }
            }

            deletedCount
        } catch (e: Exception) {
            0
        }
    }

    /**
     * Data classes for results and metadata
     */
    data class ImageMetadata(
        val mimeType: String?,
        val width: Int?,
        val height: Int?
    )

    data class StorageStats(
        val totalSizeBytes: Long,
        val imageCount: Int,
        val averageSizeBytes: Long
    )

    data class CleanupResult(
        val deletedImageCount: Int,
        val freedSpaceBytes: Long,
        val orphanedFilesDeleted: Int
    )

    /**
     * Result sealed class for operations
     */
    sealed class Result<out T> {
        data class Success<T>(val data: T) : Result<T>()
        data class Error(val message: String, val exception: Throwable? = null) : Result<Nothing>()
    }
}