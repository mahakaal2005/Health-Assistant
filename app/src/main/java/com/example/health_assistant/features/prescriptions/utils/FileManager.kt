package com.example.health_assistant.features.prescriptions.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * File manager for prescription images
 * Handles image storage, compression, and cleanup
 */
@Singleton
class FileManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val PRESCRIPTIONS_DIR = "prescriptions"
        private const val MAX_IMAGE_WIDTH = 1920
        private const val MAX_IMAGE_HEIGHT = 1080
        private const val COMPRESSION_QUALITY = 85
    }

    /**
     * Save and compress image from URI
     */
    suspend fun saveAndCompressImage(
        sourceUri: Uri,
        fileName: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(sourceUri)
                ?: return@withContext Result.failure(IOException("Cannot open input stream"))

            // Decode bitmap
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
                ?: return@withContext Result.failure(IOException("Cannot decode image"))

            inputStream.close()

            // Compress and resize if needed
            val compressedBitmap = compressImage(originalBitmap)

            // Save to app storage
            val savedPath = saveToAppStorage(compressedBitmap, fileName)

            // Clean up bitmaps
            if (originalBitmap != compressedBitmap) {
                originalBitmap.recycle()
            }
            compressedBitmap.recycle()

            Result.success(savedPath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get sharable URI for a file using FileProvider
     */
    fun getShareableUri(filePath: String): Uri? {
        return try {
            val file = File(filePath)
            if (file.exists()) {
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
            } else null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Delete prescription image file
     */
    suspend fun deleteImage(filePath: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val file = File(filePath)
            if (file.exists() && file.delete()) {
                Result.success(Unit)
            } else {
                Result.failure(IOException("File not found or cannot be deleted"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get prescriptions directory
     */
    fun getPrescriptionsDirectory(): File {
        val dir = File(context.filesDir, PRESCRIPTIONS_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Clean up old prescription images (optional maintenance)
     */
    suspend fun cleanupOldImages(maxAgeInDays: Int = 30): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val prescriptionsDir = getPrescriptionsDirectory()
            val cutoffTime = System.currentTimeMillis() - (maxAgeInDays * 24 * 60 * 60 * 1000L)

            var deletedCount = 0
            prescriptionsDir.listFiles()?.forEach { file ->
                if (file.isFile && file.lastModified() < cutoffTime) {
                    if (file.delete()) {
                        deletedCount++
                    }
                }
            }

            Result.success(deletedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Compress image to reduce file size
     */
    private fun compressImage(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        // Check if resizing is needed
        if (width <= MAX_IMAGE_WIDTH && height <= MAX_IMAGE_HEIGHT) {
            return bitmap
        }

        // Calculate new dimensions maintaining aspect ratio
        val aspectRatio = width.toFloat() / height.toFloat()
        val newWidth: Int
        val newHeight: Int

        if (aspectRatio > 1) {
            // Landscape
            newWidth = MAX_IMAGE_WIDTH
            newHeight = (MAX_IMAGE_WIDTH / aspectRatio).toInt()
        } else {
            // Portrait
            newHeight = MAX_IMAGE_HEIGHT
            newWidth = (MAX_IMAGE_HEIGHT * aspectRatio).toInt()
        }

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    /**
     * Save bitmap to app storage
     */
    private fun saveToAppStorage(bitmap: Bitmap, fileName: String?): String {
        val prescriptionsDir = getPrescriptionsDirectory()

        val finalFileName = fileName ?: generateFileName()
        val file = File(prescriptionsDir, finalFileName)

        FileOutputStream(file).use { outputStream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, COMPRESSION_QUALITY, outputStream)
        }

        return file.absolutePath
    }

    /**
     * Generate unique file name
     */
    private fun generateFileName(): String {
        val timestamp = System.currentTimeMillis()
        return "prescription_${timestamp}.jpg"
    }
}