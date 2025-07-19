package com.example.health_assistant.features.discover.presentation

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.example.health_assistant.R
import com.example.health_assistant.databinding.ViewOfflineStatusIndicatorBinding
import com.example.health_assistant.features.discover.data.cache.CacheStatus
import com.example.health_assistant.features.discover.data.cache.OfflineContentStatus
import com.example.health_assistant.features.discover.data.cache.SyncFreshness
import com.example.health_assistant.features.discover.data.cache.SyncStatus
import java.text.SimpleDateFormat
import java.util.*

/**
 * Custom view that displays offline content availability and sync status
 * Shows indicators for cache status, sync freshness, and offline readiness
 */
class OfflineStatusIndicator @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val binding: ViewOfflineStatusIndicatorBinding
    private val dateFormatter = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())

    init {
        binding = ViewOfflineStatusIndicatorBinding.inflate(
            LayoutInflater.from(context), this, true
        )
        orientation = VERTICAL
        setupView()
    }

    private fun setupView() {
        // Set up click listeners for expandable details
        binding.statusHeader.setOnClickListener {
            binding.statusDetails.isVisible = !binding.statusDetails.isVisible
            binding.expandIcon.rotation = if (binding.statusDetails.isVisible) 180f else 0f
        }
    }

    /**
     * Update the offline content status display
     */
    fun updateOfflineStatus(status: OfflineContentStatus) {
        binding.apply {
            // Update main status indicator
            when {
                status.isOfflineReady && status.essentialContentAvailable -> {
                    statusIcon.setImageResource(R.drawable.ic_offline_ready)
                    statusIcon.setColorFilter(ContextCompat.getColor(context, R.color.success_green))
                    statusText.text = "Offline Ready"
                    statusText.setTextColor(ContextCompat.getColor(context, R.color.success_green))
                }
                status.isOfflineReady -> {
                    statusIcon.setImageResource(R.drawable.ic_offline_partial)
                    statusIcon.setColorFilter(ContextCompat.getColor(context, R.color.warning_orange))
                    statusText.text = "Partially Offline"
                    statusText.setTextColor(ContextCompat.getColor(context, R.color.warning_orange))
                }
                else -> {
                    statusIcon.setImageResource(R.drawable.ic_offline_unavailable)
                    statusIcon.setColorFilter(ContextCompat.getColor(context, R.color.error_red))
                    statusText.text = "Online Only"
                    statusText.setTextColor(ContextCompat.getColor(context, R.color.error_red))
                }
            }

            // Update sync freshness indicator
            updateSyncFreshnessIndicator(status.syncFreshness, status.lastSyncTime)

            // Update detailed statistics
            cachedItemsText.text = "${status.totalCachedItems} items cached"
            cacheSizeText.text = formatCacheSize(status.cacheSize)
            
            // Show essential content status
            essentialContentStatus.isVisible = true
            essentialContentText.text = if (status.essentialContentAvailable) {
                "Essential health info available offline"
            } else {
                "Essential health info needs sync"
            }
            essentialContentIcon.setImageResource(
                if (status.essentialContentAvailable) 
                    R.drawable.ic_check_circle 
                else 
                    R.drawable.ic_warning
            )
            essentialContentIcon.setColorFilter(
                ContextCompat.getColor(
                    context,
                    if (status.essentialContentAvailable) R.color.success_green else R.color.warning_orange
                )
            )
        }
    }

    /**
     * Update the sync status display
     */
    fun updateSyncStatus(status: SyncStatus) {
        binding.apply {
            syncStatusContainer.isVisible = true
            
            when (status) {
                is SyncStatus.Idle -> {
                    syncStatusContainer.isVisible = false
                }
                is SyncStatus.Syncing -> {
                    syncIcon.setImageResource(R.drawable.ic_sync)
                    syncIcon.startAnimation(createRotationAnimation())
                    syncText.text = "Syncing content..."
                    syncProgress.isVisible = false
                }
                is SyncStatus.Progress -> {
                    syncIcon.setImageResource(R.drawable.ic_sync)
                    syncIcon.startAnimation(createRotationAnimation())
                    syncText.text = "Syncing: ${status.currentItem}"
                    syncProgress.isVisible = true
                    syncProgress.progress = status.percentage
                }
                is SyncStatus.Success -> {
                    syncIcon.clearAnimation()
                    syncIcon.setImageResource(R.drawable.ic_check_circle)
                    syncIcon.setColorFilter(ContextCompat.getColor(context, R.color.success_green))
                    syncText.text = "Sync completed"
                    syncProgress.isVisible = false
                    
                    // Hide success message after delay
                    postDelayed({ 
                        syncStatusContainer.isVisible = false 
                    }, 3000)
                }
                is SyncStatus.Error -> {
                    syncIcon.clearAnimation()
                    syncIcon.setImageResource(R.drawable.ic_error)
                    syncIcon.setColorFilter(ContextCompat.getColor(context, R.color.error_red))
                    syncText.text = "Sync failed: ${status.message}"
                    syncProgress.isVisible = false
                }
                is SyncStatus.Partial -> {
                    syncIcon.clearAnimation()
                    syncIcon.setImageResource(R.drawable.ic_warning)
                    syncIcon.setColorFilter(ContextCompat.getColor(context, R.color.warning_orange))
                    syncText.text = "Partial sync: ${status.successCount} success, ${status.failureCount} failed"
                    syncProgress.isVisible = false
                }
            }
        }
    }

    /**
     * Update cache status display
     */
    fun updateCacheStatus(status: CacheStatus) {
        binding.apply {
            cacheStatusContainer.isVisible = true
            
            when (status) {
                is CacheStatus.Idle -> {
                    cacheStatusContainer.isVisible = false
                }
                is CacheStatus.Initializing -> {
                    cacheIcon.setImageResource(R.drawable.ic_cached)
                    cacheIcon.startAnimation(createPulseAnimation())
                    cacheText.text = "Initializing cache..."
                }
                is CacheStatus.Prefetching -> {
                    cacheIcon.setImageResource(R.drawable.ic_download)
                    cacheIcon.startAnimation(createPulseAnimation())
                    cacheText.text = "Prefetching essential content..."
                }
                is CacheStatus.Cleaning -> {
                    cacheIcon.setImageResource(R.drawable.ic_cleaning)
                    cacheIcon.startAnimation(createPulseAnimation())
                    cacheText.text = "Optimizing cache..."
                }
                is CacheStatus.Ready -> {
                    cacheIcon.clearAnimation()
                    cacheIcon.setImageResource(R.drawable.ic_check_circle)
                    cacheIcon.setColorFilter(ContextCompat.getColor(context, R.color.success_green))
                    cacheText.text = "Cache ready"
                    
                    // Hide ready message after delay
                    postDelayed({ 
                        cacheStatusContainer.isVisible = false 
                    }, 2000)
                }
                is CacheStatus.Error -> {
                    cacheIcon.clearAnimation()
                    cacheIcon.setImageResource(R.drawable.ic_error)
                    cacheIcon.setColorFilter(ContextCompat.getColor(context, R.color.error_red))
                    cacheText.text = "Cache error: ${status.message}"
                }
            }
        }
    }

    /**
     * Show/hide the entire offline status indicator
     */
    fun setVisible(visible: Boolean) {
        isVisible = visible
    }

    /**
     * Set compact mode (show only essential info)
     */
    fun setCompactMode(compact: Boolean) {
        binding.statusDetails.isVisible = !compact
        binding.expandIcon.isVisible = !compact
    }

    // Private helper methods

    private fun updateSyncFreshnessIndicator(freshness: SyncFreshness, lastSyncTime: Long) {
        binding.apply {
            val (color, text) = when (freshness) {
                SyncFreshness.Fresh -> {
                    R.color.success_green to "Recently synced"
                }
                SyncFreshness.Recent -> {
                    R.color.success_green to "Synced today"
                }
                SyncFreshness.Stale -> {
                    R.color.warning_orange to "Sync recommended"
                }
                SyncFreshness.VeryStale -> {
                    R.color.error_red to "Sync needed"
                }
            }
            
            syncFreshnessIndicator.setColorFilter(ContextCompat.getColor(context, color))
            syncFreshnessText.text = text
            syncFreshnessText.setTextColor(ContextCompat.getColor(context, color))
            
            if (lastSyncTime > 0) {
                lastSyncText.text = "Last sync: ${dateFormatter.format(Date(lastSyncTime))}"
                lastSyncText.isVisible = true
            } else {
                lastSyncText.isVisible = false
            }
        }
    }

    private fun formatCacheSize(sizeBytes: Long): String {
        return when {
            sizeBytes < 1024 -> "${sizeBytes}B"
            sizeBytes < 1024 * 1024 -> "${sizeBytes / 1024}KB"
            sizeBytes < 1024 * 1024 * 1024 -> "${sizeBytes / (1024 * 1024)}MB"
            else -> "${sizeBytes / (1024 * 1024 * 1024)}GB"
        }
    }

    private fun createRotationAnimation() = android.view.animation.AnimationUtils.loadAnimation(
        context, R.anim.rotate_indefinite
    )

    private fun createPulseAnimation() = android.view.animation.AnimationUtils.loadAnimation(
        context, R.anim.pulse
    )
}