package com.example.health_assistant.regression

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Utility class for advanced screenshot comparison in visual regression testing
 * 
 * Provides pixel-by-pixel comparison and similarity scoring for automated
 * visual regression detection.
 */
object ScreenshotComparisonUtil {

    /**
     * Compare two screenshots and return similarity score
     * 
     * @param currentFile Current screenshot file
     * @param baselineFile Baseline screenshot file
     * @return Similarity score between 0.0 (completely different) and 1.0 (identical)
     */
    fun compareScreenshots(currentFile: File, baselineFile: File): Double {
        if (!currentFile.exists() || !baselineFile.exists()) {
            return 0.0
        }

        val currentBitmap = BitmapFactory.decodeFile(currentFile.absolutePath)
        val baselineBitmap = BitmapFactory.decodeFile(baselineFile.absolutePath)

        return compareBitmaps(currentBitmap, baselineBitmap)
    }

    /**
     * Compare two bitmaps pixel by pixel
     * 
     * @param bitmap1 First bitmap
     * @param bitmap2 Second bitmap
     * @return Similarity score between 0.0 and 1.0
     */
    private fun compareBitmaps(bitmap1: Bitmap?, bitmap2: Bitmap?): Double {
        if (bitmap1 == null || bitmap2 == null) return 0.0
        
        // Check if dimensions match
        if (bitmap1.width != bitmap2.width || bitmap1.height != bitmap2.height) {
            return 0.0
        }

        val width = bitmap1.width
        val height = bitmap1.height
        var totalPixels = 0
        var matchingPixels = 0

        for (x in 0 until width) {
            for (y in 0 until height) {
                val pixel1 = bitmap1.getPixel(x, y)
                val pixel2 = bitmap2.getPixel(x, y)
                
                totalPixels++
                
                // Calculate color difference using Euclidean distance
                val colorDifference = calculateColorDifference(pixel1, pixel2)
                
                // Consider pixels matching if color difference is below threshold
                if (colorDifference < 30) { // Threshold for acceptable color difference
                    matchingPixels++
                }
            }
        }

        return matchingPixels.toDouble() / totalPixels.toDouble()
    }

    /**
     * Calculate color difference between two pixels using Euclidean distance
     * 
     * @param pixel1 First pixel color
     * @param pixel2 Second pixel color
     * @return Color difference value
     */
    private fun calculateColorDifference(pixel1: Int, pixel2: Int): Double {
        val r1 = (pixel1 shr 16) and 0xFF
        val g1 = (pixel1 shr 8) and 0xFF
        val b1 = pixel1 and 0xFF
        
        val r2 = (pixel2 shr 16) and 0xFF
        val g2 = (pixel2 shr 8) and 0xFF
        val b2 = pixel2 and 0xFF
        
        val deltaR = r1 - r2
        val deltaG = g1 - g2
        val deltaB = b1 - b2
        
        return sqrt((deltaR * deltaR + deltaG * deltaG + deltaB * deltaB).toDouble())
    }

    /**
     * Generate difference image highlighting changed pixels
     * 
     * @param currentFile Current screenshot file
     * @param baselineFile Baseline screenshot file
     * @param outputFile Output file for difference image
     */
    fun generateDifferenceImage(currentFile: File, baselineFile: File, outputFile: File) {
        val currentBitmap = BitmapFactory.decodeFile(currentFile.absolutePath)
        val baselineBitmap = BitmapFactory.decodeFile(baselineFile.absolutePath)
        
        if (currentBitmap == null || baselineBitmap == null) return
        if (currentBitmap.width != baselineBitmap.width || currentBitmap.height != baselineBitmap.height) return

        val width = currentBitmap.width
        val height = currentBitmap.height
        val diffBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        for (x in 0 until width) {
            for (y in 0 until height) {
                val pixel1 = currentBitmap.getPixel(x, y)
                val pixel2 = baselineBitmap.getPixel(x, y)
                
                val colorDifference = calculateColorDifference(pixel1, pixel2)
                
                if (colorDifference > 30) {
                    // Highlight different pixels in red
                    diffBitmap.setPixel(x, y, 0xFFFF0000.toInt())
                } else {
                    // Keep original pixel for unchanged areas
                    diffBitmap.setPixel(x, y, pixel1)
                }
            }
        }

        // Save difference image
        try {
            val outputStream = outputFile.outputStream()
            diffBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            outputStream.close()
        } catch (e: Exception) {
            // Handle save error
        }
    }

    /**
     * Validate screenshot with configurable similarity threshold
     * 
     * @param currentFile Current screenshot file
     * @param baselineFile Baseline screenshot file
     * @param threshold Minimum similarity threshold (default 0.95 = 95% similar)
     * @return True if screenshots are similar enough, false otherwise
     */
    fun validateScreenshot(currentFile: File, baselineFile: File, threshold: Double = 0.95): Boolean {
        val similarity = compareScreenshots(currentFile, baselineFile)
        return similarity >= threshold
    }
}