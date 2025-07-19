package com.example.health_assistant.features.discover.presentation

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.health_assistant.features.discover.domain.model.DiscoverContent

/**
 * Manager class for handling deep links and app attribution
 * Supports creating shareable links and handling incoming deep links
 */
class DeepLinkManager(private val context: Context) {

    companion object {
        private const val DEEP_LINK_SCHEME = "healthassistant"
        private const val WEB_BASE_URL = "https://healthassistant.app"
        private const val PLAY_STORE_URL = "https://play.google.com/store/apps/details?id="
    }

    /**
     * Create a deep link for content that can be shared
     */
    fun createContentDeepLink(content: DiscoverContent): String {
        return when (content) {
            is DiscoverContent.Article -> "$DEEP_LINK_SCHEME://discover/article/${content.id}"
            is DiscoverContent.News -> "$DEEP_LINK_SCHEME://discover/news/${content.id}"
            is DiscoverContent.Video -> "$DEEP_LINK_SCHEME://discover/video/${content.id}"
        }
    }

    /**
     * Create a web fallback link for content sharing
     */
    fun createWebFallbackLink(content: DiscoverContent): String {
        return when (content) {
            is DiscoverContent.Article -> "$WEB_BASE_URL/article/${content.id}"
            is DiscoverContent.News -> "$WEB_BASE_URL/news/${content.id}"
            is DiscoverContent.Video -> "$WEB_BASE_URL/video/${content.id}"
        }
    }

    /**
     * Create a universal link that works both as deep link and web fallback
     */
    fun createUniversalLink(content: DiscoverContent): String {
        val webLink = createWebFallbackLink(content)
        val deepLink = createContentDeepLink(content)
        
        return "$webLink?deeplink=${Uri.encode(deepLink)}"
    }

    /**
     * Create an app store link for app promotion
     */
    fun createAppStoreLink(): String {
        val packageName = context.packageName
        return "$PLAY_STORE_URL$packageName"
    }

    /**
     * Handle incoming deep link and extract content information
     */
    fun parseDeepLink(uri: Uri): DeepLinkData? {
        if (uri.scheme != DEEP_LINK_SCHEME) return null
        
        val pathSegments = uri.pathSegments
        if (pathSegments.size < 3) return null
        
        val section = pathSegments[0] // "discover"
        val contentType = pathSegments[1] // "article", "news", "video"
        val contentId = pathSegments[2]
        
        if (section != "discover") return null
        
        return DeepLinkData(
            contentType = contentType,
            contentId = contentId,
            parameters = uri.queryParameterNames.associateWith { uri.getQueryParameter(it) }
        )
    }

    /**
     * Create an intent to open content in the app
     */
    fun createContentIntent(content: DiscoverContent): Intent {
        val deepLink = createContentDeepLink(content)
        return Intent(Intent.ACTION_VIEW, Uri.parse(deepLink)).apply {
            setPackage(context.packageName)
        }
    }

    /**
     * Check if the app can handle a specific deep link
     */
    fun canHandleDeepLink(uri: Uri): Boolean {
        return uri.scheme == DEEP_LINK_SCHEME && 
               uri.host == "discover" && 
               uri.pathSegments.size >= 3
    }

    /**
     * Generate attribution text for sharing
     */
    fun generateAttributionText(): String {
        return "Shared via Health Assistant - Your Personal Health Companion"
    }

    /**
     * Generate app promotion text for sharing
     */
    fun generateAppPromotionText(): String {
        return "Download Health Assistant for more health content: ${createAppStoreLink()}"
    }

    /**
     * Data class for parsed deep link information
     */
    data class DeepLinkData(
        val contentType: String,
        val contentId: String,
        val parameters: Map<String, String?>
    )
}