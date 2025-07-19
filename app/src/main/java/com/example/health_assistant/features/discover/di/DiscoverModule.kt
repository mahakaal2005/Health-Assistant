package com.example.health_assistant.features.discover.di

import com.example.health_assistant.features.discover.navigation.DiscoverDeepLinkHandler
import com.example.health_assistant.features.discover.navigation.DiscoverNavigationHelper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dependency injection module for Discover feature
 * 
 * This module provides centralized dependency injection configuration for the Discover section.
 * The Discover feature follows a comprehensive DI setup with proper scoping:
 * 
 * ## Repository Layer
 * - DiscoverRepository interface bound to DiscoverRepositoryImpl (@Singleton)
 * - Binding configured in RepositoryModule
 * 
 * ## Data Layer  
 * - DiscoverDao provided by DatabaseModule
 * - All entity classes are simple data classes (no DI needed)
 * - Mapper classes use @Inject constructors with @Singleton scoping
 * 
 * ## Domain Layer
 * - All use cases use @Inject constructors with @Singleton scoping:
 *   * SimpleGetContentUseCase
 *   * SimpleBookmarkUseCase  
 *   * SimpleContentValidationUseCase
 *   * SimpleSearchUseCase
 * - DiscoverManager coordinates use cases (@Singleton)
 * - ContentCredibilityValidator handles validation (@Singleton)
 * - DiscoverErrorHandler manages errors (@Singleton)
 * 
 * ## Presentation Layer
 * - ViewModels use @HiltViewModel annotation:
 *   * DiscoverViewModel
 *   * BookmarksViewModel
 *   * ArticleReaderViewModel
 *   * VideoPlayerViewModel
 * - Fragments use @AndroidEntryPoint annotation
 * - Adapters use standard constructor injection
 * 
 * ## Cache Management
 * - ContentCacheManager handles offline content (@Singleton)
 * - ThumbnailCacheManager manages image caching (@Singleton)  
 * - VideoDownloadManager handles offline videos (@Singleton)
 * 
 * ## Worker Management
 * - ContentSyncWorker handles background content synchronization (@HiltWorker)
 * - ContentSyncScheduler manages sync scheduling (@Singleton)
 * - SyncStatusManager tracks sync status and conflicts (@Singleton)
 * 
 * ## Scoping Strategy
 * - @Singleton: Repository, use cases, managers, validators, cache managers, schedulers
 * - @ViewModelScoped: Automatically handled by @HiltViewModel
 * - @HiltWorker: For background workers
 * - No custom scopes needed for this feature
 * 
 * All classes use @Inject constructors with appropriate scoping annotations,
 * eliminating the need for explicit @Provides methods in most cases.
 */
@Module
@InstallIn(SingletonComponent::class)
object DiscoverModule {
    
    /**
     * Provide navigation helper for Discover feature
     */
    @Provides
    @Singleton
    fun provideDiscoverNavigationHelper(): DiscoverNavigationHelper {
        return DiscoverNavigationHelper()
    }

    /**
     * Provide deep link handler for Discover feature
     */
    @Provides
    @Singleton
    fun provideDiscoverDeepLinkHandler(
        navigationHelper: DiscoverNavigationHelper
    ): DiscoverDeepLinkHandler {
        return DiscoverDeepLinkHandler(navigationHelper)
    }

    /**
     * Provide error mapper for comprehensive error handling
     */
    @Provides
    @Singleton
    fun provideErrorMapper(): com.example.health_assistant.features.discover.domain.error.ErrorMapper {
        return com.example.health_assistant.features.discover.domain.error.ErrorMapper()
    }

    /**
     * Provide retry manager for failed operations
     */
    @Provides
    @Singleton
    fun provideRetryManager(): com.example.health_assistant.features.discover.domain.error.RetryManager {
        return com.example.health_assistant.features.discover.domain.error.RetryManager()
    }

    /**
     * Provide content reporting use case
     */
    @Provides
    @Singleton
    fun provideReportContentUseCase(
        discoverRepository: com.example.health_assistant.features.discover.domain.repository.DiscoverRepository
    ): com.example.health_assistant.features.discover.domain.usecase.ReportContentUseCase {
        return com.example.health_assistant.features.discover.domain.usecase.ReportContentUseCase(discoverRepository)
    }

    /**
     * Provide analytics tracking use case
     */
    @Provides
    @Singleton
    fun provideAnalyticsTrackingUseCase(
        analyticsManager: com.example.health_assistant.features.discover.domain.analytics.AnalyticsManager,
        recommendationEngine: com.example.health_assistant.features.discover.domain.analytics.RecommendationEngine,
        abTestManager: com.example.health_assistant.features.discover.domain.analytics.ABTestManager
    ): com.example.health_assistant.features.discover.domain.usecase.AnalyticsTrackingUseCase {
        return com.example.health_assistant.features.discover.domain.usecase.AnalyticsTrackingUseCase(
            analyticsManager,
            recommendationEngine,
            abTestManager
        )
    }
    
    /**
     * This module provides navigation-related dependencies for the Discover feature.
     * 
     * Current dependency injection is handled through:
     * 1. @Inject constructors on all classes
     * 2. @Singleton scoping for stateful components  
     * 3. Repository binding in RepositoryModule
     * 4. DAO provision in DatabaseModule
     * 5. @HiltViewModel for presentation layer
     * 6. @HiltWorker for background workers
     * 
     * Navigation dependencies:
     * - DiscoverNavigationHelper: Centralized navigation logic (@Singleton)
     * - DiscoverDeepLinkHandler: Deep link processing (@Singleton)
     * 
     * Worker-related dependencies:
     * - ContentSyncWorker: Background content synchronization
     * - ContentSyncScheduler: Sync scheduling and management
     * - SyncStatusManager: Sync status tracking and conflict resolution
     * 
     * Future custom providers can be added here if needed for:
     * - Complex object construction
     * - Third-party library integration
     * - Configuration-dependent instances
     * - Testing overrides
     */
}