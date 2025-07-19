package com.example.health_assistant.features.discover.navigation

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.navigation.NavController
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles deep links for Discover feature
 * Processes incoming deep links and navigates to appropriate content
 */
@Singleton
class DiscoverDeepLinkHandler @Inject constructor(
    private val navigationHelper: DiscoverNavigationHelper
) {
    
    private val TAG = "DiscoverDeepLinkHandler"

    /**
     * Handle incoming deep link intent
     * Returns true if the deep link was handled, false otherwise
     */
    fun handleDeepLink(intent: Intent, navController: NavController): Boolean {
        val data = intent.data ?: return false
        
        Log.d(TAG, "Handling deep link: $data")
        
        return when {
            isDiscoverArticleLink(data) -> {
                handleArticleDeepLink(data, navController)
                true
            }
            isDiscoverVideoLink(data) -> {
                handleVideoDeepLink(data, navController)
                true
            }
            isDiscoverBookmarksLink(data) -> {
                handleBookmarksDeepLink(navController)
                true
            }
            isDiscoverMainLink(data) -> {
                handleDiscoverMainDeepLink(navController)
                true
            }
            else -> {
                Log.d(TAG, "Deep link not handled by Discover: $data")
                false
            }
        }
    }

    /**
     * Check if the URI is a discover article link
     */
    private fun isDiscoverArticleLink(uri: Uri): Boolean {
        return uri.pathSegments?.let { segments ->
            segments.size >= 2 && 
            segments[0] == "discover" && 
            (segments[1] == "article" || segments[1] == "news")
        } ?: false
    }

    /**
     * Check if the URI is a discover video link
     */
    private fun isDiscoverVideoLink(uri: Uri): Boolean {
        return uri.pathSegments?.let { segments ->
            segments.size >= 2 && 
            segments[0] == "discover" && 
            segments[1] == "video"
        } ?: false
    }

    /**
     * Check if the URI is a discover bookmarks link
     */
    private fun isDiscoverBookmarksLink(uri: Uri): Boolean {
        return uri.pathSegments?.let { segments ->
            segments.size >= 2 && 
            segments[0] == "discover" && 
            segments[1] == "bookmarks"
        } ?: false
    }

    /**
     * Check if the URI is a discover main link
     */
    private fun isDiscoverMainLink(uri: Uri): Boolean {
        return uri.pathSegments?.let { segments ->
            segments.size == 1 && segments[0] == "discover"
        } ?: false
    }

    /**
     * Handle article deep link
     * Expected format: healthassistant://discover/article/{articleId}
     * or: https://healthassistant.app/discover/article/{articleId}
     */
    private fun handleArticleDeepLink(uri: Uri, navController: NavController) {
        val segments = uri.pathSegments
        if (segments != null && segments.size >= 3) {
            val contentType = segments[1] // "article" or "news"
            val articleId = segments[2]
            
            Log.d(TAG, "Navigating to article: $articleId, type: $contentType")
            navigationHelper.navigateToArticleReaderGlobal(navController, articleId, contentType)
        } else {
            Log.w(TAG, "Invalid article deep link format: $uri")
        }
    }

    /**
     * Handle video deep link
     * Expected format: healthassistant://discover/video/{videoId}
     * or: https://healthassistant.app/discover/video/{videoId}
     */
    private fun handleVideoDeepLink(uri: Uri, navController: NavController) {
        val segments = uri.pathSegments
        if (segments != null && segments.size >= 3) {
            val videoId = segments[2]
            val autoPlay = uri.getBooleanQueryParameter("autoPlay", true)
            
            Log.d(TAG, "Navigating to video: $videoId, autoPlay: $autoPlay")
            navigationHelper.navigateToVideoPlayerGlobal(navController, videoId, autoPlay)
        } else {
            Log.w(TAG, "Invalid video deep link format: $uri")
        }
    }

    /**
     * Handle bookmarks deep link
     * Expected format: healthassistant://discover/bookmarks
     * or: https://healthassistant.app/discover/bookmarks
     */
    private fun handleBookmarksDeepLink(navController: NavController) {
        Log.d(TAG, "Navigating to bookmarks")
        navigationHelper.navigateToBookmarksGlobal(navController)
    }

    /**
     * Handle discover main deep link
     * Expected format: healthassistant://discover
     * or: https://healthassistant.app/discover
     */
    private fun handleDiscoverMainDeepLink(navController: NavController) {
        Log.d(TAG, "Navigating to discover main")
        navigationHelper.navigateToDiscoverGlobal(navController)
    }

    /**
     * Generate share link for article content
     */
    fun generateArticleShareLink(articleId: String, contentType: String = "article"): String {
        return "https://healthassistant.app/discover/$contentType/$articleId"
    }

    /**
     * Generate share link for video content
     */
    fun generateVideoShareLink(videoId: String): String {
        return "https://healthassistant.app/discover/video/$videoId"
    }

    /**
     * Generate share link for bookmarks
     */
    fun generateBookmarksShareLink(): String {
        return "https://healthassistant.app/discover/bookmarks"
    }
}

/**
 * Extension function to get boolean query parameter with default value
 */
private fun Uri.getBooleanQueryParameter(key: String, defaultValue: Boolean): Boolean {
    return getQueryParameter(key)?.toBooleanStrictOrNull() ?: defaultValue
}