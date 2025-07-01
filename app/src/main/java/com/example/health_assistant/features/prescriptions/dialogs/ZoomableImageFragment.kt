package com.example.health_assistant.features.prescriptions.dialogs

import android.app.Dialog
import android.graphics.Matrix
import android.graphics.PointF
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import coil3.load
import coil3.request.crossfade
import coil3.request.error
import coil3.request.placeholder
import com.example.health_assistant.R
import com.example.health_assistant.databinding.FragmentZoomableImageBinding
import com.example.health_assistant.features.prescriptions.utils.FileManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.math.min

/**
 * DialogFragment for displaying prescription images with zoom and pan functionality
 */
@AndroidEntryPoint
class ZoomableImageFragment : DialogFragment() {

    private var _binding: FragmentZoomableImageBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var fileManager: FileManager

    private lateinit var imagePath: String
    private lateinit var scaleGestureDetector: ScaleGestureDetector

    // Matrix for image transformations
    private val matrix = Matrix()
    private val savedMatrix = Matrix()

    // Touch handling
    private val start = PointF()
    private val mid = PointF()
    private var mode = NONE
    private var scaleFactor = 1f
    private val minScale = 0.5f
    private val maxScale = 5f

    companion object {
        private const val ARG_IMAGE_PATH = "image_path"
        private const val NONE = 0
        private const val DRAG = 1
        private const val ZOOM = 2

        fun newInstance(imagePath: String): ZoomableImageFragment {
            return ZoomableImageFragment().apply {
                arguments = bundleOf(ARG_IMAGE_PATH to imagePath)
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // Create a full-screen dialog for image viewing
        val dialog = Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen)

        dialog.window?.apply {
            setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
            setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT
            )
        }

        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentZoomableImageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        imagePath = arguments?.getString(ARG_IMAGE_PATH)
            ?: throw IllegalArgumentException("Image path is required")

        setupZoomGestures()
        setupToolbar()
        loadImage()
    }

    private fun setupZoomGestures() {
        scaleGestureDetector = ScaleGestureDetector(requireContext(), ScaleListener())

        binding.zoomableImageView.setOnTouchListener { _, event ->
            scaleGestureDetector.onTouchEvent(event)

            when (event.action and MotionEvent.ACTION_MASK) {
                MotionEvent.ACTION_DOWN -> {
                    savedMatrix.set(matrix)
                    start.set(event.x, event.y)
                    mode = DRAG
                }

                MotionEvent.ACTION_POINTER_DOWN -> {
                    savedMatrix.set(matrix)
                    midPoint(mid, event)
                    mode = ZOOM
                }

                MotionEvent.ACTION_MOVE -> {
                    if (mode == DRAG && scaleFactor > 1f) {
                        // Only allow dragging when zoomed in
                        matrix.set(savedMatrix)
                        val dx = event.x - start.x
                        val dy = event.y - start.y
                        matrix.postTranslate(dx, dy)

                        // Apply bounds checking
                        checkBounds()
                        binding.zoomableImageView.imageMatrix = matrix
                    }
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                    mode = NONE
                    savedMatrix.set(matrix)
                }
            }

            true
        }
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            dismiss() // Use dismiss() instead of findNavController().navigateUp() for DialogFragment
        }

