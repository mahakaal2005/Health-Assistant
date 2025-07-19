package com.example.health_assistant.features.discover.navigation

import android.os.Bundle
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import com.example.health_assistant.R
import com.example.health_assistant.features.discover.domain.model.DiscoverContent
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Navigation helper for Discover feature
 * Provides centralized navigation logic for all Discover-related screens
 */
@Singleton
class DiscoverNavigationHelper @Inject constructor() {

    /**
     * Navigate to article reader from any discover screen
     */
    fun navigateToArticleReader(
        navController: NavController,
        articleId: String,
        contentType: String = "article"
    ) {
        val bundle = Bundle().apply {
            putString("articleId", articleId)
            putString("contentType", contentType)
        }
        
        val navOptions = NavOptions.Builder()
            .setEnterAnim(R.anim.slide_in_right)
            .setExitAnim(R.anim.slide_out_left)
            .setPopEnterAnim(R.anim.slide_in_left)
            .setPopExitAnim(R.anim.slide_out_right)
            .build()
            
        navController.navigate(R.id.articleReaderFragment, bundle, navOptions)
    }

    /**
     * Navigate to video player from any discover screen
     */
    fun navigateToVideoPlayer(
        navController: NavController,
        videoId: String,
        autoPlay: Boolean = true
    ) {
        val bundle = Bundle().apply {
            putString("videoId", videoId)
            putBoolean("autoPlay", autoPlay)
        }
        
        val navOptions = NavOptions.Builder()
            .setEnterAnim(R.anim.slide_in_right)
            .setExitAnim(R.anim.slide_out_left)
            .setPopEnterAnim(R.anim.slide_in_left)
            .setPopExitAnim(R.anim.slide_out_right)
            .build()
            
        navController.navigate(R.id.videoPlayerFragment, bundle, navOptions)
    }

    /**
     * Navigate to bookmarks screen
     */
    fun navigateToBookmarks(navController: NavController) {
        val navOptions = NavOptions.Builder()
            .setEnterAnim(R.anim.slide_in_right)
            .setExitAnim(R.anim.slide_out_left)
            .setPopEnterAnim(R.anim.slide_in_left)
            .setPopExitAnim(R.anim.slide_out_right)
            .build()
            
        navController.navigate(R.id.bookmarksFragment, null, navOptions)
    }

    /**
     * Navigate to content based on DiscoverContent type
     */
    fun navigateToContent(navController: NavController, content: DiscoverContent) {
        when (content) {
            is DiscoverContent.Article -> {
                navigateToArticleReader(navController, content.id, "article")
            }
            is DiscoverContent.News -> {
                navigateToArticleReader(navController, content.id, "news")
            }
            is DiscoverContent.Video -> {
                navigateToVideoPlayer(navController, content.id)
            }
        }
    }

    /**
     * Navigate to Journal for health tracking (cross-feature navigation)
     */
    fun navigateToJournal(navController: NavController) {
        val navOptions = NavOptions.Builder()
            .setPopUpTo(R.id.homeFragment, false)
            .build()
            
        navController.navigate(R.id.journalFragment, null, navOptions)
    }

    /**
     * Navigate to Profile for health preferences (cross-feature navigation)
     */
    fun navigateToProfile(navController: NavController) {
        val navOptions = NavOptions.Builder()
            .setPopUpTo(R.id.homeFragment, false)
            .build()
            
        navController.navigate(R.id.profileFragment, null, navOptions)
    }

    /**
     * Navigate back to Discover main screen from any discover detail screen
     */
    fun navigateBackToDiscover(navController: NavController) {
        val navOptions = NavOptions.Builder()
            .setPopUpTo(R.id.discoverFragment, false)
            .build()
            
        navController.navigate(R.id.discoverFragment, null, navOptions)
    }

    /**
     * Global navigation to article reader (can be called from anywhere in the app)
     */
    fun navigateToArticleReaderGlobal(
        navController: NavController,
        articleId: String,
        contentType: String = "article"
    ) {
        val bundle = Bundle().apply {
            putString("articleId", articleId)
            putString("contentType", contentType)
        }
        
        navController.navigate(R.id.action_global_to_articleReaderFragment, bundle)
    }

    /**
     * Global navigation to video player (can be called from anywhere in the app)
     */
    fun navigateToVideoPlayerGlobal(
        navController: NavController,
        videoId: String,
        autoPlay: Boolean = true
    ) {
        val bundle = Bundle().apply {
            putString("videoId", videoId)
            putBoolean("autoPlay", autoPlay)
        }
        
        navController.navigate(R.id.action_global_to_videoPlayerFragment, bundle)
    }

    /**
     * Global navigation to bookmarks (can be called from anywhere in the app)
     */
    fun navigateToBookmarksGlobal(navController: NavController) {
        navController.navigate(R.id.action_global_to_bookmarksFragment)
    }

    /**
     * Global navigation to discover main screen (can be called from anywhere in the app)
     */
    fun navigateToDiscoverGlobal(navController: NavController) {
        navController.navigate(R.id.action_global_to_discoverFragment)
    }
}