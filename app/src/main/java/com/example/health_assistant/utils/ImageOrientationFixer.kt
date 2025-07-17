package com.example.health_assistant.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

/**
 * Utility class to handle image orientation issues
 * Fixes images that are rotated incorrectly when captured from camera
 */
object ImageOrientationFixer {

    private const val TAG = "ImageOrientationFixer"

    /**
     * Fix orientation of an image from a Uri
     * @param context Application context
     * @param imageUri Uri of the image to fix
     * @return Uri of the fixed image, or the original Uri if no fix was needed or possible
     */
    fun fixImageOrientation(context: Context, imageUri: Uri): Uri {
        try {
            // Get input stream from Uri
            val inputStream = context.contentResolver.openInputStream(imageUri) ?: return imageUri
            
            // Get EXIF orientation
            val orientation = getExifOrientation(context, imageUri)
            
            // If no rotation needed, return original
            if (orientation == ExifInterface.ORIENTATION_NORMAL || orientation == ExifInterface.ORIENTATION_UNDEFINED) {
                inputStream.close()
                return imageUri
            }
            
            // Decode bitmap
            inputStream.close()
            val newInputStream = context.contentResolver.openInputStream(imageUri) ?: return imageUri
            val bitmap = BitmapFactory.decodeStream(newInputStream)
            newInputStream.close()
            
            // Rotate bitmap according to EXIF orientation
            val rotatedBitmap = rotateBitmapByExif(bitmap, orientation)
            
            // If rotation failed, return original
            if (rotatedBitmap == bitmap) {
                return imageUri
            }
            
            // Save rotated bitmap to a temporary file
            val tempFile = createTempFile(context)
            FileOutputStream(tempFile).use { outputStream ->
                rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            }
            
            // Clean up if rotated bitmap is different from original
            if (rotatedBitmap != bitmap) {
                bitmap.recycle()
            }
            
            // Return Uri for the new file
            return Uri.fromFile(tempFile)
        } catch (e: Exception) {
            Log.e(TAG, "Error fixing image orientation: ${e.message}", e)
            return imageUri // Return original on error
        }
    }
    
    /**
     * Get EXIF orientation from an image Uri
     */
    private fun getExifOrientation(context: Context, imageUri: Uri): Int {
        var inputStream: InputStream? = null
        try {
            inputStream = context.contentResolver.openInputStream(imageUri)
            if (inputStream != null) {
                val exif = ExifInterface(inputStream)
                return exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting EXIF orientation: ${e.message}", e)
        } finally {
            inputStream?.close()
        }
        return ExifInterface.ORIENTATION_NORMAL
    }
    
    /**
     * Rotate bitmap according to EXIF orientation
     */
    private fun rotateBitmapByExif(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> {
                matrix.preScale(1f, -1f)
            }
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.preScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.preScale(-1f, 1f)
            }
            else -> return bitmap
        }
        
        return try {
            val rotatedBitmap = Bitmap.createBitmap(
                bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
            )
            rotatedBitmap
        } catch (e: OutOfMemoryError) {
            Log.e(TAG, "OutOfMemoryError when rotating bitmap", e)
            bitmap
        }
    }
    
    /**
     * Create a temporary file for storing the fixed image
     */
    private fun createTempFile(context: Context): File {
        val tempDir = File(context.cacheDir, "fixed_images").apply {
            if (!exists()) mkdirs()
        }
        
        return File(tempDir, "fixed_${System.currentTimeMillis()}.jpg")
    }
    
    /**
     * Clean up temporary files
     * Call this periodically to avoid filling up storage
     */
    fun cleanupTempFiles(context: Context) {
        try {
            val tempDir = File(context.cacheDir, "fixed_images")
            if (tempDir.exists()) {
                val files = tempDir.listFiles()
                files?.forEach { file ->
                    // Delete files older than 1 day
                    if (System.currentTimeMillis() - file.lastModified() > 24 * 60 * 60 * 1000) {
                        file.delete()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up temp files: ${e.message}", e)
        }
    }
} 