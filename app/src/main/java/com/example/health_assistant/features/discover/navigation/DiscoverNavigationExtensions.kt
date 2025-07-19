package com.example.health_assistant.features.discover.navigation

import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.health_assistant.features.discover.domain.model.DiscoverContent

/**
 * Navigation extensions for Discover fragments
 * Provides easy access to navigation functionality
 */

/**
 * Navigate to article reader from any fragment
 */
fun Fragment.navigateToArticleReader(articleId: String, contentType: String = "article") {
    val navigationHelper = DiscoverNavigationHelper()
    navigationHelper.navigateToArticleReader(findNavController(), articleId, contentType)
}

/**
 * Navigate to video player from any fragment
 */
fun Fragment.navigateToVideoPlayer(videoId: String, autoPlay: Boolean = true) {
    val navigationHelper = DiscoverNavigationHelper()
    navigationHelper.navigateToVideoPlayer(findNavController(), videoId, autoPlay)
}

/**
 * Navigate to bookmarks from any fragment
 */
fun Fragment.navigateToBookmarks() {
    val navigationHelper = DiscoverNavigationHelper()
    navigationHelper.navigateToBookmarks(findNavController())
}

/**
 * Navigate to content based on DiscoverContent type
 */
fun Fragment.navigateToContent(content: DiscoverContent) {
    val navigationHelper = DiscoverNavigationHelper()
    navigationHelper.navigateToContent(findNavController(), content)
}

/**
 * Navigate to Journal for health tracking (cross-feature navigation)
 */
fun Fragment.navigateToJournal() {
    val navigationHelper = DiscoverNavigationHelper()
    navigationHelper.navigateToJournal(findNavController())
}

/**
 * Navigate to Profile for health preferences (cross-feature navigation)
 */
fun Fragment.navigateToProfile() {
    val navigationHelper = DiscoverNavigationHelper()
    navigationHelper.navigateToProfile(findNavController())
}

/**
 * Navigate back to Discover main screen from any discover detail screen
 */
fun Fragment.navigateBackToDiscover() {
    val navigationHelper = DiscoverNavigationHelper()
    navigationHelper.navigateBackToDiscover(findNavController())
}

/**
 * Generate share link for current content
 */
fun Fragment.generateShareLink(content: DiscoverContent): String {
    val deepLinkHandler = DiscoverDeepLinkHandler(DiscoverNavigationHelper())
    return when (content) {
        is DiscoverContent.Article -> {
            deepLinkHandler.generateArticleShareLink(content.id, "article")
        }
        is DiscoverContent.News -> {
            deepLinkHandler.generateArticleShareLink(content.id, "news")
        }
        is DiscoverContent.Video -> {
            deepLinkHandler.generateVideoShareLink(content.id)
        }
    }
}

/**
 * Share content using Android's share intent
 */
fun Fragment.shareContent(content: DiscoverContent) {
    val shareLink = generateShareLink(content)
    val shareText = when (content) {
        is DiscoverContent.Article -> "Check out this health article: ${content.title}\n$shareLink"
        is DiscoverContent.News -> "Check out this health news: ${content.title}\n$shareLink"
        is DiscoverContent.Video -> "Watch this health video: ${content.title}\n$shareLink"
    }
    
    val shareIntent = android.content.Intent().apply {
        action = android.content.Intent.ACTION_SEND
        type = "text/plain"
        putExtra(android.content.Intent.EXTRA_TEXT, shareText)
        putExtra(android.content.Intent.EXTRA_SUBJECT, "Health Assistant - ${content.title}")
    }
    
    startActivity(android.content.Intent.createChooser(shareIntent, "Share via"))
}