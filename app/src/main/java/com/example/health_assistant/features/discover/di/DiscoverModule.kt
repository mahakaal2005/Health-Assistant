package com.example.health_assistant.features.discover.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Simplified dependency injection module for Discover feature
 * 
 * This module provides minimal dependency injection configuration for the simplified Discover section.
 * 
 * ## Current Architecture
 * - SimpleDiscoverViewModel: Uses @HiltViewModel annotation
 * - DiscoverRepository: Bound in RepositoryModule  
 * - API Services: Provided in NetworkModule
 * - All classes use @Inject constructors where needed
 * 
 * ## Removed Dependencies
 * - Analytics components
 * - Navigation helpers
 * - Deep link handlers
 * - Complex error handling
 * - Content sharing managers
 * - Bookmarking features
 * 
 * The simplified implementation relies on:
 * 1. Direct API calls through repository
 * 2. Basic error handling in ViewModel
 * 3. Simple content display in Fragment
 * 4. External browser for content viewing
 */
@Module
@InstallIn(SingletonComponent::class)
object DiscoverModule {
    // No providers needed for simplified implementation
    // All dependencies are handled through @Inject constructors
}