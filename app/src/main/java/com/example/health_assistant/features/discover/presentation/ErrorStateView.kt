package com.example.health_assistant.features.discover.presentation

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.core.view.isVisible
import com.example.health_assistant.databinding.ViewErrorStateBinding
import com.example.health_assistant.features.discover.domain.error.DiscoverError

/**
 * Custom view for displaying error states with retry functionality
 */
class ErrorStateView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {
    
    private val binding: ViewErrorStateBinding
    private var onRetryClickListener: (() -> Unit)? = null
    private var onSecondaryActionClickListener: (() -> Unit)? = null
    
    init {
        binding = ViewErrorStateBinding.inflate(LayoutInflater.from(context), this, true)
        setupClickListeners()
    }
    
    private fun setupClickListeners() {
        binding.retryButton.setOnClickListener {
            onRetryClickListener?.invoke()
        }
        
        binding.secondaryActionButton.setOnClickListener {
            onSecondaryActionClickListener?.invoke()
        }
    }
    
    fun setErrorState(errorState: ErrorState) {
        when (errorState) {
            is ErrorState.None -> {
                isVisible = false
            }
            
            is ErrorState.Error -> {
                isVisible = true
                showError(errorState.discoverError, errorState.isRetrying, errorState.retryAttempt)
            }
            
            is ErrorState.PartialError -> {
                isVisible = true
                showPartialError(errorState.discoverError, errorState.isRetrying)
            }
            
            is ErrorState.NetworkOffline -> {
                isVisible = true
                showOfflineState(errorState.hasCache)
            }
        }
    }
    
    private fun showError(error: DiscoverError, isRetrying: Boolean, retryAttempt: Int) {
        binding.apply {
            // Set error icon based on error type
            errorIcon.setImageResource(getErrorIcon(error))
            
            // Set error title and message
            errorTitle.text = getErrorTitle(error)
            errorMessage.text = error.userMessage
            
            // Configure retry button
            retryContainer.isVisible = error.isRetryable
            retryButton.isVisible = !isRetrying && error.isRetryable
            retryProgress.isVisible = isRetrying
            
            // Show retry status if retrying
            if (isRetrying && retryAttempt > 0) {
                retryStatusText.isVisible = true
                retryStatusText.text = "Retrying... (Attempt $retryAttempt of 3)"
            } else {
                retryStatusText.isVisible = false
            }
            
            // Configure secondary action based on error type
            configureSecondaryAction(error)
        }
    }
    
    private fun showPartialError(error: DiscoverError.PartialContentError, isRetrying: Boolean) {
        binding.apply {
            errorIcon.setImageResource(android.R.drawable.ic_dialog_alert)
            errorTitle.text = "Some Content Unavailable"
            errorMessage.text = error.userMessage
            
            retryContainer.isVisible = true
            retryButton.isVisible = !isRetrying
            retryProgress.isVisible = isRetrying
            retryStatusText.isVisible = false
            
            secondaryActionButton.isVisible = false
        }
    }
    
    private fun showOfflineState(hasCache: Boolean) {
        binding.apply {
            errorIcon.setImageResource(android.R.drawable.ic_dialog_alert)
            errorTitle.text = "You're Offline"
            errorMessage.text = if (hasCache) {
                "Showing cached content. Connect to the internet for the latest updates."
            } else {
                "No internet connection and no cached content available."
            }
            
            retryContainer.isVisible = true
            retryButton.text = "Try Again"
            retryButton.isVisible = true
            retryProgress.isVisible = false
            retryStatusText.isVisible = false
            
            if (hasCache) {
                secondaryActionButton.isVisible = true
                secondaryActionButton.text = "View Cached Content"
            } else {
                secondaryActionButton.isVisible = false
            }
        }
    }
    
    private fun getErrorIcon(error: DiscoverError): Int {
        return when (error) {
            is DiscoverError.NetworkError -> android.R.drawable.ic_dialog_alert
            is DiscoverError.ServerError -> android.R.drawable.ic_dialog_info
            is DiscoverError.TimeoutError -> android.R.drawable.ic_dialog_alert
            is DiscoverError.ContentNotFoundError -> android.R.drawable.ic_delete
            is DiscoverError.VideoPlaybackError -> android.R.drawable.ic_media_play
            else -> android.R.drawable.ic_dialog_alert
        }
    }
    
    private fun getErrorTitle(error: DiscoverError): String {
        return when (error) {
            is DiscoverError.NetworkError -> "Connection Error"
            is DiscoverError.ServerError -> "Server Error"
            is DiscoverError.TimeoutError -> "Request Timeout"
            is DiscoverError.ContentNotFoundError -> "Content Not Found"
            is DiscoverError.ContentLoadError -> "Loading Error"
            is DiscoverError.DatabaseError -> "Storage Error"
            is DiscoverError.VideoPlaybackError -> "Video Error"
            is DiscoverError.SearchError -> "Search Error"
            is DiscoverError.SyncError -> "Sync Error"
            else -> "Error"
        }
    }
    
    private fun configureSecondaryAction(error: DiscoverError) {
        binding.secondaryActionButton.apply {
            when (error) {
                is DiscoverError.NetworkError -> {
                    isVisible = true
                    text = "View Offline Content"
                }
                is DiscoverError.VideoPlaybackError -> {
                    isVisible = true
                    text = "Download for Offline"
                }
                is DiscoverError.ContentNotFoundError -> {
                    isVisible = true
                    text = "Browse Similar Content"
                }
                else -> {
                    isVisible = false
                }
            }
        }
    }
    
    fun setOnRetryClickListener(listener: () -> Unit) {
        onRetryClickListener = listener
    }
    
    fun setOnSecondaryActionClickListener(listener: () -> Unit) {
        onSecondaryActionClickListener = listener
    }
    
    fun showRetryProgress(show: Boolean) {
        binding.retryButton.isVisible = !show
        binding.retryProgress.isVisible = show
    }
}