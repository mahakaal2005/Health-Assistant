package com.example.health_assistant.features.prescriptions.camera

import android.content.Context
import android.net.Uri
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.guava.await
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Camera manager for capturing prescription photos
 * Handles camera setup, photo capture, and file management
 */
class CameraManager(private val context: Context) {

    private var imageCapture: ImageCapture? = null
    private var cameraProvider: ProcessCameraProvider? = null

    /**
     * Setup camera with preview
     */
    suspend fun setupCamera(
        previewView: PreviewView,
        lifecycleOwner: LifecycleOwner
    ): Result<Unit> {
        return try {
            val cameraProvider = getCameraProvider()
            this.cameraProvider = cameraProvider

            // Build camera preview
            val preview = Preview.Builder().build()
            preview.setSurfaceProvider(previewView.surfaceProvider)

            // Build image capture
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            // Select back camera as default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            // Unbind any previous use cases and bind new ones
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageCapture
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Capture photo and save to app's private storage
     */
    suspend fun capturePhoto(): Result<Uri> {
        val imageCapture = this.imageCapture ?: return Result.failure(
            IllegalStateException("Camera not initialized")
        )

        return suspendCancellableCoroutine { continuation ->
            // Create output file
            val photoFile = createImageFile()
            val outputFileOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

            // Capture photo
            imageCapture.takePicture(
                outputFileOptions,
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        val savedUri = Uri.fromFile(photoFile)
                        continuation.resume(Result.success(savedUri))
                    }

                    override fun onError(exception: ImageCaptureException) {
                        continuation.resume(Result.failure(exception))
                    }
                }
            )

            continuation.invokeOnCancellation {
                // Cleanup if cancelled
                try {
                    if (photoFile.exists()) {
                        photoFile.delete()
                    }
                } catch (e: Exception) {
                    // Ignore cleanup errors
                }
            }
        }
    }

    /**
     * Release camera resources
     */
    fun releaseCamera() {
        cameraProvider?.unbindAll()
        cameraProvider = null
        imageCapture = null
    }

    /**
     * Get camera provider instance
     */
    private suspend fun getCameraProvider(): ProcessCameraProvider {
        return ProcessCameraProvider.getInstance(context).await()
    }

    /**
     * Create unique image file in app's private storage
     */
    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "PRESCRIPTION_${timeStamp}.jpg"

        // Create prescriptions directory if it doesn't exist
        val prescriptionsDir = File(context.filesDir, "prescriptions")
        if (!prescriptionsDir.exists()) {
            prescriptionsDir.mkdirs()
        }

        return File(prescriptionsDir, imageFileName)
    }

    /**
     * Check if camera is available and permissions are granted
     */
    fun isCameraAvailable(): Boolean {
        return context.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_CAMERA_ANY)
    }
}