        binding.shareImageButton.setOnClickListener {
            shareImage()
        }
    }

    private fun loadImage() {
        binding.loadingProgress.visibility = View.VISIBLE

        binding.zoomableImageView.load(imagePath) {
            placeholder(R.drawable.ic_prescription_placeholder)
            error(R.drawable.ic_prescription_placeholder)
            crossfade(true)
            listener(
                onSuccess = { _, _ ->
                    binding.loadingProgress.visibility = View.GONE
                    // Center the image initially
                    centerImage()
                },
                onError = { _, _ ->
                    binding.loadingProgress.visibility = View.GONE
                }
            )
        }
    }

    private fun checkBounds() {
        val imageView = binding.zoomableImageView
        val drawable = imageView.drawable ?: return

        val values = FloatArray(9)
        matrix.getValues(values)

        val transX = values[Matrix.MTRANS_X]
        val transY = values[Matrix.MTRANS_Y]
        val scaleX = values[Matrix.MSCALE_X]
        val scaleY = values[Matrix.MSCALE_Y]

        val viewWidth = imageView.width.toFloat()
        val viewHeight = imageView.height.toFloat()
        val imageWidth = drawable.intrinsicWidth * scaleX
        val imageHeight = drawable.intrinsicHeight * scaleY

        var deltaX = 0f
        var deltaY = 0f

        // Check horizontal bounds
        if (imageWidth <= viewWidth) {
            deltaX = (viewWidth - imageWidth) / 2 - transX
        } else {
            if (transX > 0) deltaX = -transX
            if (transX + imageWidth < viewWidth) deltaX = viewWidth - imageWidth - transX
        }

        // Check vertical bounds
        if (imageHeight <= viewHeight) {
            deltaY = (viewHeight - imageHeight) / 2 - transY
        } else {
            if (transY > 0) deltaY = -transY
            if (transY + imageHeight < viewHeight) deltaY = viewHeight - imageHeight - transY
        }

        matrix.postTranslate(deltaX, deltaY)
    }

    private fun centerImage() {
        // Post this to ensure the view has been laid out
        binding.zoomableImageView.post {
            val imageView = binding.zoomableImageView
            val drawable = imageView.drawable ?: return@post

            val viewWidth = imageView.width.toFloat()
            val viewHeight = imageView.height.toFloat()
            val drawableWidth = drawable.intrinsicWidth.toFloat()
            val drawableHeight = drawable.intrinsicHeight.toFloat()

            if (viewWidth > 0 && viewHeight > 0 && drawableWidth > 0 && drawableHeight > 0) {
                val scale = min(viewWidth / drawableWidth, viewHeight / drawableHeight)

                matrix.reset()
                matrix.postScale(scale, scale)
                matrix.postTranslate(
                    (viewWidth - drawableWidth * scale) / 2f,
                    (viewHeight - drawableHeight * scale) / 2f
                )

                scaleFactor = scale
                savedMatrix.set(matrix)
                imageView.imageMatrix = matrix
                imageView.scaleType = android.widget.ImageView.ScaleType.MATRIX
            }
        }
    }

    private fun shareImage() {
        try {
            val shareableUri = fileManager.getShareableUri(imagePath)
            if (shareableUri != null) {
                val shareIntent = android.content.Intent().apply {
                    action = android.content.Intent.ACTION_SEND
                    type = "image/png"
                    putExtra(android.content.Intent.EXTRA_STREAM, shareableUri)
                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                startActivity(android.content.Intent.createChooser(shareIntent, "Share Prescription Image"))
            } else {
                android.widget.Toast.makeText(
                    requireContext(),
                    "Error sharing image",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        } catch (e: Exception) {
            android.widget.Toast.makeText(
                requireContext(),
                "Error sharing image: ${e.message}",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun midPoint(point: PointF, event: MotionEvent) {
        val x = event.getX(0) + event.getX(1)
        val y = event.getY(0) + event.getY(1)
        point.set(x / 2, y / 2)
    }

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            mode = ZOOM
            return true
        }

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val detectorScaleFactor = detector.scaleFactor
            val newScaleFactor = scaleFactor * detectorScaleFactor

            // Constrain scale factor within bounds
            if (newScaleFactor >= minScale && newScaleFactor <= maxScale) {
                scaleFactor = newScaleFactor

                matrix.set(savedMatrix)
                matrix.postScale(detectorScaleFactor, detectorScaleFactor, detector.focusX, detector.focusY)

                checkBounds()
                binding.zoomableImageView.imageMatrix = matrix
            }

            return true
        }

        override fun onScaleEnd(detector: ScaleGestureDetector) {
            savedMatrix.set(matrix)
            mode = NONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}